"""
역할 부여 폴링 — 5초마다 GET /auth/role-queue 를 확인하고
새 항목이 있으면 "인증 유저" 역할을 부여한다.
"""
from __future__ import annotations

import discord
from discord.ext import commands, tasks

import config
from api_client import EmpireApiClient


class RolePollCog(commands.Cog):
    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot
        self.api = EmpireApiClient()

    @commands.Cog.listener()
    async def on_ready(self) -> None:
        if not self.poll_role_queue.is_running():
            self.poll_role_queue.start()

    @tasks.loop(seconds=5)
    async def poll_role_queue(self) -> None:
        try:
            entries = await self.api.poll_role_queue()
        except Exception:
            return  # 서버 오프라인 — 무시

        if not entries:
            return

        guild = self.bot.get_guild(config.GUILD_ID)
        if guild is None:
            return

        verified_role  = guild.get_role(config.ROLE_인증유저_ID)
        pending_role   = guild.get_role(config.ROLE_접속대기_ID)
        unverified_role = (
            guild.get_role(config.ROLE_미인증_ID) if config.ROLE_미인증_ID else None
        )

        for entry in entries:
            discord_user_id: str = entry.get("discord_user_id", "")
            minecraft_nick: str = entry.get("minecraft_nick", "")

            member = guild.get_member(int(discord_user_id)) if discord_user_id.isdigit() else None
            if member is None:
                # 캐시에 없으면 fetch 시도
                try:
                    member = await guild.fetch_member(int(discord_user_id))
                except (discord.NotFound, discord.HTTPException, ValueError):
                    pass

            if member:
                roles_to_add = [verified_role] if verified_role else []
                roles_to_remove = [
                    r for r in (pending_role, unverified_role)
                    if r and r in member.roles
                ]
                try:
                    if roles_to_add:
                        await member.add_roles(*roles_to_add, reason="마인크래프트 연동 완료")
                    if roles_to_remove:
                        await member.remove_roles(*roles_to_remove, reason="인증 유저 역할 전환")
                except discord.Forbidden:
                    pass

                # DM 발송
                try:
                    await member.send(
                        f"연동 완료! 마인크래프트 닉네임 **{minecraft_nick}** 으로 인증되었습니다.\n"
                        "이제 서버에서 플레이할 수 있습니다."
                    )
                except (discord.Forbidden, discord.HTTPException):
                    pass

            # acknowledge — 성공/실패 무관하게 큐에서 제거 요청
            try:
                await self.api.acknowledge_role_granted(discord_user_id)
            except Exception:
                pass

    @poll_role_queue.before_loop
    async def before_poll(self) -> None:
        await self.bot.wait_until_ready()

    async def cog_unload(self) -> None:
        self.poll_role_queue.cancel()
        await self.api.close()


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(RolePollCog(bot))
