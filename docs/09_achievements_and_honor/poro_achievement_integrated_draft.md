# 포로 서버 업적 시스템 통합 초안

## 문서 목적
업적 관련 4개 초안(`poro_achievement_system_outline.md` + `poro_achievement_reward_and_ui_structure.md` + `poro_achievement_seed_config_table.md` + `poro_achievement_master_table.md`)을 단일 통합 문서로 재편. 원본 4파일은 병합 직후 삭제.

핵심 원칙:
- 업적은 숫자 놀음이 아니라 **플레이 기록과 명예를 남기는 장치**.
- 전투·보스·탐험·생활·영지·특별 6카테고리 고르게 포괄.
- 칭호·프로필 뱃지·영지 전시·명예의 전당과 연결.
- **성능 보상 최소화**, 명예형 보상 중심.

## 1. 카테고리 구조

### 전투 업적
- 첫 결전 클리어 / 무사망 클리어 / 데카 0 생존 / 특정 보스 최초 클리어

### 탐험 업적
- 지역 핵심 구역 발견 / 숨겨진 루트 발견 / 테마별 유적·장치 발견

### 생활 업적
- 첫 제작·만찬·연금·수확 / 특정 생활 레벨 도달

### 영지 업적
- 영지 해금 / 시설 Lv3 / 테마 시설 설치 / 전시 트로피

### 특별·극상위 업적
- 이벤트성 클리어 / 극상위 최초 클리어 / 조건부 도전

## 2. 보상 구조

### 추천
- 칭호, 프로필 뱃지, 기록 문구, 영지 전시물, 소량 상징 재화

### 비추천
- 강한 전투 성능 보상
- 미달성 유저와 큰 격차 만드는 보상

### 일반 업적 보상
- 소형 칭호 / 프로필 배지 / 소량 골드·상징 재화 / 기록 문구

### 지역·보스 업적 보상
- 지역 상징 칭호 / 영지 트로피 / 전용 휘장

### 극상위 업적 보상
- 전승 칭호 / 후광·외형 / 명예의 전당 노출 / 계정 전승 유물

## 3. UI 구조

### 업적 메뉴
- 카테고리별 탭
- 진행도 표시
- 완료 업적 강조
- 보상 미수령 표시

### 프로필 연결
- 대표 업적 1~3개 노출
- 칭호/뱃지 선택 장착

### 결과창 연동
- 보스 클리어 후 새 업적 달성 시 즉시 노출

## 4. Seed / Config 필드
- `achievement_id`
- `category`
- `achievement_name`
- `condition_type`
- `condition_target_id`
- `condition_amount`
- `reward_type`
- `reward_target_id`
- `reward_amount`
- `is_hidden`
- `is_repeatable`
- `notes`

### Seed 예시
| achievement_id | category | name | condition_type | target | amount | reward_type | reward_target | amount | hidden |
|---|---|---|---|---|---:|---|---|---:|---|
| ach_clear_ragnes | boss | 종화를 넘은 자 | BOSS_CLEAR | ragnes | 1 | TITLE | title_ragnes_clear | 1 | false |
| ach_first_craft | life | 첫 제작 | CRAFT_ANY | any | 1 | BADGE | badge_first_craft | 1 | false |
| ach_estate_unlock | estate | 나만의 거점 | ESTATE_UNLOCK | estate | 1 | TITLE | title_estate_unlock | 1 | false |
| ach_server_first | special | 선발대 | SERVER_FIRST_CLEAR | any_extreme | 1 | RECORD | hall_of_fame_entry | 1 | true |

## 5. 업적 마스터 표

### 5-1. 전투 / 보스 업적
| 업적 ID | 업적명 | 조건 | 보상 방향 |
|---|---|---|---|
| ach_clear_first_duel | 첫 결전 | 아무 결전 보스 1회 클리어 | 칭호 조각·기록 |
| ach_clear_ragnes | 종화를 넘은 자 | 라그네스 클리어 | 지역 칭호 |
| ach_clear_morvain | 백빙을 헤친 자 | 모르바인 클리어 | 지역 칭호 |
| ach_clear_hazard | 폭풍의 틈을 본 자 | 하자드 클리어 | 지역 칭호 |
| ach_clear_serkain | 판결을 넘은 자 | 세르카인 클리어 | 지역 칭호 |
| ach_no_death_clear | 무사망 돌파 | 보스전 무사망 클리어 | 뱃지 |
| ach_zero_deathcount_clear | 최후 생존 | 데카 0 상태 생존 클리어 | 기록 문구 |
| ach_extreme_first_clear | 전승의 문을 연 자 | 아무 극상위 첫 클리어 | 전승 칭호 |

### 5-2. 탐험 업적
| 업적 ID | 업적명 | 조건 | 보상 방향 |
|---|---|---|---|
| ach_discover_hidden_route | 숨겨진 길 | 비밀 루트 1회 발견 | 탐험 뱃지 |
| ach_east_core_discover | 숲의 심부 발견 | 동부 핵심 구역 발견 | 지역 기록 |
| ach_west_core_discover | 폭풍핵 관측 | 서부 핵심 구역 발견 | 지역 기록 |
| ach_south_core_discover | 화맥 심부 발견 | 남부 핵심 구역 발견 | 지역 기록 |
| ach_north_core_discover | 백빙 성소 발견 | 북부 핵심 구역 발견 | 지역 기록 |
| ach_sahar_truth_discover | 진실의 모래 | 사하르 핵심 구역 발견 | 지역 기록 |
| ach_arkanon_core_discover | 공명 심부 접근 | 아르카논 핵심 구역 발견 | 지역 기록 |

### 5-3. 생활 업적
| 업적 ID | 업적명 | 조건 | 보상 방향 |
|---|---|---|---|
| ach_first_craft | 첫 제작 | 아무 제작 1회 | 생활 입문 칭호 |
| ach_first_feast | 첫 만찬 | 만찬 1회 제작 | 프로필 뱃지 |
| ach_first_potion | 첫 연금 | 물약 1회 제작 | 기록 |
| ach_life_lv10 | 숙련의 시작 | 아무 생활 레벨 10 달성 | 생활 뱃지 |
| ach_life_lv20 | 장인 | 아무 생활 레벨 20 달성 | 칭호 |
| ach_first_harvest | 첫 수확 | 영지 첫 수확 | 영지 기록 |

### 5-4. 영지 업적
| 업적 ID | 업적명 | 조건 | 보상 방향 |
|---|---|---|---|
| ach_estate_unlock | 나만의 거점 | 영지 해금 | 영지 칭호 |
| ach_facility_lv3 | 숙련된 관리 | 시설 Lv3 1회 달성 | 영지 뱃지 |
| ach_theme_facility_install | 테마 개척 | 테마 시설 1개 설치 | 전시 아이템 |
| ach_trophy_place | 전시의 시작 | 영지 트로피 1개 배치 | 기록 문구 |

### 5-5. 특별 / 극상위 업적
| 업적 ID | 업적명 | 조건 | 보상 방향 |
|---|---|---|---|
| ach_clear_agner | 불멸화룡의 종화를 넘은 자 | 아그네르 첫 클리어 | 전승 칭호 |
| ach_clear_eldheim | 백설 장막의 종결자 | 엘드하임 첫 클리어 | 후광·뱃지 |
| ach_clear_setra | 기만의 막을 찢은 자 | 세트라 첫 클리어 | 전승 칭호 |
| ach_clear_carmen | 최종 공명 붕괴 생존자 | 카르멘 첫 클리어 | 대표 전승 칭호 |
| ach_extreme_no_death | 완전 정복 | 극상위 무사망 클리어 | 전용 휘장 |
| ach_server_first | 선발대 | 서버 최초 클리어 | 명예의 전당 표시 |

## 6. 한 줄 요약
**업적은 전투·보스·탐험·생활·영지·특별 6카테고리로 나누고, 조건·보상을 seed/config로 관리하며, 보상은 칭호·뱃지·영지 전시·명예의 전당 등 명예형 보상 중심으로 연결한다. 강한 전투 성능 보상은 의도적으로 배제.**

## 상위 문서 참조
- 마스터플래닝: `../poro_master_planning.md`
- 칭호 통합: `poro_title_integrated_draft.md`
- 명예의 전당 UI: `poro_hall_of_fame_ui_structure.md`
- 보스 전승 보상: `../02_boss_rewards_ui_logs/poro_boss_symbolic_legacy_rewards_draft.md`
