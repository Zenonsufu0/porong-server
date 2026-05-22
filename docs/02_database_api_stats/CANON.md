# 02. DB / 경제 / 통계 — CANON

> **[STATUS: CANON]** — DB, API, 경제 수치, 강화/잠재 수치의 공식 기준.

## 공식 기준

| 항목 | 기준 |
|---|---|
| 저장 구조 | SQLite `empire.db` + 플레이어 JSON |
| API | EmpireRPG HTTP API, 포트 8765 |
| 강화 비용 | `economy_numbers_v2.md`의 T1 1~25강 표 |
| 강화석 | 강화석 파편 시스템 폐지. `mat_stone_enhance` 직접 드랍/소모 |
| 방어구 강화석 | `ceil(무기 강화석 ÷ 1.5)` |
| 잠재능력 | 커먼/레어/에픽/유니크/레전더리 모두 1차 구현 |
| 큐브 | 1회 5,000G. 전 라인 재롤 + 등업 시도. 큐브 촉매 없음 |
| 장비 이름 변경권 | 10,000G |

## 참조 우선순위

| 문서 | 역할 |
|---|---|
| `../final_master_plan.md` §7, §15 | 성장 경제와 API 요약 |
| `economy_numbers_v2.md` | 강화 비용, 골드 경제, 경제 운영 지표 |
| `potential_options_v1.md` | 잠재 옵션 풀과 등급/라인 구조 |
| `equipment_growth_spec.md` | 장비 성장 상세 |
| `enhancement_droprate_v1.md` | 강화석 드랍률 검토 참조. 단, 파편 표기는 구버전으로 본다 |
| `../01_plugin_architecture/implementation_reference.md` | DB 스키마와 API 구현 상세 |

## 충돌 처리

구버전 문서의 강화석 파편, 보조재 B/C, 큐브 촉매, 마력 경제 항목은 폐지된 기준이다. 실제 구현 기준은 이 문서와 `economy_numbers_v2.md`의 2026-05-22 반영 내용을 우선한다.
