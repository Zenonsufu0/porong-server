# bb-export-register

Use this skill after a model is accepted.

## Goal
Export, organize, and record the finished asset for the Poro Server resource pipeline.

## Outputs
Recommended:

```txt
exports/final/<asset_id>.bbmodel
exports/final/<asset_id>.png
exports/renders/<asset_id>_front.png
exports/renders/<asset_id>_angle.png
reference_notes.md update
asset_registry_entry.json
```

## asset_registry_entry.json Minimum Schema
```json
{
  "asset_id": "",
  "category": "",
  "display_name_ko": "",
  "source_inspiration": [],
  "poro_transform_notes": "",
  "final_files": {
    "bbmodel": "",
    "texture": "",
    "renders": []
  },
  "status": "accepted|needs_review|deprecated"
}
```

## Rules
- Keep source references recorded.
- Mark the asset as transformed Poro Server design, not direct reproduction.
- Do not overwrite previous accepted assets without backup.
