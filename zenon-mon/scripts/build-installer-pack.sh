#!/usr/bin/env bash
#
# build-installer-pack.sh ─ pack.json → 설치기 번들 스테이징 (결정 046 / installer_design.md §6)
# ---------------------------------------------------------------------------
# 목적:
#   `modpack/pack.json`(= gen-pack-json.py 산출)을 읽어 자체 설치기 엔진이 패키징할
#   "번들 디렉터리"를 만든다. 엔진(Python→exe)은 이 번들을 동봉해 유저 PC에서
#   필수+선택 모드를 설치한다.
#
# 번들 구조(installer_design.md §2):
#   <stage>/
#   ├── pack.json                  # 팩 정의(모드목록·토글·서버·버전핀)
#   ├── mods/                      # 클라 81 jar(required+optional+libraries). PoroMonCore=빌드본
#   ├── overrides/                 # config·showdown·xaero·fancymenu_data·shaderpacks
#   │                              #   (단 config/openloader/data = 서버 데이터팩 제외, 결정 043)
#   └── tools/fabric-installer.jar # Fabric headless 설치용(있으면)
#
# 비고:
#   - 모드는 전부 번들(결정 046) → 외부 다운로드 0. (S)서버전용 5개는 pack.json에서
#     이미 제외돼 있으므로 여기서도 자동 제외.
#   - PoroMonCore는 client/mods 사본이 stale일 수 있어 항상 빌드본으로 덮어쓴다
#     (서버와 동일 해시, installer_design §4).
#   - 산출(번들·jar)은 Git 비추적(.local/).
#
# 사용:
#   ./scripts/build-installer-pack.sh                 # 빌드 + 번들 스테이징
#   SKIP_BUILD=1 ./scripts/build-installer-pack.sh    # gradle 빌드 생략(기존 jar)
#   FABRIC_INSTALLER=/path/to.jar ./scripts/...        # fabric-installer 경로 지정
# ---------------------------------------------------------------------------
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(dirname "$SCRIPT_DIR")"

PACK_JSON="$ROOT/modpack/pack.json"
MODS_SRC="$ROOT/modpack/client/mods"
OVERRIDES_SRC="$ROOT/modpack/overrides"
CORE_DIR="$ROOT/custom-mods/poromon-core"

SKIP_BUILD="${SKIP_BUILD:-0}"
DIST_DIR="${DIST_DIR:-$ROOT/.local/installer-pack}"
PACK_NAME="${PACK_NAME:-PoroMon}"
FABRIC_INSTALLER="${FABRIC_INSTALLER:-$ROOT/modpack/tools/fabric-installer.jar}"

# overrides 복사 시 제외(클라 미적용 서버 데이터팩) — overrides 기준 상대경로
OVERRIDE_EXCLUDES=( "config/openloader/data" )

# ── 사전 점검 ──────────────────────────────────────────────────────────────
if [ ! -f "$PACK_JSON" ]; then
  echo "ERROR: pack.json 없음 ($PACK_JSON). 먼저 'python3 scripts/gen-pack-json.py' 실행." >&2
  exit 1
fi

# ── [1/5] PoroMonCore 빌드 ─────────────────────────────────────────────────
if [ "$SKIP_BUILD" = "1" ]; then
  echo "[1/5] PoroMonCore 빌드 생략 (SKIP_BUILD=1)"
else
  echo "[1/5] PoroMonCore 빌드..."
  ( cd "$CORE_DIR" && ./gradlew build -q )
fi
CORE_JAR="$(find "$CORE_DIR/build/libs" -maxdepth 1 -name 'poromon-core-*.jar' ! -name '*-sources.jar' 2>/dev/null | sort | tail -n1)"
if [ -z "$CORE_JAR" ]; then
  echo "ERROR: poromon-core jar 없음 ($CORE_DIR/build/libs)." >&2
  exit 1
fi
echo "  → $(basename "$CORE_JAR")"

# ── 스테이징 초기화 ────────────────────────────────────────────────────────
STAGE="$DIST_DIR/$PACK_NAME"
rm -rf "$STAGE"
mkdir -p "$STAGE/mods" "$STAGE/overrides" "$STAGE/tools"

# ── [2/5] 클라 mods 복사 (required + optional + libraries) ──────────────────
echo "[2/5] 클라 mods 복사..."
mapfile -t CLIENT_FILES < <(python3 -c "
import json,sys
p=json.load(open('$PACK_JSON'))
for k in ('required','optional','libraries'):
    for e in p.get(k,[]):
        print(e['file'])
")
MISSING=()
for f in "${CLIENT_FILES[@]}"; do
  if [ -f "$MODS_SRC/$f" ]; then
    cp "$MODS_SRC/$f" "$STAGE/mods/"
  else
    MISSING+=( "$f" )
  fi
done
if [ "${#MISSING[@]}" -gt 0 ]; then
  echo "ERROR: client/mods 에 없는 pack.json 항목(${#MISSING[@]}):" >&2
  printf '  - %s\n' "${MISSING[@]}" >&2
  exit 2
fi
# PoroMonCore = 빌드본으로 교체(stale 방지)
rm -f "$STAGE/mods/"poromon-core-*.jar
cp "$CORE_JAR" "$STAGE/mods/"
echo "  → ${#CLIENT_FILES[@]}개 복사 + PoroMonCore 빌드본 교체"

# ── [3/5] overrides 복사 (pack.json overrides[], 서버 데이터팩 제외) ────────
echo "[3/5] overrides 복사..."
EXCLUDE_ARGS=()
for ex in "${OVERRIDE_EXCLUDES[@]}"; do
  EXCLUDE_ARGS+=( --exclude "$ex" )
  echo "  제외: overrides/$ex"
done
mapfile -t OVR_DIRS < <(python3 -c "
import json
for d in json.load(open('$PACK_JSON')).get('overrides',[]): print(d)
")
for d in "${OVR_DIRS[@]}"; do
  if [ -d "$OVERRIDES_SRC/$d" ]; then
    rsync -a "${EXCLUDE_ARGS[@]}" "$OVERRIDES_SRC/$d" "$STAGE/overrides/"
    echo "  + overrides/$d"
  else
    echo "  WARN: overrides/$d 없음(건너뜀)"
  fi
done

# ── [4/5] pack.json + fabric-installer ─────────────────────────────────────
echo "[4/5] pack.json + tools..."
cp "$PACK_JSON" "$STAGE/pack.json"
if [ -f "$FABRIC_INSTALLER" ]; then
  cp "$FABRIC_INSTALLER" "$STAGE/tools/fabric-installer.jar"
  echo "  + tools/fabric-installer.jar"
else
  echo "  ⚠️ fabric-installer 없음 ($FABRIC_INSTALLER) — 엔진 빌드 전 채울 것"
  echo "     (Fabric 공식 installer jar를 modpack/tools/fabric-installer.jar 로 배치하거나 FABRIC_INSTALLER 지정)"
fi

# ── [5/5] 검증 + 요약 ──────────────────────────────────────────────────────
MODS_COUNT="$(find "$STAGE/mods" -maxdepth 1 -name '*.jar' | wc -l | tr -d ' ')"
EXPECTED="${#CLIENT_FILES[@]}"
echo "[5/5] 완료."
echo "  스테이징    : $STAGE"
echo "  mods        : $MODS_COUNT개 (기대 $EXPECTED)"
[ "$MODS_COUNT" = "$EXPECTED" ] && echo "  검증        : OK" || echo "  검증        : ⚠️ 개수 불일치"
echo ""
echo "⚠️ 다음: ① fabric-installer.jar 번들(미보유 시) ② 엔진(Python GUI→PyInstaller exe)이"
echo "        이 스테이징을 동봉해 패키징 ③ 서버 PoroMonCore 동기화(동일 해시)"
