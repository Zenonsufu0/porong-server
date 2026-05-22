#!/usr/bin/env bash
set -e

MAIN="$HOME/dev/poro-server"
REVIEW="$HOME/dev/poro-server-review"

echo "=== Checking review worktree ==="
cd "$REVIEW"

if [ -n "$(git status --short)" ]; then
  echo "ERROR: review worktree is not clean."
  echo "Commit or stash changes first."
  git status --short
  exit 1
fi

echo "=== Checking main worktree ==="
cd "$MAIN"

if [ -n "$(git status --short)" ]; then
  echo "ERROR: main worktree is not clean."
  echo "Commit or stash changes first."
  git status --short
  exit 1
fi

echo "=== Merging codex-review into master ==="
git merge codex-review

echo
echo "=== Syncing review worktree with latest master ==="
cd "$REVIEW"
git merge master

echo
echo "=== Done ==="
cd "$MAIN"
git status --short
git log --oneline --graph --decorate -5
