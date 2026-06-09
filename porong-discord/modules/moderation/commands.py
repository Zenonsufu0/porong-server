"""
모더레이션 (T15) — moderation.md §1 구현.

기록계(경고):
  - 🟢 /경고        (유저, 사유)  — warnings 추가 + DM(베스트에포트) + 누적 활성 경고수 안내
  - 🟢 /경고목록    (유저)        — 경고 이력(활성/철회) 조회
  - 🟢 /경고취소    (경고_id)     — warnings.active=0(철회, 이력 보존)

상태변경 제재(디스코드 멤버 상태 변경 — 사용자 명시 승인하 구현, 2026-06-09):
  - 🟢 /타임아웃     (유저, 기간, 단위, 사유) — 디스코드 timeout(최대 28일) + 사전 DM
  - 🟢 /타임아웃해제 (유저)                   — timeout 해제
  - 🟢 /추방         (유저, 사유)             — kick + 사전 DM
  - 🟢 /차단         (유저, 사유)             — ban + 사전 DM
  - 🟢 /차단해제     (유저_id)               — unban(대상은 길드에 없음 → id 입력)

공통 규약:
  - 권한: 경고·타임아웃 = admin·support / 추방·차단·차단해제 = admin. Owner 항상 통과.
  - 대상 보호(§1b): 봇·자기 자신·서버 소유자·operator와 같거나 상위 권한자 거부
    + 디스코드 역할 위계상 봇보다 상위면 거부(_bot_hierarchy_reject).
  - DM: 추방/차단은 길드 제거 전 발송(이후 공유 서버 없어 DM 불가). 모두 베스트에포트.
  - 적재: core.mod_log.record() 만 사용(raw INSERT 금지). 응답 ephemeral.
"""
from __future__ import annotations

import logging
from datetime import timedelta

import discord
from discord import app_commands
from discord.ext import commands

from core import mod_log, permissions, warnings
from core.permissions import requires_permission

# 타임아웃 단위 → 분 환산. 디스코드 timeout 상한 = 28일.
_UNIT_MINUTES = {"m": 1, "h": 60, "d": 1440}
_TIMEOUT_MAX_MINUTES = 28 * 1440

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
    if target.id == target.guild.owner_id:
        return "서버 소유자는 제재할 수 없습니다."
    t_rank = permissions.permission_rank(target)
    if t_rank and t_rank >= permissions.permission_rank(operator):
        return "대상이 본인과 같거나 상위 권한자입니다. 제재할 수 없습니다."
    return None


def _bot_hierarchy_reject(guild: discord.Guild, target: discord.Member) -> str | None:
    """디스코드 역할 위계상 봇이 대상에 조치 가능한지(§1b). 불가면 사유.

    봇의 최상위 역할이 대상보다 높아야 timeout/kick/ban 이 가능하다(디스코드 제약).
    """
    me = guild.me
    if me is None:
        return "봇 멤버 정보를 찾을 수 없습니다."
    if target.top_role >= me.top_role:
        return "대상의 역할이 봇과 같거나 높아 조치할 수 없습니다(봇 역할을 상위로 올려주세요)."
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

    # ─── /타임아웃 ────────────────────────────────────────────────
    @app_commands.command(name="타임아웃", description="유저를 일정 시간 타임아웃합니다(최대 28일).")
    @app_commands.describe(유저="대상", 기간="숫자", 단위="시간 단위", 사유="사유")
    @app_commands.choices(단위=[
        app_commands.Choice(name="분", value="m"),
        app_commands.Choice(name="시간", value="h"),
        app_commands.Choice(name="일", value="d"),
    ])
    @requires_permission("admin", "support")
    async def timeout(
        self,
        interaction: discord.Interaction,
        유저: discord.Member,
        기간: app_commands.Range[int, 1, 40320],
        단위: app_commands.Choice[str],
        사유: str,
    ) -> None:
        reject = self._guard(interaction, 유저)
        if reject:
            await interaction.response.send_message(reject, ephemeral=True)
            return
        minutes = 기간 * _UNIT_MINUTES[단위.value]
        if minutes > _TIMEOUT_MAX_MINUTES:
            await interaction.response.send_message(
                "타임아웃은 최대 28일까지만 가능합니다.", ephemeral=True
            )
            return

        # 사전 DM(베스트에포트) → 적용
        await self._dm(
            유저, f"⏲ {minutes}분 동안 타임아웃되었습니다.\n**사유:** {사유}"
        )
        try:
            await 유저.timeout(timedelta(minutes=minutes), reason=f"{interaction.user}: {사유}")
        except discord.Forbidden:
            await interaction.response.send_message(
                "봇 권한 부족(Moderate Members) 또는 위계 문제로 타임아웃 실패.", ephemeral=True
            )
            return
        except discord.HTTPException:
            await interaction.response.send_message("타임아웃 처리 중 오류가 발생했습니다.", ephemeral=True)
            return

        await mod_log.record(
            self.bot, action="timeout", operator_id=interaction.user.id,
            target_id=유저.id, reason=사유, detail={"minutes": minutes},
        )
        await interaction.response.send_message(
            f"⏲ {유저.mention} {minutes}분 타임아웃 완료. 사유: {사유}",
            ephemeral=True, allowed_mentions=discord.AllowedMentions.none(),
        )

    # ─── /타임아웃해제 ────────────────────────────────────────────
    @app_commands.command(name="타임아웃해제", description="유저의 타임아웃을 해제합니다.")
    @app_commands.describe(유저="대상")
    @requires_permission("admin", "support")
    async def timeout_clear(self, interaction: discord.Interaction, 유저: discord.Member) -> None:
        if 유저.timed_out_until is None:
            await interaction.response.send_message("해당 유저는 타임아웃 상태가 아닙니다.", ephemeral=True)
            return
        try:
            await 유저.timeout(None, reason=f"{interaction.user}: 타임아웃 해제")
        except discord.Forbidden:
            await interaction.response.send_message("봇 권한/위계 문제로 해제 실패.", ephemeral=True)
            return
        except discord.HTTPException:
            await interaction.response.send_message("처리 중 오류가 발생했습니다.", ephemeral=True)
            return
        await mod_log.record(
            self.bot, action="timeout_clear", operator_id=interaction.user.id, target_id=유저.id,
        )
        await interaction.response.send_message(
            f"⏲ {유저.mention} 타임아웃 해제 완료.",
            ephemeral=True, allowed_mentions=discord.AllowedMentions.none(),
        )

    # ─── /추방 ────────────────────────────────────────────────────
    @app_commands.command(name="추방", description="유저를 서버에서 추방(kick)합니다.")
    @app_commands.describe(유저="대상", 사유="사유")
    @requires_permission("admin")
    async def kick(self, interaction: discord.Interaction, 유저: discord.Member, 사유: str) -> None:
        reject = self._guard(interaction, 유저)
        if reject:
            await interaction.response.send_message(reject, ephemeral=True)
            return
        # 추방 전 DM(이후 공유 서버 없어 DM 불가)
        await self._dm(유저, f"👢 서버에서 추방되었습니다.\n**사유:** {사유}")
        try:
            await 유저.kick(reason=f"{interaction.user}: {사유}")
        except discord.Forbidden:
            await interaction.response.send_message(
                "봇 권한 부족(Kick Members) 또는 위계 문제로 추방 실패.", ephemeral=True
            )
            return
        except discord.HTTPException:
            await interaction.response.send_message("추방 처리 중 오류가 발생했습니다.", ephemeral=True)
            return
        await mod_log.record(
            self.bot, action="kick", operator_id=interaction.user.id, target_id=유저.id, reason=사유,
        )
        await interaction.response.send_message(
            f"👢 {유저} 추방 완료. 사유: {사유}", ephemeral=True,
        )

    # ─── /차단 ────────────────────────────────────────────────────
    @app_commands.command(name="차단", description="유저를 서버에서 차단(ban)합니다.")
    @app_commands.describe(유저="대상", 사유="사유")
    @requires_permission("admin")
    async def ban(self, interaction: discord.Interaction, 유저: discord.Member, 사유: str) -> None:
        reject = self._guard(interaction, 유저)
        if reject:
            await interaction.response.send_message(reject, ephemeral=True)
            return
        await self._dm(유저, f"🔨 서버에서 차단되었습니다.\n**사유:** {사유}")
        try:
            await interaction.guild.ban(
                유저, reason=f"{interaction.user}: {사유}", delete_message_seconds=0
            )
        except discord.Forbidden:
            await interaction.response.send_message(
                "봇 권한 부족(Ban Members) 또는 위계 문제로 차단 실패.", ephemeral=True
            )
            return
        except discord.HTTPException:
            await interaction.response.send_message("차단 처리 중 오류가 발생했습니다.", ephemeral=True)
            return
        await mod_log.record(
            self.bot, action="ban", operator_id=interaction.user.id, target_id=유저.id, reason=사유,
        )
        await interaction.response.send_message(
            f"🔨 {유저} 차단 완료. 사유: {사유}", ephemeral=True,
        )

    # ─── /차단해제 ────────────────────────────────────────────────
    @app_commands.command(name="차단해제", description="차단된 유저를 해제(unban)합니다.")
    @app_commands.describe(유저_id="차단 해제할 유저의 디스코드 ID")
    @requires_permission("admin")
    async def unban(self, interaction: discord.Interaction, 유저_id: str) -> None:
        try:
            uid = int(유저_id)
        except ValueError:
            await interaction.response.send_message("유저 ID는 숫자여야 합니다.", ephemeral=True)
            return
        try:
            await interaction.guild.unban(discord.Object(id=uid), reason=f"{interaction.user}: 차단해제")
        except discord.NotFound:
            await interaction.response.send_message("차단 목록에 없는 유저입니다.", ephemeral=True)
            return
        except discord.Forbidden:
            await interaction.response.send_message("봇 권한 부족(Ban Members)으로 해제 실패.", ephemeral=True)
            return
        except discord.HTTPException:
            await interaction.response.send_message("처리 중 오류가 발생했습니다.", ephemeral=True)
            return
        await mod_log.record(
            self.bot, action="unban", operator_id=interaction.user.id, target_id=uid,
        )
        await interaction.response.send_message(f"🔓 `{uid}` 차단 해제 완료.", ephemeral=True)

    # ─── 내부 헬퍼 ────────────────────────────────────────────────
    def _guard(self, interaction: discord.Interaction, target: discord.Member) -> str | None:
        """제재 공통 가드: 대상 보호(§1b) + 봇 위계. 거부 사유, 통과면 None."""
        return (
            _target_reject_reason(interaction.user, target, self.bot.user)  # type: ignore[arg-type]
            or _bot_hierarchy_reject(interaction.guild, target)
        )

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
