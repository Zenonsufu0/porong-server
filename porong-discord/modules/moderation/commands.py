"""
모더레이션 — 경고계 (T15) — moderation.md §1 구현(1차: 경고만).

이번 슬라이스 = 디스코드 상태를 바꾸지 않는 **기록계** 명령만:
  - 🟢 /경고        (유저, 사유)  — warnings 추가 + DM(베스트에포트) + 누적 활성 경고수 안내
  - 🟢 /경고목록    (유저)        — 경고 이력(활성/철회) 조회
  - 🟢 /경고취소    (경고_id)     — warnings.active=0(철회, 이력 보존)

상태변경 제재(/타임아웃·/추방·/차단)는 보안 영향이 커 다음 슬라이스(명시 승인 시).

공통 규약:
  - 권한: @requires_permission("admin", "support"). Owner 항상 통과.
  - 대상 보호(§1b): 봇·자기 자신·operator와 같거나 상위 권한자는 거부.
  - 적재: core.mod_log.record() 만 사용(raw INSERT 금지). 응답 ephemeral.
"""
from __future__ import annotations

import logging

import discord
from discord import app_commands
from discord.ext import commands

from core import mod_log, permissions, warnings
from core.permissions import requires_permission

log = logging.getLogger(__name__)

# 경고/철회를 다룰 수 있는 권한 키
_MOD_ROLES = ("admin", "support")


def _target_reject_reason(
    operator: discord.Member, target: discord.Member, bot_user: discord.abc.User | None
) -> str | None:
    """제재 대상 보호 판정(§1b). 거부 사유 문자열, 통과면 None."""
    if target.bot:
        return "봇은 제재 대상이 아닙니다."
    if bot_user is not None and target.id == bot_user.id:
        return "봇은 제재 대상이 아닙니다."
    if target.id == operator.id:
        return "자기 자신은 제재할 수 없습니다."
    t_rank = permissions.permission_rank(target)
    if t_rank and t_rank >= permissions.permission_rank(operator):
        return "대상이 본인과 같거나 상위 권한자입니다. 제재할 수 없습니다."
    return None


class ModerationCog(commands.Cog):
    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot

    @property
    def db(self):
        return self.bot.db  # type: ignore[attr-defined]

    # ─── /경고 ────────────────────────────────────────────────────
    @app_commands.command(name="경고", description="유저에게 경고를 부여합니다(기록 + DM 통보).")
    @app_commands.describe(유저="경고 대상", 사유="경고 사유")
    @requires_permission(*_MOD_ROLES)
    async def warn(
        self, interaction: discord.Interaction, 유저: discord.Member, 사유: str
    ) -> None:
        operator = interaction.user
        reject = _target_reject_reason(operator, 유저, self.bot.user)  # type: ignore[arg-type]
        if reject:
            await interaction.response.send_message(reject, ephemeral=True)
            return

        wid = await warnings.add_warning(
            self.db, user_id=유저.id, reason=사유, operator_id=operator.id
        )
        active = await warnings.count_active(self.db, 유저.id)

        # DM 통보(베스트에포트 — 차단 시 무시)
        dm_ok = await self._dm(
            유저,
            f"⚠ 경고를 받았습니다.\n**사유:** {사유}\n현재 활성 경고: **{active}회**",
        )

        await mod_log.record(
            self.bot,
            action="warn",
            operator_id=operator.id,
            target_id=유저.id,
            reason=사유,
            detail={"warning_id": wid, "active_count": active},
        )

        dm_note = "" if dm_ok else " (DM 전송 실패 — 유저가 DM 차단)"
        await interaction.response.send_message(
            f"✅ {유저.mention} 경고 등록(`#{wid}`). 활성 경고 **{active}회**.{dm_note}",
            ephemeral=True,
            allowed_mentions=discord.AllowedMentions.none(),
        )

    # ─── /경고목록 ────────────────────────────────────────────────
    @app_commands.command(name="경고목록", description="유저의 경고 이력(활성/철회)을 조회합니다.")
    @app_commands.describe(유저="조회 대상")
    @requires_permission(*_MOD_ROLES)
    async def warn_list(
        self, interaction: discord.Interaction, 유저: discord.Member
    ) -> None:
        rows = await warnings.list_warnings(self.db, 유저.id)
        if not rows:
            await interaction.response.send_message(
                f"{유저.mention} 의 경고 이력이 없습니다.",
                ephemeral=True,
                allowed_mentions=discord.AllowedMentions.none(),
            )
            return
        active_n = sum(1 for r in rows if r["active"])
        lines = []
        for r in rows:
            mark = "🟥" if r["active"] else "⬜"
            ts = f"<t:{r['created_at']}:d>"
            lines.append(
                f"{mark} `#{r['id']}` {ts} — {r['reason'] or '(사유 없음)'} "
                f"· 처리 <@{r['operator_id']}>"
            )
        embed = discord.Embed(
            title=f"경고 이력 — {유저.display_name}",
            description="\n".join(lines)[:4000],
            color=discord.Color.orange(),
        )
        embed.set_footer(text=f"전체 {len(rows)}건 · 활성 {active_n}건")
        await interaction.response.send_message(
            embed=embed, ephemeral=True, allowed_mentions=discord.AllowedMentions.none()
        )

    # ─── /경고취소 ────────────────────────────────────────────────
    @app_commands.command(name="경고취소", description="경고를 철회합니다(이력은 보존).")
    @app_commands.describe(경고_id="철회할 경고 ID(/경고목록 의 #번호)")
    @requires_permission(*_MOD_ROLES)
    async def warn_revoke(self, interaction: discord.Interaction, 경고_id: int) -> None:
        row = await warnings.get_warning(self.db, 경고_id)
        if row is None:
            await interaction.response.send_message(
                f"경고 `#{경고_id}` 를 찾을 수 없습니다.", ephemeral=True
            )
            return
        if not row["active"]:
            await interaction.response.send_message(
                f"경고 `#{경고_id}` 는 이미 철회된 상태입니다.", ephemeral=True
            )
            return
        await warnings.revoke_warning(self.db, 경고_id)
        await mod_log.record(
            self.bot,
            action="warn_revoke",
            operator_id=interaction.user.id,
            target_id=row["discord_user_id"],
            detail={"warning_id": 경고_id},
        )
        await interaction.response.send_message(
            f"↩ 경고 `#{경고_id}` 철회 완료(<@{row['discord_user_id']}>).",
            ephemeral=True,
            allowed_mentions=discord.AllowedMentions.none(),
        )

    # ─── 내부 헬퍼 ────────────────────────────────────────────────
    @staticmethod
    async def _dm(member: discord.Member, content: str) -> bool:
        """대상에게 DM(베스트에포트). 성공 여부 반환(차단/실패=False)."""
        try:
            await member.send(content)
            return True
        except discord.HTTPException:
            return False


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(ModerationCog(bot))
