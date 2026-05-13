# bb-audit-fix

Use this skill when a rendered Blockbench screenshot or in-editor view looks wrong.

## Goal
Perform a small, targeted fix pass. Do not restart from scratch unless the user explicitly requests it.

## Inputs
- current `.bbmodel`
- latest render/screenshot
- reference image(s)
- user complaint or audit goal

## Output
- updated `.bbmodel` or change instructions
- `audit_fix_notes.md` with before/after issues

## Audit Checklist
Score only what matters:
1. silhouette
2. weapon type readability
3. proportion
4. palette separation
5. texture contrast
6. group cleanliness
7. unwanted legacy design contamination

## Fix Limit
Choose 1 to 5 fixes per pass.

Example:
```txt
Fix 1: blade too rectangular → taper upper third
Fix 2: guard too wide → reduce x width by 20%
Fix 3: all-white read → separate trim and core colors
```

## Do Not
- Do not run a huge multi-phase rebuild.
- Do not edit 20 unrelated things.
- Do not preserve a bad shape just because it passed an old checklist.
