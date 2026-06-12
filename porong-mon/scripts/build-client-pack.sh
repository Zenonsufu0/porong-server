#!/usr/bin/env bash
#
# build-client-pack.sh ─ PoroMon 클라 배포 팩 빌드 (결정 045 / client_pack_policy.md §6)
# ---------------------------------------------------------------------------
# 목적:
#   1) PoroMonCore 빌드 → custom-mods/poromon-core/build/libs/poromon-core-*.jar
#   2) 그 jar를 modpack/overrides/mods/ 에 번들 (CF에 없는 자체 모드 = 영구 번들, §4)
#   3) 클라 export 스테이징: overrides에서 서버 전용 데이터팩(config/openloader/data)
#      을 제외(§3-2, 결정 043) — 클라는 worldgen 서버권한이라 미적용 + 혼선 방지.
#   4) manifest.json + modlist.html + overrides/ 를 zip 패키징 → CF 업로드용.
#
# 비고:
#   - manifest의 80개 모드는 CF가 다운로드한다. overrides/mods 에는 PoroMonCore
#     처럼 CF에 없는 모드만 들어간다(나머지를 넣으면 중복/라이선스 문제).
#   - manifest(80) ↔ 실제(86) 정합·loader 0.19.3 은 별도 검증(§3-1). 이 스크립트는
#     현재 base/manifest 를 그대로 패키징한다(재익스포트 후 실행 권장).
#   - 빌드 산출 jar 와 zip 은 Git 비추적(.local/, **/*.jar gitignore).
#
# 사용:
#   ./scripts/build-client-pack.sh              # 빌드 + 번들 + 스테이징 + zip
#   SKIP_BUILD=1 ./scripts/build-client-pack.sh # gradle 빌드 생략(기존 jar 사용)
#   DIST_DIR=/path PACK_NAME=PoroMon-0.1 ...     # 출력 경로/이름 덮어쓰기
# ---------------------------------------------------------------------------
set -euo pipefail

# ROOT = porong-mon 워크스페이스(이 스크립트 상위). 폴더명 변경에 안전.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(dirname "$SCRIPT_DIR")"

CORE_DIR="$ROOT/custom-mods/poromon-core"
OVERRIDES="$ROOT/modpack/overrides"
MANIFEST="$ROOT/modpack/base/manifest/manifest.json"
MODLIST="$ROOT/modpack/base/mods-list/modlist.html"

SKIP_BUILD="${SKIP_BUILD:-0}"
DIST_DIR="${DIST_DIR:-$ROOT/.local/pack-build}"
PACK_NAME="${PACK_NAME:-PoroMon-0.1}"

# 클라 export 에서 제외할 경로(overrides 기준 상대) — 서버 전용 데이터팩(결정 043)
CLIENT_EXCLUDES=(
  "config/openloader/data"
)

# ── [1/5] PoroMonCore 빌드 ─────────────────────────────────────────────────
if [ "$SKIP_BUILD" = "1" ]; then
  echo "[1/5] PoroMonCore 빌드 생략 (SKIP_BUILD=1)"
else
  echo "[1/5] PoroMonCore 빌드..."
  ( cd "$CORE_DIR" && ./gradlew build -q )
fi

CORE_JAR="$(find "$CORE_DIR/build/libs" -maxdepth 1 -name 'poromon-core-*.jar' ! -name '*-sources.jar' 2>/dev/null | sort | tail -n1)"
if [ -z "$CORE_JAR" ]; then
  echo "ERROR: poromon-core jar 없음 ($CORE_DIR/build/libs). 먼저 빌드 필요."
  exit 1
fi
echo "  → $CORE_JAR"

# ── [2/5] overrides/mods 번들 (PoroMonCore 만) ─────────────────────────────
echo "[2/5] overrides/mods 번들..."
mkdir -p "$OVERRIDES/mods"
rm -f "$OVERRIDES/mods/"poromon-core-*.jar       # 옛 버전 잔재 제거
cp "$CORE_JAR" "$OVERRIDES/mods/"
echo "  → $OVERRIDES/mods/$(basename "$CORE_JAR")"

# ── [3/5] 클라 스테이징 (서버 전용 경로 제외) ──────────────────────────────
echo "[3/5] 클라 스테이징 구성..."
STAGE="$DIST_DIR/$PACK_NAME"
rm -rf "$STAGE"
mkdir -p "$STAGE/overrides"

EXCLUDE_ARGS=()
for ex in "${CLIENT_EXCLUDES[@]}"; do
  EXCLUDE_ARGS+=( --exclude "$ex" )
  echo "  제외: overrides/$ex"
done
rsync -a "${EXCLUDE_ARGS[@]}" "$OVERRIDES/" "$STAGE/overrides/"

if [ -f "$MANIFEST" ]; then cp "$MANIFEST" "$STAGE/manifest.json"; else echo "  WARN: manifest.json 없음 ($MANIFEST)"; fi
if [ -f "$MODLIST" ]; then cp "$MODLIST" "$STAGE/modlist.html"; else echo "  WARN: modlist.html 없음 ($MODLIST)"; fi

# ── [4/5] zip 패키징 ───────────────────────────────────────────────────────
echo "[4/5] zip 패키징..."
ZIP_PATH="$DIST_DIR/$PACK_NAME.zip"
rm -f "$ZIP_PATH"
( cd "$STAGE" && zip -qr "$ZIP_PATH" . )
echo "  → $ZIP_PATH"

# ── [5/5] 요약 ─────────────────────────────────────────────────────────────
echo "[5/5] 완료."
echo "  PoroMonCore jar : $(basename "$CORE_JAR")"
echo "  overrides/mods  : $OVERRIDES/mods/"
echo "  스테이징         : $STAGE"
echo "  배포 zip         : $ZIP_PATH"
echo ""
echo "⚠️ 다음: ① manifest 정합(projectID/fileID, loader fabric-0.19.3, MC 1.21.1, §3-1)"
echo "        ② 서버 PoroMonCore 동기화 — scripts/sync-server-mods.sh (동일 jar 해시)"
echo "        ③ 깨끗한 런처에 zip 설치 검증 (client_pack_policy.md §7)"
