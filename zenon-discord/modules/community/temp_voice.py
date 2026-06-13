"""
임시 음성채널 (T13) — community_level.md §8.

"입장 시 생성" 패턴: 유저가 **허브 채널**(`➕ 음성방 만들기`)에 입장하면 봇이 같은
카테고리에 개인 음성방을 만들고 이동시킨다. 방이 **비면 자동 삭제**(orphan 정리).

전역 단일 active 모델 → 허브는 active 서버의 지원·음성 카테고리에 템플릿이 생성한
`➕ 음성방 만들기` 하나(카테고리별 다중 허브, 2026-06-09 결정). 식별 = 채널 이름 매칭.

런타임 추적(별도 DB 불필요): 생성한 방 ID 집합 + 이름 프리픽스. 재시작 시 빈 임시방 청소.
`voice_states`(비특권, default intents) 만 사용 — 메시지 내용 미열람.

봇 권한 요구(배포 T8): Manage Channels · Move Members.
"""
from __future__ import annotations

import logging

import discord
from discord.ext import commands

log = logging.getLogger(__name__)

# 템플릿이 만든 허브 채널 이름(modules/server_lifecycle/templates.py 와 일치).
HUB_NAME = "➕ 음성방 만들기"
# 임시방 이름 프리픽스(식별·청소용).
TEMP_PREFIX = "🔊 "
_REASON = "임시 음성방"


def _is_temp(channel: discord.abc.GuildChannel) -> bool:
    return isinstance(channel, discord.VoiceChannel) and channel.name.startswith(TEMP_PREFIX)


class TempVoiceCog(commands.Cog):
    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot
        self._temp_ids: set[int] = set()

    @commands.Cog.listener()
    async def on_ready(self) -> None:
        # 재시작 후 남은 빈 임시방 청소 + 추적 복구.
        for guild in self.bot.guilds:
            for ch in guild.voice_channels:
                if _is_temp(ch):
                    if not ch.members:
                        try:
                            await ch.delete(reason=f"{_REASON} — 재시작 orphan 정리")
                        except discord.HTTPException:
                            pass
                    else:
                        self._temp_ids.add(ch.id)

    @commands.Cog.listener()
    async def on_voice_state_update(
        self,
        member: discord.Member,
        before: discord.VoiceState,
        after: discord.VoiceState,
    ) -> None:
        if member.bot:
            return
        # 허브 입장 → 개인 방 생성·이동.
        if (
            after.channel is not None
            and after.channel is not before.channel
            and isinstance(after.channel, discord.VoiceChannel)
            and after.channel.name == HUB_NAME
        ):
            await self._create_room(member, after.channel)
        # 임시방에서 나감 → 비었으면 삭제.
        if before.channel is not None and before.channel is not after.channel:
            await self._cleanup_if_empty(before.channel)

    async def _create_room(self, member: discord.Member, hub: discord.VoiceChannel) -> None:
        category = hub.category
        try:
            room = await member.guild.create_voice_channel(
                f"{TEMP_PREFIX}{member.display_name}",
                category=category,
                reason=f"{_REASON} 생성 — {member}",
            )
        except discord.Forbidden:
            log.warning("임시 음성방 생성 권한 부족(Manage Channels) — member=%s", member.id)
            return
        except discord.HTTPException:
            log.warning("임시 음성방 생성 실패 — member=%s", member.id, exc_info=True)
            return
        self._temp_ids.add(room.id)
        try:
            await member.move_to(room, reason=_REASON)
        except discord.HTTPException:
            # 이동 실패(이미 나감 등) → 방이 비면 다음 청소에서 제거.
            await self._cleanup_if_empty(room)

    async def _cleanup_if_empty(self, channel: discord.abc.GuildChannel) -> None:
        if not isinstance(channel, discord.VoiceChannel):
            return
        if channel.id not in self._temp_ids and not _is_temp(channel):
            return
        if channel.members:
            return
        try:
            await channel.delete(reason=f"{_REASON} — 비어서 삭제")
        except discord.HTTPException:
            pass
        self._temp_ids.discard(channel.id)


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(TempVoiceCog(bot))
