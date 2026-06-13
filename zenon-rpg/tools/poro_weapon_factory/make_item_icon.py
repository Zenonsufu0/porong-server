#!/usr/bin/env python3
from __future__ import annotations

import argparse
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont


def load_font(size: int):
    candidates = [
        "/mnt/c/Windows/Fonts/malgun.ttf",
        "/mnt/c/Windows/Fonts/malgunbd.ttf",
        "/usr/share/fonts/truetype/nanum/NanumGothic.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    ]
    for p in candidates:
        if Path(p).exists():
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def crop_alpha(im: Image.Image) -> Image.Image:
    im = im.convert("RGBA")
    bbox = im.getbbox()
    return im.crop(bbox) if bbox else im


def fit_square(im: Image.Image, size: int, pad_ratio: float = 0.04) -> Image.Image:
    im = im.convert("RGBA")
    max_w = int(size * (1 - pad_ratio * 2))
    max_h = int(size * (1 - pad_ratio * 2))
    scale = min(max_w / im.width, max_h / im.height)

    nw = max(1, int(im.width * scale))
    nh = max(1, int(im.height * scale))

    resized = im.resize((nw, nh), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    canvas.alpha_composite(resized, ((size - nw) // 2, (size - nh) // 2))
    return canvas


def make_preview(asset_id: str, display_name: str, tex: Image.Image, out_path: Path) -> None:
    font_title = load_font(22)
    font_small = load_font(16)

    canvas = Image.new("RGBA", (512, 512), (230, 230, 230, 255))
    draw = ImageDraw.Draw(canvas)

    draw.rounded_rectangle(
        (64, 48, 448, 432),
        radius=24,
        fill=(245, 245, 245, 255),
        outline=(180, 180, 180, 255),
        width=2,
    )

    big = tex.resize((352, 352), Image.Resampling.NEAREST)
    canvas.alpha_composite(big, (80, 56))

    draw.text((64, 448), display_name, fill=(20, 20, 20, 255), font=font_title)
    draw.text((64, 476), asset_id, fill=(70, 70, 70, 255), font=font_small)

    canvas.save(out_path)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--id", required=True)
    ap.add_argument("--source", required=True)
    ap.add_argument("--display-name", default=None)
    ap.add_argument("--out-root", required=True)
    ap.add_argument("--angle", type=float, default=-45.0)
    ap.add_argument("--width-boost", type=float, default=1.12)
    args = ap.parse_args()

    asset_id = args.id
    display_name = args.display_name or asset_id
    out_root = Path(args.out_root)

    tex_dir = out_root / "02_textures"
    preview_dir = out_root / "06_preview"
    rp_tex_dir = out_root / "03_resourcepack/assets/minecraft/textures/item/poro_weapons"

    tex_dir.mkdir(parents=True, exist_ok=True)
    preview_dir.mkdir(parents=True, exist_ok=True)
    rp_tex_dir.mkdir(parents=True, exist_ok=True)

    im = Image.open(args.source).convert("RGBA")
    im = crop_alpha(im)

    # 세로 원화를 아이템용으로 살짝 넓힌 뒤 대각선 회전
    boosted_w = max(1, int(im.width * args.width_boost))
    im = im.resize((boosted_w, im.height), Image.Resampling.LANCZOS)

    rot = im.rotate(args.angle, expand=True, resample=Image.Resampling.BICUBIC)

    tex256 = fit_square(rot, 256, 0.03)
    tex128 = fit_square(rot, 128, 0.03)

    tex256_path = tex_dir / f"{asset_id}_item_256.png"
    tex128_path = tex_dir / f"{asset_id}_item_128.png"
    preview_path = preview_dir / f"{asset_id}_item_preview.png"

    tex256.save(tex256_path)
    tex128.save(tex128_path)
    make_preview(asset_id, display_name, tex256, preview_path)

    # 실제 리소스팩용 최종 텍스처는 대각선 아이콘으로 교체
    tex256.save(rp_tex_dir / f"{asset_id}.png")

    print("✅ item icon generated")
    print(f"- 256: {tex256_path}")
    print(f"- 128: {tex128_path}")
    print(f"- preview: {preview_path}")
    print(f"- resourcepack texture updated: {rp_tex_dir / f'{asset_id}.png'}")


if __name__ == "__main__":
    main()
