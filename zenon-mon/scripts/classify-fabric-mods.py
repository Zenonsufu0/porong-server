#!/usr/bin/env python3
import json
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MODS = ROOT / "modpack/client/mods"
OUT = ROOT / "reports/mod_classification.md"

rows = []

for jar in sorted(MODS.glob("*.jar")):
    mod_id = "UNKNOWN"
    name = jar.name
    version = "UNKNOWN"
    env = "UNKNOWN"
    side_guess = "UNKNOWN"
    reason = ""

    try:
        with zipfile.ZipFile(jar) as z:
            if "fabric.mod.json" in z.namelist():
                data = json.loads(z.read("fabric.mod.json").decode("utf-8"))
                mod_id = data.get("id", "UNKNOWN")
                name = data.get("name", jar.name)
                version = data.get("version", "UNKNOWN")
                env = data.get("environment", "*")

                if env == "client":
                    side_guess = "CLIENT_ONLY"
                    reason = 'fabric.mod.json environment="client"'
                elif env == "server":
                    side_guess = "SERVER_ONLY"
                    reason = 'fabric.mod.json environment="server"'
                else:
                    side_guess = "COMMON_OR_UNKNOWN"
                    reason = 'environment is "*" or missing; server test required'
            else:
                reason = "fabric.mod.json not found"
    except Exception as e:
        reason = f"read error: {e}"

    rows.append((jar.name, mod_id, name, version, env, side_guess, reason))

OUT.parent.mkdir(parents=True, exist_ok=True)

with OUT.open("w", encoding="utf-8") as f:
    f.write("# Mod Classification Report\n\n")
    f.write("> This is an automatic first-pass classification. Dedicated server boot testing is still required.\n\n")
    f.write("| Jar | Mod ID | Name | Version | Environment | Guess | Reason |\n")
    f.write("|---|---|---|---|---|---|---|\n")
    for row in rows:
        f.write("| " + " | ".join(str(x).replace("|", "\\|") for x in row) + " |\n")

print(f"written: {OUT}")
print(f"jars: {len(rows)}")
