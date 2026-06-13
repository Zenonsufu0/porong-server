# -*- mode: python ; coding: utf-8 -*-
# Zenon Mon 설치기 PyInstaller spec (결정 046 / installer_design.md §6)
#
# 선행: ../scripts/build-installer-pack.sh 로 번들 생성
#       (.local/installer-pack/ZenonMon = pack.json + mods/ + overrides/ + tools/)
# 빌드(Windows 권장 — exe 타겟은 Windows에서):
#       cd installer && pyinstaller ZenonMon.spec
#       → dist/ZenonMon설치기.exe (번들 동봉, 단일 파일)
#
# SPECPATH = 이 spec 파일이 있는 디렉터리(installer/). PyInstaller 가 주입.
import os

bundle_src = os.path.join(SPECPATH, "..", ".local", "installer-pack", "ZenonMon")
if not os.path.isfile(os.path.join(bundle_src, "pack.json")):
    raise SystemExit(
        f"번들 없음: {bundle_src}\n먼저 scripts/build-installer-pack.sh 를 실행하세요."
    )

a = Analysis(
    ["gui.py"],
    pathex=[SPECPATH],
    binaries=[],
    datas=[(bundle_src, "bundle")],          # exe 안에 번들 동봉 → 런타임 _MEIPASS/bundle
    hiddenimports=[
        "zenon_mon_installer.pack",
        "zenon_mon_installer.nbt",
        "zenon_mon_installer.steps",
        "zenon_mon_installer.installer",
        "zenon_mon_installer.platform",
    ],
    hookspath=[],
    runtime_hooks=[],
    excludes=[],
    noarchive=False,
)
pyz = PYZ(a.pure)

# onefile = EXE 에 a.binaries/a.datas 를 모두 포함(COLLECT 없음).
# 아이콘: Windows exe 는 .ico 필요. modpack/overrides 에 icon.ico 있으면 사용(없으면 기본).
_ico = os.path.join(SPECPATH, "..", "modpack", "overrides", "icon.ico")
exe = EXE(
    pyz,
    a.scripts,
    a.binaries,
    a.datas,
    [],
    name="ZenonMon설치기",
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    upx_exclude=[],
    runtime_tmpdir=None,
    console=False,            # 창 모드(콘솔 숨김)
    disable_windowed_traceback=False,
    icon=_ico if os.path.isfile(_ico) else None,
)
