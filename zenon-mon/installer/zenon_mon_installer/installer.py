"""설치 오케스트레이션 — 번들 + 선택 → 인스턴스 설치 (installer_design.md §4)."""
import os

from . import platform as plat
from . import steps
from .pack import Pack


class Installer:
    def __init__(self, bundle_dir, target_dir=None, mc_dir=None, logger=None):
        self.bundle = bundle_dir
        self.pack = Pack.from_bundle(bundle_dir)
        self.mc_dir = mc_dir or plat.minecraft_dir()
        self.target = target_dir or plat.default_instance_dir(self.pack.instance_name)
        self.log = logger or (lambda m: print(m))

    @property
    def bundle_mods(self):
        return os.path.join(self.bundle, "mods")

    @property
    def bundle_overrides(self):
        return os.path.join(self.bundle, "overrides")

    @property
    def fabric_installer(self):
        return os.path.join(self.bundle, "tools", "fabric-installer.jar")

    def resolve_selection(self, enable=None, disable=None):
        """기본 선택(default:true) ± enable/disable 파일 → optional 선택 집합."""
        sel = set(self.pack.default_selection())
        for f in enable or []:
            sel.add(f)
        for f in disable or []:
            sel.discard(f)
        return sel

    def install(self, selected, dry=False):
        p = self.pack
        files = p.client_files(selected)
        self.log(f"=== {p.name} {p.version} 설치 ({'계획(dry-run)' if dry else '실행'}) ===")
        self.log(f"MC {p.mc_version} / Fabric {p.loader_version} / 인스턴스 {self.target}")
        self.log(f"모드 {len(files)}개 (필수 {len(p.required)} + 라이브러리 {len(p.libraries)} "
                 f"+ 선택 {len(files) - len(p.required) - len(p.libraries)}/{len(p.optional)})")

        steps.install_fabric(self.fabric_installer, p.mc_version, p.loader_version, self.mc_dir, self.log, dry)
        steps.place_mods(self.bundle_mods, files, os.path.join(self.target, "mods"), self.log, dry)
        steps.copy_overrides(self.bundle_overrides, p.overrides, self.target, self.log, dry)
        steps.register_server(self.target, p.server_name, p.server_address, self.log, dry)
        vid = plat.fabric_version_id(p.mc_version, p.loader_version)
        steps.add_launcher_profile(self.mc_dir, p.name, vid, self.target, self.log, dry)

        self.log("=== 완료 ===" if not dry else "=== 계획 끝(변경 없음) ===")
        return files
