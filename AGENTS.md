# Zenon Server — Agent Rules

## Scope
- Default response language is Korean.
- Keep changes minimal and targeted.
- Do not modify unrelated areas.
- `zenon-economy/`·`zenon-gun/`는 **구상 단계(개발 미착수)** 프로젝트다. 문서만 존재하며 런타임·빌드 산출물·플랫폼 템플릿(Gradle/Fabric/Paper)을 만들지 않는다. 착수는 사용자 명시 요청 시에만.

## Forbidden paths unless explicitly requested
- `zenon-rpg/server/`
- `security/`
- `ops/`
- `tests/`
- `scripts/`
- `zenon-rpg/custom-plugins/`
- `.github/`

## Asset work
- Blockbench source assets live under `zenon-rpg/assets/source`.
- Exported pack outputs belong under `zenon-rpg/assets/export/resourcepack`.
- When working under `zenon-rpg/assets/source`, also follow the deeper AGENTS files.

---

## Documentation source of truth

모든 설계 판단은 아래 계층 순서로 읽는다. 위 문서가 아래 문서보다 항상 우선한다.

| 계층 | 문서 | 역할 |
|---|---|---|
| 1 | `zenon-rpg/docs/final_master_plan.md` | 프로젝트 전체 방향성·시스템 연결·우선순위의 최상위 기준 |
| 2 | 각 프로젝트 docs의 `CANON.md` (RPG: `zenon-rpg/docs/NN_*/CANON.md`) | 도메인별 현재 공식 기준 (수치·구조·충돌 처리 포함) |
| 3 | `zenon-rpg/docs/decision_log.md` | 설계 결정 이력. DL-NNN 형식으로 무엇을/왜/근거 문서를 기록 |
| 4 | 각 도메인 하위 `*.md` | CANON이 위임한 세부 수치·스펙·구현 참조 |
| 5 | `zenon-rpg/docs/_archive/` | **폐기 문서.** 현재 설계 판단에 사용 금지 (아래 참조) |

### 도메인별 CANON 진입점

| 도메인 | 공식 기준 |
|---|---|
| 플러그인/구현 경계 | `zenon-rpg/docs/01_plugin_architecture/CANON.md` |
| DB/API/경제/통계 | `zenon-rpg/docs/02_database_api_stats/CANON.md` |
| 디스코드 온보딩 | `zenon-discord/docs/index.md` |
| 전투/무기/스킬 | `zenon-rpg/docs/04_combat_weapon_skills/CANON.md` |
| 영지/농사/공방 | `zenon-rpg/docs/05_island_farm_system/CANON.md` |
| 필드/보스/드랍 | `zenon-rpg/docs/06_fields_bosses/CANON.md` |
| 보스 패턴 | `zenon-rpg/docs/07_boss_pattern_modules/index.md` |
| GUI/리소스팩 | `zenon-rpg/docs/08_resourcepack_pipeline/index.md` |
| 약관/운영정책 | `zenon-rpg/docs/09_terms_and_policy/index.md` |
| 개발 로드맵 | `zenon-rpg/docs/10_development_roadmap/index.md` |
| 맵 디자인 | `zenon-rpg/docs/12_map_design/` |
| Archive | `zenon-rpg/docs/_archive/README.md` |

---

## Archive 문서 사용 규칙

`zenon-rpg/docs/_archive/` 안의 모든 파일은 **현재 canon이 아니다.**

- archive 문서의 수치·구조·결정을 현재 설계 기준으로 인용하지 않는다.
- 어떤 문서가 archive로 이동된 이유와 대체 문서는 `zenon-rpg/docs/_archive/README.md`에서 확인한다.
- 구버전 `00_master_plan.md`, `economy_numbers_v1.md`, `atk_dps_baseline_v1.md` 등은 모두 대체 문서가 있다.
- archive 파일을 읽는 경우: 결정 이력 추적 또는 과거 설계 맥락 파악 목적에 한한다.

---

## Codex 역할 정의

Codex는 기본적으로 **리뷰 / QA / docs consistency** 담당이다. 구현자가 아니다.

### 기본 행동 (명시적 요청 없을 때)

- 프로젝트 docs 일관성 검토: CANON ↔ 하위 문서 수치·명칭 충돌 발견 및 보고
- `decision_log.md` 참조 경로 유효성 확인 (깨진 §번호, 이동된 파일 경로 등)
- `_archive/README.md` 누락 항목 확인 (archive 폴더 파일과 README 항목 대조)
- 구현 코드 리뷰: 설계 문서 기준 대비 로직 일치 여부 확인
- 테스트 케이스 제안: 경계 조건, 수치 오버플로, 상태 전이 검증 항목 제안
- README/AGENTS.md/CLAUDE.md 업데이트 제안

### 코드 수정 규칙

- 코드 파일 수정은 **사용자가 명시적으로 요청할 때만** 수행한다.
- `zenon-rpg/custom-plugins/`, `zenon-rpg/server/`, `scripts/`, `tests/` 는 명시적 요청 없이 수정하지 않는다.
- 발견된 코드 버그나 개선점은 수정하지 않고 **보고서 형태로 출력**한다.

### 항상 금지

- archive 문서를 canon으로 승격하거나 인용하는 것
- 확정되지 않은 수치(미확정 M-tag)를 임의로 채워 넣는 것
- 삭제: 파일 삭제, 코드 삭제, 문서 삭제 (archive로의 이동은 사용자 명시 요청 필요)
- 비밀값 커밋: 토큰, IP, 비밀번호, Discord 봇 토큰, API 키, DB 자격증명

---

## 아이디어 문서 반영 검토

리뷰 시 "대화·작업에서 나온 아이디어가 문서에 반영됐는지" 확인한다.

### 검토 항목

- `zenon-rpg/docs/idea_inbox.md`에 `[PROMOTED]`·`[폐기]` 처리 없이 오래 방치된 항목이 있는가?
- 최근 커밋의 변경 내용이 CANON.md 또는 decision_log.md에 기록됐는가?
- decision_log.md의 최신 DL-NNN이 실제 파일 변경과 일치하는가?

### 누락 발견 시

- 문서 반영이 빠진 항목은 **수정하지 않고** 아래 형식으로 보고한다:

```
[아이디어 누락 발견]
- 누락 항목:
- 분류 제안: CANON 반영 / decision_log 기록 / idea_inbox DRAFT / 폐기
- 반영 대상 문서:
- 근거:
```

- 대규모 CANON 변경이 필요하다고 판단되더라도 직접 수정하지 않는다. 먼저 보고하고 사용자 확인 후 수정한다.

---

## 작업 완료 보고 형식

모든 작업 완료 후 반드시 아래 형식으로 보고한다.  
항목이 해당 없으면 "해당 없음"으로 명시한다. 생략하지 않는다.

```
[작업 완료 보고]
- 변경 요약:
- 수정/생성한 파일:
- 반영한 사용자 아이디어:
- 문서 반영 상태:
  - CANON 반영:
  - DRAFT/보류 (idea_inbox):
  - decision_log 기록:
  - 미반영 (이유):
- 검증:
  - git status:
  - git diff --stat:
  - git diff --check:
- 남은 위험/미확정:
- 다음 단계 (제안):
```
