"""PR#4 — 리포트 포맷터 단위 테스트.

`--format=human|json` + `--summary` 출력 포맷 검증.
스냅샷 비교보다는 필수 필드·포맷 규약 확인(고정 오염 CSV 기반).

실행:
    python3 -m unittest tools.cmd_registry_lint.tests.test_report_format
"""

from __future__ import annotations

import json
import unittest
from pathlib import Path

from tools.cmd_registry_lint import __main__ as cli
from tools.cmd_registry_lint import rules


FIXTURES = Path(__file__).parent / "fixtures"


class FormatHumanTests(unittest.TestCase):
    def test_empty_errors_renders_empty_string(self):
        self.assertEqual(cli.format_human([]), "")

    def test_error_with_hint_renders_two_lines(self):
        errors = [
            rules.LintError(
                code="DUP_CMD",
                row=3,
                col="customModelData",
                value="1111000",
                prev_row=2,
                hint="suggest-cmd --domain=weapon",
            )
        ]
        text = cli.format_human(errors)
        # 2줄: 에러 헤더 + 힌트
        self.assertEqual(text.count("\n"), 1)
        self.assertIn("[DUP_CMD] row 3, col 'customModelData': 1111000", text)
        self.assertIn("(이전 row 2)", text)
        self.assertIn("suggest-cmd --domain=weapon", text)

    def test_error_without_hint_renders_single_line(self):
        errors = [
            rules.LintError(
                code="OUT_OF_RANGE",
                row=5,
                col="customModelData",
                value="2111000",
            )
        ]
        text = cli.format_human(errors)
        self.assertNotIn("\n", text)
        self.assertIn("[OUT_OF_RANGE]", text)


class FormatJsonTests(unittest.TestCase):
    def test_empty_errors_json_has_ok_true(self):
        out = cli.format_json(rows_count=14, errors=[])
        payload = json.loads(out)
        self.assertEqual(payload["rows"], 14)
        self.assertEqual(payload["error_count"], 0)
        self.assertTrue(payload["ok"])
        self.assertEqual(payload["errors"], [])

    def test_errors_json_contains_all_fields(self):
        errors = [
            rules.LintError(
                code="BAD_STATUS",
                row=2,
                col="status",
                value="IN_REVIEW",
                prev_row=None,
                hint="허용: reserved / designed / implemented / deprecated",
            )
        ]
        out = cli.format_json(rows_count=1, errors=errors)
        payload = json.loads(out)
        self.assertFalse(payload["ok"])
        self.assertEqual(payload["error_count"], 1)
        self.assertEqual(payload["errors"][0]["code"], "BAD_STATUS")
        self.assertEqual(payload["errors"][0]["row"], 2)
        self.assertEqual(payload["errors"][0]["col"], "status")
        self.assertEqual(payload["errors"][0]["value"], "IN_REVIEW")
        self.assertIsNone(payload["errors"][0]["prev_row"])

    def test_json_is_parseable_utf8(self):
        """한국어 hint가 JSON에 포함돼도 파싱 가능한지 확인."""
        errors = [
            rules.LintError(
                code="DUP_CMD",
                row=3,
                col="customModelData",
                value="1111000",
                prev_row=2,
                hint="같은 CMD가 두 행에 중복",
            )
        ]
        out = cli.format_json(rows_count=2, errors=errors)
        # UTF-8 한국어 직접 출력되는지(ensure_ascii=False)
        self.assertIn("같은 CMD가 두 행에 중복", out)
        # 파싱 가능
        payload = json.loads(out)
        self.assertEqual(payload["errors"][0]["hint"], "같은 CMD가 두 행에 중복")


class FormatSummaryTests(unittest.TestCase):
    def test_empty_errors_summary_is_ok(self):
        out = cli.format_summary(rows_count=14, errors=[])
        self.assertEqual(out, "OK: 14 rows · 에러 0건")

    def test_single_error_summary_shows_breakdown(self):
        errors = [
            rules.LintError(code="DUP_CMD", row=3, col="customModelData", value="1111000")
        ]
        out = cli.format_summary(rows_count=2, errors=errors)
        self.assertIn("FAIL: 2 rows · 에러 1건", out)
        self.assertIn("DUP_CMD 1", out)

    def test_multi_code_summary_has_sorted_breakdown(self):
        errors = [
            rules.LintError(code="DUP_CMD", row=3, col="customModelData", value="x"),
            rules.LintError(code="DUP_CMD", row=4, col="customModelData", value="y"),
            rules.LintError(code="BAD_STATUS", row=5, col="status", value="z"),
        ]
        out = cli.format_summary(rows_count=3, errors=errors)
        self.assertIn("에러 3건", out)
        # 정렬 순서: BAD_STATUS 1 · DUP_CMD 2 (알파벳)
        self.assertIn("BAD_STATUS 1", out)
        self.assertIn("DUP_CMD 2", out)
        # 알파벳 순서 확인
        self.assertLess(out.index("BAD_STATUS"), out.index("DUP_CMD"))


class MainCliExitCodeTests(unittest.TestCase):
    def test_clean_csv_returns_exit_0(self):
        clean_csv = Path(__file__).resolve().parents[3] / "docs" / "10_seed_and_config_tables" / "poro_custom_model_data_registry.csv"
        if not clean_csv.exists():
            self.skipTest("스켈레톤 CSV 부재")
        code = cli.main(["prog", str(clean_csv)])
        self.assertEqual(code, 0)

    def test_dup_cmd_fixture_returns_exit_1(self):
        fixture = FIXTURES / "dup_cmd.csv"
        code = cli.main(["prog", str(fixture)])
        self.assertEqual(code, 1)

    def test_nonexistent_file_returns_exit_2(self):
        code = cli.main(["prog", "/nonexistent/path/to/csv.csv"])
        self.assertEqual(code, 2)


if __name__ == "__main__":
    unittest.main()
