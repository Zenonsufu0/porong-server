"""
알림 embed 빌더 카탈로그 (T1) — notifications.md ②③, integration_contract B-1.

`(domain, kind) → data dict → discord.Embed` 빌더를 notifier 에 등록한다. 라우팅·전송은
core/notifier 가 담당하고, 여기선 "이벤트 의미 → 임베드" 표현만 책임진다(도메인 격리).

data 스키마(B-1)는 게임서버 계약이라 누락에 관대하게 .get 으로 읽는다(빌더가 던지면
notifier 가 graceful 처리). 빌더 추가 = 라우팅(_ROUTES) 항목과 짝맞춰 확장.
"""
from __future__ import annotations

import discord
from discord.ext import commands

from core import notifier


def _field_boss_pre(data: dict) -> discord.Embed:
    name = data.get("field_name", "필드보스")
    minutes = data.get("minutes", "?")
    return discord.Embed(
        title="⚔️ 필드보스 등장 임박",
        description=f"**{name}** 가 약 **{minutes}분** 후 등장합니다!",
        color=discord.Color.orange(),
    )


def _field_boss_spawn(data: dict) -> discord.Embed:
    name = data.get("field_name", "필드보스")
    return discord.Embed(
        title="⚔️ 필드보스 등장!",
        description=f"**{name}** 가 지금 등장했습니다. 전투 준비!",
        color=discord.Color.red(),
    )


def _season_boss_recruit(data: dict) -> discord.Embed:
    embed = discord.Embed(
        title="🐉 시즌보스 모집",
        description=data.get("info", "시즌보스 토벌대를 모집합니다."),
        color=discord.Color.dark_purple(),
    )
    if data.get("boss_name"):
        embed.add_field(name="보스", value=data["boss_name"], inline=True)
    if data.get("when"):
        embed.add_field(name="일정", value=data["when"], inline=True)
    return embed


def _world_boss(data: dict) -> discord.Embed:
    embed = discord.Embed(
        title="🌍 월드보스 출현",
        description=data.get("boss_name", "월드보스"),
        color=discord.Color.dark_red(),
    )
    if data.get("when"):
        embed.add_field(name="일정", value=data["when"], inline=True)
    return embed


def _maintenance(data: dict) -> discord.Embed:
    embed = discord.Embed(
        title="🛠 점검 안내",
        description=data.get("reason", "서버 점검이 예정되어 있습니다."),
        color=discord.Color.greyple(),
    )
    if data.get("start"):
        embed.add_field(name="시작", value=data["start"], inline=True)
    if data.get("eta"):
        embed.add_field(name="예상 소요", value=data["eta"], inline=True)
    return embed


def _update(data: dict) -> discord.Embed:
    embed = discord.Embed(
        title="🆕 업데이트",
        description=data.get("summary", "새로운 업데이트가 적용되었습니다."),
        color=discord.Color.green(),
    )
    if data.get("version"):
        embed.add_field(name="버전", value=data["version"], inline=True)
    return embed


def _event_start(data: dict) -> discord.Embed:
    embed = discord.Embed(
        title="🎉 이벤트 시작",
        description=data.get("info", ""),
        color=discord.Color.gold(),
    )
    if data.get("event_name"):
        embed.add_field(name="이벤트", value=data["event_name"], inline=True)
    return embed


def _event_end(data: dict) -> discord.Embed:
    embed = discord.Embed(
        title="🏁 이벤트 종료",
        description=data.get("info", ""),
        color=discord.Color.light_grey(),
    )
    if data.get("event_name"):
        embed.add_field(name="이벤트", value=data["event_name"], inline=True)
    return embed


def _poromon_event(data: dict) -> discord.Embed:
    return discord.Embed(
        title=f"🐾 {data.get('title', '포로몬 소식')}",
        description=data.get("info", ""),
        color=discord.Color.teal(),
    )


_BUILDERS = {
    ("rpg", "field_boss_pre"): _field_boss_pre,
    ("rpg", "field_boss_spawn"): _field_boss_spawn,
    ("rpg", "season_boss_recruit"): _season_boss_recruit,
    ("common", "world_boss"): _world_boss,
    ("common", "maintenance"): _maintenance,
    ("common", "update"): _update,
    ("common", "event_start"): _event_start,
    ("common", "event_end"): _event_end,
    ("poromon", "event"): _poromon_event,
}


async def setup(bot: commands.Bot) -> None:
    # Cog 없이 빌더만 등록(EXTENSIONS 로드 훅 재사용).
    for (domain, kind), builder in _BUILDERS.items():
        notifier.register_builder(domain, kind, builder)
