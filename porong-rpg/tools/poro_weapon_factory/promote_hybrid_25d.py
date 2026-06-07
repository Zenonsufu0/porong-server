#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path
from PIL import Image, ImageDraw


def write_parts_texture(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)

    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)

    # 4x4 swatches
    d.rectangle((0, 0, 3, 3), fill=(220, 132, 24, 255))      # bright gold/orange
    d.rectangle((4, 0, 7, 3), fill=(126, 70, 28, 255))       # dark bronze
    d.rectangle((8, 0, 11, 3), fill=(63, 35, 22, 255))       # handle brown
    d.rectangle((12, 0, 15, 3), fill=(255, 72, 0, 255))      # ember red

    d.rectangle((0, 4, 3, 7), fill=(255, 210, 70, 255))      # highlight gold
    d.rectangle((4, 4, 7, 7), fill=(90, 40, 18, 255))        # dark red brown
    d.rectangle((8, 4, 11, 7), fill=(255, 150, 0, 255))      # flame orange
    d.rectangle((12, 4, 15, 7), fill=(35, 20, 14, 255))      # deep shadow

    # Fill lower half with usable neutral variants
    d.rectangle((0, 8, 7, 15), fill=(180, 95, 30, 255))
    d.rectangle((8, 8, 15, 15), fill=(50, 25, 18, 255))

    im.save(path)


def face(texture: str, uv):
    return {"texture": texture, "uv": uv}


def all_faces(texture: str, uv):
    return {
        "north": face(texture, uv),
        "south": face(texture, uv),
        "east": face(texture, uv),
        "west": face(texture, uv),
        "up": face(texture, uv),
        "down": face(texture, uv),
    }


def side_faces(texture: str, uv):
    return {
        "east": face(texture, uv),
        "west": face(texture, uv),
        "up": face(texture, uv),
        "down": face(texture, uv),
    }


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--id", required=True)
    ap.add_argument("--rp", required=True, help="03_resourcepack folder")
    ap.add_argument("--namespace", default="minecraft")
    args = ap.parse_args()

    asset_id = args.id
    ns = args.namespace
    rp = Path(args.rp)

    model_dir = rp / "assets" / ns / "models" / "item"
    item_dir = rp / "assets" / ns / "items"
    tex_dir = rp / "assets" / ns / "textures" / "item" / "poro_weapons"

    model_dir.mkdir(parents=True, exist_ok=True)
    item_dir.mkdir(parents=True, exist_ok=True)
    tex_dir.mkdir(parents=True, exist_ok=True)

    main_tex = tex_dir / f"{asset_id}.png"
    if not main_tex.exists():
        raise FileNotFoundError(f"main texture not found: {main_tex}")

    parts_tex = tex_dir / f"{asset_id}_parts.png"
    write_parts_texture(parts_tex)

    model = {
        "parent": "minecraft:item/handheld",
        "ambientocclusion": False,
        "textures": {
            "weapon": f"{ns}:item/poro_weapons/{asset_id}",
            "parts": f"{ns}:item/poro_weapons/{asset_id}_parts",
            "particle": f"{ns}:item/poro_weapons/{asset_id}"
        },
        "elements": [
            {
                "name": "weapon_front_back_png_plane",
                "from": [2.0, 0.0, 7.96],
                "to": [14.0, 16.0, 8.04],
                "faces": {
                    "north": {"texture": "#weapon", "uv": [0, 0, 16, 16]},
                    "south": {"texture": "#weapon", "uv": [0, 0, 16, 16]}
                }
            },

            # Blade side ribs: front image를 덮지 않고, 옆에서만 얇은 두께감 부여
            {
                "name": "blade_left_side_rib",
                "from": [6.85, 5.5, 7.35],
                "to": [7.15, 15.0, 8.65],
                "faces": side_faces("#parts", [0, 0, 4, 4])
            },
            {
                "name": "blade_right_side_rib",
                "from": [8.85, 5.5, 7.35],
                "to": [9.15, 15.0, 8.65],
                "faces": side_faces("#parts", [0, 0, 4, 4])
            },

            # Guard / handle / pommel volume
            {
                "name": "guard_center_volume",
                "from": [6.6, 4.35, 6.95],
                "to": [9.4, 5.75, 9.05],
                "faces": all_faces("#parts", [0, 4, 4, 8])
            },
            {
                "name": "guard_left_volume",
                "from": [3.2, 4.25, 7.05],
                "to": [6.8, 5.25, 8.95],
                "faces": all_faces("#parts", [0, 0, 4, 4])
            },
            {
                "name": "guard_right_volume",
                "from": [9.2, 4.25, 7.05],
                "to": [12.8, 5.25, 8.95],
                "faces": all_faces("#parts", [0, 0, 4, 4])
            },
            {
                "name": "handle_volume",
                "from": [7.25, 0.8, 7.0],
                "to": [8.75, 4.65, 9.0],
                "faces": all_faces("#parts", [8, 0, 12, 4])
            },
            {
                "name": "pommel_volume",
                "from": [6.85, 0.0, 7.0],
                "to": [9.15, 1.0, 9.0],
                "faces": all_faces("#parts", [4, 0, 8, 4])
            },

            # Center gem overlay
            {
                "name": "front_center_ember_gem",
                "from": [7.2, 4.65, 6.75],
                "to": [8.8, 6.15, 7.05],
                "faces": {
                    "north": {"texture": "#parts", "uv": [12, 0, 16, 4]}
                }
            }
        ],
        "display": {
            "thirdperson_righthand": {
                "rotation": [0, -90, 42],
                "translation": [0, 4.2, 1],
                "scale": [1.55, 1.55, 1.55]
            },
            "thirdperson_lefthand": {
                "rotation": [0, 90, -42],
                "translation": [0, 4.2, 1],
                "scale": [1.55, 1.55, 1.55]
            },
            "firstperson_righthand": {
                "rotation": [0, -90, 12],
                "translation": [0.8, 3.1, 0.6],
                "scale": [1.12, 1.12, 1.12]
            },
            "firstperson_lefthand": {
                "rotation": [0, 90, -12],
                "translation": [0.8, 3.1, 0.6],
                "scale": [1.12, 1.12, 1.12]
            },
            "gui": {
                "rotation": [0, 0, 0],
                "translation": [0, 0, 0],
                "scale": [1.0, 1.0, 1.0]
            },
            "ground": {
                "rotation": [0, 0, 0],
                "translation": [0, 3, 0],
                "scale": [1.0, 1.0, 1.0]
            },
            "fixed": {
                "rotation": [0, 0, 0],
                "translation": [0, 0, 0],
                "scale": [1.15, 1.15, 1.15]
            }
        }
    }

    (model_dir / f"{asset_id}.json").write_text(
        json.dumps(model, ensure_ascii=False, indent=2),
        encoding="utf-8"
    )

    item_def = {
        "model": {
            "type": "minecraft:model",
            "model": f"{ns}:item/{asset_id}"
        }
    }

    (item_dir / f"{asset_id}.json").write_text(
        json.dumps(item_def, ensure_ascii=False, indent=2),
        encoding="utf-8"
    )

    print("✅ hybrid 2.5D model generated")
    print(f"- model: {model_dir / f'{asset_id}.json'}")
    print(f"- item: {item_dir / f'{asset_id}.json'}")
    print(f"- parts texture: {parts_tex}")


if __name__ == "__main__":
    main()
