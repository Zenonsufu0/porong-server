# 플레이어 플래그 저장소 v0.1 — PR0 + PR1 실제 구현 계획 드래프트

작성일: 2026-04-19
작성자: implementation-reviewer
전제: 사용자 승인 — PR0 착수, Q2 비상안, Q3 C안, Q4 A안 전체 수용.
본 드래프트는 **구현 명세**이며, 실제 코드 편집은 본 드래프트 승인 후 별도 작업으로 진행한다.

---

## 1. 구현 목표

- PR0: `FoundationContext` 인프라 구멍 3개(의존성·초기화 훅·풀 정책)를 메워 DB 초기화가 실제 가동 상태로 승격되게 한다.
- PR1: `player_flag` 테이블 DDL을 SQLite에 실제 생성하고, 러너 도착 전까지 `CREATE TABLE IF NOT EXISTS` 스텁을 전용 마이그레이션 클래스로 격리한다.
- 범위 밖(후속 PR): FlagKey 검증기(PR2), Repository(PR3), Store/캐시(PR4), Lifecycle Hook(PR5), Event(PR6), 커맨드(PR7), 파사드(PR8).

## 2. 선행조건

- 스프린트 플랜 `docs/00_index_and_execution/poro_flag_store_v01_sprint_plan.md` PR 분해 고정.
- 자문 응답 `docs/00_index_and_execution/poro_flag_store_v01_consultation_response_v1.md` Q2 비상안(스텁), Q3 C안(BOOL/LONG/STRING), Q4 A안(Bukkit Event) 적용.
- 기존 흐름 유지: `EmpireRPGPlugin.onEnable()` → `CommonFoundationBootstrap.bootstrap(this)` → `FoundationContext` 반환 구조 그대로.
- SQLite JDBC 드라이버는 외부 의존성으로 shade 또는 runtimeOnly 추가(Paper 내장 없음).
- HikariCP는 **v0.1 미도입**. per-call `DriverManager.getConnection()` 유지(현재 `SqliteConnectionProvider` 구조 그대로). 풀 도입은 v0.2 별도 PR.

## 3. 작업 분해

### 3.1 브랜치 전략

- 베이스: `master` (현재 작업 중인 `wsl-setup` 브랜치와 분리).
- PR0 브랜치: `feat/flag-store-pr0-foundation-wiring`
- PR1 브랜치: `feat/flag-store-pr1-player-flag-ddl` (PR0 머지 후 `master`에서 분기)
- PR1.5 브랜치(예약): `feat/flag-store-pr1_5-migration-runner` (러너 도입 후 스텁 제거)
- **순차 머지 필수**. 병렬 작업 금지 이유:
  - PR1의 통합 테스트가 PR0의 `DatabaseBootstrapper.initialize()` 호출에 의존한다.
  - 병렬 진행 시 `FoundationContext` 시그니처 충돌 리스크가 남는다.
- 리뷰 승인 의존성: PR0 리뷰 1인 + 머지 → PR1 브랜치 리베이스 → PR1 리뷰 1인 + 머지.

### 3.2 커밋 메시지 규약

- 형식: `feat(flag-store): pr<N> <영문 요약>` (부제 선택)
- 예시:
  - `feat(flag-store): pr0 add sqlite jdbc dependency`
  - `feat(flag-store): pr0 wire DatabaseBootstrapper.initialize() into onEnable`
  - `feat(flag-store): pr1 add player_flag DDL stub migration`
  - `test(flag-store): pr1 add DDL idempotency test`
- Revert 필요 시 `revert(flag-store): pr<N> <원 요약>`.
- PR 제목 = 최상위 커밋 메시지와 동일.

### 3.3 PR0 — 파일별 변경 명세 (0.5 man-day)

> **2026-04-19 Paper 배포 방식 판정 결과 보정**: 현재 `build.gradle.kts`는 Shadow plugin 미적용(`plugins { java }` 단독) + 런타임 의존성(`implementation`/`runtimeOnly`) 선언 없음. `plugin.yml`에도 `libraries:` 섹션 없음. 이 상태에서 원안대로 `implementation("org.xerial:sqlite-jdbc")`만 추가하면 **빌드 통과 + 런타임 `ClassNotFoundException`** 발생(Paper `PluginClassLoader`가 비번들 의존성을 자동 흡수하지 않음). **정정안 = Paper 1.18+ `libraries:` manifest 방식** 채택: `build.gradle.kts`는 `compileOnly` 유지 + `plugin.yml`에 `libraries:` 선언 추가. Paper가 첫 로드 시 Maven Central에서 자동 다운로드·검증·캐시. Shadow plugin 도입 불필요, JAR 크기 증가 없음. 아래 변경 명세는 이 정정안 기반.

#### 파일 1. `custom-plugins/empire-rpg/build.gradle.kts`

**변경 위치**: line 22~35 `dependencies { ... }` 블록.

**삽입 라인**: line 29(`compileOnly("net.luckperms:api:5.4")`) 바로 아래.

**의존성 좌표** (정정):
```kotlin
compileOnly("org.xerial:sqlite-jdbc:3.46.1.0")
```

근거:
- `compileOnly`로 선언해 컴파일 시에만 API 접근. 런타임은 아래 파일 1-b(`plugin.yml`)의 `libraries:` 선언으로 Paper가 Maven에서 자동 로드.
- Paper 1.21.10은 SQLite JDBC를 내장하지 않으므로 외부 의존성 필수.
- `org.xerial:sqlite-jdbc`는 `DriverManager.getConnection("jdbc:sqlite:...")` 호출 시 자동 등록(JDBC 4.0 ServiceLoader).
- Shadow plugin 추가 도입 없이 기존 `plugins { java }` 단일 구조 유지.

#### 파일 1-b. `custom-plugins/empire-rpg/src/main/resources/plugin.yml` (2026-04-19 정정안 신규 추가)

**변경 위치**: line 11(`  - BetonQuest`) 바로 아래, `commands:` 블록 앞.

**삽입 내용**:
```yaml
libraries:
  - org.xerial:sqlite-jdbc:3.46.1.0
```

근거:
- Paper 1.18+ 공식 `libraries:` manifest 기능. 서버 첫 로드 시 Paper가 지정 좌표를 Maven Central에서 다운로드해 플러그인 클래스로더에 주입.
- 런타임 자동 주입으로 `PluginClassLoader`가 `org.sqlite.*` 클래스를 정상 해석 → `ClassNotFoundException` 원천 차단.
- 네트워크 차단 환경에서는 수동으로 Maven Central에서 받아 `libraries/` 폴더에 배치 필요(운영 가이드 후속 과제).

#### 파일 2. `custom-plugins/empire-rpg/src/main/java/com/poro/empire/EmpireRPGPlugin.java`

**변경 위치**: line 54~64 사이 `CommonFoundationBootstrap.bootstrap(this)` 성공 분기 직후.

**삽입 위치**: line 64 `this.foundationContext.logger().domain("foundation").info("Foundation bootstrap completed.");` **다음 줄**에 DB 초기화 호출 추가.

**삽입 스니펫**:
```java
this.foundationContext = foundationResult.value();
this.foundationContext.logger().domain("foundation").info("Foundation bootstrap completed.");

Result<Void> dbInitResult = this.foundationContext.databaseBootstrapper().initialize();
if (dbInitResult.isFailure()) {
    getLogger().severe("Failed to initialize database: " + dbInitResult.message());
    if (dbInitResult.cause() != null) {
        getLogger().severe("Cause: " + dbInitResult.cause().getMessage());
    }
    getServer().getPluginManager().disablePlugin(this);
    return;
}
this.foundationContext.logger().domain("db.migration").info("Database initialize completed.");
```

근거:
- 현재 `CommonFoundationBootstrap.bootstrap`은 `DatabaseBootstrapper`를 **생성만** 하고 `initialize()`를 호출하지 않는다(line 40~44).
- `onEnable()` 전체를 재설계하지 않고, 기존 `Result` 실패 분기 패턴을 그대로 복제해 최소 침습.
- MasterRegistry·NpcSync·CombatEngine 등 후속 모듈이 DB 가동을 전제로 하므로 DB 초기화는 **Foundation 직후, MasterRegistry 이전**에 삽입.
- 대안: `CommonFoundationBootstrap.bootstrap()` 내부에서 `initialize()` 호출. **기각 이유** — bootstrap은 컨텍스트 구성만 담당하고, Result 플로우는 `onEnable`이 모두 위임받는 현재 구조와 일치시키기 위함.

#### 파일 3. (선택 미적용) HikariCP 도입 여부

- **v0.1 결정: 미도입.**
- 유지 파일: `SqliteConnectionProvider.java` line 22~33 `DriverManager.getConnection()` 그대로.
- 도입 시 영향 파일(기록용, v0.2 예약):
  - `build.gradle.kts` — `com.zaxxer:HikariCP:5.1.0` 추가
  - `SqliteConnectionProvider.java` — 내부에 `HikariDataSource` 보유로 전환
  - `CommonFoundationBootstrap.java` — `HikariConfig` 구성 값 주입(풀 사이즈·타임아웃)
  - `JdbcTransactionHelper.java` — 변경 없음(ConnectionProvider 추상화 덕분)
- v0.1 근거: 단일 서버·단일 프로세스·v0.1 read/write 빈도 낮음. 풀 도입은 v0.2 동시성 강화 시점에 `ConnectionProvider` 구현 교체만으로 가능.

### 3.4 PR1 — 파일별 변경 명세 (1 man-day)

#### 파일 1(신규). `custom-plugins/empire-rpg/src/main/java/com/poro/empire/common/db/migration/PlayerFlagTableStubMigration.java`

전용 클래스로 DDL을 격리하여 PR1.5에서 삭제 경계가 명확하게 한다. `NoopMigrationEntryPoint`는 교체하지 않고, **합성 엔트리포인트**가 Noop + 스텁을 순차 실행하도록 한다.

```java
package com.poro.empire.common.db.migration;

import com.poro.empire.common.db.MigrationEntryPoint;
import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.common.result.ErrorCode;
import com.poro.empire.common.result.Result;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;

/**
 * v0.1 한시적 스텁. PR1.5에서 마이그레이션 러너로 이관 후 삭제 예정.
 */
public final class PlayerFlagTableStubMigration implements MigrationEntryPoint {
    private final DomainLogger logger;

    public PlayerFlagTableStubMigration(DomainLogger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public Result<Void> migrate(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(PlayerFlagDdl.CREATE_TABLE);
            statement.execute(PlayerFlagDdl.CREATE_INDEX_UPDATED_AT);
            statement.execute(PlayerFlagDdl.CREATE_INDEX_FLAG_KEY);
            logger.info("player_flag stub DDL applied (idempotent).");
            return Result.success();
        } catch (Exception exception) {
            return Result.failure(
                    ErrorCode.DB_CONNECTION_FAILED,
                    "Failed to apply player_flag stub DDL",
                    exception);
        }
    }
}
```

#### 파일 2(신규). `custom-plugins/empire-rpg/src/main/java/com/poro/empire/common/db/migration/PlayerFlagDdl.java`

DDL 상수를 클래스 상수로 분리하여 테스트·마이그레이션·향후 러너 재사용을 용이하게 한다.

```java
package com.poro.empire.common.db.migration;

public final class PlayerFlagDdl {
    private PlayerFlagDdl() {}

    public static final String CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS player_flag (
            player_uuid   TEXT    NOT NULL,
            flag_key      TEXT    NOT NULL,
            value_type    TEXT    NOT NULL CHECK (value_type IN ('BOOL','LONG','STRING')),
            value_text    TEXT,
            value_long    INTEGER,
            updated_at    INTEGER NOT NULL,
            version       INTEGER NOT NULL DEFAULT 1,
            PRIMARY KEY (player_uuid, flag_key)
        )
        """;

    public static final String CREATE_INDEX_UPDATED_AT =
        "CREATE INDEX IF NOT EXISTS idx_player_flag_updated_at ON player_flag(updated_at)";

    public static final String CREATE_INDEX_FLAG_KEY =
        "CREATE INDEX IF NOT EXISTS idx_player_flag_flag_key ON player_flag(flag_key)";
}
```

**DDL 최종 안 설명**:
- 컬럼 7개: `player_uuid`, `flag_key`, `value_type`, `value_text`, `value_long`, `updated_at`, `version`.
- 복합 PK `(player_uuid, flag_key)` → 자동 unique 인덱스 생성.
- 명시 인덱스 2개: `idx_player_flag_updated_at`(최근 변경 추적·디버깅), `idx_player_flag_flag_key`(PR3 `findByFlag` 디버깅용).
- CHECK 제약: `value_type`을 3종으로 한정(Q3 C안 반영).
- `updated_at`은 epoch millis 저장(INTEGER). SQLite는 TIMESTAMP 네이티브 타입 없음, 정수 저장이 표준 관행.
- `version` = 낙관적 락 예약 슬롯(현재 미사용). v0.2에서 `UPDATE ... WHERE version = ?` 패턴으로 활용.
- **v0.2 JSON 대비 컬럼 배치**: `value_long` 뒤에 `value_json TEXT`를 `ALTER TABLE ADD COLUMN`으로 추가할 예정. SQLite는 ADD COLUMN이 테이블 재생성 없이 가능하고, 컬럼 순서는 물리 저장에 민감하지 않으므로 현재 순서 유지로 충분. CHECK 제약은 v0.2에서 DROP/재생성 필요(SQLite 한계) — 이 비용은 v0.2 PR 범위에 포함.

#### 파일 3(신규). `custom-plugins/empire-rpg/src/main/java/com/poro/empire/common/db/migration/CompositeMigrationEntryPoint.java`

현재 `CommonFoundationBootstrap`는 단일 `MigrationEntryPoint`를 주입하므로, 여러 스텁을 순차 실행할 합성 엔트리를 둔다. PR1.5에서 이 파일이 실제 러너로 교체되는 자리가 된다.

```java
package com.poro.empire.common.db.migration;

import com.poro.empire.common.db.MigrationEntryPoint;
import com.poro.empire.common.result.Result;

import java.sql.Connection;
import java.util.List;

public final class CompositeMigrationEntryPoint implements MigrationEntryPoint {
    private final List<MigrationEntryPoint> delegates;

    public CompositeMigrationEntryPoint(List<MigrationEntryPoint> delegates) {
        this.delegates = List.copyOf(delegates);
    }

    @Override
    public Result<Void> migrate(Connection connection) {
        for (MigrationEntryPoint delegate : delegates) {
            Result<Void> result = delegate.migrate(connection);
            if (result.isFailure()) {
                return result;
            }
        }
        return Result.success();
    }
}
```

#### 파일 4. `custom-plugins/empire-rpg/src/main/java/com/poro/empire/common/config/CommonFoundationBootstrap.java`

**변경 위치**: line 40~44 `DatabaseBootstrapper` 생성 블록.

**변경 내용**: `NoopMigrationEntryPoint`를 그대로 두되, `CompositeMigrationEntryPoint`로 감싸 스텁을 추가.

```java
MigrationEntryPoint migrationEntryPoint = new CompositeMigrationEntryPoint(List.of(
        new NoopMigrationEntryPoint(),
        new PlayerFlagTableStubMigration(logger.domain("db.migration.player-flag"))
));
DatabaseBootstrapper databaseBootstrapper = new DatabaseBootstrapper(
        connectionProvider,
        migrationEntryPoint,
        logger.domain("db.migration")
);
```

**PR1.5 스텁 제거 전략**:
1. PR1.5에서 실제 러너(Flyway/Liquibase/자체) 도입.
2. 러너가 `player_flag` DDL을 자신의 버전 관리 테이블 기반으로 재적용(또는 `schema_version` 초기 시드로 "v1 적용됨" 기록).
3. `CompositeMigrationEntryPoint`의 `delegates` 리스트에서 `PlayerFlagTableStubMigration` 제거.
4. `PlayerFlagTableStubMigration.java`·`PlayerFlagDdl.java` 파일 삭제 또는 러너의 리소스 디렉터리로 이동(`resources/db/migration/V1__create_player_flag.sql`).
5. `CREATE TABLE IF NOT EXISTS`의 idempotent 특성 덕분에 데이터 정합성 손실 없음 — 기존 테이블은 그대로 유지.
6. 운영 DB에는 `schema_version` 수동 시드 INSERT 1줄 필요(Flyway baseline 커맨드 또는 자체 러너 초기화 SQL).

## 4. 기술 리스크

- **SQLite JDBC 버전 충돌**: Paper 1.21.10은 내장 안 함. ~~`implementation`으로 추가~~ → **2026-04-19 정정**: `compileOnly` + `plugin.yml` `libraries:` manifest 방식으로 변경(`org.xerial:sqlite-jdbc:3.46.1.0`). Paper 1.18+ 공식 기능으로 Shadow plugin 도입 없이 런타임 자동 로드.
- **`onEnable()` 실패 연쇄**: 현재 구조가 DB 실패 시 `disablePlugin(this)` 경로라 나머지 엔진이 모두 중단된다. PR0에서 동일 패턴을 유지하는 것은 **정합**이나, 첫 구동 시 SQLite 파일 쓰기 권한 문제로 플러그인이 죽을 수 있음. 롤백 경로: PR0 revert 커밋 1개로 복귀 가능하게 단일 커밋 분리 유지.
- **스텁 제거 시 데이터 정합성**: 위 PR1.5 전략 항목 참조. `IF NOT EXISTS`와 idempotent 설계로 리스크 차단.
- **CHECK 제약의 v0.2 전환 비용**: SQLite는 ADD CHECK 불가, 테이블 재생성(`ALTER TABLE RENAME` → `CREATE` → `INSERT SELECT` → `DROP`) 필요. v0.2 PR 공수에 +0.5 man-day 반영 권장.
- **PR0 롤백 경로**:
  - PR0 머지 후 문제 발견 시 `git revert <pr0-merge-commit>` 1회로 전체 되돌림.
  - PR0의 변경이 2개 파일(`build.gradle.kts`·`EmpireRPGPlugin.java`)로 제한되므로 revert 충돌 리스크 최소.
  - `FoundationContext` 시그니처는 변경하지 않는다(롤백 용이성 보장).

## 5. 테스트 포인트

### PR0

- 단위 테스트: `CommonFoundationBootstrapTest`(신규) — `bootstrap()` 반환 컨텍스트의 `databaseBootstrapper()`가 non-null. 기존 테스트 패턴 없으면 최소 JUnit 1건만.
- 통합 테스트(수동): 로컬 Paper 서버 기동 → 로그에서 `Foundation bootstrap completed.`와 `Database initialize completed.` 2줄 확인 → `storage/poro.sqlite` 파일 생성 확인.
- 회귀: MasterRegistry·CombatEngine 등 후속 부트스트랩이 정상 통과하는지(기존 `onEnable` 로그).

### PR1

- 단위 테스트 1: `PlayerFlagDdlTest` — in-memory SQLite(`jdbc:sqlite::memory:`)에 `CREATE_TABLE` 2회 실행 → idempotent 확인.
- 단위 테스트 2: 동일 `(player_uuid, flag_key)` INSERT 2회 → SQLITE_CONSTRAINT_PRIMARYKEY 예외 확인.
- 단위 테스트 3: `value_type = 'JSON'` INSERT → CHECK 제약 위반 예외 확인.
- 단위 테스트 4: `sqlite_master`에서 `idx_player_flag_updated_at`·`idx_player_flag_flag_key` 존재 확인.
- 단위 테스트 5: `CompositeMigrationEntryPoint`가 delegate 리스트를 순서대로 호출하고 중간 실패 시 뒤 delegate는 호출 안 되는지(목 객체 기반).
- 크래시 복구 시나리오: migrate() 중간 실패 → `DatabaseBootstrapper.initialize()`가 Connection을 try-with-resources로 닫는지(`DatabaseBootstrapper.java` line 31) 재확인. 부분 DDL 적용 상태에서 재기동해도 `IF NOT EXISTS`로 통과.

## 6. 1차 버전 권장안

- PR0: **파일 3개 변경** — `build.gradle.kts` (line 29 `compileOnly` 추가) + `plugin.yml` (line 11 직후 `libraries:` 섹션 추가) + `EmpireRPGPlugin.java` (line 64 직후 `DatabaseBootstrapper.initialize()` 훅 삽입). HikariCP 미도입. (2026-04-19 Paper 배포 방식 정정으로 2개 → 3개로 변경)
- PR1: 신규 파일 3개(`PlayerFlagTableStubMigration`·`PlayerFlagDdl`·`CompositeMigrationEntryPoint`) + 기존 파일 1개 수정(`CommonFoundationBootstrap`). 총 4개 파일.
- 테스트는 PR1에서 JUnit 5건만 추가. 통합 테스트는 수동 로그 확인으로 충분.
- 드래프트 승인 후 실제 코드 편집 순서: PR0 구현 → 로컬 빌드/수동 기동 확인 → PR0 오픈·머지 → PR1 브랜치 분기 → PR1 구현·테스트 → PR1 오픈·머지.

## 7. 나중으로 미뤄도 되는 것

- HikariCP 풀 도입 → v0.2.
- 실제 마이그레이션 러너(Flyway/Liquibase/자체) → PR1.5.
- `value_json` 컬럼 ADD + CHECK 재생성 → v0.2 JSON 허용 PR.
- `version` 컬럼 기반 낙관적 락 실사용 → v0.2.
- 비동기 Join 로드 → v0.2 (PR5 범위 밖).

---

## 이번에 정리한 핵심
- PR0 2파일·PR1 4파일 단위로 변경을 최소화했다.
- DDL을 전용 스텁 클래스로 격리해 PR1.5에서 삭제 경계를 분명히 했다.
- PR0·PR1 순차 머지와 커밋 규약·롤백 경로를 명세했다.

## 남은 확인 포인트
- ~~Paper 플러그인 배포가 shadow jar 기반인지 `libraries:` manifest 기반인지 확인 필요(PR0 오픈 전 결정).~~ **해결 완료 (2026-04-19)** — 현 `build.gradle.kts`·`plugin.yml` 실측 결과 둘 다 미적용 상태. **`libraries:` manifest 방식 채택**: `build.gradle.kts`에 `compileOnly(...)` + `plugin.yml`에 `libraries:` 선언 2파일 변경으로 해결. Shadow plugin 도입 불필요.
- `CommonFoundationBootstrapTest` 등 테스트 인프라가 현재 존재하는지 확인 필요(없으면 최소 JUnit 스켈레톤 추가).
- PR1.5 러너 선택(Flyway/Liquibase/자체) 최종 결정 시점.

## 다음 추천 작업
- 본 드래프트 리뷰 후 PR0 브랜치 생성 및 파일 2개 편집 착수.
- PR0 로컬 빌드 통과·수동 기동 확인 뒤 PR 오픈.
- PR0 머지 후 PR1 브랜치 분기.
