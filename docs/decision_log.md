# 설계 결정 로그 (Decision Log)

> **[STATUS: DRAFT]** — PHASE 2~4 수정 작업 기록. 각 항목은 "무엇을 / 왜 / 근거 문서" 형식.

---

### DL-066 영지 작위 승급 재료 — 전장의 파편 + 골드만으로 확정

작위 승급 소비 재료를 **전장의 파편(`mat_battle_shard`) + 골드**만으로 확정한다.

- `island_system_design.md` 초안에 자작령 달의 흔적 3개, 백작령 태양의 흔적 2개가 포함되어 있었으나,
  달/태양 흔적은 공방 제작 아이템으로 획득 경로가 별도 시스템을 요구함.
- 1차 시즌 범위 내에서 WorkshopGui 레시피가 구현되기 전까지 승급 경로를 막지 않기 위해 제외.
- `island_system_design.md` 승급 테이블에서 흔적 재료 칸 제거 완료.
- `IslandRank.java` 모든 단계 upgradeMaterials에서 mat_trace_moon/sun 제거.
- mat_trace_star/moon/sun은 **강화 보정 아이템**(공방 제작)으로 존재, 승급과 무관.

---

### DL-065 보스 세션 진행 중 상태 — result='abandoned' 초기값 + ended_at IS NOT NULL 뷰 필터

**결정:**
- `boss_session_log` INSERT 시 `result='abandoned'`을 초기값으로 사용 (CANON 원안의 `result=NULL` 대신).
- `boss_stats_summary` VIEW에 `WHERE ended_at IS NOT NULL` 조건을 추가해 진행 중 세션을 통계에서 제외.
- 서버 크래시로 종료된 세션(`result='abandoned'`, `ended_at=NULL`)은 통계 제외 — 허용 가능한 트레이드오프.

**이유:** SQLite `ALTER TABLE`로 컬럼 제약을 변경하려면 테이블 전체 rebuild가 필요하다. `NULL` vs `'abandoned'` 초기값은 VIEW 필터를 통해 기능적으로 동일하므로 schema 변경 비용 대비 실익 없음.

**영향 범위:** `BossSessionDdl.CREATE_SESSION_LOG` (DEFAULT 'abandoned'), `BossSessionRepository.recordStart()`, `BossSessionDdl.CREATE_STATS_SUMMARY_VIEW` (WHERE ended_at IS NOT NULL).

**관련:** `docs/02_database_api_stats/boss_clear_stats_spec.md` §5 업데이트 완료.

---

### DL-064 시즌보스 참여 기준 — 보스룸 입장 확인, damage_share 집계 1차 시즌 제외

**결정:**
- 시즌보스 참여 기준: **파티 보스룸 입장 확인** (방 입장 = 참여 게이트).
- 필드보스 기준인 "총 피해량 3%"는 오픈필드 무임승차 방지용; 1~3인 제한 보스룸에는 적용 안 함.
- `BossResultSummaryBuilder`의 `damage_share`는 1차 시즌 `0.0` placeholder 유지. 실제 집계 연결은 §7+ 보스 패턴 구현 단계로 이관.
- **CANON §6 및 drop_tables_v1.md §시즌보스 보상 구조 반영 완료** (2026-05-25).

**이유:** `BossDefenseListener` stub 상태, MythicMobs 기반 시즌보스 데미지 이벤트 연결 미완. 보스룸 입장 자체가 참여 확인으로 충분.

**근거:** 사용자 확인 (2026-05-25). CANON.md + drop_tables_v1.md 동기화 완료.

---

### DL-063 튜토리얼 완료 → 영지 자동 생성 + 섬 도구 지급 흐름 확정

**결정:** 튜토리얼 완료(무기 선택) 시점에 IridiumSkyblock 섬을 자동 생성하고, 영지로 텔레포트하면서 섬 도구 세트를 함께 지급한다.

**튜토리얼 흐름:**
```
튜토리얼 방 입장
  → 서버 규칙 읽기
  → 무기 선택 GUI (27슬롯, "클래스 선택")
    → RPG 장비 5슬롯 지급 (WEAPON + 방어구 4종, ClassInitService)
    → IridiumSkyblock 섬 자동 생성 (/is create 또는 API 호출)
    → 섬 도구 세트 지급 (아래 명세)
    → 영지(IridiumSkyblock 세계)로 텔레포트
```

**섬 도구 세트 (ClassInitService.grantIslandTools):**

| 아이템 | 인챈트 | 비고 |
|---|---|---|
| 네더라이트 곡괭이 | Efficiency V + Fortune III | `setUnbreakable(true)` |
| 네더라이트 삽 | Efficiency V + Fortune III | `setUnbreakable(true)` |
| 네더라이트 괭이 | Efficiency V + Fortune III | `setUnbreakable(true)` |
| 네더라이트 도끼 | Efficiency V + Fortune III | `setUnbreakable(true)` |

**정책:**
- 모루 사용 불가 (WorldGuard 또는 이벤트로 차단 예정)
- 인챈트 시스템(상점 인챈트북) 불필요 — 도구에 풀 인챈트 박아서 지급
- Silk Touch 곡괭이 미지급 — Fortune III 1종으로 통일
- 내구도 소모 없음 (`setUnbreakable(true)`, Mending/Unbreaking 미사용)

**근거:** 45일 시즌 서버 특성상 유저가 바닐라 도구 파밍 루프에 시간을 쏟지 않고 RPG 콘텐츠(전투·성장·영지 생산)에 바로 집중할 수 있도록 한다.

---

### DL-062 공방 광물 변환 탭 — 원석 → 주괴 정정 + 금 주괴 소모처 확정

**결정:** `tab_ore_convert`의 모든 교환 재료가 **원석(Ore)이 아닌 주괴(Ingot)**임을 확정. 문서 오기 수정.

| 교환 재료 | 기존 표기 | 확정 표기 |
|---|---|---|
| 철 변환 (다이아) | 철 원석 × 32 | **철 주괴 × 32** |
| 금 변환 (다이아) | 금 원석 × 16 | **금 주괴 × 16** |
| 철 변환 (에메) | 철 원석 × 48 | **철 주괴 × 48** |
| 금 변환 (에메) | 금 원석 × 24 | **금 주괴 × 24** |

**금 주괴 소모처 해소:** 금 주괴는 공방 광물 변환 탭이 유일 소모처. 별도 NPC 매입 불필요.

**이유:** 제련 후 주괴 형태가 인벤토리에 쌓이는 구조이므로 원석 요구는 UX상 부자연스러움. 공방은 가공된 재료 투입이 일관된 설계.

**근거 문서:** `docs/05_island_farm_system/workshop_crafting_spec.md §4`, `docs/02_database_api_stats/economy_numbers_v2.md §8`

---

### DL-061 스탯 트리 치명 부효과(치피) 수치 확정

**결정:** 치명 트리 부 효과 치명타 피해량% **+0.15%/pt** 확정.

| 트리 | 주 효과 | pt당 | 부 효과 | pt당 |
|---|---|---|---|---|
| 치명 | 치명타 확률% | +0.30% | 치명타 피해량% | **+0.15%** |
| 특화 | 스킬 피해% | +0.30% | 태그 피해% | +0.15% |
| 인내 | 받는 피해 감소% | +0.15% | 방어력 | +0.4 |

레벨당 스탯 포인트 지급량: **3pt** (기존 확정 유지).

**이유:** 기존 +0.20%/pt 초안 대비 하향. 치피 스텟만으로 얻는 이득을 줄여 치확 또는 잠재에 더 의존하게 만드는 설계 방향. 레벨 50 치명 all-in 시 치명 vs 특화 DPS 격차 6.3% → 설계 목표 5~10% 내 유지.

**근거 문서:** `docs/04_combat_weapon_skills/level_stat_system_v1.md §2, §4`

---

### DL-060 daily_economy_snapshot 집계 방식 확정

**결정:** **메모리 누적 + 10분 주기 플러시** 방식 채택.

- 이벤트 발생 시 `Map<String, Long>` 메모리 카운터만 증가 (DB 접근 없음)
- Bukkit 비동기 스케줄러로 10분(12,000 ticks)마다 DB upsert
- 자정에 최종 flush → 카운터 초기화

**이유:** 이벤트마다 DB write 시 메인 틱 스레드 블로킹으로 틱 지연 직결. 자정 일괄 집계는 당일 실시간 조회 불가. 메모리 누적 + 주기 플러시는 서버 부하 거의 없고 웹 대시보드에서 약 10분 지연의 실시간 통계 제공 가능. 45일 시즌 서버에서 크래시 시 최대 10분 손실은 허용 범위.

**근거 문서:** `docs/11_web_dashboard/db_event_log_spec.md`, `docs/02_database_api_stats/CANON.md`

---

### DL-059 경매소 1차 포함 재확인 + 파티 경매 제외

**결정:**

1. **경매소 1차 포함** — `economy_numbers_v2.md §11` 2026-05-17 확정 사항 유지. 고정가 즉시 거래, 수수료 5%, 72시간 등록 기간.
2. **파티 경매 1차 제외** — 보스 드랍 후 파티원 간 입찰 분배 방식 미구현. 포로 서버는 기여도 3% 이상이면 개인 독립 확률로 드랍하는 구조이므로 파티 경매와 설계 전제가 충돌한다.

**이유:** 개인 독립 드랍 구조에서는 보스 보상이 이미 각자에게 귀속되므로 파티 경매를 위한 공용 드랍 풀이 존재하지 않는다. 구현 복잡도 대비 설계 이점 없음.

**근거 문서:** `docs/02_database_api_stats/economy_numbers_v2.md §11`, `docs/06_fields_bosses/CANON.md`

---

### DL-058 시즌보스 드랍 기준량 확정 + 강화석 완성품 폐지

**결정:**

1. **시즌보스 드랍 3인 기준량 (1.00×) 확정** — `drop_tables_v1.md §4` 초안 수치 그대로 공식 기준으로 채택.
2. **강화석 완성품 항목 폐지** — 강화석 DB 가상 재화 전환(M-5) 이후 남아 있던 "강화석 완성품" 행을 전 보스 드랍 테이블에서 제거.
3. **인원 배율 확정** — 방법 B 유지: 강화석·큐브 조각은 1인 0.55×/2인 0.78×/3인 1.00×. 고대흔적·장비의 흔적·치장 파편·칭호 재료·트로피는 인원 무관 동일.

**이유:** 드랍 수량이 초안으로만 남아 구현 블로커였음. 수치는 45일 시즌 경제 시뮬레이션(economy_numbers_v2.md) 기준 잉여 설계 의도와 충돌하지 않음. 강화석 완성품은 파편 시스템 시절 잔재로 DB 가상 재화 체계와 이중 설계가 됨.

**근거 문서:** `docs/06_fields_bosses/drop_tables_v1.md §4`, `docs/04_combat_weapon_skills/season_boss_stats_v1.md §8.1 R-5`

---

### DL-057 큐브 조각 필드보스 드랍 + 구현 미결 4종 확정

**결정:**

**큐브 조각 필드보스 기본 보상 (확정, DL-057):**

| 필드보스 | 큐브 조각 (기본 보상 확정) | 비고 |
|---|---|---|
| 들판 포식자 | 3~5개 | 기존 드랍 없음 → 신규 추가 |
| 폐광 골렘 | 5~8개 | 기존 드랍 없음 → 신규 추가 |
| 수로 군주 | 5~8개 | 기존 드랍 없음 → 신규 추가 |
| 타락한 수호기사 | 8~12개 | 기존 희귀 확률형(20%×10개) → 기본 확정으로 전환 |
| 균열 파수꾼 | 10~15개 | 기존 희귀 확률형(30%×15개) → 기본 확정으로 전환 |

**스킬타입 판별 구현:** 입력 이벤트 태깅 방식. 스킬 등록 시 `SkillType(BASIC/MOBILITY/SPECIAL/CORE)` 열거형 부여 → 피해 계산 시 해당 잠재 배율 주입. 별도 런타임 추적 없음.

**메모리얼 fallback:** 큐브 소모 확정 시 DB에 `pending_memorial` 레코드 기록. 재접속 시 pending 존재 → 선택 화면 재표시. 서버 재시작 소실 시 현재 옵션 유지. 큐브 복구 없음 (무료 롤 미리보기 악용 방지).

**이동속도% 바닐라 캡:** 레전더리 신발 최대 +13%는 바닐라 속성 캡 범위 내. 별도 처리 불필요. 구현 시 실측 확인만.

**이유:** 필드보스 큐브 조각을 확률형에서 확정형으로 전환 — 30분 보스 참여 보상의 예측 가능성 확보. 불확실한 0% 보상보다 소량 확정이 참여 유도에 효과적.  
**파일:** `docs/06_fields_bosses/drop_tables_v1.md`, `docs/02_database_api_stats/potential_options_v1.md`  
**근거:** 2026-05-24 확정

---

### DL-056 큐브 조각 교환 방식 확정 — 공방 제작 없음, 즉시 교환

**결정:** 큐브 조각 10개 → 큐브 1개 교환은 공방 가공기 제작 경로가 아닌 별도 즉시 교환 UI로 처리.

- 공방 가공기에 큐브 제작 탭 없음. 큐브 제작 재료 없음.
- 조각 10개 즉시 교환 UI (별도 GUI 또는 명령어 기반).
- 스코어보드에 현재 큐브 수 옆 **(큐브 조각: n개)** 표시.

**이유:** 공방 슬롯을 점유하지 않아 가공 병렬성 유지. 조각 수집 즉시 교환 가능해 수급 체감 개선.  
**파일:** `docs/02_database_api_stats/potential_options_v1.md`, `docs/10_development_roadmap/index.md`  
**근거:** 2026-05-24 확정

---

### DL-055 필드보스 P-10 전면 교체 + P-00 추가 (DL-048 후속)

**결정:** 필드보스 패턴 파일의 P-10 잔재(3곳) 제거 및 DL-048 기준 재정합.

| 보스 | 구 패턴 | 신 패턴 | 이유 |
|---|---|---|---|
| 폐광 골렘 Phase 2 | P-10 독 가스 장판 | P-12 연속 타격 | TPS 부하 제거, 골렘 묵직함 표현 |
| 수로 군주 Phase 2 | P-10 이동형 오염 수막 | P-11 추적 독 구체 | 이동 장판 TPS 최악, 수생 테마 유지 |
| 균열 파수꾼 Phase 3 | P-10 균열 에너지 장판 | P-13 낙하 충격 | 균열 낙하 연출로 대체 |

전 필드보스 5종에 **P-00 기본 공격** 추가. 헤더 "P-01~P-10" → "P-00~P-13" 갱신.

**파일:** `docs/07_boss_pattern_modules/field_boss_patterns.md`  
**근거:** DL-048 P-10 폐기 후속 정합 (2026-05-24)

---

### DL-054 시즌최종보스 패턴 확정 — SP-84 신설·동기화 전환·1~2인 보정 없음

**결정:**

**타락한 이중체 (SP-8X)**
- **동기화 Phase 전환**: 두 개체 모두 전환 HP%에 도달해야 발동. 먼저 도달한 개체는 피해 면역 대기.
- **전투 종료**: Z·S 두 개체 모두 0% 처치 필요.
- **SP-84 합체 저지 신설** (Phase 전환 #2): Z·S가 서로를 향해 이동하며 합체 시도. 파티원 차단 + 40%p 딜로 해제. 실패 시 융합체 생성 — HP 재생 + 즉사 광역으로 실질 클리어 불가 (와이프 메카닉).
- Phase 구조 표 확정: Phase 1(100%~60%) / 전환 #1 SP-83 / Phase 2(60%~30%) / 전환 #2 SP-84 / Phase 3(30%~0%)

**진혼의 주시자 (SP-9X)**
- **Phase 전환 HP%**: Phase 2 진입 70%, Phase 3 진입 35%.
- **P-09 취약 창**: 마법사 0명 기준 → **마법사 5마리 누적 처치**마다 취약 창 +20% 30초 (초반 0명 반복 악용 방지).

**공통**
- 시즌최종보스 3종: 1~2인 입장 가능, **인원 스케일 보정 없음** (3인 기준 설계).
- SP-81~84, SP-91~92 전체 상태 초안 → 확정.

**파일:** `docs/07_boss_pattern_modules/season_boss_patterns.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-053 보스4~6 패턴 확정 — SP-43 신설·보스5 P-12 추가·SP-62 1인 고난이도 확정

**결정:**

1. **SP-43 번개 집중포화 신설** (보스4 Phase 3 전용)  
   보스4 Phase 3에 고유 압박 패턴 부재 문제 해소. 15초 누적 피해 최다 플레이어에게 번개 3연타(각 130%, 0.8s 간격, 쿨 15s). 어그로 교체 전략 강제.

2. **보스5 P-12 연속 타격 추가** (Phase 2·3)  
   워든 계열임에도 근접 압박 패턴이 P-00·P-04만 존재했던 문제 보완. 피해 160%, 연타 2~4회, 0.5s 간격, 쿨 9s. 근거리 딜러 위험 강화.

3. **SP-62 1인 고난이도 의도 확정**  
   허상 4개·오공격 HP 회복 패널티는 1인 입장 시 그대로 유지. 보스6 1인 클리어는 최고 난이도 도전으로 의도.

**SP-41~62 전체 상태 초안 → 확정.**

**파일:** `docs/07_boss_pattern_modules/season_boss_patterns.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-052 보스 클리어 통계 수집 스펙 확정

**결정:** 웹 운영 대시보드 및 밸런스 판단을 위한 보스 클리어 통계 DB 스키마·API 설계 확정.

수집 항목:
- 보스별 클리어율 / 타임아웃 실패율 / 패턴 실패율
- 평균 클리어 시간 (클리어 성공 세션만)
- 파티 평균 강화 수치 / 평균 IL
- 방무 옵션 보유율 (파티 내 최소 1인 보유 기준)
- 시즌 주차별 클리어율 추이

DB 테이블: `boss_session_log` (세션), `boss_session_player` (참여자 스펙)  
API: `GET /api/v1/boss/stats`, `/boss/{boss_id}/stats`, `/boss/{boss_id}/weekly`, `/boss/{boss_id}/party-spec`

버프/너프 기준: 클리어율이 설계 목표 ±15~20%p 이상 2주 지속 시 검토.

**파일:** `docs/02_database_api_stats/boss_clear_stats_spec.md` (신규), `docs/02_database_api_stats/CANON.md`  
**근거:** INBOX-002 → 확정 (2026-05-24)

---

### DL-051 시즌보스 1~6 HP 우상향 재설계 — 방어력 무시 필수화

**결정:** 보스1→6 HP를 우하향에서 **우상향**으로 전면 재설계.

| 보스 | 구 HP | 신 HP | 방무 없으면 |
|---|---|---|---|
| 보스1 타락 기사장 | 160,000 | **150,000** | 클리어 가능 (입문) |
| 보스2 오염된 군주 | 155,000 | **155,000** | 빡빡하지만 가능 |
| 보스3 석조 거상 | 150,000 | **158,000** | 취약창 필수 |
| 보스4 폭풍 술사 | 145,000 | **160,000** | 타임아웃 실패 |
| 보스5 심연 수호자 | 135,000 | **165,000** | 클리어 불가 |
| 보스6 공허 사자 | 125,000 | **170,000** | 레전더리 방무 필요 |

- DEF는 이미 우상향 (100→265). HP도 우상향으로 맞춰 두 수치 모두 강함이 직관적으로 올라가는 구조.
- 방어력 무시 유니크(~17%)가 보스4+ 클리어의 실질 필요 조건.
- 정확한 TTK 검증은 M-1 프리 시즌 실측 후. 방향성 확정, 수치는 조정 가능.

**파일:** `docs/04_combat_weapon_skills/season_boss_stats_v1.md`
**근거:** 사용자 확인 (2026-05-24)

---

### DL-050 균열왕 분노 타이머 수정

**결정:** 분노 트리거 "25분 경과" → **"8분 경과"** 로 수정.

- 구버전(35분 타이머 시절) 설계값이 그대로 남아있던 오류.
- 확정 타이머 10분 기준으로 8분 경과 시 전 패턴 속도 +20%, 분노 구간 진입.

**파일:** `docs/07_boss_pattern_modules/season_boss_patterns.md`  
**근거:** 타이머 불일치 발견 + 사용자 확인 (2026-05-24)

---

### DL-049 SP-83 결속 차단 신설 (타락한 이중체 Phase 전환 #1)

**결정:** 타락한 이중체 Phase 전환 #1 메카닉으로 SP-83 결속 차단 추가.

- Z·S 사이 결속 링크 형성 → 피해 전이율 50% (딜 상쇄)
- 파티원 1명이 두 개체 사이 위치 시 링크 차단 → 기절 4초 + 취약 창 +30%
- 실패 시 전이율 80% 강화 + 분노 복귀
- **설계 의도**: SP-81(개체 분리 강제) ↔ SP-83(집결 강제) — 전환마다 파티 배치 반전 압박

**파일:** `docs/07_boss_pattern_modules/season_boss_patterns.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-048 P-10 장판 패턴 폐기 + 전 보스 대체

**결정:** P-10(안전지대/지속 장판) 공용 패턴 **완전 폐기**. 서버 TPS 부하 이유.

- MythicMobs 지속 장판은 매 틱 범위 검사 — 보스 전투 중 복수 장판 상시 활성 시 TPS 직접 영향.
- 교체 원칙: 지속 체크 없는 단발 폭발·낙하·추적 구체로 동일 위협 연출.

| 보스 | 구 패턴 | 교체 |
|---|---|---|
| 보스2 오염된 군주 | P-10 전자기장 | P-03 전자기 폭발 15s 주기 |
| 보스4 SP-42 실패 | 번개 장판 상시 | P-06 산개탄 8s 강제 발동 |
| 보스4 분노(12분) | 번개 장판 1개 상시 | P-06 쿨타임 50% 감소 |
| 보스5 심연 수호자 | P-10 암흑 구간 | P-13 암흑 낙하 20s 주기 |
| 보스6 공허 사자 | P-10 균열 장판 | P-11 추적 균열 구체 (착탄 시 텔레포트) |
| 균열왕 | P-10 저주 성장 장판 | P-03 저주 폭발 Phase별 반지름 증가 |

**파일:** `docs/07_boss_pattern_modules/common_patterns.md`, `season_boss_patterns.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-047 공용 패턴 풀 개편 — P-00 신설 + P-11/12/13 추가

**결정 1 — P-00 기본 공격 신설**

- 전 보스 공통 보유. 쿨타임 1.5~2.2s, 피해 60~90%, 예고 0.3s.
- 스킬(P-01 이상) 쿨타임 사이를 채워 "보스가 항상 뭔가를 한다"는 전투 리듬 형성.
- 원거리 보스(보스2·4)는 약한 투사체 형태로 오버라이드.

**결정 2 — P-11·P-12·P-13 추가**

| 코드 | 이름 | 개요 |
|---|---|---|
| P-11 | 추적 구체 | 특정 플레이어 유도 추적. 보스 방향으로 유인하면 소멸. |
| P-12 | 연속 타격 | 2~4회 빠른 근접 연타. 1타씩 약하나 전타 합산 피해 큼. |
| P-13 | 낙하 충격 | 공중 도약 후 낙하, 착지 광역 피해. 파티클로 낙하 지점 예고. |

- P-14(독기/화염 장판)은 서버 부하 이유로 미채택 (DL-048 참조).

**파일:** `docs/07_boss_pattern_modules/common_patterns.md`, `season_boss_patterns.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-046 석조 거상 (보스3) 패턴 전면 재설계

**결정:** SP-3X 전체 교체. 암흑/촉수 테마(구 심연의 군주) → 석조/지진 테마(아이언 골렘).

| 항목 | 구버전 | 신버전 |
|---|---|---|
| 공용 패턴 | P-07(소환체) P-08 P-09 P-10 | P-01 P-02 P-03 P-04 P-08 P-09 |
| 고유 패턴 | SP-31~35 (암흑·수정·에너지) | SP-31 지진 발걸음, SP-32 거석 팔 투척 |
| Phase 수 | 4 (15% 추가 Phase) | 3 (60%·25%) |

- SP-31 지진 발걸음: 발구름 3회 동심원 진동파 → 점프 회피 + 석판 충전으로 무적 해제
- SP-32 거석 팔 투척: 팔 오브젝트(HP 20,000) 파괴로 무적 해제. 성공 시 Phase 3 P-04 범위 -30%, 실패 시 공격력 +15% 분노

**파일:** `docs/07_boss_pattern_modules/season_boss_patterns.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-045 시즌최종보스 칭호명 확정 + 닉네임·커스텀 칭호 변경권 보스 드랍 추가

**결정 1 — 칭호명 확정**

| 보스 | 클리어 칭호 |
|---|---|
| 균열왕 | **수호자** |
| 타락한 이중체 | **이중주** |
| 진혼의 주시자 | **낙성자** |
| 3종 모두 클리어 | **심연을 넘은 자** |

**결정 2 — 한글 닉네임 변경권 · 커스텀 칭호 변경권 시즌보스 4+ 확률 드랍 추가**

| 보스 | 닉네임 변경권 (S/A) | 커스텀 칭호 변경권 (S/A) |
|---|---|---|
| 시즌보스4 | 5% / 3% | 3% / — |
| 시즌보스5 | 8% / 5% | 5% / 2% |
| 시즌보스6 | 12% / 7% | 7% / 3% |
| 시즌최종보스 3종 | 15% / 10% | 10% / 5% |

- 두 아이템 현재 상점 미판매. 보스 드랍 단일 경로.
- 수치는 초안 — 운영 후 조정 가능.

**파일:** `docs/06_fields_bosses/drop_tables_v1.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-044 균열왕 봉인 파편 입장 조건 폐지

**결정:** 균열왕 봉인 파편 드랍·입장권 제작 시스템 전면 삭제. 보스6 클리어 시 시즌최종보스 3종 자유 입장.

- 시즌보스 4·5·6 보상에서 균열왕 봉인 파편 행 제거
- 균열왕 입장 조건: "봉인 파편 수집 후 입장권 제작" → "보스6 클리어 후 자유 입장"
- 공방 가공기 균열왕 입장권 제작 레시피 불필요

**근거:** 입장권 수집 과정이 콘텐츠 진행 흐름을 단절시킴. 보스6 클리어 자체가 최종보스 도전 자격의 자연스러운 기준. 사용자 확인 (2026-05-24)  
**파일:** `docs/06_fields_bosses/drop_tables_v1.md`, `docs/decision_log.md`

---

### DL-043 시즌최종보스 3종 티어 확정

**결정:** 시즌최종보스를 균열왕 단일 최종이 아닌 3종 동급 최종 티어로 구성.

| 순번 | 이름 | 베이스 몹 | 핵심 메카닉 | 고유 칭호 |
|---|---|---|---|---|
| 최종보스1 | **균열왕** | 위더스켈레톤+말 | 분노 스택 (SP-7X) | 수호자 (DL-045) |
| 최종보스2 | **타락한 이중체** | 좀비+스켈레톤 쌍 | HP 균형 광폭화 (SP-8X) | 이중주 (DL-045) |
| 최종보스3 | **진혼의 주시자** | 위더 | 별빛 마법사 스택 (SP-9X) | 낙성자 (DL-045) |

- 3종 모두 클리어율 목표 ~10%, 우열 없음
- **입장 조건:** 보스6 클리어 후 자유 입장 (봉인 파편 폐지 — DL-044)
- **보상:** 각 고유 칭호 별도 지급 (수호자·이중주·낙성자) + 3종 모두 클리어 시 **심연을 넘은 자** 칭호 발급 (DL-045)
- 찬란한 고대흔적·강화석·큐브 등 나머지 보상은 3종 동일 구조 적용
- **타락한 이중체 핵심 설계:** 좀비 HP% · 스켈레톤 HP% 차이 >25% 시 광폭화. 각 HP 445,000 (총 890,000), DEF 270, 타이머 10분
- **진혼의 주시자 핵심 설계:** 위더가 별빛 마법사 주기 소환. 마법사 수 스택별 위더 받피감 증가: 1~2명 +15%/명, 3~4명 +20%/명 추가, 5명+ 전원 소모 → 별빛 대마법 발동. HP 890,000, DEF 280, 타이머 10분

**파일:** `docs/06_fields_bosses/CANON.md`, `docs/06_fields_bosses/drop_tables_v1.md`, `docs/04_combat_weapon_skills/season_boss_stats_v1.md`, `docs/07_boss_pattern_modules/season_boss_patterns.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-042 시즌보스 6종 + 균열왕 이름 확정

**결정:** 보스 이름 7종 확정.

| 보스 | 이름 | 베이스 몹 | 컨셉 |
|---|---|---|---|
| 보스1 | **타락 기사장** | 좀비 + 풀아머 | 타락한 제국 중갑 기사 |
| 보스2 | **오염된 군주** | Drowned + 아머 | 오염된 수로 군주 |
| 보스3 | **석조 거상** | 아이언 골렘 | 석조 거대 골렘 |
| 보스4 | **폭풍 술사** | 에보커 | 폭풍 소환사 (마법형) |
| 보스5 | **심연 수호자** | 워든 | 심연의 수호자 (어둠/지하) |
| 보스6 | **공허 사자** | 엔더맨 | 균열 파편 (균열 에너지) |
| 최종보스 | **균열왕** | 위더스켈레톤 + 말 | 검 든 기사왕 |

**파일:** `docs/06_fields_bosses/CANON.md`, `docs/06_fields_bosses/drop_tables_v1.md`, `docs/04_combat_weapon_skills/season_boss_stats_v1.md`, `docs/07_boss_pattern_modules/season_boss_patterns.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-041 시즌보스 3종 → 6종으로 확장

**결정:** 시즌보스 6종 + 균열왕 구조로 변경.

| 보스 | 클리어율 | 권장 강화 | 고대흔적 |
|---|---|---|---|
| 보스1 | 75% | 6~10강 | 빛 바랜 |
| 보스2 | 65% | 10~13강 | 빛 바랜 |
| 보스3 | 55% | 13~16강 | 빛나는 |
| 보스4 | 45% | 16~18강 | 빛나는 |
| 보스5 | 35% | 18~20강 | 눈부신 |
| 보스6 | 25% | 20~22강 | 찬란한 |
| 균열왕 | 10% | 22강+ | 찬란한 |

- 모든 보스 자유 입장 (해금 조건 없음)
- 45일 시즌 상한 22강에 맞춰 상한 수정 (기존 20강 → 22강)
- 모듈 패턴 시스템으로 추가 3보스 구현 비용 최소화
- 컨셉/이름/스탯/보상 표는 후속 작업에서 확정

**파일:** `docs/06_fields_bosses/CANON.md`, `docs/final_master_plan.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-040 균열왕 분노 스택 수치 확정

**결정:** 스택당 공격력 +10%, 최대 3스택(+30%)

| 발동 조건 | 스택 |
|---|---|
| SP-41 에너지 결절 방치 (전환 #1) | +1 |
| SP-44 분신 처치 실패 (전환 #4) | +1 |
| 25분 경과 후 무적 기믹 실패 | +1 |

3스택 도달 시 공격력 +30% → 사실상 클리어 불가 수준의 압박. "기믹 실패 = 끝" 의도된 설계.

> **보스명/컨셉 변경 가능성 있음.** "균열왕"은 현재 임시 명칭. 변경 시 수치 재검토 가능하나 스택 구조 자체는 유지.

**파일:** `docs/04_combat_weapon_skills/season_boss_stats_v1.md` (M-7 해소), `docs/07_boss_pattern_modules/season_boss_patterns.md` (기존 초안 수치 확정)  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-039 시즌보스 핵심 재료 폐기 → 고대흔적 확률 드랍으로 교체

**결정:** 시즌보스 전용 핵심 재료 아이템 제거. 대신 티어별 고대흔적을 확률 드랍.

| 보스 | 드랍 고대흔적 | S | A | B | C |
|---|---|---|---|---|---|
| 시즌보스 1 | 빛 바랜 고대흔적 | 확정 | 70% | 40% | — |
| 시즌보스 2 | 빛나는 고대흔적 | 확정 | 확정 | 50% | — |
| 시즌보스 3 | 눈부신 고대흔적 | 확정 | 확정 | 70% | 30% |
| 균열왕 | 찬란한 고대흔적 | 확정 | 확정 | — | — |

- 고대흔적은 **주간 첫 클리어에서만 지급** (재도전 보상 미포함)
- 인원 배율 표에서 "시즌보스 핵심 재료" → "고대흔적" 희소재 보호 유지

**폐기된 아이템:** `시즌보스 1/2/3 핵심 재료`

**파일:** `docs/06_fields_bosses/drop_tables_v1.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-038 균열왕 최소 입장 인원 제한 없음 확정

**결정:** 최소 입장 인원 제한 없음. 솔로·2인·3인 모두 입장 허용.

- 솔로 TTK 38분으로 35분 타이머 초과 → 사실상 솔로 클리어 불가이나 입장 자체는 막지 않음
- 입장 시 인원 부족 경고 메시지 출력 (`§e[경고] 현재 인원으로 클리어가 어려울 수 있습니다.`)
- 클리어 여부는 유저 책임. 구현 복잡도 절감 (인원 체크 로직 불필요)

**파일:** `docs/04_combat_weapon_skills/season_boss_stats_v1.md` (R-3, M-3 해소), `docs/06_fields_bosses/CANON.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-037 시즌보스 반복 보상 구조 확정

**결정:** 주간 첫 클리어 풀 보상 + 이후 재도전 감소 보상 (방안 A)

| 항목 | 기준 |
|---|---|
| 보상 리셋 | 매주 월요일 00:00 (서버 시간) |
| 첫 클리어 보상 | 장비의 흔적 · 핵심 재료 · 칭호 재료 포함 풀 보상 |
| 재도전 보상 | 강화석 · 큐브 조각 · 핵심 재료 소량만. 장비의 흔적 · 칭호 재료 미포함 |
| 균열왕의 심장 | 매 클리어 드랍 허용 (시즌 고유 트로피, 반복 획득 의미 있음) |
| 참여 기준 | 기여도 3% 이상 |

**45일 시즌 기준:** 6주 = 풀 보상 최대 6회 기회.

**파일:** `docs/06_fields_bosses/drop_tables_v1.md`, `docs/06_fields_bosses/CANON.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-036 공방 가공기 오프라인 완료 — 타임스탬프 기반으로 확정

**결정:**
- 공방 대기열은 사이클 캡 없는 타임스탬프 기반 완료로 처리
- `island_workshop_queue`에 `duration_minutes`, `due_ts` 컬럼 추가
- 완료 조건: `due_ts ≤ now` (= 등록 시각 + 처리 시간)
- 완료 체크 시점: 스케줄러 20분 틱 + `PlayerLoginEvent` + GUI 열람
- 완료 시 영지 창고 자동 입금 + 로그인 알림

**약초 재배지 / 광물 채굴기는 기존 3사이클 캡 유지** — 주기적 생산 재화 폭발 방지 목적.

**파일:** `docs/05_island_farm_system/island_system_design.md`, `docs/05_island_farm_system/CANON.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-035 공방 가공기 처리 시간 확정

**목표 사이클:** 사냥 전 대기 등록 → 1~2시간 사냥 후 귀환 → 완료 확인 → 재등록 → 재사냥

**처리 시간 기준:**

| 등급 | 시간 | 레시피 |
|---|---:|---|
| 즉시 소비품 | 5분 | 제련(마도합금), 정제(약초), 광물 변환 전종 |
| 단기 소비품 | 10분 | 치료 포션 3종, 만찬 4종 |
| 핵심 재료 | 15분 | 자연의 정수, 부스트 포션 3종 |
| 핵심 재료 | 20분 | 별의 흔적 |
| 핵심 재료 | 30분 | 농부의 정수, 달의 흔적, 미감정 흔적, 빛 바랜 고대흔적 |
| 핵심 재료 | 45분 | 광부의 정수, 태양의 흔적, 빛나는 고대흔적 |
| 고급 완성품 | 60분 | 눈부신 고대흔적 |
| 고급 완성품 | 90분 | 찬란한 고대흔적 (최대 1.5 사냥 세션) |

**설계 의도:** 처리 시간이 곧 "게임 내 대기 비용". 고급일수록 길어지지만 최대 90분(찬란한)을 상한으로 둬 오프라인 누적 보상(최대 3사이클)과 조합 시 비효율이 없도록 설계.

**파일:** `docs/05_island_farm_system/workshop_crafting_spec.md`  
**근거:** 사용자 확인 (2026-05-24)

---

### DL-034 마도합금 용도 확정 + 레시피 재구성

**결정:**
1. `mat_mado_alloy` (마도합금) 용도: 강화 흔적 3종 + 고대흔적 4종 핵심 재료
2. 강화 흔적 / 고대흔적 레시피에서 광부의 정수 → 마도합금으로 교체
3. 부스트 포션 레시피 현행 유지 (농부의 정수 × 1 + 정제 약초 × 1 + 철블럭 × 16 + 금블럭 × 16)

**마도합금 투입량:**

| 결과물 | 마도합금 |
|---|---|
| 별의 흔적 | × 10 |
| 달의 흔적 | × 20 |
| 태양의 흔적 | × 40 |
| 빛 바랜 고대흔적 | × 20 |
| 빛나는 고대흔적 | × 40 |
| 눈부신 고대흔적 | × 60 |
| 찬란한 고대흔적 | × 80 |

**역할 분리 결과:**
- 광부의 정수: 자연의 정수 → 미감정 흔적 체인 + 부스트 포션 전용 (고비용 유지)
- 마도합금: 광물 채굴기 → 제련 → 강화 흔적/고대흔적 체인 담당

**파일:** `docs/05_island_farm_system/workshop_crafting_spec.md`
**근거:** 사용자 확인 (2026-05-24)

---

### DL-033 강화석 드랍률 + 11~16강 성공률 확정

**개발 철학:** 수급량을 수학적으로 맞추려 하지 않는다. 공급을 넉넉하게 두고 성공률을 낮춰 플레이어가 버튼을 많이 누르게 하는 것이 목표.

**강화석 드랍률 (일반몹, 세트 A):**

| 필드 | 구 확률 | 신 확률 |
|---|---|---|
| 필드1 수도 외곽 평원 | 4% | **6%** |
| 필드2 폐광 지대 | 5% | **8%** |
| 필드3 오염된 수로 | 6% | **9%** |
| 필드4 무너진 초소 | 6% | **10%** |
| 필드5 고대 성벽 잔해 | 6% | **12%** |

정예몹·필드보스 드랍률 변경 없음.

**11~16강 성공률 하향 (세트 A):**

| 강화 | 구 성공률 | 신 성공률 | 신 천장 |
|---|---|---|---|
| 11강 | 50% | **25%** | 8회 |
| 12강 | 40% | **20%** | 10회 |
| 13강 | 35% | **17%** | 12회 |
| 14강 | 30% | **15%** | 14회 |
| 15강 | 25% | **12%** | 17회 |
| 16강 | 20% | **10%** | 20회 |

17~25강 성공률 현행 유지.

**파일:**
- `docs/06_fields_bosses/drop_tables_v1.md`
- `docs/02_database_api_stats/economy_numbers_v2.md`
- `docs/final_master_plan.md` §13 미확정 항목 업데이트

**이유:** 버튼 많이 누르는 경험 > 정밀한 수급 계산. 오픈 후 3일 실측 기반 재조정 여지 유지.
**근거:** 사용자 확인 (2026-05-24)

---

### DL-032 스킬 히트박스 수치 / 이동기 거리 확정

**결정:** 전 무기 24스킬의 판정 수치(반경·각도·길이·너비·사거리·탄속) 및 이동기 6종 거리·소요시간 확정.

**스킬 데이터 스키마 추가 필드:**
- `angle` — melee_arc / cone 좌우 총 각도(°)
- `movement_distance` — 이동기 이동 거리(블럭)
- `movement_duration` — 이동 소요 시간(초)

**기준값 요약:**
- 기본 근접 reach 2.5블럭, 창·낫처럼 컨셉이 "넓거나 긴" 무기는 +0.5~1블럭
- 이동기 거리: 스태프 1.5 < 검/석궁 2~2.5 < 도끼 3 < 창 5블럭
- 각도: 표준 근접 120°, 좌우 대응 보완 스킬 150°

**파일:** `docs/04_combat_weapon_skills/weapon_skills_v1.md` — 히트박스 섹션 + 스키마 필드 추가
**근거:** 사용자 확인 (2026-05-24)

---

### DL-031 스킬 이펙트 시스템 아키텍처 확정

**결정:** 방안 4 혼합 구조 — MythicMobs(파티클·사운드) + Display Entity(투사체·검기) + Bukkit Particle(fallback) 조합.

**원칙:**
1. 스킬 판정·피해·쿨타임·상태는 EmpireRPG 단독 책임
2. MythicMobs는 이펙트 위임 전용, 판정 위임 없음
3. Display Entity는 0.5초 이상 보이는 투사체·검기·마법탄에만 사용
4. MythicMobs 없어도 전투 로직 정상 작동 (graceful degradation)
5. ModelEngine/BetterModel은 2차 확장 보류

**effect_key prefix 규칙:**
- `mm:xxx` — MythicMobs castSkill
- `dp:xxx` — Display Entity
- `pt:xxx` — Bukkit Particle 직접
- 빈 값 — 이펙트 없음

**구현 격리 원칙:** MythicMobs API 직접 호출은 `MythicMobsEffectHandler` 안에만 격리. EmpireRPG core는 인터페이스(`EffectDispatcher`)만 참조.

**파일:**
- `docs/04_combat_weapon_skills/weapon_skills_v1.md` — 이펙트 시스템 섹션 + 24개 스킬 effect_key 기준값 추가

**이유:** MythicMobs는 이미 스택에 있어 추가 의존 없음. prefix 기반 dispatch는 핸들러를 교체해도 스킬 데이터를 건드리지 않아 2차 확장 시 ModelEngine 교체 비용 최소화.
**근거:** 사용자 확인 (2026-05-24)

---

### DL-030 디스코드 봇 설계 결정 3종

**결정:**
1. `/강화계산` — 봇 내부 계산 (강화 비용 표 하드코딩). API 호출 없음.
2. `/프로필` 타인 조회 — 전체 공개. 본인과 동일 형식 출력.
3. 봇 구현 언어 — **Node.js (Discord.js)**, EmpireRPG와 독립 프로세스.

**파일:**
- `docs/03_discord_onboarding_bot/discord_bot_spec.md`

**이유:**
- `/강화계산`: 강화 비용은 CANON 고정 수치. API 왕복 불필요. 봇 내부 계산이 응답 빠르고 구현 단순. 45일 시즌 내 수치 변경 가능성 낮음.
- 타인 프로필 공개: 파티 모집·랭킹 맥락에서 플레이어 간 정보 공유가 게임플레이 경험에 유익. 숨길 민감 정보 없음.
- Node.js: Discord.js가 버튼·모달·선택메뉴 등 인터랙티브 기능을 가장 완전하게 지원. 게임 서버와 독립되어 봇 재시작이 게임에 영향 없음. 커뮤니티·예제 가장 풍부.
**근거:** 사용자 확인 (2026-05-24)

---

### DL-029 버그 제보 접수번호 체계 확정

**결정:** 디스코드 봇 `/버그제보` 접수번호는 `bug_report.id` AUTOINCREMENT를 사용하고 `BUG-{id}` 형식으로 공개한다.

**파일:**
- `docs/03_discord_onboarding_bot/discord_bot_spec.md`
- `docs/11_web_dashboard/db_event_log_spec.md` — `bug_report` 테이블 추가

**이유:** 날짜+순번 방식(`BUG-20260523-001`)은 날짜 파싱·중복 방지 로직이 추가로 필요하다. DB AUTOINCREMENT는 SQLite가 보장하므로 구현이 단순하고, 45일 시즌 내 버그 수가 수백 건 이하로 예상되므로 번호 자릿수 부담 없음.
**근거:** 사용자 확인 (2026-05-24)

---

### DL-028 운영자 웹 대시보드 도메인 신설

**결정:** `docs/11_web_dashboard/` 폴더를 신설하고 운영자 전용 웹 대시보드 설계 문서 4종을 작성한다.

**변경:**
- `docs/11_web_dashboard/index.md` 신규: 전체 구조 개요, 페이지 목록, 기술 스택
- `docs/11_web_dashboard/web_dashboard_spec.md` 신규: 페이지별 레이아웃·필터·표시 데이터 상세
- `docs/11_web_dashboard/api_endpoints.md` 신규: 대시보드 호출 API 엔드포인트 목록
- `docs/11_web_dashboard/db_event_log_spec.md` 신규: 경제·전투 이벤트 로그 DB 테이블 설계
- `docs/final_master_plan.md`: 공식 문서 구조 표에 도메인 11 추가, §11 데이터와 API에 웹 대시보드 설명 추가

**이유:** 45일 시즌 서버에서 경제 이상·보스 밸런스를 운영 중 감지하고 근거 있는 너프/버프 판단을 하기 위해 운영자 관제 도구가 필요하다.

**핵심 결정 내용:**
- 접근: 운영자 전용, Bearer 토큰 인증
- 데이터 원본: EmpireRPG HTTP API(포트 8765) 단일 경로. DB 직접 접근 없음
- 갱신: 일별 집계(자정 스냅샷). 실시간은 현재 접속자·최근 보스 클리어만
- 1차 범위: 경제 관제, 보스 기록, 서버 현황, 아이템 발행 수
- 기존 `empire.db` 테이블 수정 없음. 이벤트 로그 테이블 10종 신규 추가만

**근거:** 사용자 요구사항 (2026-05-23)

---

### DL-026 날개 치장 1차 시즌 포함 확정

**결정:** 날개 치장(wing_volt_poro_01)을 1차 시즌 리소스팩 범위에 포함한다.

**변경:**
- `final_master_plan.md` 리소스팩 우선순위에서 "날개 치장 제외" → "날개 치장 1종 포함"으로 수정
- CMD 500xxx 범위를 코스메틱 전용으로 신설 (500001~500099: 날개)
- `paper.json` CMD 500001 = `poro:item/cosmetics/wings/wing_volt_poro_01` 등록
- `assets/source/items/cosmetics/cosmetics_registry.yml` 신규 생성
- 코스메틱 아이템 lore에 "장착 부위: XXX" 명시 + 플러그인 슬롯 차단 규칙 확정

**이유:** 모델·텍스처가 이미 완성돼 있어 추가 비용 없이 등록 가능. 1차 오픈 콘텐츠 다양성 확보.
**근거:** 사용자 확인 (2026-05-23)

---

## 2026-05-22 — PHASE 2 canon 충돌 수정

### DL-001 Citizens 플러그인 제거

**파일:** `docs/01_plugin_architecture/index.md`  
**변경:** 플러그인 목록 테이블에서 `Citizens | NPC 껍데기` 행 제거  
**이유:** `final_master_plan.md`의 "플러그인 구조"에서 Citizens 제거가 확정됨. NPC 역할은 EmpireRPG 자체 처리로 전환.  
**근거:** `final_master_plan.md`의 "플러그인 구조" (2026-05-20 기준)

---

### DL-002 마력 과부하 채팅 메시지 제거

**파일:** `docs/04_combat_weapon_skills/index.md`  
**변경:** 채팅/알림 포맷 테이블에서 `마력 과부하`, `마력 과부하 반복` 행 제거  
**이유:** 마력 시스템(발전기·마력 소비 구조) 2026-05-19 전면 폐지 확정.  
**근거:** `final_master_plan.md`의 "개인 영지" + `economy_numbers_v2.md`의 마력 시스템 폐지 주석

---

### DL-003 무기 이름 망치 → 도끼

**파일:**  
- `docs/04_combat_weapon_skills/weapon_skills_v1.md` (전체 표시명 치환)  
- `docs/04_combat_weapon_skills/index.md` (무기 클래스 테이블, GUI 레이아웃, 아이콘 테이블)  
**변경:** 표시명 "망치" → "도끼" (당시 YAML 코드 식별자 `hammer`는 구현 로직으로 유지했으나, DL-025에서 `axe`로 전환 확정)
**이유:** `final_master_plan.md`의 "전투와 장비 성장"에서 무기 6종 중 "도끼"로 확정됨. `weapon_skills_v1.md`의 도끼 항목도 `minecraft:netherite_axe`를 추천 베이스 아이템으로 기재함.  
**근거:** `final_master_plan.md`의 "전투와 장비 성장" (2026-05-20 기준)

---

### DL-004 도끼 베이스 아이템 NETHERITE_PICKAXE → NETHERITE_AXE

**파일:** `docs/04_combat_weapon_skills/index.md`  
**변경:** 무기 선택창 아이콘 테이블에서 도끼 베이스 아이템을 `NETHERITE_PICKAXE` → `NETHERITE_AXE`로 수정  
**이유:** `weapon_skills_v1.md`의 도끼 항목에 "추천 베이스 아이템: `minecraft:netherite_axe`"로 명시됨. index.md의 `NETHERITE_PICKAXE`는 이전 망치 기반 시절의 잔재.  
**근거:** `weapon_skills_v1.md`의 도끼 항목 + `final_master_plan.md`의 "전투와 장비 성장"

---

### DL-005 강화 비용표 전면 교체

**파일:** `docs/02_database_api_stats/economy_numbers_v2.md`  
**변경:** `### 강화 비용표` 섹션 전체를 `final_master_plan.md`의 "전투와 장비 성장" 및 "미확정 항목" 확정 내용 기준으로 교체  
**상세 변경:**
- 골드 비용: 1강 180G → **2,000G**, 22강+ 25,000G → **27,000G 고정**
- 강화석 파편 시스템 폐지 → 강화석 직접 소모 (M-5 확정)
- 보조재 B/C 시스템 폐지 → 강화 흔적 3종(별/달/태양) 시스템 (M-3 확정)
- 방어구 강화석 비율: 무기의 50% → `ceil(무기 강화석 ÷ 1.5)` (약 67%, 확정 2026-05-20)
- 구 계산 테이블(강화석 파편 소모량, 보조재 소모량)은 구버전 표기로 인라인 주석 처리

**이유:** economy_numbers_v2가 2026-05-15 기준 수치이고, `final_master_plan.md`의 "전투와 장비 성장"은 2026-05-20 M-3 확정 기준. 최신 확정이 우선.  
**근거:** `final_master_plan.md`의 "전투와 장비 성장" 및 `docs/02_database_api_stats/CANON.md`

---

### DL-006 마력 관련 항목 제거 (economy_numbers_v2)

**파일:** `docs/02_database_api_stats/economy_numbers_v2.md`  
**변경:**
- `economy_numbers_v2.md`의 병목 구간 테이블에서 "발전기 마력 부족" 행 제거
- `economy_numbers_v2.md`의 v2 신규 체크포인트 테이블에서 "마력 과부하 발생 비율" 행 제거
- `economy_numbers_v2.md`의 영지 편의 해금 테이블에서 "발전기 효율 업그레이드 +5 MP/h" 행 제거
**이유:** 마력 시스템 2026-05-19 전면 폐지.  
**근거:** `final_master_plan.md`의 "개인 영지" + `economy_numbers_v2.md`의 마력 시스템 폐지 주석

---

## 2026-05-22 — PHASE 4 final_master_plan 재정리

### DL-007 final_master_plan.md 2,341줄 → 601줄 축소

**파일:** `docs/final_master_plan.md`  
**변경:** 세부 수치·슬롯 매핑·레시피 체인·확정 완료 M-tags를 "→ 상세: X 참조" 형태로 위임.  
**제거 및 위임 목록:**
- 무기 6종 스킬 상세 → `04_combat_weapon_skills/weapon_skills_v1.md`
- 장비 포맷·시작 장비 → `01_plugin_architecture/implementation_reference.md`
- 시스템 메시지 포맷 → `01_plugin_architecture/CANON.md`
- 상점 상세 → `02_database_api_stats/CANON.md`
- 잠재 등급 상세 표 → `02_database_api_stats/potential_options_v1.md`
- 강화 비용표 전체 → `02_database_api_stats/economy_numbers_v2.md`
- 작위별 상세 표 → `05_island_farm_system/CANON.md`
- 공방 레시피 체인 → `05_island_farm_system/workshop_crafting_spec.md`
- 광물 생성기 확률표 → `05_island_farm_system/CANON.md`
- 엘리베이터 구현 코드 → `01_plugin_architecture/implementation_reference.md`
- GUI 슬롯 매핑 전체 → `08_resourcepack_pipeline/gui_*.md`
- 확정 완료 M-tags → 각 도메인 문서와 `decision_log.md`에 흡수
- 관리자 커맨드 상세 표 → `01_plugin_architecture/admin_command_spec.md`
**추가 수정:** 장비 이름 변경권 가격 300,000G → **10,000G** (DL-007 충돌 해소, M-11 확정 반영)  
**이유:** final_master_plan을 프로젝트 철학·핵심 방향성·시스템 연결 중심 문서로 전환. 세부 내용은 전용 시스템 문서(CANON.md, 시스템 스펙 문서)에서 관리.  
**근거:** `docs/_archive/master_plan_content_audit.md` 분류 기준

---

## 2026-05-22 — PHASE 5 docs 리빌드 검수 반영

### DL-008 archive 구조 확정

**파일:**
- `docs/_archive/README.md`
- `docs/_archive/docs_restructure_plan.md`
- `docs/_archive/master_plan_content_audit.md`
- `docs/_archive/11_remaining_decisions/index.md`

**변경:** 활성 docs 루트에 남아 있던 리빌드 계획/감사/구 결정 문서를 `docs/_archive/`로 이동하고, README에 archive 이유와 대체 문서를 기록.  
**이유:** 실행 완료된 계획 문서와 흡수된 결정 문서가 활성 기준 문서처럼 보이는 문제 제거.  
**근거:** docs 리빌드 검수 기준 — archive 폴더는 `docs/_archive/`로 통일.

---

### DL-009 구현 레퍼런스 경로 확정

**파일:** `docs/01_plugin_architecture/implementation_reference.md`  
**변경:** 기존 루트 `docs/final_설계_plan.md`를 플러그인 아키텍처 하위 구현 레퍼런스로 이동.  
**이유:** `final_master_plan.md`과 각 CANON에서 이미 구현 상세를 `01_plugin_architecture/implementation_reference.md`로 위임하고 있었으므로 실제 파일 경로를 참조와 일치시킴.  
**근거:** `docs/01_plugin_architecture/CANON.md`

---

### DL-010 CANON.md 역할 정리

**파일:**
- `docs/01_plugin_architecture/CANON.md`
- `docs/02_database_api_stats/CANON.md`
- `docs/04_combat_weapon_skills/CANON.md`
- `docs/05_island_farm_system/CANON.md`
- `docs/06_fields_bosses/CANON.md`

**변경:** TODO/잔존 충돌 문구를 제거하고, 공식 기준·참조 우선순위·충돌 처리 방식만 남김. 잘못된 섹션 번호와 이동 예정 문구도 정리.  
**이유:** CANON 문서가 작업 체크리스트가 아니라 현재 공식 기준 역할을 해야 함.  
**근거:** `final_master_plan.md`의 "공식 문서 구조".

---

### DL-011 Status 태그와 참조 경로 통일

**파일:** `docs/**/*.md`, `CLAUDE.md`  
**변경:** 주요 문서 상단 Status를 `[STATUS: CANON|REFERENCE|DRAFT|ARCHIVED]` 형식으로 통일. 구 GUI 경로를 `docs/08_resourcepack_pipeline/`로 수정. 존재하지 않는 경제 검토 문서 참조 제거.  
**이유:** 문서 역할과 현재 docs 구조를 명확히 하고 깨진 참조를 제거.  
**근거:** docs 리빌드 검수 기준 — Status 태그 누락, CLAUDE/AGENTS 참조 경로 정합성.

---

### DL-012 final_master_plan 재축약

**파일:** `docs/final_master_plan.md`  
**변경:** 구현 절차, 상세 수치표, 슬롯 배치, API 상세, 관리자 커맨드 상세를 하위 문서로 위임하고 원칙/방향성/도메인 진입점 중심으로 재구성.  
**이유:** `final_master_plan.md`가 너무 많은 세부사항을 보유하면 각 CANON.md와 하위 문서가 공식 기준으로 기능하기 어렵다.  
**근거:** `docs/_archive/master_plan_content_audit.md`

---

### DL-013 AI orchestra workflow 명령어 통일

**파일:**
- `CLAUDE.md`
- `AGENTS.md`
- `scripts/orchestra.sh`

**변경:** AI 작업 루프의 공식 명령어를 `orc`로 통일하고, main → review는 `orc handoff-main` / `orc to-review`, review → main은 `orc handoff-review` / `orc to-master` 흐름으로 정리. `handoff-*` 명령은 현재 worktree 변경사항을 `add -A` 후 commit하고 대응하는 동기화 명령을 실행하는 것으로 정의. `to-review` / `to-master`는 양쪽 worktree가 dirty 상태이면 중단하는 안전 동기화 명령으로 명시.
**이유:** Claude(main/master)와 Codex(review/codex-review)의 역할을 분리하면서도 handoff 명령과 legacy alias 설명이 혼동되지 않도록 공식 workflow를 단일 명령 체계로 정리하기 위함.
**근거:** `CLAUDE.md`와 `AGENTS.md`의 작업 루프 명령어 섹션, `scripts/orchestra.sh`

---

## 2026-05-22 — 미확정 M-tag 확정 처리

### DL-014 M-6 확정 — 강화 흔적 3종 아이템 정의·수급 경로

**파일:**
- `docs/final_master_plan.md` (§13 미확정 항목에서 제거)
- `docs/02_database_api_stats/CANON.md` (흔적 수급 경로 추가)
- `docs/05_island_farm_system/CANON.md` (공방 가공기 제작 대상 명시)

**변경:**
- 별의 흔적 / 달의 흔적 / 태양의 흔적 = **강화 성공률 보정 아이템**으로 정의
- 수급 경로: **영지 공방 가공기에서 제작 가능** (레시피 미확정 — 사용자 확정 필요)
- economy_numbers_v2.md 강화표의 "강화 흔적 (선택)" 열과 정합성 확보

**이유:** 21강 이상 강화 진행 시 흔적 소모가 필수인데 수급 경로가 미확정이면 강화 진행 자체가 막히는 블로커였음. M-2 서브 에이전트 분석에서도 "성공률 수치보다 흔적 수급이 더 큰 병목"으로 지적됨.  
**근거:** 사용자 확인 (2026-05-22)

---

### DL-015 M-4 확정 — 전승권 비용

**파일:**
- `docs/final_master_plan.md` (§13 미확정 항목에서 제거)
- `docs/02_database_api_stats/CANON.md` (전승권 비용 추가)

**변경:**
- 기본 전승: **0G (무료)**
- 등급전승권: **100,000G**
- 세부스탯전승권: **100,000G**

**이유:** 오픈 후 7~14일차 시세 확인 후 결정 예정이었으나 기본 전승 무료 + 등급/세부스탯 전승권 각 100,000G로 사전 확정.  
**근거:** 사용자 확인 (2026-05-22)

---

## 2026-05-22 — PHASE 7 큐브 비용 하향 및 문서 정리

### DL-016 큐브 1회 비용 5,000G → 500G

**파일:**
- `docs/02_database_api_stats/CANON.md`
- `docs/02_database_api_stats/economy_numbers_v2.md`

**변경:**
- 큐브 1회 사용 골드 비용: **5,000G → 500G**
- 선발대 기준 일 150회 사용 (75,000G/일 소모) 기준으로 추정치 재산정

**이유:** 큐브를 일상적인 골드 소모처로 활용. 5,000G는 선발대도 하루 0.4회 수준으로 잠재 성장 체감이 너무 낮았음.  
**근거:** 사용자 확인 (2026-05-22)

---

### DL-017 enhancement_droprate_v1.md 아카이브

**파일:**
- `docs/02_database_api_stats/enhancement_droprate_v1.md` → `docs/_archive/enhancement_droprate_v1.md`
- `docs/_archive/README.md` (PHASE 6 항목 추가)

**변경:** enhancement_droprate_v1.md를 _archive로 이동

**이유:** 강화석 파편 시스템 기반 드랍률 계산 문서. 파편 시스템 폐지(강화석 직접 드랍/소모)로 계산 방식 전체 무효.  
**근거:** 강화석 파편 시스템 폐지 확정 (DL-005 참조)

---

### DL-018 economy_numbers_v2.md 영지·공방 섹션 제거

**파일:** `docs/02_database_api_stats/economy_numbers_v2.md`

**변경:**
- §2 영지 시설 슬롯 구성, §3 공방 가공기 레시피, §4 공방 대기열 한도 전체 삭제
- 해당 내용은 `05_island_farm_system/island_system_design.md`, `05_island_farm_system/workshop_crafting_spec.md`가 권위 있는 최신 버전으로 유지
- economy_numbers_v2는 강화·큐브·경제 분석 전용 문서로 범위 축소
- §1 작위 구매 비용 재료 컬럼을 전장의 파편 기반으로 수정 (island_system_design.md 기준)
- 폐지된 마력 결정, 자동재배기, 전투 식량, 구버전 공명 추출기 잔존 참조 일괄 제거

**이유:** 같은 내용이 두 문서에 있되 economy 문서가 구버전이면 반드시 혼동 발생. AI 에이전트가 구버전을 현재 기준으로 읽는 오류 재발 방지.  
**근거:** 사용자 지시 (2026-05-22)

---

## 2026-05-22 — PHASE 1 문서 통폐합 기준 확정

### DL-019 문서 정리 기준과 폐기 설계 목록 확정

**파일:**
- `docs/final_master_plan.md`
- `docs/02_database_api_stats/CANON.md`
- `docs/04_combat_weapon_skills/CANON.md`
- `docs/05_island_farm_system/CANON.md`
- `docs/06_fields_bosses/CANON.md`
- `docs/08_resourcepack_pipeline/index.md`

**변경:**
- 문서 정리 기준을 `final_master_plan.md` / 각 `CANON.md` / 최신 DL 항목 우선으로 고정
- archive 문서는 과거 맥락 추적용이며 현재 구현·수치·GUI 기준으로 사용하지 않는다고 명시
- 폐기된 구버전 설계 목록을 명시: 마력/발전기, 강화석 파편, 큐브 5,000G, 전승권 5,000G/50,000G, 망치 표시명, 도감/컬렉션 1차 포함, 시즌보스 주간 입장 제한, 고퀄 외부 보스 모델 1차 적용
- archive 이동된 `enhancement_droprate_v1.md` 활성 참조를 제거하고, 드랍률 재정리 기준을 `drop_tables_v1.md`로 이동
- 리소스팩 1차 우선순위에서 도감 GUI를 제외 항목으로 정리

**이유:** 통폐합/archive 전환 전에 어떤 문서가 현재 기준이고 어떤 구버전 설계를 폐기해야 하는지 먼저 고정해야 후속 archive 이동 시 기준 충돌이 재발하지 않음.
**근거:** 2026-05-22 문서 모순 리뷰 결과 및 사용자 지시

---

## 2026-05-22 — PHASE 2 구버전 GUI 문서 archive

### DL-020 구버전 GUI 상세 문서 3종 archive

**파일:**
- `docs/08_resourcepack_pipeline/gui_functional_specs.md` → `docs/_archive/gui_functional_specs.md`
- `docs/08_resourcepack_pipeline/gui_territory_status.md` → `docs/_archive/gui_territory_status.md`
- `docs/08_resourcepack_pipeline/gui_boss_info.md` → `docs/_archive/gui_boss_info.md`
- `docs/08_resourcepack_pipeline/index.md`
- `docs/08_resourcepack_pipeline/gui_todo_list.md`
- `docs/05_island_farm_system/CANON.md`
- `docs/05_island_farm_system/index.md`
- `docs/04_combat_weapon_skills/index.md`
- `docs/_archive/README.md`

**변경:**
- 마력/발전기, 강화석 파편, 큐브 5,000G, 전승권 구가격, 구 보스 보상 기준이 섞인 GUI 상세 문서 3종을 archive로 이동
- 활성 문서의 해당 링크를 제거하고 "현행 기준 재작성 필요" 상태로 표시
- archive README에 이동 이유와 대체 기준 문서를 기록

**이유:** 구버전 GUI 문서가 현재 CANON보다 상세해서 AI 에이전트가 구버전 수치와 흐름을 구현 기준으로 오인할 위험이 큼.
**근거:** PHASE 1 폐기 설계 목록, 2026-05-22 문서 모순 리뷰 결과

---

## 2026-05-22 — PHASE 3 활성 상세 문서 통폐합

### DL-021 경제·성장·드랍·GUI 허브 활성 문서 기준 정리

**파일:**
- `docs/02_database_api_stats/economy_numbers_v2.md`
- `docs/02_database_api_stats/equipment_growth_spec.md`
- `docs/02_database_api_stats/potential_options_v1.md`
- `docs/06_fields_bosses/drop_tables_v1.md`
- `docs/08_resourcepack_pipeline/gui_hub_structure.md`
- `docs/08_resourcepack_pipeline/gui_shop.md`

**변경:**
- `economy_numbers_v2.md`는 강화 비용·큐브 비용·골드 경제 지표 문서로 범위를 좁히고, 잠재 확률 세부 기준은 `potential_options_v1.md`로 위임
- 구 방어구 50% 강화석 비율, 강화석 파편 경매 표현, 자동심기 300,000G 잔재를 현재 기준으로 정리
- `equipment_growth_spec.md`의 큐브 비용을 500G로, 전승권 비용을 DL-015 기준으로 정리
- `drop_tables_v1.md`의 주간 첫 클리어/반복 보상 구조를 구버전 초안으로 표시하고, 큐브 비용과 균열왕 심장 용도에서 구버전 표기를 제거
- GUI 허브/상점 문서에서 마력 잔량, 강화석 제작, 강화석 파편, 자동심기 50,000G 잔재를 현재 기준으로 정리

**이유:** archive하지 않고 유지할 활성 상세 문서가 CANON보다 오래된 수치·흐름을 포함하면 통폐합 후에도 구현 기준 혼선이 계속 발생함.
**근거:** PHASE 1 폐기 설계 목록, DL-015, DL-016, DL-018

---

## 2026-05-22 — PHASE 4 구현 레퍼런스 재축약

### DL-022 implementation_reference.md 장문 구버전 archive

**파일:**
- `docs/01_plugin_architecture/implementation_reference.md` → `docs/_archive/implementation_reference_legacy.md`
- `docs/01_plugin_architecture/implementation_reference.md` (현재 기준 진입점으로 재작성)
- `docs/_archive/README.md`

**변경:**
- 1,500줄 이상의 장문 구현 레퍼런스를 archive로 이동
- 활성 `implementation_reference.md`는 현재 CANON 기준, 구현 상세 진입점, 확정 구현 기준, 금지된 구버전 기준만 담는 얇은 문서로 재작성
- archive README에 이동 이유와 대체 기준 문서를 기록

**이유:** 기존 구현 레퍼런스에 큐브 5,000G, 전승권 구가격, M-tag 미정, 구 GUI/보상 기준이 섞여 있어 AI 에이전트가 현재 CANON보다 구버전 구현 메모를 우선할 위험이 큼.
**근거:** PHASE 1 폐기 설계 목록, PHASE 3 활성 상세 문서 정리 결과

---

## 2026-05-22 — PHASE 5 활성 문서 잔여 구버전 키워드 정리

### DL-024 강화 흔적 3종 제작 레시피 및 효과 확정

**파일:**
- `docs/05_island_farm_system/workshop_crafting_spec.md` (§9 레시피 전면 교체, `tab_trace` 신설)
- `docs/05_island_farm_system/CANON.md` (레시피 확정 명시)
- `docs/idea_inbox.md` (INBOX-001 PROMOTED)

**변경:**
- 별의 흔적 (`mat_trace_star`): 강화 성공률 **+20%p**, 마도합금×10 + 다이아블럭×2 + 에메랄드블럭×2
- 달의 흔적 (`mat_trace_moon`): 강화 성공률 **+30%p**, 마도합금×20 + 다이아블럭×4 + 에메랄드블럭×4
- 태양의 흔적 (`mat_trace_sun`): 강화 성공률 **+50%p**, 마도합금×40 + 다이아블럭×8 + 에메랄드블럭×8
- 공방 탭 `tab_trace` 신설 (기존 §9 "공방 제작 탭 내 포함" 표현 교체)
- 마도합금(`mat_mado_alloy`) 용도: **DL-034에서 확정** — 강화 흔적 3종 + 고대흔적 4종 핵심 재료. DL-034 전 구 레시피의 광부의 정수는 현행 기준이 아니다.

**이유:** 강화 흔적 레시피가 INBOX-001로만 남아 있어 구현 블로커. 사용자 확정으로 레시피와 효과 수치 동시 확정.  
**근거:** 사용자 확인 (2026-05-22)

---

### DL-023 활성 문서 최종 스캔 반영

**파일:**
- `docs/03_discord_onboarding_bot/index.md`
- `docs/07_boss_pattern_modules/season_boss_patterns.md`
- `docs/01_plugin_architecture/empire_rpg_design_intent.md`
- `docs/01_plugin_architecture/empire_rpg_module_design.md`
- `docs/04_combat_weapon_skills/combat_balance_v2.md`
- `docs/08_resourcepack_pipeline/gui_hud_spec.md`
- `docs/02_database_api_stats/index.md`

**변경:**
- Discord 알림 목록에서 폐지된 마력 과부하 제거
- 보스 패턴/HUD/모듈 설계의 사용자 노출 무기명 `망치`를 `도끼`로 정리
- 강화석 획득량 설명에서 강화석 파편 표기를 제거
- DB/API 통계 개요에서 1차 시즌 제외 대상인 컬렉션 관련 테이블/API/통계 목표 제거
- Citizens/외부 보스 모델 설명을 현재 CANON 기준으로 정리

**이유:** Phase 1~4 이후에도 활성 문서 일부가 구버전 키워드를 현재 구현 기준처럼 포함하고 있었음.
**근거:** Phase 5 활성 문서 스캔 결과

---

## 2026-05-23 — 사용자 확정 반영

### DL-025 강화석 DB 재화 및 `axe` 내부 식별자 확정

**파일:**
- `docs/02_database_api_stats/CANON.md`
- `docs/02_database_api_stats/economy_numbers_v2.md`
- `docs/02_database_api_stats/equipment_growth_spec.md`
- `docs/06_fields_bosses/CANON.md`
- `docs/06_fields_bosses/drop_tables_v1.md`
- `docs/final_master_plan.md`
- `docs/01_plugin_architecture/implementation_reference.md`
- `docs/04_combat_weapon_skills/CANON.md`
- `docs/04_combat_weapon_skills/weapon_skills_v1.md`
- `docs/04_combat_weapon_skills/index.md`
- `docs/08_resourcepack_pipeline/index.md`
- `docs/08_resourcepack_pipeline/gui_todo_list.md`
- `docs/08_resourcepack_pipeline/gui_boss_info.md`

**변경:**
- 강화석은 실물 아이템 드랍이 아니라 DB 가상 재화로 처치 시 직접 적립/소모하는 기준으로 통일
- `mat_stone_enhance` 실물 드랍/소모 표현 제거
- 무기 내부 식별자 `hammer` 유지 방침을 폐기하고 `axe`로 전환
- GUI 세부 설계 상태를 장비 계열 작성완료, 영지·보스·필드 계열 재작성필요로 정리

**이유:** 사용자 확정에 따라 강화석 데이터 모델과 무기 내부 식별자 기준을 명확히 하고, 진행 중인 GUI 상세 설계 상태를 실제 작업 상태와 일치시킴.
**근거:** 사용자 확인 (2026-05-23)

---

### DL-027 GUI 완료 상태 및 공식 배경 4종 확정

**파일:**
- `docs/08_resourcepack_pipeline/index.md`
- `docs/08_resourcepack_pipeline/gui_todo_list.md`
- `docs/08_resourcepack_pipeline/gui_bitmap_spec.md`
- `docs/08_resourcepack_pipeline/gui_png_make_guide.md`
- `docs/08_resourcepack_pipeline/gui_hub_structure.md`

**변경:**
- 최신 커밋 기준 모든 GUI 설정이 완료된 상태로 정리
- 공식 GUI 배경을 `menu_main.png`, `menu_equipment.png`, `menu_territory.png`, `menu_boss.png` 4개로 확정
- `menu_hub_*` 27슬롯/서브허브 전용 배경은 공식 사용 대상에서 제외
- GUI 글리프 문자를 실제 `gui.json` 기준(``, ``, ``, ``)으로 통일

**이유:** GUI 설정 완료 상태와 공식 배경 4개 사용 방침을 문서 전체에 일관되게 반영하기 위함.
**근거:** 사용자 확인 (2026-05-23)

---

### DL-067 플레이어 레벨링 경험치 곡선·몹 EXP·보스 EXP 재확정

**파일:**
- `docs/04_combat_weapon_skills/level_stat_system_v1.md` (§5 전체 대체)
- `custom-plugins/empire-rpg/src/main/java/com/poro/empire/leveling/PlayerLevelingService.java` (신규)
- `custom-plugins/empire-rpg/src/main/java/com/poro/empire/listener/FieldDropListener.java` (EXP 연동)

**변경:**
- 경험치 곡선: `550 × n^1.5` (2026-05-16 초안) → `round(800 × 1.1^(n-1))` (기하급수, 2026-05-26 확정)
- 몹 EXP: F1 50/100 → 10/25, F2 70/140 → 20/50, F3 100/200 → 30/75, F4 140/275 → 45/110, F5 200/400 → 60/150
- 보스 처치 EXP: **없음** (재화·흔적만 지급). 초안의 "보스 처치 시 보너스 경험치" 항목 삭제.
- 레벨 목표: "80 사실상 불가" → 선발대(하루 8h) 83~87 목표, 일반 ~60, 활성 ~70, 상위 ~78 로 재설정

**이유:**
- 초안 멱함수(`550 × n^1.5`)는 고정 킬레이트(25/min) 기준 선발대가 45일에 100레벨 초과 → 설계 한계 역할 불가.
- 기하급수 곡선은 레벨 85+ 진입 비용을 지수 폭증시켜 선발대의 80~85 목표를 자연 차단.
- 구 문서의 몹 EXP(200/kill@F5)는 "80 사실상 불가" 주장과 내부적으로 모순됨.
- 보스 EXP는 수급 예측이 어렵고 경쟁 요소를 과도하게 자극할 수 있어 배제.

**근거:** 사용자 구두 확정 2026-05-26, Codex 리뷰(P1) 지적 → 문서 갱신으로 해소.

---

### DL-068 장비의 흔적 드랍 등급 5종 확장

**파일:**
- `custom-plugins/empire-rpg/src/main/java/com/poro/empire/listener/FieldDropListener.java`
- `custom-plugins/empire-rpg/src/main/java/com/poro/empire/boss/engine/BossRewardService.java`
- `docs/06_fields_bosses/drop_tables_v1.md`
- `docs/04_combat_weapon_skills/item_grade_substat_v1.md` (§2 표 "시즌보스 1~3" → "1~6")

**변경:**
- 초기 구현: 정예몹/보스 모두 broken/faded/glowing 3등급만 드랍.
- 확장 후: 필드4/5 정예몹에 radiant(유니크)·brilliant(레전더리) 추가. 필드보스 필드4/5도 동일 확장. 시즌보스 1~6 전원 5등급 분포, 최종 3보스(균열왕·이중체·주시자)는 상위 분포 별도 적용.
- 등급 분포 수치: `item_grade_substat_v1.md §2` 표 기준 (상세 확률은 `FieldDropListener.randomTraceId()`, `BossRewardService.pickTrace()` 구현 참조).

**이유:**
- 상위 필드/보스 콘텐츠에 차별화된 보상 가치가 없으면 필드4/5 진입 동기 부족.
- `item_grade_substat_v1.md §2`는 처음부터 5등급 분포 체계를 설계했으나 초기 구현에서 3등급만 반영됨 — 이번 작업에서 문서 설계 의도 구현으로 정합.

**근거:** 사용자 구두 확정 2026-05-26 (Codex 리뷰 P2 지적 → 구현 확장 선택).
