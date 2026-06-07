"""도메인 예약 구간 상수 테이블.

스펙 §3.3 "예약 구간 정합 규약" 그대로. 신규 도메인 추가 시 이 파일만 수정.

각 튜플: (lo, hi) — 포함(inclusive) 범위.
"""

from __future__ import annotations

DOMAIN_RANGES: dict[str, tuple[int, int]] = {
    "weapon":     (1_000_000, 1_999_999),
    "cosmetic":   (2_000_000, 2_999_999),
    "ui":         (3_000_000, 3_099_999),
    "block":      (4_000_000, 4_099_999),
    "consumable": (5_000_000, 5_099_999),
    "reserved":   (9_000_000, 9_999_999),
}

# R5용 enum 허용값
STATUS_VALUES: frozenset[str] = frozenset({
    "reserved", "designed", "implemented", "deprecated",
})

# semver 패턴: s<season>-v<major>.<minor>.<patch>
#   예: s1-v0.1.0
import re

SEMVER_PATTERN = re.compile(r"^s[1-9]\d*-v\d+\.\d+\.\d+$")
