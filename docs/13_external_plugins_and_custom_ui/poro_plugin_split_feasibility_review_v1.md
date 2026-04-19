# 포로 서버 EmpireRPG 플러그인 분할 타당성 검토 v1

> **상태 (2026-04-19 사용자 결정 4건 일괄)**: 권장안 수용하되 주도권 원칙 강화로 일부 조정.
> - **(a)** "EmpireRPG 단독" = **"empire-rpg가 다른 플러그인에서 모든 주도권을 잡고 간다"**로 정의 확정. 마스터 플래닝 line 13·180·1135 문구 "EmpireRPG가 주도권 단독 보유"로 미세 수정 완료.
> - **(b)** 착수 시점 = 플래그 저장소 **PR4~PR8 머지 후**.
> - **(c)** `empire-common` = **라이브러리 유지**(플러그인 승격하면 주도권 분산). 3단계 "플러그인 승격" = **영구 보류**.
> - **(d)** 생활 서버 개설 시급성 = **낮음**. 4·5단계는 **후순위** — 생활 서버 요구 발생 시 재개.
> - **실 진행 로드맵 축소판**: 1단계(Gradle multi-module) + 2단계(empire-common 서브모듈 분리 / 라이브러리 형태) 2개만 우선 진행 총 2~3 md. 3~5단계 보류.

## 문서 목적
단일 `empire-rpg` 플러그인을 테마별 JAR(RPG / 생활 / 보스 등)로 나누는 방안을 실제 코드베이스 실측에 기반해 검토한다. 사용자(2026-04-19) 요구:

1. 재활용성 — 생활 전용 서버에서 `empire-life`만 배포 가능하게
2. 용량 과다 위험 해소
3. 오류 발생 시 `disablePlugin()` 연쇄 차단
4. **우선순위**: 전체 분할이 부담이면 **RPG + 생활 2분할 먼저**

본 문서는 타당성 검토 v1이며 실제 분할 PR은 사용자 승인 후 별도 분해한다.

---

## 1. 목표
- 현 `empire-rpg` 단일 JAR을 **재활용 가능한 JAR 집합**으로 전환한다.
- "EmpireRPG(Custom Plugin) 단독 판정" 원칙은 유지한다 — 단, 이를 **"판정 주체 = 우리 커스텀 플러그인 집합(BetonQuest 등 외부 판정 엔진 아님)"** 으로 재해석한다. JAR 수와 무관한 원칙이다.
- **생활 프로젝트 서버에서 `empire-life + empire-common`만으로 기동**을 목표 시나리오로 삼는다.

---

## 2. 선행조건
- 플래그 저장소 v0.1 PR4 이후 분할 착수 권장(공통 라이브러리 경계가 플래그 저장소 API로 확정된 뒤에 분리하는 편이 리팩터링 범위가 작다).
- 3월드 전환(PR-W0~W9) 미착수 구간이라 분할이 월드 전환보다 먼저 들어가도 충돌은 없다. 단 3월드 착수 후 분할을 병행하면 머지 지옥이 발생하므로 **3월드 착수 전에 분할 로드맵 1~2단계 마무리**가 안전.
- 머지 대기 중인 CSV CI PR#4~ 및 플래그 저장소 PR5~PR8이 단일 JAR 전제 하에 작성돼 있는지 확인 필요(오픈 질문).
- JDK 21 / Paper 1.21.10 / Gradle 설정은 현 `build.gradle.kts` 그대로 승계 가능.

---

## 3. 실측 인벤토리 (1페이지 요약)

### 3.1 패키지 트리 (패키지 수 = 16)
`com.poro.empire` 하위:

- `boss` — 보스 엔진(entry rule, pattern, validator)
- `classes` — 직업 / 무기 정체성
- `combat` — 공식·상태·태그·조건부·자원·스킬·쿨다운
- `command` — `/empire` 명령
- `common` — **공통 기반 계층**: `config` / `db` / `logging` / `registry`(+`master`) / `result` / `seed` / `time` / `flag`(v0.1 WIP)
- `flag` — (v0.1 구현 진행 중, 현재 `common/` 혹은 그 하위에 배치 중)
- `growth` — 강화·잠재·세트·룬·각인 엔진
- `life` — 생활·조합·영지·시설 엔진
- `listener` — HealthHud / HungerLock 리스너
- `npc` — Citizens 리플렉션 게이트웨이 + NPC 싱크
- `operations` — 운영/통계/디스코드/웹 쿼리 어댑터
- `quest` — 퀘스트·업적 엔진
- `reputation` — 평판 매니저
- `settlement` — 영지(life와 결합되지만 물리적으로 별 패키지)
- `storage` — PlayerDataManager (UUID 단위 데이터)
- `util` — HealthHudFormatter 등 보조

### 3.2 JAR 크기
본 검토 환경은 빌드 실행 권한이 없어 실측 JAR 크기는 측정 불가. 대체 지표:

- `plugin.yml` `libraries:`에 SQLite JDBC 1건만 등록 → 런타임 fat jar가 아니라 paper 런타임 라이브러리 로더를 쓰므로 **현재 JAR은 순수 바이트코드+리소스**.
- 16 패키지 × 평균 10~20 클래스 추정 = 약 **200~300 클래스**, 바이트코드 1.5~3 MB + seeds/리소스 CSV 400KB~1MB = **JAR 2~4 MB 선**으로 추정.
- 즉 **지금 당장 "용량 과다" 임계치(수십 MB)는 아니다**. 분할의 1차 명분은 용량이 아니라 **재활용성 + 오류 격리** 쪽이다.
- 실측 값은 착수 직전 `./gradlew :empire-rpg:build` 실행해 수치로 확정 필요.

### 3.3 의존성 그래프 (부트스트랩 기반 실측)

| 모듈 | 의존 대상 |
|---|---|
| `common` | (없음, Paper API만) |
| `npc` | `common` |
| `combat` | `common` |
| `boss` | `common` |
| `growth` | `common` |
| `life` | `common` (+ `master`의 `questMasters`·`itemMasters` 참조) |
| `quest` | `common` |
| `operations` | `common` + **`boss`+`growth`+`life`+`quest` 런타임 모두** |
| `storage` / `reputation` / `command` / `listener` | 플러그인 진입점(Main)에서 조립 |

**핵심 발견**:
1. **`life`는 `combat`·`boss`에 의존하지 않는다** — 사용자 요구(생활 전용 서버에서 전투·보스 없이 구동)와 정확히 맞물린다.
2. `life` 시드 검증에서 `masterRegistryContext.questMasters().find(...)` 호출이 있다 — 이것은 경고(warn)이며 블로킹은 아님. `empire-life` 단독 시 `questMasters`는 빈 레지스트리(stub)로 주입 가능.
3. **`operations`만 4개 도메인을 묶는 응집점** — 분할 시 operations는 "full bundle"에서만 활성, 생활 전용 서버에서는 비활성 혹은 life 전용 서브셋만 등록.
4. Main 플러그인의 `onEnable()`은 **실패 시 전부 `disablePlugin()`** — 현재 구조로는 한 엔진 실패가 전체 diable을 유발한다. 분할 후에도 이 정책을 계속 쓸지, **"치명 실패만 disable · 비치명 실패는 해당 엔진만 skip"** 으로 완화할지 결정 필요(별건).

---

## 4. 작업 분해 — 분할 옵션 A/B/C/D 비교

| 옵션 | 구성 | 재활용성 | 초기 공수(man-day) | 운영 복잡도 | 생활 단독 배포 |
|---|---|---|---|---|---|
| A 현상 유지 | `empire-rpg` 단일 JAR | 없음 | 0 | 매우 낮음 | 불가 |
| B 2분할 (RPG+생활) | `empire-rpg` + `empire-life` + `empire-common` (공용 라이브러리 JAR) | 중 | 4~6 | 낮음~중 | **가능** |
| C 3분할 (+보스) | B + `empire-boss` 분리 | 중상 | 7~10 | 중 | 가능 |
| D 세부 분할 | B/C + `empire-quest` / `empire-npc` / `empire-operations` 분리 | 상 | 12~18 | 높음 | 가능 |

### 4.1 Gradle 구조 변경 범위
- **옵션 B 기준**: `custom-plugins/` 루트에 `settings.gradle.kts` 추가해 multi-module 전환. 모듈은 `empire-common`(library, `java-library` 플러그인) / `empire-rpg`(plugin jar) / `empire-life`(plugin jar).
- `empire-common`은 Paper 플러그인이 아니라 **라이브러리 JAR**이다. `paper-api`를 `compileOnly`로 가져가며, 런타임엔 다른 두 플러그인이 각자 클래스로더에서 재로딩한다(후술 6.1 리스크 참조).

### 4.2 플러그인 간 통신 방식
분할 시 `empire-rpg`와 `empire-life`가 런타임에 서로 데이터를 주고받는 경로는 3가지:

1. **Bukkit Event** — 이벤트 버스로 느슨한 결합. 가장 안전.
2. **Bukkit `ServicesManager`** — 서비스 인터페이스 등록/조회. 중간 결합.
3. **플러그인 참조 getter** — `Bukkit.getPluginManager().getPlugin("EmpireCommon")` 캐스팅. 강결합(클래스로더 이슈 있음).

**권장**: `empire-common`에 정의된 인터페이스를 `ServicesManager`로 등록 · 조회. 이벤트로 충분한 것(보스 처치 → life XP 지급 등)은 이벤트 우선.

### 4.3 manifest 설정
- `empire-life`는 `depend: [EmpireCommon]` (공통 라이브러리 미존재 시 기동 실패가 맞다).
- `empire-rpg`는 `depend: [EmpireCommon]` + `softdepend: [EmpireLife]` (생활 없는 전투 서버 시나리오 허용).
- **단 Paper에서 "라이브러리 JAR"과 "플러그인 JAR" 구분**: 공통 라이브러리를 진짜 Paper 플러그인으로 만들어야 `ServicesManager`·`PluginManager` 경로를 쓸 수 있다. 그렇지 않으면 paper 런타임 라이브러리 로더(`libraries:` manifest)로 로드하거나 각 플러그인에 shade해야 함. **권장 = 공통도 플러그인(`EmpireCommon`)으로 패키징** (클래스로더 일원화).

### 4.4 진행 중 작업과의 상호 영향
- 플래그 저장소 PR5~PR8 대기 — 분할 1단계(Gradle multi-module, 단일 JAR 유지)까지는 영향 없음. JAR 분리(3단계)부터는 플래그 저장소가 `empire-common`에 들어가야 생활·RPG 양쪽에서 쓸 수 있음.
- CSV CI PR#4~ 머지 대기 — 단일 JAR 가정이면 seeds 디렉토리를 `empire-rpg/resources/seeds`로 본다. 분리 후 `empire-common/resources/seeds` 혹은 도메인별로 쪼개야 함. **분할 착수 전에 CSV CI가 멈춰 있지 않은지 확인 필요**.
- 3월드 전환 PR-W0~W9 — 월드 전환은 `life`(영지) 도메인에 몰려 있어 **분할 후에 진행하는 편이 안전**.

---

## 5. 기술 리스크

### 5.1 클래스로더 교차 참조
Paper 1.21은 플러그인마다 별도 `URLClassLoader`를 쓴다. 다른 플러그인이 정의한 record·enum을 제3의 플러그인에서 `instanceof`·캐스팅하면 **같은 이름이라도 서로 다른 Class 객체**가 돼 `ClassCastException` 난다.

해결:
- 모든 공유 타입(예: `FlagKey`, `ItemMaster`, `Result<T>`)은 **반드시 `empire-common` 단 한 곳에만 존재**.
- 공통 라이브러리를 Paper 플러그인으로 등록 → Paper가 단일 클래스로더로 로드 → 다른 플러그인이 `depend`로 가져가면 같은 Class 객체 공유.
- 혹은 Paper의 `libraries:` manifest로 공통을 런타임 라이브러리로 로드(단 Paper가 플러그인 로딩 순서를 보장하는지 확인 필요).

### 5.2 `onEnable` 실패 시 파급
현재 Main은 각 엔진 bootstrap 실패 시 `disablePlugin(this)` 호출. 분할 후:
- `empire-common` 실패 → `depend` 플러그인 전부 로드 차단(Paper 표준 동작)
- `empire-rpg`만 실패 → `empire-life`는 정상 기동 유지(분할의 핵심 이득)
- **단 현재 `PlayerFlagTableStubMigration` 실패는 common 실패로 승격** — 이 실패는 전부를 막는다. 스텁 실패가 전체를 막는 건 맞는 정책인지 별도 검토 필요.

### 5.3 로드 순서
- `EmpireCommon.onEnable()` → `EmpireRPG.onEnable()` → `EmpireLife.onEnable()` 순서 보장이 필요.
- Paper는 `depend` 체인을 위상 정렬해주지만, `MasterRegistryContext` 같은 "데이터가 준비되었는가"는 **이벤트 `PluginEnableEvent`** 로 대기하거나 `ServicesManager`에 등록된 시점까지 기다리는 패턴이 안전.

### 5.4 백업 전략
- 분할 작업은 별도 브랜치 `feature/split-phase-1`에서 진행.
- 1단계(multi-module 전환, 단일 JAR 유지)는 배포 아티팩트가 동일하므로 머지 리스크 낮음.
- 2단계(공용 라이브러리 JAR 분리)부터는 **dev 서버에서 1주 스모크 테스트** 후 master 머지.

### 5.5 기존 판정 원칙 충돌
마스터 플래닝 line 13·179·372·1135 "EmpireRPG(Custom Plugin) 단독 판정"의 문맥은 **"BetonQuest 등 외부 퀘스트 플러그인이 판정하지 않는다"** 이다. JAR 수에 대한 규정이 아니다.

재해석안:
- **현재 문구**: "EmpireRPG(Custom Plugin) 단독"
- **재해석안**: "EmpireRPG 플러그인 집합(empire-common + empire-rpg + empire-life ...) 단독" — 이는 판정 주체가 여전히 우리 코드라는 뜻이다.
- **마스터 플래닝 미세 수정 제안**: line 13, 179, 1135의 "Custom Plugin 단독"을 **"EmpireRPG 플러그인 체계 단독"** 로 표기. 괄호 안에 "(단일 JAR 또는 복수 JAR 모두 허용, BetonQuest·외부 엔진 미사용 의미)" 주석 1회. 다른 본문 수정은 없음.
- 원칙 자체 변경은 불필요. 표현 확장으로 충분.

---

## 6. 테스트 포인트

### 6.1 단위·통합 레벨
- `empire-common` 단위 테스트는 현 test 스위트 그대로 이관.
- `empire-life` 단독 배포 스모크: `empire-common + empire-life`만 설치한 dev 서버에서
  - 영지 해금 / 시설 레벨업 / 조합 / 채집 4 시나리오 PASS
  - 보스·전투 명령 없음 확인 (`/empire class` 실행 시 안내 메시지)
  - 플래그 저장소 SQLite 파일 생성 확인
- `empire-rpg` 단독 배포 스모크: `empire-common + empire-rpg`만 설치한 dev 서버에서 생활 명령 없이도 전투·보스 루프 PASS.

### 6.2 클래스로더·통신 테스트
- `empire-rpg`가 `empire-life`의 서비스를 `ServicesManager`로 조회 시 정상 캐스팅 확인(다른 클래스로더 이슈 없는지).
- `empire-life` 비활성 상태에서 `empire-rpg`가 `softdepend` 경로로 정상 진행하는지(NPE 방지).

### 6.3 CI
- Gradle multi-module 전환 후 `./gradlew build` 루트 호출 시 세 모듈 모두 빌드·테스트 실행.
- 산출 JAR 3종(`empire-common`, `empire-rpg`, `empire-life`) 크기를 CI 로그에 출력하여 회귀 감시.

---

## 7. 1차 버전 권장안 (옵션 B + 단계 로드맵)

### 7.1 권장안 = 옵션 B (2분할 + 공용 라이브러리)
이유:
- 사용자 우선순위(생활 단독 배포) 직접 충족
- 실측 의존성에서 life가 combat/boss에 의존하지 않아 분리 비용이 가장 싸다
- 옵션 C(보스 추가 분리)는 재활용 이득이 불명확 — 보스 도메인만 떼어갈 수요가 현재 없음
- 옵션 D는 PR·운영 부담이 ROI 대비 과하다

### 7.2 단계별 마이그레이션 로드맵

| 단계 | 내용 | 공수 | 리스크 |
|---|---|---|---|
| 1 | Gradle multi-module 전환 (루트 `settings.gradle.kts`, 서브모듈 `empire-rpg`만 유지). **단일 JAR 배포는 그대로**. | 0.5~1 md | 낮음(CI 설정만 조정) |
| 2 | `empire-common` 서브모듈 신설 + `com.poro.empire.common.*` 이동. `empire-rpg`가 `implementation(project(":empire-common"))`로 의존. **아직 JAR 1개, 내부에 shade**. | 1.5~2 md | 중(import 이동 다수, 테스트 스위트 조정) |
| 3 | `empire-common`을 **독립 Paper 플러그인 JAR**로 승격. `empire-rpg` `plugin.yml`에 `depend: [EmpireCommon]`. dev 서버 스모크 1주. | 1~1.5 md | 중상(클래스로더 첫 분리) |
| 4 | `empire-life` 서브모듈 신설 + `com.poro.empire.life.*` + `settlement.*` 이동. `operations`에서 life 의존 부분은 softdepend로 재배선. | 1.5~2 md | 중 |
| 5 | 생활 전용 dev 서버에 `empire-common + empire-life`만 배포해 스모크. 정상 작동 확인 시 master 머지. | 0.5 md | 중(실배포 검증) |

**총 공수 = 5~7 man-day**. 블로커 = 플래그 저장소 PR5~PR8 머지(공용 경계 확정), CSV CI PR#4 상태 확인.

### 7.3 1단계 착수 시 즉시 공수
**0.5~1 man-day**. 실제 코드 이동이 없으므로 리스크가 거의 없고, 여기서 막히면 다음 단계로 가지 않는다.

### 7.4 생활 프로젝트 단독 배포 체크리스트
`empire-common`에 들어가야 하는 것:
- `common/config`, `common/db`, `common/logging`, `common/registry`(`master` 포함), `common/result`, `common/seed`, `common/time`
- 플래그 저장소 v0.1 전체(도메인 공유 전제)
- `ItemMaster` / `QuestMaster` / `AchievementMaster` 등 마스터 레지스트리 모델(`life` 검증에서 참조)

`empire-life`에만 있어야 하는 것:
- `life/engine/*`, `settlement/*`, 생활 시드 CSV, 생활 명령 조각

**`empire-life`에 있으면 안 되는 것**:
- `combat`, `boss`, `growth`, `classes` 런타임 참조(컴파일 의존 자체 금지)
- `operations`(생활 전용 서버는 전체 통계 서비스 불필요, 필요 시 `empire-life-ops` 별도 고려)

생활 서버 외부 의존:
- 필수: Paper 1.21.10, SQLite JDBC (runtime)
- 선택(softdepend): Citizens(NPC 배치) · FAWE(영지 스키매틱) · LuckPerms(권한) · PlaceholderAPI
- **불필요**: BetonQuest(마스터 플래닝에서 미사용 확정)

---

## 8. 나중으로 미뤄도 되는 것
- **옵션 C/D** (보스 / 퀘스트 / NPC / operations 별도 JAR): 옵션 B가 실제 배포 안정화된 뒤 재검토. 최소 1시즌 운영 후.
- `empire-common`을 Paper 플러그인이 아니라 라이브러리 로더 방식으로 전환(성능·로딩 시간 최적화) — 기능 요구가 생길 때.
- `onEnable` 실패 시 "엔진 단위 skip" 정책 — 분할과 별개로 검토 가능.
- JAR 크기 실측 기반 추가 최적화(fat jar 회피, seed 리소스 외부 분리).

---

## 9. 오픈 질문 (사용자 확인 필요)
1. "EmpireRPG 단독 판정" 문구를 "EmpireRPG 플러그인 체계 단독"으로 표기 확장해도 되는가?
2. 1단계 착수 시점 — 플래그 저장소 PR5~PR8 머지 **전** / **후** 중 어느 쪽?
3. `empire-common`을 Paper 플러그인으로 승격(권장) vs 라이브러리 JAR 방식 — 후자는 클래스로더 설계 추가 필요, 사용자가 후자를 원하는지?
4. 생활 프로젝트 서버 운영 시점이 언제인지(단독 배포 스모크의 시급성 결정).
5. 옵션 C(보스 추가 분리) 수요가 향후 1시즌 내 실제로 있는지 — 없으면 영구 보류 판정.
