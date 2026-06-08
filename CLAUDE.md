# Poro Server 모노레포 — 전역 규칙

- 기본 응답 언어: 한국어.
- 이 저장소는 Porong(포롱) 서버 프로젝트 모노레포다. 독립 프로젝트로 분리돼 있으며(활성 3 + 구상 2), 코드 공유는 거의 없다.

| 프로젝트 | 폴더 | 단계 | 규칙 / 설계 docs |
|---|---|---|---|
| **porong-rpg** | `porong-rpg/` | 활성 | Paper 플러그인 RPG 서버 (전투·성장·영지·보스·DB/API). → `porong-rpg/CLAUDE.md`, `porong-rpg/docs/` |
| **porong-mon** | `porong-mon/` | 활성 | 모드 서버/모드팩. → `porong-mon/CLAUDE.md`, `porong-mon/docs/` |
| **porong-discord** | `porong-discord/` | 활성 | 디스코드 온보딩/운영 봇 (Python). |
| **porong-economy** | `porong-economy/` | 구상 | 경제/거래/생산/시장 중심 후보 서버. → `porong-economy/docs/` |
| **porong-gun** | `porong-gun/` | 구상 | 총기/전술/생존 후보 서버(컨셉 미확정). → `porong-gun/docs/` |

- **구상 단계 프로젝트(`porong-economy`/`porong-gun`)는 문서만 있다.** 런타임·빌드 산출물·플랫폼 템플릿(Gradle/Fabric/Paper)을 만들지 않는다. 착수는 사용자 명시 요청 시에만.

- 프로젝트 전역 단일 진실 공급원(SoT): **`porong-rpg/docs/final_master_plan.md`** (충돌 시 최우선). 결정 기록: `porong-rpg/docs/decision_log.md` (DL-NNN), 미확정 인박스: `porong-rpg/docs/idea_inbox.md`, 아카이브: `porong-rpg/docs/_archive/`.
- 작업을 시작하기 전, 대상 프로젝트의 `CLAUDE.md`와 관련 docs를 먼저 읽는다.

## 워크트리 작업 범위 (이 체크아웃 = `poro-work-rpg`)

- **이 worktree(`poro-work-rpg`, 브랜치 `feature/rpg-dev`)에서는 RPG(`poro-rpg/`) 관련 작업만 한다.** 포로몬(`poromon`)·디스코드봇(`poro-discord`) 작업은 각자의 worktree(`poro-work-poromon`·`poro-work-discord`)에서 수행하고, 이 세션에서는 **하지 않는다**.
- 포로몬·디스코드 파일은 이 worktree에 없거나 참조 대상일 뿐이다. 필요하면 읽기만 하고 **수정·커밋하지 않는다**.
- 다른 프로젝트 작업 요청이 오면, 잘못된 worktree임을 먼저 알리고 해당 worktree에서 진행하도록 안내한다.

## 안전 / 범위

- 무관한 영역은 수정하지 않는다. 변경은 작업 대상 프로젝트 폴더 안으로 한정한다.
- 코드 구현(플러그인·봇·모드)·서버 설정·테스트·CI는 사용자가 명시적으로 요청할 때만 수행한다.
- 런타임 파일은 Git에 포함하지 않는다: `*/server/`, world/logs/cache/`*.jar` 등은 gitignored 유지 (단 `gradle-wrapper.jar`는 예외 추적).
- 비밀정보(토큰·키·IP·비밀번호·디스코드 봇 토큰·API 키·DB 자격증명)는 절대 커밋하지 않는다.
- 파일 변경 전 변경 대상 파일을 요약하고, 변경 후 변경 파일·내용을 요약한다. 작고 리뷰 가능한 단위를 선호한다.
- commit · merge · push는 사용자 승인 후에만 실행한다.

## 아이디어 문서 반영 프로토콜

사용자가 작업 중 새 아이디어·설정 변경·기획/구현 방향을 말하면 채팅에만 남기지 않는다. 아래 기준으로 분류해 해당 문서에 반영하거나, 반영하지 않으면 이유를 보고한다.

| 분류 | 조건 | 반영 위치 |
|---|---|---|
| CANON 반영 | 확정된 공식 기준 변경 | 관련 프로젝트 docs의 `CANON.md` + `porong-rpg/docs/decision_log.md` |
| 결정 기록 | 이유 있는 설계 결정 | `porong-rpg/docs/decision_log.md` (DL-NNN) |
| DRAFT 보관 | 미확정·검토 필요 | `porong-rpg/docs/idea_inbox.md` |
| 폐기 기록 | 대체·폐기된 내용 | `porong-rpg/docs/decision_log.md` 또는 `porong-rpg/docs/_archive/`에 이유 기록 |
| 미반영 | 위 어디에도 해당 안 됨 | 보고 시 "미반영 이유" 명시 |

`porong-rpg/docs/idea_inbox.md`: 확정 시 CANON 반영 + decision_log 기록 + inbox 항목에 `[PROMOTED → DL-NNN]`. 폐기 시 `[폐기 — 이유]`. 불확실하면 inbox에라도 기록하고 비워두지 않는다.

## 작업 완료 보고 형식

모든 작업 완료 후 아래 형식으로 보고한다. 해당 없으면 "해당 없음"으로 명시하고 생략하지 않는다.

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

commit · merge · push는 자동으로 하지 않는다. 다음 단계는 제안만 한다.
