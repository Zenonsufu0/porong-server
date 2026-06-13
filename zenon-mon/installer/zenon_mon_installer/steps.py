"""설치 단계 (인스턴스·Fabric·mods·overrides·서버등록·런처프로필).

각 함수는 logger(콜백)와 dry(계획만) 를 받는다. dry=True 면 파일시스템을 건드리지 않고
무엇을 할지 로그만 남긴다(WSL/개발 검증·미리보기용).
"""
import json
import os
import shutil
import subprocess
import sys
import time

from . import nbt
from . import platform as plat


def find_java():
    """Fabric installer headless 실행용 java 경로."""
    jh = os.environ.get("JAVA_HOME")
    if jh:
        exe = "java.exe" if sys.platform.startswith("win") else "java"
        cand = os.path.join(jh, "bin", exe)
        if os.path.isfile(cand):
            return cand
    return "java"  # PATH 위임(공식 런처 JRE 또는 시스템 java)


def install_fabric(installer_jar, mc_version, loader_version, mc_dir, log, dry):
    """Fabric loader 를 .minecraft/versions 에 설치(공식 installer headless)."""
    if not os.path.isfile(installer_jar):
        if dry:
            log(f"[Fabric] (dry) installer 없음: {installer_jar}")
            return
        raise FileNotFoundError(f"fabric-installer.jar 없음: {installer_jar}")
    cmd = [find_java(), "-jar", installer_jar, "client",
           "-mcversion", mc_version, "-loader", loader_version,
           "-dir", mc_dir, "-noprofile"]
    log(f"[Fabric] {mc_version} / loader {loader_version} 설치")
    if dry:
        log("  (dry) " + " ".join(cmd))
        return
    os.makedirs(mc_dir, exist_ok=True)
    subprocess.run(cmd, check=True)


def place_mods(bundle_mods, files, target_mods, log, dry):
    """선택된 mods 를 인스턴스 mods/ 에 배치(관리 폴더 = 기존 jar 청소 후 복사)."""
    log(f"[mods] {len(files)}개 → {target_mods}")
    missing = [f for f in files if not os.path.isfile(os.path.join(bundle_mods, f))]
    if missing:
        raise FileNotFoundError(f"번들 mods 누락 {len(missing)}: {missing[:3]}…")
    if dry:
        for f in files:
            log(f"  + {f}")
        return
    os.makedirs(target_mods, exist_ok=True)
    for ex in os.listdir(target_mods):
        if ex.endswith(".jar"):
            os.remove(os.path.join(target_mods, ex))
    for f in files:
        shutil.copy2(os.path.join(bundle_mods, f), os.path.join(target_mods, f))


def copy_overrides(bundle_overrides, names, target_dir, log, dry):
    """config/showdown/xaero 등 overrides 를 인스턴스에 복사."""
    for n in names:
        src = os.path.join(bundle_overrides, n)
        if not os.path.isdir(src):
            log(f"[overrides] WARN: {n} 없음(건너뜀)")
            continue
        log(f"[overrides] {n}")
        if dry:
            continue
        shutil.copytree(src, os.path.join(target_dir, n), dirs_exist_ok=True)


def register_server(instance_dir, name, ip, log, dry):
    """servers.dat 에 서버 자동등록."""
    if not ip or ip.startswith("TODO"):
        log(f"[server] 주소 미설정({ip}) — 등록 생략")
        return
    log(f"[server] 등록: {name} ({ip})")
    if dry:
        return
    os.makedirs(instance_dir, exist_ok=True)
    nbt.register_server(os.path.join(instance_dir, "servers.dat"), name, ip)


def add_launcher_profile(mc_dir, profile_name, version_id, game_dir, log, dry, icon="Furnace"):
    """공식 런처 launcher_profiles.json 에 Zenon Mon 프로필 추가(gameDir=독립 인스턴스)."""
    path = plat.launcher_profiles_path(mc_dir)
    log(f"[profile] {profile_name} → {version_id} (gameDir={game_dir})")
    if dry:
        return
    data = {"profiles": {}}
    if os.path.isfile(path):
        try:
            with open(path, "r", encoding="utf-8") as f:
                data = json.load(f)
        except Exception:
            pass
    profiles = data.setdefault("profiles", {})
    now = time.strftime("%Y-%m-%dT%H:%M:%S.000Z", time.gmtime())
    key = profile_name.lower().replace(" ", "_")
    existing = profiles.get(key, {})
    profiles[key] = {
        "name": profile_name,
        "type": "custom",
        "lastVersionId": version_id,
        "gameDir": game_dir,
        "icon": existing.get("icon", icon),
        "created": existing.get("created", now),
        "lastUsed": now,
    }
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
