# bb-texture-pass

Use this skill only after a blockout has been accepted.

## Goal
Apply palette, UV mapping, face-level color separation, highlights, shadows, and simple material cues.

## Inputs
- approved `.bbmodel`
- `asset_brief.md`
- `ref_spec.json`
- primary/support references

## Output
- updated `.bbmodel`
- texture PNG(s)
- `texture_notes.md`

## Texture Rules
- Prefer a small consistent palette.
- Use clear separation between primary material, secondary trim, shadow, and glow.
- Minecraft Java Edition item readability is more important than realism.
- Strong contrast beats subtle gradients at icon/hand scale.

## Suggested Palette Roles
```txt
primary: main metal/blade/body
secondary: trim/frame
accent: gem/stripe/edge color
shadow: dark structure, grip, holes
highlight: edge light
fx/glow: magical or energy area
```

## Face Painting Rules
- Front faces should be clearest.
- Side/back faces may be darker.
- Avoid noisy random pixels.
- Avoid semi-transparent alpha unless silhouette shaping requires it.
- Use 0 or 255 alpha only for Minecraft Java item textures unless the pipeline explicitly supports another mode.

## Do Not
- Do not rebuild the whole geometry unless a critical texture issue exposes a geometry problem.
- Do not use one color everywhere.
- Do not add details that destroy the silhouette.
