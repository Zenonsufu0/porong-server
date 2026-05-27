"""
EmpireRPG HTTP API 클라이언트 (aiohttp 기반).

모든 요청에 X-Api-Key 헤더를 포함한다.
"""
import aiohttp
from config import EMPIRE_API_URL, EMPIRE_API_KEY


class EmpireApiClient:
    def __init__(self, session: aiohttp.ClientSession | None = None):
        self._session = session
        self._headers = {"X-Api-Key": EMPIRE_API_KEY, "Content-Type": "application/json"}

    def _get_session(self) -> aiohttp.ClientSession:
        if self._session is None or self._session.closed:
            self._session = aiohttp.ClientSession()
        return self._session

    async def close(self) -> None:
        if self._session and not self._session.closed:
            await self._session.close()

    # ─── 인증 ────────────────────────────────────────────────────

    async def create_pending(self, discord_user_id: str, minecraft_nick: str) -> dict:
        """POST /auth/pending → {code, expires_in}"""
        url = f"{EMPIRE_API_URL}/auth/pending"
        async with self._get_session().post(
            url,
            json={"discord_user_id": discord_user_id, "minecraft_nick": minecraft_nick},
            headers=self._headers,
        ) as resp:
            resp.raise_for_status()
            return await resp.json()

    async def get_auth_status(self, minecraft_nick: str) -> dict:
        """GET /auth/status/{nick} → {verified, discord_user_id}"""
        url = f"{EMPIRE_API_URL}/auth/status/{minecraft_nick}"
        async with self._get_session().get(url, headers=self._headers) as resp:
            resp.raise_for_status()
            return await resp.json()

    async def poll_role_queue(self) -> list:
        """GET /auth/role-queue → [{discord_user_id, minecraft_nick}, ...]"""
        url = f"{EMPIRE_API_URL}/auth/role-queue"
        async with self._get_session().get(url, headers=self._headers) as resp:
            resp.raise_for_status()
            return await resp.json()

    async def acknowledge_role_granted(self, discord_user_id: str) -> None:
        """POST /auth/role-granted"""
        url = f"{EMPIRE_API_URL}/auth/role-granted"
        async with self._get_session().post(
            url,
            json={"discord_user_id": discord_user_id},
            headers=self._headers,
        ) as resp:
            resp.raise_for_status()

    # ─── 필드보스 ────────────────────────────────────────────────

    async def get_field_boss_status(self) -> list:
        """GET /field-boss/status → [{field_id, status, respawn_minutes, player_count}, ...]"""
        url = f"{EMPIRE_API_URL}/field-boss/status"
        async with self._get_session().get(url, headers=self._headers) as resp:
            resp.raise_for_status()
            return await resp.json()

    # ─── 플레이어 ────────────────────────────────────────────────

    async def get_player_by_nick(self, nick: str) -> dict:
        """GET /player/by-nick/{nick} → DiscordCardResponse"""
        url = f"{EMPIRE_API_URL}/player/by-nick/{nick}"
        async with self._get_session().get(url, headers=self._headers) as resp:
            resp.raise_for_status()
            return await resp.json()

    async def get_island_by_nick(self, nick: str) -> dict:
        """GET /island/by-nick/{nick} → DiscordCardResponse"""
        url = f"{EMPIRE_API_URL}/island/by-nick/{nick}"
        async with self._get_session().get(url, headers=self._headers) as resp:
            resp.raise_for_status()
            return await resp.json()

    async def get_boss_by_nick(self, nick: str) -> dict:
        """GET /boss-history/by-nick/{nick} → DiscordCardResponse"""
        url = f"{EMPIRE_API_URL}/boss-history/by-nick/{nick}"
        async with self._get_session().get(url, headers=self._headers) as resp:
            resp.raise_for_status()
            return await resp.json()
