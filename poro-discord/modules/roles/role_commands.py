"""
역할 선택 명령어 — /클래스선택, /알림설정.
"""
from __future__ import annotations

import discord
from discord import app_commands
from discord.ext import commands

from core import config

CLASS_DISPLAY: dict[str, str] = {
    "sword":    "검사",
    "axe":      "도끼전사",
    "spear":    "창기사",
    "crossbow": "석궁사",
    "scythe":   "낫사냥꾼",
    "staff":    "지팡이술사",
}

NOTIFY_DISPLAY: dict[str, str] = {
    "필드보스알림": "필드보스 알림",
    "시즌보스알림": "시즌보스 알림",
    "월드보스알림": "월드보스 알림",
    "포로몬알림":   "포로몬 알림",
    "이벤트알림":   "이벤트 알림",
    "점검알림":     "점검 알림",
    "업데이트알림": "업데이트 알림",
}


# ─── 클래스 선택 ─────────────────────────────────────────────────

class ClassButton(discord.ui.Button):
    def __init__(self, class_key: str, role_id: int, is_current: bool) -> None:
        label = ("✅ " if is_current else "") + CLASS_DISPLAY.get(class_key, class_key)
        super().__init__(
            label=label,
            style=discord.ButtonStyle.success if is_current else discord.ButtonStyle.secondary,
            custom_id=f"class_{class_key}",
        )
        self.class_key = class_key
        self.role_id = role_id

    async def callback(self, interaction: discord.Interaction) -> None:
        member = interaction.guild.get_member(interaction.user.id)
        if member is None:
            await interaction.response.send_message("오류: 멤버를 찾을 수 없습니다.", ephemeral=True)
            return

        roles_to_remove = [
            member.guild.get_role(rid)
            for rid in config.CLASS_ROLE_IDS.values()
            if rid != 0
        ]
        roles_to_remove = [r for r in roles_to_remove if r and r in member.roles]
        if roles_to_remove:
            await member.remove_roles(*roles_to_remove, reason="클래스 변경")

        new_role = member.guild.get_role(self.role_id)
        if new_role:
            await member.add_roles(new_role, reason="클래스 선택")

        await interaction.response.edit_message(view=ClassView(member))


class ClassView(discord.ui.View):
    def __init__(self, member: discord.Member) -> None:
        super().__init__(timeout=120)
        member_role_ids = {r.id for r in member.roles}
        for key, role_id in config.CLASS_ROLE_IDS.items():
            if role_id == 0:
                continue
            self.add_item(ClassButton(key, role_id, role_id in member_role_ids))


# ─── 알림 설정 ────────────────────────────────────────────────────

class NotifyButton(discord.ui.Button):
    def __init__(self, key: str, role_id: int, is_subscribed: bool) -> None:
        label = ("🔔 " if is_subscribed else "🔕 ") + NOTIFY_DISPLAY.get(key, key)
        super().__init__(
            label=label,
            style=discord.ButtonStyle.success if is_subscribed else discord.ButtonStyle.secondary,
            custom_id=f"notify_{key}",
        )
        self.key = key
        self.role_id = role_id
        self.is_subscribed = is_subscribed

    async def callback(self, interaction: discord.Interaction) -> None:
        member = interaction.guild.get_member(interaction.user.id)
        if member is None:
            await interaction.response.send_message("오류: 멤버를 찾을 수 없습니다.", ephemeral=True)
            return
        role = member.guild.get_role(self.role_id)
        if role is None:
            await interaction.response.send_message("역할을 찾을 수 없습니다.", ephemeral=True)
            return

        if self.is_subscribed:
            await member.remove_roles(role, reason="알림 구독 해제")
        else:
            await member.add_roles(role, reason="알림 구독")

        # member.roles 캐시가 즉시 갱신되지 않을 수 있으므로 수동 토글
        member_role_ids = {r.id for r in member.roles}
        if self.is_subscribed:
            member_role_ids.discard(self.role_id)
        else:
            member_role_ids.add(self.role_id)

        await interaction.response.edit_message(view=NotifyView(member_role_ids))


class NotifyView(discord.ui.View):
    def __init__(self, member_role_ids: set[int]) -> None:
        super().__init__(timeout=120)
        for key, role_id in config.NOTIFY_ROLE_IDS.items():
            if role_id == 0:
                continue
            self.add_item(NotifyButton(key, role_id, role_id in member_role_ids))


# ─── Cog ─────────────────────────────────────────────────────────

class RoleCommandsCog(commands.Cog):

    @app_commands.command(name="클래스선택", description="플레이 클래스 역할을 선택합니다.")
    async def choose_class(self, interaction: discord.Interaction) -> None:
        member = interaction.guild.get_member(interaction.user.id)
        if member is None:
            await interaction.response.send_message("오류: 서버 멤버 정보를 찾을 수 없습니다.", ephemeral=True)
            return
        view = ClassView(member)
        if not view.children:
            await interaction.response.send_message(
                "클래스 역할이 설정되지 않았습니다. 관리자에게 문의하세요.", ephemeral=True
            )
            return
        await interaction.response.send_message("클래스를 선택하세요:", view=view, ephemeral=True)

    @app_commands.command(name="알림설정", description="보스 출현 알림 구독을 설정합니다.")
    async def set_notify(self, interaction: discord.Interaction) -> None:
        member = interaction.guild.get_member(interaction.user.id)
        if member is None:
            await interaction.response.send_message("오류: 서버 멤버 정보를 찾을 수 없습니다.", ephemeral=True)
            return
        member_role_ids = {r.id for r in member.roles}
        view = NotifyView(member_role_ids)
        if not view.children:
            await interaction.response.send_message(
                "알림 역할이 설정되지 않았습니다. 관리자에게 문의하세요.", ephemeral=True
            )
            return
        await interaction.response.send_message("알림 구독을 설정하세요:", view=view, ephemeral=True)


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(RoleCommandsCog(bot))
