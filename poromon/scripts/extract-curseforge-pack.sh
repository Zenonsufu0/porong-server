#!/usr/bin/env bash
set -euo pipefail

ROOT="/home/zenonsufu1/dev/poro-server-poromon"
ZIP_PATH="${1:-}"

if [ -z "$ZIP_PATH" ]; then
  echo "Usage: ./scripts/extract-curseforge-pack.sh <curseforge-export.zip>"
  exit 1
fi

if [ ! -f "$ZIP_PATH" ]; then
  echo "File not found: $ZIP_PATH"
  exit 1
fi

TMP="/tmp/poromon-pack-extract"

echo "[1/6] Cleaning temp..."
rm -rf "$TMP"
mkdir -p "$TMP"

echo "[2/6] Extracting zip..."
unzip -q "$ZIP_PATH" -d "$TMP"

echo "[3/6] Copying manifest/modlist..."
mkdir -p "$ROOT/modpack/base/manifest"
mkdir -p "$ROOT/modpack/base/mods-list"

if [ -f "$TMP/manifest.json" ]; then
  cp "$TMP/manifest.json" "$ROOT/modpack/base/manifest/manifest.json"
else
  echo "WARN: manifest.json not found"
fi

if [ -f "$TMP/modlist.html" ]; then
  cp "$TMP/modlist.html" "$ROOT/modpack/base/mods-list/modlist.html"
else
  echo "WARN: modlist.html not found"
fi

echo "[4/6] Resetting overrides..."
rm -rf "$ROOT/modpack/overrides"
mkdir -p "$ROOT/modpack/overrides"

echo "[5/6] Copying overrides folder if exists..."
if [ -d "$TMP/overrides" ]; then
  rsync -av "$TMP/overrides/" "$ROOT/modpack/overrides/"
fi

echo "[6/6] Copying selected root folders into overrides..."
for name in config defaultconfigs resourcepacks shaderpacks datapacks kubejs mods fancymenu_data xaero yosbr openloader showdown icon.png instance.png options.txt; do
  if [ -e "$TMP/$name" ]; then
    rsync -av "$TMP/$name" "$ROOT/modpack/overrides/"
  fi
done

echo ""
echo "Done."
echo "Manifest: $ROOT/modpack/base/manifest/manifest.json"
echo "Modlist:  $ROOT/modpack/base/mods-list/modlist.html"
echo "Overrides: $ROOT/modpack/overrides"
