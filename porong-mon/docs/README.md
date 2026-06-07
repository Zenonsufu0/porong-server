# PoroMon Docs

> PoroMon(포로몬) 문서 루트 / 서버 기본 컨셉 + 문서 인덱스.
> 권위 규칙은 레포 루트 `CLAUDE.md`. 본 문서는 그 요약 + 길잡이.

---

## 1. 서버 기본 컨셉

**PoroMon = Cobblemon 기반 Fabric 모드 서버.** Poro 브랜드의 별도 라인이며, 메인 Poro RPG(Paper) 서버와 무관하다.

- **무엇인가**: 자유 생존형 포켓몬 탐험 서버. 탐험·포획·건축·채굴·농사·거래·자연 마을 사용은 자유.
- **무엇을 통제하나**: 고임팩트 엔드게임만 서버가 중앙 통제 — 레전드 조우, 메가/테라 해금, 짐/배지, 리그/챔피언, 시즌. 일반 플레이는 과잉 통제하지 않는다.
- **역할 분담**:
  - **Cobblemon** = 포켓몬 엔진
  - **Mega Showdown** = 메가/테라/다이맥스/Z무브 배틀 기믹
  - **PoroMonCore** = 서버 고유 진행/보상/제한/허브 규칙 엔진(커스텀 Fabric 모드)
- **허브 중앙집중**: 짐·배지·레전드 제단·메가/테라 연구·배틀타워·리그·챔피언·마켓을 허브에 모은다(자유 탐험은 그대로).

### 코어 루프
탐험 → 포획 → 건축/채집 → 재화 → 짐 도전 → 메가/테라 해금 → 레전드 티켓 → 리그/챔피언

### 핵심 정책(요약)
- PvP **전면 비활성**(포켓몬 배틀은 Cobblemon이 별도 처리) — `decisions.md` 011
- 보호는 **허브(스폰) 건물만**, 일반 월드는 claim 없음 — `decisions.md` 012
- 레전드는 **티켓 + 사설 제단룸**(공개 랜덤 스폰 금지)
- 초기엔 기존 포켓몬 타입/종족값 불변, 커스텀 포켓몬 대량 추가 금지

### 기술 베이스라인
| 항목 | 값 |
|---|---|
| Minecraft | 1.21.1 |
| 로더 | Fabric (Loader 0.18.4) |
| Java | 21 |
| 베이스 모드팩 | Cobblemon Official [Fabric] 1.7.3 + Mega Showdown + SimpleTMs + Eggs Addon + Legendary Monuments |
| 커스텀 모드 | `custom-mods/poromon-core` (PoroMonCore) |
| 개발 환경 | WSL Ubuntu / VS Code + Claude Code |

---

## 2. 문서 인덱스

상태: ✅ 작성됨 · 📝 TODO

### 00_project — 프로젝트 방향
- ✅ [overview.md](00_project/overview.md) — 서버 방향/코어 루프
- ✅ [decisions.md](00_project/decisions.md) — 결정 기록(001~012)
- 📝 [roadmap.md](00_project/roadmap.md) — 단계별 로드맵

### 01_modpack — 모드팩
- ✅ [modpack_list.md](01_modpack/modpack_list.md) — 80개 모드 목록(+SimpleTMs, Eggs, Legendary Monuments)
- ✅ [server_mod_separation.md](01_modpack/server_mod_separation.md) — 서버/클라 분리(필수·권장·제외·애매) + 테스트 체크리스트
- ✅ [jar_feature_audit.md](01_modpack/jar_feature_audit.md) — jar 내부 기준 기능 검토(LM/Eggs/SimpleTMs/MSD ID·한글화·충돌)
- 📝 [client_pack_policy.md](01_modpack/client_pack_policy.md) — 클라 배포 팩 정책
- 📝 [export_notes.md](01_modpack/export_notes.md) — CurseForge export 메모

### 02_server — 서버 운영
- ✅ [server_setup.md](02_server/server_setup.md) — server/run 구성 + start.sh/eula + PoroMonCore 선행조건
- ✅ [world_policy.md](02_server/world_policy.md) — 월드/게임규칙/백업/시즌
- ✅ [protection_policy.md](02_server/protection_policy.md) — 허브 보호/그리핑
- ✅ [server_runbook.md](02_server/server_runbook.md) — 기동/정지/백업/롤백/장애

### 03_poromoncore — 커스텀 모드
- ✅ [poromoncore_spec.md](03_poromoncore/poromoncore_spec.md) — 역할/0.1 스코프/매니저
- ✅ [module_structure.md](03_poromoncore/module_structure.md) — 패키지/모듈/의존/초기화
- ✅ [config_structure.md](03_poromoncore/config_structure.md) — 설정 파일 스키마
- ✅ [database_schema.md](03_poromoncore/database_schema.md) — 런타임 데이터 저장
- ✅ [commands.md](03_poromoncore/commands.md) — `/poromon` 명령 트리
- ✅ [menu_design.md](03_poromoncore/menu_design.md) — 리그 패스 아이템 정책 + 메뉴 GUI 레이아웃(결정 024 하이브리드)

### 04_game_design — 게임 설계
- ✅ [hub_design.md](04_game_design/hub_design.md) — 허브 구성
- ✅ [legendary_encounter.md](04_game_design/legendary_encounter.md) — 레전드 조우 흐름
- ✅ [mega_tera_unlock.md](04_game_design/mega_tera_unlock.md) — 메가/테라 해금
- ✅ [economy_design.md](04_game_design/economy_design.md) — 경제(골드 단일)
- ✅ [shop_design.md](04_game_design/shop_design.md) — 상점(7구역/게이트/골드 단일)
- ✅ [shop_catalog_0.1.md](04_game_design/shop_catalog_0.1.md) — 상점 카탈로그(품목·ID 표, ID는 TODO)
- ✅ [encounter_pool_design.md](04_game_design/encounter_pool_design.md) — 전설 조우권 등급별 풀
- ✅ [egg_pool_design.md](04_game_design/egg_pool_design.md) — 알 등급별 풀(커스텀 가능 여부 확인 필요)
- ✅ [gym_badge_design.md](04_game_design/gym_badge_design.md) — 짐/배지
- ✅ [league_season_design.md](04_game_design/league_season_design.md) — 리그/시즌

### 05_operations — 운영
- ✅ [localization_policy.md](05_operations/localization_policy.md) — 한글화 정책(유저 표시 텍스트 한국어)
- 📝 [admin_policy.md](05_operations/admin_policy.md) · [balance_policy.md](05_operations/balance_policy.md) · [known_issues.md](05_operations/known_issues.md) · [user_install_guide.md](05_operations/user_install_guide.md)

---

## 3. 읽는 순서(신규 합류자)
1. `CLAUDE.md`(루트) → 본 README §1
2. `00_project/overview.md`, `decisions.md`
3. `01_modpack/server_mod_separation.md` → `02_server/server_setup.md`
4. `03_poromoncore/poromoncore_spec.md` → `module_structure.md`

## 4. 현재 진행 / 다음 단계
- ✅ 모드팩 분석, 서버/클라 분리 설계, 서버 정책 4종, PoroMonCore 설계 4종
- ▶ 다음: 서버 최소구성 기동 테스트(`server_setup.md` 선행조건) → PoroMonCore Gradle 골격 → `/poromon` 0.1

## 5. 정리 메모(중복 파일)
루트의 다음 4개는 하위 폴더 문서와 **중복되는 빈 파일** — 삭제 또는 통합 대상:
`docs/economy_plan.md`, `docs/hub_plan.md`, `docs/poromon_design.md`, `docs/poromoncore_spec.md`
(정식 위치: `04_game_design/economy_design.md`, `04_game_design/hub_design.md`, `00_project/overview.md`, `03_poromoncore/poromoncore_spec.md`)
