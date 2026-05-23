# 설계 결정 로그 (Decision Log)

> **[STATUS: DRAFT]** — PHASE 2~4 수정 작업 기록. 각 항목은 "무엇을 / 왜 / 근거 문서" 형식.

---

### DL-027 운영자 웹 대시보드 도메인 신설

**결정:** `docs/11_web_dashboard/` 폴더를 신설하고 운영자 전용 웹 대시보드 설계 문서 4종을 작성한다.

**변경:**
- `docs/11_web_dashboard/index.md` 신규: 전체 구조 개요, 페이지 목록, 기술 스택
- `docs/11_web_dashboard/web_dashboard_spec.md` 신규: 페이지별 레이아웃·필터·표시 데이터 상세
- `docs/11_web_dashboard/api_endpoints.md` 신규: 대시보드 호출 API 엔드포인트 목록
- `docs/11_web_dashboard/db_event_log_spec.md` 신규: 경제·전투 이벤트 로그 DB 테이블 설계
- `docs/final_master_plan.md`: 공식 문서 구조 표에 도메인 11 추가, §11 데이터와 API에 웹 대시보드 설명 추가

**이유:** 45일 시즌 서버에서 경제 이상·보스 밸런스를 운영 중 감지하고 근거 있는 너프/버프 판단을 하기 위해 운영자 관제 도구가 필요하다.

**핵심 결정 내용:**
- 접근: 운영자 전용, Bearer 토큰 인증
- 데이터 원본: EmpireRPG HTTP API(포트 8765) 단일 경로. DB 직접 접근 없음
- 갱신: 일별 집계(자정 스냅샷). 실시간은 현재 접속자·최근 보스 클리어만
- 1차 범위: 경제 관제, 보스 기록, 서버 현황, 아이템 발행 수
- 기존 `empire.db` 테이블 수정 없음. 이벤트 로그 테이블 7종 신규 추가만

**근거:** 사용자 요구사항 (2026-05-23)

---

### DL-026 날개 치장 1차 시즌 포함 확정

**결정:** 날개 치장(wing_volt_poro_01)을 1차 시즌 리소스팩 범위에 포함한다.

**변경:**
- `final_master_plan.md` 리소스팩 우선순위에서 "날개 치장 제외" → "날개 치장 1종 포함"으로 수정
- CMD 500xxx 범위를 코스메틱 전용으로 신설 (500001~500099: 날개)
- `paper.json` CMD 500001 = `poro:item/cosmetics/wings/wing_volt_poro_01` 등록
- `assets/source/items/cosmetics/cosmetics_registry.yml` 신규 생성
- 코스메틱 아이템 lore에 "장착 부위: XXX" 명시 + 플러그인 슬롯 차단 규칙 확정

**이유:** 모델·텍스처가 이미 완성돼 있어 추가 비용 없이 등록 가능. 1차 오픈 콘텐츠 다양성 확보.
**근거:** 사용자 확인 (2026-05-23)

---

## 2026-05-22 — PHASE 2 canon 충돌 수정

### DL-001 Citizens 플러그인 제거

**파일:** `docs/01_plugin_architecture/index.md`  
**변경:** 플러그인 목록 테이블에서 `Citizens | NPC 껍데기` 행 제거  
**이유:** `final_master_plan.md`의 "플러그인 구조"에서 Citizens 제거가 확정됨. NPC 역할은 EmpireRPG 자체 처리로 전환.  
**근거:** `final_master_plan.md`의 "플러그인 구조" (2026-05-20 기준)

---

### DL-002 마력 과부하 채팅 메시지 제거

**파일:** `docs/04_combat_weapon_skills/index.md`  
**변경:** 채팅/알림 포맷 테이블에서 `마력 과부하`, `마력 과부하 반복` 행 제거  
**이유:** 마력 시스템(발전기·마력 소비 구조) 2026-05-19 전면 폐지 확정.  
**근거:** `final_master_plan.md`의 "개인 영지" + `economy_numbers_v2.md`의 마력 시스템 폐지 주석

---

### DL-003 무기 이름 망치 → 도끼

**파일:**  
- `docs/04_combat_weapon_skills/weapon_skills_v1.md` (전체 표시명 치환)  
- `docs/04_combat_weapon_skills/index.md` (무기 클래스 테이블, GUI 레이아웃, 아이콘 테이블)  
**변경:** 표시명 "망치" → "도끼" (당시 YAML 코드 식별자 `hammer`는 구현 로직으로 유지했으나, DL-025에서 `axe`로 전환 확정)
**이유:** `final_master_plan.md`의 "전투와 장비 성장"에서 무기 6종 중 "도끼"로 확정됨. `weapon_skills_v1.md`의 도끼 항목도 `minecraft:netherite_axe`를 추천 베이스 아이템으로 기재함.  
**근거:** `final_master_plan.md`의 "전투와 장비 성장" (2026-05-20 기준)

---

### DL-004 도끼 베이스 아이템 NETHERITE_PICKAXE → NETHERITE_AXE

**파일:** `docs/04_combat_weapon_skills/index.md`  
**변경:** 무기 선택창 아이콘 테이블에서 도끼 베이스 아이템을 `NETHERITE_PICKAXE` → `NETHERITE_AXE`로 수정  
**이유:** `weapon_skills_v1.md`의 도끼 항목에 "추천 베이스 아이템: `minecraft:netherite_axe`"로 명시됨. index.md의 `NETHERITE_PICKAXE`는 이전 망치 기반 시절의 잔재.  
**근거:** `weapon_skills_v1.md`의 도끼 항목 + `final_master_plan.md`의 "전투와 장비 성장"

---

### DL-005 강화 비용표 전면 교체

**파일:** `docs/02_database_api_stats/economy_numbers_v2.md`  
**변경:** `### 강화 비용표` 섹션 전체를 `final_master_plan.md`의 "전투와 장비 성장" 및 "미확정 항목" 확정 내용 기준으로 교체  
**상세 변경:**
- 골드 비용: 1강 180G → **2,000G**, 22강+ 25,000G → **27,000G 고정**
- 강화석 파편 시스템 폐지 → 강화석 직접 소모 (M-5 확정)
- 보조재 B/C 시스템 폐지 → 강화 흔적 3종(별/달/태양) 시스템 (M-3 확정)
- 방어구 강화석 비율: 무기의 50% → `ceil(무기 강화석 ÷ 1.5)` (약 67%, 확정 2026-05-20)
- 구 계산 테이블(강화석 파편 소모량, 보조재 소모량)은 구버전 표기로 인라인 주석 처리

**이유:** economy_numbers_v2가 2026-05-15 기준 수치이고, `final_master_plan.md`의 "전투와 장비 성장"은 2026-05-20 M-3 확정 기준. 최신 확정이 우선.  
**근거:** `final_master_plan.md`의 "전투와 장비 성장" 및 `docs/02_database_api_stats/CANON.md`

---

### DL-006 마력 관련 항목 제거 (economy_numbers_v2)

**파일:** `docs/02_database_api_stats/economy_numbers_v2.md`  
**변경:**
- `economy_numbers_v2.md`의 병목 구간 테이블에서 "발전기 마력 부족" 행 제거
- `economy_numbers_v2.md`의 v2 신규 체크포인트 테이블에서 "마력 과부하 발생 비율" 행 제거
- `economy_numbers_v2.md`의 영지 편의 해금 테이블에서 "발전기 효율 업그레이드 +5 MP/h" 행 제거
**이유:** 마력 시스템 2026-05-19 전면 폐지.  
**근거:** `final_master_plan.md`의 "개인 영지" + `economy_numbers_v2.md`의 마력 시스템 폐지 주석

---

## 2026-05-22 — PHASE 4 final_master_plan 재정리

### DL-007 final_master_plan.md 2,341줄 → 601줄 축소

**파일:** `docs/final_master_plan.md`  
**변경:** 세부 수치·슬롯 매핑·레시피 체인·확정 완료 M-tags를 "→ 상세: X 참조" 형태로 위임.  
**제거 및 위임 목록:**
- 무기 6종 스킬 상세 → `04_combat_weapon_skills/weapon_skills_v1.md`
- 장비 포맷·시작 장비 → `01_plugin_architecture/implementation_reference.md`
- 시스템 메시지 포맷 → `01_plugin_architecture/CANON.md`
- 상점 상세 → `02_database_api_stats/CANON.md`
- 잠재 등급 상세 표 → `02_database_api_stats/potential_options_v1.md`
- 강화 비용표 전체 → `02_database_api_stats/economy_numbers_v2.md`
- 작위별 상세 표 → `05_island_farm_system/CANON.md`
- 공방 레시피 체인 → `05_island_farm_system/workshop_crafting_spec.md`
- 광물 생성기 확률표 → `05_island_farm_system/CANON.md`
- 엘리베이터 구현 코드 → `01_plugin_architecture/implementation_reference.md`
- GUI 슬롯 매핑 전체 → `08_resourcepack_pipeline/gui_*.md`
- 확정 완료 M-tags → 각 도메인 문서와 `decision_log.md`에 흡수
- 관리자 커맨드 상세 표 → `01_plugin_architecture/admin_command_spec.md`
**추가 수정:** 장비 이름 변경권 가격 300,000G → **10,000G** (DL-007 충돌 해소, M-11 확정 반영)  
**이유:** final_master_plan을 프로젝트 철학·핵심 방향성·시스템 연결 중심 문서로 전환. 세부 내용은 전용 시스템 문서(CANON.md, 시스템 스펙 문서)에서 관리.  
**근거:** `docs/_archive/master_plan_content_audit.md` 분류 기준

---

## 2026-05-22 — PHASE 5 docs 리빌드 검수 반영

### DL-008 archive 구조 확정

**파일:**
- `docs/_archive/README.md`
- `docs/_archive/docs_restructure_plan.md`
- `docs/_archive/master_plan_content_audit.md`
- `docs/_archive/11_remaining_decisions/index.md`

**변경:** 활성 docs 루트에 남아 있던 리빌드 계획/감사/구 결정 문서를 `docs/_archive/`로 이동하고, README에 archive 이유와 대체 문서를 기록.  
**이유:** 실행 완료된 계획 문서와 흡수된 결정 문서가 활성 기준 문서처럼 보이는 문제 제거.  
**근거:** docs 리빌드 검수 기준 — archive 폴더는 `docs/_archive/`로 통일.

---

### DL-009 구현 레퍼런스 경로 확정

**파일:** `docs/01_plugin_architecture/implementation_reference.md`  
**변경:** 기존 루트 `docs/final_설계_plan.md`를 플러그인 아키텍처 하위 구현 레퍼런스로 이동.  
**이유:** `final_master_plan.md`과 각 CANON에서 이미 구현 상세를 `01_plugin_architecture/implementation_reference.md`로 위임하고 있었으므로 실제 파일 경로를 참조와 일치시킴.  
**근거:** `docs/01_plugin_architecture/CANON.md`

---

### DL-010 CANON.md 역할 정리

**파일:**
- `docs/01_plugin_architecture/CANON.md`
- `docs/02_database_api_stats/CANON.md`
- `docs/04_combat_weapon_skills/CANON.md`
- `docs/05_island_farm_system/CANON.md`
- `docs/06_fields_bosses/CANON.md`

**변경:** TODO/잔존 충돌 문구를 제거하고, 공식 기준·참조 우선순위·충돌 처리 방식만 남김. 잘못된 섹션 번호와 이동 예정 문구도 정리.  
**이유:** CANON 문서가 작업 체크리스트가 아니라 현재 공식 기준 역할을 해야 함.  
**근거:** `final_master_plan.md`의 "공식 문서 구조".

---

### DL-011 Status 태그와 참조 경로 통일

**파일:** `docs/**/*.md`, `CLAUDE.md`  
**변경:** 주요 문서 상단 Status를 `[STATUS: CANON|REFERENCE|DRAFT|ARCHIVED]` 형식으로 통일. 구 GUI 경로를 `docs/08_resourcepack_pipeline/`로 수정. 존재하지 않는 경제 검토 문서 참조 제거.  
**이유:** 문서 역할과 현재 docs 구조를 명확히 하고 깨진 참조를 제거.  
**근거:** docs 리빌드 검수 기준 — Status 태그 누락, CLAUDE/AGENTS 참조 경로 정합성.

---

### DL-012 final_master_plan 재축약

**파일:** `docs/final_master_plan.md`  
**변경:** 구현 절차, 상세 수치표, 슬롯 배치, API 상세, 관리자 커맨드 상세를 하위 문서로 위임하고 원칙/방향성/도메인 진입점 중심으로 재구성.  
**이유:** `final_master_plan.md`가 너무 많은 세부사항을 보유하면 각 CANON.md와 하위 문서가 공식 기준으로 기능하기 어렵다.  
**근거:** `docs/_archive/master_plan_content_audit.md`

---

### DL-013 AI orchestra workflow 명령어 통일

**파일:**
- `CLAUDE.md`
- `AGENTS.md`
- `scripts/orchestra.sh`

**변경:** AI 작업 루프의 공식 명령어를 `orc`로 통일하고, main → review는 `orc handoff-main` / `orc to-review`, review → main은 `orc handoff-review` / `orc to-master` 흐름으로 정리. `handoff-*` 명령은 현재 worktree 변경사항을 `add -A` 후 commit하고 대응하는 동기화 명령을 실행하는 것으로 정의. `to-review` / `to-master`는 양쪽 worktree가 dirty 상태이면 중단하는 안전 동기화 명령으로 명시.
**이유:** Claude(main/master)와 Codex(review/codex-review)의 역할을 분리하면서도 handoff 명령과 legacy alias 설명이 혼동되지 않도록 공식 workflow를 단일 명령 체계로 정리하기 위함.
**근거:** `CLAUDE.md`와 `AGENTS.md`의 작업 루프 명령어 섹션, `scripts/orchestra.sh`

---

## 2026-05-22 — 미확정 M-tag 확정 처리

### DL-014 M-6 확정 — 강화 흔적 3종 아이템 정의·수급 경로

**파일:**
- `docs/final_master_plan.md` (§13 미확정 항목에서 제거)
- `docs/02_database_api_stats/CANON.md` (흔적 수급 경로 추가)
- `docs/05_island_farm_system/CANON.md` (공방 가공기 제작 대상 명시)

**변경:**
- 별의 흔적 / 달의 흔적 / 태양의 흔적 = **강화 성공률 보정 아이템**으로 정의
- 수급 경로: **영지 공방 가공기에서 제작 가능** (레시피 미확정 — 사용자 확정 필요)
- economy_numbers_v2.md 강화표의 "강화 흔적 (선택)" 열과 정합성 확보

**이유:** 21강 이상 강화 진행 시 흔적 소모가 필수인데 수급 경로가 미확정이면 강화 진행 자체가 막히는 블로커였음. M-2 서브 에이전트 분석에서도 "성공률 수치보다 흔적 수급이 더 큰 병목"으로 지적됨.  
**근거:** 사용자 확인 (2026-05-22)

---

### DL-015 M-4 확정 — 전승권 비용

**파일:**
- `docs/final_master_plan.md` (§13 미확정 항목에서 제거)
- `docs/02_database_api_stats/CANON.md` (전승권 비용 추가)

**변경:**
- 기본 전승: **0G (무료)**
- 등급전승권: **100,000G**
- 세부스탯전승권: **100,000G**

**이유:** 오픈 후 7~14일차 시세 확인 후 결정 예정이었으나 기본 전승 무료 + 등급/세부스탯 전승권 각 100,000G로 사전 확정.  
**근거:** 사용자 확인 (2026-05-22)

---

## 2026-05-22 — PHASE 7 큐브 비용 하향 및 문서 정리

### DL-016 큐브 1회 비용 5,000G → 500G

**파일:**
- `docs/02_database_api_stats/CANON.md`
- `docs/02_database_api_stats/economy_numbers_v2.md`

**변경:**
- 큐브 1회 사용 골드 비용: **5,000G → 500G**
- 선발대 기준 일 150회 사용 (75,000G/일 소모) 기준으로 추정치 재산정

**이유:** 큐브를 일상적인 골드 소모처로 활용. 5,000G는 선발대도 하루 0.4회 수준으로 잠재 성장 체감이 너무 낮았음.  
**근거:** 사용자 확인 (2026-05-22)

---

### DL-017 enhancement_droprate_v1.md 아카이브

**파일:**
- `docs/02_database_api_stats/enhancement_droprate_v1.md` → `docs/_archive/enhancement_droprate_v1.md`
- `docs/_archive/README.md` (PHASE 6 항목 추가)

**변경:** enhancement_droprate_v1.md를 _archive로 이동

**이유:** 강화석 파편 시스템 기반 드랍률 계산 문서. 파편 시스템 폐지(강화석 직접 드랍/소모)로 계산 방식 전체 무효.  
**근거:** 강화석 파편 시스템 폐지 확정 (DL-005 참조)

---

### DL-018 economy_numbers_v2.md 영지·공방 섹션 제거

**파일:** `docs/02_database_api_stats/economy_numbers_v2.md`

**변경:**
- §2 영지 시설 슬롯 구성, §3 공방 가공기 레시피, §4 공방 대기열 한도 전체 삭제
- 해당 내용은 `05_island_farm_system/island_system_design.md`, `05_island_farm_system/workshop_crafting_spec.md`가 권위 있는 최신 버전으로 유지
- economy_numbers_v2는 강화·큐브·경제 분석 전용 문서로 범위 축소
- §1 작위 구매 비용 재료 컬럼을 전장의 파편 기반으로 수정 (island_system_design.md 기준)
- 폐지된 마력 결정, 자동재배기, 전투 식량, 구버전 공명 추출기 잔존 참조 일괄 제거

**이유:** 같은 내용이 두 문서에 있되 economy 문서가 구버전이면 반드시 혼동 발생. AI 에이전트가 구버전을 현재 기준으로 읽는 오류 재발 방지.  
**근거:** 사용자 지시 (2026-05-22)

---

## 2026-05-22 — PHASE 1 문서 통폐합 기준 확정

### DL-019 문서 정리 기준과 폐기 설계 목록 확정

**파일:**
- `docs/final_master_plan.md`
- `docs/02_database_api_stats/CANON.md`
- `docs/04_combat_weapon_skills/CANON.md`
- `docs/05_island_farm_system/CANON.md`
- `docs/06_fields_bosses/CANON.md`
- `docs/08_resourcepack_pipeline/index.md`

**변경:**
- 문서 정리 기준을 `final_master_plan.md` / 각 `CANON.md` / 최신 DL 항목 우선으로 고정
- archive 문서는 과거 맥락 추적용이며 현재 구현·수치·GUI 기준으로 사용하지 않는다고 명시
- 폐기된 구버전 설계 목록을 명시: 마력/발전기, 강화석 파편, 큐브 5,000G, 전승권 5,000G/50,000G, 망치 표시명, 도감/컬렉션 1차 포함, 시즌보스 주간 입장 제한, 고퀄 외부 보스 모델 1차 적용
- archive 이동된 `enhancement_droprate_v1.md` 활성 참조를 제거하고, 드랍률 재정리 기준을 `drop_tables_v1.md`로 이동
- 리소스팩 1차 우선순위에서 도감 GUI를 제외 항목으로 정리

**이유:** 통폐합/archive 전환 전에 어떤 문서가 현재 기준이고 어떤 구버전 설계를 폐기해야 하는지 먼저 고정해야 후속 archive 이동 시 기준 충돌이 재발하지 않음.
**근거:** 2026-05-22 문서 모순 리뷰 결과 및 사용자 지시

---

## 2026-05-22 — PHASE 2 구버전 GUI 문서 archive

### DL-020 구버전 GUI 상세 문서 3종 archive

**파일:**
- `docs/08_resourcepack_pipeline/gui_functional_specs.md` → `docs/_archive/gui_functional_specs.md`
- `docs/08_resourcepack_pipeline/gui_territory_status.md` → `docs/_archive/gui_territory_status.md`
- `docs/08_resourcepack_pipeline/gui_boss_info.md` → `docs/_archive/gui_boss_info.md`
- `docs/08_resourcepack_pipeline/index.md`
- `docs/08_resourcepack_pipeline/gui_todo_list.md`
- `docs/05_island_farm_system/CANON.md`
- `docs/05_island_farm_system/index.md`
- `docs/04_combat_weapon_skills/index.md`
- `docs/_archive/README.md`

**변경:**
- 마력/발전기, 강화석 파편, 큐브 5,000G, 전승권 구가격, 구 보스 보상 기준이 섞인 GUI 상세 문서 3종을 archive로 이동
- 활성 문서의 해당 링크를 제거하고 "현행 기준 재작성 필요" 상태로 표시
- archive README에 이동 이유와 대체 기준 문서를 기록

**이유:** 구버전 GUI 문서가 현재 CANON보다 상세해서 AI 에이전트가 구버전 수치와 흐름을 구현 기준으로 오인할 위험이 큼.
**근거:** PHASE 1 폐기 설계 목록, 2026-05-22 문서 모순 리뷰 결과

---

## 2026-05-22 — PHASE 3 활성 상세 문서 통폐합

### DL-021 경제·성장·드랍·GUI 허브 활성 문서 기준 정리

**파일:**
- `docs/02_database_api_stats/economy_numbers_v2.md`
- `docs/02_database_api_stats/equipment_growth_spec.md`
- `docs/02_database_api_stats/potential_options_v1.md`
- `docs/06_fields_bosses/drop_tables_v1.md`
- `docs/08_resourcepack_pipeline/gui_hub_structure.md`
- `docs/08_resourcepack_pipeline/gui_shop.md`

**변경:**
- `economy_numbers_v2.md`는 강화 비용·큐브 비용·골드 경제 지표 문서로 범위를 좁히고, 잠재 확률 세부 기준은 `potential_options_v1.md`로 위임
- 구 방어구 50% 강화석 비율, 강화석 파편 경매 표현, 자동심기 300,000G 잔재를 현재 기준으로 정리
- `equipment_growth_spec.md`의 큐브 비용을 500G로, 전승권 비용을 DL-015 기준으로 정리
- `drop_tables_v1.md`의 주간 첫 클리어/반복 보상 구조를 구버전 초안으로 표시하고, 큐브 비용과 균열왕 심장 용도에서 구버전 표기를 제거
- GUI 허브/상점 문서에서 마력 잔량, 강화석 제작, 강화석 파편, 자동심기 50,000G 잔재를 현재 기준으로 정리

**이유:** archive하지 않고 유지할 활성 상세 문서가 CANON보다 오래된 수치·흐름을 포함하면 통폐합 후에도 구현 기준 혼선이 계속 발생함.
**근거:** PHASE 1 폐기 설계 목록, DL-015, DL-016, DL-018

---

## 2026-05-22 — PHASE 4 구현 레퍼런스 재축약

### DL-022 implementation_reference.md 장문 구버전 archive

**파일:**
- `docs/01_plugin_architecture/implementation_reference.md` → `docs/_archive/implementation_reference_legacy.md`
- `docs/01_plugin_architecture/implementation_reference.md` (현재 기준 진입점으로 재작성)
- `docs/_archive/README.md`

**변경:**
- 1,500줄 이상의 장문 구현 레퍼런스를 archive로 이동
- 활성 `implementation_reference.md`는 현재 CANON 기준, 구현 상세 진입점, 확정 구현 기준, 금지된 구버전 기준만 담는 얇은 문서로 재작성
- archive README에 이동 이유와 대체 기준 문서를 기록

**이유:** 기존 구현 레퍼런스에 큐브 5,000G, 전승권 구가격, M-tag 미정, 구 GUI/보상 기준이 섞여 있어 AI 에이전트가 현재 CANON보다 구버전 구현 메모를 우선할 위험이 큼.
**근거:** PHASE 1 폐기 설계 목록, PHASE 3 활성 상세 문서 정리 결과

---

## 2026-05-22 — PHASE 5 활성 문서 잔여 구버전 키워드 정리

### DL-024 강화 흔적 3종 제작 레시피 및 효과 확정

**파일:**
- `docs/05_island_farm_system/workshop_crafting_spec.md` (§9 레시피 전면 교체, `tab_trace` 신설)
- `docs/05_island_farm_system/CANON.md` (레시피 확정 명시)
- `docs/idea_inbox.md` (INBOX-001 PROMOTED)

**변경:**
- 별의 흔적 (`mat_trace_star`): 강화 성공률 **+20%p**, 광부의 정수×1 + 다이아블럭×2 + 에메랄드블럭×2
- 달의 흔적 (`mat_trace_moon`): 강화 성공률 **+30%p**, 광부의 정수×2 + 다이아블럭×4 + 에메랄드블럭×4
- 태양의 흔적 (`mat_trace_sun`): 강화 성공률 **+50%p**, 광부의 정수×3 + 다이아블럭×8 + 에메랄드블럭×8
- 공방 탭 `tab_trace` 신설 (기존 §9 "공방 제작 탭 내 포함" 표현 교체)
- 마도합금(`mat_mado_alloy`) 용도: 구 강화 흔적 재료 아님 → 미확정

**이유:** 강화 흔적 레시피가 INBOX-001로만 남아 있어 구현 블로커. 사용자 확정으로 레시피와 효과 수치 동시 확정.  
**근거:** 사용자 확인 (2026-05-22)

---

### DL-023 활성 문서 최종 스캔 반영

**파일:**
- `docs/03_discord_onboarding_bot/index.md`
- `docs/07_boss_pattern_modules/season_boss_patterns.md`
- `docs/01_plugin_architecture/empire_rpg_design_intent.md`
- `docs/01_plugin_architecture/empire_rpg_module_design.md`
- `docs/04_combat_weapon_skills/combat_balance_v2.md`
- `docs/08_resourcepack_pipeline/gui_hud_spec.md`
- `docs/02_database_api_stats/index.md`

**변경:**
- Discord 알림 목록에서 폐지된 마력 과부하 제거
- 보스 패턴/HUD/모듈 설계의 사용자 노출 무기명 `망치`를 `도끼`로 정리
- 강화석 획득량 설명에서 강화석 파편 표기를 제거
- DB/API 통계 개요에서 1차 시즌 제외 대상인 컬렉션 관련 테이블/API/통계 목표 제거
- Citizens/외부 보스 모델 설명을 현재 CANON 기준으로 정리

**이유:** Phase 1~4 이후에도 활성 문서 일부가 구버전 키워드를 현재 구현 기준처럼 포함하고 있었음.
**근거:** Phase 5 활성 문서 스캔 결과

---

## 2026-05-23 — 사용자 확정 반영

### DL-025 강화석 DB 재화 및 `axe` 내부 식별자 확정

**파일:**
- `docs/02_database_api_stats/CANON.md`
- `docs/02_database_api_stats/economy_numbers_v2.md`
- `docs/02_database_api_stats/equipment_growth_spec.md`
- `docs/06_fields_bosses/CANON.md`
- `docs/06_fields_bosses/drop_tables_v1.md`
- `docs/final_master_plan.md`
- `docs/01_plugin_architecture/implementation_reference.md`
- `docs/04_combat_weapon_skills/CANON.md`
- `docs/04_combat_weapon_skills/weapon_skills_v1.md`
- `docs/04_combat_weapon_skills/index.md`
- `docs/08_resourcepack_pipeline/index.md`
- `docs/08_resourcepack_pipeline/gui_todo_list.md`
- `docs/08_resourcepack_pipeline/gui_boss_info.md`

**변경:**
- 강화석은 실물 아이템 드랍이 아니라 DB 가상 재화로 처치 시 직접 적립/소모하는 기준으로 통일
- `mat_stone_enhance` 실물 드랍/소모 표현 제거
- 무기 내부 식별자 `hammer` 유지 방침을 폐기하고 `axe`로 전환
- GUI 세부 설계 상태를 장비 계열 작성완료, 영지·보스·필드 계열 재작성필요로 정리

**이유:** 사용자 확정에 따라 강화석 데이터 모델과 무기 내부 식별자 기준을 명확히 하고, 진행 중인 GUI 상세 설계 상태를 실제 작업 상태와 일치시킴.
**근거:** 사용자 확인 (2026-05-23)

---

### DL-027 GUI 완료 상태 및 공식 배경 4종 확정

**파일:**
- `docs/08_resourcepack_pipeline/index.md`
- `docs/08_resourcepack_pipeline/gui_todo_list.md`
- `docs/08_resourcepack_pipeline/gui_bitmap_spec.md`
- `docs/08_resourcepack_pipeline/gui_png_make_guide.md`
- `docs/08_resourcepack_pipeline/gui_hub_structure.md`

**변경:**
- 최신 커밋 기준 모든 GUI 설정이 완료된 상태로 정리
- 공식 GUI 배경을 `menu_main.png`, `menu_equipment.png`, `menu_territory.png`, `menu_boss.png` 4개로 확정
- `menu_hub_*` 27슬롯/서브허브 전용 배경은 공식 사용 대상에서 제외
- GUI 글리프 문자를 실제 `gui.json` 기준(``, ``, ``, ``)으로 통일

**이유:** GUI 설정 완료 상태와 공식 배경 4개 사용 방침을 문서 전체에 일관되게 반영하기 위함.
**근거:** 사용자 확인 (2026-05-23)
