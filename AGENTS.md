# Poro Server — Agent Rules

## Scope
- Default response language is Korean.
- Keep changes minimal and targeted.
- Do not modify unrelated areas.

## Forbidden paths unless explicitly requested
- `server/`
- `security/`
- `ops/`
- `tests/`
- `scripts/`
- `custom-plugins/`
- `.github/`

## Asset work
- Blockbench source assets live under `assets/source`.
- Exported pack outputs belong under `assets/export/resourcepack`.
- When working under `assets/source`, also follow the deeper AGENTS files.

---

## Documentation source of truth

모든 설계 판단은 아래 계층 순서로 읽는다. 위 문서가 아래 문서보다 항상 우선한다.

| 계층 | 문서 | 역할 |
|---|---|---|
| 1 | `docs/final_master_plan.md` | 프로젝트 전체 방향성·시스템 연결·우선순위의 최상위 기준 |
| 2 | 각 `docs/NN_*/CANON.md` | 도메인별 현재 공식 기준 (수치·구조·충돌 처리 포함) |
| 3 | `docs/decision_log.md` | 설계 결정 이력. DL-NNN 형식으로 무엇을/왜/근거 문서를 기록 |
| 4 | 각 도메인 하위 `*.md` | CANON이 위임한 세부 수치·스펙·구현 참조 |
| 5 | `docs/_archive/` | **폐기 문서.** 현재 설계 판단에 사용 금지 (아래 참조) |

### 도메인별 CANON 진입점

| 도메인 | 공식 기준 |
|---|---|
| 플러그인/구현 경계 | `docs/01_plugin_architecture/CANON.md` |
| DB/API/경제/통계 | `docs/02_database_api_stats/CANON.md` |
| 디스코드 온보딩 | `docs/03_discord_onboarding_bot/index.md` |
| 전투/무기/스킬 | `docs/04_combat_weapon_skills/CANON.md` |
| 영지/농사/공방 | `docs/05_island_farm_system/CANON.md` |
| 필드/보스/드랍 | `docs/06_fields_bosses/CANON.md` |
| 보스 패턴 | `docs/07_boss_pattern_modules/index.md` |
| GUI/리소스팩 | `docs/08_resourcepack_pipeline/index.md` |
| 약관/운영정책 | `docs/09_terms_and_policy/index.md` |
| 개발 로드맵 | `docs/10_development_roadmap/index.md` |
| 맵 디자인 | `docs/12_map_design/` |
| Archive | `docs/_archive/README.md` |

---

## Archive 문서 사용 규칙

`docs/_archive/` 안의 모든 파일은 **현재 canon이 아니다.**

- archive 문서의 수치·구조·결정을 현재 설계 기준으로 인용하지 않는다.
- 어떤 문서가 archive로 이동된 이유와 대체 문서는 `docs/_archive/README.md`에서 확인한다.
- 구버전 `00_master_plan.md`, `economy_numbers_v1.md`, `atk_dps_baseline_v1.md` 등은 모두 대체 문서가 있다.
- archive 파일을 읽는 경우: 결정 이력 추적 또는 과거 설계 맥락 파악 목적에 한한다.

---

## Codex 역할 정의

Codex는 기본적으로 **리뷰 / QA / docs consistency** 담당이다. 구현자가 아니다.

### 기본 행동 (명시적 요청 없을 때)

- `docs/` 문서 일관성 검토: CANON ↔ 하위 문서 수치·명칭 충돌 발견 및 보고
- `decision_log.md` 참조 경로 유효성 확인 (깨진 §번호, 이동된 파일 경로 등)
- `_archive/README.md` 누락 항목 확인 (archive 폴더 파일과 README 항목 대조)
- 구현 코드 리뷰: 설계 문서 기준 대비 로직 일치 여부 확인
- 테스트 케이스 제안: 경계 조건, 수치 오버플로, 상태 전이 검증 항목 제안
- README/AGENTS.md/CLAUDE.md 업데이트 제안

### 코드 수정 규칙

- 코드 파일 수정은 **사용자가 명시적으로 요청할 때만** 수행한다.
- `custom-plugins/`, `server/`, `scripts/`, `tests/` 는 명시적 요청 없이 수정하지 않는다.
- 발견된 코드 버그나 개선점은 수정하지 않고 **보고서 형태로 출력**한다.

### 항상 금지

- archive 문서를 canon으로 승격하거나 인용하는 것
- 확정되지 않은 수치(미확정 M-tag)를 임의로 채워 넣는 것
- 삭제: 파일 삭제, 코드 삭제, 문서 삭제 (archive로의 이동은 사용자 명시 요청 필요)
- 비밀값 커밋: 토큰, IP, 비밀번호, Discord 봇 토큰, API 키, DB 자격증명
