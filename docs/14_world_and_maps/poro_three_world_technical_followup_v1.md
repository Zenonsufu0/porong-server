# 포로 서버 3월드 기술 분해 후속 자문 v1

> **상태 (2026-04-19 사용자 결정)**: **G-1~G-5 권장안 전체 수용 확정**. FAWE `softdepend` + stale 30분 + heartbeat 10s·TTL 65s + Sponge `.schem` + 시즌 T-10분 batch. 원안 20 md → 22 md로 흡수.
> 문서 버전: 2026-04-19 1차 (implementation-reviewer)
> 선행 문서: `poro_three_world_technical_breakdown_draft.md` (10 PR 분해, 20 md)
> 상위 기획: `poro_three_world_transition_design_draft.md`
> 본 문서는 선행 드래프트가 남긴 **오픈 질문 5건**(FAWE 도입·stale txn 만료·영지 락 heartbeat·스키매틱 포맷·시즌 경계 배치 타이밍)을 확정 권고안 수준까지 좁히는 후속 자문이다.
> 본 문서는 구현 계획 흐름("목표 → 선행조건 → 작업 분해 → 리스크 → 테스트 포인트 → 1차 버전")을 그대로 따른다.

---

## 1. 구현 목표

- 선행 드래프트 **오픈 질문 5건에 각각 권장안 1개씩 확정**하고, 선택 근거·영향 범위·1차 구현 지침을 한 문서에서 조망한다.
- 5건 중 **가장 파급력 큰 1건(FAWE 도입 여부)**이 나머지 4건에 어떻게 영향을 주는지 명시한다.
- 새 시스템을 추가 제안하지 않는다. 기존 10 PR(PR-W0~PR-W9) 안에 **판단 분기·설정 상수·안전장치**만 꽂는 수준으로 정리한다.

---

## 2. 선행조건

- 선행 드래프트의 10 PR 분해 구조가 그대로 유효하다(본 문서는 PR을 추가·삭제하지 않는다).
- 플래그 저장소 PR0/PR1(`sqlite-jdbc`) 머지 완료 전제.
- `build.gradle.kts`·`plugin.yml` 변경이 허용된다(본 문서의 1·4건 결정이 직접 수정한다).
- Paper 1.21.10, Java 21, 단일 인스턴스 단일 노드 운영 전제는 유지한다.
- 운영팀 공지 채널(디스코드)·인게임 브로드캐스트 API가 이미 존재한다고 가정하지 않는다 — 5건에서 필요한 최소 알림은 `Bukkit.broadcast` + 디스코드 봇 기존 공지 채널 재사용으로 제한한다.

---

## 3. 작업 분해 — 5건 권장안

본 절은 5건 각각을 "선택지 비교 → 권장안 → 구현 영향"으로 정리한다. 5건 사이 의존 관계가 있으므로 **1번(FAWE)을 먼저 확정**해야 2·4번 세부가 고정된다.

### 3.1 질문 1 — FAWE 도입 확정

**권장: FAWE(FastAsyncWorldEdit) 도입. 단, PR-W3에 동기 스탬핑 폴백 경로 유지.**

| 선택지 | Lv5 6×6 스탬프 | 메인 스레드 위험 | 의존 부담 | Paper 1.21.10 지원 | 권장 |
|---|---|---|---|---|---|
| A. FAWE | 비동기, TPS 안전 | 낮음 | FAWE 플러그인 1개 추가 | 2026-04 시점 1.21.x 대응 릴리스 존재 | 본 후속 권장 |
| B. WorldEdit 순정 | 동기, 100ms+ 점유 | 중간 | WorldEdit 1개 | 안정 지원 | Lv1~2 한정 폴백 |
| C. Paper Block API 직접 | 동기, `setBlock` 셀 단위 수천~수만 호출 | 높음 (TPS 경고 확정) | 없음 | 항상 가능 | 부적합 |

선택 근거 요약:
- Lv5 6×6 청크 = 약 96×96×Y 블록. 동기 루프는 TPS 쇼크 필연. **비동기 붙여넣기는 FAWE 전용 강점**.
- Multiverse-Core를 거절한 선행 드래프트 결정과 충돌하지 않음(FAWE는 월드 관리 대체재가 아니라 청크 편집 엔진).
- Paper `libraries:` manifest로 런타임 로드 가능한 형태는 FAWE가 **공식 제공 루트가 아니다** — FAWE는 **플러그인(`.jar` 단독)** 으로 `plugins/` 폴더에 배치하고, EmpireRPG는 `softdepend: FastAsyncWorldEdit` + `build.gradle.kts` `compileOnly`로 API만 참조한다. 플래그 저장소 PR0의 `sqlite-jdbc` 런타임 로드(`libraries:`) 방식과는 경로가 다르다는 점을 혼동하지 않도록 주의.
- 라이선스: FAWE는 LGPL-3.0. EmpireRPG가 API만 호출하고 재배포하지 않으므로 **내부 운영 사용에는 추가 의무 없음**. 코드 수정·포크 배포 시에만 LGPL 고지 의무 발생.
- 운영 부담: FAWE는 자체 업데이트 주기가 있고, Paper major 업데이트 직후 잠시 비호환 기간이 생길 수 있다. → **Lv1(2×2 청크) 동기 폴백**을 항상 유지해 FAWE 장애 시 최소 기능 유지.

구현 영향:
- `build.gradle.kts`: `compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core:<2.x 안정판>")` 또는 WorldEdit API 의존(`compileOnly("com.sk89q.worldedit:worldedit-bukkit:<7.3.x>")`) 추가. 정확한 버전은 PR-W3 착수 시 FAWE 릴리스 페이지에서 Paper 1.21.10 호환 태그로 확정.
- `plugin.yml`: `softdepend: [FastAsyncWorldEdit, WorldGuard]`로 2개 추가(WorldGuard는 선행 드래프트 PR-W8에서 이미 예정).
- `EstateSchematicStamper` 구현에 `FaweAsyncStamper`(1차)·`SyncFallbackStamper`(장애 폴백) 2개 전략. 설정 토글 `estate.schematic.fawe_enabled` (기본 true).

파급 영향(2·4번으로 전달):
- FAWE 채택 → **4번 스키매틱 포맷 자동으로 `.schem` 고정 유리**(FAWE가 `.schem` sponge 포맷을 1급 지원).
- FAWE 채택 → Lv5 스탬프 소요 시간이 수 초 단위로 들어오므로 **2번 stale txn 만료 주기의 하한**이 "스탬프 최대 시간 + 여유"로 제약된다(섹션 3.2 참고).

### 3.2 질문 2 — stale `transfer_txn` 자동 만료 주기

**권장: 30분. 단, 만료 시점에는 자동 "출발 좌표 복원"만 수행하고 인벤토리·소유물은 건드리지 않는다.**

| 선택지 | 오인 만료 위험 | 운영 개입 지연 | 영지 락(60초)과 정합 | 권장 |
|---|---|---|---|---|
| 5분 | 높음 — Lv5 스탬프 중 크래시 재기동 지연·대기 환경에서 정상 txn이 만료될 수 있음 | 빠름 | 50배 격차, 감각상 위화감 | 부적합 |
| 10분 | 중간 | 중간 | 10배 격차 | 보조 선택지 |
| **30분** | **낮음** | **운영이 로그 보고 대응 가능한 현실적 창** | 30배 격차, 영지 락은 "세션 내 실시간", txn은 "크래시 복구 잠금"으로 성격 다름 | **권장** |
| 1시간 | 매우 낮음 | 너무 늦음 — 플레이어 재접속 시 이미 복원됐어야 할 케이스가 수동 대기 상태로 남음 | 60배 격차 | 과보수 |

선택 근거 요약:
- `transfer_txn`의 stale은 "서버 크래시 후 재기동했는데 해당 플레이어가 오랫동안 다시 접속하지 않은" 상황에서만 관측된다. 정상 플레이어 접속 시는 `PlayerJoinEvent`에서 1틱 내 복구(선행 드래프트 3.7).
- 30분은 **영지 락과 서로 다른 축**임을 명시: 영지 락은 "세션 점유 실시간 판정", txn stale은 "DB 행의 자동 청소 기준". 두 수치 격차가 있어도 무방하다.
- 30일(선행 드래프트 오픈 질문의 극단값) 같은 장기 보관은 **수동 운영 복구 전용 플래그**로 별도 마킹한다(아래 구현 영향 참고).

만료 시 복구 동작:
1. `transfer_txn.teleporting=1`이고 `started_at < now()-1800s` 조건 만족 → 해당 행의 `src_world`·`src_x`·`src_y`·`src_z`만 **보존**하고, `teleporting` 플래그를 `2`(=EXPIRED) 상태로 전이.
2. 해당 플레이어가 이후 접속하면 `PlayerJoinEvent`에서 `teleporting=2` 상태를 감지해 **수도 기본 스폰으로 폴백**(선행 드래프트 3.7 규칙 3과 같은 경로) + 디스코드 알림.
3. 인벤토리·버프 복원은 시도하지 않음(선행 드래프트 기조 유지).

구현 영향:
- 스케줄러: 플러그인 `onEnable()` + 5분 간격 Bukkit async 스케줄러가 `SELECT ... WHERE teleporting=1 AND started_at < ?` 실행.
- `transfer_txn.teleporting` 컬럼을 `INTEGER` 그대로 두되 값 집합을 `0=IDLE / 1=PENDING / 2=EXPIRED`로 확장(선행 드래프트 스키마 DDL 수정 불필요, 값 의미만 확장).
- 설정 `transfer.stale_threshold_seconds` 기본 1800.

파급 영향:
- 1번 FAWE가 비동기이므로 스탬프 자체는 주 스레드 점유를 만들지 않고, stale 판단이 "스탬프 진행 중"과 섞이지 않도록 **txn과 스탬프를 별 단계로 분리**(txn = 이관 좌표 이동만, 스탬프 = 영지 최초 진입 시 1회). 30분 창은 이 분리를 이미 전제.

### 3.3 질문 3 — 영지 점유 락 heartbeat 주기 적정성

**권장: heartbeat 10초 / stale 60초(선행 드래프트 원안 유지). 단, 재연결 대기 중 5초 허용창을 추가.**

| 주기 | 허용 누락 횟수 (stale=6×주기) | DB 쓰기 부담 (100명/시간) | TPS 저하 시 내결함성 | 권장 |
|---|---|---|---|---|
| 5초 | 12회 | 720k writes/h 상한, SQLite 경합 위험 | 민감 — 프리즈 1초에도 bounce | 부적합 |
| **10초** | **6회** | **360k writes/h, SQLite 여유** | **TPS 5까지 낙하 1초 프리즈 견딤** | **권장** |
| 15초 | 4회 | 240k | 견고하나 크래시 후 최대 90초 락 잔존 | 보조 |
| 30초 | 2회 | 120k | 누락 1회가 즉시 stale → 오락가락 | 부적합 |

선택 근거 요약:
- 본 서버 동접 목표(100~300명) × 영지 체류 30% = 시간당 약 100k~300k heartbeat. SQLite WAL 모드에서 충분 — 축 1 부하 자문(청크 부하 v1)과 정합.
- Paper TPS 일시 저하(예: GC 200ms)에도 6회 누락 창 안이면 복구된다.
- 네트워크 프리즈 3~5초 대응을 위해 **heartbeat는 10초 주기지만, `expires_at` 계산은 "last_heartbeat + 65초"로 +5초 허용창**을 두어 단발성 지연으로 오해제되는 케이스를 차단.

구현 영향:
- `EstateOccupancyLockService`의 상수 `HEARTBEAT_PERIOD_TICKS=200`(10s), `LOCK_TTL_SECONDS=65`(60+5 허용창).
- 설정 키 `estate.lock.heartbeat_seconds` / `estate.lock.ttl_seconds` 노출해 운영 튜닝 여지만 남기고 기본값은 위 수치 고정.
- 크로스 기기 kick 콜백(선행 드래프트 3.5)에서 "kick된 세션 보유 락 즉시 해제"가 성공하지 못해도 최대 65초 내 자연 해제 보장.

파급 영향:
- FAWE(1번) 채택으로 비동기 스탬핑 중에도 주 스레드 heartbeat가 끊기지 않으므로, heartbeat 10초 주기가 스탬프 소요(수 초)와 충돌하지 않는다.

### 3.4 질문 4 — 스키매틱 저장 포맷

**권장: `.schem`(Sponge schematic format) 고정.**

| 포맷 | 로딩 속도 | 파일 크기 | 편집 도구 | FAWE 지원 | 권장 |
|---|---|---|---|---|---|
| **`.schem` (Sponge)** | 빠름 (FAWE 1급) | 작음 (gzip NBT + 팔레트 압축) | WorldEdit·FAWE·VoxelSniper 전 범위 | 1급 지원 | **권장** |
| `.nbt` (Paper 구조 블록) | 중간 | 중간 | Paper 구조 블록 UI(게임 내), MCEdit 일부 | 직접 지원 아님, 변환 필요 | 부적합 |
| JSON + 블록 ID 배열 자체 포맷 | 가장 빠름(파싱 단순)·가장 느림(IO 커짐) 양극단 | 큼 (텍스트) | 자체 도구 필요 | 미지원 | 운영 부담 큼, 부적합 |

선택 근거 요약:
- FAWE 채택(1번)과 자동 정합. `.schematic`(legacy MCEdit)은 1.13+ 블록 상태 표현 한계로 부적합.
- Lv1~5 총 5장은 기획·운영팀이 FAWE/WorldEdit CLI `//schem save` 한 줄로 산출 가능 → 제작 파이프라인 단순.
- Paper 구조 블록(`.nbt`)은 게임 내 편집 편의가 있으나 FAWE 비동기 붙여넣기 API에서 직접 소비 불가 — 변환 단계 추가는 운영 리스크.

구현 영향:
- 디렉터리 고정: `plugins/EmpireRPG/schematics/estate_lv{1..5}.schem`.
- `SchematicFileResolver`가 `{level}` 치환 후 파일 존재 확인 실패 시 **Lv1 폴백**(더미 스키매틱 1장으로 1차 릴리스 가능하게).
- 버전 관리: 스키매틱 변경 이력은 `git lfs` 대상이 아니라 **운영 산출물 저장소** 별도 경로(추후 결정). 본 에픽 범위 외.

파급 영향:
- 1번이 WorldEdit 순정으로 후퇴해도 `.schem`은 WorldEdit 7.3+ 에서도 표준이므로 **포맷 권장안은 불변**.

### 3.5 질문 5 — 시즌 경계 일괄 재활성화 타이밍

**권장: 시즌 시작 직전(점검 종료 직후, 신 시즌 월드 개방 T-10분)에 단일 batch job으로 일괄 재활성화.**

| 타이밍 | 운영 혼선 | 유저 통지 용이성 | batch 실패 시 영향 | 권장 |
|---|---|---|---|---|
| A. 시즌 종료 직후(구 시즌 점검 중) | 낮음 | 중간(시즌 리셋 공지에 포함) | 빈 시즌 시작까지 재시도 창 있음 | 보조 선택지 |
| **B. 시즌 시작 직전(신 시즌 오픈 T-10분)** | **낮음** | **시즌 오프닝 공지와 함께 노출 가능** | **신 시즌 오픈 직전 실패 시 오픈 지연** — 단, 사전 dry-run으로 방지 | **권장** |
| C. 시즌 시작 직후 공지 후 24h 점진 | 높음(시즌 초반 운영 부하 집중) | 어색 — 유저 체감 "왜 지금?" | 점진 실패 시 부분 회수 상태 잔존 | 부적합 |

선택 근거 요약:
- 선행 드래프트의 슬롯 상태 머신(`FREE` / `ASSIGNED` / `RELEASED`)이 명확 — `RELEASED → FREE` 전이는 운영 결정 이벤트이므로 **단일 시점**에 몰아치는 편이 관측·롤백이 쉽다.
- "시즌 시작 직전"은 신 시즌 오픈 공지와 묶을 수 있어 별도 공지 세션 필요 없음. 시즌 시작 직후·24시간 점진은 **유저 체감상 불가해한 타이밍**.
- batch 실패 방어: 시즌 점검 창 중반에 **dry-run**(`SELECT COUNT(*) WHERE state='RELEASED'` + 변경 예정 행 미리보기)을 실행해 운영이 승인 후 실 실행.

구현 영향:
- 배치 진입점: 운영 명령어 `/empire admin season release-slots --dry-run|--commit`(PR-W9 또는 별도 운영 PR에 1일 작업).
- 통지 방식: **디스코드 운영 채널 공지 1건** + 인게임 브로드캐스트는 선택. 재활성화 대상 유저에게 개별 DM은 **범위 외**(계정 삭제·장기 미접속이 대상이므로 DM 도달률 낮음).
- 로그: 변경 전 `estate_slot` 행을 `estate_slot_history`(1차 범위 외) 혹은 최소한 JSON 덤프 파일로 백업.

파급 영향:
- 1·2·3·4번 어느 결정과도 독립. 시즌 경계 이벤트는 `estate_slot` 테이블에만 작용.

---

## 4. 기술 리스크

1. **FAWE 업데이트 지연**: Paper 1.21.11/1.22 이후 FAWE 호환 릴리스 지연 시 스탬핑이 Lv1 폴백에만 의존하게 된다. **완화**: Lv1 동기 폴백 경로를 1차 버전에서 함께 머지(PR-W3 범위).
2. **30분 stale 만료 중 서버 재기동 반복**: 크래시 직후 재기동 → 다시 크래시 반복 시 txn이 30분 내에 여러 번 재시도되어 로그가 더러워진다. **완화**: 재기동 간 cooldown 또는 `started_at` 갱신 금지 규칙으로 원본 시점 보존.
3. **heartbeat 10초 × TPS 저하 복합**: GC 스톱 500ms + 네트워크 1초가 겹치면 `expires_at` 계산이 흔들릴 수 있다. **완화**: heartbeat 기록 시 `System.currentTimeMillis()` 기반(Bukkit tick 무관) + 5초 허용창.
4. **.schem 파일 손상**: 운영 실수로 스키매틱 파일이 깨지면 영지 진입 전체가 막힐 수 있다. **완화**: `EstateSchematicStamper`가 로드 실패 시 **플레이어에게 안내 메시지 + 수도 스폰 유지**(진입 자체를 블록).
5. **시즌 경계 batch 중 온라인 유저 존재**: 운영 실수로 점검 해제 전 batch가 실행되면 온라인 플레이어의 슬롯이 날아갈 수 있다. **완화**: batch 쿼리에 `AND player_count_online = 0` 선행 점검 + `--commit` 2단 확인.
6. **FAWE LGPL 고지 누락**: 본 서버가 EmpireRPG 소스를 공개 배포할 경우 LGPL 고지 의무 발생. **완화**: 공개 배포 시점에 `LICENSE-THIRD-PARTY.md`에 FAWE LGPL 명기(배포 계획 미정이므로 본 문서는 리스크 등록만).

---

## 5. 테스트 포인트

- **FAWE 비동기 스탬프 TPS 측정**: Lv5 스키매틱 10건 동시 붙여넣기 시 TPS 18 이상(선행 드래프트 테스트 포인트 재확인).
- **FAWE 폴백 스위치 테스트**: `estate.schematic.fawe_enabled=false` 설정 후 Lv1 동기 스탬프가 정상 동작하는지.
- **stale txn 30분 만료 유닛 테스트**: `started_at=now-1801s` 주입 → 스케줄러 실행 → `teleporting=2` 전이 확인.
- **stale txn 만료 후 로그인 복원 테스트**: `teleporting=2` 상태에서 로그인 → 수도 스폰 폴백 + 디스코드 알림 호출.
- **영지 락 heartbeat 누락 6회 복구 테스트**: 5회 연속 누락 시 락 유지, 7회 누락 시 자연 해제.
- **영지 락 +5초 허용창 테스트**: heartbeat 간격 14초(허용 상한)에서도 stale 처리 안 되는지.
- **`.schem` 파일 로드 실패 테스트**: 파일 0바이트·형식 오류 시 영지 진입 차단 + 에러 메시지.
- **시즌 경계 batch dry-run 테스트**: `--dry-run`이 실제 행을 변경하지 않는지 + 변경 예정 건수를 정확히 보고하는지.
- **시즌 경계 batch 온라인 차단 테스트**: 온라인 플레이어 1명이라도 남아 있으면 `--commit` 거부.

---

## 6. 1차 버전 권장안

본 후속 자문 5건은 **신규 PR을 추가하지 않는다**. 기존 10 PR 안에 다음 변경만 꽂는다.

| 선행 PR | 본 문서가 지정하는 추가 구현 | 영향 man-day |
|---|---|---|
| PR-W3 (FAWE 의존성 + 스탬퍼) | FAWE `compileOnly` 버전 확정(1.21.10 호환 태그) + Lv1 동기 폴백 + `estate.schematic.fawe_enabled` 설정 + `.schem` 포맷 고정 + 파일 손상 방어 | +0.5 md (원안 2 md → 2.5 md) |
| PR-W4 (TransferService) | `teleporting` 값 집합에 `2=EXPIRED` 추가 + 30분 스케줄러 + 만료 시 디스코드 알림 훅(스텁) | +0.5 md (원안 3 md → 3.5 md) |
| PR-W5 (영지 락 heartbeat) | `LOCK_TTL_SECONDS=65`로 +5초 허용창 반영 + 설정 키 노출 | +0 md (원안 2 md 내 흡수) |
| PR-W9 (명령어 + UX) | 운영 명령어 `/empire admin season release-slots --dry-run|--commit` 추가 + 온라인 차단 가드 + JSON 덤프 백업 | +1 md (원안 2 md → 3 md) |

**총 man-day 영향**: 원안 20 md → 약 **22 md** (+2 md, 10% 증가).

**권장 확정 순서**:
1. FAWE 도입 최종 승인(1번) → PR-W3 착수 전 `build.gradle.kts` 수정 PR 선행 가능.
2. 스키매틱 포맷 `.schem` 확정(4번) → Lv1 더미 스키매틱 1장 제작 의뢰와 동시 착수.
3. stale txn 30분(2번)·영지 락 10초/65초(3번) → 설정 키 상수로만 꽂으므로 PR-W4·PR-W5 범위 내 흡수.
4. 시즌 경계 batch 타이밍(5번) → PR-W9 또는 분리된 운영 PR에서 구현.

---

## 7. 나중으로 미뤄도 되는 것

- **FAWE 자체 포크·재배포**: 내부 운영이므로 불필요. 공개 배포 계획이 생기면 LGPL 고지 보강.
- **`estate_slot_history` 테이블**: 시즌 경계 batch의 변경 이력을 정식 보관. 1차는 JSON 덤프 파일로 갈음.
- **디스코드 DM 개별 통지**: 시즌 경계 재활성화 대상 유저에게 DM 발송. 대상자 대부분이 장기 미접속이므로 도달률 낮아 운영 공지로 대체.
- **heartbeat 주기 운영 A/B 튜닝**: 10초가 목표 동접 기준 충분. 동접 300명 초과 시 5초/15초 A/B로 재측정.
- **스키매틱 CDN 배포**: Lv1~5 `.schem` 파일을 원격 CDN에서 다운로드해 핫스왑. 운영 단순성을 해치므로 1차 범위 외.
- **stale txn 만료 시 인벤토리/버프 복원**: 선행 드래프트 기조 유지. 1차는 좌표만.
- **multi-node 크로스 서버 FAWE 스탬핑 동기화**: 단일 노드 전제 유지. Velocity 확장 시 재검토.
- **시즌 경계 batch 온라인 유저가 영지 안에 있을 때 "킥 후 batch" 플로우**: 권장하지 않음. batch는 점검 종료 직후 온라인 0명 조건에서만 실행.

---

## 오픈 질문

1. **FAWE 정확한 버전 태그 확정**: 1.21.10 호환 안정판 검증은 PR-W3 착수 시점에 FAWE 릴리스 페이지 크로스체크 필요.
2. **스키매틱 변경 이력 저장소 경로**: git lfs·별도 저장소·CDN 중 어느 쪽에 `.schem` 파일을 두고 운영이 편집할지. 본 에픽 범위 외이나 배포 파이프라인 설계 시 같이 결정.
3. **시즌 경계 batch의 "점검 종료 후 오픈 T-10분"** 이 실제 시즌 운영 캘린더의 어느 단계에 고정되는지 운영팀 확정 필요.
4. **stale txn 30분 만료 시 디스코드 알림 채널**: 운영 전용 채널 ID. 기존 공지 봇 재사용 여부.

---

## 상위 문서 참조

- 선행 기술 분해: `poro_three_world_technical_breakdown_draft.md`
- 상위 기획: `poro_three_world_transition_design_draft.md`
- 청크 부하 검토: `../03_life_system_core/poro_estate_chunk_load_review_v1.md`
- 마스터플래닝: `../poro_master_planning.md`
- 빌드 설정: `custom-plugins/empire-rpg/build.gradle.kts`
- 플러그인 선언: `custom-plugins/empire-rpg/src/main/resources/plugin.yml`

---

## 다음 추천 작업

- FAWE 도입 최종 승인(본 문서 권장안 1) → `build.gradle.kts`·`plugin.yml` 선행 PR 착수.
- PR-W3·PR-W4·PR-W5·PR-W9 각 착수 티켓에 본 문서 6절 체크리스트 반영.
- 운영팀과 시즌 경계 batch 타이밍(5번) 최종 스케줄 합의 → `poro_master_planning.md` 시즌 운영 절 업데이트.
- Lv1 더미 `.schem` 제작 의뢰(스키매틱 포맷 확정 후속).
