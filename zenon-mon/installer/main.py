#!/usr/bin/env python3
"""Zenon Mon 설치기 CLI (엔진 검증·개발용. 유저 배포는 gui.py→exe).

사용:
    python3 main.py --bundle <dir> --list
    python3 main.py --bundle <dir> --plan
    python3 main.py --bundle <dir> --install --target <dir>
    python3 main.py --bundle <dir> --plan --disable jei-*.jar --enable iris-*.jar
"""
import argparse
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from zenon_mon_installer import Pack, Installer  # noqa: E402


def cmd_list(pack):
    print(f"# {pack.name} {pack.version}  (MC {pack.mc_version} / Fabric {pack.loader_version})")
    print(f"\n[필수 {len(pack.required)}] (항상 설치)")
    for m in pack.required:
        print(f"  - {m.name}  ({m.file})")
    print(f"\n[라이브러리 {len(pack.libraries)}] (자동 포함)")
    for m in pack.libraries:
        print(f"  - {m.name}")
    print(f"\n[선택 {len(pack.optional)}] (체크박스)")
    last = None
    for m in pack.optional:
        g = m.group or "기타"
        if g != last:
            print(f"  · {g}")
            last = g
        mark = "[v]" if m.default else "[ ]"
        desc = f" — {m.desc}" if m.desc else ""
        print(f"      {mark} {m.name}{desc}")


def main():
    ap = argparse.ArgumentParser(description="Zenon Mon 설치기 (CLI)")
    ap.add_argument("--bundle", required=True, help="번들 디렉터리(pack.json 포함)")
    ap.add_argument("--target", help="설치 인스턴스 폴더(기본=.minecraft/instances/<name>)")
    ap.add_argument("--mc-dir", help=".minecraft 경로(기본=OS 표준)")
    ap.add_argument("--list", action="store_true", help="모드 목록 출력")
    ap.add_argument("--plan", action="store_true", help="설치 계획(dry-run)")
    ap.add_argument("--install", action="store_true", help="실제 설치")
    ap.add_argument("--enable", action="append", default=[], help="선택 모드 켜기(파일명)")
    ap.add_argument("--disable", action="append", default=[], help="선택 모드 끄기(파일명)")
    args = ap.parse_args()

    pack_json = os.path.join(args.bundle, "pack.json")
    if not os.path.isfile(pack_json):
        print(f"ERROR: pack.json 없음: {pack_json}", file=sys.stderr)
        return 1

    if args.list:
        cmd_list(Pack.load(pack_json))
        return 0

    inst = Installer(args.bundle, target_dir=args.target, mc_dir=args.mc_dir)
    selected = inst.resolve_selection(enable=args.enable, disable=args.disable)

    if args.install:
        inst.install(selected, dry=False)
    else:  # 기본 = plan
        inst.install(selected, dry=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
