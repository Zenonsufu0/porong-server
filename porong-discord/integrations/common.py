"""
integrations 공통 — 도메인 비종속 유틸/예외.

도메인 격리(루트 CLAUDE.md): RPG·포로몬 코드는 서로 직접 import 하지 않는다.
이 모듈은 중립(도메인 비종속)이므로 양쪽이 공통으로 import 해도 격리를 깨지 않는다.
"""
from __future__ import annotations


class VerifyError(Exception):
    """인증 verify 중 **운영자 확인이 필요한** 오류(키 불일치·네트워크 실패·예상외 응답).

    사용자 잘못이 아님 → 호출부(온보딩)는 이 예외를 잡아 운영 로그를 남기고
    사용자에겐 일반 오류로 안내한다. 코드 만료/없음(404)·시도 과다(429)는
    예외가 아니라 verify 반환값(`{"ok": False, "reason": ...}`)으로 구분한다.

    도메인별 클라이언트는 이 예외(또는 하위 클래스)를 던진다 → 온보딩 라우터는
    도메인과 무관하게 `except VerifyError` 한 번으로 처리한다.
    """
