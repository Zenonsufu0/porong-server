# 설계 결정 로그 (Decision Log)

> **[STATUS: DRAFT]** — PHASE 2~4 수정 작업 기록. 각 항목은 "무엇을 / 왜 / 근거 문서" 형식.

---

## 2026-05-22 — PHASE 2 canon 충돌 수정

### DL-001 Citizens 플러그인 제거

**파일:** `docs/01_plugin_architecture/index.md`  
**변경:** 플러그인 목록 테이블에서 `Citizens | NPC 껍데기` 행 제거  
**이유:** `final_master_plan.md §1` 플러그인 스택에서 Citizens 제거 확정. NPC 역할은 EmpireRPG 자체 처리로 전환.  
**근거:** `final_master_plan.md §1` (2026-05-20 기준)

---

### DL-002 마력 과부하 채팅 메시지 제거

**파일:** `docs/04_combat_weapon_skills/index.md`  
**변경:** 채팅/알림 포맷 테이블에서 `마력 과부하`, `마력 과부하 반복` 행 제거  
**이유:** 마력 시스템(발전기·마력 소비 구조) 2026-05-19 전면 폐지 확정.  
**근거:** `final_master_plan.md §8` + `economy_numbers_v2.md §2` (2026-05-19 설계 변경 주석)

---

### DL-003 무기 이름 망치 → 도끼

**파일:**  
- `docs/04_combat_weapon_skills/weapon_skills_v1.md` (전체 표시명 치환)  
- `docs/04_combat_weapon_skills/index.md` (무기 클래스 테이블, GUI 레이아웃, 아이콘 테이블)  
**변경:** 표시명 "망치" → "도끼" (YAML 코드 식별자 `hammer`는 구현 로직이므로 유지)  
**이유:** `final_master_plan.md §5` 무기 6종 정의에서 "도끼"로 확정됨. `weapon_skills_v1.md §2` 추천 아이템도 `minecraft:netherite_axe`로 이미 기재됨.  
**근거:** `final_master_plan.md §5` (2026-05-20 기준)

---

### DL-004 도끼 베이스 아이템 NETHERITE_PICKAXE → NETHERITE_AXE

**파일:** `docs/04_combat_weapon_skills/index.md`  
**변경:** 무기 선택창 아이콘 테이블에서 도끼 베이스 아이템을 `NETHERITE_PICKAXE` → `NETHERITE_AXE`로 수정  
**이유:** `weapon_skills_v1.md §2`에 "추천 베이스 아이템: `minecraft:netherite_axe`"로 명시됨. index.md의 `NETHERITE_PICKAXE`는 이전 망치 기반 시절의 잔재.  
**근거:** `weapon_skills_v1.md §2` + `final_master_plan.md §5`

---

### DL-005 강화 비용표 전면 교체

**파일:** `docs/02_database_api_stats/economy_numbers_v2.md`  
**변경:** `### 강화 비용표` 섹션 전체를 `final_master_plan §7.3` (M-3 확정 2026-05-20) 기준으로 교체  
**상세 변경:**
- 골드 비용: 1강 180G → **2,000G**, 22강+ 25,000G → **27,000G 고정**
- 강화석 파편 시스템 폐지 → 강화석 직접 소모 (M-5 확정)
- 보조재 B/C 시스템 폐지 → 강화 흔적 3종(별/달/태양) 시스템 (M-3 확정)
- 방어구 강화석 비율: 무기의 50% → `ceil(무기 강화석 ÷ 1.5)` (약 67%, 확정 2026-05-20)
- 구 계산 테이블(강화석 파편 소모량, 보조재 소모량)은 구버전 표기로 인라인 주석 처리

**이유:** economy_numbers_v2가 2026-05-15 기준 수치이고, final_master_plan §7.3은 2026-05-20 M-3 확정 기준. 최신 확정이 우선.  
**근거:** `final_master_plan.md §7.3` M-3 (2026-05-20)

---

### DL-006 마력 관련 항목 제거 (economy_numbers_v2)

**파일:** `docs/02_database_api_stats/economy_numbers_v2.md`  
**변경:**
- `§8 병목 구간` 테이블에서 "발전기 마력 부족" 행 제거
- `§7 v2 신규 체크포인트` 테이블에서 "마력 과부하 발생 비율" 행 제거
- `§10 영지 편의 해금` 테이블에서 "발전기 효율 업그레이드 +5 MP/h" 행 제거
**이유:** 마력 시스템 2026-05-19 전면 폐지.  
**근거:** `final_master_plan.md §8` + `economy_numbers_v2.md §2` (2026-05-19 주석)

---

## 2026-05-22 — PHASE 4 final_master_plan 재정리

### DL-007 final_master_plan.md 2,341줄 → 601줄 축소

**파일:** `docs/final_master_plan.md`  
**변경:** 세부 수치·슬롯 매핑·레시피 체인·확정 완료 M-tags를 "→ 상세: X 참조" 형태로 위임.  
**제거 및 위임 목록:**
- §5.4 무기 6종 스킬 상세 (~96줄) → `04_combat_weapon_skills/weapon_skills_v1.md`
- §5.5~5.6 장비 포맷·시작 장비 (~130줄) → `01_plugin_architecture/implementation_reference.md`
- §6.3 시스템 메시지 포맷 (~15줄) → `01_plugin_architecture/CANON.md`
- §6.4 상점 상세 (~90줄) → `02_database_api_stats/CANON.md`
- §7.2 잠재 등급 상세 표 (~70줄) → `02_database_api_stats/potential_options_v1.md`
- §7.3 강화 비용표 전체 (~55줄) → `02_database_api_stats/economy_numbers_v2.md`
- §8.2 작위별 상세 표 (~29줄) → `05_island_farm_system/CANON.md`
- §8.5 공방 레시피 체인 (~130줄) → `05_island_farm_system/workshop_crafting_spec.md`
- §8.7 광물 생성기 확률표 (~30줄) → `05_island_farm_system/CANON.md`
- §8.8 엘리베이터 구현 코드 (~15줄) → `01_plugin_architecture/implementation_reference.md`
- §14.1.1~14.4 GUI 슬롯 매핑 전체 (~970줄) → `08_resourcepack_pipeline/gui_*.md`
- §18 확정 완료 M-tags (~10줄) → 한 줄 요약으로 축약
- §20 관리자 커맨드 상세 표 (~85줄) → `01_plugin_architecture/admin_command_spec.md`
**추가 수정:** §6.4 장비 이름 변경권 가격 300,000G → **10,000G** (DL-007 충돌 해소, M-11 확정 반영)  
**이유:** final_master_plan을 프로젝트 철학·핵심 방향성·시스템 연결 중심 문서로 전환. 세부 내용은 전용 시스템 문서(CANON.md, 시스템 스펙 문서)에서 관리.  
**근거:** `docs/master_plan_content_audit.md` 분류 기준

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
**근거:** `final_master_plan.md` §2 공식 문서 구조.

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
