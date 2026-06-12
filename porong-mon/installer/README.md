# PoroMon 설치기 (범용 엔진)

자체 제작 모드 설치기 — **결정 046** / 설계 SoT: `../docs/01_modpack/installer_design.md`.

유저가 `.exe` 더블클릭 → 필수 모드 자동 + 선택 모드 체크박스 → 설치 → 서버 접속.
**범용 엔진**: 코드는 공용, 서버별 `pack.json` + 번들만 교체해 재활용(포로몬/포로건…).

## 구조

```
installer/
├── poromon_installer/
│   ├── pack.py        # pack.json 로드/모델 (required/optional/libraries/overrides)
│   ├── nbt.py         # servers.dat 미니 NBT 읽기/쓰기 (자체·무의존)
│   ├── steps.py       # 설치 단계(인스턴스·Fabric·mods·overrides·서버등록·런처프로필)
│   ├── installer.py   # 오케스트레이션(install)
│   └── platform.py    # OS별 경로(.minecraft·launcher_profiles)
├── main.py            # CLI 엔트리(--list / --plan / --install)
└── gui.py             # (다음 단계) tkinter 체크박스 UI
```

## 설계 원칙

- **무의존(표준 라이브러리만)** — PyInstaller `--onefile` 패키징을 쉽게. NBT도 자체 구현.
- **번들 입력** = `build-installer-pack.sh` 산출(`.local/installer-pack/PoroMon/`): `pack.json` + `mods/` + `overrides/` + `tools/fabric-installer.jar`.
- **독립 인스턴스** — 유저 기존 `.minecraft` 세이브를 건드리지 않고 별도 gameDir에 설치.
- **dry-run** — 실제 파일 변경 없이 설치 계획만 출력(WSL/개발 검증용).

## 사용 (개발/CLI)

```bash
# 번들 먼저 생성: ../scripts/build-installer-pack.sh
python3 main.py --bundle <bundle_dir> --list             # 모드 목록(필수/선택)
python3 main.py --bundle <bundle_dir> --plan             # 설치 계획(dry-run)
python3 main.py --bundle <bundle_dir> --install --target <dir>   # 실제 설치
```

기본값: 선택 모드는 pack.json `default`(T1 ON / T2 OFF). `--enable/--disable <file>`로 조정.

## 상태

- ✅ 엔진 코어(pack·nbt·steps·installer·CLI) — WSL 단위 검증.
- ⬜ GUI(tkinter) + PyInstaller exe 빌드 — 다음 단계.
- ⬜ 실 설치 검증(Windows+MC) — 사용자.
- ⬜ `tools/fabric-installer.jar` 확보(번들).
