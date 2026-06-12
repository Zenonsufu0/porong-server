"""OS별 경로 (.minecraft / launcher_profiles / 기본 인스턴스 위치).

설치기는 유저 기존 `.minecraft` 의 versions/launcher_profiles 는 공유하되,
게임 데이터(세이브/mods)는 **독립 인스턴스 폴더**(gameDir)에 두어 격리한다.
"""
import os
import sys


def minecraft_dir():
    """공식 런처 .minecraft 경로(OS별)."""
    if sys.platform.startswith("win"):
        base = os.environ.get("APPDATA") or os.path.expanduser("~\\AppData\\Roaming")
        return os.path.join(base, ".minecraft")
    if sys.platform == "darwin":
        return os.path.expanduser("~/Library/Application Support/minecraft")
    return os.path.expanduser("~/.minecraft")


def launcher_profiles_path(mc_dir=None):
    return os.path.join(mc_dir or minecraft_dir(), "launcher_profiles.json")


def default_instance_dir(instance_name):
    """독립 인스턴스(gameDir) 기본 위치 = .minecraft/instances/<name>.

    mods/saves 가 여기 격리되어 유저의 기존 바닐라 세이브와 섞이지 않는다.
    """
    return os.path.join(minecraft_dir(), "instances", instance_name)


def fabric_version_id(mc_version, loader_version):
    """Fabric 설치 후 versions/ 에 생기는 버전 id 형식."""
    return f"fabric-loader-{loader_version}-{mc_version}"
