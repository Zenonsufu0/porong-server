# World Policy (월드 정책)

> 대상: PoroMon 데디케이티드 서버 (MC 1.21.1 / Fabric / Cobblemon 1.7.3 + Mega Showdown)
> 철학(overview.md / CLAUDE.md): **일반 플레이는 자유**, 고임팩트 시스템만 PoroMonCore가 통제.
> ⚠️ `TBD` = 아직 미정. 결정되면 본 문서와 `server.properties`를 함께 갱신한다.

## 1. 월드 생성

| 항목 | 값 | 메모 |
|---|---|---|
| 월드 타입 | `default` (일반 생성) | Cobblemon 스폰이 바닐라 바이옴 기반이므로 기본 생성 권장 |
| 시드(seed) | **TBD** | 공식 월드 확정 시 고정 시드 기록(재현/문서화 목적) |
| 차원 | 오버월드 + 네더 + 엔드 (전부 사용) | **차원 정책 = §1-2(결정 039)** |
| 구조물 생성 | 바닐라 기본 ON | 자연 마을 사용 허용(decisions 010 / CLAUDE.md) |

## 1-2. 차원 정책 (결정 039)

| 차원 | 경계(지름) | 정책 |
|---|---|---|
| 오버월드 | 5000 (중심 -15.5/755.5) | 자유 탐험. 야생 랜덤이동·전설 필드이벤트(결정038) 기준 차원 |
| 네더 | **5000**(÷8 비적용) | **고정 허브**(요새 인접·블레이즈/네더라이트)로 포탈 집결 + 플레이어별 오버월드 복귀 좌표. 블레이즈 스포너 파괴불가. → **네더 허브 시스템 = IB-005** |
| 엔드 | TBD | **드래곤전 없음 + 엔드시티(바깥섬) 탐험 가능**. 입장 허용·보스전 제거/무력화(구현 TBD). 엔드 전용 포켓몬/엘리트라/셸커 접근 |
| LM 커스텀(`hall_of_origin`·`distortion`) | — | **접근 차단**(결정 023/039). 입장 아이템 자연획득 차단 + 잔여 제작경로 점검(TODO) |

> 네더 1:8 포탈 매핑은 고정 허브 리다이렉트로 대체(IB-005). 전설 필드이벤트는 오버월드 한정.

## 2. 게임 규칙 (server.properties / gamerule)

| 키 | 값 | 근거 |
|---|---|---|
| `gamemode` | `survival` | 자유 생존형 탐험 |
| `difficulty` | `normal` | 적대 몹 + 포켓몬 공존. (hard는 추후 검토) |
| `hardcore` | `false` | — |
| `pvp` | **`false`** | **PvP 전면 비활성 (결정).** 인간 vs 인간 전투 off. 포켓몬 배틀은 Cobblemon이 별도 처리하므로 영향 없음 |
| `spawn-protection` | 허브/스폰 반경만 (값 TBD) | **스폰(허브) 건물만 보호 (결정).** non-op는 스폰 보호 반경 내 블록 변경 불가. 그 밖의 월드는 자유 |
| `allow-nether` / `allow-end` | `true` | 차원 탐험 허용 |
| `view-distance` | `8~10` (TBD) | Cobblemon 엔티티 부하 큼 → 과도하게 올리지 않음 |
| `simulation-distance` | `6~8` (TBD) | 스폰/AI 연산 범위. 성능 보며 조정 |
| `max-players` | TBD | 테스트 단계엔 소수 |
| `online-mode` | `true` | 정품 인증 (런처 배포 전제) |
| `keepInventory`(gamerule) | TBD | 사망 페널티 정책과 함께 결정 |

> PvP를 끄더라도 **리그/아레나에서의 대전은 Cobblemon 포켓몬 배틀**이라 `pvp` 플래그와 무관하게 동작한다. (CLAUDE.md의 "리그 PvP는 별도 통제"는 *포켓몬 대전 규칙* 차원으로 해석)

## 3. 토지 보호 / 그리핑 (요약 — 상세는 protection_policy.md)

- **보호 대상: 스폰 = 허브 건물만.** 허브(짐/제단/마켓/리그 시설 등)는 spawn-protection 또는 보호 영역으로 고정 보호.
- **일반 월드: 보호 없음(claim 모드 미도입).** 플레이어 건축물은 자유 영역에 짓는 구조 — 초기엔 보호 모드를 추가하지 않아 서버를 가볍게 유지.
  - 트레이드오프: 그리핑 리스크 존재. 초기 소규모/지인 테스트 단계라 허용. 공개 확장 시 재검토(`protection_policy.md`에서 결정).
- 상세 규칙(허브 보호 반경, op 권한, 향후 claim 도입 여부)은 **`docs/02_server/protection_policy.md`** 에서 다룬다(현재 TODO).

## 4. 포켓몬/월드 저장 정책 (Cobblemon)

`config/cobblemon/main.json` 현재 값 기준:
- `savePokemonToWorld: true` — 야생/필드 포켓몬을 월드에 저장
- `pokemonSaveIntervalSeconds: 30` — 30초 주기 저장
- `storageFormat: "nbt"` — NBT 저장 (MongoDB 미사용; `mongoDBConnectionString`은 로컬 기본값일 뿐 비활성)
- 디스폰: `despawner*` 설정으로 필드 포켓몬 정리 (Let Me Despawn 권장 모드와 함께 부하 관리)

> 월드 백업 시 **Cobblemon PC/파티 데이터(플레이어 NBT)와 월드가 정합**해야 하므로, 백업은 서버 정지 상태 또는 save-off 후 수행 (→ server_runbook.md).

## 4-1. 스폰 통제 (전설 / 관장·트레이너) — 결정 013
- **전설 자연 랜덤 스폰 off.** 전설은 조우권+사설 제단룸으로만(결정 008, `../04_game_design/legendary_encounter.md`).
  - 구현: Cobblemon 스폰 풀에서 전설 종 제거 — 스폰 설정/데이터팩 오버라이드 방식(정확한 키/파일 검증 필요). `enableSpawning`은 일반 스폰 유지를 위해 끄지 않음(전설만 선별 제거).
- **관장·트레이너 맵 스폰 없음.** 관장은 허브 전용 PoroMonCore NPC로만 존재(`../04_game_design/gym_badge_design.md`). 야생/필드 트레이너 스폰 없음.
- 검증: 서버 기동 후 일정 시간 관찰 + 스폰 로그로 전설 스폰 0 확인(`server_runbook.md` 체크).

## 5. 백업 / 보존

| 항목 | 정책 |
|---|---|
| 1순위 백업 대상 | `server/run/world` (+ 플레이어 데이터) |
| 주기 | 일 1회 + 재시작 직전 (TBD: 자동화는 `scripts/backup-server.sh`) |
| 방식 | save-off → 복사 → save-on, 또는 정지 중 복사 (정합성) |
| 보존 | 최근 N개 롤링 (N = TBD, 예: 7) |
| 저장 위치 | `server/backups/` |

## 6. 월드 리셋 / 시즌

- 기본: **영속 월드** (시즌마다 전체 와이프하지 않음).
- 시즌 콘텐츠(리그/챔피언/시즌 보상)는 **PoroMonCore의 진행/기록 리셋**으로 처리하는 방향 — 월드 지형 와이프와 분리.
- 전체 월드 리셋 정책(맵 교체/시즌 와이프 여부): **TBD** (게임 설계 `04_game_design/league_season_design.md`와 함께 결정).

---

## 미정(TBD) 모음 — 결정 필요
1. 공식 월드 시드 고정 여부
2. view/simulation-distance 최종값 (성능 테스트 후)
3. spawn-protection 반경 / 허브 보호 영역 크기
4. 사망 페널티(keepInventory) 정책
5. max-players, 백업 보존 개수
6. 시즌 시 월드 리셋 여부

## 관련 문서
- 보호 상세: `docs/02_server/protection_policy.md` (TODO)
- 운영 절차: `docs/02_server/server_runbook.md` (TODO)
- 서버 셋업: `docs/02_server/server_setup.md`
- 결정 기록: `docs/00_project/decisions.md`
