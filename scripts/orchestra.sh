#!/usr/bin/env bash
set -e

MAIN="$HOME/dev/poro-server"
REVIEW="$HOME/dev/poro-server-review"

cmd="$1"
shift || true

case "$cmd" in
  status)
    "$MAIN/scripts/ai_status.sh"
    ;;

  main)
    cd "$MAIN"
    echo "=== MAIN: master / Claude worktree ==="
    git status --short
    echo
    echo "Run Claude with:"
    echo "  claude"
    ;;

  review)
    cd "$REVIEW"
    echo "=== REVIEW: codex-review / Codex worktree ==="
    git status --short
    echo
    echo "Run Codex with:"
    echo "  codex"
    ;;

  to-review)
    "$MAIN/scripts/ai_to_review.sh"
    ;;

  to-master)
    "$MAIN/scripts/ai_to_master.sh"
    ;;

  diff-main)
    cd "$MAIN"
    echo "=== MAIN diff stat ==="
    git diff --stat
    echo
    echo "=== MAIN staged diff stat ==="
    git diff --cached --stat
    ;;

  diff-review)
    cd "$REVIEW"
    echo "=== REVIEW diff stat ==="
    git diff --stat
    echo
    echo "=== REVIEW staged diff stat ==="
    git diff --cached --stat
    ;;

  commit-main)
    cd "$MAIN"
    if [ -z "$*" ]; then
      echo "ERROR: commit message required."
      echo "Usage: orc commit-main \"message\""
      exit 1
    fi
    git status --short
    git add -A
    git diff --cached --stat
    git commit -m "$*"
    ;;

  commit-review)
    cd "$REVIEW"
    if [ -z "$*" ]; then
      echo "ERROR: commit message required."
      echo "Usage: orc commit-review \"message\""
      exit 1
    fi
    git status --short
    git add -A
    git diff --cached --stat
    git commit -m "$*"
    ;;

  prompt-docs-review)
    cat <<'PROMPT'
현재 변경사항을 docs/canon/archive 관점에서 리뷰해.

검토 기준:
- final_master_plan.md와 각 CANON.md의 역할이 충돌하지 않는가?
- archive 문서가 현재 canon으로 잘못 참조되는 곳이 있는가?
- decision_log.md에 필요한 변경 이유가 기록되어 있는가?
- Status 태그가 누락되거나 이상한 문서가 있는가?
- 깨진 참조, 오래된 §번호/줄번호 참조가 있는가?
- 삭제된 문서가 없는가?

수정은 하지 말고 먼저 보고서만 작성해.
PROMPT
    ;;

  prompt-code-review)
    cat <<'PROMPT'
현재 최신 변경사항을 구현 관점에서 리뷰해.

검토 기준:
- 버그 가능성
- 예외/경계조건 누락
- 데이터 저장/로드 안정성
- 동시성 문제
- 성능 문제
- 테스트 누락
- docs/CANON.md와 구현 충돌
- 보안상 위험한 처리
- 불필요하게 넓은 수정 범위

수정은 하지 말고 먼저 보고서만 작성해.
Critical / Major / Minor / Suggestion으로 나눠서 보고해.
PROMPT
    ;;

  prompt-docs-fix)
    cat <<'PROMPT'
방금 리뷰에서 발견한 docs/canon/archive 문제 중 Critical/Major만 수정해.

규칙:
- 코드 파일 수정 금지
- 삭제 금지
- archive 문서는 현재 canon으로 승격하지 말 것
- decision_log.md가 필요하면 함께 업데이트
- 수정 후 변경 파일 목록과 이유를 보고

수정 범위:
- docs/
- CLAUDE.md
- AGENTS.md
PROMPT
    ;;

  help|"")
    cat <<'HELP'
Poro Server local AI orchestra

Usage:
  orc status
  orc main
  orc review
  orc to-review
  orc to-master
  orc diff-main
  orc diff-review
  orc commit-main "message"
  orc commit-review "message"
  orc prompt-docs-review
  orc prompt-code-review
  orc prompt-docs-fix

Typical loop:
  # Claude worktree
  orc main
  claude
  orc commit-main "work summary"
  orc to-review

  # Codex worktree
  orc review
  codex
  orc commit-review "review summary"
  orc to-master
HELP
    ;;

  *)
    echo "Unknown command: $cmd"
    echo "Run: orc help"
    exit 1
    ;;
esac
