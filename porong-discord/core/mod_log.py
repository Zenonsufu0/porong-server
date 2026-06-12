"""
운영/감사 로그 인프라 (T15 mod_log) — data_model.md §2.5 + moderation.md §3.

모든 제재·서버 생애주기·운영명령은 record() 한 번으로:
  1) mod_log 테이블 적재(누가·언제·무엇·왜)         — 보장
  2) #운영로그(CHANNEL_MODLOG_ID) 임베드 게시        — best-effort

게시는 best-effort: 채널 미설정(0)·미발견·권한부족·전송실패여도 DB 적재는 보장한다.
moderation(T15)·admin(T17)·server_lifecycle(T21) 공통 선행 — raw INSERT 중복 방지.
"""
from __future__ import annotations

import json
import logging

import discord
from discord.ext import commands

from core import config
from core.db import Database

log = logging.getLogger(__name__)

# action 코드 → (한국어 라벨, 임베드 색). 미등록 action 도 graceful(코드 그대로 표시).
_ACTION_META: dict[str, tuple[str, discord.Color]] = {
    "warn":          ("⚠ 경고",        discord.Color.yellow()),
    "warn_revoke":   ("↩ 경고취소",    discord.Color.light_grey()),
    "timeout":       ("⏲ 타임아웃",    discord.Color.orange()),
    "timeout_clear": ("⏲ 타임아웃해제", discord.Color.light_grey()),
    "kick":          ("👢 추방",        discord.Color.red()),
    "ban":           ("🔨 차단",        discord.Color.dark_red()),
    "unban":         ("🔓 차단해제",    discord.Color.green()),
    "server_create": ("🆕 서버신설",    discord.Color.blurple()),
    "server_start":  ("🟢 서버시작",    discord.Color.green()),
    "server_end":    ("⚫ 서버종료",    discord.Color.dark_grey()),
    "relink":        ("🔁 닉네임재인증", discord.Color.blue()),
    "terms_update":  ("📜 약관수정",    discord.Color.teal()),
    "xp_grant":      ("✨ XP지급",      discord.Color.green()),
    "xp_revoke":     ("➖ XP회수",      discord.Color.orange()),
    "ticket_open":   ("🎫 문의개설",    discord.Color.blurple()),
    "ticket_close":  ("🔒 문의종료",    discord.Color.light_grey()),
    "faq_add":       ("❓ FAQ추가",     discord.Color.green()),
    "faq_update":    ("✏ FAQ수정",     discord.Color.blue()),
    "faq_delete":    ("🗑 FAQ삭제",     discord.Color.light_grey()),
    "temp_role_grant":  ("⏳ 임시역할부여", discord.Color.teal()),
    "temp_role_expire": ("⌛ 임시역할만료", discord.Color.light_grey()),
    "announce":         ("📢 알림발송",    discord.Color.blurple()),
}


async def record(
    bot: commands.Bot,
    *,
    action: str,
    operator_id: int,
    target_id: int | None = None,
    reason: str | None = None,
    detail: dict | None = None,
) -> int:
    """운영 액션을 mod_log 에 적재 + #운영로그 게시. mod_log.id 반환.

    detail(dict)은 JSON 직렬화해 저장한다(부가정보). 게시는 best-effort.
    """
    db: Database = bot.db  # type: ignore[attr-defined]
    detail_json = json.dumps(detail, ensure_ascii=False) if detail else None
    log_id = await db.execute(
        "INSERT INTO mod_log(action, target_id, operator_id, reason, detail, created_at) "
        "VALUES(?, ?, ?, ?, ?, strftime('%s','now'))",
        (action, target_id, operator_id, reason, detail_json),
    )
    await _post(bot, action, log_id, operator_id, target_id, reason, detail)
    return log_id


async def _post(
    bot: commands.Bot,
    action: str,
    log_id: int,
    operator_id: int,
    target_id: int | None,
    reason: str | None,
    detail: dict | None,
) -> None:
    """#운영로그 임베드 게시(best-effort). 실패는 로깅만 — record() 흐름을 막지 않는다."""
    channel_id = config.CHANNEL_MODLOG_ID
    if not channel_id:
        return
    channel = bot.get_channel(channel_id)
    if not isinstance(channel, (discord.TextChannel, discord.Thread)):
        log.warning("운영로그 채널(%s) 미발견 — DB만 적재(mod_log #%d)", channel_id, log_id)
        return
    label, color = _ACTION_META.get(action, (action, discord.Color.greyple()))
    embed = discord.Embed(title=label, color=color, timestamp=discord.utils.utcnow())
    embed.add_field(name="처리자", value=f"<@{operator_id}>", inline=True)
    if target_id is not None:
        embed.add_field(name="대상", value=f"<@{target_id}>", inline=True)
    if reason:
        embed.add_field(name="사유", value=reason[:1024], inline=False)
    if detail:
        detail_str = ", ".join(f"`{k}`={v}" for k, v in detail.items())
        embed.add_field(name="상세", value=detail_str[:1024], inline=False)
    embed.set_footer(text=f"mod_log #{log_id}")
    try:
        await channel.send(embed=embed, allowed_mentions=discord.AllowedMentions.none())
    except discord.HTTPException:
        log.warning("운영로그 게시 실패(mod_log #%d)", log_id, exc_info=True)
