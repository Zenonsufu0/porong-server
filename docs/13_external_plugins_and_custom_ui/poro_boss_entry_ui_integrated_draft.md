# 포로 서버 보스 입장 UI 통합 초안

## 문서 목적
이전 두 초안 `poro_boss_entry_ui_seed_and_flow_draft.md`(seed 스키마) + `poro_boss_entry_theme_hub_ui_draft.md`(구조·정당성)를 하나의 문서로 통합. 병합 후 원본 2파일은 삭제 처리.

## 1. 결론 — 테마 허브 중심 구조

보스 입장은 **난이도별 분류보다 테마별 분류가 더 적합**하다.

이유:
- 포로 서버는 지역/세계관/탐험 감각이 강하다.
- 유저가 "어려운 보스"보다 "남부 화맥 계열", "북부 설혼 계열"처럼 기억하게 만드는 편이 서버 정체성에 맞다.
- 테마 허브 안에서 결전 / 극상위 / 심층 / 필드보스를 계층적으로 보여주면 난이도 정보도 같이 제공할 수 있다.
- 나중에 지역 확장, 신규 보스 추가, 전승/세트 장비 연결도 훨씬 자연스럽다.

난이도는 **보조 필터**로만 제공.

## 2. 3층 구조

### 1차 선택 — 테마 허브
추천 테마:
- 수도 / 황성
- 동부 / 숲과 침식
- 서부 / 황야와 폭풍
- 남부 / 화맥과 과열
- 북부 / 설혼과 백빙
- 사하르 / 환영과 계약
- 아르카논 / 공명과 구획

### 2차 선택 — 해당 테마 보스 목록
각 보스 카드에 표시:
- 보스 이름
- 보스 계층(필드/심층/결전/극상위)
- 추천 파티 감각
- 해금 상태
- 첫 클리어 여부
- 전승/세트 연결 여부

### 3차 선택 — 상세 입장 화면
- 보스 설명
- 요구 조건
- 입장 가능 여부
- 파티 인원
- 예상 보상
- 입장 버튼

## 3. Seed 스키마

### 3.1 테마 허브 seed 추천 필드
| 필드 | 설명 |
|---|---|
| `theme_id` | 내부 ID |
| `theme_name` | 표시 이름 |
| `theme_icon` | 아이콘 키 |
| `theme_description` | 허브 설명 |
| `display_order` | 정렬 순서 |
| `is_enabled` | 테마 활성화 여부 |

예시:
| theme_id | theme_name | theme_icon | display_order |
|---|---|---|---:|
| south | 남부 화맥 | fire_core | 1 |
| north | 북부 설혼 | frost_core | 2 |
| west | 서부 황야 | sand_storm | 3 |
| east | 동부 침식 숲 | spirit_leaf | 4 |
| sahar | 사하르 | mirage_eye | 5 |
| arkanon | 아르카논 | resonance_cube | 6 |
| capital | 수도 황성 | imperial_seal | 7 |

### 3.2 보스 카드 seed 추천 필드
- `boss_id`
- `theme_id`
- `boss_name`
- `boss_tier` (필드 / 심층 / 결전 / 극상위)
- `unlock_condition_type`
- `unlock_condition_value`
- `entry_ui_profile_id`
- `loot_theme_id`
- `is_first_clear_reward`
- `is_legacy_linked`
- `display_order`

### 3.3 보스 상세 profile 추천 필드
- `profile_id`
- `boss_id`
- `title_text`
- `flavor_text`
- `recommended_party_text`
- `entry_requirement_text`
- `reward_summary_text`
- `warning_text`
- `confirm_button_text`

## 4. UI 흐름

1. NPC 클릭 (Citizens 껍데기 + EmpireRPG InteractionProfile)
2. theme hub UI open
3. theme select
4. boss list UI open
5. boss card click
6. boss detail UI open
7. entry validate (EmpireRPG 단독 판정)
8. success → boss run start (MythicMobs 스폰 + EmpireRPG 상태 관리)
9. fail → reason popup

## 5. 권장 보조 필터
- 전체
- 필드
- 심층
- 결전
- 극상위
- 첫 클리어 가능
- 전승 연계

## 6. 한 줄 요약
**보스 입장은 난이도 탭 중심보다 테마 허브 중심이 더 포로 서버답고, 테마 안에서 계층(필드/심층/결전/극상위)을 함께 보여주면서 seed 3층(테마 허브 / 보스 카드 / 상세 profile)으로 관리하는 구조가 가장 적합하다.**

## 상위 문서 참조
- 마스터플래닝: `../poro_master_planning.md`
- 플러그인 역할 매트릭스: `poro_plugin_role_matrix_final.md`
- 1막 커스텀 UI: `poro_act1_custom_ui_draft.md`
- Citizens NPC seed: `poro_citizens_npc_seed_structure_draft.md`
