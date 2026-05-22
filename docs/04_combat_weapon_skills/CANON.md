# 04. 전투 / 무기 / 스킬 — CANON

> **[STATUS: CANON]** — 이 폴더 도메인의 단일 정답 소스.  
> 다른 문서와 충돌 시 이 문서(및 final_master_plan.md §5)가 우선.

---

## 역할

6무기 클래스 정의, 스킬 입력 방식, 쿨타임, 자기 자원(스택/에너지), 전투 수치(ATK/크리/DPS), 직업각인의 확정 소스.

---

## Canonical Source

| 섹션 | 위치 | 내용 |
|------|------|------|
| 6무기 클래스 + 스킬표 | `final_master_plan.md §5` | 클래스별 스킬 3종 + 자원 정의 |
| 전투 수치 공식 | `final_master_plan.md §5.2` | ATK 공식, 크리 공식, DPS 기준 |
| 직업각인 | `final_master_plan.md §5.3` | 1차 시즌: 고유 각인만, 공용 각인 제외 |
| 레벨/스탯 성장 | `final_master_plan.md §4` | 레벨 1~30, 기본 스탯 성장 |

---

## 참조 문서

| 문서 | 상태 | 역할 |
|------|------|------|
| `weapon_skills_v1.md` | DRAFT | 스킬 설계 상세 ⚠️ 망치→도끼 미수정 |
| `combat_balance_v2.md` | DRAFT | 전투 수치 계산 참조 (현재 최신) |
| `level_stat_system_v1.md` | DRAFT | 레벨별 스탯 테이블 |
| `item_grade_substat_v1.md` | DRAFT | 아이템 등급별 서브스탯 |
| `season_boss_stats_v1.md` | DRAFT | 시즌보스 스탯 참조 |
| `_archive/atk_dps_baseline_v1.md` | ARCHIVED | 구 선형 공식 기반, 2026-05-22 이동 완료 |
| `_archive/dps_analysis_v1.md` | ARCHIVED | 수치 변경으로 무효화, 2026-05-22 이동 완료 |

---

## ⚠️ 잔존 충돌

| 항목 | 파일 | 상태 |
|------|------|------|
| "망치" 표현 | `combat_balance_v2.md` | 미수정 — PHASE 3 대상 |

---

## TODO

- [x] `weapon_skills_v1.md` 전체에서 "망치" → "도끼" 수정 **완료 (2026-05-22 DL-003)**
- [x] `atk_dps_baseline_v1.md` → `_archive/` 이동 **완료 (2026-05-22)**
- [x] `dps_analysis_v1.md` → `_archive/` 이동 **완료 (2026-05-22)**
- [ ] `combat_balance_v2.md` 수치를 final_master_plan §5.2 기준으로 검증
- [ ] 직업각인 목록을 이 CANON에 직접 기재
- [ ] 공용각인 1차 시즌 제외 명시 확인 (final_master_plan §5.3)
