#!/usr/bin/env bash
#
# sync-server-mods.sh  ─  [DRAFT / 초안 — 아직 실행하지 말 것]
# ---------------------------------------------------------------------------
# 목적:
#   클라이언트 모드 폴더(modpack/client/mods)에서 "서버용 화이트리스트" jar만
#   .local/server/mods 로 복사한다. 클라 전용 모드는 절대 복사하지 않는다.
#   (서버 실행 폴더 = zenon-mon/.local/server, Git 비추적. RPG DL-130과 동치 표준.)
#
# 분류 근거: docs/01_modpack/server_mod_separation.md (§1+§1b+§1c+§2 = 25개, 결정 044)
#            reports/mod_classification.md (1차 자동) + 도메인 수동 보정.
#
# !!! 초안: DRY_RUN=1 기본값으로 실제 복사는 막혀 있다 !!!
#   - 화이트리스트는 modpack/client/mods 의 "실제 파일명" 기준(정확 매칭).
#   - 모드팩 업데이트로 버전(파일명)이 바뀌면 화이트리스트도 갱신해야 한다.
# ---------------------------------------------------------------------------
set -euo pipefail

# ROOT = Zenon Mon worktree 프로젝트 폴더(이 스크립트 상위). 환경변수로 덮어쓸 수 있다.
# 스크립트 위치에서 도출하므로 워크트리/폴더명이 바뀌어도 깨지지 않는다.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="${ROOT:-$(dirname "$SCRIPT_DIR")}"
SRC_DIR="${SRC_DIR:-$ROOT/modpack/client/mods}"   # 실제 jar 원본(클라 프로필 복사본)
DEST_DIR="${DEST_DIR:-$ROOT/.local/server/mods}"  # 서버 실행 폴더(.local/server)의 모드 디렉터리
DRY_RUN="${DRY_RUN:-1}"          # 1 = 출력만(기본). 0 = 실제 복사.
INCLUDE_AMBIGUOUS="${INCLUDE_AMBIGUOUS:-0}"  # 1 = §3 애매/공용 후보도 포함.

# ---------------------------------------------------------------------------
# 서버 화이트리스트 (§1+§1b+§1c+§2 = 25개) — 정확 파일명. eggs−1(032)+ZenonMonCore+1(044).
# ---------------------------------------------------------------------------
SERVER_WHITELIST=(
  # §1 서버 필수 (Cobblemon 스택 + 의존)
  "fabric-api-0.116.8+1.21.1.jar"
  "architectury-13.0.8-fabric.jar"
  "owo-lib-0.12.15.4+1.21.jar"
  "Cobblemon-fabric-1.7.3+1.21.1.jar"
  "mega_showdown-fabric-1.8.4+1.7.3+1.21.1.jar"
  "SimpleTMs-fabric-2.3.3.jar"
  "accessories-fabric-1.1.0-beta.53+1.21.1.jar"
  # §1c ZenonMonCore — 자체 빌드 서버 규칙 엔진(결정 044 추가 / eggs 결정 032 제거).
  #   ⚠️ client/mods 사본이 최신 빌드여야 함(빌드 후 갱신). 클라 번들과 동일 해시.
  "zenon-mon-core-0.1.0.jar"
  # §1b Legendary Monuments + 하드 의존 체인 (최종 포함 확정 — 2026-06-05 프로필 추가)
  #   LM 7.8 depends: chipped/cobblefurnies/terrablender. chipped/cobblefurnies→athena,
  #   chipped→resourcefullib 추가 의존. 5종 모두 env="*"(서버 필요).
  "LegendaryMonuments-7.8.jar"
  "chipped-fabric-1.21.1-4.0.2.jar"
  "CobbleFurnies-fabric-1.1.jar"
  "TerraBlender-fabric-1.21.1-4.1.0.8.jar"
  "athena-fabric-1.21.1-4.0.6.jar"
  "resourcefullib-fabric-1.21-3.0.12.jar"
  # §2 서버 권장 (성능/운영 + 의존 lib)
  "lithium-fabric-0.15.1+mc1.21.1.jar"
  "krypton-0.2.8.jar"
  "ferritecore-7.0.3-fabric.jar"
  "Clumps-fabric-1.21.1-19.0.0.1.jar"
  "letmedespawn-1.21.x-fabric-1.5.0.jar"
  "Almanac-1.21.1-2-fabric-1.5.2.jar"
  "netherportalfix-fabric-1.21.1-21.1.3.jar"
  "balm-fabric-1.21.1-21.0.56.jar"
  "bwncr-fabric-1.21.1-3.20.3.jar"
  "OpenLoader-fabric-1.21.1-21.1.5.jar"
  "prickle-fabric-1.21.1-21.1.11.jar"               # OpenLoader 하드 의존(prickle>=21.1.8). §3→서버 필수 재분류.
)

# §3 애매/공용 후보 (기본 제외; INCLUDE_AMBIGUOUS=1 시 추가; 부팅 의존성 경고 시 개별 활성)
# (prickle은 OpenLoader 하드 의존이라 위 SERVER_WHITELIST로 승격)
AMBIGUOUS_WHITELIST=(
  "appleskin-fabric-mc1.21-3.0.6.jar"
  "craftingtweaks-fabric-1.21.1-21.1.7.jar"
  "cloth-config-15.0.140-fabric.jar"
  "bookshelf-fabric-1.21.1-21.1.80.jar"
)

# ---------------------------------------------------------------------------
# 이중 안전장치: 절대 서버로 가면 안 되는 키워드(클라 전용). 오매칭 최종 차단.
# ---------------------------------------------------------------------------
DENY_KEYWORDS=(
  "sodium" "iris" "reeses" "xaero" "fancymenu" "konkrete" "melody"
  "emi" "jei" "jeed" "advancedlootinfo"
  "entity_texture" "entity_model" "entityculling"
  "particular" "particlerain" "visuality" "wakes" "make_bubbles"
  "fallingleaves" "lambdynamic" "ambientenvironment"
  "modmenu" "betteradvancements" "betterpingdisplay" "betterthirdperson"
  "dynamiccrosshair" "shulkerboxtooltip" "tooltipfix" "tipsmod" "stendhal"
  "notenoughanimations" "invmove" "citresewn" "cherishedworlds"
  "craftpresence" "unilib" "bhmenu" "presencefootsteps" "monsters-in-the-closet"
  "language-reload" "enchdesc" "enhanced_attack_indicator"
  "yet_another_config_lib" "yosbr" "swingthrough"
)

is_denied() { local lc="${1,,}"; for d in "${DENY_KEYWORDS[@]}"; do [[ "$lc" == *"$d"* ]] && return 0; done; return 1; }

copy_one() {  # $1 = filename
  local f="$1" src="$SRC_DIR/$1"
  if [[ ! -f "$src" ]]; then echo "  MISSING (소스에 없음): $f"; return; fi
  if is_denied "$f"; then echo "  DENY (클라 전용 키워드 차단): $f"; return; fi
  if [[ "$DRY_RUN" == "1" ]]; then echo "  WOULD COPY: $f"; else cp -v "$src" "$DEST_DIR/"; fi
}

main() {
  echo "=== sync-server-mods.sh (DRAFT)  DRY_RUN=$DRY_RUN  INCLUDE_AMBIGUOUS=$INCLUDE_AMBIGUOUS ==="
  echo "SRC : $SRC_DIR"
  echo "DEST: $DEST_DIR"
  [[ -d "$SRC_DIR" ]] || { echo "ERROR: SRC_DIR 없음"; exit 1; }
  [[ "$DRY_RUN" != "1" ]] && mkdir -p "$DEST_DIR"

  echo "[화이트리스트 §1+§2 = ${#SERVER_WHITELIST[@]}개]"
  for f in "${SERVER_WHITELIST[@]}"; do copy_one "$f"; done

  if [[ "$INCLUDE_AMBIGUOUS" == "1" ]]; then
    echo "[애매/공용 §3 = ${#AMBIGUOUS_WHITELIST[@]}개]"
    for f in "${AMBIGUOUS_WHITELIST[@]}"; do copy_one "$f"; done
  fi

  # ── prune: DEST를 화이트리스트로 미러링. 화이트리스트에 없는 stale jar 제거 ──
  #    (예: 보류된 LegendaryMonuments jar가 이전 동기화로 DEST에 남아 있으면 제거)
  echo "[prune] 화이트리스트에 없는 DEST jar 제거:"
  if [[ -d "$DEST_DIR" ]]; then
    local allowed=(); allowed+=("${SERVER_WHITELIST[@]}")
    [[ "$INCLUDE_AMBIGUOUS" == "1" ]] && allowed+=("${AMBIGUOUS_WHITELIST[@]}")
    local pruned=0
    for j in "$DEST_DIR"/*.jar; do [[ -e "$j" ]] || continue
      local bn keep=0; bn="$(basename "$j")"
      for a in "${allowed[@]}"; do [[ "$bn" == "$a" ]] && { keep=1; break; }; done
      if [[ "$keep" == 0 ]]; then
        pruned=1
        if [[ "$DRY_RUN" == "1" ]]; then echo "  WOULD REMOVE: $bn"; else rm -v "$j"; fi
      fi
    done
    [[ "$pruned" == 0 ]] && echo "  (제거 대상 없음)"
  fi

  echo "[사후 검증] DEST에 DENY 키워드 jar 잔존 검사:"
  if [[ -d "$DEST_DIR" ]]; then
    local bad=0
    for j in "$DEST_DIR"/*.jar; do [[ -e "$j" ]] || continue
      if is_denied "$(basename "$j")"; then echo "  ⚠️ 클라 전용 의심: $(basename "$j")"; bad=1; fi
    done
    [[ "$bad" == 0 ]] && echo "  OK (클라 전용 jar 없음)"
  fi
  echo "=== done. (DRY_RUN=1 이면 실제 복사 없음) ==="
}

main "$@"

# ---------------------------------------------------------------------------
# 사용 예 (아직 실행하지 말 것):
#   DRY_RUN=1 ./scripts/sync-server-mods.sh                 # 미리보기(기본)
#   DRY_RUN=0 ./scripts/sync-server-mods.sh                 # 실제 복사(필수+권장 19)
#   DRY_RUN=0 INCLUDE_AMBIGUOUS=1 ./scripts/sync-server-mods.sh   # 애매 5개 포함
#
# TODO (실행 전 확정):
#   1) 서버 부팅 테스트로 owo/accessories 등 의존성·environment 최종 검증.
#   2) Legendary Monuments 전설 통제 우회 여부 확인 후 유지/게이트 결정.
#   3) 모드팩 업데이트 시 SERVER_WHITELIST 파일명(버전) 갱신.
#   4) ZenonMonCore: client/mods 사본이 최신 빌드본인지 확인(빌드→client/mods 갱신 후 동기화).
# ---------------------------------------------------------------------------
