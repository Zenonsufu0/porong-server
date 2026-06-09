"""
서버 카테고리 템플릿 전개 (T17) — server_lifecycle.md §3 구현.

"서버 생성"은 **새 디스코드 길드가 아니라 기존 허브 길드 안에 게임/시즌 세트**
(접근역할 + 카테고리 + 채널들)를 자동 생성하는 것이다(task.md §11).

전개 모델(§1·§3):
  - 신설(prep) 시점 = 카테고리·채널·접근역할 **생성하되 비공개**(@everyone·접근역할 모두 view=False).
    유저 접근 불가. /서버시작(prep→active)에서 접근역할에 view=True 부여(commands._set_category_visibility).
  - 따라서 여기서는 '숨겨진 채로 구조만' 만든다. 가시성 ON 은 시작 전이가 담당.

실패/중복 시 cleanup() 으로 생성분을 롤백(부분 생성 잔재 방지).

⚠ 온보딩(약관/인증) 배선: 자동 생성된 약관/인증 채널은 ONBOARDING_SERVERS(.env 기반)에
   자동 연결되지 않는다(현행 env ID 매핑). 온보딩 패널 연결은 후속(레지스트리에 채널ID 영속화 필요).
"""
from __future__ import annotations

import logging

import discord

log = logging.getLogger(__name__)

# 채널 템플릿: (이름, 종류). 종류 = "text" | "voice".
# 도메인별 오버라이드 없으면 _DEFAULT 사용.
_DEFAULT_TEMPLATE: list[tuple[str, str]] = [
    ("공지", "text"),
    ("약관", "text"),
    ("인증", "text"),
    ("잡담", "text"),
    ("음성", "voice"),
]

# 도메인별 템플릿 오버라이드(필요 시 추가). 미정의 도메인은 _DEFAULT.
_DOMAIN_TEMPLATES: dict[str, list[tuple[str, str]]] = {}

_REASON = "T17 서버 템플릿 신설"


def template_for(domain: str) -> list[tuple[str, str]]:
    return _DOMAIN_TEMPLATES.get(domain, _DEFAULT_TEMPLATE)


async def provision(
    guild: discord.Guild, *, domain: str, display_name: str
) -> tuple[discord.Role, discord.CategoryChannel]:
    """접근역할 + 카테고리 + 채널 세트를 prep(비공개) 상태로 생성.

    반환 = (생성한 접근역할, 생성한 카테고리).
    실패 시 생성분을 롤백하고 예외(discord.Forbidden / discord.HTTPException)를 재전파.
    """
    role = await guild.create_role(name=f"{display_name} 접근", reason=_REASON)
    created: list[discord.abc.GuildChannel] = []
    try:
        # prep = 비공개: @everyone·접근역할 모두 숨김. /서버시작에서 접근역할 view=True 로 공개.
        overwrites = {
            guild.default_role: discord.PermissionOverwrite(view_channel=False),
            role: discord.PermissionOverwrite(view_channel=False),
        }
        category = await guild.create_category(
            display_name, overwrites=overwrites, reason=_REASON
        )
        created.append(category)
        for name, kind in template_for(domain):
            if kind == "voice":
                ch = await guild.create_voice_channel(name, category=category, reason=_REASON)
            else:
                ch = await guild.create_text_channel(name, category=category, reason=_REASON)
            created.append(ch)
    except discord.HTTPException:
        # 부분 생성 롤백: 채널 → 역할 순으로 정리(베스트에포트).
        await _delete_all(created)
        try:
            await role.delete(reason=f"{_REASON} 롤백")
        except discord.HTTPException:
            log.warning("접근역할 롤백 실패: %s", role.id, exc_info=True)
        raise
    return role, category


async def cleanup(role: discord.Role | None, category: discord.CategoryChannel | None) -> None:
    """레지스트리 기록 실패(중복 등) 시 생성한 디스코드 자산 정리(베스트에포트)."""
    if category is not None:
        await _delete_all(list(category.channels))
        try:
            await category.delete(reason=f"{_REASON} 롤백")
        except discord.HTTPException:
            log.warning("카테고리 롤백 실패: %s", category.id, exc_info=True)
    if role is not None:
        try:
            await role.delete(reason=f"{_REASON} 롤백")
        except discord.HTTPException:
            log.warning("접근역할 롤백 실패: %s", role.id, exc_info=True)


async def _delete_all(channels: list) -> None:
    # 역순 삭제(자식 채널 먼저). 카테고리 삭제는 자식 채널을 지우지 않으므로 명시 삭제 필요.
    for ch in reversed(channels):
        try:
            await ch.delete(reason=f"{_REASON} 롤백")
        except discord.HTTPException:
            log.warning("채널 롤백 실패: %s", getattr(ch, "id", "?"), exc_info=True)
