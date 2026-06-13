#!/usr/bin/env python3
"""Zenon Mon 설치기 GUI (tkinter — 유저 배포용 exe 엔트리).

필수 코어는 자동(토글 불가), 선택 모드는 그룹별 체크박스(pack.json default 적용).
[설치] → 백그라운드 스레드로 Installer 실행, 로그 실시간 표시.

번들은 exe 안에 동봉(PyInstaller --add-data). 개발 시엔 resolve_bundle() 기본 경로.
"""
import os
import sys
import threading

import tkinter as tk
from tkinter import ttk, filedialog, messagebox

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from zenon_mon_installer import Pack, Installer            # noqa: E402
from zenon_mon_installer import platform as plat           # noqa: E402

GROUP_LABELS = {
    "performance": "성능",
    "models": "모델/텍스처",
    "viewer": "정보/뷰어",
    "qol": "조작/편의",
    "ui": "UI/메뉴",
    "shader": "셰이더",
    "visual": "비주얼/파티클",
    "sound": "사운드",
    "misc": "기타",
}


class InstallerApp:
    def __init__(self, bundle_dir):
        self.bundle = bundle_dir
        self.pack = Pack.from_bundle(bundle_dir)
        self.vars = {}  # file -> BooleanVar
        self.installing = False

        self.root = tk.Tk()
        self.root.title(f"{self.pack.name} 설치기")
        self.root.geometry("560x680")
        self.target_var = tk.StringVar(value=plat.default_instance_dir(self.pack.instance_name))
        self._build()

    # ── UI 구성 ────────────────────────────────────────────────────────────
    def _build(self):
        p = self.pack
        top = ttk.Frame(self.root, padding=12)
        top.pack(fill="x")
        ttk.Label(top, text=f"{p.name} {p.version}", font=("", 15, "bold")).pack(anchor="w")
        ttk.Label(top, text=f"MC {p.mc_version} · Fabric {p.loader_version} · Java {p.minecraft.get('java','?')}",
                  foreground="#666").pack(anchor="w")
        ttk.Label(top, text=f"■ 필수 코어 {len(p.required)}개 + 라이브러리 {len(p.libraries)}개 — 자동 설치(선택 불가)",
                  foreground="#1a7").pack(anchor="w", pady=(6, 0))

        ttk.Label(self.root, text="선택 모드 (체크해서 설치)", padding=(12, 4)).pack(anchor="w")

        # 스크롤 가능한 체크박스 영역
        mid = ttk.Frame(self.root)
        mid.pack(fill="both", expand=True, padx=12)
        canvas = tk.Canvas(mid, highlightthickness=0)
        sb = ttk.Scrollbar(mid, orient="vertical", command=canvas.yview)
        inner = ttk.Frame(canvas)
        inner.bind("<Configure>", lambda e: canvas.configure(scrollregion=canvas.bbox("all")))
        canvas.create_window((0, 0), window=inner, anchor="nw")
        canvas.configure(yscrollcommand=sb.set)
        canvas.pack(side="left", fill="both", expand=True)
        sb.pack(side="right", fill="y")
        canvas.bind_all("<MouseWheel>", lambda e: canvas.yview_scroll(int(-e.delta / 120), "units"))

        last_group = None
        for m in p.optional:
            if m.group != last_group:
                ttk.Label(inner, text=GROUP_LABELS.get(m.group, m.group or "기타"),
                          font=("", 10, "bold"), foreground="#357").pack(anchor="w", pady=(8, 2))
                last_group = m.group
            var = tk.BooleanVar(value=m.default)
            self.vars[m.file] = var
            text = m.name + (f"  — {m.desc}" if m.desc else "")
            ttk.Checkbutton(inner, text=text, variable=var).pack(anchor="w", padx=12)

        # 설치 경로
        bottom = ttk.Frame(self.root, padding=12)
        bottom.pack(fill="x")
        ttk.Label(bottom, text="설치 위치").pack(anchor="w")
        row = ttk.Frame(bottom)
        row.pack(fill="x")
        ttk.Entry(row, textvariable=self.target_var).pack(side="left", fill="x", expand=True)
        ttk.Button(row, text="찾기", command=self._browse).pack(side="left", padx=(6, 0))

        self.progress = ttk.Progressbar(bottom, mode="indeterminate")
        self.progress.pack(fill="x", pady=(8, 4))
        self.install_btn = ttk.Button(bottom, text="설치", command=self._on_install)
        self.install_btn.pack(fill="x")

        self.logbox = tk.Text(self.root, height=8, state="disabled", bg="#111", fg="#ddd", font=("Consolas", 9))
        self.logbox.pack(fill="both", expand=False, padx=12, pady=(0, 12))

    def _browse(self):
        d = filedialog.askdirectory(initialdir=os.path.dirname(self.target_var.get()) or "/")
        if d:
            self.target_var.set(os.path.join(d, self.pack.instance_name))

    def selected(self):
        return {f for f, v in self.vars.items() if v.get()}

    # ── 로그(스레드 안전) ──────────────────────────────────────────────────
    def log(self, msg):
        self.root.after(0, self._append, msg)

    def _append(self, msg):
        self.logbox.configure(state="normal")
        self.logbox.insert("end", msg + "\n")
        self.logbox.see("end")
        self.logbox.configure(state="disabled")

    # ── 설치 ───────────────────────────────────────────────────────────────
    def _on_install(self):
        if self.installing:
            return
        self.installing = True
        self.install_btn.configure(state="disabled")
        self.progress.start(12)
        threading.Thread(target=self._run, daemon=True).start()

    def _run(self):
        try:
            inst = Installer(self.bundle, target_dir=self.target_var.get(), logger=self.log)
            inst.install(self.selected(), dry=False)
            self.root.after(0, lambda: messagebox.showinfo("완료", "설치가 끝났습니다.\n공식 런처에서 Zenon Mon 프로필을 선택하세요."))
        except Exception as e:  # noqa: BLE001
            self.log(f"[오류] {e}")
            self.root.after(0, lambda: messagebox.showerror("오류", str(e)))
        finally:
            self.root.after(0, self._done)

    def _done(self):
        self.progress.stop()
        self.install_btn.configure(state="normal")
        self.installing = False

    def run(self):
        self.root.mainloop()


def main():
    bundle = plat.resolve_bundle(sys.argv[1] if len(sys.argv) > 1 else None)
    if not os.path.isfile(os.path.join(bundle, "pack.json")):
        msg = f"번들(pack.json)을 찾을 수 없습니다: {bundle}"
        try:
            r = tk.Tk(); r.withdraw(); messagebox.showerror("Zenon Mon 설치기", msg); r.destroy()
        except Exception:
            print(msg, file=sys.stderr)
        return 1
    InstallerApp(bundle).run()
    return 0


if __name__ == "__main__":
    sys.exit(main())
