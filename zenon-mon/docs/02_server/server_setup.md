# Server Setup (server/run 구성 계획)

> 대상: PoroMon 데디케이티드 서버 (MC 1.21.1 / Fabric Loader 0.18.4 / Java 21)
> 모드 분리 기준: `docs/01_modpack/server_mod_separation.md`
> ⚠️ 본 문서는 **계획**이다. 아직 실제 jar 복사/서버 설치는 수행하지 않는다.

## 1. server/run 디렉터리 구성

현재 골격은 이미 존재(`server/run/{mods,config,defaultconfigs,world,logs}`). 각 폴더의 채울 내용:

```
server/run/
├─ mods/             # 서버 필수 + 권장 모드 jar만 (클라 전용 0개)
│                    #   ← scripts/sync-server-mods.sh 로 화이트리스트 동기화
├─ config/           # 런타임 적용 config (서버가 기동하며 읽고/쓰는 실파일)
│                    #   ← cobblemon/main.json, mega_showdown/config.json,
│                    #      lithium.properties 등 "서버 관련"만 modpack/overrides/config 에서 선별 복사
├─ defaultconfigs/   # 새 월드 최초 생성 시 config 로 복사되는 "기본값" 스냅샷
│                    #   ← 서버 기준 config 의 초기본 보관(설정 리셋/신규월드 대비)
├─ world/            # 서버 월드 데이터 (오버월드+DIM-1+DIM1). 백업 대상 1순위
├─ logs/             # latest.log / 날짜별 로그. 기동/크래시 진단
├─ eula.txt          # eula=true  (없으면 서버 기동 거부)
└─ start.sh          # 서버 기동 스크립트 (Fabric server launcher + JVM 옵션)
```

추가로 서버 기동 시 자동 생성되는 파일(수동 작성 불필요, 단 관리 대상):
- `server.properties` (포트/난이도/뷰디스턴스/시뮬레이션-디스턴스/online-mode 등)
- `whitelist.json`, `ops.json`, `banned-*.json`
- `fabric-server-launch.jar` 또는 Fabric loader 런처 jar (설치 산출물)

### 1-1. config 채우는 방침
- `modpack/overrides/config/` 에는 **클라+서버 config가 섞여** 있다.
  - 서버 관련(예): `cobblemon/`, `mega_showdown/`, `lithium.properties`, `bwncr.toml`, `letmedespawn.json`, `forgeconfigapiport.toml`, `balm-common.toml`
  - 클라 전용(서버 불필요): `sodium-*`, `iris.properties`, `xaero*`, `fancymenu/`, `emi.css`, `entity_texture_features.json` 등
- 따라서 config도 **모드 화이트리스트와 같은 원칙으로 선별 복사**한다. (mods만 거르고 config는 통째 복사하면 무의미한 클라 config가 섞임 → 정리 차원에서 선별 권장)
- `showdown/` 데이터(MSDPatch/sim/data)는 서버에서 MSD가 런타임 갱신하는지 확인 후, 필요 시 `server/run/`(또는 모드가 지정한 경로)로 동기화.

### 1-2. defaultconfigs 의미
- Fabric 서버는 새 월드 생성 시 `defaultconfigs/`의 내용을 `config/`로 복사한다(지원 모드 한정).
- 운영상: 검증 끝난 서버 config 세트를 `defaultconfigs/`에 스냅샷해 두면, 월드 초기화/신규 시즌 때 동일 설정으로 재현 가능.

## 2. eula.txt
```
eula=true
```
- Mojang EULA 동의. 파일 없거나 false면 기동 즉시 종료.

## 3. start.sh 설계 방향 (초안 — 아직 미작성)
- 위치: `server/run/start.sh` (또는 `scripts/run-server.sh`가 `server/run`을 작업 디렉터리로 호출)
- 포함 요소:
  - Java 21 경로 확인
  - 작업 디렉터리 = `server/run`
  - JVM 메모리: `-Xms4G -Xmx6G` (테스트 환경 기준, 조정)
  - GC: Aikar's Flags 권장(G1GC 튜닝) — 대규모 엔티티(포켓몬) 환경에 유리
  - Fabric server launcher jar 실행 + `nogui`
  - 비정상 종료 시 재시작 루프는 **초기 테스트 단계에선 넣지 않음**(크래시 원인 파악 우선)
- 예시 골격(작성 시):
  ```bash
  #!/usr/bin/env bash
  set -euo pipefail
  cd "$(dirname "$0")"
  java -Xms4G -Xmx6G \
    -XX:+UseG1GC <Aikar flags...> \
    -jar fabric-server-launch.jar nogui
  ```

---

# PoroMonCore 개발 전 선행 조건

PoroMonCore(커스텀 Fabric 모드)는 **서버가 먼저 안정적으로 떠야** 개발/테스트 가치가 있다.
아래 순서를 모두 통과한 뒤에 `custom-mods/poromon-core` Gradle 프로젝트 생성으로 넘어간다.

> ⚠️ 본 단계에서는 PoroMonCore Gradle 프로젝트를 아직 만들지 않는다.

### 단계 0. 환경
- [ ] Java 21 (`java -version`)
- [ ] Fabric **server** 설치 (Loader 0.18.4 / MC 1.21.1)
- [ ] `eula.txt = true`, `server/run` 구성 완료

### 단계 1. Fabric 서버 단독 정상 구동
- [ ] mods 비우거나 최소 라이브러리만으로 `Done` 출력 (바닐라+Fabric 부팅 확인)
- [ ] 크래시/포트 충돌 없음

### 단계 2. Cobblemon 서버 로딩 확인
- [ ] 서버 필수 모드(1번) 투입 후 기동
- [ ] Cobblemon 1.7.3 초기화 + Kotlin(JIJ) 로드 로그
- [ ] 클라 접속 후 야생 스폰/포획/PC 동작

### 단계 3. Mega Showdown 서버 로딩 확인
- [ ] MSD 로드 + `mega_showdown/config.json` 적용
- [ ] 배틀에서 메가/테라 기믹이 서버측 처리됨
- [ ] showdown 데이터 경로 이슈 없음

### 단계 4. 클라이언트 접속 확인
- [ ] PoroMon 클라 모드팩으로 접속 성공
- [ ] 서버가 클라 전용 모드를 요구하지 않음(모드 불일치 거부 없음)
- [ ] 전투/동기화 정상, 5~10분 TPS 안정

### 단계 5. (이후) PoroMonCore 빈 모드 로딩 테스트
- [ ] `custom-mods/poromon-core` Gradle 골격 생성 (→ 별도 작업)
- [ ] 빈 `ModInitializer` 빌드 → jar 산출
- [ ] jar를 `server/run/mods`에 투입 → 서버 기동 시 PoroMonCore 로드 로그 확인
- [ ] Cobblemon/MSD와 동시 로드 충돌 없음
- [ ] 여기까지 통과해야 `/poromon` 등 0.1 스코프 구현 착수

---

## 관련 문서
- 모드 분리: `docs/01_modpack/server_mod_separation.md`
- 동기화 스크립트(초안): `scripts/sync-server-mods.sh`
- PoroMonCore 스펙: `docs/03_poromoncore/poromoncore_spec.md`
- 운영 런북: `docs/02_server/server_runbook.md` (TODO)
