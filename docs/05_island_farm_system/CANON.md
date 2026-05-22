# 05. 영지 / 농사 시스템 — CANON

> **[STATUS: CANON]** — 이 폴더 도메인의 단일 정답 소스.  
> 다른 문서와 충돌 시 이 문서(및 final_master_plan.md §8)가 우선.

---

## 역할

개인 영지 구조, 영지 저장고 GUI, 시설 3종(약초 재배지/광물 채굴기/공방 가공기), 재배·채굴·가공 로직의 확정 소스.

---

## Canonical Source

| 섹션 | 위치 | 내용 |
|------|------|------|
| 영지 전체 구조 | `final_master_plan.md §8` | IridiumSkyblock 연동, 시설 3종 |
| 영지 저장고 | `final_master_plan.md §8.1` | GUI 슬롯, 아이템 분류 |
| 약초 재배지 | `final_master_plan.md §8.2` | 씨앗 종류, 성장 시간, 수확 |
| 광물 채굴기 | `final_master_plan.md §8.3` | 광석 종류, 채굴 주기 |
| 공방 가공기 | `final_master_plan.md §8.4` | 가공 레시피, 처리 시간 |

---

## 참조 문서

| 문서 | 상태 | 역할 |
|------|------|------|
| `index.md` | DRAFT | 영지 시스템 개요 |
| `island_system_design.md` | DRAFT | 영지 상세 설계 |
| `workshop_crafting_spec.md` | DRAFT | 공방 가공 레시피 상세 |

---

## ✅ 확정 사항

- **마력 시스템 완전 제거** (2026-05-19 확정) — 참조 문서에 마력 관련 내용이 남아 있으면 무시
- IridiumSkyblock = 영지 생성/보호 껍데기만 담당
- EmpireRPG = 저장고/시설/가공 로직 전체 소유
- 호퍼/케이블 물류 없음 (GUI 기반 수동 조작)

---

## TODO

- [ ] `index.md`에 마력 관련 내용이 있으면 제거 또는 표기
- [ ] `island_system_design.md` 내용 검증 — 마력 시스템 참조 없는지 확인
- [ ] `workshop_crafting_spec.md` 레시피를 CANON으로 통합
- [ ] 시설 3종 수치 (성장시간, 채굴주기, 가공시간) CANON에 직접 기재
- [ ] GUI 슬롯 매핑 (`gui_storage.md` 참조) 연결 정리
