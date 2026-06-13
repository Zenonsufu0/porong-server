"""
중앙제어 봇 권한/역할 정책 — 인터페이스 레벨.

핵심 원칙
---------
1. 권한 역할(운영 제어)과 알림 역할(플레이어 선택)을 **분리**한다.
2. 운영 권한 역할(Owner/Admin/매니저/Support)은 **수동 지급 전용**이다.
   봇은 절대 버튼·이모지·자동 로직으로 이 역할들을 지급하지 않는다.
3. 알림 역할만 자동 지급(/알림설정 토글 등)을 허용한다.

이 모듈은 각 도메인 모듈(admin/event/zenon_mon/roles)이 공통으로 쓰는
권한 체크 헬퍼와 데코레이터를 제공한다. 실제 운영 명령어 구현은
설계 확정 후 각 모듈에서 진행한다(여기서는 정책·인터페이스만 정의).
"""
from __future__ import annotations

import discord
from discord import app_commands

from core import config

# ─── 역할 분류 ──────────────────────────────────────────────────────
# 수동 지급 전용 권한 역할 키 (config.PERMISSION_ROLE_IDS 와 일치)
MANUAL_PERMISSION_ROLES: tuple[str, ...] = (
    "owner",
    "admin",
    "rpg_manager",
    "zenon_mon_manager",
    "event_manager",
    "support",
)

# 자동 지급 가능한 알림 역할 키 (config.NOTIFY_ROLE_IDS 의 키)
AUTO_GRANTABLE_NOTIFY_ROLES: tuple[str, ...] = tuple(config.NOTIFY_ROLE_IDS.keys())

# 관리자급(전체 운영 권한)으로 취급할 키
ADMIN_LEVEL_ROLES: tuple[str, ...] = ("owner", "admin")

# 권한 서열 (제재 대상 보호용 — 높을수록 상위, 미보유=0). moderation.md §1b:
# "operator보다 같거나 높은 권한 보유자·Owner는 제재 대상에서 제외".
_PERMISSION_RANK: dict[str, int] = {
    "owner":           100,
    "admin":           80,
    "rpg_manager":     50,
    "zenon_mon_manager": 50,
    "event_manager":   50,
    "support":         40,
}


# ─── 정책 질의 ──────────────────────────────────────────────────────

def is_auto_grantable(role_key: str) -> bool:
    """해당 역할 키를 봇이 자동 지급해도 되는가?

    권한 역할은 어떤 경우에도 False. 알림 역할만 True.
    """
    if role_key in MANUAL_PERMISSION_ROLES:
        return False
    return role_key in AUTO_GRANTABLE_NOTIFY_ROLES


def _ids_for(keys: tuple[str, ...]) -> set[int]:
    return {
        config.PERMISSION_ROLE_IDS.get(k, 0)
        for k in keys
        if config.PERMISSION_ROLE_IDS.get(k, 0)
    }


def member_has_permission(member: discord.Member, *keys: str) -> bool:
    """멤버가 주어진 권한 역할 키 중 하나라도 보유하는가?

    Owner 는 항상 통과(슈퍼유저). 미설정(0) 역할은 무시한다.
    """
    if not isinstance(member, discord.Member):
        return False
    member_role_ids = {r.id for r in member.roles}
    owner_id = config.PERMISSION_ROLE_IDS.get("owner", 0)
    if owner_id and owner_id in member_role_ids:
        return True
    return bool(member_role_ids & _ids_for(keys))


def is_admin(member: discord.Member) -> bool:
    """Owner 또는 Admin 권한 보유 여부."""
    return member_has_permission(member, *ADMIN_LEVEL_ROLES)


def permission_rank(member: discord.Member) -> int:
    """멤버의 최고 권한 서열(미보유=0). 제재 대상 보호 판정에 사용(§1b).

    미설정(0) 역할은 무시. 여러 권한 역할 보유 시 최댓값.
    """
    if not isinstance(member, discord.Member):
        return 0
    member_role_ids = {r.id for r in member.roles}
    best = 0
    for key, rank in _PERMISSION_RANK.items():
        rid = config.PERMISSION_ROLE_IDS.get(key, 0)
        if rid and rid in member_role_ids and rank > best:
            best = rank
    return best


# ─── 슬래시 커맨드 권한 데코레이터 ──────────────────────────────────

def requires_permission(*keys: str):
    """app_commands 권한 체크 데코레이터.

    예) @requires_permission("admin", "event_manager")

    설정된 권한 역할이 하나도 없으면(전부 0) 보수적으로 차단한다
    (운영 명령어가 권한 미설정 상태에서 무방비로 열리지 않도록).
    """
    async def predicate(interaction: discord.Interaction) -> bool:
        member = interaction.guild.get_member(interaction.user.id) if interaction.guild else None
        if member is None or not member_has_permission(member, *keys):
            raise app_commands.CheckFailure("이 명령어를 실행할 권한이 없습니다.")
        return True

    return app_commands.check(predicate)
