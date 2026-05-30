# 아이디어 인박스 (Idea Inbox)

> **[STATUS: DRAFT]** — 대화에서 나온 미확정 아이디어·방향·제안을 임시 보관하는 문서.  
> 확정되면 관련 CANON.md + decision_log.md로 이동한다. 이 문서 자체는 공식 기준이 아니다.

---

## 운용 규칙

- 아이디어가 **확정**되면: 관련 CANON.md 반영 + decision_log.md에 DL-NNN 기록 + 아래 항목에 `[PROMOTED → DL-NNN]` 표시
- 아이디어가 **폐기**되면: 항목에 `[폐기 — 이유]` 표시
- 판단을 못 내리겠으면: 채팅에만 남기지 말고 일단 여기에 DRAFT로 기록

## 항목 형식

```
### INBOX-NNN 제목
- 날짜: YYYY-MM-DD
- 출처: 대화/작업 설명
- 내용: 아이디어 또는 방향 요약
- 분류: [ ] CANON 반영 후보 / [ ] 기획 확정 필요 / [ ] 실험적 / [ ] 폐기 후보
- 관련 문서:
- 상태: DRAFT
```

---

## 항목 목록

### INBOX-002 웹 — 보스별 클리어 통계 모니터링
- 날짜: 2026-05-24
- 출처: 보스 HP/DEF 설계 검토 중
- 내용: 웹에서 보스별 클리어 파티 스펙(평균 강화 수치, 잠재 등급)과 클리어 시간을 조회할 수 있어야 한다. 이 데이터를 기반으로 운영 중 HP·DEF 버프/너프 판단.
  - 보스별: 클리어율, 평균 클리어 시간, 파티 평균 강화 수치, 방무 보유율
  - 타임아웃 실패율, 패턴 실패(무적 해제 실패) 비율
  - 시즌 경과에 따른 주간 클리어율 추이
- 분류: [x] CANON 반영 후보 (DB/API 설계 반영 필요)
- 관련 문서: `docs/02_database_api_stats/CANON.md`, `docs/10_development_roadmap/index.md`
- 상태: **[PROMOTED → DL-052]** (2026-05-24 DB 스키마·API 스펙 확정)

### INBOX-001 강화 흔적 3종 공방 레시피
- 날짜: 2026-05-22
- 출처: M-6 확정 작업 (DL-014)
- 내용: 별의 흔적 / 달의 흔적 / 태양의 흔적의 영지 공방 가공기 제작 레시피 (재료·수량·제작 시간) 사용자 정의 필요. 수급 경로(공방 제작)와 역할(강화 성공률 보정)은 확정됨.
- 분류: [x] CANON 반영 후보 (레시피 확정 시 `05_island_farm_system/workshop_crafting_spec.md`에 기록)
- 관련 문서: `docs/05_island_farm_system/CANON.md`, `docs/decision_log.md` DL-014, `docs/02_database_api_stats/economy_numbers_v2.md` 탭 1
- 상태: **[PROMOTED → DL-024]** (2026-05-22 레시피·효과 수치 확정)

### INBOX-003 PvP 대전 시스템 — 자유/정규/친선 3종
- 날짜: 2026-05-28
- 출처: 사용자 아이디어 제안
- 내용:

  **진입 조건 (공통)**
  - 모든 대전은 영지(island)에서만 최초 진입 가능.

  **자유대전**
  - 현재 착용 장비(IL·잠재·각인·스탯 모두 그대로) 그대로 진행.
  - 점수·랭킹 없음. 보상 미정.

  **정규대전**
  - 점수 기반 랭킹. 초기 100점, 승 +15, 패 -10.
  - 장비 스탯 동일화: IL 평균 12강, 등급/세부스탯/잠재능력 동일.
  - 무기 종류·각인·스탯 포인트 배분은 현재 착용한 캐릭터 그대로 유지.
  - 랭킹 조회 가능.

  **친선대전**
  - 상대 이름 입력 → 해당 플레이어에게 요청 알림(`[수락][거절]`).
  - 수락은 상대가 영지에 있을 때만 가능.

  **매칭 및 진행**
  - 자유·정규대전: 버튼 클릭 → 즉시 대기열 진입 → 매칭 성사 시 수락 없이 아레나로 텔레포트.
  - 친선대전: 수락 시 텔레포트.
  - 종료 조건: 한 명만 생존 또는 타임아웃.

- 분류: [x] CANON 반영 후보 / [ ] 기획 확정 필요 / [ ] 실험적 / [ ] 폐기 후보
- 관련 문서: `docs/13_pvp_system/CANON.md`, `docs/decision_log.md` DL-069

---

### INBOX-004 데이터 수집 보강 — 운영 판단용 공백 7종
- 날짜: 2026-05-30
- 출처: 사용자 요청 "웹쪽 검토 — 지금 충분히 판단 가능한 데이터를 수집하는지" 검토 결과
- 내용:

  현재 **판단 가능**(DB 영속·실측): 보스 클리어 통계(`boss_stats_summary`), PvP 랭킹·매치 로그, 경매 거래/시세(`auction_listings`).

  **판단 불가 공백** (우선순위순). 45일 시즌 서버 기준 #1·#2가 가장 치명적:

  | # | 영역 | 현재 상태 | 보강 방향(미확정) |
  |---|---|---|---|
  | 1 | 리텐션·활동(DAU·플레이타임·이탈) | **✅ 구현 완료 (2026-05-30) [PROMOTED → DL-078]** — `player_session_log` + `/api/v1/activity/*` | ~~`player_session_log`(join/quit 시각) 신규 + DAU/플레이타임 집계~~ |
  | 2 | 골드 인플레이션/싱크 | **✅ 구현 완료 (2026-05-30) [PROMOTED → DL-080]** — `economy_flow`(지갑 후킹+로드 우회) + `/api/v1/economy/flow`. net 정확, source 미세분은 후속 | ~~inflow/outflow를 재화 변동 지점에서 기록 + DB 영속~~ |
  | 3 | 강화 성공률·소모량 | **✅ 구현 완료 (2026-05-30) [PROMOTED → DL-079]** — `enhancement_log` + Composite hook + `/api/v1/economy/enhancement` | ~~`DbEnhancementLogHook` 추가~~ |
  | 4 | 보스 파티 스펙 밸런스 | **✅ 구현 완료 (2026-05-30) [PROMOTED → DL-081]** — 입장 시 실측 IL/강화(`recordPlayerEntry`+`recordPartySpec`). defense_ignore는 1차 시즌 N/A로 0 | ~~입장 시 실측 IL/강화 기록~~ |
  | 5 | 보스 데미지 기여(damage_share) | **✅ 구현 완료 (2026-05-30) [PROMOTED → DL-084]** — mob UUID 추적+`BossDamageTracker`+`boss_session_player.damage_share`. damage_total은 미저장 | ~~참여자 데미지 집계 컬럼 추가~~ |
  | 6 | PvP 클래스 밸런스 | **✅ 구현 완료 (2026-05-30) [PROMOTED → DL-082]** — `pvp_match_log` 무기/IL 컬럼 + `/api/v1/pvp/balance`(무기별 승률). 데미지량은 미수집(별도) | ~~양측 무기/IL 컬럼 추가~~ |
  | 7 | 성장 시계열 곡선 | **✅ 구현 완료 (2026-05-30) [PROMOTED → DL-083]** — `growth_snapshot`(30분 일별 upsert) + `/api/v1/growth/curve` | ~~일/주 단위 성장 스냅샷 누적 테이블~~ |

  **죽은 데이터 모델**(모델 정의만 있고 write 호출 0건): `EconomyFlowRecord`, `MarketPricePoint`, `LifeResourceSupplyRecord`. (단 `EstateHarvestLogHook`은 기록되나 in-memory 휘발)

  ※ 시세(market price)는 별도 수집 없이도 `auction_listings`의 sold price/time으로 재구성 가능(`getAveragePrice` 이미 존재).

- 분류: [ ] CANON 반영 후보 / [x] 기획 확정 필요 / [ ] 실험적 / [ ] 폐기 후보
- 관련 문서: `docs/02_database_api_stats/CANON.md`, `boss_clear_stats_spec.md`, task.md 알려진 기술 부채
- 상태: ✅ 전체 완료 (2026-05-30) — 7종 모두 구현 [PROMOTED → DL-078~084]. 죽은 모델 `MarketPricePoint`·`LifeResourceSupplyRecord`는 미사용 유지(필요 시 별도 검토)

  **오픈 질문 (답변 완료)**
  1. 1차 시즌 포함 여부? (구현 규모 상 2차 시즌 이후 적합할 수 있음)
  2. 아레나 맵 — 별도 월드/방인가, 보스룸 풀과 동일 방식인가?
  3. 타임아웃 기준 시간 (예: 3분, 5분)?
  4. 사망 처리 — 즉시 영지 귀환? 스펙테이터 전환 후 귀환?
  5. 정규대전 점수 초기화 주기 — 시즌 단위(45일)?
  6. 자유대전 보상 — 없음? 소량 골드?
  7. 정규대전 동일화 기준: IL 12강 = 5슬롯 모두 12강 (IL 60)인가, 평균 IL 60인가?
  8. 대기열 취소 가능 여부, 대기 중 서버 이탈 처리?

- 상태: **[PROMOTED → DL-069]** (2026-05-28 핵심 설계 확정 → `docs/13_pvp_system/CANON.md` 생성)

### INBOX-005 코드 ↔ 기획 정합성 감사 결과 — 시드/구현 불일치
- 날짜: 2026-05-30
- 출처: 사용자 요청 "코드 전수 조사하고 기획서랑 맞는지" 감사 결과 (에이전트 grep 기반, 수정 전 실제 시드 재확인 권장)
- 내용 (운영·밸런스 영향순):

  | 우선도 | 불일치 | 근거 |
  |---|---|---|
  | ~~🔴 높음 강화 테이블 수치 불일치~~ ✅ 해소 (2026-05-30, DL-086) | T1 1~25강 확정 표 반영 + 21~25강 추가 + 방어구 강화석 `ceil(÷1.5)` 구현 + validate 1~3강 완화. ※배포 사본 동기화 필요 |
  | ~~🔴 높음 보스 시드 두 벌 모순~~ ✅ 해소 (2026-05-30, DL-087) | boss_master를 정본 시즌6+최종3으로 교체, 필드4 `fallen_knight`→`outpost_knight` 분리. **잔여: MM 셸 충돌**(`field_outpost.yml` Fallen_Knight vs `season_bosses.yml` fallen_knight, server-config 별도 수정 필요) |
  | ~~🟠 영지 생산 온라인 전용~~ ✅ 해소 (2026-05-30, DL-088) | 오프라인 누적 생산(storage_hours_cap 상한) 구현. lastProductionAt 영속화 |
  | ~~🟠 광물 채굴기 시드 누락~~ ✅ 해소 (2026-05-30, DL-088) | estate_facility_master에 extractor 행 추가(life_type=mining) |
  | ~~🟠 중간 강화 흔적 미연동~~ ✅ 해소 (2026-05-30, DL-089) | 강화 GUI 흔적 선택 슬롯 + `EnhancementService` 성공률 %p 보정(+10강 이상, 천장 시 미소모) + `enhancement_log.trace_id` 기록 |
  | 🟠 중간 | 필드보스 스폰 스케줄러 stub | `FieldBossRespawnScheduler` 항상 RESPAWNING/30분/0명 |
  | 🟡 낮음 | 금지 설계 시드 잔존 | `state_master.csv` DEBUFF_MARK, 방깎/받피증 각인, 공용각인 12종, T2 강화표 — 런타임 미적용이나 정리 대상 |
  | 🟡 낮음 | 장비 이름 변경권(10,000G) 미구현 | 기획 §7 |

  ※ EXP는 감사 결과 **기획 일치**(필드 몹 사냥 커스텀 XP). 바닐라 XP 바 병존 문제만 DL-085로 해소.

- 분류: [ ] CANON 반영 후보 / [x] 기획 확정 필요 (수정 방향 결정) / [ ] 실험적 / [ ] 폐기 후보
- 관련 문서: `final_master_plan.md`, 각 CANON.md, decision_log DL-033/DL-076
- 상태: DRAFT (🔴 2건 실제 시드 재확인 후 수정 착수 권장)

#### 2026-05-31 서버 테스트 진입 전 2차 감사 (코드↔기획, 5도메인 병렬) — 신규 발견

> 1차 감사(위) 이후 잔여/신규. 데이터·시드·경제·DB·마이그레이션·강화/흔적/EXP 곡선은 **정합 양호**. 전투 데미지 배선·보스 런타임 배선·서버 설정 선행조건에 블로커 집중. 사용자 결정(2026-05-31): 전투 파이프라인 #6·#7은 **미완 구현 → 수정**, 코드 블로커부터 착수.

| 우선도 | 발견 | 근거(파일:라인) | 상태 |
|---|---|---|---|
| 🔴 차단 | config.yml 좌표·월드명 플레이스홀더(필드/보스룸/필드보스 = `world`, 더미좌표). 실맵+`world_main/boss`+방생성 선행 필요 | `config.yml:125-240`, `FieldTeleportService:39` | 선행조건(코드 밖) |
| 🔴 차단 | MythicMobs 셸 의존 — 미감지 시 전투/보스/필드 리스너 통째 미등록 | `EmpireRPGPlugin:656`, `BossRoomListener:100` | 선행조건(server-config) |
| 🔴 차단 | 보스 처치→`endRun`(보상) 브리지 부재 — death 이벤트→보상 발화 리스너 없음. 보상 0·슬롯 미회수 | `BossRunService:173`, `BossRoomListener` | 코드 수정 대상 |
| 🔴 차단 | 원샷 방지 85% 클램프 미구현 — `BossDefenseListener` 빈 스텁 (combat+content 교차확인) | `BossDefenseListener:8-10` | 코드 수정 대상 |
| 🔴 차단 | 최종보스 입장 게이트 무력화 — `AllowAllUnlockQuestChecker` 무조건 true | `BossEngineBootstrap:64` | 코드 수정 대상 |
| 🟠 결정→수정 | **무기 ATK 환산 선형(+10%/강)** ↔ CANON flat 테이블(20강 240 vs 157) | `WeaponPowerCalculator:37` | 미완(수정 확정) |
| 🟠 결정→수정 | **피해 공식 스킬%/태그%/조건부/치명/DEF경감 계층 전부 미적용** — 잠재·특화·만찬 무효 | `BaseWeaponSkill:39-47` | 미완(수정 확정) |
| 🟠 | 직업각인 고유효과 미구현(스택 상한 토글만) | `BaseWeaponSkill:55-60` | 수정 대상 |
| 🟠 | 잠재(큐브) 등급 확률 모델 코드·문서 3원 분기(코드 승급3% vs 경매 1%=100회 전제) | `PotentialService:100-106` | 기획 확정 필요 |
| 🟠 | 보스 타임아웃·페이즈 틱 루프 미등록 / 시즌보스 인원배율 미적용 / 균열왕 심장·최종 트로피 지급 불일치 | `BossRunService`, `BossRewardService:155` | 수정/확정 |
| 🟡 | SQLite busy_timeout/WAL 미설정 → 동시쓰기 버스트 시 통계 유실 | `SqliteConnectionProvider` | 정리 |
| 🟡 | `/캐릭터`·`/작물` 라우팅 미연결, 온보딩에 영지 생성 트리거 부재, `season-start-epoch:0` | `PlayerCommandRouter:95-99`, `ClassInitService:72` | 정리 |
| 🟡 | 잠재 옵션 풀 슬롯 차등 미구현(weapon/armor 2풀), `island_settings` PK 1줄 확인 권장 | `PotentialService:202`, `IslandSettingsDdl` | 확인/정리 |

- 분류: [x] 기획 확정 필요(잠재 모델) + [x] 코드 수정 착수(전투 파이프라인·보스 배선) / 선행조건 2건은 맵·server-config 작업
- 상태: 코드 블로커 수정 진행 중 (DL-091)
  - ✅ 완료(DL-091): #6 ATK flat 테이블, #3 보스 처치→보상 브리지, #5 최종보스 게이트, #4 원샷 85% 클램프
  - ✅ 완료(DL-092): #7 피해 공식 계층 — attack_percent(ATK)·general_damage_increase(스킬피해%)·치명 적용 + WeaponPowerCalculator flat합산 버그 교정. ※boss_damage_increase(보스판정)·DEF항(바닐라 armor 위임)은 후속/위임
  - ✅ 완료(DL-093): #10 보스 전투 타임아웃(15/10분) 스케줄러 — 경과 런 강제종료+슬롯회수+보스 디스폰+알림. ※페이즈/패턴 진행 틱·참가자 자동 텔레포트는 후속
  - ⏳ 남음(코드): boss_damage_increase 보스판정 배선, 보스 클리어 기록 영속화(in-memory 소실), 페이즈/패턴 진행 틱
  - ✅ 완료(DL-094): 잠재(큐브) 등급 모델 = 메모리얼 승급 확정 + 확률 강화(25/12/6/1.5%, 레전 ~96큐브). drop_tables §6 플랫표 폐기
  - ✅ 완료(DL-095): 치명 %/pt 충돌 해소 — **0.30%/pt 정본 확정**(level_stat_system §3). #7 코드 이미 정합(변경 없음), potential_options_v1 정정. ※치명 피해량 0.2%/pt 미적용은 #7 후속
  - ✅ 완료(DL-096): boss_damage_increase 보스판정 적용(필드태그+인스턴스추적) + 치명 피해량 0.15%/pt 스탯 적용. #7 피해 공식 계층 완성
  - ✅ 완료(DL-097): 보스 클리어 게이트 영속화(boss_session lazy 복원). 페이즈/패턴 틱은 MM 구동이라 불요 확정(스킵)
  - ✅ 완료(DL-098): 배포(jar+seed+MM) + 실부팅 검증. 부팅 중 드러난 검증 과엄격 4건(skill_master class_id, NPC sync fatal, life 바닐라 재료, facility workshop) 해소. **8개 bootstrap 전부 통과·disable 0**
  - ✅ 완료(DL-099): 단일 평지 월드 `world` 생성 + config 배포(보스룸30/아레나10 슬롯) + genrooms/genarenas 실행. NPC 3명 스폰, 구조물 생성 검증.
  - → **🎉 서버 테스트 진입 준비 완료.** 남은 건 첫날 스모크 테스트 10단계(server_test_prep.md §5) + 선택사항(필드 장식·season-start-epoch)
  - ⏳ 선행조건(코드 밖) → `docs/10_development_roadmap/server_test_prep.md` 체크리스트로 정리(2026-05-31):
    - 🔴 **런타임 seed/jar stale** — `server/plugins/EmpireRPG/seeds/boss_master.csv`가 DL-087 이전(void_herald·최종보스 없음). `saveResource(replace=false)`라 jar 교체로도 갱신 안 됨 → 빌드+seeds폴더 비우고 재시작 필수. DL-086~090·이번 코드수정 전부 미배포 상태.
    - 🔴 config 좌표·월드명(world_main/farm/boss/test), `/empire genrooms`·`genarenas`
    - 🟠 MM 셸 배포(현재 Example만), 보스 armor 확인
    - [x] MM ID 충돌 해소: `field_outpost.yml` Fallen_Knight→Outpost_Knight (2026-05-31)

### INBOX-006 동적 필드 스폰 시스템 (플레이어 주변 웨이브 + 일반/정예 토글)
- 날짜: 2026-05-31
- 출처: 사용자 설계 (맵 구성 논의 중 확정)
- 상태: **코어 구현 완료(DL-100, 2026-05-31), 2차 대기** — 일반/정예 토글·랜덤 진입·디스폰 정밀화 남음. 필드 간격 1000 적용·검증됨
- 내용:
  - **필드 크기/배치**: 각 300×300, 단일 평지 월드 `world`(DL-099). 5필드 좌표 간격 ≥500(현 500=200갭 OK).
  - **필드 크기 = 필드 하나당 300×300** (5개 각각, config 좌표 ±150, 500간격→200갭. 전체 가로 ~2300).
  - **동적 스폰**: 고정 스폰 대신 **플레이어 주변 웨이브**. ~15초 주기로 필드 내 각 플레이어 반경 20~30블록 지면에 10~15마리 스폰(현 ~45/분 흐름에 맞춤). 멀어진/오래된 몹 디스폰. MM mob을 mythicSpawner로 스폰(필드 tier별).
  - **2단 캡**: ①플레이어당 standing 상한(시작 ~40) + ②**필드 전체 상한**(시작 ~250, 엔티티 부하). 필드캡 도달 시 신규 스폰 보류. 스폰몹에 **소유자 기록**(태그/맵) → 내 캡·디스폰 기준.
  - **일반/정예 토글**: 플레이어별 모드. **명령어(`/필드 일반|정예`) + 필드 GUI** 둘 다. 정예 = 수 적게·강하게·드랍↑(예: 1/3 수·×2.5 강도). 필드 index(1~5)는 기본 몹 강도.
  - **경계**: WorldGuard 리전만(PvP-off·필드 영역 마커). 벽·중앙복귀 없음 — 이탈 시 필드 밖(스폰 없음).
  - **진입 스폰**: 필드 내 **랜덤 지점**(고정 X), 단 **타 플레이어 ≥35블록 빈 곳** 선택(혼잡 시 가장 한산) → 자리 경쟁·남의 사냥 난입 방지.
  - **PvP/친선사격**: 이미 구현됨(`PvpDamageListener` — 매치+아레나 외 플레이어 피해 전면 차단, 필드·보스 포함). 추가 작업 없음.
- 구현 범위(예상): 필드 리전 정의, 동적 스폰 스케줄러(주변/배치/캡/디스폰), 일반-정예 모드 상태+명령어+GUI, FieldTeleportService 랜덤 진입, 기존 MM 자연 스폰과 조정.
- **온보딩 추가 설계 (사장님 확정)**: 접속→허브 월드(world_hub, 별도 평지) / 첫접속→튜토리얼 맵(안내 스텝)→무기선택→스타터→영지(IS 섬 자동생성+이동). 복귀 유저는 허브 스폰.
  - ✅ 허브 월드 + 접속 스폰 이동 코어 (DL-101)
  - ✅ 첫접속 라우팅 + IS 섬 자동생성+이동 (DL-102, 무기선택→is create poro). 실흐름 인게임 검증 대기
  - ⏳ 튜토리얼 맵(안내 스텝) — 첫접속을 허브 대신 튜토리얼로, TutorialService 구현
- 파라미터(시작값, 튜닝): 주기 15~20s / 배치 10~15 / 캡 ~40 / 반경 20~30 / 정예 1:3 수·×2.5.
- 관련 문서: `docs/06_fields_bosses/`, DL-099(단일 월드), `FieldTeleportService`, `FieldDropListener`(필드 드랍), `MobTagHelper`(empire_field_N 태그).

### INBOX-007 라이브 온보딩 테스트 1차 피드백 (7건)
- 날짜: 2026-05-31
- 출처: 사용자 인게임 테스트 (첫접속→허브→무기선택 흐름 검증)
- 상태: **7건 처리 완료** (6건 수정·빌드·배포 + ④ 가상 전용 확정 → DL-103). ③ 인게임 재검증·⑦ rename 반영은 후속
- 처리 내역:
  - ① 무기 선택 GUI 3+3 배치 — ✅ 슬롯 11/13/15(검·도끼·창)·20/22/24(석궁·낫·스태프). 리스너 매핑 동기.
  - ② 창 아이콘 네더라이트검→**삼지창(TRIDENT)** — ✅ `materialFor` 분리.
  - ③ `is create` 시 "Select a Schematic" GUI 우회 — ✅ `schematics.yml`를 `poro` 1개만 남김(IS는 스키매틱 1개면 picker 생략). **인게임 재검증 필요**.
  - ④ 스타터 방어구 — ✅ **가상 전용 확정 [PROMOTED → DL-103]**. 런타임 item_master에 `t1_*_starter` 4종 정상 등록, 캐릭터 메뉴 스탯 적용. 바닐라 물리 방어구 미지급(§2 DEF 이중 적용 방지), 재질/외형 컬럼 원래 없음 → 코드 변경 없음. **분기 리스크**: `server-config/.../item_master.csv`가 다른 스키마(`t1_armor_head`)로 갈라짐 — 정리 후속.
  - ⑤ 스타터 무기 이름/lore 없음 — ✅ `taggedWeapon`에 "수련용 {무기}"명 + lore 3줄. `Component.text("§..")`가 §를 해석 안 해 이름이 깨지던 것 → **LegacyComponentSerializer**로 변환(+이탤릭 off). GUI 아이콘도 동일 적용.
  - ⑥ 낫 우클릭(월영회전) 항상 우측 이동 — ✅ `dashInInputDirection`(getVelocity 서버측 ~0 버그)→**`dashForward`**(시선 방향).
  - ⑦ 스코어보드 "world_hub" 표기 — ✅ world_hub→"수도", IridiumSkyblock→"[플레이어]의 영지". **영지명 변경 반영은 후속**(territory store 연동 필요).
- 후속(미구현): ④ 설계 확정 / ⑦ 영지명 rename 반영 / HUD·리소스팩(HP·XP바 위치, 스코어보드 글자 깨짐·PNG 크기 — Phase 8) / 튜토리얼 맵(INBOX-006 ⏳).

#### INBOX-007 2차 (라이브 재검증 피드백)
- ⑥ 낫 우클릭 시선 방향 — ✅ 정상 확인 / ① 무기 GUI 3+3 — ✅ 확인.
- **무기별 독립 성장** — ✅ **확정 [PROMOTED → DL-104]**. 기존 "공유" 구조를 무기별(`weapon_<타입>`) 독립으로 전환. 강화/큐브가 현재 장착 무기에만 적용.
- **무기 선택/변경 GUI 통합** — ✅ **확정 [PROMOTED → DL-104]**. 스펙 36칸 공용 레이아웃(`WeaponGui`) 재사용.
- 손에 든 무기 lore — ✅ 무기변경 형식(강화/등급/잠재/세부스탯/각인)으로 통일(`WeaponItemFactory`).
- 창→검 — ✅ `weaponCosmeticMaterial` SPEAR→TRIDENT 정정.
- 스코어보드 위치 미갱신 — ✅ 위치 변경 감시 태스크(1초 폴링, 변경 시에만 refresh) + 단일 `world` 좌표 기반 구역명(필드5/보스룸/PvP). **후속**: config 좌표와 `resolveWorldArea` 상수 결합 / 영지명 rename 반영.
- 후속(DL-104): 손에 든 무기 lore 강화 직후 실시간 동기화 / 무기 변경 3분 쿨타임 / 무기별 각인.

<!-- 새 항목은 이 주석 위에 추가한다 -->
