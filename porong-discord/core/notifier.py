"""
통합 알림 디스패처 (T1) — notifications.md ②③, DL-133.

게임서버 push(core/inbound.py) 또는 봇 내부 트리거 → `dispatch(bot, domain, kind, data)`.
라우팅 테이블 `(domain, kind) → (채널 config 키, 멘션 역할 키)` lookup → 등록된 embed
빌더로 임베드 구성 → 채널 전송 + 역할 멘션. 전송 실패는 best-effort(무시 + 로깅).

도메인 격리: 이 모듈은 도메인 코드를 import 하지 않는다. 각 도메인/알림 모듈이 로드 시
`register_builder(domain, kind, fn)` 으로 embed 빌더를 등록하고, 여기선 등록분만 조회한다.
"""
from __future__ import annotations

import logging
from typing import Callable

import discord

from core import config

log = logging.getLogger(__name__)

# (domain, kind) → (채널 config 속성명, 멘션 역할 키 또는 None). notifications.md ③.
_ROUTES: dict[tuple[str, str], tuple[str, str | None]] = {
    ("rpg", "field_boss_pre"):     ("CHANNEL_FIELD_BOSS_ID", "필드보스알림"),
    ("rpg", "field_boss_spawn"):   ("CHANNEL_FIELD_BOSS_ID", "필드보스알림"),
    ("rpg", "season_boss_recruit"):("CHANNEL_NOTICE_ID",      "시즌보스알림"),
    ("common", "world_boss"):      ("CHANNEL_NOTICE_ID",      "월드보스알림"),
    ("common", "maintenance"):     ("CHANNEL_NOTICE_ID",      "점검알림"),
    ("common", "update"):          ("CHANNEL_NOTICE_ID",      "업데이트알림"),
    ("common", "event_start"):     ("CHANNEL_NOTICE_ID",      "이벤트알림"),
    ("common", "event_end"):       ("CHANNEL_NOTICE_ID",      "이벤트알림"),
    ("poromon", "event"):          ("CHANNEL_POROMON_NOTICE_ID", "포로몬알림"),
}

# (domain, kind) → builder(data: dict) -> discord.Embed
_BUILDERS: dict[tuple[str, str], Callable[[dict], discord.Embed]] = {}


def register_builder(domain: str, kind: str, builder: Callable[[dict], discord.Embed]) -> None:
    """도메인 모듈이 로드 시 embed 빌더를 등록(도메인 격리 유지)."""
    _BUILDERS[(domain, kind)] = builder


def has_route(domain: str, kind: str) -> bool:
    return (domain, kind) in _ROUTES


def _fallback_embed(domain: str, kind: str, data: dict) -> discord.Embed:
    """빌더 미등록 시 generic 임베드(크래시 대신 최소 표시)."""
    embed = discord.Embed(
        title=f"📢 {domain}.{kind}",
        color=discord.Color.blurple(),
    )
    for k, v in list(data.items())[:10]:
        embed.add_field(name=str(k)[:256], value=str(v)[:1024], inline=True)
    return embed


async def dispatch(bot, domain: str, kind: str, data: dict) -> bool:
    """알림 1건 라우팅·전송. 전송 성공 시 True, 미등록/채널없음/실패 시 False(graceful).

    반환값은 운영자 트리거 명령(T5)의 피드백용 — 인바운드 push 경로는 무시해도 무방.
    """
    route = _ROUTES.get((domain, kind))
    if route is None:
        log.info("미등록 알림 (%s.%s) — 무시", domain, kind)
        return False
    channel_attr, mention_key = route

    builder = _BUILDERS.get((domain, kind))
    try:
        embed = builder(data) if builder else _fallback_embed(domain, kind, data)
    except Exception:  # 빌더가 data 형식 오류로 던져도 봇은 멈추지 않음
        log.warning("embed 빌더 실패 (%s.%s)", domain, kind, exc_info=True)
        return False

    channel_id = getattr(config, channel_attr, 0)
    channel = bot.get_channel(channel_id) if channel_id else None
    if not isinstance(channel, (discord.TextChannel, discord.Thread)):
        log.warning("알림 채널 없음/부적합 (%s.%s, %s=%s)", domain, kind, channel_attr, channel_id)
        return False

    mention_id = config.NOTIFY_ROLE_IDS.get(mention_key, 0) if mention_key else 0
    content = f"<@&{mention_id}>" if mention_id else None
    allowed = (
        discord.AllowedMentions(roles=True) if mention_id else discord.AllowedMentions.none()
    )
    try:
        await channel.send(content=content, embed=embed, allowed_mentions=allowed)
        return True
    except discord.HTTPException:
        log.warning("알림 전송 실패 (%s.%s)", domain, kind, exc_info=True)
        return False
