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
