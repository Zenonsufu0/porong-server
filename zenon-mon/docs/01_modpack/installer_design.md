# 자체 제작 범용 설치기 설계 (결정 046)

> 대상: **Zenon Mon 0.1** 클라 배포(이후 포로 모드 서버 공용). MC 1.21.1 / Fabric Loader 0.19.3 / Java 21.
> 결정 근거: **결정 046**(2026-06-12, 045 대체) — CurseForge 배제, 자체 제작 exe 설치기 + 전부 번들 + 토글 0.1부터.
> 상태: **설계 수립(2026-06-12)**. 구현은 별도(설치기는 `zenon-mon` 외부 산출물 가능 — §10). 미확정은 §9.
> 선행: 모드 분류 `client_mod_tiers.md`(T0/T1/T2), 서버/클라 분리 `server_mod_separation.md`. CF 모델(폐기)은 `client_pack_policy.md`(참고용).

---

## 0. 한 줄 요약

유저가 **`Zenon Mon설치기.exe` 더블클릭 → 필수 모드 자동 + 선택 모드 체크박스 → [설치] → 게임 실행·서버 접속**. 모드는 전부 exe에 번들(인터넷·로그인 불필요). 엔진은 공용, 서버별 설정(JSON)만 교체해 재활용.

---

## 1. 원칙 (결정 046)

1. **유저는 설치기 하나만** — 수동 mods 폴더 설치·CurseForge/런처 로그인 없음.
2. **필수(T0)=강제·토글 불가** — 서버 레지스트리와 1:1 정합(버전·해시). 누락/불일치 = 접속 거부.
3. **선택(T1·T2)=개별 체크박스** — 게임 동작 무관, 켜고 끔 자유.
4. **전부 번들** — 모든 jar를 exe에 동봉. 설치 = 압축 해제 + 배치. 외부 다운로드 0.
5. **범용 엔진 + 서버별 설정 분리** — 엔진 코드는 공용, `pack.json`만 갈아끼움(Zenon Mon/Zenon Gun…).
6. **버전 핀 고정** — 모든 모드를 정확한 파일/해시로 고정. 서버=클라 동일 빌드.
7. **독립 인스턴스** — 유저 기존 `.minecraft`를 건드리지 않고 별도 게임 폴더에 설치(안전·격리).

---

## 2. 아키텍처 — 엔진 + 팩 분리

```
Zenon Mon설치기.exe
├── [엔진]  공용 코드 (서버 무관)
│   ├── GUI(체크박스 토글)·설치 오케스트레이션
│   ├── Fabric loader 설치 / MC 프로필 생성 / servers.dat 등록
│   └── pack.json 로더 + 번들 추출기
└── [번들]  이 팩 전용 데이터 (= pack.json이 가리키는 것)
    ├── pack.json              # 팩 정의(모드목록·토글·서버·버전핀)
    ├── mods/                  # 모든 jar(필수+선택+라이브러리) 번들
    ├── overrides/             # config·resourcepacks 등 그대로 복사할 것
    └── tools/fabric-installer.jar (또는 사전 생성 version json)
```

**재활용**: Zenon Gun은 같은 엔진에 `pack.json`(Zenon Gun 모드목록·서버주소) + Zenon Gun `mods/` 번들만 바꿔 빌드. 엔진은 한 곳에서 유지보수.

---

## 3. 팩 설정 스키마 (`pack.json`)

엔진이 읽는 단일 진실. (예시 — 실제 파일명/버전은 빌드 시 `modpack/client/mods` 실측에서 채움.)

```json
{
  "pack":      { "id": "zenonmon", "name": "Zenon Mon", "version": "0.1.0" },
  "minecraft": { "version": "1.21.1", "loader": "fabric", "loaderVersion": "0.19.3", "java": 21 },
  "install":   { "instanceName": "Zenon Mon", "isolated": true },
  "server":    { "name": "Zenon Mon", "address": "HOST:25566" },

  "required": [
    { "file": "fabric-api-0.116.8+1.21.1.jar",        "name": "Fabric API" },
    { "file": "Cobblemon-fabric-1.7.3+1.21.1.jar",    "name": "Cobblemon" },
    { "file": "mega_showdown-fabric-...jar",          "name": "Mega Showdown" },
    { "file": "zenon-mon-core-0.1.0.jar",               "name": "ZenonMonCore" }
    /* … T0 코어 14 + 서버 정합 모드 … */
  ],

  "optional": [
    { "file": "sodium-...jar",   "name": "성능 향상(Sodium)", "group": "performance", "default": true,
      "desc": "프레임 향상. 저사양 권장." },
    { "file": "xaeros-minimap-...jar", "name": "미니맵(Xaero)", "group": "viewer", "default": true,
      "desc": "미니맵/월드맵." },
    { "file": "complete-cobblemon-collection-...jar", "name": "전설 모델 보충", "group": "qol", "default": true,
      "desc": "미구현 전설/환상 모델(없으면 인형으로 보임)." },
    { "file": "<shader>.jar", "name": "셰이더(Iris)", "group": "shader", "default": false,
      "desc": "고사양 비주얼." }
    /* … T1 기본 ON / T2 기본 OFF … */
  ],

  "libraries": [
    { "file": "architectury-...jar" }, { "file": "owo-lib-...jar" }
    /* 의존 라이브러리: 0.1은 항상 포함(번들이라 용량만, 의존 깨짐 방지) */
  ],

  "overrides": [ "config", "resourcepacks" ]
}
```

- **`group`** = 토글 UI 묶음(성능/뷰어/QoL/UI/셰이더/파티클/사운드). `client_mod_tiers.md` 분류와 매핑.
- **`default`** = 체크박스 초기값(T1 true / T2 false). `client_mod_tiers.md` §7 "최소 설치=T0+성능".
- **`libraries`** = 0.1은 **항상 포함**(번들이라 용량만 더 들고 의존 깨짐을 원천 차단). 0.2에서 선택 모드별 `requires`로 정교화(§8).
- **버전 핀** = `file`이 곧 핀(정확 파일명). 서버 `sync-server-mods.sh` 화이트리스트와 **동일 파일명**이어야 정합.

---

## 4. 설치 흐름

**유저 관점:**
1. `Zenon Mon설치기.exe` 실행.
2. 화면: 상단 "필수 코어 ✔ (자동)" 고정, 아래 선택 모드 체크박스 목록(그룹별, 기본값 적용).
3. 설치 경로 확인(기본=권장 폴더) → **[설치]**.
4. 진행바 → 완료 → **[게임 실행]**(또는 "공식 런처에서 Zenon Mon 프로필 선택" 안내).

**엔진 내부 단계:**
1. **환경 점검** — MC 공식 런처 존재/Java. (독립 인스턴스라 기존 세이브 영향 없음.)
2. **인스턴스 생성** — `install.instanceName` 폴더(격리 gameDir).
3. **Fabric 설치** — 번들 `fabric-installer.jar`를 headless 실행(`client -mcversion 1.21.1 -loader 0.19.3 -dir <inst> -noprofile`) 또는 사전 생성 version json 배치.
4. **모드 배치** — `required` + 체크된 `optional` + `libraries`를 인스턴스 `mods/`에 추출.
5. **overrides 복사** — `config/` 등 그대로 복사.
6. **서버 등록** — gameDir `servers.dat`(NBT)에 `server.address` 추가.
7. **런처 프로필** — 공식 런처 `launcher_profiles.json`에 Zenon Mon 프로필(이 gameDir·Fabric 버전) 추가.
8. **검증** — mods 개수·핵심 jar 해시 확인 → 완료 보고.

> **Java 21**: 공식 런처는 버전별 JRE를 자동 제공(1.21.1 = Java 21) → 런처 경유면 별도 JRE 불필요. 자체 실행(런처 우회) 옵션을 0.2에서 검토하면 JRE 번들 고려(§9).

---

## 5. 토글 모델 (= `client_mod_tiers.md` 매핑)

| 티어 | 설치기 | pack.json |
|---|---|---|
| **T0 코어(14)** | 항상 설치·체크박스 없음 | `required` |
| **T1 권장 편의** | 체크박스 **기본 ON** | `optional` `default:true` |
| **T2 선택 취향** | 체크박스 **기본 OFF** | `optional` `default:false` |
| **L 라이브러리** | UI 비노출·항상 포함 | `libraries` |
| **(S) 서버전용** | 클라 번들 제외 | (pack.json에 없음) |

- 그룹(`group`)으로 UI 섹션 구성: 성능 / 뷰어 / QoL / UI / 셰이더 / 파티클 / 사운드.
- **ZenonMonCore·collection 등 T0/T1 정합 항목은 §결정 044 분류 준수**(ZenonMonCore=required, collection=optional 강권장 default:true).

---

## 6. 빌드 파이프라인 (계획)

목표: **단일 소스(`modpack/client/mods` 86 + `overrides/`) → pack.json + 번들 → exe**.

1. **모드 실측 → pack.json 생성/검증** — `modpack/client/mods` 실파일명으로 required/optional/libraries 채움. 분류 출처=`client_mod_tiers.md`. 서버 화이트리스트(`sync-server-mods.sh`)와 파일명 정합 체크.
2. **ZenonMonCore 빌드** — `custom-mods/zenon-mon-core/gradlew build` → jar를 번들 `mods/`에 포함(서버와 동일 해시).
3. **번들 스테이징** — `mods/`(전체) + `overrides/`(단 `config/openloader/data` = 서버 데이터팩 제외, 결정 043) + `tools/fabric-installer.jar` + `pack.json`.
4. **exe 패키징** — 엔진(Python) + 번들을 PyInstaller로 `--onefile` exe 빌드(§7).
5. **검증 설치** — 깨끗한 Windows에서 exe → 인스턴스 생성·접속 성공(§검증 체크리스트).

> 기존 `scripts/build-client-pack.sh`(CF zip)·`extract-curseforge-pack.sh`는 **CF 전용 → 보류**. 새 파이프라인은 `build-installer-pack`(가칭, pack.json 생성 + 번들 스테이징) + exe 빌드로 대체.

---

## 7. 기술 스택 (제안)

- **언어/GUI**: Python + GUI(tkinter 기본 의존 / PySide6 더 예쁨). → **PyInstaller `--onefile`** 로 단일 exe.
- **Fabric 설치**: Fabric 공식 `fabric-installer.jar` headless 호출(번들). MC/loader/Java는 공식 런처 위임.
- **NBT(servers.dat)**: Python NBT 라이브러리(예 `nbtlib`)로 서버 항목 추가.
- **코드서명(중요)**: 미서명 exe는 Windows SmartScreen "알 수 없는 게시자" 경고 → 유저 불안. 0.1 알파는 "추가 정보→실행" 안내로 감수, 정식 배포 시 코드서명 인증서 검토(§9).
- **배포처**: GitHub Releases 등(설치기 exe 자체는 익명 다운로드). 모드는 exe 내부라 별도 호스팅 불필요.

> 대안 스택(검토): C#/.NET(Windows 친화·서명 용이), Electron(웹 UI·용량 큼), Java(런처와 동일 생태계). **1차 추천=Python**(개발 속도·NBT/zip 처리 용이). 확정은 구현 착수 시.

---

## 8. 로드맵

- **0.1**: 단일 엔진 + Zenon Mon pack.json + 전부 번들 + 체크박스 토글 + 독립 인스턴스 + 서버 자동등록. 라이브러리 항상 포함.
- **0.2**: 선택 모드별 `requires`(의존 정교화), 업데이트 감지(설치된 버전 vs 번들), 셰이더/리소스팩 그룹 세분, Zenon Gun pack.json 재활용 실증.
- **0.3+**: 코드서명, 자체 JRE 번들(런처 우회 실행), Mac 빌드(수요 시), 델타 업데이트(번들 재배포 대신 변경분).

### 검증 체크리스트 (0.1, 클라 필요)
- [ ] 깨끗한 Windows에서 exe 실행 → 경고 후 정상 구동
- [ ] 필수 코어 자동 + 선택 체크 반영(켠 것만 mods에 들어감)
- [ ] Fabric 인스턴스 생성·기존 `.minecraft` 세이브 무영향
- [ ] ZenonMonCore 포함 + 배지/조우권/정수 텍스처 렌더(종이 아님)
- [ ] 서버 자동등록(서버 목록에 Zenon Mon) → 접속 성공(모드 불일치 거부 없음, T0 해시 일치)
- [ ] 선택 OFF 모드 빠진 상태로도 정상 플레이(T0만으로 접속)

---

## 9. 오픈 질문 / 결정 필요

1. **재배포 라이선스** — 번들=재배포. 모드별 허용 여부 확인(특히 ARR). 불가 모드 발견 시 해당만 제외/대체. **오픈 전 일괄 점검**. → ✅ 1차 점검 완료(2026-06-12): `license_audit.md`(OK 63/COPYLEFT 1/CHECK 22). CHECK 22개 permission 실확인이 잔여.
2. **코드서명** — 0.1 미서명(경고 감수) vs 인증서 구매. 정식 배포 전 결정.
3. **기술 스택 확정** — Python(추천) vs C#/.NET vs 기타(§7).
4. **JEI vs EMI 택1** — 둘 다 번들 중복(`client_mod_tiers.md` §2-2). optional 양자택일 라디오로 노출? 운영자 택1?
5. **공식 런처 프로필 vs 자체 실행** — 0.1은 공식 런처 프로필(JRE 위임). 자체 실행(JRE 번들)은 0.3.
6. **설치기 코드 위치** — `zenon-mon` 내부 `installer/` vs 별도 레포(범용=공용이라 분리 후보). → §10.

---

## 10. 설치기 코드 위치 (메모)

엔진이 **범용(공용)**이므로 장기적으로는 포로 서버들이 공유하는 위치가 맞다. 단기(0.1)는 Zenon Mon에서 시작:
- 단기: `zenon-mon/installer/`(엔진+Zenon Mon pack 빌드 스크립트). 번들 소스=`modpack/client/mods`·`overrides`·ZenonMonCore 빌드.
- 장기: 엔진을 별도 공용 레포/디렉터리로 승격, 각 서버는 pack.json + 번들만 제공.
- ⚠️ exe·번들 산출물은 **Git 비추적**(용량·jar). 빌드 산출은 `.local/` 등.

---

## 11. 관련 문서

- 모드 티어(토글 출처): `client_mod_tiers.md` · 서버/클라 분리: `server_mod_separation.md`
- CF 모델(폐기·참고): `client_pack_policy.md` · 결정: `../00_project/decisions.md` 046(←045)
- 서버 동기화 스크립트: `../../scripts/sync-server-mods.sh`(화이트리스트=required 정합)
- ZenonMonCore 빌드: `../../custom-mods/zenon-mon-core/`
