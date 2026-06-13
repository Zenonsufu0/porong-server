# 02. DB / 경제 / 통계 — CANON

> **[STATUS: CANON]** — DB, API, 경제 수치, 강화/잠재 수치의 공식 기준.

## 공식 기준

| 항목 | 기준 |
|---|---|
| 저장 구조 | SQLite `zenon_rpg.db` + 플레이어 JSON |
| API | ZenonRPG HTTP API, 포트 8765 |
| 강화 비용 | `economy_numbers_v2.md`의 T1 1~25강 표 |
| 강화석 | 강화석 파편 시스템 폐지. DB 가상 재화로 처치 시 직접 적립/소모. 실물 아이템 드랍 아님 |
| 방어구 강화석 | `ceil(무기 강화석 ÷ 1.5)` |
| 잠재능력 | 커먼/레어/에픽/유니크/레전더리 모두 1차 구현 |
| 큐브 | 1회 500G. 전 라인 재롤 + 등업 시도. 큐브 촉매 없음 |
| 큐브 조각 | 필드보스 확정 드랍 (DL-057). 10조각 적립 즉시 큐브 1개 자동 교환 (DL-056). 스코어보드에 잔량 표시 |
| 장비 이름 변경권 | 10,000G |
| 기본 전승 | 0G (무료) |
| 등급전승권 | 100,000G |
| 세부스탯전승권 | 100,000G |
| 강화 흔적 3종 | 별의 흔적 / 달의 흔적 / 태양의 흔적. 강화 성공률 보정 아이템. 영지 공방 가공기 제작. |

## 참조 우선순위

| 문서 | 역할 |
|---|---|
| `../final_master_plan.md` — "전투와 장비 성장", "데이터와 API" | 성장 경제와 API 요약 |
| `economy_numbers_v2.md` | 강화 비용, 골드 경제, 경제 운영 지표 |
| `potential_options_v1.md` | 잠재 옵션 풀과 등급/라인 구조 |
| `equipment_growth_spec.md` | 장비 성장 상세 |
| `boss_clear_stats_spec.md` | 보스 클리어 통계 수집 스펙 (DB 스키마·API·운영 기준) |
| `../01_plugin_architecture/implementation_reference.md` | DB 스키마와 API 구현 상세 |
| `../06_fields_bosses/drop_tables_v1.md` | 강화석·큐브 조각 드랍 상세. CANON과 충돌하는 구 수치는 정리 대상 |
| `item_master_v1.md` | 전체 item_id 목록, MC 재질, CMD 범위 할당 기준 |

## 충돌 처리

구버전 문서의 강화석 파편, 보조재 B/C, 큐브 5,000G, 큐브 촉매, 전승권 5,000G/50,000G, 마력 경제 항목은 폐지된 기준이다. 실제 구현 기준은 이 문서와 `economy_numbers_v2.md`의 2026-05-22 반영 내용을 우선한다.
