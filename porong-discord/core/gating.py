"""
게이팅 3층 헬퍼 (server_lifecycle.md §4, task.md §10).

도메인 명령 실행 시 순서대로 통과해야 동작:
  1. 접근역할(가시성)  — 디스코드 측(운영자 Integrations). 봇 코드 아님.
  2. 카테고리 가드      — requires_category(domain): 호출 채널이 그 domain 의 active
                          시즌 카테고리인지.
  3. 서버 상태          — requires_server_active(domain): 그 domain 의 active 시즌이
                          있는지(prep/ended/없음 = 거부).

판정부는 순수 함수로 분리(§12.4 — discord 없이 단위 테스트). 데코레이터는 순수
함수에 DB 조회만 얹는다. 이 데코레이터들은 도메인 명령(poromon/rpg 조회 등)에
부착하기 위한 인프라다 — 도메인 명령이 생기면 거기서 사용한다.
"""
from __future__ import annotations

import discord
from discord import app_commands

from core import servers


# ─── 순수 판정 함수 (단위 테스트 대상) ──────────────────────────────

def category_matches(channel_category_id: int | None, server_category_id: int | None) -> bool:
    """호출 채널의 카테고리가 서버 카테고리와 일치하는가(단수 — 수동연결용).

    둘 중 하나라도 미지정(None/0)이면 False(가드 통과 불가 — 보수적).
    """
    if not channel_category_id or not server_category_id:
        return False
    return channel_category_id == server_category_id


def category_in(channel_category_id: int | None, server_category_ids: set[int]) -> bool:
    """호출 채널의 카테고리가 서버의 카테고리 집합(다중)에 속하는가.

    채널 카테고리 미지정(None/0)이거나 집합이 비면 False(보수적).
    """
    if not channel_category_id or not server_category_ids:
        return False
    return channel_category_id in server_category_ids


def is_active(server_row) -> bool:
    """레지스트리 행이 active 상태인가(None 안전)."""
    return server_row is not None and server_row["state"] == "active"


# ─── app_commands 데코레이터 ────────────────────────────────────────

def _channel_category_id(interaction: discord.Interaction) -> int | None:
    ch = interaction.channel
    # 스레드면 부모 채널의 카테고리 사용
    parent = getattr(ch, "parent", None)
    if parent is not None and getattr(ch, "category_id", None) is None:
        return getattr(parent, "category_id", None)
    return getattr(ch, "category_id", None)


def requires_server_active(domain: str):
    """domain 의 active 시즌이 없으면(prep/ended/없음) 거부."""
    async def predicate(interaction: discord.Interaction) -> bool:
        db = interaction.client.db  # type: ignore[attr-defined]
        row = await servers.get_active(db, domain)
        if not is_active(row):
            raise app_commands.CheckFailure(f"`{domain}` 서버가 현재 활성 상태가 아닙니다.")
        return True

    return app_commands.check(predicate)


def requires_category(domain: str):
    """호출 채널이 domain 의 active 시즌 카테고리(다중) 중 하나가 아니면 거부."""
    async def predicate(interaction: discord.Interaction) -> bool:
        db = interaction.client.db  # type: ignore[attr-defined]
        category_ids = await servers.get_active_category_ids(db, domain)
        if not category_in(_channel_category_id(interaction), category_ids):
            raise app_commands.CheckFailure("이 채널에서는 사용할 수 없는 명령입니다.")
        return True

    return app_commands.check(predicate)
