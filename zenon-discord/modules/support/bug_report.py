"""
버그제보 — T16 support.md §1 (rpg.md §4 일반화).

`/버그제보 대상 심각도` → 모달(제목·재현·기대·실제) → 공통 #버그제보 채널 임베드 게시
(`BUG-{메시지}` 접수번호) + 제보자 DM + 운영진 상태 버튼(접수→확인중→완료/기각).

설계 경계(data_model §0): 버그 데이터는 **봇 DB 에 저장하지 않는다**. 채널 임베드가 유일 기록.
RPG/포로몬 게임 버그의 게임 DB 이관(`rpg_api.create_bug_report` 등)은 게임 API 확정 후
연동(현재는 도메인 태그만 달아 동일 채널로 접수 — 운영 가시 baseline).
"""
from __future__ import annotations

import logging
import re

import discord
from discord import app_commands
from discord.ext import commands

from core import config, permissions

log = logging.getLogger(__name__)

_REPORTER_RE = re.compile(r"reporter:(\d+)")

_DOMAIN_CHOICES = [
    app_commands.Choice(name="RPG", value="rpg"),
    app_commands.Choice(name="포로몬", value="poromon"),
    app_commands.Choice(name="기타·봇", value="etc"),
]
_DOMAIN_LABEL = {"rpg": "RPG", "poromon": "포로몬", "etc": "기타·봇"}
# 게임 DB 이관 예정 도메인(현재 채널 baseline 접수 + 안내 노트).
_GAME_DOMAINS = {"rpg", "poromon"}

_SEVERITY_CHOICES = [
    app_commands.Choice(name="낮음", value="low"),
    app_commands.Choice(name="보통", value="normal"),
    app_commands.Choice(name="높음", value="high"),
    app_commands.Choice(name="심각", value="critical"),
]
_SEVERITY_LABEL = {"low": "🟢 낮음", "normal": "🟡 보통", "high": "🟠 높음", "critical": "🔴 심각"}

# 상태 → (라벨, 색). 접수 = 초기. 운영진 버튼으로 전이.
_STATUS = {
    "received": ("📥 접수", discord.Color.blurple()),
    "progress": ("🔧 확인중", discord.Color.orange()),
    "done":     ("✅ 완료", discord.Color.green()),
    "rejected": ("🚫 기각", discord.Color.light_grey()),
}

# 상태 버튼을 누를 수 있는 운영 권한.
_STAFF_KEYS = ("admin", "support", "rpg_manager", "poromon_manager")


def _set_status(embed: discord.Embed, status_key: str) -> discord.Embed:
    """임베드의 '상태' 필드 + 색을 갱신(필드 없으면 추가)."""
    label, color = _STATUS[status_key]
    embed.color = color
    for i, f in enumerate(embed.fields):
        if f.name == "상태":
            embed.set_field_at(i, name="상태", value=label, inline=True)
            return embed
    embed.add_field(name="상태", value=label, inline=True)
    return embed


class BugStatusView(discord.ui.View):
    """버그제보 임베드 하단 운영 상태 버튼(영구 뷰)."""

    def __init__(self) -> None:
        super().__init__(timeout=None)
        for key, (label, _), cid in (
            ("progress", _STATUS["progress"], "bug_status_progress"),
            ("done", _STATUS["done"], "bug_status_done"),
            ("rejected", _STATUS["rejected"], "bug_status_rejected"),
        ):
            btn = discord.ui.Button(
                label=label, custom_id=cid,
                style=discord.ButtonStyle.secondary,
            )
            btn.callback = self._make_cb(key)
            self.add_item(btn)

    def _make_cb(self, status_key: str):
        async def cb(interaction: discord.Interaction) -> None:
            await self._apply(interaction, status_key)
        return cb

    async def _apply(self, interaction: discord.Interaction, status_key: str) -> None:
        member = interaction.user
        if not isinstance(member, discord.Member) or not permissions.member_has_permission(
            member, *_STAFF_KEYS
        ):
            await interaction.response.send_message(
                "운영진만 상태를 변경할 수 있습니다.", ephemeral=True
            )
            return
        msg = interaction.message
        if msg is None or not msg.embeds:
            await interaction.response.send_message("대상 제보를 찾을 수 없습니다.", ephemeral=True)
            return
        embed = _set_status(msg.embeds[0], status_key)
        await interaction.response.edit_message(embed=embed, view=self)
        await self._dm_reporter(interaction, embed, status_key)

    async def _dm_reporter(
        self, interaction: discord.Interaction, embed: discord.Embed, status_key: str
    ) -> None:
        if embed.footer is None or not embed.footer.text:
            return
        m = _REPORTER_RE.search(embed.footer.text)
        if not m:
            return
        reporter = interaction.client.get_user(int(m.group(1)))
        if reporter is None:
            return
        label, _ = _STATUS[status_key]
        try:
            await reporter.send(
                f"📨 제보하신 버그의 상태가 **{label}** 로 변경되었습니다.\n"
                f"> {embed.title}"
            )
        except discord.HTTPException:
            pass  # DM 차단 등 — 무시


class BugModal(discord.ui.Modal):
    제목 = discord.ui.TextInput(label="제목", max_length=100)
    재현 = discord.ui.TextInput(
        label="재현 단계", style=discord.TextStyle.paragraph, max_length=1000,
        placeholder="1) … 2) … 3) …",
    )
    기대 = discord.ui.TextInput(
        label="기대한 결과", style=discord.TextStyle.paragraph, max_length=500
    )
    실제 = discord.ui.TextInput(
        label="실제 결과", style=discord.TextStyle.paragraph, max_length=500
    )

    def __init__(self, cog: "BugReportCog", *, domain: str, severity: str) -> None:
        super().__init__(title=f"버그제보 — {_DOMAIN_LABEL[domain]}")
        self.cog = cog
        self.domain = domain
        self.severity = severity

    async def on_submit(self, interaction: discord.Interaction) -> None:
        channel = interaction.client.get_channel(config.CHANNEL_BUGREPORT_ID)
        if not isinstance(channel, discord.TextChannel):
            await interaction.response.send_message(
                "버그제보 채널이 설정되지 않았습니다. 운영진에게 알려주세요.", ephemeral=True
            )
            return

        embed = discord.Embed(
            title=f"🐞 [{_DOMAIN_LABEL[self.domain]}] {self.제목.value}",
            color=_STATUS["received"][1],
            timestamp=discord.utils.utcnow(),
        )
        embed.add_field(name="심각도", value=_SEVERITY_LABEL[self.severity], inline=True)
        embed.add_field(name="대상", value=_DOMAIN_LABEL[self.domain], inline=True)
        embed.add_field(name="상태", value=_STATUS["received"][0], inline=True)
        embed.add_field(name="재현 단계", value=self.재현.value[:1024], inline=False)
        embed.add_field(name="기대한 결과", value=self.기대.value[:1024], inline=False)
        embed.add_field(name="실제 결과", value=self.실제.value[:1024], inline=False)
        if self.domain in _GAME_DOMAINS:
            embed.add_field(
                name="ℹ 참고",
                value="게임 서버 연동 전 — 채널 접수입니다(추후 게임 DB 이관 예정).",
                inline=False,
            )
        embed.set_author(
            name=str(interaction.user),
            icon_url=interaction.user.display_avatar.url,
        )
        embed.set_footer(text=f"BUG · reporter:{interaction.user.id}")

        try:
            await channel.send(embed=embed, view=BugStatusView())
        except discord.Forbidden:
            await interaction.response.send_message(
                "봇 권한 부족으로 버그제보 채널에 게시할 수 없습니다.", ephemeral=True
            )
            return

        try:
            await interaction.user.send(
                f"📨 버그제보가 접수되었습니다. 운영진이 확인 후 처리합니다.\n> {embed.title}"
            )
        except discord.HTTPException:
            pass
        await interaction.response.send_message(
            f"✅ 버그제보가 접수되었습니다: {channel.mention}", ephemeral=True
        )


class BugReportCog(commands.Cog):
    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot

    @commands.Cog.listener()
    async def on_ready(self) -> None:
        self.bot.add_view(BugStatusView())  # 재시작 후 상태 버튼 동작

    @app_commands.command(name="버그제보", description="버그를 제보합니다(대상·심각도 선택).")
    @app_commands.describe(대상="버그가 발생한 서버/영역", 심각도="버그 심각도")
    @app_commands.choices(대상=_DOMAIN_CHOICES, 심각도=_SEVERITY_CHOICES)
    async def bug_report(
        self,
        interaction: discord.Interaction,
        대상: app_commands.Choice[str],
        심각도: app_commands.Choice[str],
    ) -> None:
        if not config.CHANNEL_BUGREPORT_ID:
            await interaction.response.send_message(
                "버그제보 기능이 아직 설정되지 않았습니다. 운영진 문의(`/문의`)를 이용해주세요.",
                ephemeral=True,
            )
            return
        await interaction.response.send_modal(
            BugModal(self, domain=대상.value, severity=심각도.value)
        )


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(BugReportCog(bot))
