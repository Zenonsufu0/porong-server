# bb-asset-brief

Use this skill after reference curation and before building a model.

## Goal
Convert one selected reference set into a small, buildable Poro Server asset plan.

## Required Output
Create or update:

```txt
asset_brief.md
ref_spec.json
```

## asset_brief.md Sections
```md
# <asset_id>

## Role
weapon_type:
server_set_or_theme:
combat_identity:

## Reference Inputs
- primary_ref:
- support_refs:
- source_note:

## Poro Transformation
What is kept:
What is changed:
What is removed:

## Minecraft Readability
Viewed in hand, the model must read as:
Silhouette priority:
Color priority:

## Blockout Plan
Main parts:
- blade/head
- guard/core
- handle/shaft
- pommel/back module

## Texture Plan
Palette:
Material cues:
Glow/accent zones:

## Risk Notes
Top 3 risks:
```

## ref_spec.json Minimum Schema
```json
{
  "asset_id": "",
  "category": "greatsword|katana|spear|gun|staff|orb|ark|other",
  "source_family": "",
  "theme": "",
  "reference_files": {
    "primary": "",
    "support": []
  },
  "dimensions_bb": {
    "target_length_y": 0,
    "max_width_x": 0,
    "max_depth_z": 0
  },
  "parts": [],
  "palette": {
    "primary": "",
    "secondary": "",
    "accent": "",
    "shadow": "",
    "glow": ""
  },
  "simplification_rules": [],
  "validation_flags": []
}
```

## Rules
- Keep it buildable. Avoid huge wall-of-text specs.
- Do not include the entire reference analysis. Keep only decisions.
- Do not build geometry here.
