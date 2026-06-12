# 클라이언트 팩 정책 + 간편설치기 제작 계획 (결정 045)

> ⛔ **배포 모델 폐기 — 결정 046(2026-06-12)으로 대체. SoT = `installer_design.md`.**
> CurseForge 공식 팩 1차 모델은 **자체 제작 범용 exe 설치기 + 전부 번들 + 토글 0.1부터**로 전환됨(사유: CF 로그인·외부 다운로드 차단·유저별 실패). 이 문서는 **CF 방식 참고/패키지 구조 자료**로만 보존. **유지되는 원칙**: 유저 수동설치 금지·T0 서버정합·PoroMonCore 항상 번들·버전 핀. manifest 정합/`extract-curseforge-pack.sh`/`build-client-pack.sh`(CF zip)는 **CF 전용 → 보류**.

> 대상: **PoroMon 0.1** 클라이언트 배포. MC 1.21.1 / Fabric Loader 0.19.3 / Java 21.
> 목적: "유저가 모드를 수동 설치하지 않고 한 번에 설치→플레이"(`CLAUDE.md` 모드팩 정책)를 실현하는 **배포 패키지 + 간편설치기**의 제작 계획.
> 선행 문서: 분류는 `client_mod_tiers.md`(T0/T1/T2), 서버/클라 분리는 `server_mod_separation.md`. 이 문서는 **"어떻게 패키징·배포·설치하게 만들 것인가"**.
> 상태: **계획 수립(2026-06-09)**. 실제 빌드/익스포트는 별도 세션. 미확정은 §8 오픈 질문.

---

## 0. 현재 실측 (계획의 출발점)

| 항목 | 값 | 출처 |
|---|---|---|
| 실제 클라 jar | **86개** | `modpack/client/mods/` |
| 서버 화이트리스트 | **25개** | `.local/server/mods/` |
| CurseForge manifest 파일 | **80개** | `modpack/base/manifest/manifest.json` (`projectID`×80) |
| manifest 로더 | `fabric-0.19.3` (✅ 2026-06-09 정정) | 실제 런타임 일치(이전 0.18.4 stale 수정) |
| PoroMonCore | `poromon-core-0.1.0.jar` (커스텀) | `custom-mods/poromon-core/build/libs/` |

> **핵심 갭 1 — manifest(80) ≠ 실제(86)**: manifest는 stale 옛 export. 실측(§3-1) = **manifest에 없는 실제 jar 8개**(LM 의존 5 + collection + swingthrough + PoroMonCore) + **manifest에만 있는 폐기분 1개**(eggs). 8개 중 7개는 CF 재익스포트로 자동 해소, PoroMonCore만 영구 번들 → §3·§4.
> **핵심 갭 2 — PoroMonCore는 CurseForge에 존재하지 않음** → 어떤 배포 방식이든 **overrides에 직접 번들**해야 한다(§4).

---

## 1. 원칙

1. **수동 mods 폴더 설치를 기본 가이드로 두지 않는다**(`CLAUDE.md`). 유저 동선 = 런처 설치 → Play → 접속.
2. **T0(코어)는 서버와 1:1 일치**(레지스트리 정합). T0 누락/버전불일치 = 접속 거부. → 설치기에서 끌 수 없음.
3. **T1/T2는 순수 클라**(켜고 끔 자유, 게임 동작 무관). 의존 라이브러리는 자동 해소.
4. **버전 핀 고정**: 모든 모드는 정확한 fileID/해시로 고정(부동 "latest" 금지) — 서버와 클라가 같은 빌드.
5. **PoroMonCore는 자체 빌드 산출물** → CurseForge/Modrinth 메타로 해소 불가, 항상 overrides 번들.
6. **추측 금지**: projectID/fileID/해시는 실제 export에서 채취. 미확보분은 TODO.

---

## 2. 배포 모델 결정 (결정 045)

세 방식을 검토했다.

| 안 | 방식 | 장점 | 단점 |
|---|---|---|---|
| **A. CurseForge 공식 팩** (채택) | CurseForge에 "PoroMon Official Pack" 업로드, manifest+overrides | 런처 1클릭 설치·자동 업데이트·유저 친숙 | PoroMonCore는 CF에 못 올림 → overrides 번들 필요. 승인/심사 절차 |
| B. Modrinth 공식 팩 | `.mrpack`(modrinth.index.json) | 오픈·해시 기반·CLI(`packwiz`) 친화 | 유저 풀 작음(국내 CF 우세) |
| C. 자체 설치기(exe/PrismLauncher import) | 커스텀 런처 또는 Prism `.mmc` 프로필 | 토글 UI 완전 제어 | 제작·유지보수 큼, 코드서명 등 |

**결정**: **1차 = A(CurseForge 공식 팩)**, 보조로 **packwiz 소스 트리**를 단일 진실로 두고 거기서 CF/Modrinth 양쪽 export(향후). 토글(T1/T2)은 0.1에서 **"풀 패키지 단일 프로필"**로 시작(전부 포함, 끄고 싶으면 ModMenu로 개별 비활성) → 토글형 설치기는 **0.2 이후**(§7 로드맵).

> 사유: 0.1 알파는 **재현 가능한 단일 클라**가 최우선. 토글 UI는 알파 후 수요 확인하고 투자. PoroMonCore 번들 문제(§4)가 어느 방식에서도 공통 핵심이므로 그것부터 확립한다.

---

## 3. 패키지 구성 (CurseForge 팩 = manifest + overrides)

CurseForge 모드팩 zip 구조:
```
PoroMon-0.1.zip
├── manifest.json            # CF가 자동 다운로드할 모드들(projectID/fileID)
├── modlist.html             # 사람이 읽는 목록
└── overrides/               # CF가 해소 못 하는 것 전부(그대로 인스턴스에 복사)
    ├── mods/                # ★ poromon-core + CF에 없는 모드 jar 직접 동봉
    ├── config/              # 모드 설정(현 modpack/overrides/config 그대로)
    ├── resourcepacks/       # (한글팩 등 필요 시)
    └── ...                  # showdown/, xaero/, fancymenu_data/ 등 기존 overrides
```

### 3-1. manifest(80) ↔ 실제(86) 정합 — 실측 결과(2026-06-09)

실제 86 jar를 `modlist.html`(80 앵커/79 슬러그)와 정밀 대조(별칭 보정: BHMenu=bisecthosting, xaerominimap=xaeros-minimap 등). **결론: manifest.json은 LM 의존 추가·collection·swingthrough 이전 + eggs 포함 시점의 stale 옛 export.**

**(A) manifest/modlist에 없는 실제 jar = 8개**

| jar | CF 등재 | 분류 | 처리 |
|---|---|---|---|
| `chipped-fabric` | ✅ CF | (a) | manifest 추가 |
| `CobbleFurnies-fabric` | ✅ CF | (a) | manifest 추가 |
| `TerraBlender-fabric` | ✅ CF | (a) | manifest 추가 |
| `athena-fabric` | ✅ CF | (a) | manifest 추가 |
| `resourcefullib-fabric` | ✅ CF | (a) | manifest 추가 |
| `complete-cobblemon-collection-myths-and-legends-compat` | CF 추정(확인) | (a) | manifest 추가 (CF 아니면 (b) 번들) |
| `swingthrough` | ✅ CF | (a) | manifest 추가 |
| `poromon-core` | ❌ 없음(자체) | **(b)** | **overrides/mods 번들** |

> (a) 7개 = LM 의존 5 + collection + swingthrough → 모두 CF 자동해소 후보. (b) 1개 = PoroMonCore(영구 번들).

**(B) modlist에만 있고 jar 없음 = 1개** → manifest에서 제거
- `eggs-cobblemon-addon` (결정 032 폐기).

**권장 처리 = manifest 재익스포트(가장 안전)**: 현재 CurseForge 프로필에는 이미 (a) 7개가 추가돼 있을 것이므로, **프로필에서 팩을 새로 export**하면 manifest가 자동으로 (a) 전부 포함 + eggs 제거됨. 그러면 남는 건 **PoroMonCore 번들(b)** 하나뿐. → `extract-curseforge-pack.sh`로 재반영.
- 대안(수동): 위 (a) 7개의 projectID/fileID를 CF에서 채취해 manifest에 추가 + eggs 항목 삭제. ⚠️ fileID는 정확한 버전 필요 → 재익스포트가 오류 적음.

> ⚠️ **재익스포트 시 확인**: ① collection이 CF가 아니라 Modrinth-only면 manifest에 안 잡힘 → (b) 번들로 전환 ② loader가 export에서 다시 `0.18.4`로 돌아오면 재정정.
> ✅ **loader 버전 정정 완료(2026-06-09)**: manifest `fabric-0.18.4` → **`fabric-0.19.3`**(실제 런타임 일치, 무오류 확인).

### 3-2. overrides에 들어가는 것
- `mods/poromon-core-0.1.0.jar`(필수) + (b) 판정 모드들.
- 기존 `modpack/overrides/{config,showdown,xaero,fancymenu_data,shaderpacks}` 그대로.
- ⚠️ `config/openloader/data/*`(LM·메가 차단 데이터팩)는 **서버측 통제**(결정 043). 클라 overrides에 포함돼도 무해(클라는 worldgen 서버권한이라 미적용)하나, **클라 팩에서는 제외 권장**(혼선 방지). → 클라 export 시 openloader/data 제외 필터.

---

## 4. PoroMonCore 번들 — 가장 중요한 제약

PoroMonCore는 **CurseForge/Modrinth에 없고**(자체 빌드), **클라에도 반드시 필요**(배지·조우권·정수 CustomModelData 텍스처 렌더, `server_mod_separation.md` §1c). 따라서:

1. **빌드**: `cd custom-mods/poromon-core && ./gradlew build` → `build/libs/poromon-core-0.1.0.jar`.
2. **번들**: 그 jar를 `overrides/mods/`에 복사해 팩에 동봉.
3. **버전 동기화**: 서버 `.local/server/mods/`의 PoroMonCore와 **동일 해시**여야 함(빌드 변경 시 서버+클라+팩 3곳 동시 갱신).
4. **업데이트 흐름**: PoroMonCore 갱신 = 팩 새 버전 릴리스(유저는 런처에서 팩 업데이트). 커스텀 텍스처/모델 변경도 이 경로.

> ⚠️ 0.1은 버전 번호 수기 관리(`0.1.0`). 향후 빌드 해시를 팩 버전에 박아 불일치 감지(0.2 로드맵).

---

## 5. 토글 프로필 (0.2 이후 — 설치기 레이어)

`client_mod_tiers.md`의 T0/T1/T2를 설치기 UI로 노출하는 단계. 0.1에선 **단일 풀 프로필**, 0.2부터 도입.

| 구분 | 설치기 | 근거 |
|---|---|---|
| T0 코어(14) | 항상 포함·토글 불가 | 서버 정합 |
| T1 권장(39) | 기본 ON | 성능/편의 |
| T2 선택(18) | 기본 OFF | 비주얼/취향 |
| L 라이브러리 | 선택에 따라 자동 | 의존 해소 |

구현 후보(0.2): **packwiz + 옵션 그룹**(packwiz는 optional mod·기본값 지원) → CF/Modrinth export. 또는 PrismLauncher 프로필 분기. **의존성 자동 해소는 각 모드 `fabric.mod.json depends` + CF/Modrinth 메타로 검증**(추측 금지).

> 최소 설치(저사양) = **T0 + 성능(2-1)**만으로 정상 플레이(`client_mod_tiers.md` §7).

---

## 6. 빌드/익스포트 파이프라인 (계획)

목표: **단일 소스 → 재현 가능한 클라 팩 + 서버 mods**. 현 `scripts/`는 골격뿐(`extract-curseforge-pack.sh`만 동작, 경로 stale).

제안 단계(스크립트화 대상, 0.1~0.2):
1. **소스 진실 고정**: `modpack/client/mods/`(86) = 클라 진실. `modpack/base/manifest/manifest.json` 정합(§3-1).
2. **PoroMonCore 빌드** → `overrides/mods/`로 복사(§4).
3. **클라 export 필터**: `modpack/overrides`에서 `config/openloader/data`(서버 데이터팩) 제외(§3-2).
4. **manifest 검증**: projectID/fileID 80→정합 후, loader 0.19.3, MC 1.21.1.
5. **zip 패키징**: `manifest.json + modlist.html + overrides/` → `PoroMon-0.1.zip`(CF 업로드용).
6. **서버 mods 동기화**: `scripts/sync-server-mods.sh`(화이트리스트 25, PoroMonCore 포함) — 클라 export와 **동일 PoroMonCore 해시** 확인.
7. **검증 설치**(§7-검증): 깨끗한 런처에 팩 설치 → 서버 접속 성공.

> ⚠️ `scripts/extract-curseforge-pack.sh` ROOT 경로가 옛 `poro-server-poromon`(stale) → 현 워크트리 경로로 수정 필요(별도 작업).

---

## 7. 단계별 로드맵

- **0.1 (알파 직전)**: §2-A 단일 풀 프로필. 핵심 = **manifest 정합(§3-1) + PoroMonCore 번들(§4) + 깨끗한 설치 검증**. 토글 없음.
- **0.2**: T1/T2 토글 프로필(§5), packwiz 소스 트리화, 빌드 자동화(§6 스크립트), 버전 해시 핀.
- **0.3+**: Modrinth 동시 배포, 한글 리소스팩 번들(필요 시), 자동 업데이트 안내.

### 검증 체크리스트 (0.1 설치 — 별도 세션, 클라 필요)
- [ ] 깨끗한 CurseForge 런처에 PoroMon 팩 설치 → 모드 86개(또는 정합된 수) 전부 적용
- [ ] PoroMonCore jar 클라 존재 + 배지/조우권/정수 텍스처 정상 렌더(종이 아님)
- [ ] 서버 접속 성공(모드 불일치 거부 없음) — T0 클라/서버 해시 일치
- [ ] 스폰/배틀/메뉴 GUI 정상, 5~10분 안정
- [ ] (collection 포함) 전설 조우 시 모델 정상(인형 아님)

---

## 8. 오픈 질문 / 결정 필요

1. **재배포 라이선스**: overrides에 직접 동봉(b 그룹·PoroMonCore 외)할 모드의 라이선스가 재배포 허용인가? CF에 있는 건 manifest 참조(다운로드 위임)가 안전 — 가능한 한 (a)로 분류.
2. **JEI vs EMI 택1**(`client_mod_tiers.md` §2-2): 둘 다 동봉 중복 → 운영자 택1?
3. **한글 리소스팩 번들 여부**: 현재 translatable+모드 내장 ko_kr로 충분(task.md §4k). 누락 발견 시 `PoroMon-Korean-Pack` overrides 동봉(localization_policy §8.3).
4. **배포 채널 우선순위**: CF 단독 vs CF+Modrinth 동시(packwiz로 단일 소스화 시점).
5. **PoroMonCore CF 등재 가능성**: 불가(자체 모드) 전제. 영구 overrides 번들 확정.

## 9. 관련 문서
- 클라 티어: `client_mod_tiers.md` · 서버/클라 분리: `server_mod_separation.md`
- 모드팩 목록: `modpack_list.md` · jar 감사: `jar_feature_audit.md`
- 유저 설치 가이드(작성 예정): `user_install_guide.md` · export 메모(작성 예정): `export_notes.md`
- 한글화: `../05_operations/localization_policy.md`
- 스크립트: `../../scripts/{sync-server-mods.sh, extract-curseforge-pack.sh}`
