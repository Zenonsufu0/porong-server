"""
FAQ(자주 묻는 질문) — T16 support.md §3.

조회 = 패널형(B안). `/faq` → 등록된 질문 Select 메뉴에서 골라 답변(ephemeral).
미매칭/원하는 답이 없으면 [운영진 문의] 버튼 → 티켓(1:1 문의) 폴백(§2 연결, LLM 미사용).

내용은 운영자가 직접 등록(`/faq추가`·`/faq수정`·`/faq삭제`, admin·support). domain NULL=공통.
조회 패널은 공통 + 현재 active 서버 도메인만 노출(전역 단일 active 모델, task.md §5).
"""
from __future__ import annotations

import logging

import discord
from discord import app_commands
from discord.ext import commands

from core import faq, mod_log, servers
from core.permissions import requires_permission

log = logging.getLogger(__name__)

_SELECT_LIMIT = 25  # 디스코드 Select 옵션 상한

# /faq추가·수정 대상 도메인 선택지. value="common" → DB domain=NULL(전 서버 공통).
_DOMAIN_CHOICES = [
    app_commands.Choice(name="공통(전 서버)", value="common"),
    app_commands.Choice(name="RPG", value="rpg"),
    app_commands.Choice(name="포로몬", value="poromon"),
]


def _domain_label(domain: str | None) -> str:
    return {"rpg": "RPG", "poromon": "포로몬"}.get(domain or "", "공통") if domain else "공통"


class FaqModal(discord.ui.Modal):
    """FAQ 추가/수정 입력 모달(질문 + 답변)."""

    질문 = discord.ui.TextInput(
        label="질문(트리거)", max_length=100, placeholder="예) 서버 주소가 뭔가요?"
    )
    답변 = discord.ui.TextInput(
        label="답변", style=discord.TextStyle.paragraph, max_length=2000
    )

    def __init__(
        self, cog: "FaqCog", *, domain: str | None, faq_id: int | None,
        trigger: str = "", answer: str = "",
    ) -> None:
        title = "FAQ 수정" if faq_id is not None else "FAQ 추가"
        super().__init__(title=title)
        self.cog = cog
        self.domain = domain
        self.faq_id = faq_id
        self.질문.default = trigger
        self.답변.default = answer

    async def on_submit(self, interaction: discord.Interaction) -> None:
        trigger = self.질문.value.strip()
        answer = self.답변.value.strip()
        if self.faq_id is None:
            fid = await faq.add_faq(self.cog.db, self.domain, trigger, answer, interaction.user.id)
            await mod_log.record(
                self.cog.bot, action="faq_add", operator_id=interaction.user.id,
                detail={"faq_id": fid, "domain": self.domain or "공통"},
            )
            await interaction.response.send_message(
                f"✅ FAQ #{fid} 추가됨 ({_domain_label(self.domain)}): **{trigger}**", ephemeral=True
            )
        else:
            await faq.update_faq(self.cog.db, self.faq_id, trigger, answer, interaction.user.id)
            await mod_log.record(
                self.cog.bot, action="faq_update", operator_id=interaction.user.id,
                detail={"faq_id": self.faq_id},
            )
            await interaction.response.send_message(
                f"✅ FAQ #{self.faq_id} 수정됨: **{trigger}**", ephemeral=True
            )


class FaqSelect(discord.ui.Select):
    """등록된 FAQ 선택 → 답변 ephemeral."""

    def __init__(self, cog: "FaqCog", faqs: list) -> None:
        self.cog = cog
        options = [
            discord.SelectOption(
                label=row["trigger"][:100],
                value=str(row["id"]),
                description=(row["answer"][:100] or None),
            )
            for row in faqs[:_SELECT_LIMIT]
        ]
        super().__init__(placeholder="질문을 선택하세요…", options=options)

    async def callback(self, interaction: discord.Interaction) -> None:
        row = await faq.get_faq(self.cog.db, int(self.values[0]))
        if row is None:
            await interaction.response.send_message(
                "해당 FAQ가 삭제되었습니다.", ephemeral=True
            )
            return
        embed = discord.Embed(
            title=f"❓ {row['trigger']}",
            description=row["answer"],
            color=discord.Color.blurple(),
        )
        await interaction.response.send_message(embed=embed, ephemeral=True)


class FaqPanelView(discord.ui.View):
    """FAQ 조회 패널(ephemeral, 호출 시점 FAQ로 구성 — 비영구 뷰)."""

    def __init__(self, cog: "FaqCog", faqs: list) -> None:
        super().__init__(timeout=180)
        self.cog = cog
        if faqs:
            self.add_item(FaqSelect(cog, faqs))
        contact = discord.ui.Button(
            label="원하는 답이 없어요 — 운영진 문의",
            style=discord.ButtonStyle.secondary,
            emoji="📨",
        )
        contact.callback = self._on_contact
        self.add_item(contact)

    async def _on_contact(self, interaction: discord.Interaction) -> None:
        ticket_cog = self.cog.bot.get_cog("TicketCog")
        if ticket_cog is None:
            await interaction.response.send_message(
                "문의 기능을 사용할 수 없습니다. `/문의` 명령을 직접 사용해주세요.", ephemeral=True
            )
            return
        await ticket_cog.open_ticket_for(interaction)  # type: ignore[attr-defined]


class FaqCog(commands.Cog):
    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot

    @property
    def db(self):
        return self.bot.db  # type: ignore[attr-defined]

    # ─── 조회(유저) ────────────────────────────────────────────────

    @app_commands.command(name="faq", description="자주 묻는 질문에서 골라 답변을 확인합니다.")
    async def faq_panel(self, interaction: discord.Interaction) -> None:
        active = await servers.get_any_active(self.db)
        domain = active["domain"] if active else None
        faqs = await faq.list_for_domain(self.db, domain)

        if faqs:
            content = "❓ **자주 묻는 질문** — 아래에서 질문을 선택하세요."
            if len(faqs) > _SELECT_LIMIT:
                content += f"\n*(총 {len(faqs)}개 중 {_SELECT_LIMIT}개 표시. 못 찾으면 운영진 문의를 이용하세요.)*"
        else:
            content = "등록된 FAQ가 아직 없습니다. 궁금한 점은 운영진 문의를 이용하세요."
        await interaction.response.send_message(
            content, view=FaqPanelView(self, faqs), ephemeral=True
        )

    # ─── 관리(운영) ────────────────────────────────────────────────

    @app_commands.command(name="faq추가", description="FAQ 항목을 추가합니다(운영).")
    @app_commands.describe(대상="공통/서버별 구분(생략 시 공통)")
    @app_commands.choices(대상=_DOMAIN_CHOICES)
    @requires_permission("admin", "support")
    async def faq_add(
        self, interaction: discord.Interaction,
        대상: app_commands.Choice[str] | None = None,
    ) -> None:
        domain = None if 대상 is None or 대상.value == "common" else 대상.value
        await interaction.response.send_modal(FaqModal(self, domain=domain, faq_id=None))

    @app_commands.command(name="faq수정", description="FAQ 항목을 수정합니다(운영).")
    @app_commands.describe(번호="수정할 FAQ 번호")
    @requires_permission("admin", "support")
    async def faq_edit(self, interaction: discord.Interaction, 번호: int) -> None:
        row = await faq.get_faq(self.db, 번호)
        if row is None:
            await interaction.response.send_message(
                f"FAQ #{번호} 를 찾을 수 없습니다.", ephemeral=True
            )
            return
        await interaction.response.send_modal(
            FaqModal(
                self, domain=row["domain"], faq_id=번호,
                trigger=row["trigger"], answer=row["answer"],
            )
        )

    @app_commands.command(name="faq삭제", description="FAQ 항목을 삭제합니다(운영).")
    @app_commands.describe(번호="삭제할 FAQ 번호")
    @requires_permission("admin", "support")
    async def faq_delete(self, interaction: discord.Interaction, 번호: int) -> None:
        row = await faq.get_faq(self.db, 번호)
        if row is None:
            await interaction.response.send_message(
                f"FAQ #{번호} 를 찾을 수 없습니다.", ephemeral=True
            )
            return
        await faq.delete_faq(self.db, 번호)
        await mod_log.record(
            self.bot, action="faq_delete", operator_id=interaction.user.id,
            detail={"faq_id": 번호, "trigger": row["trigger"][:50]},
        )
        await interaction.response.send_message(
            f"🗑 FAQ #{번호} 를 삭제했습니다: **{row['trigger']}**", ephemeral=True
        )

    @faq_edit.autocomplete("번호")
    @faq_delete.autocomplete("번호")
    async def _faq_id_autocomplete(
        self, interaction: discord.Interaction, current: str
    ) -> list[app_commands.Choice[int]]:
        rows = await faq.list_all(self.db)
        choices: list[app_commands.Choice[int]] = []
        for row in rows:
            name = f"#{row['id']} [{_domain_label(row['domain'])}] {row['trigger']}"[:100]
            if current and current not in name and current != str(row["id"]):
                continue
            choices.append(app_commands.Choice(name=name, value=row["id"]))
            if len(choices) >= 25:
                break
        return choices


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(FaqCog(bot))
