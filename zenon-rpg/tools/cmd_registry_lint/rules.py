"""PR#2 — R1·R2·R3 감지 규칙.

참조: docs/10_seed_and_config_tables/poro_custom_model_data_registry_csv_spec_draft.md §6.2
     docs/10_seed_and_config_tables/poro_custom_model_data_registry_ci_implementation_plan_draft.md §5

R1: customModelData 중복 불가 (PK 무결성)
R2: asset_path 중복 불가 (같은 에셋을 두 CMD가 점유하면 로드 충돌)
R3: item_id ↔ customModelData 1:1 (같은 item_id가 두 CMD에 걸리면 런타임 충돌)
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Iterable


@dataclass(frozen=True)
class LintError:
    code: str
    row: int
    col: str
    value: str
    prev_row: int | None = None
    hint: str = ""


@dataclass
class LintReport:
    errors: list[LintError] = field(default_factory=list)

    @property
    def ok(self) -> bool:
        return not self.errors

    def extend(self, other: Iterable[LintError]) -> None:
        self.errors.extend(other)


def check_duplicate_cmd(rows: list[dict[str, str]]) -> list[LintError]:
    """R1 — customModelData 중복 감지."""
    seen: dict[str, int] = {}
    errors: list[LintError] = []
    for i, row in enumerate(rows, start=2):  # 2 = 헤더 다음 첫 행
        cmd = row.get("customModelData", "")
        if not cmd:
            continue
        if cmd in seen:
            errors.append(
                LintError(
                    code="DUP_CMD",
                    row=i,
                    col="customModelData",
                    value=cmd,
                    prev_row=seen[cmd],
                    hint=f"suggest-cmd --domain={row.get('domain', '?')}",
                )
            )
        else:
            seen[cmd] = i
    return errors


def check_duplicate_asset_path(rows: list[dict[str, str]]) -> list[LintError]:
    """R2 — asset_path 중복 감지. 빈 값은 무시."""
    seen: dict[str, int] = {}
    errors: list[LintError] = []
    for i, row in enumerate(rows, start=2):
        asset = row.get("asset_path", "").strip()
        if not asset:
            continue
        if asset in seen:
            errors.append(
                LintError(
                    code="DUP_ASSET_PATH",
                    row=i,
                    col="asset_path",
                    value=asset,
                    prev_row=seen[asset],
                    hint="같은 에셋을 두 CMD가 점유하면 리소스팩 로드 충돌",
                )
            )
        else:
            seen[asset] = i
    return errors


def check_item_id_cmd_one_to_one(rows: list[dict[str, str]]) -> list[LintError]:
    """R3 — item_id ↔ customModelData 1:1 제약.

    같은 item_id가 다른 customModelData에 매핑되면 오류.
    """
    seen_item: dict[str, tuple[str, int]] = {}
    errors: list[LintError] = []
    for i, row in enumerate(rows, start=2):
        item_id = row.get("item_id", "").strip()
        cmd = row.get("customModelData", "").strip()
        if not item_id or not cmd:
            continue
        if item_id in seen_item:
            prev_cmd, prev_row = seen_item[item_id]
            if prev_cmd != cmd:
                errors.append(
                    LintError(
                        code="ITEM_ID_CMD_MISMATCH",
                        row=i,
                        col="item_id",
                        value=f"{item_id} → {cmd} (이전 {prev_cmd} @ row {prev_row})",
                        prev_row=prev_row,
                        hint="같은 item_id는 단일 customModelData에만 매핑되어야 함",
                    )
                )
        else:
            seen_item[item_id] = (cmd, i)
    return errors


def run_all(rows: list[dict[str, str]]) -> LintReport:
    report = LintReport()
    report.extend(check_duplicate_cmd(rows))
    report.extend(check_duplicate_asset_path(rows))
    report.extend(check_item_id_cmd_one_to_one(rows))
    return report
