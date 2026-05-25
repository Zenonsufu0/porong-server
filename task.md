# 포로 서버 작업 현황

> 마지막 갱신: 2026-05-25 (Phase 6 잔여 완료)

---

## 현재 브랜치 상태

- 브랜치: `master` = `codex-review` (동기화 완료)
- 최근 작업: §6 플러그인 코어 구현 목록 1~11 전체 완료

---

## §6 EmpireRPG 플러그인 코어 구현 — 완료

기준 문서: `docs/10_development_roadmap/implementation_design_plan.md`

| 순서 | 작업 | 상태 |
|---|---|---|
| 1 | PotentialService `useCube()` 단일 통합 | ✅ |
| 2 | SkillInputListener 4종 매핑 (LMB/RMB/Shift+RMB/F) + SafeZone 연결 | ✅ |
| 3 | MobTagHelper 신규 + FieldDropListener scoreboard tag 교체 | ✅ |
| 4 | GuiTitles 상수 클래스 | ✅ |
| 5 | PlayerPersistenceService migration v1→v2→v3 | ✅ |
| 6 | WeaponSelectionGuiListener GuiTitles 교체 | ✅ |
| 7 | PlayerQuitEvent save | ✅ |
| 8 | ClassInitService 신규 (장비 지급 + 영지 도구) | ✅ |
| 8b | poro_island.schem + schematics.yml | ✅ |
| 9 | 메인 허브 / 장비 허브 최소 구현 | ✅ |
| 10 | BossRewardService + BossEngineRuntime 연동, ContributionTracker | ✅ |
| 11 | SafeZoneService (WorldGuardSafeZoneService / Noop) | ✅ |

---

## 빌드 상태

```
./gradlew compileJava → BUILD SUCCESSFUL
```

---

## Phase 6 잔여 — 완료

| 항목 | 상태 |
|---|---|
| FieldMobTagListener 등록 + compileJava | ✅ |
| `boss_patterns.yml` (P-00~P-13) 신규 생성 | ✅ |
| 5개 필드보스 YAML Phase 구조 + BossBar 업데이트 | ✅ |
| `season_bosses.yml` 9보스 셸 신규 생성 | ✅ |

---

## 다음 작업 후보 (병렬 Phase)

| 우선도 | 항목 | 관련 문서 |
|---|---|---|
| 높음 | DB 스키마 + API 엔드포인트 (Phase 7 선행) | `docs/02_database_api_stats/CANON.md` |
| 중간 | 디스코드 인증봇 (Phase 2) | `docs/03_discord_onboarding_bot/index.md` |
| 중간 | 스킬 전체 구현 (Phase 3) | `docs/04_combat_weapon_skills/CANON.md` |
| 중간 | 영지/농장 시스템 (Phase 5) | `docs/05_island_farm_system/CANON.md` |
| 낮음 | 리소스팩 파이프라인 (Phase 8) | `docs/08_resourcepack_pipeline/index.md` |

---

## 알려진 기술 부채

| 항목 | 내용 |
|---|---|
| compileTestJava | GrowthEngineSampleTest legacy `EquipmentSlot` 참조 (§6 범위 외) |
| MythicMobs/IridiumSkyblock | `../../server/plugins/` 로컬 JAR 경로 — review worktree 미지원 (pre-existing) |
| 시즌보스 damage_share | `BossResultSummaryBuilder` placeholder 0.0 유지 — §7+ 구현 (DL-064) |
| AllowAllUnlockQuestChecker | 보스6 클리어 조건 stub — 퀘스트 시스템 구현 후 연결 |
| boss_pattern_seed.csv | 7개 보스 placeholder 패턴만 있음 — 실제 패턴 설계 필요 |
