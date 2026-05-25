# 포로 서버 작업 현황

> 마지막 갱신: 2026-05-25

---

## 현재 브랜치 상태

- 브랜치: `master` = `codex-review` (동기화 완료)
- 최근 커밋: `270c618` — §6-1~3 구현 완료
- 미커밋 변경사항: §6-4~7 구현 4개 파일 (커밋 대기)

---

## 현재 작업 — EmpireRPG 플러그인 §6 구현 순서

기준 문서: `docs/10_development_roadmap/implementation_design_plan.md`

| 순서 | 작업 | 파일 | 상태 |
|---|---|---|---|
| 1 | PotentialService cube key 정렬 → `useCube()` 단일 통합 | `PotentialService.java` | ✅ 완료 |
| 2 | SkillInputListener 4종 매핑 수정 (LMB/RMB/Shift+RMB/F) | `SkillInputListener.java` | ✅ 완료 |
| 3 | MobTagHelper 신규 + FieldDropListener scoreboard tag 교체 | `MobTagHelper.java`, `FieldDropListener.java` | ✅ 완료 |
| 4 | GuiTitles 상수 클래스 작성 | `GuiTitles.java` | ✅ 완료 |
| 5 | PlayerPersistenceService migration (v1→v2→v3) | `PlayerPersistenceService.java` | ✅ 완료 |
| 6 | WeaponSelectionGuiListener GuiTitles 상수 교체 | `WeaponSelectionGuiListener.java` | ✅ 완료 |
| 7 | PlayerQuitEvent save 추가 | `PlayerJoinListener.java` | ✅ 완료 |
| 8 | ClassInitService 신규 (초기 5슬롯 장비 지급 + 영지 도구 4종) | `ClassInitService.java` | ✅ 완료 |
| 8b | poro_island.schem 생성 (30×30 플랫폼) + schematics.yml poro 엔트리 추가 | `poro_island.schem`, `schematics.yml` | ✅ 완료 |
| 9 | 메인 허브 / 장비 허브 최소 구현 | `MainHubListener.java`, `GrowthGuiListener.java` | ✅ 완료 |
| 10 | BossRewardService + BossEngineRuntime 이벤트 연동 | `BossRewardService.java` | ⬜ 미완료 |
| 11 | SafeZoneService / WorldGuard adapter → SkillInputListener 연결 | `SafeZoneService.java` | ⬜ 미완료 |

---

## §6-1~3 완료 내용 (미커밋)

### PotentialService.java
- `MATERIAL_MEMORY_CUBE` / `MATERIAL_UPGRADE_CUBE` 제거
- `reroll()` / `upgrade()` 제거
- `useCube(state, itemId)` / `useCube(state, itemId, fixedRoll)` 추가
- CANON: 큐브 1회 = 전 라인 재롤 + 등업 시도 + 500G 차감

### SkillInputListener.java
- `onAttack(EntityDamageByEntityEvent)` 추가 → LMB → slot1 기본기
- `onInteract` 수정: sneaking 분기 → slot2(RMB) / slot3(Shift+RMB)
- `setUseInteractedBlock(DENY)` 추가
- `slot1Key()` / `slot2Key()` / `slot3Key()` / `slot4Key()` 4종 헬퍼 완성

### MobTagHelper.java (신규)
- `fieldIndex(entity)` — `empire_field_N` 태그 파싱
- `isElite(entity)` — `empire_rank_elite` 태그
- `isFieldBoss(entity)` — `empire_type_field_boss` 태그

### FieldDropListener.java
- name/customName 기반 `fieldIndex()` / `isElite()` / `marker()` 제거
- `MobTagHelper` 위임으로 교체
- `isFieldBoss()` → `profileFor()` early return (BossRewardService 위임)
- `mat_cube_fragment` / `mat_cube`: `islandTerritoryStateStore` → `growth.addCurrency()` (wallet)

### GrowthEngineSampleTest.java (테스트)
- `memory_cube` / `upgrade_cube` → `mat_cube`
- `reroll()` + `upgrade()` → `useCube()` 단일 호출

---

## 빌드 상태

```
./gradlew compileJava → BUILD SUCCESSFUL
```

---

## 남은 주요 위험/미확정

| 항목 | 내용 |
|---|---|
| compileTestJava | GrowthEngineSampleTest의 `EquipmentSlot.ARMOR_HEAD` / `ACCESSORY_1` legacy 참조 존재 (§6 범위 외) |
| §6-4 GuiTitles | listener들이 아직 raw 문자열로 title 비교 중 |
| §6-5 migration | schemaVersion v1→v2 migration 코드 미구현 |
| §6-8 ClassInitService | 첫 접속 장비 지급 로직 미구현 |

---

## 다음 커밋 제안

```
orc handoff-main "§6-1~3 구현 — PotentialService useCube 통합, SkillInput 4종 매핑, MobTagHelper scoreboard tag"
```
