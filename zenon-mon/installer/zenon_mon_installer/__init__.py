"""Zenon Mon 범용 모드 설치기 엔진 (결정 046).

서버 무관 — pack.json + 번들만 교체해 재사용. 무의존(표준 라이브러리만).
"""
from .pack import Pack
from .installer import Installer

__all__ = ["Pack", "Installer"]
__version__ = "0.1.0"
