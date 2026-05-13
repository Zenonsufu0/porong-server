---
name: bb-build-phase
description: Build or edit one detailed Blockbench modeling phase at a time for the current greatsword variant.
---

# Greatsword Phase Builder

Use this skill when the user wants actual modeling work for the current greatsword variant.

## Goal
Model or edit the current asset in clearly separated phases so that structure is stabilized before ornament and pixel polish.

## Required behavior
- Read `design.md` first.
- Read `task.md` if present.
- Identify the current target variant folder.
- Work on one phase only, then stop.
- Do not silently continue into the next phase.
- Preserve already approved work unless the user explicitly asks for rework.

## Phases
- Phase 1: silhouette
- Phase 2: major structure
- Phase 3: secondary structure
- Phase 4: ornament
- Phase 5: pixel detail and polish

## Phase rules

### Phase 1: silhouette
Purpose:
- establish overall proportions and the main readable shape

Allowed:
- blade core mass
- rough guard mass
- handle
- pommel base
- major overall length/width adjustment

Not allowed:
- fine trims
- gems
- decorative wings
- small pixel detail
- color breakup for texture effect

Success criteria:
- the weapon reads clearly at a glance
- overall proportions match the reference direction
- left/right symmetry is correct unless intentional asymmetry is specified

### Phase 2: major structure
Purpose:
- establish the main structural layers and primary part hierarchy

Allowed:
- blade_core
- blade_trim
- guard main forms
- wing_guard main forms
- handle layer separation
- pommel main shape
- major recesses or protrusions

Not allowed:
- fine ornament
- embedded gems
- tiny block accents
- noisy detail

Success criteria:
- the large forms are properly layered
- the weapon has clear structural depth
- major part naming and grouping are clean

### Phase 3: secondary structure
Purpose:
- refine structure and prepare clean bases for decoration

Allowed:
- grooves
- inner channels
- guard inner support shapes
- additional trim supports
- embedded sockets for gems
- edge step-downs
- handle wrap structure layers
- pommel support frames

Not allowed:
- decorative gem faces
- flashy ornament clusters
- final pixel polish

Success criteria:
- flat zones are reduced
- ornament anchor points are prepared
- decorative parts will have structural support

### Phase 4: ornament
Purpose:
- add decorative identity without breaking readability

Allowed:
- gold trims
- gems
- wing decorations
- emblem-like accent structures
- decorative channels
- ornamental edge accents

Rules:
- decorative parts must connect to a structural block
- gems should appear embedded, not floating
- ornament should enhance silhouette, not replace structure
- keep symmetry unless asymmetry is explicitly requested

Not allowed:
- noisy mosaic color breakup
- unsupported floating decorative cubes
- overfilling every surface

Success criteria:
- ornament looks intentional and supported
- the asset becomes visually distinctive
- large readable masses are still preserved

### Phase 5: pixel detail and polish
Purpose:
- add controlled micro detail and finish quality

Allowed:
- small pixel/block accents
- subtle color/value variation
- controlled edge highlights
- minor shape cleanup
- smoothing of awkward transitions
- removal of visually floating or noisy detail

Rules:
- avoid random mosaic color placement
- use small pixel clusters with intention
- prefer 2-3 value steps for shading expression
- preserve Minecraft readability
- do not damage the approved silhouette or main structure

Success criteria:
- detail density feels rich but controlled
- surfaces are no longer too flat
- the weapon still looks clean and readable from game distance

## Tool order
1. Inspect the on-disk `.bbmodel`
2. Use Blockbench MCP if live confirmation, outline inspection, or direct manipulation is needed
3. If MCP fails, stop and report unless direct file editing fallback is explicitly allowed

## Output
After editing, report:
- target file
- current phase
- parts changed
- whether MCP was used
- whether Blockbench reopen/reload is needed
- what should be done in the next phase
