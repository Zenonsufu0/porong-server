# 05. 개인 영지 시스템

> **[STATUS: DRAFT]** — `CANON.md` 참조. 마력 시스템은 완전 제거됨 (2026-05-19 확정).

> 확정 기준: `final_master_plan.md`의 "개인 영지" (2026-05-21)

## 핵심 구조

```
IridiumSkyblock  → 영지 생성 / 보호 / 방문 (껍데기)
ZenonRPG        → 저장고 / 시설 스케줄 / 공방 / 작위 승급
```

유저에게 "스카이블럭" 표현 노출 금지 → **개인 영지**로 표기.

**폐지 확정 (2026-05-19):** 마력 시스템, 발전기, 자동채굴기(광맥 생성기/광물 추출기).

## 확정 시설 3종

| 시설 | 역할 | 생산 주기 | 최대 레벨 |
|---|---|---|---|
| 약초 재배지 (`estate_herb_plot`) | 제국 약초 생산 | 20분 | 3 |
| 광물 채굴기 (`estate_ore_extractor`) | 마도철 원석 생산 | 20분 | 3 |
| 공방 가공기 (`estate_workshop`) | 레시피 제작 (대기열) | 즉시(대기열) | 1 |

시설은 **GUI 슬롯 배정 방식** — 물리 블럭 설치 없음.

## 바닐라 농사/채광

기본: 인벤토리 획득. 편의 해금 후: 영지 저장고 자동 입금. 자동 심기: 작위 취득 후 골드/재료로 권한 구매.

## 기본 광물 생성기

울타리 + 빈칸 + 물 직접 설치 → 작위별 확률표로 광물 1개 생성 (BlockFormEvent 가로채기).

---

## 상세 설계 문서

| 문서 | 내용 |
|---|---|
| [`island_system_design.md`](island_system_design.md) | 작위 테이블, 시설 산출량, DB 스키마, 생산 스케줄러, 재화 소모 규칙 |
| [`workshop_crafting_spec.md`](workshop_crafting_spec.md) | 공방 탭별 레시피 전체, 전승 아이템 체계 |

## GUI 설계 (docs/08 이관)

| 화면 | 설계 문서 |
|---|---|
| 영지 서브 허브 | `zenon-rpg/docs/08_resourcepack_pipeline/gui_hub_structure.md §4` |
| 공방 GUI | 현행 기준 재작성 필요 |
| 영지상태 GUI | 현행 기준 재작성 필요 |
| 영지 저장고 GUI | `zenon-rpg/docs/08_resourcepack_pipeline/gui_storage.md` |
| 영지설정 GUI | `zenon-rpg/docs/08_resourcepack_pipeline/gui_territory_settings.md` |
| 작물관리 GUI | `zenon-rpg/docs/08_resourcepack_pipeline/gui_crop_management.md` |
