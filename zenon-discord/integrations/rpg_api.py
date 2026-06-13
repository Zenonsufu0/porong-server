"""
ZenonRPG HTTP API 클라이언트 (aiohttp 기반).

모든 요청에 X-Api-Key 헤더를 포함한다.
"""
import aiohttp
from core.config import ZENON_RPG_API_URL, ZENON_RPG_API_KEY
from integrations.common import VerifyError


class RpgAuthError(VerifyError):
    """RPG 인증 API 호출 중 운영자 확인이 필요한 오류(VerifyError 하위)."""


class ZenonRpgApiClient:
    def __init__(self, session: aiohttp.ClientSession | None = None):
        self._session = session
        self._headers = {"X-Api-Key": ZENON_RPG_API_KEY, "Content-Type": "application/json"}

    def _get_session(self) -> aiohttp.ClientSession:
        if self._session is None or self._session.closed:
            self._session = aiohttp.ClientSession()
        return self._session

    async def close(self) -> None:
        if self._session and not self._session.closed:
            await self._session.close()

    # ─── 인증 ────────────────────────────────────────────────────

    async def verify_code(self, code: str, discord_id: int | str) -> dict:
        """POST /auth/verify — 인게임 `/인증` 발급 코드 검증 (DL-138 코드방향 통일).

        계약 = 포로몬 verify_code 와 동일(레퍼런스). 헤더 `X-Api-Key`,
        바디 `{"code", "discordId"}`.

        반환:
          {"ok": True,  "uuid": <MC UUID|None>, "name": <MC 닉|None>}  — 200 성공
          {"ok": False, "reason": "not_found"}     — 404 코드 만료/없음
          {"ok": False, "reason": "rate_limited"}  — 429 시도 과다
        예외:
          RpgAuthError — 401(키 불일치)·네트워크 실패·예상치 못한 응답.

        ⚠ 필드명(`discordId` camelCase)은 DL-138 레퍼런스 기준. RPG AuthApiHandler
          실제 계약과 다르면(예: snake_case) 맞춰야 함 — RPG worktree 확인 필요.
        """
        url = f"{ZENON_RPG_API_URL}/auth/verify"
        payload = {"code": code, "discordId": str(discord_id)}
        try:
            async with self._get_session().post(url, json=payload, headers=self._headers) as resp:
                if resp.status == 200:
                    data = await resp.json()
                    return {"ok": True, "uuid": data.get("uuid"), "name": data.get("name")}
                if resp.status == 404:
                    return {"ok": False, "reason": "not_found"}
                if resp.status == 429:
                    return {"ok": False, "reason": "rate_limited"}
                if resp.status == 401:
                    raise RpgAuthError("RPG 인증 API 키 불일치(401)")
                text = await resp.text()
                raise RpgAuthError(f"예상치 못한 응답 {resp.status}: {text[:200]}")
        except aiohttp.ClientError as e:
            raise RpgAuthError(f"RPG 인증 API 네트워크 오류: {e}") from e

    # 구방향(create_pending·get_auth_status·poll_role_queue·acknowledge_role_granted)은
    # DL-138 코드방향 통일로 폐기(2026-06-09). 신방향 = verify_code(인게임 발급→봇 검증).

    # ─── 필드보스 ────────────────────────────────────────────────

    async def get_field_boss_status(self) -> list:
        """GET /field-boss/status → [{field_id, status, respawn_minutes, player_count}, ...]"""
        url = f"{ZENON_RPG_API_URL}/field-boss/status"
        async with self._get_session().get(url, headers=self._headers) as resp:
            resp.raise_for_status()
            return await resp.json()

    # ─── 플레이어 ────────────────────────────────────────────────

    async def get_player_by_nick(self, nick: str) -> dict:
        """GET /player/by-nick/{nick} → DiscordCardResponse"""
        url = f"{ZENON_RPG_API_URL}/player/by-nick/{nick}"
        async with self._get_session().get(url, headers=self._headers) as resp:
            resp.raise_for_status()
            return await resp.json()

    async def get_island_by_nick(self, nick: str) -> dict:
        """GET /island/by-nick/{nick} → DiscordCardResponse"""
        url = f"{ZENON_RPG_API_URL}/island/by-nick/{nick}"
        async with self._get_session().get(url, headers=self._headers) as resp:
            resp.raise_for_status()
            return await resp.json()

    async def get_boss_by_nick(self, nick: str) -> dict:
        """GET /boss-history/by-nick/{nick} → DiscordCardResponse"""
        url = f"{ZENON_RPG_API_URL}/boss-history/by-nick/{nick}"
        async with self._get_session().get(url, headers=self._headers) as resp:
            resp.raise_for_status()
            return await resp.json()
