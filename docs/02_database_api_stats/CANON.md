# 02. DB / 경제 / 통계 — CANON

> **[STATUS: CANON]** — 이 폴더 도메인의 단일 정답 소스.  
> 다른 문서와 충돌 시 이 문서(및 final_master_plan.md §7)가 우선.

---

## 역할

골드 경제 수치, 강화 비용표, 드랍량, DB 스키마, API 엔드포인트 구조의 확정 소스.

---

## Canonical Source

| 섹션 | 위치 | 내용 |
|------|------|------|
| 골드 경제 전반 | `final_master_plan.md §7` | 몹 드랍, 강화 비용, 상점 가격 |
| 강화 비용표 (1~25강) | `final_master_plan.md §7.3` | M-3 확정 (2026-05-18) 기준 |
| 잠재 옵션 | `final_master_plan.md §6` | 큐브 시스템, 잠재 등급 |
| DB 스키마 | `final_설계_plan.md` | SQL DDL, 테이블 정의 |

---

## 참조 문서

| 문서 | 상태 | 역할 |
|------|------|------|
| `economy_numbers_v2.md` | DRAFT | 경제 수치 계산 참조 ⚠️ 강화비용 충돌 존재 |
| `_archive/economy_numbers_v1.md` | ARCHIVED | v2로 대체됨, 2026-05-22 archive 이동 완료 |
| `enhancement_droprate_v1.md` | DRAFT | 강화석 드랍률 계산 |
| `equipment_growth_spec.md` | DRAFT | 장비 성장 스펙 |
| `potential_options_v1.md` | DRAFT | 잠재 옵션 목록 |

---

## ⚠️ 알려진 충돌

| 항목 | 이전 값 | 현재 값 | 상태 |
|------|---------|---------|------|
| 1강 강화 비용 | ~~180G~~ | **2,000G (M-3 확정)** | ✅ 수정 완료 (2026-05-22 DL-005) |

---

## TODO

- [ ] `economy_numbers_v2.md` 강화 비용 수치를 final_master_plan §7.3 기준으로 수정 (PHASE 3)
- [x] `economy_numbers_v1.md` → `_archive/` 이동 **완료 (2026-05-22)**
- [ ] DB 스키마 테이블을 이 CANON에 직접 기재 또는 `implementation_reference.md` 참조
- [ ] API 엔드포인트 목록 정리 (index.md 내용 검증 후 통합)
- [ ] 강화석 드랍률 (`enhancement_droprate_v1.md`) 수치 검증 후 CANON 반영
