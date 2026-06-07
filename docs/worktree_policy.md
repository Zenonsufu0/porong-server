# Worktree 운영 정책 (monorepo + git worktree + sparse-checkout)

> 포로 서버 monorepo는 **하나의 합본 저장소**를 두고, 프로젝트별 개발/실행은
> **별도 worktree**에서 한다. 각 worktree는 자기 프로젝트 폴더만 sparse-checkout한다.
> 이 문서가 worktree 운영의 단일 기준이다.

---

## 1. 역할 분담

| 경로 | 역할 | 서버 실행 | VS Code(개발용) |
|---|---|---|---|
| `/home/zenonsufu1/dev/porong-server` | **합본/merge/기록용** 원본. 전체 구조 확인. | ❌ 안 함 | ❌ 안 엶 |
| `/home/zenonsufu1/dev/porong-work-rpg` | RPG 개발/실행 worktree | ✅ Paper RPG | `porong-work-rpg/porong-rpg` |
| `/home/zenonsufu1/dev/porong-work-mon` | PoroMon 개발/실행 worktree | ✅ Fabric/Cobblemon | `porong-work-mon/porong-mon` |
| `/home/zenonsufu1/dev/porong-work-discord` | Discord 봇 개발/실행 worktree | ✅ Discord 봇 | `porong-work-discord/porong-discord` |

- **원본 `porong-server`에서는 서버를 실행하지 않고, 개발용 VS Code도 열지 않는다.**
- 모든 merge·태그·전체 히스토리 작업은 원본 `porong-server`에서 한다.
- **서버 실행물의 표준 위치는 각 worktree의 `<프로젝트>/.local/server`** 다 (아래 §5).
  - RPG: `porong-work-rpg/porong-rpg/.local/server` (Paper: paper.jar·world·plugins·config·logs 등)
  - PoroMon: `porong-work-mon/porong-mon/.local/server` (Fabric/Cobblemon: run·worlds·mods·server jar 등)
  - `.local/`은 `.gitignore`(`**/.local/`)로 어느 worktree에서도 추적되지 않는다. (DL-130)

---

## 2. monorepo 폴더 구조 (in-repo)

monorepo 안의 프로젝트 폴더 이름은 **`porong-` 접두사 기준**이다(2026-06-07 `poro-`→`porong-` rename, DL-131). 이 이름은
SoT 경로(`porong-rpg/docs/final_master_plan.md`)·`.gitignore`·다수 문서가 직접
참조하므로 **함부로 rename하지 않는다.**

```
porong-server/
├─ porong-rpg/      RPG 서버 + 전역 설계 문서(docs/ SoT)
├─ porong-mon/       모드 서버/모드팩
├─ porong-discord/  디스코드 봇
├─ scripts/       레포 전역 보조 스크립트 (공용)
├─ docs/          레포 전역 운영 문서 (이 파일 포함, 공용)
├─ CLAUDE.md / AGENTS.md / README.md / LICENSE  (공용)
└─ .gitignore / .mcp.json / .agents / .claude / .codex (공용)
```

> **worktree 디렉토리 이름**(`porong-work-rpg` 등)과 **in-repo 폴더 이름**(`porong-rpg` 등)은
> 별개 레이어다. worktree 디렉토리 이름은 git 추적 내용과 무관하므로 자유롭게 바꿔도
> (`git worktree move`) 커밋·문서에 영향이 없다. in-repo 폴더 이름은 영향이 크다.

---

## 3. 각 worktree의 sparse-checkout (cone 모드)

각 worktree는 자기 프로젝트 폴더만 체크아웃한다. `--cone` 모드에서는 **최상위
폴더 이름과 정확히 일치**해야 한다(틀리면 빈 worktree가 된다).

```bash
# RPG
cd /home/zenonsufu1/dev/porong-work-rpg
git sparse-checkout init --cone
git sparse-checkout set porong-rpg

# PoroMon
cd /home/zenonsufu1/dev/porong-work-mon
git sparse-checkout init --cone
git sparse-checkout set porong-mon

# Discord (봇이 RPG/PoroMon 문서를 참조하므로 docs 하위만 추가 포함)
cd /home/zenonsufu1/dev/porong-work-discord
git sparse-checkout init --cone
git sparse-checkout set porong-discord porong-rpg/docs porong-mon/docs
```

| worktree | sparse 대상 | 비고 |
|---|---|---|
| `porong-work-rpg` | `porong-rpg` | |
| `porong-work-mon` | `porong-mon` | |
| `porong-work-discord` | `porong-discord` `porong-rpg/docs` `porong-mon/docs` | docs는 참조 전용. RPG/PoroMon 코드는 안 가져옴 |

> sparse-checkout 설정은 **worktree-local**(`.git/worktrees/<name>/sparse-checkout`)이며
> 커밋되지 않는다. 새 클론/새 worktree에서는 매번 다시 설정한다.
>
> ⚠️ **rename 전환기 주의:** 위 sparse 대상(`porong-rpg`/`porong-mon`/`porong-discord`)은
> **master 기준** 새 폴더명이다. feature 브랜치(`feature/rpg-dev`/`feature/poromon-dev`/`feature/discord-dev`)에는
> 아직 이번 rename이 머지되지 않아 옛 폴더명(`poro-rpg`/`poromon`/`poro-discord`)이 그대로다.
> 각 worktree의 실제 `git sparse-checkout set …`을 새 이름으로 바꾸는 것은 **해당 feature 브랜치에
> 이번 rename이 머지된 뒤**에 한다(미리 바꾸면 빈 worktree가 됨).

---

## 4. 작업 흐름

1. 각 프로젝트는 자기 worktree에서 자기 feature 브랜치로 개발한다.
   - RPG: `feature/rpg-dev` / PoroMon: `feature/poromon-dev` / Discord: `feature/discord-dev`
2. commit은 해당 worktree에서, **사용자 승인 후** 한다.
3. merge는 원본 `porong-server`(master)에서 한다.
4. 충돌이 나면 임의 해결하지 않고 충돌 파일·원인을 먼저 보고한다.

---

## 5. 파일 배치 규칙

- **프로젝트 전용 파일은 해당 하위 폴더 안에만** 만든다
  (`porong-rpg/`, `porong-mon/`, `porong-discord/`).
- monorepo **루트에는 공용 파일만** 둔다
  (`README.md`, `CLAUDE.md`, `AGENTS.md`, `LICENSE`, `scripts/`, `docs/`, `.gitignore` 등).
- 서버 실행 산출물은 추적하지 않는다 — `.gitignore`로 관리:
  `*/server/`, `world*/`, `logs/`, `build/`, `*.jar`, `mods/`,
  `.local/`(런타임 데이터) 등.
- **실행물의 표준 배치 = `<프로젝트>/.local/server`** (worktree-local 런타임). 원본
  `porong-server` 작업트리 안에는 서버 실행물을 두지 않는다. (2026-06-05 정리, DL-130)
  - RPG → `porong-work-rpg/porong-rpg/.local/server`
  - PoroMon → `porong-work-mon/porong-mon/.local/server`
  - 직접 만든 산출물만 추적: 플러그인 소스 `porong-rpg/custom-plugins/`,
    커스텀 모드 소스 `porong-mon/custom-mods/`, 모드팩 메타/오버라이드 `porong-mon/modpack/`.

---

## 6. 새 서버/새 하위 프로젝트 추가 절차

1. monorepo 루트에 새 폴더를 만든다(예: `porong-foo/`). 전용 파일은 그 안에만 둔다.
2. 새 worktree를 만든다:
   `git worktree add /home/zenonsufu1/dev/porong-work-foo <branch>`
3. 그 worktree에서 새 폴더만 sparse-checkout한다:
   `git sparse-checkout init --cone && git sparse-checkout set porong-foo`
4. 이 문서 표에 추가한다.

---

## 7. 현재 worktree 목록 (참고)

`git -C /home/zenonsufu1/dev/porong-server worktree list` 결과:

```
porong-server        [master]              ← 합본/merge/기록 (main)
porong-server-review [codex-review]        ← 코드리뷰 전용(별도 운영)
porong-work-discord  [feature/discord-dev] ← sparse: porong-discord porong-rpg/docs porong-mon/docs
porong-work-mon      [feature/poromon-dev] ← sparse: porong-mon
porong-work-rpg      [feature/rpg-dev]     ← sparse: porong-rpg
```

> **브랜드 전환 메모(Poro → Porong):** worktree **디렉토리** 이름은 모두 `porong-*`로 전환 완료했다
> (main 원본 폴더 포함 → `/home/zenonsufu1/dev/porong-server`). main 폴더는 `git worktree move`가
> 지원하지 않아 `mv` + `git worktree repair`로 옮기고 worktree 연결을 검증했다.
> **GitHub repo 이름**도 `poro_server` → `porong-server`로 변경 완료했고, origin remote URL을
> `https://github.com/Zenonsufu0/porong-server.git`로 갱신했다(전 worktree 공유).
> **in-repo 폴더 이름**도 `poro-rpg`/`poromon`/`poro-discord` → `porong-rpg`/`porong-mon`/`porong-discord`로
> rename 완료했다(2026-06-07, master, DL-131). Java 패키지·mod id·assets 네임스페이스·Gradle 내부명·item id/config key는
> **변경하지 않았다.** feature 브랜치(`feature/*-dev`)는 아직 옛 폴더명이며, 머지 후 각 worktree sparse-checkout을
> 새 이름으로 재설정한다(§3 전환기 주의 참조).
