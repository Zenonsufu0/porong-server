# bb-ref-curator

Use this skill when collecting, naming, sorting, and evaluating visual references for Poro Server Blockbench assets.

## Goal
Create a clean reference library before modeling. Do not build models in this skill.

## Inputs
- Source images, screenshots, web references, exported PNGs, or downloaded preview images.
- Optional source URL / game name / character or weapon name.

## Output
- Files placed into the reference library.
- A short `reference_notes.md` entry with:
  - source_game
  - source_name
  - source_url when available
  - intended_weapon_type
  - selected/support/unsorted
  - useful_features
  - simplification_notes
  - copyright_note: reference only, transformed Poro Server design required

## Folder Rules
Use this root when available:

```txt
assets/source/items/weapons/_reference_library/
```

Recommended structure:

```txt
_reference_library/<source>/
├─ 02_character_sets/
├─ 03_selected_by_weapon_type/
│  ├─ 01_greatsword/
│  ├─ 02_katana/
│  ├─ 03_spear/
│  ├─ 04_shortsword/
│  ├─ 05_doublesword/
│  ├─ 06_gun/
│  ├─ 07_staff/
│  ├─ 08_orbs/
│  ├─ 09_arks/
│  └─ support_future/
├─ 04_costume_mood/
└─ 99_unsorted/
```

## Selection Rules
A reference can be `selected` only if it has at least one of:
- clear full silhouette
- enough angle coverage to infer volume
- strong Poro-compatible identity
- realistic simplification path for Minecraft Java Edition item scale

Use `support` for:
- close-up details
- partial weapon shots
- palette/mood/costume references
- effect references

Use `99_unsorted` for:
- unknown weapon type
- too cropped to classify
- potentially useful but not actionable yet

## Do Not
- Do not copy a commercial design one-to-one.
- Do not start Blockbench construction.
- Do not over-classify uncertain references.
