# Server Setup (.local/server 구성 계획)

> 대상: PoroMon 데디케이티드 서버 (MC 1.21.1 / Fabric Loader 0.19.3 / Java 21)
> 서버 실행 폴더: **`poromon/.local/server`** (worktree-local, Git 비추적 — RPG DL-130과 동치 표준)
> 모드 분리 기준: `docs/01_modpack/server_mod_separation.md`
> ⚠️ 본 문서는 **계획**이다. 아직 실제 jar 복사/서버 설치는 수행하지 않는다.

## 1. .local/server 디렉터리 구성

서버 실행 폴더는 `poromon/.local/server`이며 `.local/` 전체가 gitignored(런타임 비추적). 골격은 이미 존재(`.local/server/{mods,config,world,logs}` + `eula.txt` + `start.sh`). 각 폴더의 채울 내용:

```
.local/server/
├─ fabric-server-mc.1.21.1-loader.0.19.3-launcher.1.1.1.jar   # Fabric server 런처 (산출물, 비추적)
├─ mods/             # 서버 필수 + 권장 모드 jar만 (클라 전용 0개)
│                    #   ← scripts/sync-server-mods.sh 로 화이트리스트 동기화 (DEST=.local/server/mods)
├─ config/           # 런타임 적용 config (서버가 기동하며 읽고/쓰는 실파일)
│                    #   ← cobblemon/main.json, mega_showdown/config.json,
│                    #      lithium.properties 등 "서버 관련"만 modpack/overrides/config 에서 선별 복사
├─ world/            # 서버 월드 데이터 (오버월드+DIM-1+DIM1). 백업 대상 1순위
├─ logs/             # latest.log / 날짜별 로그. 기동/크래시 진단
├─ eula.txt          # eula=true  (없으면 서버 기동 거부)
└─ start.sh          # 서버 기동 스크립트 (cd .local/server → fabric-server*.jar nogui)
```

> ℹ️ 옛 `server/run/` 레이아웃은 폐지. `.local/server/run/`이 잔존하면 stale 잔재이며,
> start.sh가 `cd "$(dirname "$0")"`(=`.local/server`) 후 실행하므로 Fabric이 읽는 모드 폴더는 `.local/server/mods`다.

추가로 서버 기동 시 자동 생성되는 파일(수동 작성 불필요, 단 관리 대상):
- `server.properties` (포트/난이도/뷰디스턴스/시뮬레이션-디스턴스/online-mode 등)
- `whitelist.json`, `ops.json`, `banned-*.json`
- `libraries/`, `versions/` (런처가 받는 Minecraft/Fabric 라이브러리 — 비추적)

### 1-0. Fabric server 런처 배치 방법 (MC 1.21.1 / Loader 0.19.3)
1. **다운로드**: <https://fabricmc.net/use/server/> 에서 *Minecraft 1.21.1 + Loader 0.19.3 + Installer/Launcher*용 **Fabric server launcher jar**를 받는다.
   현재 배치된 산출물: `fabric-server-mc.1.21.1-loader.0.19.3-launcher.1.1.1.jar`.
   (CLI 대안: `java -jar fabric-installer.jar server -mcversion 1.21.1 -loader 0.19.3 -downloadMinecraft -dir <경로>`)
2. **배치 위치**: `poromon/.local/server/` 바로 아래에 둔다(=start.sh의 작업 디렉터리). 파일명이 바뀌어도 `start.sh`가 `fabric-server*.jar` 글롭으로 잡는다.
3. **EULA**: 같은 폴더 `eula.txt`에 `eula=true`.
4. **첫 기동**: `./start.sh` 실행 시 런처가 `libraries/`·`versions/`에 필요한 Minecraft/Fabric 파일을 자동 다운로드 후 서버를 띄운다. (※ 이번 작업에서는 아직 실행하지 않는다.)
- 이 런처 jar·libraries·versions·world·logs는 **모두 산출물 → Git 비추적**(`.local/` gitignored). 저장소에 커밋하지 않는다.

### 1-1. config 채우는 방침
- `modpack/overrides/config/` 에는 **클라+서버 config가 섞여** 있다.
  - 서버 관련(예): `cobblemon/`, `mega_showdown/`, `lithium.properties`, `bwncr.toml`, `letmedespawn.json`, `forgeconfigapiport.toml`, `balm-common.toml`
  - 클라 전용(서버 불필요): `sodium-*`, `iris.properties`, `xaero*`, `fancymenu/`, `emi.css`, `entity_texture_features.json` 등
- 따라서 config도 **모드 화이트리스트와 같은 원칙으로 선별 복사**한다. (mods만 거르고 config는 통째 복사하면 무의미한 클라 config가 섞임 → 정리 차원에서 선별 권장)
- `showdown/` 데이터(MSDPatch/sim/data)는 서버에서 MSD가 런타임 갱신하는지 확인 후, 필요 시 `.local/server/`(또는 모드가 지정한 경로)로 동기화.

### 1-2. defaultconfigs 의미
- Fabric 서버는 새 월드 생성 시 `defaultconfigs/`의 내용을 `config/`로 복사한다(지원 모드 한정).
- 운영상: 검증 끝난 서버 config 세트를 `defaultconfigs/`에 스냅샷해 두면, 월드 초기화/신규 시즌 때 동일 설정으로 재현 가능.

## 2. eula.txt
```
eula=true
```
- Mojang EULA 동의. 파일 없거나 false면 기동 즉시 종료.

## 3. start.sh (현재 `.local/server/start.sh`에 배치됨 — 런타임 산출물, 비추적)
- 위치: `.local/server/start.sh`. `cd "$(dirname "$0")"`로 작업 디렉터리를 `.local/server`로 고정.
- 현재 내용(최소판):
  ```bash
  #!/usr/bin/env bash
  cd "$(dirname "$0")"
  java -Xms2G -Xmx6G -jar fabric-server*.jar nogui
  ```
- 향후 튜닝 후보(필요 시 운영자가 직접 적용 — 런타임 파일이라 저장소 비추적):
  - JVM 메모리: `-Xms4G -Xmx6G` (테스트 환경 기준, 조정)
  - GC: Aikar's Flags(G1GC 튜닝) — 대규모 엔티티(포켓몬) 환경에 유리
  - 비정상 종료 시 재시작 루프는 **초기 테스트 단계에선 넣지 않음**(크래시 원인 파악 우선)

---

# PoroMonCore 개발 전 선행 조건

PoroMonCore(커스텀 Fabric 모드)는 **서버가 먼저 안정적으로 떠야** 개발/테스트 가치가 있다.
아래 순서를 모두 통과한 뒤에 `custom-mods/poromon-core` Gradle 프로젝트 생성으로 넘어간다.

> ⚠️ 본 단계에서는 PoroMonCore Gradle 프로젝트를 아직 만들지 않는다.

### 단계 0. 환경
- [ ] Java 21 (`java -version`)
- [ ] Fabric **server** 설치 (Loader 0.19.3 / MC 1.21.1)
- [ ] `eula.txt = true`, `.local/server` 구성 완료

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
- [ ] jar를 `.local/server/mods`에 투입 → 서버 기동 시 PoroMonCore 로드 로그 확인
- [ ] Cobblemon/MSD와 동시 로드 충돌 없음
- [ ] 여기까지 통과해야 `/poromon` 등 0.1 스코프 구현 착수

---

## 관련 문서
- 모드 분리: `docs/01_modpack/server_mod_separation.md`
- 동기화 스크립트(초안): `scripts/sync-server-mods.sh`
- PoroMonCore 스펙: `docs/03_poromoncore/poromoncore_spec.md`
- 운영 런북: `docs/02_server/server_runbook.md` (TODO)
