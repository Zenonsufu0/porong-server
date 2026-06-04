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
