---
name: bb-design
description: Turn reference images and an existing .bbmodel into a Blockbench-ready design spec for one greatsword variant.
---

# Greatsword Design Spec

Use this skill before modeling.

## Goal
Create or update `design.md` for the current variant folder.

## Inputs
- files in `refs/`
- working file in `source/`
- `task.md` if present

## Output format
Write `design.md` with:
1. overall proportions
2. part list
3. symmetry rules
4. color palette
5. thickness guide
6. construction phases
7. must-not-break constraints

## Rules
- Do not model yet unless explicitly asked.
- Prefer part names that match Blockbench outliner groups.
- If references conflict, say so and choose one direction.
- Keep the design Minecraft-style and block-readable.
