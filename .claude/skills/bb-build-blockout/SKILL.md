# bb-build-blockout

Use this skill to create the first Blockbench model pass from `asset_brief.md` and `ref_spec.json`.

## Goal
Make only the large silhouette and proportions. No final texture, no micro-detail.

## Required Inputs
- `asset_brief.md`
- `ref_spec.json`
- selected primary reference image(s)

## Output
- `.bbmodel` with clean groups and named elements
- optional quick front/side screenshot
- short `blockout_notes.md`

## Blockout Priorities
1. In-hand silhouette readability
2. Weapon type recognition
3. Correct big proportions
4. Clean group naming
5. Simple, editable cubes

## Group Naming
Use stable group names:

```txt
blade_group
head_group
guard_group
core_group
handle_group
pommel_group
fx_group
```

Only use groups relevant to the weapon type.

## Category Guidance

### Greatsword
- Long vertical read.
- Strong blade mass.
- Guard must not become wider than the whole weapon identity.
- Avoid overgrown wing guards unless the primary silhouette requires it.

### Katana / Sword
- Thin blade silhouette.
- Guard and pommel should be readable but not bulky.
- Emphasize line, curve, and core accents.

### Spear
- Shaft line must be clean.
- Head shape carries identity.
- Keep head readable at small scale.

### Staff / Wand
- Long shaft plus top identity module.
- Top module can be orb/ring/crystal/crescent.

### Orb / Ark
- Treat as floating device or held focus.
- Emphasize central gem/core and outer frame.

## Hard Limits
- Do not texture-paint final colors here.
- Do not create tiny decorative cubes until blockout is approved.
- Do not force old holy-wing layout on unrelated weapons.
- Keep the model editable.
