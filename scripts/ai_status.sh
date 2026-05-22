#!/usr/bin/env bash
set -e

MAIN="$HOME/dev/poro-server"
REVIEW="$HOME/dev/poro-server-review"

echo "=== MAIN: master / Claude worktree ==="
cd "$MAIN"
git status --short
echo
git log --oneline --graph --decorate -3

echo
echo "=== REVIEW: codex-review / Codex worktree ==="
cd "$REVIEW"
git status --short
echo
git log --oneline --graph --decorate -3
