"""
임시역할 자동만료 — T14 community_level.md §10.

`/임시역할부여 <유저> <역할> <기간> <단위> [사유]`(admin) → 역할 부여 + temp_roles 적재.
주기 tick(60초)이 만료분을 조회 → 역할 회수 + 행 삭제(재시작 후에도 DB 기준 복구).
모든 부여/만료 = mod_log 기록.
"""
from __future__ import annotations

import logging
import time

import discord
from discord import app_commands
from discord.ext import commands, tasks

from core import config, mod_log, temp_roles
from core.permissions import requires_permission

log = logging.getLogger(__name__)

_EXPIRE_TICK_SEC = 60
_UNIT_SECONDS = {"minute": 60, "hour": 3600, "day": 86400}
_UNIT_CHOICES = [
    app_commands.Choice(name="분", value="minute"),
    app_commands.Choice(name="시간", value="hour"),
    app_commands.Choice(name="일", value="day"),
]


class TempRoleCog(commands.Cog):
    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot

    @property
    def db(self):
        return self.bot.db  # type: ignore[attr-defined]

    async def cog_load(self) -> None:
        self._expire_loop.start()

    async def cog_unload(self) -> None:
        self._expire_loop.cancel()

    # ─── 자동 만료 tick ───────────────────────────────────────────

    @tasks.loop(seconds=_EXPIRE_TICK_SEC)
    async def _expire_loop(self) -> None:
        rows = await temp_roles.due(self.db, int(time.time()))
        if not rows:
            return
        guild = self.bot.get_guild(config.GUILD_ID)
        for row in rows:
            if guild is not None:
                member = guild.get_member(row["discord_user_id"])
                role = guild.get_role(row["role_id"])
                if member is not None and role is not None and role in member.roles:
                    try:
                        await member.remove_roles(role, reason="임시역할 만료")
                        await mod_log.record(
                            self.bot, action="temp_role_expire",
                            operator_id=self.bot.user.id if self.bot.user else 0,
                            target_id=row["discord_user_id"],
                            detail={"role_id": row["role_id"]},
                        )
                    except discord.HTTPException:
                        log.warning("임시역할 회수 실패: user=%s role=%s",
                                    row["discord_user_id"], row["role_id"], exc_info=True)
            await temp_roles.delete(self.db, row["id"])

    @_expire_loop.before_loop
    async def _before(self) -> None:
        await self.bot.wait_until_ready()

    # ─── 운영 명령 ────────────────────────────────────────────────

    @app_commands.command(name="임시역할부여", description="기간이 지나면 자동 회수되는 역할을 부여합니다(운영).")
    @app_commands.describe(유저="대상", 역할="부여할 역할", 기간="숫자", 단위="기간 단위", 사유="사유(선택)")
    @app_commands.choices(단위=_UNIT_CHOICES)
    @requires_permission("admin")
    async def grant_temp_role(
        self,
        interaction: discord.Interaction,
        유저: discord.Member,
        역할: discord.Role,
        기간: app_commands.Range[int, 1, 3650],
        단위: app_commands.Choice[str],
        사유: str | None = None,
    ) -> None:
        guild = interaction.guild
        if guild is None:
            await interaction.response.send_message("길드에서만 사용할 수 있습니다.", ephemeral=True)
            return
        # 봇 위계 검증: 봇 최상위 역할보다 낮아야 부여/회수 가능.
        if guild.me is not None and 역할 >= guild.me.top_role:
            await interaction.response.send_message(
                "봇보다 높거나 같은 역할은 부여할 수 없습니다(역할 위계).", ephemeral=True
            )
            return
        if 역할.is_default() or 역할.managed:
            await interaction.response.send_message(
                "@everyone 또는 봇/연동 관리 역할은 지정할 수 없습니다.", ephemeral=True
            )
            return

        expires_at = int(time.time()) + 기간 * _UNIT_SECONDS[단위.value]
        try:
            await 유저.add_roles(역할, reason=f"임시역할 — {사유 or '사유 없음'}")
        except discord.Forbidden:
            await interaction.response.send_message(
                "봇 권한 부족(Manage Roles)으로 역할을 부여할 수 없습니다.", ephemeral=True
            )
            return

        await temp_roles.grant(self.db, 유저.id, 역할.id, expires_at, 사유)
        await mod_log.record(
            self.bot, action="temp_role_grant", operator_id=interaction.user.id,
            target_id=유저.id, reason=사유,
            detail={"role_id": 역할.id, "단위": 단위.name, "기간": 기간},
        )
        await interaction.response.send_message(
            f"✅ {유저.mention} 에게 {역할.mention} 부여 — "
            f"<t:{expires_at}:R> (<t:{expires_at}:f>) 자동 회수.",
            allowed_mentions=discord.AllowedMentions.none(),
        )


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(TempRoleCog(bot))
