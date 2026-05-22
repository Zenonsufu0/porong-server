# Docs Archive

> **[STATUS: ARCHIVED]** — 현재 공식 기준에서 제외된 문서 보관소. 활성 설계 판단에는 대체 문서를 우선한다.

## PHASE 3 — 초안 검토 및 경제/전투 수치 정리 (이동일: 2026-05-22)

| 문서 | Archive 이유 | 대체 문서 |
|---|---|---|
| `00_master_plan.md` | Citizens·마력 시스템 포함 구버전 마스터플랜. 2026-05-20 `final_master_plan.md`로 대체됨. | `docs/final_master_plan.md` |
| `economy_numbers_v1.md` | ×1.5 강화석 비율 조정 미반영. v2에서 전면 교체됨. | `docs/02_database_api_stats/economy_numbers_v2.md` |
| `atk_dps_baseline_v1.md` | 구 선형 ATK 공식 기반 베이스라인. v2.1 전투 설계 이후 무효. | `docs/04_combat_weapon_skills/combat_balance_v2.md` |
| `dps_analysis_v1.md` | 스킬 쿨타임 확정(v2.1) 이전 DPS 분석. 수치가 현행 설계와 불일치. | `docs/04_combat_weapon_skills/combat_balance_v2.md` |
| `design_decisions_confirmed.md` | `00_master_plan.md` 기반 결정 문서. 항목 대부분이 `final_master_plan.md`와 `decision_log.md`에 흡수됨. | `docs/final_master_plan.md`, `docs/decision_log.md` |
| `numbers_and_open_decisions.md` | 마력 시스템(T-6) 등 구 미결정 항목 포함. 현행 미확정 항목은 `final_master_plan.md`의 "미확정 항목"에서 관리. | `docs/final_master_plan.md`의 "미확정 항목" |
| `99_check.md` | 수치 검토 발견사항이 `economy_numbers_v2.md`에 반영 완료. | `docs/02_database_api_stats/economy_numbers_v2.md` |

## PHASE 5 — docs 리빌드 검수 (이동일: 2026-05-22)

| 문서 | Archive 이유 | 대체 문서 |
|---|---|---|
| `docs_restructure_plan.md` | 2026-05-22 문서 리빌드 작업 계획. 실행 완료된 계획 문서라 활성 설계 기준에서 제외. | `docs/decision_log.md`, 각 `CANON.md` |
| `master_plan_content_audit.md` | `final_master_plan.md` 축약을 위한 감사 보고서. 결과가 `final_master_plan.md`와 각 `CANON.md`에 반영됨. | `docs/final_master_plan.md`, 각 `CANON.md` |
| `11_remaining_decisions/index.md` | 미결정 항목 대부분이 `final_master_plan.md`와 각 도메인 문서에 흡수됨. | `docs/final_master_plan.md`의 "미확정 항목", `docs/decision_log.md` |

## PHASE 6 — economy 문서 정리 (이동일: 2026-05-22)

| 문서 | Archive 이유 | 대체 문서 |
|---|---|---|
| `enhancement_droprate_v1.md` | 강화석 파편 시스템 기반 드랍률 계산. 파편 시스템 폐지(강화석 직접 드랍/소모)로 전체 계산 방식 무효. | `docs/06_fields_bosses/drop_tables_v1.md` (직접 소모 기준 드랍률 재산정 필요) |

## PHASE 8 — GUI 구버전 문서 archive (이동일: 2026-05-22)

| 문서 | Archive 이유 | 대체 문서 |
|---|---|---|
| `gui_functional_specs.md` | §1~§4(강화·잠재·각인·전승)·§7(보스 정보) 레이아웃은 각 활성 문서로 복원됨. §5 공방(큐브 공방제작 60분 구버전)·§6 영지상태(마력/발전기 기반)는 폐기 기준 유지. | `gui_enhancement.md`, `gui_potential.md`, `gui_engraving.md`, `gui_succession.md`, `gui_boss_info.md` |
| `gui_territory_status.md` | 마력 현황, 발전기, 마력 과부하를 전제로 한 영지 상태 GUI. 마력/발전기 폐지 기준과 충돌. | `docs/05_island_farm_system/CANON.md`, `docs/05_island_farm_system/island_system_design.md` 기준으로 재작성 필요 |
| `gui_boss_info.md` | 강화석 파편 보상, 구 보스 정보, 구 타이머/보상 표기가 남아 현행 필드/보스 기준과 충돌. | `docs/06_fields_bosses/CANON.md`, `docs/06_fields_bosses/drop_tables_v1.md` 기준으로 재작성 필요 |

## PHASE 9 — 구현 레퍼런스 재축약 (이동일: 2026-05-22)

| 문서 | Archive 이유 | 대체 문서 |
|---|---|---|
| `implementation_reference_legacy.md` | 1,500줄 이상의 장문 구현 메모에 큐브 5,000G, 전승권 구가격, M-tag 미정, 구 GUI/보상 기준이 섞여 현재 CANON과 충돌. | `docs/01_plugin_architecture/implementation_reference.md`의 얇은 구현 진입점 + 각 도메인 `CANON.md`와 활성 상세 문서 |

## 참고

- 향후 archive 이동 문서는 삭제하지 말고 이 README에 이유와 대체 문서를 추가한다.
- PHASE별 섹션에 이동일과 함께 기록한다.
