#!/usr/bin/env bash
# ==========================================================
# 포로 서버 월드 초기 생성 스크립트
# ==========================================================
# 사용법:
#   서버를 먼저 켜고, 콘솔에서 아래 명령어를 순서대로 붙여넣기
#   또는: bash setup-worlds.sh  →  명령어 목록 출력
#
# 각 월드는 -t flat 옵션으로 평지 월드로 생성됨
# worlds.yml에 auto-load: false 설정이 사전 구성되어 있음
# → 생성 완료 후 worlds.yml에서 auto-load: true 로 변경
# ==========================================================

COMMANDS=(
  "# ── 1단계: 평지 월드 생성 ──────────────────────────"
  "mv create world_main normal -t flat"
  "mv create world_boss normal -t flat"
  "mv create world_test normal -t flat"

  "# ── 2단계: world_main 설정 (필드/훈련장) ──────────"
  "mv modify set difficulty normal world_main"
  "mv modify set pvp false world_main"
  "mv modify set monsters false world_main"
  "mv modify set animals false world_main"
  "mv modify set gamemode survival world_main"
  "mv modify set hunger true world_main"

  "# ── 3단계: world_boss 설정 (보스 인스턴스) ────────"
  "mv modify set difficulty hard world_boss"
  "mv modify set pvp false world_boss"
  "mv modify set monsters false world_boss"
  "mv modify set animals false world_boss"
  "mv modify set gamemode survival world_boss"
  "mv modify set weather false world_boss"

  "# ── 4단계: world_test 설정 (개발/테스트) ──────────"
  "mv modify set difficulty peaceful world_test"
  "mv modify set pvp true world_test"
  "mv modify set monsters false world_test"
  "mv modify set animals false world_test"
  "mv modify set gamemode creative world_test"
  "mv modify set flight true world_test"
  "mv modify set hunger false world_test"

  "# ── 5단계: 스폰 위치 확인 후 필요 시 조정 ─────────"
  "# (각 월드에 tp 후 /mv setspawn 으로 스폰 포인트 지정)"
  "# mv tp world_main"
  "# mv setspawn"
)

echo ""
echo "======================================================"
echo " 포로 서버 월드 생성 명령어"
echo " 서버 콘솔에 아래 명령어를 순서대로 입력하세요"
echo "======================================================"
echo ""
for cmd in "${COMMANDS[@]}"; do
  echo "$cmd"
done
echo ""
echo "======================================================"
echo " 생성 완료 후: worlds.yml에서 각 월드의"
echo " auto-load: false → auto-load: true 로 변경"
echo "======================================================"
