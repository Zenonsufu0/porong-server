# Server Runbook (운영 절차)

> 대상: PoroMon 데디케이티드 서버 (MC 1.21.1 / Fabric / Java 21)
> 작업 디렉터리: `server/run` · 백업: `server/backups`
> 관련: `server_setup.md`(구성) · `world_policy.md`(백업 정책) · `protection_policy.md`(롤백 분쟁)
> ⚠️ 절차에 등장하는 스크립트(`start.sh`, `scripts/backup-server.sh` 등)는 **아직 미작성**. 본 문서는 그 동작 규약(runbook)을 정의한다.

## 0. 사전 점검 (최초 1회 / 환경 변경 시)
- [ ] `java -version` → 21
- [ ] `server/run/eula.txt` = `eula=true`
- [ ] Fabric server (Loader 0.18.4 / MC 1.21.1) 설치됨
- [ ] `server/run/mods` = 서버 필수+권장만 (클라 전용 0개 — `server_mod_separation.md`)
- [ ] `server.properties`: `pvp=false`, `spawn-protection=<허브반경>` (결정 011/012)

## 1. 기동 (Start)
```bash
cd /home/zenonsufu1/dev/poro-server-poromon/server/run
./start.sh        # 또는 scripts/run-server.sh
```
정상 신호:
- 콘솔에 `Done (xx.xxxs)! For help, type "help"`
- 로그에 Cobblemon 1.7.3 + Fabric Language Kotlin(JIJ) 로드
- `Missing dependency` / `requires` 경고 없음

비정상이면 → §6 장애 진단.

## 2. 정지 (Stop)
- 콘솔에서 **`stop`** 입력 (절대 `kill -9` 우선 사용 금지 — 월드 손상 위험).
- 정지 시 월드 자동 저장 확인 로그(`Saving worlds` / `Saved the game`).
- 응답 없을 때만: 콘솔 살아있으면 `stop` 재시도 → 그래도 무응답 시 마지막 수단으로 프로세스 종료(이 경우 §5 정합성 점검).

## 3. 저장 / 정합성 (Save)
- 수동 저장: 콘솔 `save-all flush`
- 백업 전 권장 시퀀스: `save-off` → `save-all flush` → (복사) → `save-on`
  - Cobblemon은 `pokemonSaveIntervalSeconds: 30`(자동 30초) + 플레이어 NBT에 파티/PC 저장 → **백업은 월드와 플레이어 데이터가 같은 시점이어야** 정합. save-off로 쓰기 잠근 뒤 복사.

## 4. 백업 (Backup)
정책(요약, 상세 `world_policy.md` §5):
- 1순위 대상: `server/run/world` (+ 플레이어 데이터)
- 주기: 일 1회 + **재시작 직전**
- 저장: `server/backups/` , 최근 N개 롤링(N=TBD, 예 7)

수동 절차(스크립트화 전):
```bash
# 1) 서버 콘솔에서
save-off
save-all flush
# 2) 호스트 쉘에서 (정지 상태면 1) 생략 가능)
TS=$(date +%Y%m%d-%H%M%S)
tar czf server/backups/world-$TS.tar.gz -C server/run world
# 3) 서버 콘솔에서
save-on
```
> `scripts/backup-server.sh` 최종본은 위 시퀀스 + 롤링 삭제(오래된 것부터)를 구현 (현재 빈 파일, TODO).

## 5. 롤백 / 복구 (Restore)
사용 시점: 그리핑 분쟁(보호 안 된 일반 월드), 월드 손상, 비정상 종료 후 깨짐.
```bash
# 1) 서버 정지 (stop)
# 2) 현재 월드 안전 보관 (덮어쓰기 전 보존)
mv server/run/world server/run/world.broken-$(date +%Y%m%d-%H%M%S)
# 3) 백업 복원
tar xzf server/backups/world-<TS>.tar.gz -C server/run
# 4) 기동 후 확인
```
주의:
- **부분 롤백 불가**(월드 단위). 특정 플레이어만 복구하려면 해당 player NBT만 발췌(고급, 정합 주의).
- Cobblemon PC/파티가 player NBT에 있으므로, 월드만 되돌리면 포켓몬 데이터도 함께 되돌아감 → 플레이어에게 사전 고지.

## 6. 장애 진단 (Crash / Troubleshooting)
1. **로그 위치**: `server/run/logs/latest.log`, 크래시 시 `server/run/crash-reports/`.
2. **모드 의존성 누락** (`requires X` / `Missing dependency`):
   - 해당 라이브러리만 `server_mod_separation.md` §4(애매)에서 골라 추가. 무지성 전체 복사 금지.
3. **클라 전용 모드가 서버에 섞임** (Sodium/Iris/EMI 등 `@Environment(CLIENT)` 관련 NoClassDefFound/RuntimeException):
   - `server/run/mods`에서 해당 jar 제거. DENY 키워드 점검(`server_mod_separation.md` §3).
4. **Cobblemon/MSD 관련**:
   - Showdown 데이터 경로/초기화 실패 → `overrides/showdown/` 동기화 여부 확인(`server_setup.md` §1-1).
   - 버전 불일치(Cobblemon 1.7.3 ↔ MSD ↔ MC 1.21.1) 확인.
5. **클라 접속 거부(mod mismatch)**:
   - 서버가 클라 전용 모드를 요구하면 안 됨. 서버 mods에서 클라 모드 제거.
6. **포트 점유** (`Address already in use`): 이전 프로세스 잔존 → 종료 후 재기동.
7. 원인 미상 → 재현 로그 + crash-report를 `docs/05_operations/known_issues.md`에 기록.

## 7. 업데이트 절차 (모드/팩 갱신)
1. **백업 먼저**(§4).
2. 새 CurseForge export → `scripts/extract-curseforge-pack.sh <zip>` 로 `modpack/` 갱신.
3. `manifest.json`/`modlist.html` diff로 변경 모드 파악 → `server_mod_separation.md` 재검증.
4. `scripts/sync-server-mods.sh`(초안)로 `server/run/mods` 동기화.
5. config 변경분 반영(`server/run/config`), 필요 시 `defaultconfigs` 스냅샷 갱신.
6. 테스트 기동(§1) → `server_mod_separation.md` §5 체크리스트.
7. 문제없으면 운영 반영, 문제 시 롤백(§5) + 모드 되돌림.

## 8. 정기 운영 체크
- [ ] 일일: 백업 생성 확인, `latest.log` ERROR 스캔, TPS/메모리 확인
- [ ] 주간: 백업 롤링 정상(오래된 것 정리), 디스크 여유
- [ ] 변경 시: 결정/정책 문서 갱신(`decisions.md`, 해당 정책 md)

---

## 9. 최초 오픈 셋업 (알파 전 1회)

> 런타임/시크릿 작업. **여기 적힌 값(특히 apiKey)은 Git에 커밋하지 않는다.** 대상 파일은 `config/poromoncore/core.json`(런타임).

### 9-1. 디스코드 인증 (결정 041)
1. **apiKey 설정** — `core.json → discordAuth.apiKey`를 `CHANGE_ME`에서 강한 랜덤값으로 변경(예: `python3 -c "import secrets;print(secrets.token_urlsafe(32))"`).
   - ⚠️ **봇 측(porong-discord)의 인증 API 키와 반드시 동일 값.** 봇 `.env`의 포로몬 인증 키 변수에 같은 값.
   - `bindAddress=127.0.0.1`(봇·MC 같은 호스트) 유지, 원격이면 `0.0.0.0`+방화벽. `httpPort=25580`(봇 base URL과 일치).
2. **엔드포인트 확인**(서버 기동 후):
   - `curl -s -H "X-API-Key: <key>" http://127.0.0.1:25580/auth/ping` → `{"ok":true,"service":"poromon-auth"}`
   - 잘못된 키 → 401, 없는 코드로 verify → 404 (헤드리스 검증 항목).
3. **흐름 점검(알파)**: 인게임 `/인증` → 6자리 코드 → 봇에 입력 → 역할 부여 + 허브 감금/메뉴 잠금 해제.

### 9-2. 허브 spawn (오버월드)
- `core.json → hub.spawn{ x, y, z, yaw, pitch }`. **인게임 sethub 명령 없음 → JSON 직접 편집.**
- 정책(`protection_policy.md`): **월드 스폰 = 허브 중심**. 빌드 후 실좌표로 맞추고 `/setworldspawn <x> <y> <z>`로 정렬, `spawn-protection`(server.properties)을 허브 반경으로.
- 기본값 `0.5/64/0.5`는 월드 스폰(0,0) 플레이스홀더 — 실제 빌드 바닥 Y/방향으로 갱신.

### 9-3. 네더 허브 (결정 039-b/c)
- op로 1회: `/poromon admin netherhub`(자동 위치) 또는 `/poromon admin netherhub here`(현재 위치). → 플랫폼+벽+귀환포탈+블레이즈 스포너 자동 건설.

### 9-4. 엔드 (결정 039-e)
- **자동.** 명령/건설 불필요(입장 시 드래곤 부재 + 무작위 외곽 섬 착지).

### 9-5. 데이터팩 활성 확인
- `datapack list` → `poromon_lm_control`(전설 차단)·`poromon_mega_control`(메가스톤 차단)·`poromon_battletower_test` 활성 확인.
- 차단 검증: `/locate structure mega_showdown:mega_site` **실패**(생성 차단) / LM 구조물도 동일.

### 9-6. 정합 점검
- [ ] 상점이 메가스톤47·키스톤·메가팔찌를 골드로 지급(차단 후 유일 경로 — 결정 042).
- [ ] `core.json` apiKey ≠ `CHANGE_ME` (서버 기동 시 기본값이면 경고 로그).

---

## 스크립트 현황 (TODO)
| 스크립트 | 상태 | 할 일 |
|---|---|---|
| `scripts/run-server.sh` / `server/run/start.sh` | 빈 파일 | JVM 옵션 + Fabric launcher 기동 |
| `scripts/backup-server.sh` | 빈 파일 | §4 시퀀스 + 롤링 삭제 |
| `scripts/sync-server-mods.sh` | 초안(DRY_RUN) | SOURCE_DIR 확정 + 화이트리스트 검증 |
| `scripts/extract-curseforge-pack.sh` | 작동 | — |

## 관련 문서
- 구성: `server_setup.md` · 월드: `world_policy.md` · 보호: `protection_policy.md`
- 모드 분리: `../01_modpack/server_mod_separation.md`
- 알려진 이슈: `../05_operations/known_issues.md` (TODO)
