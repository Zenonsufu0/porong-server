"""PR#2 — R1·R2·R3 감지 규칙 단위 테스트.

각 규칙에 positive(위반 있음)·negative(위반 없음) 2건씩 = 6 케이스.
unittest 표준 라이브러리만 사용(외부 의존 없음).

실행:
    python3 -m unittest tools.cmd_registry_lint.tests.test_rules_core
"""

from __future__ import annotations

import csv
import unittest
from pathlib import Path

from tools.cmd_registry_lint import rules


FIXTURES = Path(__file__).parent / "fixtures"
CLEAN_CSV = Path(__file__).resolve().parents[3] / "docs" / "10_seed_and_config_tables" / "poro_custom_model_data_registry.csv"


def _load(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f))


class R1DuplicateCmdTests(unittest.TestCase):
    def test_positive_dup_cmd_detected(self):
        rows = _load(FIXTURES / "dup_cmd.csv")
        errors = rules.check_duplicate_cmd(rows)
        self.assertEqual(len(errors), 1)
        self.assertEqual(errors[0].code, "DUP_CMD")
        self.assertEqual(errors[0].value, "1111000")
        self.assertEqual(errors[0].prev_row, 2)
        self.assertEqual(errors[0].row, 3)

    def test_negative_no_dup_cmd_in_clean_csv(self):
        if not CLEAN_CSV.exists():
            self.skipTest("스켈레톤 CSV 부재")
        rows = _load(CLEAN_CSV)
        errors = rules.check_duplicate_cmd(rows)
        self.assertEqual(errors, [])


class R2DuplicateAssetPathTests(unittest.TestCase):
    def test_positive_dup_asset_path_detected(self):
        rows = _load(FIXTURES / "dup_asset_path.csv")
        errors = rules.check_duplicate_asset_path(rows)
        self.assertEqual(len(errors), 1)
        self.assertEqual(errors[0].code, "DUP_ASSET_PATH")
        self.assertEqual(errors[0].row, 3)
        self.assertEqual(errors[0].prev_row, 2)

    def test_negative_no_dup_asset_path_in_clean_csv(self):
        if not CLEAN_CSV.exists():
            self.skipTest("스켈레톤 CSV 부재")
        rows = _load(CLEAN_CSV)
        errors = rules.check_duplicate_asset_path(rows)
        self.assertEqual(errors, [])


class R3ItemIdCmdOneToOneTests(unittest.TestCase):
    def test_positive_item_id_mismatch_detected(self):
        rows = _load(FIXTURES / "item_id_mismatch.csv")
        errors = rules.check_item_id_cmd_one_to_one(rows)
        self.assertEqual(len(errors), 1)
        self.assertEqual(errors[0].code, "ITEM_ID_CMD_MISMATCH")
        self.assertEqual(errors[0].row, 3)
        self.assertEqual(errors[0].prev_row, 2)

    def test_negative_no_item_id_mismatch_in_clean_csv(self):
        if not CLEAN_CSV.exists():
            self.skipTest("스켈레톤 CSV 부재")
        rows = _load(CLEAN_CSV)
        errors = rules.check_item_id_cmd_one_to_one(rows)
        self.assertEqual(errors, [])


if __name__ == "__main__":
    unittest.main()
