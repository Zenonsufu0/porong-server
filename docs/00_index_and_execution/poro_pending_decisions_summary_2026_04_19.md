# 결정 대기 집대성 — 2026-04-19 세션 진행 상황 v2

## 문서 목적
본 문서는 **현재 사용자 결정·리뷰 대기 중인 항목**을 한 문서에 집약해 다음 결정 라운드를 가속하기 위한 보조 문서다. v2는 2026-04-19 후반 이후 쌓인 자문 권장안·PR 리뷰 대기를 중심으로 재편.

---

## 1. 이전 묶음 A~E — 전부 해결됨 (2026-04-19 전반)

| 묶음 | 내용 | 상태 |
|---|---|---|
| A. CSV CI 정책 3건 | tools/ 신설 / Python 3.11 / 리포 분리 시즌1 직후 | ✅ 권장안 수용 완료 |
| B. 3월드 블로커 3건 | 보이드 맵 / 동접 방지 선행 / 7×7 + 시즌 경계 재활성화 | ✅ 권장안 수용 완료 |
| C. 경매 훅 선행 3건 | C-1·C-3 실측(wallet/auction 부재) / C-2 CSV 확장 | ✅ 실측·확장 완료 |
| D. 워킹 트리 정리 | 대분류 3커밋 | ✅ 3+1 커밋으로 정리 후 푸시 |
| E. 기타 소규모 4건 | 리소스팩 용량 / 올인원 잔여 4 / Lv6+ / 강화 보조 물약 | ✅ 권장안 수용·위임 완료 |

---

## 2. 현역 결정 대기 — 묶음 F·G·H·I·J (2026-04-19 후반)

자문 응답이 도착하고 사용자 최종 수용 대기 중인 항목.

### 묶음 F — combat-balance 후속 4건 (자문 v2 도착)
참조: `docs/05_classes_and_balance/poro_engraving_dps_gap_review_v2_followup.md`

| # | 항목 | 권장안 | 리스크 |
|---|---|---|---|
| F-1 | 직업각인 분기 안2 v2 | 전사 3무기 × 2~3분기 매트릭스, 독립 카테고리(공용각인 합연산 풀 분리), 전환 쿨 60s 전투 외 토글, ±2%p 클램프 | 전환 쿨 60s 체감·분기 기여 태그 풀 편입 여부 |
| F-2 | 각성의 호흡 쿨감 캡 | **전 직업 -15% 단일 캡**(기존 -20% 하향). 마법사 이원화 비권장. 이동·방어기 별도 유지 | PvP 쿨감 캡 별도 검토 필요 |
| F-3 | 레전드리 월 공급 | 기준선 5~8권 / 하한 3 / 상한 12, 상한 돌파 시 드랍률 30% 자동 하향 + 공지 2주 유예, 동일 IP·동일 파티 월 5권+ 가중치 0.4 | 소규모 서버용 보조 채널 필요성 |
| F-4 | 태그 합연산 +25%p 캡 | **+25%p 유지 확정**. UI "현재치/캡" 바 + 디스코드 `/내각인` 초과 잘림 표기 | 분기 기여가 태그 풀에 편입될 경우 캡 재산정 |

**권장안 전체 수용 시 즉시 반영되는 파일**: `poro_master_planning.md` 각인 섹션 / `poro_common_engraving_master_list_draft.md` / 레지스트리 각인 4건 취소선.

### 묶음 G — 3월드 기술 후속 5건 (자문 v1 도착)
참조: `docs/14_world_and_maps/poro_three_world_technical_followup_v1.md`

| # | 항목 | 권장안 |
|---|---|---|
| G-1 | FAWE 도입 | Paper 1.21.10 호환 FAWE를 `softdepend` 채택 + Lv1 동기 스탬핑 폴백(PR-W3 병행) |
| G-2 | stale `transfer_txn` 만료 | **30분** → `teleporting=2`(EXPIRED) 전이 + 다음 로그인 시 수도 스폰 폴백 + 좌표 보존 |
| G-3 | 영지 락 heartbeat | **10s heartbeat / 65s TTL**(5s 허용창, 6회 누락 허용) |
| G-4 | 스키매틱 포맷 | **Sponge `.schem` 단일 고정**, `plugins/EmpireRPG/schematics/estate_lv{1..5}.schem` |
| G-5 | 시즌 경계 배치 | **신 시즌 오픈 T-10분 단일 batch**(`--dry-run`/`--commit` 2단, 온라인 0명 가드) |

**파급**: FAWE 채택이 4·2·3번을 자동 정합. 원안 20 md → **22 md**(신규 PR 없이 설정/폴백만 삽입).

**권장안 수용 시 반영**: `build.gradle.kts` FAWE `softdepend` 의존성 추가 (`libraries:` manifest 또는 compileOnly) + `plugins/EmpireRPG/schematics/` 폴더 신설 준비.

### 묶음 H — 드랍 후속 3건 (자문 v1 도착)
참조: `docs/01_boss_design_core/poro_boss_drop_followup_v1.md`

| # | 항목 | 권장안 |
|---|---|---|
| H-1 | 카테고리 C 1막 공개 톤 | **중간 노출** — 2체 중 1체 메인 씬 공개 + 1체 히든 루트 소수 공개, 도전 불가 시 월렛 귀속 단서 아이템 지급 |
| H-2 | T2 상위 조각 테마 확대 | **4테마 확대 + 월렛 귀속 + 테마당 보스 1종**. 지방 0.2~0.4개/킬, CP6 상한 10 → 15% |
| H-3 | 1막 4:1 변환 이벤트 | **변형** — 22일차 시작 9~11일 한시 + 일일 상한(커먼→레어 20회 / 레어→에픽 10회) |

**45일차 지표 영향**: 레전드리 공급 불변, CP3 에픽 활성화 15 → 18~22%, 신규 CP-F1~F5 지표 5종.

**권장안 수용 시 반영**: `poro_boss_drop_grade_matrix_v1.md` + data-schema에 지방 시드·변환 이벤트 카운터 테이블 후속 위임.

### 묶음 I — 커스텀 HUD 3레이어 설계 (자문 v1 도착)
참조: `docs/13_external_plugins_and_custom_ui/poro_custom_hud_3layer_design_draft.md`

| # | 항목 | 권장안 |
|---|---|---|
| I-1 | 렌더 매체 | Adventure `sendActionBar()` 단일 멀티라인 + 음수 스페이스 수직 배치 + 리소스팩 font glyph. BossBar는 보스전 전용 보존 |
| I-2 | 바닐라 HP/허기 제거 | `setHealthScale(0)` + `setFoodLevel(20)` + 리소스팩 글리프 투명화(2중 방어) |
| I-3 | 스킬 키 5키 | Q · E(R 대체 확정) · F · 우클릭 · Shift+우클릭. Shift+F·핫바 9 폐기 |
| I-4 | `poro:weapon_type` PDC 게이트 | 비전투 Q/F/E 바닐라 동작 단일 게이트 판정 |
| I-5 | CustomModelData 예약 | `3002000~3002999` HUD 전용 신규 구간, `3001xxx` 월렛 UI와 비충돌 |
| I-6 | 기존 HealthHud 폴백 | 삭제 없이 리소스팩 미적용·체험 월드 강등자용 텍스트 렌더러로 역할 축소 |

**PR 분해**: 필수 7 + 옵션 1 = **10~13 md**, **플래그 저장소 v0.1 PR1~PR8 전체 머지 후 착수**(후행 의존성).

**남은 사소 오픈**: E vs Shift+F 병존 기록 정리(에이전트가 2026-04-19 E 확정 이전 Shift+F 기록이 문서에 있다고 지적 — 완전 삭제 정정 필요).

### 묶음 J — PR 리뷰·머지 2건 (GitHub)
| # | PR | 상태 |
|---|---|---|
| J-1 | PR #1 플래그 저장소 PR0(base=master) | 리뷰 대기. 3파일 / 16 insertions / 0 deletions |
| J-2 | PR #2 CSV CI PR#1 스켈레톤(base=wsl-setup) | 리뷰 대기. 1파일 / 15 insertions / 0 deletions |

**머지 순서 권장**: PR #1 먼저 → PR1(플래그 DDL) 브랜치 착수 가능. PR #2는 병렬 리뷰 OK, 머지 후 CSV CI PR#2(감지 규칙) 착수.

**머지 명령어 예시** (사용자 승인 시 내가 실행 가능):
```
gh pr merge 1 --squash --delete-branch
gh pr merge 2 --squash --delete-branch
```

---

## 3. 우선순위 권고 (결정 라운드 순서)

### 1차 (즉시 블로커 해소)
1. **묶음 F·G·H·I 권장안 전체 수용** — "권장안 수용"만으로 12건 일괄 확정. 4개 자문 문서의 상태 주석·레지스트리·마스터 플래닝 동시 갱신.

### 2차 (구현 파이프라인 열기)
2. **묶음 J PR #1·#2 리뷰·머지** — 머지 후 PR1(플래그 DDL) + CSV CI PR#2(감지 규칙 R1~R3 Python) 즉시 착수.

### 3차 (후속 자문 요청)
3. **남은 사소 오픈**: 직업각인 전환 쿨 60s 체감 / PvP 쿨감 캡 / 소규모 서버 레전드리 보조 채널 / 분기 기여 태그 편입 / FAWE 릴리스 태그 / T-10분 캘린더 / stale 디스코드 채널 ID / 시드 대상 보스 / 4:1 변환 일일 상한 실측 / 카테고리 C 히든 테마 배치 / E vs Shift+F 기록 정리.

### 4차 (튜닝·후속)
4. **45일차 경매 모니터링 대시보드 실측** — 시즌1 운영 후 지표 검증.

---

## 상위 문서 참조
- 마스터플래닝: `../poro_master_planning.md` (2026-04-19 변경 로그 전체)
- 오픈 질문 레지스트리: `poro_open_questions_registry.md`
- PR #1: https://github.com/Zenonsufu0/poro_server/pull/1
- PR #2: https://github.com/Zenonsufu0/poro_server/pull/2
