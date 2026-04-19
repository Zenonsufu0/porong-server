# 플레이어 플래그 저장소 v0.1 오픈 질문 자문 응답 v1

## 문서 목적
`poro_flag_store_v01_consultation_prompts.md`의 4건 오픈 질문에 대한 implementation-reviewer 드래프트 응답. 2026-04-19 사용자 결정("직접 확정 대신 추천 방식 검토") 경로.

## 응답 범위
- 질문 1·2는 코드베이스 실물 확인 기반(클래스·라인·의존성 명시).
- 질문 3·4는 3안 장단점 표 + 권장안 + 리스크 2개.
- 각 권장안은 CLAUDE.md 흐름(`목표 → 선행조건 → 작업 분해 → 리스크 → 테스트 포인트 → 1차 버전`)을 압축 적용.

## 전제 재확인(v0.1 고정 규약)
- SQLite 단일 파일 DB, `FlagValueType` = **BOOL / LONG / STRING 3종**.
- write-through 쓰기, 플레이어 단위 단일 스레드 가정.
- 스프린트 총 공수 10~12 man-day 유지, 범위 확장 금지.
- 기존 포로 서버 기준(T1/T2 장비 2단계·각인 3줄 잠재 등) 변경 없음 — 플래그 저장소는 후속 3시스템의 기반 인프라일 뿐 전투·성장 축 변경 없음.

---

## 질문 1 — `FoundationContext` SQLite DataSource/ConnectionProvider 실제 주입 상태

### 현재 상태 요약(2026-04-19 코드 기준)
`FoundationContext`(`custom-plugins/empire-rpg/src/main/java/com/poro/empire/common/config/FoundationContext.java` line 10~18)는 **`ConnectionProvider` + `TransactionHelper` + `DatabaseBootstrapper` 3종을 record 필드로 이미 노출**하고 있으며, `CommonFoundationBootstrap.bootstrap()`(같은 패키지, line 31~44)이 `SqliteConnectionProvider(config.sqliteJdbcUrl(), ...)` + `JdbcTransactionHelper(...)` + `DatabaseBootstrapper(connectionProvider, new NoopMigrationEntryPoint(), ...)`를 실제 인스턴스화하여 넘기고, `EmpireRPGPlugin.onEnable()`(line 54~64)에서 이 부트스트랩을 호출해 `foundationContext` 필드에 보관한다. **즉 "인터페이스 스텁"이 아니라 실제 주입은 완료된 상태**이다. 단, (1) `SqliteConnectionProvider.getConnection()`(line 22~33)은 매 호출마다 `DriverManager.getConnection(jdbcUrl)`을 여는 **커넥션 풀 없는 직접 오픈 방식**이며 HikariCP는 아직 도입되지 않았고, (2) `build.gradle.kts`에 **SQLite JDBC 드라이버·HikariCP 의존성이 선언되어 있지 않다**(line 22~34에 `paper-api` + `junit`만 존재). 즉 "주입 경로는 있으나 풀이 아니고, 드라이버 명시 누락 리스크가 남아 있다".

### PR3 착수 가능 여부 판정
**가능(단, 2개 선행 정리 필요)** — `ConnectionProvider` + `TransactionHelper`는 PR3가 요구하는 API 표면을 이미 만족. 다만 (a) SQLite JDBC 드라이버 gradle 의존성 추가, (b) 풀 없는 현재 방식이 v0.1 write-through(플레이어 입장·로그아웃마다 커넥션 오픈/클로즈 반복) 부하를 감당 가능한지 간단 확인이 필요하다. 이 둘은 PR1 앞단에서 **0.5 man-day 규모**로 끊어낼 수 있어 스프린트 전체를 블로킹하지 않는다.

### 권장 진행 순서 1개
**PR0(0.5 md) → PR1 → PR2 병렬 → PR3 ...** 단일 경로. PR0은 다음 두 작업만 묶는다.
1. `build.gradle.kts`에 `runtimeOnly("org.xerial:sqlite-jdbc:<LTS버전>")` 1줄 추가 + `onEnable()` 시작 지점에서 `foundationContext.databaseBootstrapper().initialize()` 호출 추가(현재 `DatabaseBootstrapper`가 주입만 되고 `initialize()`가 어디서도 호출되지 않는 점을 함께 해소).
2. `SqliteConnectionProvider` 코멘트에 "v0.1은 풀 미도입, per-call DriverManager.getConnection()이며 write-through 가정 안에서 허용. v0.2에서 HikariCP 교체 예정"을 명시.
3. 이후 PR1(DDL) / PR2(FlagKey 검증기) 병렬 착수.

목표: 현재 주입 구조를 그대로 살리되, 드라이버 의존성과 `initialize()` 호출 누락을 막아 PR1 DDL이 실제로 물리 파일에 반영되도록 한다. 선행조건은 "v0.1은 풀 없이 간다"는 운영 합의. 1차 버전은 per-call 커넥션 + write-through.

### 리스크 2개
- **드라이버 미선언으로 런타임 `ClassNotFoundException`** — 완화: PR0에서 `sqlite-jdbc` 의존성 `runtimeOnly`로 선언, 플러그인 enable 시 간단 ping 쿼리(`SELECT 1`)로 자가 진단.
- **풀 없는 per-call 오픈이 다수 접속자(100~200명) write-through 부하에서 커넥션 재오픈 지연을 누적시킴** — 완화: v0.1 범위는 유지(과한 재설계 금지), 단 `SqliteConnectionProvider`에 단일 커넥션 재사용 옵션을 TODO로 남기고 v0.2 HikariCP 전환 PR을 `poro_master_planning.md` 변경 로그에 예약.

### 충돌 지점
- **없음**. 현재 구조를 그대로 확장하는 방향이며 기존 포로 서버 기준(T1/T2 구조·SQLite v0.1·`FlagValueType` 3종)과 충돌하지 않는다.

---

## 질문 2 — `NoopMigrationEntryPoint` 교체 PR 현재 단계

### 현재 상태 요약(2026-04-19 코드 기준)
`NoopMigrationEntryPoint`(`custom-plugins/empire-rpg/src/main/java/com/poro/empire/common/db/NoopMigrationEntryPoint.java`)는 **현재도 그대로 있고**(line 7~12, `migrate(Connection)`이 `Result.success()`만 반환하는 완전 스텁), `CommonFoundationBootstrap`이 `new NoopMigrationEntryPoint()`를 직접 주입한다(`CommonFoundationBootstrap.java` line 42). **교체 PR 흔적은 현재 브랜치(wsl-setup) 코드·테스트 어디에도 발견되지 않는다** — `MigrationEntryPoint` 인터페이스의 다른 구현체 없음, Flyway/Liquibase gradle 의존성 없음, `DatabaseBootstrapper.initialize()`는 주입만 되고 호출조차 안 되는 상태(`EmpireRPGPlugin.onEnable()` line 54~64에 호출 없음). 즉 "교체 PR 진행 중"이 아니라 **"착수 전"**이며, 교체 대상(Flyway/Liquibase/자체 러너)도 확정되지 않았다.

### 예상 머지 시점
**불명 — 담당자 확인 필요**. 스프린트 플랜이 "D-0 선행 머지 필수"로 지정했으나 현재 코드 흔적상 착수 전 단계이므로, 별도 담당자 배정 또는 플래그 저장소 스프린트와 동일 담당자 병행 여부부터 결정되어야 한다.

### 권장 결정
**비상안 채택(PR1에 `CREATE TABLE IF NOT EXISTS` 직접 실행 스텁, 교체 PR 머지 시 PR1.5로 스텁 제거)**. 근거는 세 가지.
1. 교체 PR이 착수 전이고 러너 후보(Flyway/Liquibase/자체)조차 미확정 — v0.1 스프린트 10~12 md를 교체 PR 대기로 지연시키면 후속 3시스템(수도 반경·영지 외 블록·각인 활성화) 전체가 연쇄 지연.
2. v0.1 DDL은 `player_flags` 1개 테이블뿐이며 `IF NOT EXISTS` 멱등 보장으로 스텁 구현 난이도 0.5 md 이하.
3. 스텁 제거 PR1.5는 마이그레이션 러너 교체 PR과 일대일 대응되는 얇은 PR이라 회귀 범위가 작다.

### 권장안 세부(CLAUDE.md 흐름)
- 목표: 마이그레이션 러너 부재 상태에서도 PR1 DDL이 실제 SQLite 파일에 반영되게 한다.
- 선행조건: 질문 1 권장안의 PR0(드라이버 의존성 + `initialize()` 호출)가 먼저 머지될 것.
- 작업 분해: (a) PR1 내부에 `FlagStoreSchemaStub`(임시 클래스) 추가, `DatabaseBootstrapper.initialize()` 성공 직후 `CREATE TABLE IF NOT EXISTS player_flags(...)` + 인덱스·PK 실행. (b) 클래스에 `// TODO(migration-replacement): PR1.5에서 제거, 교체 러너가 대체.` 주석 명시. (c) 러너 교체 PR 머지 직후 PR1.5로 스텁 제거 + `MigrationEntryPoint` 구현체에 DDL을 이관.
- 리스크: 아래 2개.
- 테스트 포인트: 서버 콜드 스타트 2회 연속 실행 시 스텁이 예외 없이 idempotent하게 실행되는지, `player_flags` 복합 PK·`updated_at` 인덱스가 생성됐는지 `PRAGMA index_list` 로 확인.
- 1차 버전: PR1 내부 스텁만으로 v0.1 릴리즈, PR1.5는 교체 PR 머지 후 백로그.

### 리스크 2개
- **교체 PR이 시즌2 수준으로 지연돼 스텁이 장기 부채화** — 완화: `poro_open_questions_registry.md`에 "PR1.5 스텁 제거 기한 = 교체 PR 머지 +1주" SLA 명시, `poro_master_planning.md` 변경 로그에 예약. 교체 PR 담당자가 플래그 저장소 담당자와 다른 경우에도 PR1.5 책임은 플래그 저장소 담당으로 고정.
- **스텁과 교체 러너의 DDL 선언이 이중 적용되어 idempotent 깨짐** — 완화: PR1.5 체크리스트에 "스텁 DDL 제거 전에 러너 측 마이그레이션 파일에 동일 DDL 이관 완료 확인" 항목을 넣고, 테스트 DB에서 두 경로 중복 실행해도 `CREATE TABLE IF NOT EXISTS`가 성공하는지 확인.

### 충돌 지점
- **없음**. v0.1 범위 내 임시 스텁이며 T1/T2·각인 구조와 무관.

---

## 질문 3 — `FlagValueType`에 JSON 허용 여부

### 3안 장단점 표
| 안 | 추가 공수(v0.1) | 리스크 | 후속 3시스템 영향 | v0.2 마이그레이션 비용 |
|---|---|---|---|---|
| A. **v0.1 유지(BOOL/LONG/STRING 3종)** | 0 md(현 스펙) | 복합 구조 필요 시 STRING 우회로 임시 처리 누적 | 수도 반경(BOOL)·영지 외 블록(BOOL)·각인 활성화(STRING+구분자 혹은 다중 BOOL 슬롯 플래그)로 커버 가능 | v0.2에서 JSON 컬럼 추가 시 STRING 우회 데이터를 JSON으로 재변환하는 1회성 마이그레이션 필요 — 테이블 1개·컬럼 1개 추가 수준 |
| B. **v0.1에서 JSON 조기 허용** | 스키마 0.3 md + Repo/Store 타입 분기 0.5~0.8 md + 이벤트 oldValue/newValue 직렬화 0.2 md = 약 1~1.5 md 증가 | 직렬화 스키마 폭주·검증기 설계 범위 확장·테스트 매트릭스 2배(BOOL/LONG/STRING/JSON) | 각인 활성화가 "슬롯+ID+시각" 복합 구조를 자연스럽게 수용 | 없음(이미 JSON 지원) |
| C. **v0.1은 3종 유지하되 v0.2 일정 명시적으로 당김** | 0 md(v0.1) + v0.2 착수 시점을 플래그 저장소 머지 +2주로 고정 | v0.2 우선순위가 다른 시스템(수도 반경·영지 블록 실제 구현)과 경쟁 | v0.1 머지 직후 수도 반경·영지 블록 2건은 BOOL로 선진행, 각인 활성화는 v0.2 JSON 지원 후 착수 | v0.2에서 JSON 컬럼 추가 1회성 마이그레이션(B안 대비 동일) |

### 권장안 1개
**C안(v0.1 3종 유지 + v0.2 JSON 일정 명시적으로 당김)**. 근거:
1. 스프린트 플랜이 10~12 md 제약을 명시했고 B안은 1~1.5 md 증가를 유발해 제약 초과 위험. "과한 범위 확장 금지"(CLAUDE.md)와 직결.
2. 후속 3시스템 중 **수도 반경·영지 외 블록은 BOOL만으로 완전히 커버 가능**, 각인 활성화만 복합 구조를 요구한다. v0.1 머지 → 수도 반경·영지 블록 2건 선행 → v0.2 JSON 허용 → 각인 활성화라는 순서가 각 시스템 도입 순서(스프린트 플랜 후속 통합 순서 "수도 → 영지 → 각인")와 일치.
3. JSON 조기 허용이 주는 이득(각인 활성화 자연 수용)은 **각인 활성화 착수 시점까지의 남은 기간에 v0.2를 당겨도 충분히 커버 가능**.

### 권장안 세부(CLAUDE.md 흐름)
- 목표: v0.1은 3종으로 릴리즈하되, v0.2 JSON 허용 PR을 `플래그 저장소 v0.1 머지 + 2주` 내 착수로 마스터플래닝에 예약한다.
- 선행조건: 스프린트 플랜 PR1~PR8 완료 + 각인 활성화 UI/슬롯 모델 초안 확정.
- 작업 분해(v0.2 예약분): (a) `FlagValueType.JSON` 추가 + `value_json TEXT` 컬럼 ADD 마이그레이션 1건. (b) Store/Repo에 JSON 저장·전체 읽기만 지원(부분 필드 인덱싱·검색 X). (c) 각인 활성화 파사드가 JSON 구조(`{"slot":int, "engravingId":str, "activatedAt":long}`)를 직렬화.
- 리스크: 아래 2개.
- 테스트 포인트: v0.1→v0.2 마이그레이션 실행 시 기존 STRING 우회 데이터가 영향받지 않는지(동일 키에 JSON 재쓰기 시 `value_type`만 교체되는지), 각인 활성화 파사드가 JSON 구조를 BOOL/LONG/STRING과 독립적으로 읽는지.
- 1차 버전: v0.1 BOOL/LONG/STRING, v0.2에서 JSON 추가.

### 리스크 2개
- **각인 활성화가 v0.1 안에 포함되어야 하는 외부 일정이 생길 경우 C안이 블로킹** — 완화: 스프린트 플랜의 후속 통합 순서(수도 → 영지 → 각인)를 `poro_master_planning.md` 변경 로그에 못박고, 각인 활성화를 v0.1 범위에 넣자는 요청이 들어오면 B안으로 재검토하는 경로를 오픈 질문으로 남김.
- **STRING 우회로 "슬롯+ID+시각" 복합 구조가 실제로 저장되어 v0.2 마이그레이션 시 파싱 부담 발생** — 완화: v0.1에서는 각인 활성화 관련 복합 플래그 저장을 **차단**(각인 활성화 파사드 자체를 v0.2 의존으로 표시), 각인 활성화용 `FlagKey`는 v0.2 머지 후 등록.

### 충돌 지점
- **없음**. `FlagValueType` 3종 제한은 스프린트 플랜 스키마 제한과 동일. 포로 서버 기준(각인서 체계 단일축) 변경 없음.

---

## 질문 4 — 변경 이벤트 방식: Bukkit Event vs 내부 이벤트 버스

### 3안 장단점 표
| 안 | 성능 | 테스트 용이성 | 플러그인 간 결합도 | v0.1 공수 |
|---|---|---|---|---|
| A. **Bukkit `Event`** | 동기 메인스레드 호출 시 리스너 수만큼 순차 실행, async 이벤트는 별도 주의 | Bukkit mock(MockBukkit 등) 필요, 기존 `HealthHudListener` 테스트와 동일한 부담 | 타 플러그인(디스코드 봇 브리지·웹 연동 어댑터)에서 구독 가능 — 운영 목표(웹/디스코드 조회)와 정합 | 0.5 md(스프린트 플랜 원안) |
| B. **내부 이벤트 버스** | 직접 dispatcher 호출, 오버헤드 최소·async 제어 자유 | Bukkit 의존 없는 순수 JUnit 테스트 가능 | EmpireRPG 내부에서만 구독, 외부 플러그인은 어댑터 필요 | 0.5~0.8 md(간단 dispatcher + 테스트) |
| C. **하이브리드(내부 버스 1차 + Bukkit Event 브리지)** | 내부 구독은 빠르게, Bukkit은 선택적 발행 | 내부 로직은 순수 테스트, Bukkit 브리지만 mock | 향후 확장 여지 확보 | 0.8~1.2 md(원안 대비 +0.3~0.7 md) |

### 권장안 1개
**A안(Bukkit `Event`)**. 근거 2~3줄:
- 기존 EmpireRPG 코드(`HealthHudListener`·`HungerLockListener`)가 **이미 Bukkit Event 패턴만 사용**하고 있으며 별도 내부 디스패처 전례가 없다. 새 패턴 도입은 "기존 구조 유지하며 확장"(CLAUDE.md 최우선 원칙) 위반.
- 포로 서버 운영 목표가 웹·디스코드 봇 조회이므로 **장기적으로 다른 플러그인/브리지에서 플래그 변경을 구독할 수 있어야 함**. Bukkit Event는 이 확장에 표준 경로를 제공.
- v0.1 공수 0.5 md를 유지할 수 있어 스프린트 총 예산 안정.

### 권장안 세부(CLAUDE.md 흐름)
- 목표: `PlayerFlagChangedEvent`를 Bukkit 표준 `Event`(`extends org.bukkit.event.Event`)로 발행하고, 후속 3시스템이 `@EventHandler`로 구독한다.
- 선행조건: PR4(`PlayerFlagStore`)가 `set/remove` 트랜잭션 성공 지점을 확정하여 그 직후에만 이벤트를 발행.
- 작업 분해: (a) `PlayerFlagChangedEvent(player, flagKey, oldValue, newValue, source)` 정의. (b) `Store.set/remove`에서 트랜잭션 성공 후 동기 메인스레드에서 `Bukkit.getPluginManager().callEvent(...)` 호출. (c) `batchSet`은 N개 개별 발행(v0.1 규약).
- 리스크: 아래 2개.
- 테스트 포인트: set 성공 시에만 발행(실패 시 미발행), oldValue/newValue 정확성, 동일 값 set 시 발행 정책(스프린트 플랜 오픈 — "동일 값이면 미발행" 권장, 이벤트 폭주 방지). `batchSet` N개 발행 시 이벤트 리스너에서 카운트 일치 테스트.
- 1차 버전: 동기 메인스레드 발행 + N개 개별 발행. async/배치 이벤트는 v0.2.

### 리스크 2개
- **`batchSet` 호출 시 각인 일괄 변경(예: 각인 슬롯 6개 동시 갱신)에서 N개 이벤트 연쇄 발행으로 메인스레드 지연** — 완화: v0.1 규약으로 명시된 "N개 발행 허용"을 유지하되, 각인 시스템 도입 시점에 `PlayerFlagBatchChangedEvent`(단일 이벤트 + List<Change>)를 v0.2로 추가 예약. `poro_master_planning.md`에 예약 로그 남김.
- **Bukkit Event는 메인스레드 강제로 트랜잭션 직후 발행 시 I/O 지연이 스레드를 점유** — 완화: v0.1 write-through가 플레이어 단위 단일 스레드 가정이므로 단일 플레이어 당 발행 빈도는 낮음. 부하 측정 결과 문제 시 v0.2에서 async 이벤트로 전환하되, async 이벤트 구독자는 Bukkit API 호출 제약을 준수하도록 문서화.

### 충돌 지점
- **없음**. 기존 EmpireRPG 리스너 패턴 유지, 포로 서버 기준 불변.

---

## 이번에 정리한 핵심
- 질문 1: `FoundationContext`에 `ConnectionProvider/TransactionHelper/DatabaseBootstrapper` 실제 주입 완료. 단 HikariCP 풀은 없고 SQLite JDBC 드라이버 gradle 의존성 누락 + `DatabaseBootstrapper.initialize()` 호출 누락 → PR0(0.5 md)로 선행 해소 권장.
- 질문 2: `NoopMigrationEntryPoint` 교체 PR 흔적 전무(착수 전). 비상안(PR1 내부 스텁 + 교체 후 PR1.5로 제거) 채택 권장.
- 질문 3: v0.1 3종 유지 + v0.2 JSON 일정 당김(C안) 권장. 수도 반경·영지 블록 2건은 BOOL로 선진행, 각인 활성화는 v0.2 의존.
- 질문 4: Bukkit Event(A안) 권장. 기존 리스너 패턴 유지 + 운영 목표(웹/디스코드 조회) 확장 여지 확보.

## 남은 확인 포인트
- 질문 1 PR0의 SQLite JDBC 드라이버 **LTS 버전 지정**(`org.xerial:sqlite-jdbc` 3.x 중 어느 minor까지 고정할지) — 후속 결정 필요.
- 질문 2 교체 PR **담당자 배정 + 러너 후보(Flyway/Liquibase/자체) 선택** — 플래그 저장소 스프린트와 별개 트랙이어도 SLA는 명시 필요.
- 질문 3 v0.2 JSON PR **착수 시점 확정 기한** — "플래그 저장소 v0.1 머지 + 2주" 기본값이 실제 각인 활성화 UI 초안 완성 시점과 정합한지 재확인.
- 질문 4 **"동일 값 set 시 발행 정책"** — 원안은 오픈. 본 응답은 "미발행 권장"으로 제시했으나 사용자 확정 필요.

## 다음 추천 작업
- 본 응답을 사용자가 승인하면 `poro_master_planning.md` 변경 로그에 4건 결정 반영 + `poro_open_questions_registry.md`에서 4건 취소선 + 결정 요약 1줄 처리.
- `poro_flag_store_v01_sprint_plan.md` 오픈 질문 섹션에 동일 결정 복사 + **PR0(드라이버·initialize 호출) 섹션을 PR1 앞에 추가**.
- 질문 2 교체 PR 담당자·러너 후보 확정 건은 별도 운영 이슈로 분리.

## 상위 문서 참조
- 자문 요청서: `poro_flag_store_v01_consultation_prompts.md`
- 스프린트 플랜: `poro_flag_store_v01_sprint_plan.md`
- 오픈 질문 레지스트리: `poro_open_questions_registry.md` §1
- 마스터플래닝: `../poro_master_planning.md`
- 프로젝트 원칙: `../../CLAUDE.md`
