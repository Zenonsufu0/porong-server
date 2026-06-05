# PoroMon override — Eggs Addon 방랑상인 스폰 비활성 (결정 027)
#
# 원본 diesse:egg/villager_spawn (알을 바닐라 화폐로 파는 방랑상인 소환)을
# 빈 함수로 대체한다. diesse:egg/main 이 24000틱마다 이 함수를 계속 호출하나
# 내용이 없으므로 상인은 스폰되지 않는다.
#
# 알은 "야생 구매" 불가 → PoroMonCore 골드 상점에서 골드 차감 후
# function diesse:egg/give/<common|rare|shiny> 호출로만 지급(골드 단일 경제, 결정 014/024/027).
#
# (의도적으로 명령 없음. 둥지 자연 스폰 egg/nest/all 은 별도 — 필요 시 추가 통제.)
