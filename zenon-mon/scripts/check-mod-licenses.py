#!/usr/bin/env python3
"""check-mod-licenses.py ─ 클라 mods jar 의 라이선스 추출/분류 (결정 046 §9-1).

전부 번들(exe 동봉) = 우리가 직접 재배포 → 모드별 라이선스가 적용된다.
각 jar 의 fabric.mod.json(또는 quilt.mod.json) "license" 필드를 읽어
재배포 관점으로 분류한다:

  OK         : 재배포 허용 명확(MIT/Apache/BSD/ISC/Zlib/MPL/LGPL/CC0/Unlicense 등)
  COPYLEFT   : GPL/AGPL — 번들 가능하나 소스공개 의무·배포물 영향(주의)
  CHECK      : ARR/커스텀/불명확/필드없음 — 개별 확인 필요(번들 막힐 수 있음)

사용: python3 scripts/check-mod-licenses.py
"""
import json
import os
import re
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODS_DIR = os.path.join(ROOT, "modpack", "client", "mods")

OK_PATTERNS = [
    r"\bMIT\b", r"\bApache", r"\bBSD\b", r"\bISC\b", r"\bzlib\b", r"\bMPL", r"\bLGPL",
    r"\bCC0\b", r"\bUnlicense\b", r"\bWTFPL\b", r"\bCC-BY\b", r"\bcreative commons\b",
    r"\bUPL\b", r"\bEPL\b", r"\bBSL\b", r"\bboost\b",
]
COPYLEFT_PATTERNS = [r"\bGPL", r"\bGPLv", r"\bGNU General Public", r"\bAGPL"]
# GPL 매칭 시 LGPL 은 OK 로 먼저 걸러야 하므로 순서 주의(아래 classify 에서 처리)


def license_text(jar_path):
    """jar 의 license 필드 + 동봉 LICENSE 파일 유무를 (license_value, has_license_file) 로."""
    lic, has_file = None, False
    try:
        with zipfile.ZipFile(jar_path) as z:
            names = z.namelist()
            has_file = any(
                re.search(r"(^|/)(LICENSE|COPYING|license)(\.|$|_)", n, re.I) for n in names
            )
            for meta in ("fabric.mod.json", "quilt.mod.json"):
                if meta in names:
                    try:
                        data = json.loads(z.read(meta).decode("utf-8", "replace"))
                    except Exception:
                        continue
                    val = data.get("license")
                    if val is None and "quilt_loader" in data:  # quilt 구조
                        val = data.get("quilt_loader", {}).get("metadata", {}).get("license")
                    if val:
                        lic = val
                        break
    except zipfile.BadZipFile:
        return ("(bad zip)", False)
    return (lic, has_file)


def norm(val):
    if val is None:
        return ""
    if isinstance(val, list):
        return ", ".join(norm(v) for v in val)
    if isinstance(val, dict):
        return val.get("id") or val.get("name") or json.dumps(val, ensure_ascii=False)
    return str(val)


def classify(lic_str):
    s = lic_str.strip()
    if not s:
        return "CHECK"
    low = s.lower()
    if "all rights reserved" in low or low in ("arr", "proprietary"):
        return "CHECK"
    # LGPL 은 OK(약한 카피레프트). 강한 GPL/AGPL 만 COPYLEFT.
    if any(re.search(p, s, re.I) for p in OK_PATTERNS):
        # MPL/LGPL/Apache 등 먼저 OK
        if re.search(r"\bA?GPL", s, re.I) and not re.search(r"\bLGPL", s, re.I):
            return "COPYLEFT"
        return "OK"
    if re.search(r"\bA?GPL", s, re.I):
        return "COPYLEFT"
    return "CHECK"


def main():
    if not os.path.isdir(MODS_DIR):
        print(f"ERROR: {MODS_DIR} 없음", file=sys.stderr)
        return 1
    jars = sorted(f for f in os.listdir(MODS_DIR) if f.endswith(".jar"))
    rows = []
    for j in jars:
        lic, has_file = license_text(os.path.join(MODS_DIR, j))
        s = norm(lic)
        rows.append((classify(s), j, s or "(없음)", "L" if has_file else "-"))

    order = {"CHECK": 0, "COPYLEFT": 1, "OK": 2}
    rows.sort(key=lambda r: (order[r[0]], r[1].lower()))

    print(f"{'분류':<8} {'LICENSE':<3} {'라이선스':<28} jar")
    print("-" * 90)
    for cls, jar, s, hasf in rows:
        print(f"{cls:<8} {hasf:<3} {s[:28]:<28} {jar}")

    from collections import Counter
    c = Counter(r[0] for r in rows)
    print("\n요약:", dict(c), f"(총 {len(rows)})")
    if c.get("CHECK"):
        print(f"\n⚠️ CHECK {c['CHECK']}개 = 개별 확인 필요(번들 재배포 가능 여부). "
              "필드 없음은 동봉 LICENSE(L)·소스레포 확인.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
