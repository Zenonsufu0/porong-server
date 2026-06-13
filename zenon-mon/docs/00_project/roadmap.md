# Zenon Mon Roadmap (로드맵)

> 단계별 진행 계획. 상위 컨셉: `../README.md` §1, 결정: `decisions.md`.
> 원칙(CLAUDE.md): 작게·안정적으로. 엔드게임 전체를 한 번에 구현하지 않는다.
> 표기: ✅ 완료 · ▶ 진행중 · ⬜ 예정

---

## Phase 0 — 설계 / 문서 (현재)
**목표: 코딩 전 방향·구조 고정.**
- ✅ 모드팩 분석 (manifest/modlist/overrides)
- ✅ 서버/클라 모드 분리 설계 (`01_modpack/server_mod_separation.md`)
- ✅ 서버 정책 문서 (setup / world / protection / runbook)
- ✅ ZenonMonCore 설계 (spec / module / config / database / commands)
- ✅ 문서 인덱스 + 서버 컨셉 (`README.md`)
- ⬜ 남은 설계 문서: roadmap(본 문서) 외 economy/gym/league, 05_operations

**완료 기준:** 신규 합류자가 README → 설계 문서만으로 구조 이해 가능.

---

## Phase 1 — 서버 최소구성 기동
**목표: ZenonMonCore 없이 베이스 서버가 안정적으로 뜨고 접속된다.**
- ⬜ Java 21 / Fabric server(Loader 0.18.4, MC 1.21.1) 설치
- ⬜ `server/run` 구성: `eula.txt`, `server.properties`(pvp=false, spawn-protection), `start.sh`
- ⬜ `scripts/sync-server-mods.sh` SOURCE_DIR 확정 → 서버 필수+권장 모드만 동기화
- ⬜ 각 jar `fabric.mod.json` 검증으로 `server_mod_separation.md` 확정(owo/accessories/bookshelf/prickle)
- ⬜ 최소구성 기동 → Cobblemon 1.7.3 + MSD 로드 확인
- ⬜ 클라 모드팩으로 접속 + 배틀/스폰 동기화 확인

**완료 기준:** `server_mod_separation.md` §5 체크리스트 통과, 5~10분 TPS 안정.
**의존:** Phase 0. **블로커:** 클라 인스턴스 설치(jar 출처).

---

## Phase 2 — ZenonMonCore 골격
**목표: 빈 커스텀 모드가 서버에 로드된다.**
- ⬜ `custom-mods/zenon-mon-core` Fabric(Loom) Gradle 프로젝트 생성
- ⬜ 버전 핀 확정(Yarn/Loom/Fabric API/Cobblemon 좌표)
- ⬜ 빈 `ModInitializer` + `fabric.mod.json`(main/client entrypoint) 빌드 성공
- ⬜ jar를 `server/run/mods`에 투입 → 로드 로그 확인, Cobblemon/MSD와 충돌 없음
- ⬜ Cobblemon API 연동 PoC(`CobblemonEvents` 리스너 1개 로그)

**완료 기준:** ZenonMonCore가 Cobblemon+MSD와 함께 정상 로드.
**의존:** Phase 1.

---

## Phase 3 — ZenonMonCore 0.1 (최소 기능)
**목표: spec 0.1 스코프 구현.** (`zenonmoncore_spec.md` / `module_structure.md` §7)
- ⬜ ConfigManager — core/tickets/rooms.json 로드 + 기본값 생성
- ⬜ AuditLog + 기본 로깅
- ⬜ PlayerProgressManager — PersistentState 저장 스캐폴드
- ⬜ MenuItemManager — 9번 슬롯 League Pass(지급/복원/드롭방지)
- ⬜ MenuGuiManager — 우클릭 GUI(허브 텔레포트 버튼)
- ⬜ HubInteractionManager — `/zenonmon hub` 텔레포트
- ⬜ EncounterTicket / InstanceRoom — **데이터 모델만**
- ⬜ AdminCommandManager — `/zenonmon`, `menu`, `hub`, `progress`, `admin reload`

**완료 기준:** 접속 시 League Pass 지급 → 우클릭 GUI → 허브 이동, `admin reload` 동작.
**의존:** Phase 2. **비포함:** 레전드 스폰/메가 해금/짐/리그 로직.

---

## Phase 4 — 진행/조우 시스템
**목표: 티켓·레전드 조우 루프 실동작.**
- ⬜ EncounterTicketManager 발급/소모 + 검증
- ⬜ InstanceRoomManager 룸 배정/입장/정리 + RoomSession 영속
- ⬜ LegendaryEncounterManager — 가중 테이블(`legendary.json`) 스폰/배틀/정리
- ⬜ 게임 설계 반영: `legendary_encounter.md`
- ⬜ RewardManager(기초) + EconomyBridge 추상화

**완료 기준:** 티켓 → 제단 → 사설룸 → 레전드 조우 → 결과 로깅 → 룸 정리 1사이클.
**의존:** Phase 3.

---

## Phase 5 — 짐 / 배지 / 해금
- ⬜ GymBadgeManager — 짐 클리어 기록/배지 (`gym_badge_design.md` 작성 선행)
- ⬜ MegaUnlockManager — 메가/테라/다이맥스 해금 게이트 (`mega_tera_unlock.md`)
- ⬜ 보상 연계(배지→해금→티켓 구매권)

**완료 기준:** 짐 클리어 → 배지 → 메가 해금 진행선 동작.
**의존:** Phase 4.

---

## Phase 6 — 경제 / 리그 / 시즌 (엔드게임)
- ⬜ 경제 설계/연동 (`economy_design.md`)
- ⬜ 리그/챔피언/배틀타워 (`league_season_design.md`)
- ⬜ SeasonManager — 시즌 기록/리셋(월드 와이프 분리)

**완료 기준:** 시즌 단위 리그 운영 가능.
**의존:** Phase 5.

---

## Phase 7 — 배포 / 운영
- ⬜ 공식 클라 팩 배포(CurseForge/Modrinth) — `client_pack_policy.md`
- ⬜ 유저 설치 가이드 — `05_operations/user_install_guide.md`
- ⬜ 운영 정책 — `admin_policy.md`, `balance_policy.md`, `known_issues.md`
- ⬜ 백업 자동화(`scripts/backup-server.sh`) + 모니터링

**완료 기준:** "앱 설치 → 팩 설치 → Play → 접속" 흐름 완성.

---

## 즉시 다음 액션 (Top 3)
1. **클라 인스턴스 설치** → `sync-server-mods.sh` SOURCE_DIR 확정 (Phase 1 블로커)
2. **`server/run` 기동 파일** 작성: `eula.txt`, `server.properties`, `start.sh`
3. 최소구성 기동 후 → **ZenonMonCore Gradle 골격** (Phase 2)

## 관련
- 컨셉/인덱스: `../README.md` · 결정: `decisions.md`
- 서버 셋업: `../02_server/server_setup.md` · 모드 분리: `../01_modpack/server_mod_separation.md`
- 코어 설계: `../03_zenonmoncore/module_structure.md`
