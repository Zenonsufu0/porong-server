"""
인바운드 알림 수신 리스너 (T1) — notifications.md ①, integration_contract B-0, DL-133.

게임서버가 `POST {host:port}/events` 로 이벤트 봉투를 push → 검증 통과 시 notifier 로 전달.
discord 루프와 같은 프로세스에서 aiohttp `AppRunner`+`TCPSite` 로 기동(web.run_app 금지).

검증 순서(외부 유입 → 강하게):
  1. IP 허용(INBOUND_ALLOW_IPS, 설정 시) — 방화벽과 이중.
  2. X-Timestamp 신선도(±INBOUND_TS_TOLERANCE) — 리플레이 방지.
  3. X-Signature = hex(HMAC-SHA256(raw_body, INBOUND_SECRET)) — 본문 위변조 방지.
  4. idempotency_key dedup(메모리 LRU, 재시작 시 유실 허용).
봇 다운 중 push 는 유실 → 게임서버가 재시도·실패허용 책임(계약). 미등록 kind = graceful 200.
"""
from __future__ import annotations

import hashlib
import hmac
import json
import logging
import time
from collections import OrderedDict

from aiohttp import web

from core import config, notifier

log = logging.getLogger(__name__)

_SEEN_MAX = 1024  # idempotency LRU 용량


def is_enabled() -> bool:
    """SECRET·PORT 둘 다 설정될 때만 리스너 기동(무인증 엔드포인트 방지)."""
    return bool(config.INBOUND_SECRET) and config.INBOUND_PORT > 0


class InboundServer:
    def __init__(self, bot) -> None:
        self.bot = bot
        self._secret = config.INBOUND_SECRET.encode()
        self._seen: OrderedDict[str, None] = OrderedDict()
        self._runner: web.AppRunner | None = None

    # ─── dedup ────────────────────────────────────────────────────
    def _is_duplicate(self, key: str) -> bool:
        if key in self._seen:
            return True
        self._seen[key] = None
        if len(self._seen) > _SEEN_MAX:
            self._seen.popitem(last=False)
        return False

    # ─── 핸들러 ───────────────────────────────────────────────────
    async def _handle_events(self, request: web.Request) -> web.Response:
        # 1) IP 허용
        if config.INBOUND_ALLOW_IPS and request.remote not in config.INBOUND_ALLOW_IPS:
            log.warning("인바운드 거부 — 비허용 IP %s", request.remote)
            return web.json_response({"ok": False, "error": "forbidden"}, status=403)

        raw = await request.read()

        # 2) timestamp 신선도
        try:
            ts = int(request.headers.get("X-Timestamp", ""))
        except ValueError:
            return web.json_response({"ok": False, "error": "bad_timestamp"}, status=401)
        if abs(int(time.time()) - ts) > config.INBOUND_TS_TOLERANCE:
            return web.json_response({"ok": False, "error": "stale"}, status=401)

        # 3) HMAC 서명
        expected = hmac.new(self._secret, raw, hashlib.sha256).hexdigest()
        provided = request.headers.get("X-Signature", "")
        if not hmac.compare_digest(expected, provided):
            log.warning("인바운드 거부 — 서명 불일치 (IP %s)", request.remote)
            return web.json_response({"ok": False, "error": "bad_signature"}, status=401)

        # 4) 봉투 파싱·검증
        try:
            env = json.loads(raw)
        except json.JSONDecodeError:
            return web.json_response({"ok": False, "error": "bad_json"}, status=400)
        if not isinstance(env, dict):
            return web.json_response({"ok": False, "error": "bad_envelope"}, status=400)
        domain, kind = env.get("domain"), env.get("kind")
        if not isinstance(domain, str) or not isinstance(kind, str):
            return web.json_response({"ok": False, "error": "missing_domain_kind"}, status=400)

        # 5) idempotency dedup
        idem = env.get("idempotency_key")
        if isinstance(idem, str) and idem and self._is_duplicate(idem):
            return web.json_response({"ok": True, "dedup": True})

        data = env.get("data")
        data = data if isinstance(data, dict) else {}

        try:
            await notifier.dispatch(self.bot, domain, kind, data)
        except Exception:  # dispatch 내부는 graceful 이지만 최후 방어
            log.exception("알림 dispatch 실패 (%s.%s)", domain, kind)
        return web.json_response({"ok": True})

    # ─── 수명주기 ─────────────────────────────────────────────────
    async def start(self) -> None:
        app = web.Application()
        app.router.add_post("/events", self._handle_events)
        runner = web.AppRunner(app, access_log=None)
        await runner.setup()
        site = web.TCPSite(runner, config.INBOUND_HOST, config.INBOUND_PORT)
        await site.start()
        self._runner = runner
        log.info("인바운드 알림 리스너 시작: %s:%d", config.INBOUND_HOST, config.INBOUND_PORT)

    async def cleanup(self) -> None:
        if self._runner is not None:
            await self._runner.cleanup()
            self._runner = None
