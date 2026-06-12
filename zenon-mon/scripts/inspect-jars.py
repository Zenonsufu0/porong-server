#!/usr/bin/env python3
import json
import zipfile
from pathlib import Path

ROOT = Path("/home/zenonsufu1/dev/poro-server-poromon")
MODS = ROOT / "modpack/client/mods"
OUT_DIR = ROOT / "reports/jar_inspection"
OUT_DIR.mkdir(parents=True, exist_ok=True)

TARGET_KEYWORDS = [
    "legendary", "monument", "simple", "tm", "egg",
    "mega", "showdown", "cobblemon"
]

def is_target(name: str) -> bool:
    low = name.lower()
    return any(k in low for k in TARGET_KEYWORDS)

summary_rows = []

for jar in sorted(MODS.glob("*.jar")):
    if not is_target(jar.name):
        continue

    report = []
    report.append(f"# {jar.name}\n")

    try:
        with zipfile.ZipFile(jar) as z:
            names = z.namelist()

            # fabric.mod.json
            if "fabric.mod.json" in names:
                try:
                    data = json.loads(z.read("fabric.mod.json").decode("utf-8"))
                    report.append("## fabric.mod.json\n")
                    report.append("```json")
                    report.append(json.dumps(data, ensure_ascii=False, indent=2))
                    report.append("```\n")
                    mod_id = data.get("id", "UNKNOWN")
                    mod_name = data.get("name", jar.name)
                    env = data.get("environment", "*")
                except Exception as e:
                    mod_id = "UNKNOWN"
                    mod_name = jar.name
                    env = "UNKNOWN"
                    report.append(f"fabric.mod.json read error: {e}\n")
            else:
                mod_id = "UNKNOWN"
                mod_name = jar.name
                env = "UNKNOWN"
                report.append("No fabric.mod.json found.\n")

            summary_rows.append((jar.name, mod_id, mod_name, env))

            # namespaces
            assets = sorted({p.split("/")[1] for p in names if p.startswith("assets/") and len(p.split("/")) > 2})
            data_ns = sorted({p.split("/")[1] for p in names if p.startswith("data/") and len(p.split("/")) > 2})

            report.append("## Namespaces\n")
            report.append(f"- assets: {assets}\n")
            report.append(f"- data: {data_ns}\n")

            # lang files
            lang_files = [p for p in names if "/lang/" in p and p.endswith(".json")]
            report.append("## Lang files\n")
            for p in lang_files[:100]:
                report.append(f"- {p}")
            if len(lang_files) > 100:
                report.append(f"- ... and {len(lang_files) - 100} more")
            report.append("")

            # likely item/model/loot/data/config files
            interesting = []
            for p in names:
                low = p.lower()
                if (
                    p.endswith(".json")
                    and (
                        "/models/item/" in low
                        or "/items/" in low
                        or "/item/" in low
                        or "/loot" in low
                        or "/recipes/" in low
                        or "/tags/" in low
                        or "/worldgen/" in low
                        or "/structures/" in low
                        or "/advancements/" in low
                        or "trade" in low
                        or "villager" in low
                        or "egg" in low
                        or "legend" in low
                        or "spawn" in low
                        or "pokemon" in low
                        or "species" in low
                        or "tm" in low
                        or "tr" in low
                        or "mega" in low
                    )
                ):
                    interesting.append(p)

            report.append("## Interesting JSON/resources\n")
            for p in interesting[:300]:
                report.append(f"- {p}")
            if len(interesting) > 300:
                report.append(f"- ... and {len(interesting) - 300} more")
            report.append("")

            # dump lang json snippets
            for p in lang_files:
                if p.endswith("en_us.json") or p.endswith("ko_kr.json"):
                    try:
                        text = z.read(p).decode("utf-8", errors="replace")
                        report.append(f"## {p}\n")
                        report.append("```json")
                        report.append(text[:20000])
                        if len(text) > 20000:
                            report.append("\n... truncated ...")
                        report.append("```\n")
                    except Exception as e:
                        report.append(f"Could not read {p}: {e}\n")

    except Exception as e:
        report.append(f"JAR read error: {e}\n")

    out = OUT_DIR / f"{jar.name}.md"
    out.write_text("\n".join(report), encoding="utf-8")

summary = ROOT / "reports/jar_inspection_summary.md"
with summary.open("w", encoding="utf-8") as f:
    f.write("# Jar Inspection Summary\n\n")
    f.write("| Jar | Mod ID | Name | Environment |\n")
    f.write("|---|---|---|---|\n")
    for row in summary_rows:
        f.write("| " + " | ".join(str(x).replace("|", "\\|") for x in row) + " |\n")

print(f"written: {summary}")
print(f"reports: {OUT_DIR}")
