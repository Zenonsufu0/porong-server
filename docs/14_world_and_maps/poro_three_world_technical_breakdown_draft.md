# 포로 서버 월드 3축 전환 에픽 기술 분해 드래프트

> 문서 버전: 2026-04-19 1차 드래프트 (implementation-reviewer)
> 선행 기획 에픽: `poro_three_world_transition_design_draft.md` (2026-04-19 승인)
> 2026-04-19 사용자 확정 블로커 해제:
> - **맵 생성 B안**: 보이드 월드 + 영지 스키매틱 동적 생성 + 7×7 청크 슬롯 부채꼴 순차 할당
> - **크로스 기기 A안**: 기존 세션 kick 선행 후 새 세션 허용
> - **슬롯 회수 A안**: 시즌 경계 일괄 재활성화
> 본 문서는 기획 에픽 7개 구현 포인트 중 **엔지니어링 세부 위임 대상 4종**(맵 생성·이관 트랜잭션·영지 점유 락·크로스 기기 방지)을 8~12 PR 단위로 분해한다.

---

## 1. 구현 목표

- `estate_world`(영지 전용), `experience_world`(체험) 두 신규 월드를 기존 `main_world`와 병존시키며, **Paper 단일 인스턴스 내부 분리 월드**로 운영한다.
- 영지 월드는 **보이드 생성기 + 7×7 청크 슬롯 + 스키매틱 스탬핑**으로 구성하며, 슬롯 할당은 부채꼴 순차로 결정적이다.
- 세 월드 간 이관은 `Player#teleport` 단일 API만 사용하고, **이관 트랜잭션 플래그**로 크래시 복구·중복 이동 방지를 동시에 만족한다.
- 영지당 **한 시점 한 세션**만 영지 청크를 풀 로드하며, 크로스 기기 동시 접속 시도는 기존 세션을 강제 종료한 뒤 새 세션을 받는다.
- 1차 버전은 **지역 서버 단일 운영**을 전제로 하되, Velocity/BungeeCord 프록시 환경으로의 확장을 막지 않는 인터페이스를 남긴다.

---

## 2. 선행조건

- **플래그 저장소 PR0/PR1 선행 머지 필수**: 이관 트랜잭션 플래그(`teleporting=true`), 영지 점유 락, 세션 식별자(`session_id`), 복귀 좌표 모두 플래그 저장소(`sqlite-jdbc`) 위에 올라간다. PR0(스키마·커넥션)과 PR1(읽기/쓰기 API) 머지 이후에 본 에픽의 PR2 이상이 착수 가능하다.
- **Paper 1.21 API 확정**: `WorldCreator`, `ChunkGenerator`, `BiomeProvider`, `PlayerTeleportEvent`, `PlayerLoginEvent`는 1.21 기준으로 안정 API. `plugin.yml`의 `api-version: '1.21'` 유지.
- **기존 플러그인 패키지 구조**: `com.poro.empire.settlement`(영지), `com.poro.empire.common`(공통), `com.poro.empire.npc.citizens`(NPC)는 이미 존재. 신규 패키지 3종을 추가한다 — `com.poro.empire.world`(월드 부트스트랩·생성기), `com.poro.empire.estate.slot`(슬롯 할당), `com.poro.empire.transfer`(이관 트랜잭션).
- **Citizens NPC 좌대화** 정책(`poro_estate_chunk_load_review_v1.md` 리스크 1)은 별도 에픽으로 선행 또는 병행. 본 에픽에서는 Citizens AI 영향 받지 않는 월드 로딩 계층만 다룬다.
- **FAWE 또는 WorldEdit 도입 결정 필요 (오픈 질문 1)**. 본 문서는 **FAWE 선택을 권장**한다(근거: 섹션 3.3 · 섹션 4).

---

## 3. 작업 분해

### 3.1 Multiverse-Core 도입 여부 결정

**권장: Paper 내장 `WorldCreator` 순정 활용 (Multiverse-Core 미도입)**

- 도입 거절 근거:
  - 본 에픽에서 필요한 기능은 "월드 2개 부팅 + 커스텀 생성기 등록 + 기본 게임룰 고정" 3가지뿐. Multiverse-Core의 포털·인벤토리 분리·권한 그룹 기능은 오히려 EmpireRPG 플러그인과 **기능 중복 혹은 충돌 리스크**.
  - 영지 전용 리전 자동 생성(`estate_<uuid>`)은 WorldGuard API 직접 호출이 더 단순. Multiverse-Core를 거치면 플러그인 연쇄 의존 증가.
  - Paper 1.21의 `WorldCreator#generator(ChunkGenerator)`·`WorldCreator#biomeProvider(BiomeProvider)` API만으로 본 에픽 전체 요구를 충족.
- Multiverse 도입이 정당화되는 상황: 관리자 수동 월드 생성·폴드 관리·멀티월드 경매 분리 운영 UI가 필요해질 때. **현 1차 범위 외**.

```java
// com.poro.empire.world.WorldBootstrap (핵심 골자, 15줄 이내)
public final class WorldBootstrap {
    public World ensureEstateWorld(Plugin plugin) {
        World existing = Bukkit.getWorld("estate_world");
        if (existing != null) return existing;
        WorldCreator creator = new WorldCreator("estate_world")
            .environment(World.Environment.NORMAL)
            .generator(new VoidChunkGenerator())
            .biomeProvider(new PlainsBiomeProvider())
            .generateStructures(false);
        World w = creator.createWorld();
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setTime(6000L);
        return w;
    }
}
```

### 3.2 보이드 `ChunkGenerator` 스켈레톤

- 영지 월드 외부는 **순수 보이드 + plains 바이옴**(바닐라 구조물·몹·지형 생성 전면 차단).
- 플레이어가 슬롯 밖으로 이탈하는 순간은 per-player WorldBorder가 막고, **WorldBorder를 우회한 낙하(엔더펄·명령어)**는 Y<-32 감지 시 이관 트랜잭션으로 수도 스폰 복귀.

```java
// com.poro.empire.world.VoidChunkGenerator (핵심 골자)
public final class VoidChunkGenerator extends ChunkGenerator {
    @Override public void generateNoise(WorldInfo w, Random r, int cx, int cz, ChunkData d) {}
    @Override public void generateSurface(WorldInfo w, Random r, int cx, int cz, ChunkData d) {}
    @Override public void generateBedrock(WorldInfo w, Random r, int cx, int cz, ChunkData d) {}
    @Override public void generateCaves(WorldInfo w, Random r, int cx, int cz, ChunkData d) {}
    @Override public boolean shouldGenerateStructures() { return false; }
    @Override public boolean shouldGenerateDecorations() { return false; }
    @Override public boolean shouldGenerateMobs() { return false; }
    @Override public Location getFixedSpawnLocation(World w, Random r) {
        return new Location(w, 0.5, 64, 0.5);
    }
}
```

- 체험 월드(`experience_world`)도 동일한 `VoidChunkGenerator` 재사용 가능. 스키매틱만 `experience_plaza.schem` 한 장 사전 배치.

### 3.3 영지 스키매틱 로더

**권장: FAWE(FastAsyncWorldEdit) 도입**

- WorldEdit 단독 대비 FAWE는 **비동기 붙여넣기**(`AsyncPasteBuilder`)를 제공해 6×6 청크(Lv5) 영지도 메인 스레드 점유 없이 스탬핑 가능.
- Paper 내장 `Block` API 직접 호출은 6×6 청크(약 96×96×Y 블록) 셀 단위 `setBlock` 호출 시 메인 스레드 점유 위험이 크다 — 1차 부팅에서 TPS 경고 가능.
- 스키매틱 파일 포맷은 `.schem`(Sponge schematic format) 표준 채택. 플러그인 리소스 디렉터리 `plugins/EmpireRPG/schematics/estate_lv{1..5}.schem` 고정.

```java
// com.poro.empire.estate.schematic.EstateSchematicStamper (의사코드, 20줄 이내)
public final class EstateSchematicStamper {
    public CompletableFuture<Void> stamp(UUID ownerId, int level, Location slotOrigin) {
        File file = schematicFileFor(level);  // estate_lv{N}.schem
        return CompletableFuture.runAsync(() -> {
            Clipboard clip = loadClipboard(file);
            try (EditSession es = WorldEdit.getInstance().newEditSession(slotOrigin.getWorld())) {
                Operation op = new ClipboardHolder(clip)
                    .createPaste(es)
                    .to(BlockVector3.at(slotOrigin.getBlockX(), slotOrigin.getBlockY(), slotOrigin.getBlockZ()))
                    .ignoreAirBlocks(true)
                    .build();
                Operations.complete(op);
            }
        }, FAWE_ASYNC_EXECUTOR).thenRun(() -> recordSlotStamped(ownerId, level));
    }
}
```

- 스키매틱 제작은 **운영/기획팀 과제**(Lv1~5 각 1장, 총 5장). 본 에픽 1차 범위는 **로더 인터페이스 + 더미 스키매틱 1장**만 포함하고 실 에셋은 후속 에픽.

### 3.4 슬롯 할당 알고리즘

- 부채꼴 순차(스파이럴) — 원점(0,0)에서 바깥으로 한 슬롯씩 결정적으로 확장. 7×7 청크 슬롯 간격 = 112블록(7 × 16).
- 슬롯 ID는 스파이럴 인덱스 N(0부터)로 고정. DB 저장은 `(owner_uuid, slot_index, slot_x, slot_z, assigned_at, released_at, state)`.
- 해제된 슬롯은 `state='RELEASED'`로만 마킹하고, **다음 시즌 경계에서 일괄 `state='FREE'`로 전환**(A안 확정).

```java
// com.poro.empire.estate.slot.SpiralSlotAllocator (핵심 골자, 20줄 이내)
public final class SpiralSlotAllocator {
    private static final int SLOT_CHUNKS = 7;
    private static final int SLOT_BLOCKS = SLOT_CHUNKS * 16;  // 112
    public SlotCoord fromIndex(int n) {
        // ring r: 첫 인덱스 = (2r-1)^2, 바깥 링 크기 = 8r
        if (n == 0) return new SlotCoord(0, 0);
        int r = (int) Math.ceil((Math.sqrt(n + 1) - 1) / 2.0);
        int ringStart = (2 * r - 1) * (2 * r - 1);
        int offset = n - ringStart;
        int side = offset / (2 * r);
        int pos = offset % (2 * r);
        int dx, dz;
        switch (side) {
            case 0 -> { dx =  r;        dz = -r + 1 + pos; }
            case 1 -> { dx =  r - 1 - pos; dz =  r;        }
            case 2 -> { dx = -r;        dz =  r - 1 - pos; }
            default -> { dx = -r + 1 + pos; dz = -r;       }
        }
        return new SlotCoord(dx * SLOT_BLOCKS, dz * SLOT_BLOCKS);
    }
}
```

- `allocateFor(UUID owner)`는 DB 트랜잭션으로 (1) `state='FREE'`이면서 가장 낮은 `slot_index` 선점 → (2) 없으면 `MAX(slot_index)+1` 신규 기록. 동시성은 SQLite `BEGIN IMMEDIATE` + 컬럼 UNIQUE 제약으로 해결.
- **DB 스키마 (1차)**:
  ```sql
  CREATE TABLE estate_slot (
      slot_index INTEGER PRIMARY KEY,
      owner_uuid TEXT UNIQUE,
      slot_x     INTEGER NOT NULL,
      slot_z     INTEGER NOT NULL,
      state      TEXT NOT NULL,  -- FREE / ASSIGNED / RELEASED
      assigned_at INTEGER,
      released_at INTEGER
  );
  CREATE INDEX ix_estate_slot_owner ON estate_slot(owner_uuid);
  CREATE INDEX ix_estate_slot_state ON estate_slot(state);
  ```

### 3.5 크로스 기기 동시 접속 방지 (A안)

- `PlayerLoginEvent`(AsyncPlayerPreLoginEvent 우선 사용)에서 동일 UUID의 **온라인 세션 존재 여부**를 확인:
  1. 온라인 세션 없음 → 통과.
  2. 온라인 세션 있음 → 기존 세션에 `player.kickPlayer("다른 기기에서 로그인됨")` 발송 → **kick 완료 콜백 대기**(`PlayerQuitEvent` 수신 혹은 타임아웃 3초) → 신규 세션 허용.
- 프록시 환경 대비: 플래그 저장소에 `session_id`(무작위 UUID)와 `last_heartbeat`(5초 주기 업데이트) 기록. 다른 서버 노드에서 로그인 시 동일 UUID의 `session_id`가 다르면 기존 노드에 "kick 요청"을 플러그인 메시징 채널 또는 DB 폴링으로 전달. **1차 버전은 단일 노드 전제 → DB 폴링 경로 스텁만 남김.**
- 엣지 케이스: 비정상 종료(프로세스 크래시)로 `last_heartbeat`가 30초 이상 stale인 경우, 신규 세션은 stale 세션을 **DB 레벨에서 무효화**한 뒤 통과.

### 3.6 이관 트랜잭션 스키마

- 이관 단일 진입점: `TransferService#teleport(Player, TransferRequest)`.
- 3단계:
  1. **Pre-snapshot**: `teleporting=true` 플래그 기록 + 출발 월드·좌표·인벤토리 해시·체력·허기·잠재적 버프 목록 저장.
  2. **Teleport**: `Player#teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN)` 호출.
  3. **Post-commit**: 이동 성공 이벤트(`PlayerTeleportEvent` 완료 콜백 또는 1틱 딜레이 확인) 시 `teleporting=false` + `post_commit_at` 기록.
- 실패 시 롤백:
  - Pre-snapshot 이후 teleport 실패 → 같은 좌표에 `teleport` 재시도 1회 → 실패 시 출발 좌표 유지 + `teleporting=false` + 로그.
  - Post-commit 전 서버 크래시 → 재기동 시 섹션 3.7 복구 파이프라인이 판정.

- **DB 스키마 (1차)**:
  ```sql
  CREATE TABLE transfer_txn (
      player_uuid TEXT PRIMARY KEY,
      teleporting INTEGER NOT NULL DEFAULT 0,
      src_world   TEXT, src_x REAL, src_y REAL, src_z REAL,
      dst_world   TEXT, dst_x REAL, dst_y REAL, dst_z REAL,
      started_at  INTEGER, post_commit_at INTEGER,
      reason      TEXT
  );
  ```

- 인벤토리·버프 전체 snapshot은 **1차 버전 범위 외**. 이관 전후 Paper 기본 인벤토리 영속성에 의존하고, 본 테이블은 **좌표 복원용 최소 정보**만 담는다.

### 3.7 크래시 복구 파이프라인

- 서버 재기동 시 `PluginEnable` 단계에서 `SELECT * FROM transfer_txn WHERE teleporting=1` 조회.
- 복귀 월드 판정 규칙 (우선순위 순):
  1. `dst_world`가 `estate_world`이고 **영지 점유 락(3.8) 검증 성공** → 영지 목적지로 진입 재시도.
  2. 1번 실패 또는 `dst_world`가 이관 대상 아님 → `src_world`·`src_x`·`src_y`·`src_z`로 복귀.
  3. 2번도 실패(예: `main_world` 스폰 영역 이슈) → 수도 기본 스폰으로 폴백 + 경고 로그 + 디스코드 알림(운영 수동 3단 원칙과 일관).
- 복원은 다음 **해당 플레이어 로그인 시점**에 실행하며, `PlayerJoinEvent`에서 1틱 지연 후 `Player#teleport` + `teleporting=false`.
- 복원 중 체력·허기 등은 건드리지 않는다(Paper 기본 영속에 위임).

### 3.8 영지 점유 락

- 키 = `estate_<owner_uuid>`. 값 = `(holder_session_id, holder_uuid, acquired_at, expires_at)`.
- 락 획득 조건: 영지 월드 진입 이관 요청 시, 해당 owner 영지의 락이 없거나 만료(현재 시각 > `expires_at`)면 획득. 만료되지 않은 락이 **같은 세션** ID면 통과, **다른 세션**이면 이관 거절(호출자에게 "영지가 다른 세션에 의해 사용 중" 메시지).
- 락 만료: `expires_at`은 기본 60초, **영지 내 플레이어 heartbeat(10초 주기)마다 +60초 갱신**.
- 락 양도: 주인이 영지 월드에서 이탈하는 순간 `expires_at` 즉시 `now()`로 갱신하여 해제. 방문자만 남은 경우 주인 이탈과 동시에 방문자 강제 수도 이관.

- **DB 스키마 (1차)**:
  ```sql
  CREATE TABLE estate_occupancy_lock (
      owner_uuid        TEXT PRIMARY KEY,
      holder_session_id TEXT NOT NULL,
      holder_uuid       TEXT NOT NULL,
      acquired_at       INTEGER NOT NULL,
      expires_at        INTEGER NOT NULL
  );
  ```

- 크로스 기기 방지(3.5)와의 상호작용: 세션 kick 발생 시 kick되는 세션이 보유하던 영지 락은 **kick 콜백에서 즉시 해제**. 해제 실패로 stale 락이 남아도 신규 세션은 `expires_at > now()` 검증에서 60초 내에 자연 해제된다.

---

## 4. 기술 리스크

1. **FAWE 도입 비용 vs 동기 스탬핑 TPS 경고**: FAWE 미도입 시 Lv5 6×6 청크 스키매틱 붙여넣기가 메인 스레드에서 100ms 이상 점유 가능. FAWE 도입 시 의존 플러그인 1개 증가 + FAWE 자체 업데이트 주기에 종속. **권장**: FAWE 도입, 단 **Lv1(2×2 청크)은 동기 스탬핑 폴백 경로 유지**해 FAWE 장애 시 최소 기능 유지.
2. **SQLite 동시성 한계**: `transfer_txn`·`estate_occupancy_lock`·`estate_slot`가 모두 단일 SQLite 파일에 올라가면 쓰기 경합 위험. 1차 버전은 동접 200명 이하 전제에서 문제 없으나, 300명 돌파 시 MySQL/PostgreSQL 이관 필요.
3. **Paper 1.21 커스텀 생성기 호환성**: `ChunkGenerator`는 안정 API지만 `generateNoise/Surface/Bedrock/Caves` 4개 모두 오버라이드가 필요하며, Paper 업데이트 시 메서드 시그니처 변경 가능성. 버전 픽스 후 수동 회귀.
4. **영지 월드 청크 언로드와 락의 경계**: 주인 오프라인 + 영지 청크 언로드됐을 때 락이 남아 있으면 `Bukkit.getWorld("estate_world").getChunkAt(slot).load()` 호출 순서가 꼬일 수 있다. 락 획득 → 청크 강제 로드(`chunk.setForceLoaded(true)`) → 이관 순서 강제.
5. **per-player WorldBorder 해제 타이밍**: 영지 → 수도 이관 시 per-player WorldBorder를 해제하지 않으면 수도에서도 112블록 경계가 남는 버그. 이관 트랜잭션 post-commit 단계에서 반드시 reset.
6. **크로스 기기 kick 레이스**: AsyncPreLoginEvent에서 동기 kick 발송 시 기존 세션의 `PlayerQuitEvent`가 완료되기 전에 신규 세션이 스폰될 수 있다. 타임아웃 3초 + 스폰 전 재확인 2중 게이트 필수.

---

## 5. 테스트 포인트

- **슬롯 알고리즘 유닛 테스트** (`SpiralSlotAllocatorTest`):
  - `fromIndex(0)` == (0, 0)
  - `fromIndex(1..8)` 8개가 링1(반지름 1) 8슬롯을 중복 없이 커버
  - `fromIndex(9..24)` 16개가 링2를 커버
  - 10만 인덱스까지 `(dx, dz)` 유일성 보장
- **보이드 생성기 통합 테스트**: 임시 월드 생성 → 청크 5개 load → 모든 블록이 `Material.AIR`인지 확인. 몹·구조물 0 확인.
- **이관 트랜잭션 롤백 테스트**: `TransferService` 목으로 teleport 강제 실패 → `teleporting=0` 복구 + 출발 좌표 유지 검증.
- **크래시 복구 테스트**: `transfer_txn.teleporting=1` 상태로 DB를 수동 세팅 → 서버 재기동 → 다음 로그인 시 `src_world` 복귀 검증.
- **영지 락 동시성 테스트**: 동일 owner UUID로 2스레드에서 `acquire` 호출 → 하나만 성공, 다른 하나는 `LockHeldException`.
- **크로스 기기 시나리오 E2E**: 테스트 서버에 동일 UUID 2세션 접속 → 기존 세션 kick + 신규 세션 정상 스폰 + 영지 락 소유권 이전.
- **FAWE 스탬핑 TPS 측정**: Lv5 스키매틱 동시 10건 붙여넣기 시 TPS 18 이상 유지.
- **per-player WorldBorder reset 회귀 테스트**: 영지 → 수도 이관 직후 수도에서 1000블록 이동 가능한지.

---

## 6. 1차 버전 권장안

1차 버전 범위는 "영지 1명이 영지에 들어가고, 스키매틱이 붙고, 수도로 안전히 돌아오고, 다른 기기에서 접속 시 기존 세션이 종료된다"까지로 한정한다.

**PR 분해 (10 PR 권장, 의존성 그래프 포함)**

| PR | 제목 | 범위 | man-day | 의존 |
|---|---|---|---|---|
| PR-W0 | Flag store 스키마 확장 | `transfer_txn`, `estate_slot`, `estate_occupancy_lock` 3 테이블 추가. 마이그레이션 스크립트. | 1 | 플래그 저장소 PR0/PR1 |
| PR-W1 | `WorldBootstrap` + `VoidChunkGenerator` | `com.poro.empire.world` 신규 패키지. `estate_world`·`experience_world` 부팅 훅. `plugin.yml` 생성기 등록. | 2 | PR-W0 |
| PR-W2 | `SpiralSlotAllocator` + DB 연동 | `com.poro.empire.estate.slot`. 알고리즘 + `SlotRepository`. JUnit 커버리지 80%. | 2 | PR-W0 |
| PR-W3 | FAWE 의존성 추가 + `EstateSchematicStamper` 스텁 | `build.gradle.kts`에 `compileOnly` 추가, `plugin.yml` `softdepend: FastAsyncWorldEdit`, 더미 스키매틱 1장. | 2 | PR-W1 |
| PR-W4 | `TransferService` + 이관 트랜잭션 | `com.poro.empire.transfer`. pre-snapshot → teleport → post-commit 3단. | 3 | PR-W0, PR-W1 |
| PR-W5 | 영지 점유 락 + heartbeat | `EstateOccupancyLockService`. 10초 heartbeat. stale 락 자동 해제. | 2 | PR-W0, PR-W4 |
| PR-W6 | 크로스 기기 방지 (A안) | `AsyncPlayerPreLoginEvent` 리스너. kick 대기 + stale session 무효화. | 2 | PR-W0 |
| PR-W7 | 크래시 복구 파이프라인 | `PluginEnable` → pending txn 스캔 → 다음 `PlayerJoinEvent`에서 복원. | 2 | PR-W4, PR-W5 |
| PR-W8 | per-player WorldBorder + 영지 리전 자동 생성 | WorldGuard API 호출로 `estate_<uuid>` 자동 생성. 경계 reset 훅. | 2 | PR-W1, PR-W2 |
| PR-W9 | `/영지` `/복귀` 명령어 + 이관 UX 연결 | Command executor. 페이드 연출 2초 훅(실 에셋은 후속). | 2 | PR-W4, PR-W5, PR-W8 |

**총 man-day: 약 20 md (1인 기준 약 4주, 2인 병렬 시 약 2~2.5주).**

**의존성 그래프**:

```
PR0/PR1(플래그 저장소)
    ↓
PR-W0
    ├─→ PR-W1 ─→ PR-W3, PR-W8
    ├─→ PR-W2 ─→ PR-W8
    ├─→ PR-W4 ─→ PR-W5, PR-W7, PR-W9
    └─→ PR-W6
              PR-W5 ─→ PR-W9
              PR-W7
```

**빌드 체인 영향**:
- `build.gradle.kts`: FAWE, WorldGuard API `compileOnly` 추가(PR-W3, PR-W8).
- `plugin.yml`: `softdepend`에 `FastAsyncWorldEdit`, `WorldGuard` 추가. `commands`에 `영지`/`복귀` 추가(PR-W9).
- 테스트: `testImplementation` 신규 없음. 기존 JUnit 5 유지.

**착수 조건**: 플래그 저장소 PR0/PR1 머지 완료 시 PR-W0부터 순차 착수 가능.

---

## 7. 나중으로 미뤄도 되는 것

- **실 스키매틱 에셋 Lv1~5 제작**: 본 에픽은 더미 스키매틱 1장으로 인터페이스 검증만. 실 에셋은 별도 콘텐츠 에픽.
- **Velocity/BungeeCord 프록시 대응 완성본**: 플러그인 메시징 채널 기반 크로스 노드 kick·락 동기화. 1차 버전은 DB 폴링 스텁만 둔다.
- **비활성 영지 아카이브 도구**(`poro_estate_chunk_load_review_v1.md` 리스크 2): 3~6개월 미접속 영지의 리전 파일 콜드 스토리지 이동. 운영 도구 별도 에픽.
- **인벤토리·버프 전체 snapshot**: 이관 트랜잭션의 좌표 복원으로는 부족한 극단 상황(서버 강제 종료 + 월드 파일 손상) 대응. 현 범위는 좌표 복원만.
- **영지 Lv2~5 업그레이드 시 스키매틱 교체 애니메이션**: Lv 업그레이드 시 기존 구조물 보존 + 확장 영역만 추가 스탬핑하는 로직. 1차는 Lv1 스탬핑만.
- **크로스 기기 프록시 대응의 플러그인 메시징 채널**: 단일 노드 운영 전제라 1차 범위 외.
- **Multiverse-Core 도입**: 본 에픽 범위에서는 거절. 관리자 수동 월드 관리 UI 요구가 생기면 재검토.
- **영지 공개 관람 모드 / 길드 영지**: 길드 시스템 전체 도입 시 재개.
- **per-player WorldBorder 연출 효과**: 경계 시각화 파티클·사운드. 1차는 기본 Paper WorldBorder만.

---

## 오픈 질문

1. **FAWE 도입 확정**: 본 문서는 FAWE 권장. 최종 확정 전이라면 PR-W3에서 WorldEdit(동기) 폴백으로 1차 릴리스 후 FAWE 이관 가능.
2. **`teleporting=1` 상태 플레이어가 크래시 후 30일 로그인 없음**: 플래그를 자동 만료할 것인지, 수동 운영 복구만 허용할 것인지.
3. **영지 락 heartbeat 주기 10초 적정성**: 더 짧게(5초)? 네트워크 부하 vs stale 락 최소화 트레이드오프.
4. **스키매틱 포맷 `.schem` 고정**: FAWE가 지원하는 `.schematic`(legacy)·`.schem`(sponge) 중 표준을 어느 것으로 잡을지. 권장 `.schem`.
5. **슬롯 시즌 경계 일괄 재활성화 타이밍**: 시즌 개시 시각에 대량 업데이트 배치를 어느 시점에 돌릴지(운영 공지 필요).

---

## 상위 문서 참조

- 기획 에픽: `poro_three_world_transition_design_draft.md`
- 체험 월드: `poro_experience_world_level_design_draft.md`
- 영지 3축 매트릭스: `../03_life_system_core/poro_estate_level_triaxial_matrix_draft.md`
- 청크 부하 검토: `../03_life_system_core/poro_estate_chunk_load_review_v1.md`
- 마스터플래닝: `../poro_master_planning.md`
- 플러그인 구조: `custom-plugins/empire-rpg/src/main/java/com/poro/empire/`

---

## 다음 추천 작업

- FAWE 도입 여부 최종 확정(오픈 질문 1).
- 플래그 저장소 PR0/PR1 머지 완료 확인 후 PR-W0 착수.
- 스키매틱 포맷 `.schem` 표준 확정 및 Lv1 더미 스키매틱 1장 제작 의뢰.
- data-schema 공동 검토: `transfer_txn`·`estate_occupancy_lock`·`estate_slot` 3 테이블 최종 스키마 리뷰.
