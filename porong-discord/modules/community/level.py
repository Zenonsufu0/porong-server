"""
커뮤니티 레벨 (T13) — community_level.md §1~4.

채팅·음성 활동 XP → 레벨. 길드 전역(도메인 무관). **메시지 내용 미열람**(작성 이벤트만).
저장 = core.community(community_xp). 칭호(/칭호)·XP 보정(/XP지급)은 다음 슬라이스.

어뷰징 방지: 채팅 쿨다운(메모리 1차 판정 후 DB write — 핫패스 방어) · 음성 제외
(self-mute/deaf·AFK·혼자) · 봇·제외 채널 제외.
"""
from __future__ import annotations

import logging
import time

import discord
from discord import app_commands
from discord.ext import commands, tasks

from core import community, config
from core.config import VOICE_TICK_SEC

log = logging.getLogger(__name__)


class CommunityLevelCog(commands.Cog):
    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot
        self._chat_cooldown: dict[int, int] = {}  # user_id → last XP 지급 epoch(메모리)

    @property
    def db(self):
        return self.bot.db  # type: ignore[attr-defined]

    async def cog_load(self) -> None:
        self.voice_tick.start()

    async def cog_unload(self) -> None:
        self.voice_tick.cancel()

    # ─── 채팅 XP ──────────────────────────────────────────────────
    @commands.Cog.listener()
    async def on_message(self, message: discord.Message) -> None:
        if message.author.bot or message.guild is None:
            return
        if message.guild.id != config.GUILD_ID:
            return
        if message.channel.id in config.XP_EXCLUDE_CHANNEL_IDS:
            return
        uid = message.author.id
        now = int(time.time())
        # 쿨다운 메모리 1차 판정 → 통과 시에만 DB write(핫패스 방어, §12.2).
        if now - self._chat_cooldown.get(uid, 0) < config.CHAT_XP_COOLDOWN_SEC:
            return
        self._chat_cooldown[uid] = now
        _, new_level, leveled_up = await community.add_xp(
            self.db, uid, config.CHAT_XP_PER_MSG, set_last_message_ts=now
        )
        if leveled_up:
            await self._announce_levelup(message.guild, message.author, new_level)

    # ─── 음성 XP (주기 tick) ──────────────────────────────────────
    @tasks.loop(seconds=VOICE_TICK_SEC)
    async def voice_tick(self) -> None:
        guild = self.bot.get_guild(config.GUILD_ID)
        if guild is None:
            return
        for vc in guild.voice_channels:
            if config.AFK_CHANNEL_ID and vc.id == config.AFK_CHANNEL_ID:
                continue
            humans = [m for m in vc.members if not m.bot]
            if len(humans) < 2:  # 혼자(또는 빈 방) 제외
                continue
            for m in humans:
                vs = m.voice
                if vs is None or vs.self_mute or vs.self_deaf:
                    continue
                _, new_level, leveled_up = await community.add_xp(
                    self.db, m.id, config.VOICE_XP_PER_TICK,
                    add_voice_seconds=config.VOICE_TICK_SEC,
                )
                if leveled_up:
                    await self._announce_levelup(guild, m, new_level)

    @voice_tick.before_loop
    async def _before(self) -> None:
        await self.bot.wait_until_ready()

    # ─── 레벨업 알림 ──────────────────────────────────────────────
    async def _announce_levelup(
        self, guild: discord.Guild, user: discord.abc.User, level: int
    ) -> None:
        if not config.CHANNEL_LEVELUP_ID:
            return
        channel = guild.get_channel(config.CHANNEL_LEVELUP_ID)
        if not isinstance(channel, (discord.TextChannel, discord.Thread)):
            return
        try:
            await channel.send(
                f"🎉 {user.mention} 님이 **레벨 {level}** 을 달성했습니다!",
                allowed_mentions=discord.AllowedMentions(users=True),
            )
        except discord.HTTPException:
            pass

    # ─── 명령어 ───────────────────────────────────────────────────
    @app_commands.command(name="레벨", description="커뮤니티 레벨·XP를 확인합니다.")
    @app_commands.describe(유저="조회할 유저(생략 시 본인)")
    async def level_cmd(
        self, interaction: discord.Interaction, 유저: discord.Member | None = None
    ) -> None:
        target = 유저 or interaction.user
        row = await community.get_xp(self.db, target.id)
        xp = int(row["xp"]) if row else 0
        level = int(row["level"]) if row else 0
        cur = community.total_xp_for_level(level)
        nxt = community.total_xp_for_level(level + 1)
        rank = await community.rank_of(self.db, target.id)

        embed = discord.Embed(
            title=f"{target.display_name} 의 커뮤니티 레벨", color=discord.Color.blurple()
        )
        embed.add_field(name="레벨", value=f"**{level}**", inline=True)
        embed.add_field(name="순위", value=f"{rank}위" if rank else "—", inline=True)
        embed.add_field(name="누적 XP", value=f"{xp:,}", inline=True)
        embed.add_field(
            name="다음 레벨까지",
            value=f"{xp - cur:,} / {nxt - cur:,} XP",
            inline=False,
        )
        if isinstance(target, discord.Member):
            embed.set_thumbnail(url=target.display_avatar.url)
        await interaction.response.send_message(embed=embed)

    @app_commands.command(name="리더보드", description="커뮤니티 레벨 상위 랭킹을 확인합니다.")
    async def leaderboard_cmd(self, interaction: discord.Interaction) -> None:
        rows = await community.top(self.db, 10)
        if not rows:
            await interaction.response.send_message("아직 XP 기록이 없습니다.", ephemeral=True)
            return
        medals = {1: "🥇", 2: "🥈", 3: "🥉"}
        lines = [
            f"{medals.get(i, f'`{i}.`')} <@{r['discord_user_id']}> — Lv.{r['level']} ({int(r['xp']):,} XP)"
            for i, r in enumerate(rows, 1)
        ]
        embed = discord.Embed(
            title="🏆 커뮤니티 리더보드 (상위 10)",
            description="\n".join(lines),
            color=discord.Color.gold(),
        )
        my_rank = await community.rank_of(self.db, interaction.user.id)
        if my_rank:
            embed.set_footer(text=f"내 순위: {my_rank}위")
        await interaction.response.send_message(
            embed=embed, allowed_mentions=discord.AllowedMentions.none()
        )


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(CommunityLevelCog(bot))
