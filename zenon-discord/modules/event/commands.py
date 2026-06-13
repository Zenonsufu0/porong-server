"""
이벤트 명령어 모듈 — 이벤트 알림 발송(T5) 구현.

🟢 **`/이벤트알림 시작|종료`** — 운영자(event_manager)가 이벤트 시작/종료 알림을 발송.
   notifier 경유(common.event_start / event_end → `@이벤트알림`). 봇 단독(게임 무관).

⚠️ 이벤트 일정 영속(`/이벤트등록`·`/일정`)·참여 패널은 별도 설계 후(T6). 현재는 알림만.
"""
from __future__ import annotations

import logging

import discord
from discord import app_commands
from discord.ext import commands

from core import mod_log, notifier
from core.permissions import requires_permission

log = logging.getLogger(__name__)

_PHASE_CHOICES = [
    app_commands.Choice(name="시작", value="event_start"),
    app_commands.Choice(name="종료", value="event_end"),
]


class EventCog(commands.Cog):
    """이벤트 도메인 Cog — 이벤트 알림(T5)."""

    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot

    @app_commands.command(name="이벤트알림", description="이벤트 시작/종료 알림을 발송합니다(운영).")
    @app_commands.describe(구분="시작 또는 종료", 이벤트="이벤트 이름", 안내="추가 안내(선택)")
    @app_commands.choices(구분=_PHASE_CHOICES)
    @requires_permission("admin", "event_manager")
    async def event_notify(
        self, interaction: discord.Interaction,
        구분: app_commands.Choice[str], 이벤트: str, 안내: str | None = None,
    ) -> None:
        await interaction.response.defer(ephemeral=True)
        kind = 구분.value
        sent = await notifier.dispatch(
            self.bot, "common", kind, {"event_name": 이벤트, "info": 안내}
        )
        label = "이벤트 시작" if kind == "event_start" else "이벤트 종료"
        if sent:
            await mod_log.record(
                self.bot, action="announce", operator_id=interaction.user.id,
                detail={"kind": f"common.{kind}", "label": label, "event": 이벤트},
            )
            await interaction.followup.send(f"✅ {label} 알림을 발송했습니다.", ephemeral=True)
        else:
            await interaction.followup.send(
                f"⚠️ {label} 발송 실패 — 알림 채널 설정/권한을 확인해주세요.", ephemeral=True
            )


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(EventCog(bot))
