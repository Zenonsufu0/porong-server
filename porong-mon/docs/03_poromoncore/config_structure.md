# PoroMonCore Config Structure (설정 파일 구조)

> `ConfigManager`가 읽고/리로드하는 설정 파일 정의. 상위: `module_structure.md` §3.
> 원칙(CLAUDE.md): **밸런스/규칙 값 하드코딩 금지** → 전부 config. **서버 권위** → 서버만 읽고 적용.
> ⚠️ 아직 코드/파일 미생성. 본 문서는 구현 기준 스키마.

## 1. 위치 / 포맷
- 경로: `server/run/config/poromoncore/` (모드 id = `poromoncore`)
- 포맷: **JSON** (Cobblemon `main.json`, MSD `config.json`과 통일)
- 인코딩: UTF-8. 주석 불가(JSON) → 키 네이밍으로 자기설명.
- 기본값: 파일이 없으면 `ConfigManager`가 **기본값으로 생성**. `defaultconfigs/poromoncore/`에 스냅샷 보관(신규 월드/리셋 시 복사).

## 2. 파일 구성

| 파일 | 0.1 | 내용 |
|---|:--:|---|
| `core.json` | ✅ | 전역 토글, League Pass 아이템, 허브 텔레포트, 로깅 |
| `tickets.json` | ✅ | 인카운터 티켓 타입 정의 |
| `rooms.json` | ✅ | 사설 인카운터 룸 풀 정의 |
| `rewards.json` | ❌ 향후 | 보상 테이블 |
| `gyms.json` | ❌ 향후 | 짐/배지 정의 |
| `unlocks.json` | ❌ 향후 | 메가/테라/다이맥스 해금 조건 |
| `legendary_pools.json` | ✅ 구현 | 조우권 등급·필드 이벤트·컨셉 10종 풀 (§8). 설계 드래프트=`legendary_pools.draft.yml` |
| `legendary_events.json` | ❌ 향후 | 하급+중급 2시간 필드 이벤트 설정 (§8) |
| `seasons.json` | ❌ 향후 | 시즌/리그 설정 |

> ✅ **포맷 = JSON 통일(확정):** 실제 구현은 JSON 100%(`ConfigManager`=Gson만, SnakeYAML/Jackson 미사용). `core.json`·`economy.json`·`legendary_pools.json`·`seasons.json` 전부 JSON 로드. 위 표의 `*.yml` 표기는 **설계 드래프트(docs)일 뿐 런타임은 JSON**으로 번들·로드한다(예: `legendary_pools.draft.yml` → `legendary_pools.json`). 혼용 파서 불필요. (전설 풀/이벤트도 JSON으로 통일.)

> 0.1은 `core.json` + `tickets.json` + `rooms.json` **스키마만 확정**하고 일부는 빈/예시 값으로 시작.

## 3. core.json (0.1)
```jsonc
{
  "configVersion": 1,
  "menuItem": {
    "enabled": true,
    "itemId": "minecraft:clock",        // League Pass 베이스 아이템(임시)
    "displayName": "리그 패스",
    "hotbarSlot": 8,                      // 0-based, 9번 슬롯=인덱스 8
    "giveOnFirstJoin": true,
    "restoreOnJoin": true,
    "restoreOnRespawn": true,
    "preventDrop": true,
    "lockSlot": false                     // true면 슬롯 이동 잠금
  },
  "hub": {
    "world": "minecraft:overworld",
    "spawn": { "x": 0.5, "y": 64.0, "z": 0.5, "yaw": 0.0, "pitch": 0.0 },
    "teleportCommandEnabled": true
  },
  "logging": {
    "auditEnabled": true,
    "logTicketUse": true,
    "logRoomAssign": true,
    "logRewardGrant": true,
    "logAdminCommand": true
  }
}
```

## 4. tickets.json (0.1 — 데이터 모델 우선)
```jsonc
{
  "configVersion": 1,
  "ticketTypes": [
    {
      "id": "legendary_basic",
      "displayName": "전설 조우권",
      "consumedOnUse": true,
      "roomPool": "legendary_default",   // rooms.json 의 풀 id 참조
      "encounterTable": "legendary_default" // legendary.json(향후) 참조
    }
  ]
}
```

## 5. rooms.json (0.1 — 데이터 모델 우선)
```jsonc
{
  "configVersion": 1,
  "roomPools": [
    {
      "id": "legendary_default",
      "world": "minecraft:overworld",
      "instances": [
        { "id": "room_1", "min": {"x":1000,"y":100,"z":1000}, "max": {"x":1031,"y":131,"z":1031},
          "entry": {"x":1015.5,"y":101.0,"z":1015.5,"yaw":0.0,"pitch":0.0} }
      ],
      "cleanupOnExit": true,
      "maxSessionSeconds": 600
    }
  ]
}
```
> 좌표/룸은 **예시**. 실제 허브 빌드 확정 후 값 채움. InstanceRoomManager가 `instances`에서 빈 방 배정.

## 6. 전설 조우 설정 (향후 — `legendary_pools.yml` / `legendary_events.yml`)

> 풀 설계: `../04_game_design/encounter_pool_design.md`. ✅ **species ID 검증 완료** → 실 ID + 초기 가중치 드래프트: **`legendary_pools.draft.yml`**(결정 026, 16풀 전수, YAML/오타/동기화 검증 통과). 아래 §6.2는 **스키마 예시**(전체 데이터는 드래프트 파일 참조).

### 6.1 풀 구분 (legendary_pools.yml)
일반 5등급 + 필드 이벤트 + 컨셉 10종:
```
rare_encounter_pool
basic_legendary_ticket_pool
intermediate_legendary_ticket_pool
advanced_legendary_ticket_pool
apex_legendary_ticket_pool
field_event_legendary_pool
theme_sky_pool · theme_deep_sea_pool · theme_earth_pool · theme_time_pool · theme_space_pool
theme_reverse_pool · theme_light_pool · theme_dragon_king_pool · theme_guardian_pool · theme_eternity_pool
```

### 6.2 legendary_pools.yml (예시 구조)
```yml
legendary_pools:
  # 희귀(비전설): 진화 단계 가중 기본 70 / 중간 20 / 최종 10
  rare_encounter_pool:
    type: "rare"
    stage_weight: { basic: 70, middle: 20, final: 10 }
    candidates:
      - species: "TODO_gible"          # ⚠️ 실제 Cobblemon ID 확인 필요
        display_name_ko: "딥상어동"
        line_name_ko: "한카리아스 계열"
        stage: "basic"
        weight: 10
        enabled: true
      - species: "TODO_garchomp"
        display_name_ko: "한카리아스"
        line_name_ko: "한카리아스 계열"
        stage: "final"
        weight: 1
        enabled: true

  field_event_legendary_pool:          # 하급+중급 통합 (필드 이벤트)
    type: "field_event"
    basic_weight_ratio: 70
    intermediate_weight_ratio: 30
    candidates:
      - species: "TODO_articuno"
        display_name_ko: "프리져"
        tier: "basic"
        weight: 10
        biome_hint_ko: "설원"
        enabled: true
      - species: "TODO_latios"
        display_name_ko: "라티오스"
        tier: "intermediate"
        weight: 3
        biome_hint_ko: "하늘"
        enabled: true

  apex_legendary_ticket_pool:          # 최상급 전용 (중급/상급 미등장)
    type: "apex"
    display_name_ko: "최상급 전설 조우권"
    candidates:
      - species: "TODO_rayquaza"
        display_name_ko: "레쿠쟈"
        tier: "apex"
        weight: 10
        enabled: true

  theme_sky_pool:                      # 컨셉권: 중급55/상급35/최상급10
    type: "theme"
    display_name_ko: "하늘 조우권"
    tier_weight: { intermediate: 55, advanced: 35, apex: 10 }
    candidates:
      - species: "TODO_latios"
        display_name_ko: "라티오스"
        tier: "intermediate"
        weight: 20
        enabled: true
      - species: "TODO_rayquaza"       # 최상급 라인 → apex 풀에도 포함(동기화)
        display_name_ko: "레쿠쟈"
        tier: "apex"
        weight: 5
        enabled: true

  theme_eternity_pool:
    type: "theme"
    display_name_ko: "영원 조우권"
    enabled: false                     # 아르세우스 미존재/후반 → 기본 잠금
    tier_weight: { intermediate: 55, advanced: 35, apex: 10 }
    candidates:
      - species: "TODO_arceus"
        display_name_ko: "아르세우스"
        tier: "apex"
        weight: 1
        enabled: false
```
> 가격대(확정): 희귀 < 하급 < 중급 < 상급 < **컨셉권** < 최상급. 컨셉권 `apex` 후보는 `apex_legendary_ticket_pool`에도 **반드시 포함**(불일치 방지 — `encounter_pool_design.md` §10-6).

### 6.3 legendary_events.yml (하급+중급 필드 이벤트)
```yml
legendary_events:
  field_legendary_event:
    enabled: true
    interval_minutes: 120
    min_online_players: 3
    despawn_minutes: 20
    startup_delay_minutes: 30
    max_active_events: 1
    pool: "field_event_legendary_pool"
    announce_hint: true
    reveal_exact_coordinates: false
    battle_despawn_grace_minutes: 5
```

> 연동: `LegendaryEventSpawnManager`(이벤트 스케줄/공지/디스폰), `EncounterPoolManager`(풀 로드/가중), `LegendaryEncounterManager`(개인방 소환). `module_structure.md` 참고.

## 7. 리로드 / 검증
- 리로드: `/poromon admin reload` → `ConfigManager.reload()` 전체 재로드(파일별 부분 리로드는 향후).
- 검증: 로드 시 (a) `configVersion` 확인 → 구버전이면 마이그레이션/경고, (b) 참조 무결성(`ticket.roomPool` 가 `rooms.json`에 존재?), (c) 잘못된 값은 기본값 폴백 + 경고 로그.
- 핫 적용 한계: 아이템 등록 등 레지스트리성 변경은 리로드로 못 바꿈 → 재시작 필요(문서·명령 응답에 명시).

## 8. 관련
- 모듈: `module_structure.md` (ConfigManager 초기화 순서 1번)
- 데이터(런타임 상태 저장 ≠ config): `database_schema.md`
- 명령: `commands.md` (`/poromon admin reload`)
- defaultconfigs 운영: `../02_server/server_setup.md` §1-2
