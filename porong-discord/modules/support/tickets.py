"""
티켓 (1:1 문의) — T16 support.md §2.

`/문의` → 비공개 채널 생성(개설자 + Support·admin 가시) → tickets INSERT(open).
채널 내 운영진과 대화 → `/티켓종료` 또는 [티켓 종료] 버튼 → 잠금·아카이브(채널 보존).

권한: 개설 = 공통(유저, 동시 1개 제한). 종료 = 개설자 또는 admin·support.
모든 개설/종료 = mod_log 기록. 채널은 삭제 대신 잠금([종료] 프리픽스 + 개설자 접근 해제, 기록 보존).
"""
from __future__ import annotations

import logging

import discord
from discord import app_commands
from discord.ext import commands

from core import config, mod_log, permissions, tickets
from core.permissions import requires_permission

log = logging.getLogger(__name__)


class TicketCloseView(discord.ui.View):
    """티켓 채널 [종료] 버튼 (영구 뷰 — custom_id 고정)."""

    def __init__(self, cog: "TicketCog") -> None:
        super().__init__(timeout=None)
        self.cog = cog
        btn = discord.ui.Button(
            label="티켓 종료", style=discord.ButtonStyle.red, custom_id="ticket_close"
        )
        btn.callback = self._on_click
        self.add_item(btn)

    async def _on_click(self, interaction: discord.Interaction) -> None:
        await self.cog.close_here(interaction)


class TicketCog(commands.Cog):
    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot

    @property
    def db(self):
        return self.bot.db  # type: ignore[attr-defined]

    @commands.Cog.listener()
    async def on_ready(self) -> None:
        self.bot.add_view(TicketCloseView(self))  # 재시작 후 종료 버튼 동작

    def _staff_overwrites(self, guild: discord.Guild, opener: discord.Member) -> dict:
        """티켓 채널 권한: @everyone 숨김 + 개설자·운영(admin/support)·봇 가시."""
        allow = discord.PermissionOverwrite(view_channel=True, send_messages=True)
        ow: dict = {
            guild.default_role: discord.PermissionOverwrite(view_channel=False),
            opener: allow,
        }
        if guild.me is not None:
            ow[guild.me] = allow
        for key in ("admin", "support"):
            role = guild.get_role(config.PERMISSION_ROLE_IDS.get(key, 0))
            if role is not None:
                ow[role] = allow
        return ow

    @app_commands.command(name="문의", description="운영진과 1:1 비공개 문의 채널을 엽니다.")
    async def open_ticket(self, interaction: discord.Interaction) -> None:
        guild = interaction.guild
        member = interaction.user
        if guild is None or not isinstance(member, discord.Member):
            await interaction.response.send_message("길드에서만 사용할 수 있습니다.", ephemeral=True)
            return

        existing = await tickets.get_open_by_opener(self.db, member.id)
        if existing is not None:
            await interaction.response.send_message(
                f"이미 열린 문의가 있습니다: <#{existing['channel_id']}>. 먼저 종료해주세요.",
                ephemeral=True,
            )
            return

        await interaction.response.defer(ephemeral=True)
        category = guild.get_channel(config.CATEGORY_티켓_ID) if config.CATEGORY_티켓_ID else None
        category = category if isinstance(category, discord.CategoryChannel) else None
        try:
            channel = await guild.create_text_channel(
                f"문의-{member.name}"[:100],
                category=category,
                overwrites=self._staff_overwrites(guild, member),
                reason=f"티켓 개설 — {member}",
            )
        except discord.Forbidden:
            await interaction.followup.send(
                "봇 권한 부족(Manage Channels)으로 문의 채널을 만들 수 없습니다.", ephemeral=True
            )
            return

        tid = await tickets.create_ticket(self.db, channel.id, member.id)
        await mod_log.record(
            self.bot, action="ticket_open", operator_id=member.id,
            detail={"ticket_id": tid, "channel_id": channel.id},
        )
        embed = discord.Embed(
            title=f"문의 #{tid}",
            description=(
                f"{member.mention} 님의 문의 채널입니다. 운영진이 곧 확인합니다.\n"
                "문의 내용을 남겨주세요. 완료되면 아래 **티켓 종료** 버튼을 눌러주세요."
            ),
            color=discord.Color.blurple(),
        )
        await channel.send(embed=embed, view=TicketCloseView(self))
        await interaction.followup.send(f"문의 채널을 열었습니다: {channel.mention}", ephemeral=True)

    @app_commands.command(name="티켓종료", description="현재 문의 채널을 종료합니다.")
    async def close_ticket_cmd(self, interaction: discord.Interaction) -> None:
        await self.close_here(interaction)

    async def close_here(self, interaction: discord.Interaction) -> None:
        """현재 채널의 티켓을 종료(개설자 또는 운영). 버튼·명령 공용."""
        channel = interaction.channel
        guild = interaction.guild
        if guild is None or not isinstance(channel, discord.TextChannel):
            await interaction.response.send_message("문의 채널에서만 사용할 수 있습니다.", ephemeral=True)
            return
        row = await tickets.get_by_channel(self.db, channel.id)
        if row is None:
            await interaction.response.send_message("이 채널은 문의 채널이 아닙니다.", ephemeral=True)
            return
        if row["state"] == "closed":
            await interaction.response.send_message("이미 종료된 문의입니다.", ephemeral=True)
            return

        member = interaction.user
        is_staff = isinstance(member, discord.Member) and permissions.member_has_permission(
            member, "admin", "support"
        )
        if member.id != row["opener_id"] and not is_staff:
            await interaction.response.send_message(
                "본인 또는 운영진만 종료할 수 있습니다.", ephemeral=True
            )
            return

        await tickets.close_ticket(self.db, row["id"], member.id)
        await mod_log.record(
            self.bot, action="ticket_close", operator_id=member.id, target_id=row["opener_id"],
            detail={"ticket_id": row["id"], "channel_id": channel.id},
        )
        # 잠금·아카이브: 개설자 접근 해제 + [종료] 프리픽스(채널 보존).
        try:
            opener = guild.get_member(row["opener_id"])
            if opener is not None:
                await channel.set_permissions(opener, overwrite=None, reason="티켓 종료")
            if not channel.name.startswith("[종료]"):
                await channel.edit(name=f"[종료]-{channel.name}"[:100], reason="티켓 종료 아카이브")
        except discord.HTTPException:
            log.warning("티켓 종료 채널 정리 실패: #%s", row["id"], exc_info=True)
        await interaction.response.send_message(
            f"✅ 문의 #{row['id']} 를 종료했습니다.", allowed_mentions=discord.AllowedMentions.none()
        )


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(TicketCog(bot))
