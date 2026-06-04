# 포로 서버 docs 구조 개편안

> **[STATUS: ARCHIVED]** — 구조 개편 계획 문서. 2026-05-22 리빌드 정리에 따라 `docs/_archive/`로 이동됨.

> 작성일: 2026-05-22  
> 상태: **계획 문서 — 파일 수정 전 검토용**  
> 목적: canon/draft 분리, final_master_plan 의존도 감소, 시스템별 CANON.md 도입, archive 구조 정립

---

## 개편 목표

| 목표 | 현재 문제 | 해결 방향 |
|---|---|---|
| Canon 명확화 | final_master_plan이 유일 소스이지만 2,341줄 | 시스템별 CANON.md로 위임, master plan은 정책·원칙 수준만 |
| Draft 분리 | 구버전 문서가 최신 문서 옆에 공존 | `_archive/` 폴더로 격리 |
| 충돌 제거 | 망치/도끼, Citizens, 마력 시스템 등 불일치 | CANON.md 작성 시 최신 결정으로 통일 |
| 유지보수성 | 수치 변경 시 여러 파일 동시 수정 필요 | 각 CANON.md가 해당 도메인 단일 기준 |

---

## 추천 최종 폴더 구조

```
docs/
├── final_master_plan.md              ← 유지 (CLAUDE.md 지정 소스, 정책/원칙 역할)
├── final_설계_plan.md                ← 01_plugin_architecture/ 로 이동 (구현 레퍼런스)
├── 99_check.md                       ← _archive/ 로 이동 (수치 반영 완료)
│
├── 01_plugin_architecture/
│   ├── CANON.md                      ← 신규 생성
│   ├── poro_rpg_module_design.md   ← 유지
│   ├── poro_rpg_design_intent.md   ← 유지
│   ├── admin_command_spec.md         ← 유지
│   └── index.md                      ← 유지 (Citizens 표기 수정 필요)
│
├── 02_database_api_stats/
│   ├── CANON.md                      ← 신규 생성
│   ├── economy_numbers_v2.md         ← 유지 (계산 참조용)
│   ├── enhancement_droprate_v1.md    ← 유지
│   ├── equipment_growth_spec.md      ← 유지
│   ├── potential_options_v1.md       ← 유지
│   └── index.md                      ← 유지
│
├── 03_discord_onboarding_bot/
│   └── index.md                      ← 유지 (내용 보강 가능)
│
├── 04_combat_weapon_skills/
│   ├── CANON.md                      ← 신규 생성
│   ├── combat_balance_v2.md          ← 유지 (수치 참조용)
│   ├── weapon_skills_v1.md           ← 유지 (망치→도끼 수정 필요)
│   ├── item_grade_substat_v1.md      ← 유지
│   ├── level_stat_system_v1.md       ← 유지
│   ├── season_boss_stats_v1.md       ← 유지
│   └── index.md                      ← 유지
│
├── 05_island_farm_system/
│   ├── CANON.md                      ← 신규 생성
│   ├── island_system_design.md       ← 유지
│   ├── workshop_crafting_spec.md     ← 유지
│   └── index.md                      ← 유지
│
├── 06_fields_bosses/
│   ├── CANON.md                      ← 신규 생성
│   ├── drop_tables_v1.md             ← 유지
│   ├── field_boss_stats_v1.md        ← 유지
│   └── index.md                      ← 유지
│
├── 07_boss_pattern_modules/
│   ├── index.md                      ← 유지
│   ├── common_patterns.md            ← 유지
│   ├── field_boss_patterns.md        ← 유지
│   └── season_boss_patterns.md       ← 유지
│
├── 08_resourcepack_pipeline/
│   ├── index.md                      ← 유지
│   └── gui_*.md (17개)               ← 유지
│
├── 09_terms_and_policy/
│   └── index.md                      ← 유지
│
├── 10_development_roadmap/
│   └── index.md                      ← 유지
│
├── 11_remaining_decisions/
│   └── index.md                      ← 유지 (미결 항목만 남기고 정리)
│
├── 12_map_design/
│   └── field_map_concepts.md         ← 유지
│
└── _archive/                         ← 신규 폴더
    ├── 00_master_plan.md
    ├── economy_numbers_v1.md
    ├── atk_dps_baseline_v1.md
    ├── design_decisions_confirmed.md
    ├── numbers_and_open_decisions.md
    └── dps_analysis_v1.md
```

---

## 문서 처리 계획 전체

### A. 유지 (현 위치 그대로)

| 파일 | 이유 |
|---|---|
| `final_master_plan.md` | CLAUDE.md 지정 Canon, 이동하면 참조 깨짐 |
| `02_database_api_stats/economy_numbers_v2.md` | 상세 계산 참조용으로 계속 필요 |
| `02_database_api_stats/enhancement_droprate_v1.md` | 드랍률 상세 참조용 |
| `02_database_api_stats/equipment_growth_spec.md` | 강화 수치 계산 스펙 |
| `02_database_api_stats/potential_options_v1.md` | 잠재 옵션 상세 (확정값) |
| `04_combat_weapon_skills/combat_balance_v2.md` | 피해 공식 / T1 스탯 확정 |
| `04_combat_weapon_skills/item_grade_substat_v1.md` | 부가 스텟 상세 |
| `04_combat_weapon_skills/level_stat_system_v1.md` | 레벨·IL 공식 |
| `04_combat_weapon_skills/season_boss_stats_v1.md` | 시즌보스 HP·방어력 |
| `05_island_farm_system/island_system_design.md` | 영지 DB 스키마·산출량 상세 |
| `05_island_farm_system/workshop_crafting_spec.md` | 공방 레시피 전체 |
| `06_fields_bosses/drop_tables_v1.md` | 드랍 테이블 확정판 |
| `06_fields_bosses/field_boss_stats_v1.md` | 필드보스 스탯 |
| `07_boss_pattern_modules/` 전체 4개 | 보스 패턴 상세 |
| `08_resourcepack_pipeline/` 전체 17개 | GUI 설계 문서 |
| `10_development_roadmap/index.md` | 최신 Phase 구조 |
| `12_map_design/field_map_concepts.md` | 맵 설계 초안 |
| 각 폴더 `index.md` | 각 도메인 진입 요약 |

### B. 수정 필요 후 유지

| 파일 | 필요한 수정 |
|---|---|
| `01_plugin_architecture/index.md` | Citizens 유지 표기 → "제거" 로 수정 |
| `04_combat_weapon_skills/weapon_skills_v1.md` | "망치" → "도끼"로 전체 치환, 기본 공격 설명 최신화 |
| `11_remaining_decisions/index.md` | 이미 확정된 항목 제거, 진짜 미결 항목만 유지 |

### C. 이동

| 파일 | 현재 위치 | 이동 위치 | 이유 |
|---|---|---|---|
| `final_설계_plan.md` | `docs/` 루트 | `docs/01_plugin_architecture/implementation_reference.md` | 구현 레퍼런스는 플러그인 아키텍처 폴더가 맞음. 루트 오염 해소 |

### D. Archive (`_archive/` 이동)

| 파일 | 이유 |
|---|---|
| `00_master_plan.md` | final_master_plan으로 완전 대체됨. 내용이 canon과 충돌 (망치, 마력, Citizens) |
| `economy_numbers_v1.md` | economy_numbers_v2.md로 대체됨. v2가 v1 핵심 수치 전부 포함 |
| `atk_dps_baseline_v1.md` | 문서 자체에 "v2 공식으로 재산출 필요" 경고 — 구현 시 사용 금지 |
| `design_decisions_confirmed.md` | 내용이 final_master_plan에 흡수됨. 기반 문서로 00_master_plan 참조 (구버전 의존) |
| `numbers_and_open_decisions.md` | "미결 항목" 대부분 이미 확정됨. 마력 수치, 구버전 공방 대기열 등 폐지 사항 포함 |
| `dps_analysis_v1.md` | 초안 표기, 실서버 실측 전 이론치. 구현 시 재산출 필요 |
| `99_check.md` | 강화 경제 검토 보고서 — 해당 수치들이 이미 economy_numbers_v2에 반영됨 |

---

## 신규 생성할 CANON.md 목록

### 우선순위 1 — 즉시 생성 (충돌 위험 높음)

#### `01_plugin_architecture/CANON.md`

**포함 내용:**
- 플러그인 역할 분담표 (Citizens 제거 확정 반영)
- PoroRPG 부트스트랩 순서 8단계
- 월드 4개 구조 (`world_main`, `world_farm`, `world_boss`, `world_test`)
- 플레이어 데이터 관리 방식 (5분 캐시 + 퇴장 저장)
- 제거/보류 플러그인 목록

**참조 소스:** `final_master_plan.md`의 "플러그인 구조", `docs/01_plugin_architecture/implementation_reference.md`

---

#### `04_combat_weapon_skills/CANON.md`

**포함 내용:**
- 무기 6종 확정 목록 (도끼, 검세→검세, 자원명 전부 확정)
- 각인 구조 (소모형/유지형 2종 구분)
- 스킬 입력 매핑 (LC/RC/SRC/F 역할)
- 설계 금지 항목 6종
- 무기 베이스 아이템 + CMD 번호 확정표
- 피해 계산 공식
- 치명타 기본값 (확률 10%, 배율 1.5배)

**참조 소스:** `final_master_plan.md`의 "전투와 장비 성장", `combat_balance_v2.md`
**수정 필요:** weapon_skills_v1.md의 "망치" 표기 → "도끼"로 통일

---

### 우선순위 2 — 구현 직전 생성

#### `02_database_api_stats/CANON.md`

**포함 내용:**
- 강화 비용표 확정판 (골드 + 강화석, 1~25강) — `final_master_plan.md`의 "전투와 장비 성장" 기준
- 잠재 등업 확률 + 라인별 이탈 확률
- 큐브 1회 5,000G, 조각 10개 = 큐브 1개
- 골드 드랍 기준 (v2 ×1.5 적용 확정판)
- 강화석 드랍 기준 (직접 드랍 확정, 파편 폐지)
- 영지 작위 업그레이드 비용표

**참조 소스:** `final_master_plan.md`의 "전투와 장비 성장" 및 "개인 영지", `economy_numbers_v2.md`  
**주의:** economy_numbers_v2의 골드 비용 (1강 180G) vs final_master_plan (1강 2,000G) 불일치 → **final_master_plan 기준** 사용

---

#### `05_island_farm_system/CANON.md`

**포함 내용:**
- 영지 작위 8단계 + 시설 슬롯 수 + XZ 한도
- 확정 시설 3종 (약초 재배지, 광물 채굴기, 공방 가공기)
- 시설 산출량 레벨별 표
- 시설 배치 방식 (GUI 슬롯, 물리 블럭 없음)
- 폐지 확정 항목: 마력 시스템, 발전기, 자동채굴기
- 공방 탭 6종 목록 + 삭제된 탭 목록
- 영지 저장고 입출금 규칙

**참조 소스:** `final_master_plan.md`의 "개인 영지", `05_island_farm_system/index.md`  
**주의:** numbers_and_open_decisions의 "공방 대기열 5~12슬롯" → **무시**. final_master_plan "1대 = 슬롯 3개" 사용

---

#### `06_fields_bosses/CANON.md`

**포함 내용:**
- 필드 5개 기본 정보 (역할, 재료, 권장 IL)
- 필드보스 스폰 규칙 (30분, 5분 소멸)
- 기여도 기준 (3% 이상)
- 원샷 방지 (85% 클램프)
- 시즌보스 4종 (보스명, boss_id, 클리어율 목표)
- 드랍 구조 (일반몹/정예몹/필드보스/시즌보스)
- IL 경고 시스템

**참조 소스:** `final_master_plan.md`의 "필드, 보스, 드랍", `drop_tables_v1.md`, `06_fields_bosses/index.md`

---

### 우선순위 3 — 선택적 생성

현재 index.md가 충분히 역할을 하고 있는 폴더는 CANON.md 불필요.

| 폴더 | 판단 | 이유 |
|---|---|---|
| `03_discord_onboarding_bot/` | CANON.md 불필요 | index.md가 간결하고 충돌 없음 |
| `07_boss_pattern_modules/` | CANON.md 불필요 | 내용이 단일 버전, 충돌 없음 |
| `08_resourcepack_pipeline/` | CANON.md 불필요 | 이미 세분화된 GUI 문서 구조 |
| `09_terms_and_policy/` | CANON.md 불필요 | 단일 문서 |
| `10_development_roadmap/` | CANON.md 불필요 | Phase 구조 이미 명확 |

---

## CANON.md 공통 포맷

모든 CANON.md는 아래 형식을 따른다:

```markdown
# [시스템명] — CANON

> **이 문서가 [도메인]의 단일 기준입니다.**  
> 확정일: YYYY-MM-DD | 충돌 시 `final_master_plan.md` 우선
> 
> 상세 구현 참조: [관련 문서 링크]

---

## [섹션]

[내용]
```

**CANON.md 작성 원칙:**
- 확정된 수치만 기재. 미결 항목은 표시 후 `→ 11_remaining_decisions/` 참조
- 폐지된 항목은 명시적으로 "폐지 확정 (날짜)" 표기
- 구현 코드·SQL·Java 코드는 포함하지 않음 — `implementation_reference.md` 위임
- 500줄 이하 유지

---

## Archive 운영 규칙

```
docs/_archive/
├── README.md      ← 각 파일 archive 이유 기록
└── [파일들]
```

**`_archive/README.md` 내용 (신규 생성):**

```markdown
# Archive

이 폴더의 문서는 더 이상 정답 소스가 아닙니다.
삭제하지 않고 보관하는 이유: 수치 산출 과정, 의사결정 히스토리 참조용.

| 파일 | archive 이유 | 대체 문서 |
|---|---|---|
| 00_master_plan.md | final_master_plan.md로 완전 대체 | final_master_plan.md |
| economy_numbers_v1.md | v2로 대체 | economy_numbers_v2.md |
| atk_dps_baseline_v1.md | v1 선형 공식 사용, v2 비선형과 불일치 | combat_balance_v2.md |
| design_decisions_confirmed.md | final_master_plan 흡수, 기반 문서가 구버전 | final_master_plan.md |
| numbers_and_open_decisions.md | 미결 항목 대부분 확정됨, 폐지 시스템 참조 | 각 CANON.md |
| dps_analysis_v1.md | 실측 전 이론치 초안 | 재산출 필요 |
| 99_check.md | 검토 결과가 economy_numbers_v2에 반영 완료 | economy_numbers_v2.md |
```

---

## 충돌 수정이 필요한 파일 (이동 없이 내용만 수정)

아래는 파일 위치를 유지하면서 **내용만 고쳐야** 하는 항목이다:

| 파일 | 수정 내용 |
|---|---|
| `01_plugin_architecture/index.md` | Citizens → "제거 확정" 표기로 수정 |
| `04_combat_weapon_skills/weapon_skills_v1.md` | "망치" 전체 → "도끼" 치환, 각인 기본 자원 스택 수 final_master_plan과 동기화 |
| `04_combat_weapon_skills/index.md` | "망치" → "도끼" 치환 |
| `economy_numbers_v2.md` | 강화 골드 비용이 final_master_plan과 상이 (v2: 1강 180G, final: 1강 2,000G) → 어느 쪽이 실제 확정인지 결정 후 통일 |

---

## 작업 우선순위 (구현 순서 추천)

```
1단계: _archive/ 폴더 생성 + README.md 작성
       → 7개 파일 archive 이동

2단계: final_설계_plan.md 이동
       → docs/01_plugin_architecture/implementation_reference.md

3단계: 충돌 파일 수정
       → index.md Citizens, weapon_skills_v1 망치→도끼

4단계: CANON.md 신규 생성 (우선순위 1)
       → 01_plugin_architecture/CANON.md
       → 04_combat_weapon_skills/CANON.md

5단계: CANON.md 신규 생성 (우선순위 2)
       → 02_database_api_stats/CANON.md
       → 05_island_farm_system/CANON.md
       → 06_fields_bosses/CANON.md

6단계: CLAUDE.md 업데이트
       → 각 시스템 참조 경로에 CANON.md 명시
```

---

## CLAUDE.md 업데이트 계획

개편 완료 후 CLAUDE.md의 "For deeper reference" 섹션을 아래로 수정:

```markdown
- Plugin/system work: `docs/01_plugin_architecture/CANON.md`
- Economy/stats: `docs/02_database_api_stats/CANON.md`
- Discord/onboarding: `docs/03_discord_onboarding_bot/index.md`
- Combat/skills: `docs/04_combat_weapon_skills/CANON.md`
- Island/farm: `docs/05_island_farm_system/CANON.md`
- Fields/bosses/drops: `docs/06_fields_bosses/CANON.md`
- Boss patterns: `docs/07_boss_pattern_modules/index.md`
- Resource pack/assets: `docs/08_resourcepack_pipeline/index.md`
- Terms/policy: `docs/09_terms_and_policy/index.md`
- Roadmap/issues: `docs/10_development_roadmap/index.md`
```

---

## 요약표

| 처리 | 파일 수 | 대상 |
|---|---|---|
| **유지 (변경 없음)** | 34개 | 대부분의 시스템 문서 |
| **내용 수정 후 유지** | 4개 | index.md (Citizens), weapon_skills_v1 (망치), economy_numbers_v2 (비용 불일치) |
| **이동** | 1개 | final_설계_plan.md → 01_plugin_architecture/ |
| **Archive** | 7개 | 00_master_plan, economy_v1, atk_dps_v1, design_decisions, numbers_decisions, dps_v1, 99_check |
| **신규 생성 (CANON.md)** | 5개 | 01, 02, 04, 05, 06 폴더 |
| **신규 생성 (구조)** | 2개 | `_archive/`, `_archive/README.md` |
