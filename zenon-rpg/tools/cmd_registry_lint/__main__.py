"""CLI 엔트리포인트.

사용법:
    python3 -m tools.cmd_registry_lint <csv_path>

exit code:
    0: 문제 없음
    1: 린트 에러 1건 이상
    2: 실행 오류 (파일 없음·형식 이상 등)

PR#4에서 `--format=human|json` 옵션과 `--summary` 플래그 추가 예정.
"""

from __future__ import annotations

import csv
import sys
from pathlib import Path

from . import rules


def load_csv(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f)
        return list(reader)


def format_errors(errors) -> str:
    lines = []
    for err in errors:
        prev = f" (이전 row {err.prev_row})" if err.prev_row else ""
        hint = f"  힌트: {err.hint}" if err.hint else ""
        lines.append(f"[{err.code}] row {err.row}, col '{err.col}': {err.value}{prev}")
        if hint:
            lines.append(hint)
    return "\n".join(lines)


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print("usage: python3 -m tools.cmd_registry_lint <csv_path>", file=sys.stderr)
        return 2
    path = Path(argv[1])
    if not path.exists():
        print(f"error: CSV 파일을 찾을 수 없음: {path}", file=sys.stderr)
        return 2
    rows = load_csv(path)
    report = rules.run_all(rows)
    if report.ok:
        print(f"OK: {len(rows)} rows · 린트 에러 없음")
        return 0
    print(f"FAIL: {len(rows)} rows · 에러 {len(report.errors)}건")
    print(format_errors(report.errors))
    return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
