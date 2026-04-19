"""PR#2·PR#3 — R1~R5 감지 규칙.

참조: docs/10_seed_and_config_tables/poro_custom_model_data_registry_csv_spec_draft.md §3.3·§6.2
     docs/10_seed_and_config_tables/poro_custom_model_data_registry_ci_implementation_plan_draft.md §5

R1: customModelData 중복 불가 (PK 무결성)
R2: asset_path 중복 불가 (같은 에셋을 두 CMD가 점유하면 로드 충돌)
R3: item_id ↔ customModelData 1:1 (같은 item_id가 두 CMD에 걸리면 런타임 충돌)
R4: 예약 구간 정합 (customModelData 앞자리 `A`가 domain과 일치)
R5: enum·semver 포맷 (status / version_added / version_deprecated)
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Iterable

from . import ranges


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


def check_reserved_range(rows: list[dict[str, str]]) -> list[LintError]:
    """R4 — customModelData 앞자리(`A`) ↔ domain 예약 구간 정합."""
    errors: list[LintError] = []
    for i, row in enumerate(rows, start=2):
        domain = row.get("domain", "").strip()
        cmd_raw = row.get("customModelData", "").strip()
        if not domain or not cmd_raw:
            continue
        try:
            cmd = int(cmd_raw)
        except ValueError:
            errors.append(
                LintError(
                    code="CMD_NOT_INT",
                    row=i,
                    col="customModelData",
                    value=cmd_raw,
                    hint="customModelData는 7자리 정수여야 함",
                )
            )
            continue
        range_ = ranges.DOMAIN_RANGES.get(domain)
        if range_ is None:
            errors.append(
                LintError(
                    code="UNKNOWN_DOMAIN",
                    row=i,
                    col="domain",
                    value=domain,
                    hint=f"허용 domain: {sorted(ranges.DOMAIN_RANGES)}",
                )
            )
            continue
        lo, hi = range_
        if not (lo <= cmd <= hi):
            errors.append(
                LintError(
                    code="OUT_OF_RANGE",
                    row=i,
                    col="customModelData",
                    value=cmd_raw,
                    hint=f"domain={domain} requires [{lo},{hi}]",
                )
            )
    return errors


def check_enum_and_semver(rows: list[dict[str, str]]) -> list[LintError]:
    """R5 — status enum 및 version_added / version_deprecated semver 포맷."""
    errors: list[LintError] = []
    for i, row in enumerate(rows, start=2):
        status = row.get("status", "").strip()
        if status and status not in ranges.STATUS_VALUES:
            errors.append(
                LintError(
                    code="BAD_STATUS",
                    row=i,
                    col="status",
                    value=status,
                    hint=f"허용 status: {sorted(ranges.STATUS_VALUES)}",
                )
            )
        for col in ("version_added", "version_deprecated"):
            ver = row.get(col, "").strip()
            # version_deprecated는 optional (빈 값 허용)
            if not ver:
                continue
            if not ranges.SEMVER_PATTERN.match(ver):
                errors.append(
                    LintError(
                        code="BAD_SEMVER",
                        row=i,
                        col=col,
                        value=ver,
                        hint="형식: s<season>-v<major>.<minor>.<patch> (예: s1-v0.1.0)",
                    )
                )
    return errors


def run_all(rows: list[dict[str, str]]) -> LintReport:
    report = LintReport()
    report.extend(check_duplicate_cmd(rows))
    report.extend(check_duplicate_asset_path(rows))
    report.extend(check_item_id_cmd_one_to_one(rows))
    report.extend(check_reserved_range(rows))
    report.extend(check_enum_and_semver(rows))
    return report
