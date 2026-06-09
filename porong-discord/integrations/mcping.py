"""
마인크래프트 서버 상태 핑 (Server List Ping) — T18 접속정보.

RPG(Paper)·포로몬(모드) 모두 마인크래프트 서버 → 표준 SLP 로 on/off·인원·핑을
얻는다(커스텀 게임서버 API 불필요, 도메인 비종속). 라이브러리 = mcstatus.

mcstatus 미설치 시 graceful: ping() 이 None 반환(기능 비활성, 봇 기동엔 영향 없음).
"""
from __future__ import annotations

import logging

try:
    from mcstatus import JavaServer
except ImportError:  # 의존성 미설치 — 기능만 비활성
    JavaServer = None  # type: ignore[assignment]

log = logging.getLogger(__name__)


def available() -> bool:
    return JavaServer is not None


async def ping(address: str) -> dict | None:
    """`address`(host 또는 host:port) 마인크래프트 서버 상태 조회.

    반환:
      {"online": True, "players": n, "max": m, "latency_ms": ms, "version": str} — 응답
      {"online": False}                                                          — 오프라인/응답없음
      None                                                                       — mcstatus 미설치 or 주소 미설정
    """
    if JavaServer is None or not address:
        return None
    try:
        server = await JavaServer.async_lookup(address)
        status = await server.async_status()
        return {
            "online": True,
            "players": status.players.online,
            "max": status.players.max,
            "latency_ms": round(status.latency),
            "version": status.version.name,
        }
    except Exception as e:  # 연결 실패·타임아웃·프로토콜 오류 = 오프라인 취급
        log.debug("MC 핑 실패(%s): %s", address, e)
        return {"online": False}
