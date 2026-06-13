"""pack.json 로드/모델 (결정 046 / installer_design.md §3).

번들의 pack.json 을 읽어 required / optional(토글) / libraries / overrides 로 노출한다.
엔진은 서버 무관(범용) — pack.json 만 바꾸면 다른 서버에 재사용.
"""
import json
import os


class Mod:
    __slots__ = ("file", "name", "group", "default", "desc")

    def __init__(self, file, name, group=None, default=False, desc=""):
        self.file = file
        self.name = name
        self.group = group
        self.default = default
        self.desc = desc

    def __repr__(self):
        return f"Mod({self.file!r}, default={self.default})"


class Pack:
    def __init__(self, data):
        p = data.get("pack", {})
        self.id = p.get("id", "pack")
        self.name = p.get("name", self.id)
        self.version = p.get("version", "0.0.0")
        self.minecraft = data.get("minecraft", {})
        self.install = data.get("install", {})
        self.server = data.get("server", {})
        self.required = [Mod(e["file"], e.get("name", e["file"])) for e in data.get("required", [])]
        self.optional = [
            Mod(e["file"], e.get("name", e["file"]), e.get("group"), bool(e.get("default", False)), e.get("desc", ""))
            for e in data.get("optional", [])
        ]
        self.libraries = [Mod(e["file"], e.get("name", e["file"])) for e in data.get("libraries", [])]
        self.overrides = list(data.get("overrides", []))

    @classmethod
    def load(cls, path):
        with open(path, "r", encoding="utf-8") as f:
            return cls(json.load(f))

    @classmethod
    def from_bundle(cls, bundle_dir):
        return cls.load(os.path.join(bundle_dir, "pack.json"))

    # ── 토글/선택 ──────────────────────────────────────────────────────────
    def groups(self):
        """optional 의 group 을 등장 순서대로(중복 제거)."""
        seen, out = set(), []
        for m in self.optional:
            g = m.group or "기타"
            if g not in seen:
                seen.add(g)
                out.append(g)
        return out

    def default_selection(self):
        """기본 체크(ON) 되는 optional 파일 집합 = default:true (T1)."""
        return {m.file for m in self.optional if m.default}

    def client_files(self, selected):
        """실제 인스턴스 mods 에 들어갈 파일 = required + libraries + 선택된 optional.

        selected: optional 중 켤 파일명 집합.
        """
        files = [m.file for m in self.required]
        files += [m.file for m in self.libraries]
        files += [m.file for m in self.optional if m.file in selected]
        return files

    # ── 메타 ───────────────────────────────────────────────────────────────
    @property
    def mc_version(self):
        return self.minecraft.get("version")

    @property
    def loader_version(self):
        return self.minecraft.get("loaderVersion")

    @property
    def instance_name(self):
        return self.install.get("instanceName", self.name)

    @property
    def server_address(self):
        return self.server.get("address")

    @property
    def server_name(self):
        return self.server.get("name", self.name)
