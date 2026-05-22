# final_master_plan.md 내용 감사 보고서

> **[STATUS: ARCHIVED]** — 내용 분류 감사 문서. 결과가 `../final_master_plan.md`와 각 `CANON.md`에 반영되어 `docs/_archive/`로 이동됨.

> 작성일: 2026-05-22  
> 목적: MASTER_PLAN을 "프로젝트 철학 + 핵심 방향성 + 시스템 연결 개요" 수준으로 축소하기 위한 콘텐츠 분류  
> 현재: 2,341줄 | 목표: 400~500줄  
> **아직 실제 파일 수정 없음 — 분류 보고서 전용**

---

## 분류 범례

| 기호 | 처리 |
|---|---|
| ✅ KEEP | MASTER_PLAN에 반드시 유지 |
| ➡️ MOVE | 시스템 문서로 이동 |
| 🗃️ ARCHIVE | 역사 기록용 유지, 실무 활용 종료 |
| 🔄 CONSOLIDATE | 다른 섹션과 병합 후 축약 |
| ⚠️ CONFLICT | Canon 기준 재정리 필요 |

---

## §0. 한 줄 정의 (1줄) ✅ KEEP

```
포로 서버 1차는 디스코드 유입 제한형, 45일 시즌제 RPG 프로젝트 서버다.
목표: 6무기 전투 + T1 성장 + 필드/보스 + 개인 영지 생산이 하나의 시즌 루프로 안정적으로 돌아가는 것.
```

**판단:** 문서 전체 목적 한 줄 정의. 최우선 유지.

---

## §1. 운영 기본값 (~20줄) ✅ KEEP

- 운영 기간, 서버 버전, 데이터 저장, 후원 정책 표
- 경제 개입 원칙 (3줄)

**판단:** 짧고 핵심. 전부 유지. 이 정보는 어디에도 완전히 위임하기 어렵다.

---

## §2. 디스코드 온보딩 (~50줄)

### §2.1 인증 플로우 코드블럭 ✅ KEEP (축약)

인증 플로우 7단계 순서도 — 핵심 방향성이므로 유지. **단, 1~2줄 요약으로 축약 가능.**

### §2.2 역할 구조 (~10줄) ✅ KEEP

**판단:** 권한 역할 5종 + 칭호 역할 구조. 짧고 핵심.

### §2.3 디스코드 봇 MVP 표 (~10줄) ➡️ MOVE

**이동 대상:** `docs/03_discord_onboarding_bot/index.md`  
**이유:** 봇 기능 목록 상세는 디스코드 문서 소관.

---

## §3. 플러그인 구조 (~50줄)

### §3.1 역할표 (표, ~15줄) ✅ KEEP (간략화)

**판단:** 플러그인 분담 표는 전체 아키텍처 이해에 핵심. 단, Citizens 제거 확정 표기 수정 필요.  
⚠️ **CONFLICT:** 현재 Citizens 없음. 표에 "제거 확정" 표기 필요.

### §3.2 EmpireRPG 부트스트랩 순서 (~25줄) ➡️ MOVE

**이동 대상:** `docs/01_plugin_architecture/CANON.md`  
**이유:** 구현 상세 (Bootstrap 8단계)는 플러그인 아키텍처 문서 소관.  
MASTER_PLAN에는 "EmpireRPG가 8단계 Bootstrap 구조로 초기화된다" 한 줄만 남김.

---

## §4. 월드 구조 (~10줄) ✅ KEEP

월드 4개 표 — 짧고 전체 구조 이해에 필수. 전부 유지.

---

## §5. 전투 시스템 (~470줄)

### §5.1 입력 방식 표 (~15줄) ✅ KEEP

LC/RC/SRC/F 역할 표 + 발동 조건 구현 사양 → **발동 조건 코드블럭은 MOVE.** 역할 표만 유지.

### §5.2 설계 금지 항목 (~5줄) ✅ KEEP

1차 제외 6종 목록. 핵심 방향성.

### §5.3 각인 구조 공통 (~15줄) ✅ KEEP (축약)

소모형/유지형 구분 원칙 2줄 요약 유지.  
`➡️ MOVE` 상세: 스택 수 (무기별 최대 스택), F 소모 방식 상세 → `04_combat_weapon_skills/CANON.md`

### §5.4 무기 6종 스킬 상세 (~220줄) ➡️ MOVE

**이동 대상:** `docs/04_combat_weapon_skills/CANON.md`  
**이유:** 6무기 × 4스킬 × 각인 상세 표 전체. 전투 문서 소관.  
MASTER_PLAN에는 "무기 6종: 검/도끼/창/석궁/낫/스태프, 각 4스킬" 한 줄만 남김.

> 참고: §5.4에는 구현 메모(월영회전 velocity, 석궁·스태프 LC 논타겟)도 포함.  
> 이 메모들은 `docs/01_plugin_architecture/implementation_reference.md`로 이동.

### §5.5 장비 아이템 포맷 & 잠금 규칙 (~110줄)

**§5.5.1 이름·로어 템플릿 + 예시 (~40줄)** ➡️ MOVE  
→ `docs/01_plugin_architecture/CANON.md` 또는 `implementation_reference.md`  
MASTER_PLAN에는 "장비는 이름·강화·등급·고정스텟·부가스텟·잠재·스킬 섹션 구조로 표시된다" 한 줄 유지.

**§5.5.2 잠금 규칙 표 (~10줄)** ✅ KEEP (축약)  
드랍 금지 / 장착 해제 금지 원칙 — 핵심 정책. 유지.

**무기 인식 기준 (PDC + CMD 동시 확인)** ✅ KEEP  
**무기/방어구 베이스 아이템 + CMD 표 (~15줄)** ➡️ MOVE  
→ `docs/04_combat_weapon_skills/CANON.md`  
MASTER_PLAN에는 "베이스 아이템 + PDC + CMD 조합으로 무기 인식" 원칙만 유지.

**바닐라 기능 차단 목록 (~10줄)** ➡️ MOVE → `implementation_reference.md`

**영지 내 스킬 사용 불가 규칙** ✅ KEEP (1줄)

### §5.6 시작 지급 장비 정의 (~25줄) ➡️ MOVE

**이동 대상:** `docs/04_combat_weapon_skills/CANON.md`  
**이유:** 아이템 ID, 스탯 수치 정의는 전투/장비 문서 소관.

### §5.7 피해 계산 공식 (~5줄) ✅ KEEP

수식 한 줄 + 변수 정의. 전체 시스템의 핵심 공식. 유지.

---

## §6. 튜토리얼 & 핫바 (~170줄)

### §6.1 최초 접속 플로우 (~45줄) 🔄 CONSOLIDATE → MOVE

현재 5단계 코드블럭 형식. 전체 플로우 구현 상세는 `implementation_reference.md`로.  
MASTER_PLAN에 "최초 접속 시 튜토리얼 → 무기 선택 → 장비 지급 → 영지 생성" 4줄 요약 유지.

### §6.2 핫바 구성 표 (~10줄) ✅ KEEP

슬롯 9개 구성표. 짧고 핵심.

### §6.3 시스템 메시지 포맷 (~15줄) ➡️ MOVE

**이동 대상:** `docs/01_plugin_architecture/CANON.md`  
**이유:** 접두어 `§8[§e포로§8] ` + 이벤트별 메시지 표는 구현 스펙.

### §6.4 상점 아이템 (~90줄) ➡️ MOVE

**이동 대상:** `docs/02_database_api_stats/CANON.md`  
**이유:**
- 장비 이름 변경권 (300,000G) 1종 → 경제 문서
- 농작물/광물 판매 가격표 → 경제 문서
- **블럭 상점 58종 가격표 전체** — master plan에 있을 이유가 없음. 완전 이동.

---

## §7. 장비 성장 (~190줄)

### §7.1 T1 단일 체계 (~5줄) ✅ KEEP

T2/세트/공용각인 제외 원칙. 핵심 방향성.

### §7.2 잠재능력 등급 (~100줄)

**큐브 작동 방식 (5줄)** ✅ KEEP (축약)  
"큐브 1회 5,000G, 전 라인 재롤 + 등업 시도, 등급 하락 없음" — 핵심 정책.

**메모리얼 시스템 상세 표 (~20줄)** ➡️ MOVE  
→ `docs/02_database_api_stats/CANON.md`

**등업 확률 표 (~10줄)** ➡️ MOVE  
→ `docs/02_database_api_stats/CANON.md`

**라인별 등급 구조 및 이탈 표 (~10줄)** ➡️ MOVE  
→ `docs/02_database_api_stats/CANON.md` (이미 `potential_options_v1.md`에 있음 — **중복**)

**슬롯별 잠재능력 옵션 풀 표 + 등급별 대표 수치 표 (~30줄)** ➡️ MOVE  
→ `docs/02_database_api_stats/potential_options_v1.md` (이미 있음 — **중복**)  
⚠️ **CONFLICT:** 두 문서의 수치가 완전히 일치하는지 검증 필요.

**CSV 코드 메모** ➡️ MOVE → `implementation_reference.md`

### §7.3 강화 테이블 1~25강 전체 (~55줄) ➡️ MOVE

**이동 대상:** `docs/02_database_api_stats/CANON.md`  
⚠️ **CONFLICT/중복:** `economy_numbers_v2.md §0`에 동일 표 존재 + 골드 비용 수치 불일치.  
MASTER_PLAN: 1강 2,000G / economy_numbers_v2: 1강 180G  
→ CANON.md 작성 시 **MASTER_PLAN 기준**으로 통일 (§18 M-3 확정 내용 기준: "1강 2,000G → 10강 20,000G").

**가호 천장 시스템 (~10줄)** ➡️ MOVE → `docs/02_database_api_stats/CANON.md`

**강화 흔적 소모품 표 (~10줄)** ➡️ MOVE → `docs/02_database_api_stats/CANON.md`  
(이미 `economy_numbers_v2.md`에 있음 — **중복**)

MASTER_PLAN에는 "강화 1~25강, 골드+강화석 소모, 천장 시스템 적용(11강+), 파괴/강등 없음" 3~4줄 유지.

---

## §8. 개인 영지 (~370줄)

### §8.1 구조 원칙 (~5줄) ✅ KEEP

IridiumSkyblock = 껍데기, EmpireRPG = 핵심 로직. "스카이블럭" 표현 금지. 핵심 방향성.

### §8.2 영지 작위 8단계 (~40줄) ➡️ MOVE

**이동 대상:** `docs/05_island_farm_system/CANON.md`  
⚠️ **중복:** `island_system_design.md`에도 동일 표 존재.  
MASTER_PLAN에는 "8단계 작위 구매제, 개척지→공작령" 한 줄 유지.

### §8.3 영지 시설 3종 (~70줄)

**시설 배치 방식 설명 (~5줄)** ✅ KEEP (1줄 요약)  
"GUI 슬롯 배정 방식, 물리 블럭 설치 없음" — 핵심 방향성.

**시설 레벨별 산출량 표 (~30줄)** ➡️ MOVE → `docs/05_island_farm_system/CANON.md`  
(이미 `island_system_design.md`에 있음 — **중복**)

**시설 3종 역할 표** ✅ KEEP (축약)  
약초 재배지 / 광물 채굴기 / 공방 가공기 3줄 — 핵심 구성요소.

### §8.4 영지 저장고 (~10줄) ✅ KEEP (축약)

저장고 조작 방식 4줄 — "DB 기반 가상 저장고, 좌클릭 64개 출금" 핵심만 유지.

### §8.5 공방 가공기 탭 (~130줄) ➡️ MOVE

**이동 대상:** `docs/05_island_farm_system/workshop_crafting_spec.md`  
⚠️ **중복:** 레시피 체인이 `workshop_crafting_spec.md`에 이미 있음.  
MASTER_PLAN에는 "공방 6탭 (영지제작/제련/정제/연금술치료/연금술부스트/요리), 삭제된 탭 목록" 5~6줄 유지.

### §8.6 바닐라 농사/채광 (~5줄) ✅ KEEP

짧고 핵심 정책.

### §8.7 기본 광물 생성기 (~35줄) ➡️ MOVE

**이동 대상:** `docs/05_island_farm_system/CANON.md`  
이유: 작위별 확률표 8열 × 8행 상세 수치는 시스템 문서 소관.  
MASTER_PLAN에는 "울타리+빈칸+물 → 작위별 확률로 광물 생성 (BlockFormEvent 가로채기)" 1줄.

### §8.8 파란색 색유리 엘리베이터 (~20줄) ➡️ MOVE

**이동 대상:** `docs/01_plugin_architecture/implementation_reference.md`  
이유: Java 코드 블럭 포함. 명백한 구현 상세.

---

## §9. 필드 & 드랍 (~75줄)

### §9.1 필드 5개 표 ✅ KEEP

5필드 역할표. 핵심 방향성.

### §9.2 드랍 구조 (~15줄) ✅ KEEP (축약)

드랍 원칙 표 (일반몹/정예몹/필드보스 3줄) — 유지.  
"강화석 직접 드랍 확정 (파편 폐지)" — 유지.

### §9.3 IL 시스템 (~20줄) 🔄 CONSOLIDATE

**IL 공식** ✅ KEEP (1줄)  
**필드별 경고 임계 표** ➡️ MOVE → `docs/06_fields_bosses/CANON.md`  
⚠️ **중복:** `numbers_and_open_decisions.md §M-7`에 동일 표 존재.  
"경고 3회 → 화이트리스트 제외" 원칙 ✅ KEEP (1줄)

### §9.4 원샷 방지 (~5줄) ✅ KEEP

85% 클램프 규칙. 핵심 전투 정책.

### §9.5 기여도 보상 (~5줄) ✅ KEEP

3% 기준, 이진 판정 원칙. 유지.

### §9.6 잠수 방지 (~3줄) ✅ KEEP

짧음. 유지.

---

## §10. 필드보스 (~25줄)

### 기본 규칙 표 (~5줄) ✅ KEEP

스폰 주기/소멸/타임아웃/알림 4항목 표. 핵심.

### 필드보스 5종 드랍 확률 표 (~10줄) ➡️ MOVE

**이동 대상:** `docs/06_fields_bosses/drop_tables_v1.md`  
⚠️ **중복:** 이미 `drop_tables_v1.md §3`에 동일 표 존재.  
MASTER_PLAN에는 "5종 보스 목록 + 드랍은 개인 독립 확률" 2줄 유지.

---

## §11. 시즌보스 (~25줄)

### 입장 방식 (~3줄) ✅ KEEP

"입장형/파티형. 주간 횟수 제한 없음." 핵심 방향성.

### 4보스 드랍 표 (~8줄) ➡️ MOVE

**이동 대상:** `docs/06_fields_bosses/CANON.md`  
⚠️ **중복:** `final_설계_plan.md §8.2`에도 동일 표 있음.

### 파티 내부 경매 규칙 (~8줄) ➡️ MOVE

**이동 대상:** `docs/06_fields_bosses/CANON.md` 또는 `implementation_reference.md`  
MASTER_PLAN에는 "클리어 보상은 파티 내 경매(60초), 입찰 골드 균등 분배" 2줄 유지.

### 균열왕 HP 기준 (~3줄) ✅ KEEP

"22강 3인 파티 15분 클리어 역산" — 설계 철학 한 줄.

---

## §12. 보스 패턴 모듈 (~20줄) ➡️ MOVE

**이동 대상:** `docs/07_boss_pattern_modules/index.md`  
⚠️ **중복:** 이미 `07_boss_pattern_modules/common_patterns.md`에 패턴 상세 있음.  
MASTER_PLAN에는 "패턴/조합/연출/수치 분리 원칙 + 공용 10종, 1차 바닐라 강화형" 2줄 유지.

---

## §13. 훈련장 (~15줄) ➡️ MOVE

**이동 대상:** `docs/06_fields_bosses/CANON.md`  
이유: 훈련장은 필드 시스템의 일부.  
MASTER_PLAN에는 "제국 수도 내 훈련장, 딜 측정·허수아비 기능" 1줄 유지.

---

## §14. GUI & 리소스팩 (~870줄)

### §14.0 공통 재화 소모 규칙 (~8줄) ✅ KEEP

**전역 정책** (창고 → 인벤토리 순 차감). 모든 시스템에 적용되는 정책이므로 MASTER_PLAN 유지.

### §14.1 GUI 계층 트리 (~15줄) ✅ KEEP (계층 트리만)

4개 메인 허브 + 서브 GUI 나열 트리. 전체 구조 이해에 필수. **슬롯 매핑 상세는 전부 MOVE.**

### §14.1.1 메인 GUI 슬롯 매핑 (~20줄) ➡️ MOVE

**이동 대상:** `docs/08_resourcepack_pipeline/gui_hub_structure.md`  
⚠️ **중복:** `gui_hub_structure.md §1`에 이미 있을 가능성 높음.

### §14.1.2 장비 GUI 슬롯 매핑 (~15줄) ➡️ MOVE

**이동 대상:** `docs/08_resourcepack_pipeline/gui_equipment_panel.md`

### §14.1.3 영지 GUI 슬롯 매핑 (~600줄) ➡️ MOVE

**이동 대상:** `docs/08_resourcepack_pipeline/` 각 gui_*.md  
포함 내용:
- 영지이동 GUI → `gui_hub_structure.md`
- 영지상태 GUI → `gui_territory_status.md` (이미 존재, **중복**)
- 창고 GUI → `gui_storage.md` (이미 존재, **중복**)
- 공방 GUI → `gui_functional_specs.md §5` (이미 존재, **중복**)
- 작물관리 GUI → `gui_crop_management.md` (이미 존재, **중복**)
- 상점 GUI → `gui_shop.md` (이미 존재, **중복**)
- 경매장 GUI → `gui_auction.md` (이미 존재, **중복**)
- 영지설정 GUI → `gui_territory_settings.md` (이미 존재, **중복**)
- 영지 세부설정 GUI → `gui_territory_settings.md`
- 영지민 관리 GUI → `gui_territory_settings.md`
- 강화 GUI → `gui_functional_specs.md`
- 잠재(큐브) GUI → `gui_functional_specs.md`
- 각인 GUI → `gui_functional_specs.md`
- 캐릭터 GUI → `gui_functional_specs.md`
- 전승 GUI → `gui_functional_specs.md`
- 저장고 GUI 배치 예시 → `gui_storage.md`

### §14.1.4 보스 GUI (~120줄) ➡️ MOVE

**이동 대상:** `docs/08_resourcepack_pipeline/gui_boss_info.md` (이미 존재, **중복**)

### §14.1.5 필드 GUI (~20줄) ➡️ MOVE

**이동 대상:** `docs/08_resourcepack_pipeline/gui_hub_structure.md`

### §14.2 커스텀 GUI 배경 PNG (~10줄) ➡️ MOVE

**이동 대상:** `docs/08_resourcepack_pipeline/gui_bitmap_spec.md` (이미 존재, **중복**)

### §14.3 PNG 없는 서브 GUI (~25줄) ➡️ MOVE

**이동 대상:** `docs/08_resourcepack_pipeline/gui_functional_specs.md`

### §14.4 무기 선택 GUI (~20줄) ➡️ MOVE

**이동 대상:** `docs/08_resourcepack_pipeline/gui_functional_specs.md`

### §14.5 리소스팩 제작 우선순위 (~10줄) ✅ KEEP

5가지 우선순위 + 1차 제외 목록. 방향성 문서.

### §14.6 리소스팩 경로 원칙 (~8줄) ➡️ MOVE

**이동 대상:** `docs/08_resourcepack_pipeline/index.md`  
MASTER_PLAN에는 "assets/source = 작업 원본, assets/export/resourcepack = 익스포트" 1줄 유지.

---

## §15. 데이터 & API (~12줄)

### §15.1 HTTP API 표 (~12줄) ➡️ MOVE

**이동 대상:** `docs/02_database_api_stats/index.md`  
MASTER_PLAN에는 "HTTP API 포트 8765, 인증/필드보스/관리자 대시보드 제공" 1줄 유지.

---

## §16. 개발 로드맵 (~15줄) 🔄 CONSOLIDATE

Phase 표 ✅ KEEP (Phase명만)  
"도감 1차 제외" ✅ KEEP  
**이동 대상:** 상세 작업 목록 → `docs/10_development_roadmap/index.md` (이미 있음 — **중복**)

---

## §17. 확정 타임라인 (~10줄) 🗃️ ARCHIVE

2026-05-14~19 날짜별 결정사항. 이미 내용이 각 섹션에 반영됨.  
의사결정 히스토리 보관 가치는 있으나 실무 참조 불필요.  
**처리:** MASTER_PLAN 하단 `## 변경 이력` 섹션으로 축약 (날짜와 변경 항목 한 줄씩) 또는 `_archive/` 이동.

---

## §18. 미확정 항목 (M-tags, ~45줄) 🗃️ ARCHIVE (대부분)

### 확정 완료 항목 (ARCHIVE) — M-3, M-5, M-7, M-8, M-9, M-10, M-11, M-12, M-13, M-14

전부 확정됨. 내용이 각 섹션에 반영 완료.  
**처리:** 각 확정 내용은 이미 해당 섹션에 "확정 날짜" 형태로 기재되어 있으므로 M-tags 자체 제거.

### 미확정 항목 — M-1, M-2, M-4, M-6 ➡️ MOVE

**이동 대상:** `docs/11_remaining_decisions/index.md`  
- M-1: 강화석 드랍률 최종 수치
- M-2: 강화 성공률 재조정 (21~25강)
- M-4: 전승권 비용 (오픈 후 흔적 시세 확인 후 결정)
- M-6: 태양의 흔적 아이템 정의 및 수급 경로

---

## §19. 용어 확정안 (~20줄) ✅ KEEP

표기 용어 통일 표. 모든 문서에 적용되므로 MASTER_PLAN 유지.

---

## §20. 관리자 커맨드 (~90줄) ➡️ MOVE

**이동 대상:** `docs/01_plugin_architecture/admin_command_spec.md` (이미 존재)  
⚠️ **중복:** `admin_command_spec.md`가 이미 있으므로 비교 후 병합.  
MASTER_PLAN에는 "`/empire` 계층 커맨드, 권한 `empire.admin`" 1줄 유지.

---

## 전체 요약

### 1. MASTER_PLAN에 반드시 남아야 하는 내용

| 섹션 | 대략 줄 수 | 내용 |
|---|---|---|
| §0 한 줄 정의 | 3 | 서버 정의 |
| §1 운영 기본값 | 20 | 운영 기간, 서버 버전, 후원 정책, 경제 개입 |
| §2 디스코드 인증 플로우 (축약) | 15 | 인증 7단계 + 역할 구조 |
| §3 플러그인 역할표 | 15 | 플러그인 분담 |
| §4 월드 구조 | 10 | 4개 월드 |
| §5.1 입력 방식 표 | 10 | LC/RC/SRC/F 역할 |
| §5.2 설계 금지 | 5 | 1차 제외 6종 |
| §5.3 각인 구조 (요약) | 5 | 소모형/유지형 원칙 |
| §5.7 피해 계산 공식 | 5 | 수식 |
| §6.2 핫바 구성 | 8 | 슬롯 9개 표 |
| §7.1 T1 단일 체계 | 5 | T2/세트 제외 |
| §8.1 영지 구조 원칙 | 5 | IridiumSkyblock + EmpireRPG 역할 분리 |
| §8.3 시설 3종 (요약) | 5 | 약초/광물/공방 |
| §8.6 바닐라 농사/채광 | 5 | 정책 |
| §9.1 필드 5개 표 | 8 | 필드 역할 |
| §9.2 드랍 구조 (요약) | 8 | 3줄 드랍 원칙 |
| §9.4 원샷 방지 | 5 | 85% 클램프 |
| §9.5 기여도 | 5 | 3% 기준 |
| §9.6 잠수 방지 | 3 | 규칙 |
| §10 필드보스 기본 규칙 | 8 | 스폰/소멸/타임아웃 |
| §14.0 공통 재화 소모 규칙 | 8 | 전역 정책 |
| §14.1 GUI 계층 트리 | 15 | 4허브 + 서브 구조 |
| §14.5 리소스팩 우선순위 | 10 | 제작 순서 |
| §16 로드맵 (Phase 표만) | 12 | 8 Phase |
| §19 용어 확정안 | 20 | 표기 통일 |
| **합계 예상** | **~220줄** | |

---

### 2. 각 시스템 문서로 분리해야 하는 내용

| 내용 | 이동 대상 | 현재 줄 수 |
|---|---|---|
| 무기 6종 스킬 상세 (§5.4) | `04_combat_weapon_skills/CANON.md` | ~220 |
| 시작 지급 장비 (§5.6) | `04_combat_weapon_skills/CANON.md` | ~25 |
| 무기/방어구 베이스 아이템 표 | `04_combat_weapon_skills/CANON.md` | ~20 |
| 상점 아이템 + 블럭 58종 표 (§6.4) | `02_database_api_stats/CANON.md` | ~90 |
| 강화 테이블 1~25강 (§7.3) | `02_database_api_stats/CANON.md` | ~55 |
| 잠재 메모리얼/등업/이탈 상세 | `02_database_api_stats/CANON.md` | ~40 |
| 영지 작위 8단계 (§8.2) | `05_island_farm_system/CANON.md` | ~40 |
| 시설 산출량 표 (§8.3) | `05_island_farm_system/CANON.md` | ~30 |
| 공방 레시피 체인 전체 (§8.5) | `05_island_farm_system/workshop_crafting_spec.md` | ~130 |
| 기본 광물 생성기 확률표 (§8.7) | `05_island_farm_system/CANON.md` | ~35 |
| 필드보스 드랍 확률 표 (§10) | `06_fields_bosses/drop_tables_v1.md` | ~15 |
| 시즌보스 드랍 표 + 경매 규칙 (§11) | `06_fields_bosses/CANON.md` | ~20 |
| 보스 패턴 10종 표 (§12) | `07_boss_pattern_modules/index.md` | ~15 |
| 훈련장 (§13) | `06_fields_bosses/CANON.md` | ~15 |
| GUI 슬롯 매핑 전체 (§14.1.1~14.4) | `08_resourcepack_pipeline/gui_*.md` | ~820 |
| HTTP API 표 (§15.1) | `02_database_api_stats/index.md` | ~12 |
| 관리자 커맨드 (§20) | `01_plugin_architecture/admin_command_spec.md` | ~90 |
| EmpireRPG 부트스트랩 (§3.2) | `01_plugin_architecture/CANON.md` | ~25 |
| 시스템 메시지 포맷 (§6.3) | `01_plugin_architecture/CANON.md` | ~15 |
| **합계 예상** | | **~1,720줄** |

---

### 3. Archive 후보

| 내용 | 처리 |
|---|---|
| §17 확정 타임라인 (4개 날짜) | `_archive/` 또는 변경 이력 섹션으로 축약 |
| §18 확정 완료 M-tags (M-3,5,7~14) | 각 섹션에 흡수 완료 — 삭제 |
| 각종 구현 메모 (velocity, PlayerSwapHandItemsEvent 등) | `implementation_reference.md`로 |

---

### 4. 중복 내용 정리

| 내용 | 중복 위치 |
|---|---|
| 강화 테이블 1~25강 | `final_master_plan.md §7.3` = `economy_numbers_v2.md §0` (골드 비용 불일치 포함) |
| 잠재 라인별 이탈 확률 표 | `final_master_plan.md §7.2` = `potential_options_v1.md` |
| 잠재 옵션 풀 상세 표 | `final_master_plan.md §7.2` = `potential_options_v1.md` |
| 공방 레시피 체인 | `final_master_plan.md §8.5` = `workshop_crafting_spec.md` |
| 영지 작위 표 | `final_master_plan.md §8.2` = `island_system_design.md` |
| 시설 산출량 표 | `final_master_plan.md §8.3` = `island_system_design.md` |
| GUI 슬롯 매핑 전체 | `final_master_plan.md §14` = `08_resourcepack_pipeline/gui_*.md` (17개) |
| 보스 패턴 10종 | `final_master_plan.md §12` = `07_boss_pattern_modules/` |
| 개발 Phase 표 | `final_master_plan.md §16` = `10_development_roadmap/index.md` |
| 관리자 커맨드 | `final_master_plan.md §20` = `01_plugin_architecture/admin_command_spec.md` |

---

### 5. Canon 기준으로 재정리해야 하는 내용

| 항목 | 충돌 내용 | Canon 기준 (채택 값) |
|---|---|---|
| 강화 골드 비용 | MASTER_PLAN 1강=2,000G vs economy_numbers_v2 1강=180G | **MASTER_PLAN §18 M-3**: "1강 2,000G → 10강 20,000G" 확정 |
| Citizens 플러그인 | `01_plugin_architecture/index.md`에 Citizens 포함 | **제거 확정** — index.md 수정 필요 |
| 공방 대기열 슬롯 수 | `numbers_and_open_decisions §T-10` "5~12슬롯" vs MASTER_PLAN "1대=슬롯 3개" | **MASTER_PLAN §8.3** 기준: 1대=3슬롯 |
| 아이템 이름 변경권 가격 | `final_master_plan §6.4` 300,000G vs `§18 M-11` 10,000G | **M-11 확정**: 10,000G — §6.4 수정 필요 |
| 도감(컬렉션) | 일부 문서에 잔존 언급 | **1차 시즌 제외** |

---

## 최종 규모 예측

| 구분 | 현재 | 목표 |
|---|---|---|
| MASTER_PLAN 유지 분량 | 2,341줄 | ~220줄 |
| 시스템 문서로 이동 | — | ~1,720줄 분산 |
| Archive/삭제 | — | ~400줄 |
| **압축률** | | **약 90% 축소** |

---

> 다음 단계: 이 분류를 기반으로 MASTER_PLAN 리라이트 초안 작성.  
> 실제 수정 전 확정이 필요한 항목: **아이템 이름 변경권 가격 (300,000G vs 10,000G)**
