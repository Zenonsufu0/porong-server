"""
관리자/운영 명령어 모듈 — 알림 발송(T5) 구현 + 상태변경 명령 스텁.

🟢 **알림 발송(T5):** `/공지`·`/점검`·`/보스알림` — 운영자가 직접 알림을 쏜다.
   게임 서버 상태를 바꾸지 않으므로(디스코드 채널 게시일 뿐) 봇 단독으로 동작한다.
   전송은 `core/notifier.dispatch` 위임(라우팅·embed·멘션 일원화). 발송은 mod_log 기록.

⚠️ **상태 변경(역할 부여·골드 지급·닉네임 변경 등)은 미구현** — 게임 서버 API 경유 +
   사용자 명시 요청 시에만(admin.md 보안 원칙). 아래 Cog 에는 알림 명령만 있다.

TODO (설계/승인 후, 상태 변경):
  - /역할부여(권한역할 거부 가드) · /유저조회 · /골드지급(API) · /닉네임변경(API)
"""
from __future__ import annotations

import logging

import discord
from discord import app_commands
from discord.ext import commands

from core import mod_log, notifier
from core.permissions import requires_permission

log = logging.getLogger(__name__)

_BOSS_CHOICES = [
    app_commands.Choice(name="월드보스", value="world"),
    app_commands.Choice(name="시즌보스", value="season"),
]


class AdminCog(commands.Cog):
    """관리자 도메인 Cog — 알림 발송(T5)."""

    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot

    async def _send(
        self, interaction: discord.Interaction, *,
        domain: str, kind: str, data: dict, label: str,
    ) -> None:
        """notifier 발송 + 운영 피드백 + mod_log. 공통 경로."""
        await interaction.response.defer(ephemeral=True)
        sent = await notifier.dispatch(self.bot, domain, kind, data)
        if sent:
            await mod_log.record(
                self.bot, action="announce", operator_id=interaction.user.id,
                detail={"kind": f"{domain}.{kind}", "label": label},
            )
            await interaction.followup.send(f"✅ {label} 알림을 발송했습니다.", ephemeral=True)
        else:
            await interaction.followup.send(
                f"⚠️ {label} 발송 실패 — 알림 채널이 설정되지 않았거나 권한이 부족합니다. "
                "운영진에게 채널 설정을 확인해주세요.",
                ephemeral=True,
            )

    @app_commands.command(name="공지", description="업데이트/패치 공지를 발송합니다(운영).")
    @app_commands.describe(요약="공지 내용", 버전="버전(선택)")
    @requires_permission("admin")
    async def announce_update(
        self, interaction: discord.Interaction, 요약: str, 버전: str | None = None
    ) -> None:
        await self._send(
            interaction, domain="common", kind="update",
            data={"summary": 요약, "version": 버전}, label="공지",
        )

    @app_commands.command(name="점검", description="점검 안내를 발송합니다(운영).")
    @app_commands.describe(시작="점검 시작 시각", 예상시간="예상 소요(예: 1시간)", 사유="사유(선택)")
    @requires_permission("admin")
    async def announce_maintenance(
        self, interaction: discord.Interaction,
        시작: str, 예상시간: str, 사유: str | None = None,
    ) -> None:
        await self._send(
            interaction, domain="common", kind="maintenance",
            data={"start": 시작, "eta": 예상시간, "reason": 사유}, label="점검",
        )

    @app_commands.command(name="보스알림", description="월드/시즌 보스 알림을 발송합니다(운영).")
    @app_commands.describe(종류="보스 종류", 보스="보스 이름", 일정="등장/모집 일정(선택)", 안내="추가 안내(선택, 시즌보스)")
    @app_commands.choices(종류=_BOSS_CHOICES)
    @requires_permission("admin", "event_manager")
    async def announce_boss(
        self, interaction: discord.Interaction,
        종류: app_commands.Choice[str], 보스: str,
        일정: str | None = None, 안내: str | None = None,
    ) -> None:
        if 종류.value == "world":
            await self._send(
                interaction, domain="common", kind="world_boss",
                data={"boss_name": 보스, "when": 일정}, label="월드보스",
            )
        else:
            await self._send(
                interaction, domain="rpg", kind="season_boss_recruit",
                data={"boss_name": 보스, "when": 일정, "info": 안내}, label="시즌보스",
            )


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(AdminCog(bot))
