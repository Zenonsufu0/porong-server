"""
서버 카테고리 템플릿 전개 (T17) — server_lifecycle.md §3 구현.

"서버 생성"은 **새 디스코드 길드가 아니라 기존 허브 길드 안에 게임/시즌 세트**
(온보딩 3역할 + 프리픽스 카테고리 그룹 + 채널들)를 자동 생성하는 것이다(task.md §11).

구조(2026-06-09 결정 — B안: 프리픽스 카테고리, 중첩 시즌/서버 미운영):
  디스코드는 카테고리 중첩 불가 → 한 게임/시즌 = `<표시명> · <그룹>` 카테고리 여러 개가
  사이드바에 나란히 붙어 그룹처럼 보임. domain당 active 1(중첩 미운영)이라 충돌 없음.

역할(온보딩 3역할 상태머신 — task.md §5):
  접근(access)  → 약관 채널만   → 약관 동의 시
  인증전(pending) → 인증 채널만   → 인증 완료 시
  플레이어(player) → 정보·커뮤니티·지원·음성 전체(약관·인증은 이제 안 보임)

전개 모델(§1·§3):
  - 신설(prep) = 카테고리·채널·역할 생성하되 **전부 비공개**(@everyone·3역할 모두 view=False).
  - /서버시작(active) = apply_visibility(visible=True): 약관→접근 / 인증→인증전 / 그 외→플레이어 공개.
  - /서버종료(ended) = apply_visibility(visible=False, archive=True): 전부 숨김 + `[종료]` 프리픽스.
  ※ 역할 부여(접근→인증전→플레이어 전이)는 온보딩 흐름(T7/T9, 후속)이 담당. 여기선 구조·권한만.

실패/중복 시 cleanup() 으로 생성분(역할·카테고리·채널)을 롤백.

⚠ 온보딩 패널(약관동의 버튼·인증 모달) 게시 + verify 라우팅은 후속(ONBOARDING_SERVERS 연결).
"""
from __future__ import annotations

import logging

import discord

log = logging.getLogger(__name__)

_REASON = "T17 서버 템플릿 신설"

# 온보딩 3역할: (키, 역할명 접미). 키 = 가시성 대상 식별자.
_ROLE_SPEC: list[tuple[str, str]] = [
    ("access",  "접근"),
    ("pending", "인증전"),
    ("player",  "플레이어"),
]

# 카테고리 그룹: 각 원소 = 카테고리 1개.
#   key       = server_categories.group_key (레지스트리 키)
#   suffix    = 카테고리명 접미("<표시명> · <suffix>")
#   audience  = active 시 가시성 대상. "player"=플레이어 전체공개 / "onboarding"=채널별(_ONBOARDING_AUDIENCE)
#   channels  = [(이름, "text"|"voice")] — 생성 순서 = 배치 순서
_TEMPLATE_GROUPS: list[dict] = [
    {
        # 온보딩 카테고리("<표시명> · 임시") — 약관+인증. 인증 완료(플레이어) 시 사라짐.
        "key": "onboarding", "suffix": "임시", "audience": "onboarding",
        "channels": [("약관", "text"), ("인증", "text")],
    },
    {
        "key": "info", "suffix": "정보", "audience": "player",
        # 접속정보 = 서버 IP·실시간 상태 게시 자리(T18 후속)
        "channels": [("공지", "text"), ("접속정보", "text"), ("가이드", "text"), ("FAQ", "text")],
    },
    {
        "key": "community", "suffix": "커뮤니티", "audience": "player",
        "channels": [("자유채팅", "text"), ("스크린샷", "text"), ("파티모집", "text")],
    },
    {
        "key": "support", "suffix": "지원·음성", "audience": "player",
        # 건의·일반문의·버그제보 통합(T16) / 임시음성 허브(T13, 카테고리별 다중)
        "channels": [("건의-문의-버그제보", "text"), ("➕ 음성방 만들기", "voice")],
    },
]

# 온보딩 카테고리의 채널별 가시성 대상(role key). 채널 이름으로 매칭.
_ONBOARDING_AUDIENCE: dict[str, str] = {"약관": "access", "인증": "pending"}


def channel_count() -> int:
    return sum(len(g["channels"]) for g in _TEMPLATE_GROUPS)


def group_count() -> int:
    return len(_TEMPLATE_GROUPS)


# ─── 전개 ─────────────────────────────────────────────────────────────

async def provision(
    guild: discord.Guild, *, display_name: str
) -> tuple[dict[str, discord.Role], dict[str, discord.CategoryChannel]]:
    """온보딩 3역할 + 프리픽스 카테고리 그룹 + 채널을 prep(비공개) 상태로 생성.

    반환 = (roles{key→Role}, categories{group_key→CategoryChannel}).
    실패 시 생성분 롤백 후 예외(discord.Forbidden / discord.HTTPException) 재전파.
    """
    roles: dict[str, discord.Role] = {}
    created_roles: list[discord.Role] = []
    created_channels: list[discord.abc.GuildChannel] = []
    try:
        for key, suffix in _ROLE_SPEC:
            r = await guild.create_role(name=f"{display_name} {suffix}", reason=_REASON)
            roles[key] = r
            created_roles.append(r)

        # prep = 비공개: @everyone·3역할 모두 view=False. /서버시작에서 역할별 공개.
        base_overwrites: dict = {guild.default_role: discord.PermissionOverwrite(view_channel=False)}
        for r in created_roles:
            base_overwrites[r] = discord.PermissionOverwrite(view_channel=False)

        categories: dict[str, discord.CategoryChannel] = {}
        for g in _TEMPLATE_GROUPS:
            cat = await guild.create_category(
                f"{display_name} · {g['suffix']}", overwrites=dict(base_overwrites), reason=_REASON
            )
            categories[g["key"]] = cat
            created_channels.append(cat)
            for name, kind in g["channels"]:
                if kind == "voice":
                    ch = await guild.create_voice_channel(name, category=cat, reason=_REASON)
                else:
                    ch = await guild.create_text_channel(name, category=cat, reason=_REASON)
                created_channels.append(ch)
    except discord.HTTPException:
        await _delete_all(created_channels)
        for r in created_roles:
            try:
                await r.delete(reason=f"{_REASON} 롤백")
            except discord.HTTPException:
                log.warning("역할 롤백 실패: %s", r.id, exc_info=True)
        raise
    return roles, categories


async def cleanup(
    roles: dict[str, discord.Role] | None,
    categories: dict[str, discord.CategoryChannel] | None,
) -> None:
    """레지스트리 기록 실패(중복 등) 시 생성 자산 정리(베스트에포트)."""
    if categories:
        for cat in categories.values():
            await _delete_all(list(cat.channels))
            try:
                await cat.delete(reason=f"{_REASON} 롤백")
            except discord.HTTPException:
                log.warning("카테고리 롤백 실패: %s", cat.id, exc_info=True)
    if roles:
        for r in roles.values():
            try:
                await r.delete(reason=f"{_REASON} 롤백")
            except discord.HTTPException:
                log.warning("역할 롤백 실패: %s", r.id, exc_info=True)


# ─── 가시성 전이 (시작/종료) ──────────────────────────────────────────

async def apply_visibility(
    guild: discord.Guild,
    *,
    category_ids: dict[str, int],
    role_ids: dict[str, int],
    visible: bool,
    archive: bool = False,
) -> str:
    """그룹별 카테고리 가시성을 역할 기준으로 토글. 사용자 안내용 짧은 문구 반환.

    category_ids: {group_key: category_id}, role_ids: {"access"/"pending"/"player": role_id}.
    visible=True(시작): 약관→접근 / 인증→인증전 / 그 외 카테고리→플레이어 공개.
    visible=False(종료/prep): 동일 대상 view=False. archive=True 면 카테고리에 [종료] 프리픽스.
    """
    touched = 0
    forbidden = False
    for g in _TEMPLATE_GROUPS:
        cid = category_ids.get(g["key"])
        cat = guild.get_channel(cid) if cid else None
        if not isinstance(cat, discord.CategoryChannel):
            continue
        try:
            if g["audience"] == "onboarding":
                for ch in cat.channels:
                    aud = _ONBOARDING_AUDIENCE.get(ch.name)
                    role = guild.get_role(role_ids.get(aud, 0)) if aud else None
                    if role is not None:
                        await ch.set_permissions(role, view_channel=visible, reason=_REASON)
                        touched += 1
            else:  # "player"
                role = guild.get_role(role_ids.get("player", 0))
                if role is not None:
                    await cat.set_permissions(role, view_channel=visible, reason=_REASON)
                    touched += 1
            if archive and not visible and not cat.name.startswith("[종료]"):
                await cat.edit(name=f"[종료] {cat.name}", reason="서버 종료 아카이브")
        except discord.Forbidden:
            forbidden = True
    if forbidden:
        return "⚠ 봇 권한 부족(Manage Channels/Roles) — 일부 가시성 미반영"
    if touched == 0:
        return "카테고리/역할 미연결(상태만 전이)"
    return f"카테고리 가시성 갱신({touched}건)"


async def _delete_all(channels: list) -> None:
    # 역순 삭제(자식 채널 먼저). 카테고리 삭제는 자식 채널을 지우지 않으므로 명시 삭제 필요.
    for ch in reversed(channels):
        try:
            await ch.delete(reason=f"{_REASON} 롤백")
        except discord.HTTPException:
            log.warning("채널 롤백 실패: %s", getattr(ch, "id", "?"), exc_info=True)
