# zenon-economy 문서 인덱스

마인크래프트(NeoForge 1.21.1) 기반 **경제발전 시뮬레이션** 서버 설계 문서. 구상 단계(문서만, 런타임 없음).

> **시작점:** 다음 세션은 [`task.md`](task.md) 핸드오프 헤더부터.

## 카테고리

| 폴더 | 문서 | 내용 |
|---|---|---|
| (루트) | [`task.md`](task.md) | 작업 인계·진행 상황·핸드오프 |
| (루트) | [`decision_log.md`](decision_log.md) | 결정 로그 `DL-E###` (단일 진실 보조) |
| (루트) | [`idea_inbox.md`](idea_inbox.md) | 미확정·검토 대기 |
| **01-vision** | [`concept.md`](01-vision/concept.md) | 비전·개요·통합 시스템 맵 |
| **02-design** | [`design.md`](02-design/design.md) | 시스템 상세 (§0~§12) |
| | [`spec.md`](02-design/spec.md) | 설계서 (구현 명세 §1~§11) |
| **03-economy** | [`population_model.md`](03-economy/population_model.md) | 주민 집계 동학 (가계·노동·인구) |
| | [`indicators.md`](03-economy/indicators.md) | 경제 지표 마스터 리스트 |
| | [`balance.md`](03-economy/balance.md) | P1 수치 밸런싱 (§0~§18) |
| **04-content** | [`content_catalog.md`](04-content/content_catalog.md) | 산출품·가공 체인·커스텀 아이템 |
| | [`agenda_catalog.md`](04-content/agenda_catalog.md) | 의회 안건 49종 |
| **05-data** | [`data_schema.md`](05-data/data_schema.md) | 영속 DB 엔티티 스키마 |
| **06-roadmap** | [`roadmap.md`](06-roadmap/roadmap.md) | 구현 로드맵·시즌1/2 MVP 경계·제작 체크리스트 |

## 읽는 순서 (신규 파악)

1. [`concept.md`](01-vision/concept.md) — 무엇을 만드는가
2. [`design.md`](02-design/design.md) → [`spec.md`](02-design/spec.md) — 어떻게 작동하는가
3. [`population_model.md`](03-economy/population_model.md) · [`balance.md`](03-economy/balance.md) — 경제 동학·수치
4. [`content_catalog.md`](04-content/content_catalog.md) · [`agenda_catalog.md`](04-content/agenda_catalog.md) — 콘텐츠·정치
5. [`data_schema.md`](05-data/data_schema.md) — 데이터 모델
6. [`decision_log.md`](decision_log.md) — 결정 이력(DL-E001~)

> 참조 규약: 본문 백틱 `spec.md` 등은 파일명 식별용(클릭 링크 아님). `§N`=design.md, "지표 §N"=indicators.md (DL-E060).
