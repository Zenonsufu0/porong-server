"""
포로몬(모드 서버) 연동 클라이언트 — **스텁(인터페이스 전용)**.

RPG 와 포로몬은 코드 공유가 거의 없으므로 봇 내부에서도 통합 경로를 분리한다.
RPG 연동은 integrations/rpg_api.py(PoroApiClient)가 담당하고,
포로몬 연동은 이 모듈이 담당한다.

⚠️ 실제 API 연결은 사용자가 명시적으로 요청할 때만 구현한다.
현재는 엔드포인트 형태만 TODO 로 남긴 인터페이스다.
"""
from __future__ import annotations


class PoromonApiClient:
    """포로몬 서버 조회/제어용 클라이언트 (미구현).

    구현 시 RPG 클라이언트(PoroApiClient)와 동일하게 aiohttp 세션 +
    API 키 헤더 패턴을 따른다. 연결 대상(포트·인증 방식)은 설계 확정 후 결정.
    """

    def __init__(self) -> None:
        # TODO: 포로몬 서버 베이스 URL / 인증 토큰 로드 (core.config)
        ...

    async def close(self) -> None:
        # TODO: aiohttp 세션 정리
        ...

    # ─── 조회 (예시 인터페이스 — 엔드포인트 미확정) ──────────────────

    async def get_server_status(self) -> dict:
        """포로몬 모드 서버 상태(온라인 인원·TPS 등) 조회."""
        raise NotImplementedError("poromon_api: get_server_status 미구현")

    async def get_player_summary(self, nick: str) -> dict:
        """포로몬 플레이어 요약 정보 조회."""
        raise NotImplementedError("poromon_api: get_player_summary 미구현")
