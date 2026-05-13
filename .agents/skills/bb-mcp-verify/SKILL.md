---
name: bb-mcp-verify
description: Verify the currently open Blockbench project using MCP only, without direct file editing.
---

# Blockbench MCP Verify

Use this skill when the user wants live verification inside Blockbench.

## Rules
- Use Blockbench MCP only.
- Do not directly edit `.bbmodel` files.
- If MCP is unavailable, stop immediately and report the failure.
- Prefer:
  1. outline inspection
  2. selected part verification
  3. screenshot capture if useful

## Output
Report:
- whether MCP connection succeeded
- what project/file appears to be open
- changed part names visible in the outline
- whether the current Blockbench view matches the intended design
