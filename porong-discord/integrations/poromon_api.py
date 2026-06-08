"""
포로몬(모드 서버) 연동 클라이언트.

RPG 와 포로몬은 코드 공유가 거의 없으므로 봇 내부에서도 통합 경로를 분리한다.
RPG 연동은 integrations/rpg_api.py(PoroApiClient)가 담당하고,
포로몬 연동은 이 모듈이 담당한다.

현재 구현된 것:
  - verify_code: 디스코드 인증 코드 검증 (PoroMonCore /auth/verify 계약).
    봇·MC 동일 호스트 전제 → 베이스 URL 은 127.0.0.1 루프백(POROMON_AUTH_URL),
    API 키는 secret(POROMON_AUTH_KEY) 으로만 로드한다.

스텁(미구현 — 엔드포인트 미확정):
  - get_server_status / get_player_summary
"""
from __future__ import annotations

import logging

import aiohttp

from core import config

log = logging.getLogger(__name__)


class PoromonAuthError(Exception):
    """포로몬 인증 API 호출 중 운영자 확인이 필요한 오류.

    키 불일치(401)·네트워크 실패·예상치 못한 응답 등 *사용자 잘못이 아닌*
    오류에 사용한다. 호출부는 이 예외를 잡아 운영자 로그 + 사용자에겐
    일반 오류 안내로 처리한다. (코드 만료/없음(404)은 예외가 아니라
    verify_code 의 정상 반환값으로 구분한다.)
    """


class PoromonApiClient:
    """포로몬 서버 조회/인증용 클라이언트.

    RPG 클라이언트(PoroApiClient)와 동일하게 aiohttp 세션을 지연 생성하고
    API 키 헤더를 붙인다.
    """

    def __init__(self, session: aiohttp.ClientSession | None = None) -> None:
        self._session = session

    def _get_session(self) -> aiohttp.ClientSession:
        if self._session is None or self._session.closed:
            self._session = aiohttp.ClientSession()
        return self._session

    async def close(self) -> None:
        if self._session and not self._session.closed:
            await self._session.close()

    # ─── 인증 ────────────────────────────────────────────────────

    async def verify_code(self, code: str, discord_id: int | str) -> dict:
        """POST {base}/auth/verify — 디스코드 인증 코드 검증.

        헤더 `X-API-Key`, 바디 `{"code", "discordId"}` (PoroMonCore 계약).

        반환:
          {"ok": True,  "uuid": <MC UUID 또는 None>}  — 200 인증 성공
          {"ok": False, "reason": "not_found"}        — 404 코드 만료/없음

        예외:
          PoromonAuthError — 401(키 불일치)·네트워크 실패·예상치 못한 응답.
                             (운영자 확인 필요, 사용자에겐 일반 오류로 안내)
        """
        if not config.POROMON_AUTH_KEY:
            raise PoromonAuthError("POROMON_AUTH_KEY 미설정 — 포로몬 인증 비활성")

        url = f"{config.POROMON_AUTH_URL}/auth/verify"
        headers = {
            "X-API-Key": config.POROMON_AUTH_KEY,
            "Content-Type": "application/json",
        }
        payload = {"code": code, "discordId": str(discord_id)}

        try:
            async with self._get_session().post(url, json=payload, headers=headers) as resp:
                if resp.status == 200:
                    data = await resp.json()
                    return {"ok": True, "uuid": data.get("uuid")}
                if resp.status == 404:
                    return {"ok": False, "reason": "not_found"}
                if resp.status == 401:
                    raise PoromonAuthError("포로몬 인증 API 키 불일치(401)")
                text = await resp.text()
                raise PoromonAuthError(f"예상치 못한 응답 {resp.status}: {text[:200]}")
        except aiohttp.ClientError as e:
            raise PoromonAuthError(f"포로몬 인증 API 네트워크 오류: {e}") from e

    # ─── 조회 (스텁 — 엔드포인트 미확정) ──────────────────────────────

    async def get_server_status(self) -> dict:
        """포로몬 모드 서버 상태(온라인 인원·TPS 등) 조회."""
        raise NotImplementedError("poromon_api: get_server_status 미구현")

    async def get_player_summary(self, nick: str) -> dict:
        """포로몬 플레이어 요약 정보 조회."""
        raise NotImplementedError("poromon_api: get_player_summary 미구현")
