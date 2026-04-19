# 포로 문서 병합·최소화 제안 v1

## 문서 목적
`docs/` 하위 170+ 개 md 파일을 정합성·가독성·유지보수성 기준으로 정리하기 위한 **병합·삭제 후보 제안**. 실제 실행은 **사용자 승인 후**에만 진행. 정보 손실 없는 병합을 원칙으로 하고, 확실한 구식 문서만 삭제 제안.

## 카테고리별 후보

---

## A. 명백한 구식 버전 — 삭제 권고

이미 최신판이 존재하고 본 파일들은 과거 스냅샷.

| 파일 | 크기 | 최신판 | 판정 |
|---|---|---|---|
| `00_index_and_execution/poro_master_planning_v2.md` | 22KB | `docs/poro_master_planning.md` (143KB, 병합 확정본) | 🔴 삭제 권고 |
| `00_index_and_execution/poro_matser_plannigV1.md` | 17KB | 동일 | 🔴 삭제 권고 (파일명 오타까지 존재) |

사용자 결정 포인트: 이력 보존 필요하면 `docs/_archive/` 폴더로 이동 대안도 가능.

---

## B. BetonQuest 제거 확정에 따른 **사용 중지 문서** — 삭제 권고

마스터플래닝에서 "BetonQuest 본 시즌 제거 + EmpireRPG 커스텀 대화 UI로 통합" 확정. 관련 문서는 **역사 자료**로만 의미 있음.

| 파일 | 크기 | 판정 |
|---|---|---|
| `13_external_plugins_and_custom_ui/poro_betonquest_codex_prompt.md` | 2KB | 🔴 삭제 권고 |
| `13_external_plugins_and_custom_ui/poro_betonquest_package_structure_draft.md` | 2.2KB | 🔴 삭제 권고 |
| `13_external_plugins_and_custom_ui/poro_npc_and_betonquest_parallel_rollout_plan.md` | 1.9KB | 🔴 삭제 권고 |
| `13_external_plugins_and_custom_ui/poro_citizens_betonquest_mythicmobs_integration_checklist.md` | 1.9KB | 🟠 부분 병합 (Citizens/MythicMobs 항목만 역할 매트릭스로 이관 후 삭제) |

---

## C. Codex 프롬프트 파편 — **병합 후 삭제** 권고

Codex MCP 활용 원칙은 CLAUDE.md에 남아있으나, 파편 프롬프트 문서들은 실제 구현 참조가 아닌 운영 메모 수준. **통합 `codex_usage_guide.md` 1개로 축약** 또는 전면 삭제 제안.

| 파일 | 크기 | 판정 |
|---|---|---|
| `00_index_and_execution/poro_codex_prompt_set_expanded_v2.md` | 4.2KB | 🟠 병합 대상 |
| `00_index_and_execution/poro_codex_prompt_bundle_final.md` | 4.3KB | 🟠 병합 대상 |
| `00_index_and_execution/poro_codex_execution_order.md` | 6.6KB | 🟠 병합 대상 |
| `00_index_and_execution/poro_codex_implementation_guide.md` | 10.3KB | 🟠 병합 대상 |
| `00_index_and_execution/poro_codex_module_priority_final.md` | 1.8KB | 🟠 병합 대상 |
| `00_index_and_execution/poro_life_codex_prompt_set.md` | 10.8KB | 🟠 병합 대상 |
| `00_index_and_execution/poro_life_codex_guide.md` | 15KB | 🟠 병합 대상 |
| `00_index_and_execution/poro_seed_bundle_handoff_guide.md` | 1.4KB | 🟠 병합 대상 |
| `13_external_plugins_and_custom_ui/poro_dialogue_ui_codex_prompt_step1.md` | 1.1KB | 🟠 병합 대상 |
| `13_external_plugins_and_custom_ui/poro_dialogue_ui_codex_prompt_step2.md` | 1.2KB | 🟠 병합 대상 |
| `13_external_plugins_and_custom_ui/poro_dialogue_ui_codex_prompt_step3.md` | 1.3KB | 🟠 병합 대상 |
| `13_external_plugins_and_custom_ui/poro_citizens_auto_spawn_codex_prompt.md` | 2KB | 🟠 병합 대상 |
| `13_external_plugins_and_custom_ui/poro_boss_entry_theme_ui_codex_prompt.md` | 2.3KB | 🟠 병합 대상 |

**제안**: 위 13개를 **실구현에 아직 살아있는 내용(예: 대화 UI 아키텍처, 보스 입장 UI seed)** 만 `13_external_plugins_and_custom_ui/poro_<topic>_draft.md` 본 문서에 흡수하고 codex 프롬프트 파일 자체는 전부 삭제. 포로는 codex를 "타당성 검토용"으로만 쓰는 원칙이므로 구체 프롬프트 텍스트를 저장할 이유 낮음.

---

## D. 디스코드 봇 관련 파편 — **1개 통합** 권고

현재 5개 파일이 분산되어 있음. 모두 1~1.5KB 수준의 얇은 문서.

| 파일 | 크기 |
|---|---|
| `11_discord_bot_and_user_views/poro_discord_bot_response_format_draft.md` | 1.2KB |
| `11_discord_bot_and_user_views/poro_discord_slash_command_spec_draft.md` | 1.3KB |
| `11_discord_bot_and_user_views/poro_discord_payload_response_examples.md` | 1.3KB |
| `11_discord_bot_and_user_views/poro_discord_bot_command_master_draft.md` | 1.4KB |
| `11_discord_bot_and_user_views/poro_discord_bot_embed_examples_draft.md` | 1.5KB |

**제안**: 단일 `poro_discord_bot_integrated_spec_draft.md`로 병합 — 섹션 구조:
1. 슬래시 명령어 마스터
2. 응답 포맷 / 페이로드 예시
3. 임베드 예시
4. 계정 링크 연동 (기존 `13_external_plugins_and_custom_ui/poro_discord_account_link_draft.md` 링크)
5. 약관 선행 플로우 연동 (기존 `poro_discord_first_agreement_flow_draft.md` 링크)

---

## E. 보스 입장 UI 파편 — **1개 통합** 권고

| 파일 | 크기 |
|---|---|
| `13_external_plugins_and_custom_ui/poro_boss_entry_ui_seed_and_flow_draft.md` | 1.4KB |
| `13_external_plugins_and_custom_ui/poro_boss_entry_theme_hub_ui_draft.md` | 2KB |

**제안**: `poro_boss_entry_ui_integrated_draft.md`로 병합. (codex 프롬프트 버전은 C에서 삭제)

---

## F. 음식·연금 3파일 — **1개 통합** 검토

| 파일 | 크기 |
|---|---|
| `04_consumables_and_alchemy/poro_food_feast_potion_seed_table.md` | 3.3KB |
| `04_consumables_and_alchemy/poro_food_feast_potion_effect_table.md` | 8.1KB |
| `04_consumables_and_alchemy/poro_food_feast_potion_db_structure.md` | 10.6KB |
| `04_consumables_and_alchemy/poro_consumable_effect_seed_config_table.md` | 1.9KB |
| `04_consumables_and_alchemy/poro_cooking_alchemy_details.md` | 7.6KB |

**제안**: 효과 + seed + DB 3개를 `poro_food_feast_potion_master.md` 1개로 통합. `poro_cooking_alchemy_details.md`는 레시피·제작 흐름 전용으로 유지. `poro_consumable_effect_seed_config_table.md`는 마스터에 흡수 후 삭제.

---

## G. 성취·칭호 파편 — **부분 통합** 검토

| 파일 | 크기 |
|---|---|
| `09_achievements_and_honor/poro_achievement_system_outline.md` | 1.6KB |
| `09_achievements_and_honor/poro_achievement_reward_and_ui_structure.md` | 1.3KB |
| `09_achievements_and_honor/poro_achievement_master_table.md` | 3.9KB |
| `10_seed_and_config_tables/poro_achievement_seed_config_table.md` | 1.5KB |
| `09_achievements_and_honor/poro_title_master_list_draft.md` | 1.3KB |
| `09_achievements_and_honor/poro_title_badge_profile_equip_structure.md` | 1.4KB |
| `09_achievements_and_honor/poro_hall_of_fame_ui_structure.md` | 1.2KB |

**제안**: 
- `poro_achievement_integrated.md` (outline + reward/UI + master table + seed config 4개 → 1개)
- `poro_title_integrated.md` (master list + badge/equip 2개 → 1개)
- `poro_hall_of_fame_ui_structure.md`는 유지

---

## H. 대화 UI 아키텍처 — **본 문서 흡수** 권고

| 파일 | 크기 |
|---|---|
| `13_external_plugins_and_custom_ui/poro_dialogue_ui_architecture_draft.md` | 1.2KB |

너무 얇음. `poro_act1_custom_ui_draft.md` (7KB)에 흡수 검토.

---

## I. 유지 필수 (손대지 말 것)

다음 파일들은 **현재 작업의 중심축**이므로 병합·삭제 대상 아님:

- `docs/poro_master_planning.md` (143KB) — 마스터 플래닝
- `docs/poro_server_docs_structure_v6_tree.md` (6.5KB) — 문서 구조 트리
- `docs/00_index_and_execution/poro_docs_index.md` (5.4KB) — 인덱스
- `docs/00_index_and_execution/poro_open_questions_registry.md` (신규) — 오픈 질문 레지스트리
- `docs/00_index_and_execution/poro_user_agreement_v1_draft.md` — 약관 v1
- `docs/00_index_and_execution/poro_discord_first_agreement_flow_draft.md` — 디스코드 하이브리드
- `docs/00_index_and_execution/poro_flag_store_v01_sprint_plan.md` — 플래그 저장소
- `docs/03_life_system_core/poro_estate_level_triaxial_matrix_draft.md` — 영지 3축
- `docs/03_life_system_core/poro_estate_power_system_review_draft.md` — 영지 전력
- `docs/03_life_system_core/poro_factory_estate_draft.md` / `poro_factory_facility_spec_draft.md` — 공장형 영지
- `docs/03_life_system_core/poro_allinone_workbench_draft.md` — 올인원 조합대
- `docs/05_classes_and_balance/poro_class_engraving_master_list_draft.md` / `poro_common_engraving_master_list_draft.md` / `poro_t2_set_effect_detail_table.md` — 각인·세트
- `docs/08_story_and_quests/` 하위의 `act1_capital_main/` + `poro_act1_theme_*_storyline.md` + `poro_act1_theme_foreshadowing_items_draft.md` + `poro_act1_theme_sub_foreshadowing_quests_draft.md` + `poro_act1_guild_board_repeat_quest_draft.md` — 스토리 축
- `docs/10_seed_and_config_tables/poro_auction_system_draft.md` — 경매
- `docs/13_external_plugins_and_custom_ui/poro_memory_library_ui_schema_draft.md` — 차원도서관
- `docs/13_external_plugins_and_custom_ui/poro_discord_account_link_draft.md` — 디스코드 링크 (A)안
- `docs/13_external_plugins_and_custom_ui/poro_traveler_lantern_spec_draft.md` — 여행자의 등불
- `docs/13_external_plugins_and_custom_ui/poro_act1_custom_ui_draft.md` — 1막 커스텀 UI

---

## 실행 단계 제안

### 1단계 (완료 — 2026-04-19 실행)
- ✅ A (구식 2파일) **archive 이동 완료**: `docs/_archive/poro_master_planning_v2.md`, `docs/_archive/poro_matser_plannigV1.md`
- ✅ B (BetonQuest 4파일) **완전 삭제 완료** (사용자 결정: "BetonQuest 완전 삭제"). 체크리스트 파일의 Citizens·MythicMobs·EssentialsX·WorldGuard 섹션은 `poro_plugin_role_matrix_final.md`에 흡수
- ✅ 플러그인 역할 매트릭스 BetonQuest "유지" → "제거"로 정합성 수정
- ✅ E (보스 입장 UI 2파일) **통합 완료**: `poro_boss_entry_ui_integrated_draft.md` 생성, 원본 2파일 삭제

### 2단계 (철회 — 사용자 결정: Codex 전부 유지)
- ~~C (Codex 프롬프트 13파일) 병합·삭제~~ **철회**. 모든 codex 관련 파일은 현상 유지.

### 3단계 (완료 — 2026-04-19 실행)
- ✅ **D (디스코드 봇 5파일 → 1파일 통합)**: `poro_discord_bot_integrated_spec_draft.md` 생성, 원본 5파일(`_response_format_draft` / `_slash_command_spec_draft` / `_payload_response_examples` / `_bot_command_master_draft` / `_bot_embed_examples_draft`) 삭제
- ✅ **F (음식·연금 4파일 → 1파일 통합)**: `poro_food_feast_potion_master_draft.md` 생성, 원본 4파일(`_seed_table` / `_effect_table` / `_db_structure` / `poro_consumable_effect_seed_config_table`) 삭제. `poro_cooking_alchemy_details.md`는 레시피 흐름 전용으로 유지.
- ✅ **G (성취·칭호 6파일 → 2파일 통합)**: `poro_achievement_integrated_draft.md` (outline + reward_ui + seed_config + master_table 4파일 통합) + `poro_title_integrated_draft.md` (master_list + badge_equip 2파일 통합). 원본 6파일 삭제. `poro_hall_of_fame_ui_structure.md`는 유지.

### 4단계 (얇은 문서 흡수)
- ⏳ H (대화 UI 아키텍처 1파일을 `poro_act1_custom_ui_draft.md`에 흡수) 대기

### 각인 시약 제도 전면 삭제 (2026-04-19 별도 결정)
- ✅ master planning 4곳(각인 시약 원료·보류 결정·마력 다목적 축·월렛 경계선) 정리
- ✅ open_questions_registry 4곳 취소선 처리
- ✅ `poro_allinone_workbench_draft.md` 7곳(특수 제작 카테고리·4행 레시피 삭제·메모·해금 표·직업 특화·오픈 질문·최우선 오픈) 정리
- ✅ `poro_act1_branch_a_m8_10_scene_cards.md` 7곳(4테마 메인 10 보상·공통 게이트 설명·오픈 질문 3건) 정리

---

## 우려사항 / 확인 요청

1. **이력 보존 정책**: 구식 파일을 완전 삭제 vs `docs/_archive/` 이동 중 어느 쪽?
2. **Codex 관련 자산**: 전부 삭제해도 되는지, 아니면 "codex 사용 원칙" 1장만 남길지?
3. **BetonQuest 역사 자료**: 완전 삭제 vs 마이그레이션 추적용 1장만 남길지?
4. **병합 작업 담당**: 본 에이전트가 진행 vs 사용자 수동 확인 병행?

## 상위 문서 참조
- 마스터플래닝: `../poro_master_planning.md`
- 오픈 질문 레지스트리: `poro_open_questions_registry.md`
- 문서 구조 트리: `../poro_server_docs_structure_v6_tree.md`
- 인덱스: `poro_docs_index.md`
