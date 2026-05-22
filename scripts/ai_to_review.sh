#!/usr/bin/env bash
set -e

MAIN="$HOME/dev/poro-server"
REVIEW="$HOME/dev/poro-server-review"

echo "=== Checking main worktree ==="
cd "$MAIN"

if [ -n "$(git status --short)" ]; then
  echo "ERROR: main worktree is not clean."
  echo "Commit or stash changes first."
  git status --short
  exit 1
fi

echo "=== Checking review worktree ==="
cd "$REVIEW"

if [ -n "$(git status --short)" ]; then
  echo "ERROR: review worktree is not clean."
  echo "Commit or stash changes first."
  git status --short
  exit 1
fi

echo "=== Merging master into codex-review ==="
git merge master

echo
echo "=== Review worktree is ready ==="
git status --short
git log --oneline --graph --decorate -5

echo
echo "Run Codex now:"
echo "  codex"
