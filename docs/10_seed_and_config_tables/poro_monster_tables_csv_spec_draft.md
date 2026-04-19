# 포로 서버 필드 몬스터 3 CSV 테이블 스펙 + 수도·동부 샘플 시드 v1

> 작성일: 2026-04-19
> 배경: `poro_field_monster_drop_table_audit_v1.md` B 결정(B-1~B-5) 5건 수용 후속.
> B-1 태그 축 = `monster_master`, B-2 재료명 리네임 승인, B-3 보스 매트릭스 별도 유지, B-4 사하르·아르카논 생태계 = 3막 도시 개방 동시, B-5 각인서 0% 명시행 = `drop_rate_pct=0` DDL 포함.
> 본 스펙은 `poro_custom_model_data_registry_csv_spec_draft.md`의 CSV 단일 파일 + Git 브랜치 리뷰 흐름을 동일 패턴으로 재사용한다.

---

## 1. 개요

포로 서버의 필드 몬스터(정예 포함, 보스 제외)를 기준으로 `monster_master` → `monster_spawn_table` → `monster_drop_table` 3개 CSV를 SSOT로 설계한다. 보스 드랍은 `poro_boss_drop_grade_matrix_v1.md` 105셀 매트릭스가 별도로 유지되며(B-3), 본 3테이블은 **일반·정예 몬스터 축**만 담는다.

## 2. 목표

- 태그 축(B-1)을 `monster_master`에 단일 보관해 태그 최대 2개 제약을 스키마 단에서 강제한다.
- 재료명 리네임(B-2)을 드랍 행과 마스터 플래닝 양측에서 일치시킨다.
- 각인서 드랍이 없는 구간을 `drop_rate_pct=0` 명시 행(B-5)으로 기록해, 웹/디스코드 봇 조회에서도 "확실히 없음"과 "미정"을 구분 가능하게 한다.
- 생활 재료(원목·광석·약초)가 필드 몬스터에서 드랍되지 않는다는 규칙을 `is_life_material_blocked` 플래그로 가시화한다.
- 수도·동부 샘플을 통해 1막 2개 테마가 채워진 초기 상태를 CI 회귀 스위트로 확보한다.

## 3. 핵심 규칙

### 3.1 파일 위치·네이밍

| 파일 | 경로 | 행 수(초안) |
|---|---|---|
| 마스터 | `docs/10_seed_and_config_tables/poro_monster_master.csv` | 20행(수도 10 + 동부 10) |
| 스폰 | `docs/10_seed_and_config_tables/poro_monster_spawn_table.csv` | 20행 |
| 드랍 | `docs/10_seed_and_config_tables/poro_monster_drop_table.csv` | 61행 |

- 인코딩 UTF-8, 줄바꿈 LF, 헤더 1행 필수.
- `notes` 자유 텍스트는 큰따옴표 감싸기(RFC 4180).
- 정렬: 마스터·스폰은 `theme → monster_id`, 드랍은 `monster_id → drop_type`.

### 3.2 테이블 1 — `poro_monster_master.csv` (13 컬럼)

| # | 컬럼 | 타입 | 필수 | 설명 |
|---|---|---|---|---|
| 1 | `monster_id` | string | Y | PK. `poro:monster/<theme>/<snake>` 네임스페이스. |
| 2 | `theme` | enum | Y | `capital` / `east` / `west` / `south` / `north` / `sahar` / `arcanon`. |
| 3 | `level_band` | enum | Y | `low` / `mid` / `high`. 1막은 `low`·`mid` 주축. |
| 4 | `archetype` | enum | Y | `melee` / `ranged` / `magic` / `stealth` / `mixed`. |
| 5 | `tag1` | enum | Y | 태그 풀(침식·환영·분신·도적·폭풍·화염·균열·냉기·언데드·봉인·계약·마력·공명). |
| 6 | `tag2` | enum | N | 두 번째 태그. 최대 2개 원칙(피해 증가 태그 규칙과 일치). |
| 7 | `base_hp` | int | Y | 상대값 밸런스 기준(1막 일반 100 내외, 중레벨 상단 300대). |
| 8 | `base_atk` | int | Y | 상대값. |
| 9 | `base_def` | int | Y | 상대값. |
| 10 | `owner` | string | Y | 담당 에이전트/기획자. |
| 11 | `status` | enum | Y | `reserved` / `designed` / `implemented` / `deprecated`. |
| 12 | `version_added` | semver | Y | `s{int}-v{major}.{minor}.{patch}`. |
| 13 | `notes` | string | N | 자유 메모. |

### 3.3 테이블 2 — `poro_monster_spawn_table.csv` (9 컬럼)

| # | 컬럼 | 타입 | 필수 | 설명 |
|---|---|---|---|---|
| 1 | `spawn_id` | string | Y | PK. `spawn:<theme>/<location>/<monster_slug>`. |
| 2 | `theme` | enum | Y | `monster_master.theme`와 반드시 일치. |
| 3 | `location_type` | enum | Y | `숲` / `평원` / `동굴` / `사당` / `유적` / `폐허` / `상단`. |
| 4 | `monster_id` | string | Y | FK → `monster_master.monster_id`. |
| 5 | `weight` | int | Y | 동일 `(theme, location_type)` 내 상대값. 합계 정규화는 플러그인 측. |
| 6 | `owner` | string | Y | |
| 7 | `status` | enum | Y | |
| 8 | `version_added` | semver | Y | |
| 9 | `notes` | string | N | |

### 3.4 테이블 3 — `poro_monster_drop_table.csv` (13 컬럼)

| # | 컬럼 | 타입 | 필수 | 설명 |
|---|---|---|---|---|
| 1 | `drop_id` | string | Y | PK. `drop:<theme>/<monster_slug>/<drop_key>`. |
| 2 | `monster_id` | string | Y | FK → `monster_master`. |
| 3 | `drop_type` | enum | Y | `gold` / `t1_raw` / `t1_gear` / `boss_material` / `engraving_book` / `cosmetic` / `t2_seed`. |
| 4 | `grade` | enum | N | `common` / `uncommon` / `rare` / `epic` / `legendary`. 각인서·치장·장비만 필수, 그 외 비워둔다. |
| 5 | `item_id` | string | Y | 월렛 엔드포인트이면 `poro:wallet/...`, 실물이면 `poro:material/...` 등 네임스페이스. |
| 6 | `drop_rate_pct` | decimal | Y | 0~100 범위. B-5에 따라 각인서 0% 명시 행은 `0`으로 기록. |
| 7 | `qty_min` | int | Y | `drop_rate_pct=0`이면 0 허용. |
| 8 | `qty_max` | int | Y | `qty_max >= qty_min`. |
| 9 | `is_life_material_blocked` | bool | Y | `true`면 "생활 원재료(원목·광석·약초) 드랍 금지" 가드 체크 대상. 전투 재료로 재분류된 행에는 `false`. |
| 10 | `owner` | string | Y | |
| 11 | `status` | enum | Y | |
| 12 | `version_added` | semver | Y | |
| 13 | `notes` | string | N | 리네임 이력·B-5 명시 여부 기록. |

### 3.5 B-5 0% 명시행 저장 규약

- 각인서 드랍이 논리적으로 "지금은 없음, 나중에도 없음"인 경우 `drop_rate_pct=0`, `qty_min=qty_max=0`으로 명시 1행을 남긴다.
- "미정"과의 구분을 위해 `status=designed` + `notes`에 "B-5 명시 0% 행" 문구를 포함한다.
- CI는 같은 `(monster_id, drop_type=engraving_book, grade=common)` 조합이 0% 행 또는 양수 행 중 **정확히 1개** 존재해야 함을 검사(§6 R4).

## 4. 범위 / 비범위 (1차)

### 4.1 범위
- 3 CSV 스펙 + 수도·동부 20마리 샘플.
- CI 검증 규칙 R1~R6.
- 재료명 리네임 전후 매핑 표(§5.2).
- 보스 매트릭스와의 교차 검증 섹션(§7).

### 4.2 비범위
- 서·남·북 테마 샘플(추후 v1.1에서 각 10행 확대).
- 사하르·아르카논 생태계(B-4, 3막 도시 개방과 동시에 v2에서 추가).
- 보스 드랍 매트릭스 수치 변경(별도 유지 — §7).
- 실제 린트 스크립트 구현(요구사항 수준까지만).

## 5. 구현 포인트

### 5.1 샘플 시드 요약

| 테마 | 몬스터 수 | 정예 후보 | 대표 태그 |
|---|---|---|---|
| 수도(capital) | 10 | 2 (`rift_warden`, `ruined_knight`) | 균열·도적·봉인·언데드·마력 |
| 동부(east) | 10 | 1 (`twisted_gladewalker`) | 분신·환영·침식·마력 |

태그 2개 원칙을 모두 준수(`base_atk/base_def`는 상대값 기준, 1막 밸런스 조정 시 일괄 스케일 예정).

### 5.2 재료명 리네임 전후 매핑 표 (B-2 승인)

> 대상: 마스터 플래닝·보스 드랍 매트릭스에서 생활 재료와 혼동 가능한 전투 재료 이름.

| 전(기존 표기) | 후(신규 표기) | 이유 |
|---|---|---|
| 설원 털가죽 | 북부 `서리 갑각편` | 털가죽이 생활 채집을 연상 → 전투 재료 톤으로 전환 |
| 건조 약초 | 남부 `화맥 결정진` | 약초=생활 원재료 연상 → 화염/균열 전투 재료 정체성 강화 |
| 깨진 금속편 | 서부 `잔영 철편` | 금속=광물 연상 → 환영·잔영 테마 톤으로 전환 |
| 균열 코어 조각 | 수도 `ruin_rift_core_sliver` | 영문 key 일관화 + 의미 유지 |
| 균열 맥동 가루 | 수도 `rift_pulse_dust` | 신규(맥동 개념 표면화) |
| 도적단 표식 증표 | 수도 `rogue_mark_token` | 파편 도적 라인 정합 |
| 부서진 봉인 재 | 수도 `broken_seal_ash` | 봉인 테마 톤 강화 |
| 마력 문장 파편 | 수도 `mana_sigil_fragment` | 주문사 계열 재료 추가 |
| 균열 감시인 인장 | 수도 `rift_warden_badge` | 정예 고유 재료 신설 |
| 깨어진 맹세의 파편 | 수도 `shattered_oath_shard` | 폐허 기사 스토리 톤 연계 |
| 환영 눈물 조각 | 동부 `phantom_tear_shard` | 분신 잔영 재료 |
| 침식 수액 샘플 | 동부 `corrupt_sap_vial` | 동부 서브2 퀘스트명 정합 |
| 썩은 나무껍질 잔흔 | 동부 `rotten_bark_cinder` | "나무껍질"은 생활 아님을 명시(전투 재료 "잔흔") |
| 드리아드 줄기 가닥 | 동부 `dryad_vein_strand` | 정예급 재료 신설 |
| 빈 거울 파편 | 동부 `hollow_mirror_shard` | 중레벨 분신 고유 |
| 뒤틀린 심재 | 동부 `twisted_heartwood` | 정예 고유 재료 |

> 총 리네임/신설 항목: **16건** (기존 3건 대체 + 13건 신설·정식 키 부여).

### 5.3 필드 채집 폐기(`is_life_material_blocked`) 반영

- 생활 원재료(원목·광석·약초·수액 원형 등)는 **필드 몬스터 드랍 금지**. 본 3테이블 어느 행도 이 범주에 속하지 않는다.
- 기존 마스터 플래닝 테마별 드랍 섹션에서 "생활 재료"로 언급된 항목은 전투 재료 재료명으로 재분류(§5.2 적용).
- 드랍 테이블은 `is_life_material_blocked=false`가 기본값이지만, 가드 용도로 컬럼을 유지. `true`로 표기된 행은 "과거 생활 재료로 오해될 수 있었으나 현재 전투 재료로 대체됨"을 리뷰 시 시각화한다(1차 샘플에는 `false`만 존재 — 모든 행이 이미 전투 재료 기준으로 정리됨).
- 마스터 플래닝 편집 후속: 동부 서브2 "생활 재료: 왜곡된 포자" 언급도 2막 이후 영지 제작 재료로 연결 가능한지 별도 확인 필요(오픈 질문).

## 6. CI 검증 규칙 (R1~R6)

CustomModelData 레지스트리 R1~R5와 동일 흐름으로 PR 린트 + 병합 전 CI에서 모두 실행.

### R1 — PK 중복 금지
세 테이블 각각의 PK(`monster_id` / `spawn_id` / `drop_id`)는 유일.

### R2 — FK 무결성
- `spawn_table.monster_id` ∈ `monster_master.monster_id`
- `drop_table.monster_id` ∈ `monster_master.monster_id`
- `spawn_table.theme` == 참조 마스터의 `theme`

### R3 — enum / 범위 검증
- `theme`, `level_band`, `archetype`, `location_type`, `drop_type`, `grade`, `status`: 허용 enum.
- `tag1`, `tag2`: 정의된 태그 풀 내. `tag2`는 비어있을 수 있으나 `tag1==tag2` 금지.
- `drop_rate_pct`: 0.0 ≤ x ≤ 100.0.
- `qty_min ≤ qty_max`. 둘 다 ≥ 0.

### R4 — B-5 각인서 0% 행 유일성
같은 `(monster_id, drop_type='engraving_book', grade)` 조합은 0% 행 **또는** 양수 행 중 정확히 하나. 둘 다 존재하면 에러(`DUP_ENGRAVING_ROW`).

### R5 — 생활 재료 가드 의미성
`is_life_material_blocked=true`인 행은 `drop_type ∈ {t1_raw, boss_material}`에 한해 의미가 있다. 그 외 `drop_type`에 `true`가 붙으면 `IRRELEVANT_LIFE_GUARD` 경고.

### R6 — 태그 2개 상한
`monster_master`의 (tag1, tag2) 쌍은 비어있지 않은 값 기준 **최대 2개**. 스키마 상 컬럼 자체가 2개로 고정되어 구조적으로 강제되며, 린트는 `tag1` 공백·중복만 추가 검사.

### 부가 — semver·status 정합
- `version_added`: `s{int}-v{major}.{minor}.{patch}` 정규식 강제(레지스트리와 동일).
- `status=deprecated` 전환 시 이관 이력을 `notes`에 기록(1차는 필수 아님 권고).

## 7. 보스 매트릭스와의 연속성 교차 검증 (B-3)

### 7.1 경계선
- 본 3테이블: 일반 몬스터(`level_band=low|mid`) + 정예(`level_band=mid|high` 중 정예 태깅 후보)까지. **필드보스 이상은 미포함.**
- 보스 매트릭스 105셀(`poro_boss_drop_grade_matrix_v1.md` §12): 필드보스·정예·최상위 프리뷰 3카테고리 × 7품목 × 5등급.
- 정예 카테고리는 양쪽에서 다뤄지지만, **드랍 수치 주도권은 보스 매트릭스**에 있음. 본 테이블의 정예 드랍 행은 보스 매트릭스 수치 범위 안에서 개체별 샘플 시드로만 존재.

### 7.2 중복 검증 포인트 (CI 확장 제안)

| 검증 | 설명 |
|---|---|
| V1 | `monster_master.level_band=high` 개체는 본 드랍 테이블에 `engraving_book` 행을 갖지 않는다(보스 매트릭스 전속). |
| V2 | 정예 후보(`notes`에 "정예" 포함)의 각인서 커먼 드랍률은 매트릭스 §2-1 B행(0.8%) ±0.2 이내. |
| V3 | T2 시드(`drop_type=t2_seed`)는 1막 일반 몬스터에 존재 금지. 매트릭스 §2-6이 수도 상위 필드보스 전속으로 선언. |

### 7.3 1차 수용 범위
- 샘플 시드에서 정예 후보 2종(수도 `rift_warden`/`ruined_knight`)·1종(동부 `twisted_gladewalker`) 모두 커먼 각인서 0.8%로 맞춤 → 매트릭스 B 카테고리와 일치.
- 본 테이블에는 `t2_seed` 행 없음 → V3 충족.

## 8. 조회 패턴 (웹 / 디스코드 봇)

- `GET /monsters?theme=capital&level_band=mid` → 마스터 조인.
- `GET /monsters/{monster_id}/drops` → 드랍 테이블 row 집합 + 0% 행 포함.
- `/몬스터 <이름>` 디스코드 커맨드 → 마스터·스폰·드랍 3테이블 JOIN 응답, B-5 0% 행은 "드랍 없음 확정"으로 렌더.
- `/드랍 태그:<tag>` → `(tag1|tag2)=tag` + drop 집계(운영용).

## 9. 운영 지표 (수집 훅)

- 몬스터별 킬당 평균 골드·T1 조각 드랍 실측 vs 기대값 ±10% 편차.
- 태그별 일간 처치 수 분포(태그 2개 상한이 유저 파티 빌드에 편향을 일으키는지 관찰).
- `is_life_material_blocked=true` 행 추가 시 리뷰 경고 카운트(생활 재료 재유입 시도 감시).
- 정예 후보의 각인서 드랍이 주 1회 이상 실제 기록되는지 체크(V2 보조).

## 10. 무결성 주의 포인트

1. **FK 누락** — 스폰·드랍에서 오타로 `monster_id` 접두 `poro:monster/` 누락 시 R2에서 즉시 탈락시킨다.
2. **tag1==tag2 혼입** — 태그 2개 상한 인식이 "중복 가능"으로 오해될 여지. R3 보강으로 차단.
3. **B-5 0% 행과 양수 행 공존** — 정예로 승격될 때 0% 행을 지우지 않고 양수 행만 추가하는 사고가 잦을 것으로 예상. R4로 PR 차단.
4. **보스 매트릭스 중복 관리** — 본 테이블에서 필드보스 드랍을 "간단 참고용"으로 추가하려는 유혹 차단(별도 유지 B-3).
5. **생활 재료 재유입** — 리뷰 중 "약초 2~3개 정도는 괜찮지 않나" 유혹을 `is_life_material_blocked` 플래그 + §5.3 원칙으로 가시화.

## 11. 추후 확장 포인트

- **서·남·북 샘플 확대 (v1.1)**: 각 10행씩 추가, 태그 풀에서 아직 미사용인 `폭풍`/`화염`/`냉기`/`계약`/`공명` 소화.
- **사하르·아르카논 생태계 (v2, B-4)**: 3막 도시 개방과 동일 PR에서 추가. 태그 신설 여부는 3막 기획 확정 후 별도 논의.
- **`elite` 플래그 컬럼 신설 검토**: 정예 여부를 `notes`가 아닌 독립 불리언 컬럼으로 승격 시 V1·V2 검증이 단순해진다.
- **CSV → DB 이관**: 운영 규모 확대 시 `monster_master` / `monster_spawn` / `monster_drop` 3 테이블로 1:1 이관. PK/FK/enum 제약 그대로 재사용.
- **스폰 동시성 상한 컬럼**: `spawn_weight`만으로는 순간 스폰 수 상한을 표현 못 함. `max_concurrent` 필드 추가를 v1.2에서 검토.

## 12. 오픈 질문

1. `location_type` enum을 현재 7종으로 고정할지, `하수도`·`빈민가 뒷골목` 등 수도 고유 유형을 추가할지.
2. 태그 풀 13종을 1차에서 고정할지, 3막 확장과 동시에 `공명`·`계약`을 세부화할지.
3. `drop_rate_pct` 합계 제약을 둘지(`drop_type=gold`는 100 확정, 나머지는 독립). 1차 스펙은 독립 확률 가정.
4. 생활 재료 guard 플래그를 전체 CSV에 유지할지, 훗날 필드 채집 정책이 완전 고착되면 단순화할지.
5. 정예 후보를 `notes` 문자열이 아닌 `elite` boolean 컬럼으로 승격할 시점.

---

## 다음 추천 작업

1. 서·남·북 테마 샘플 각 10행 확대 PR(v1.1).
2. R1~R6 린트 스크립트 구현 위임(implementation-reviewer).
3. 웹/디스코드 봇 `/몬스터` 커맨드 응답 포맷 초안 작성(본 3테이블 JOIN).
4. `elite` 컬럼 승격 여부 결정(태그 없이 정예 카테고리 식별을 강화할지).
5. 마스터 플래닝 테마별 드랍 섹션에 §5.2 리네임 표 링크 추가(생활 재료 언급 재점검).

---

## 이번에 정리한 핵심
- 필드 몬스터 3 CSV(마스터 13컬럼 / 스폰 9컬럼 / 드랍 13컬럼)와 CI R1~R6, 보스 매트릭스 교차 검증 V1~V3을 B-1~B-5 결정에 맞춰 확정 초안화했다.
- 수도·동부 각 10마리 샘플 + 드랍 61행(0% 명시 4행 포함) + 리네임 16건으로 1차 시드를 채웠다.

## 남은 확인 포인트
- `elite` 컬럼 승격 시점, 생활 재료 guard 유지 기간, 태그 풀 13종 고정 여부.
- 마스터 플래닝 생활 재료 언급 재분류 전수 확인.

## 다음 추천 작업
- 서·남·북 샘플 확대(v1.1), R1~R6 린트 구현 위임, `/몬스터` 봇 커맨드 포맷 초안.
