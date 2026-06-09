"""
봇 설정 — .env 파일에서 로드한다.
실제 배포 전 .env.example을 복사해 .env 를 생성하고 값을 채울 것.
"""
import os
from dotenv import load_dotenv

load_dotenv()

DISCORD_TOKEN: str = os.environ["DISCORD_TOKEN"]
PORO_API_URL: str = os.getenv("PORO_API_URL", "http://localhost:8765")
PORO_API_KEY: str = os.environ["PORO_API_KEY"]

# ─── 포로몬 인증 API (RPG 와 분리된 별도 연동) ──────────────────────
# 봇·MC 동일 호스트 전제 → 루프백(127.0.0.1) 바인드. 키는 secret 으로만 로드.
# POROMON_AUTH_KEY 미설정(빈 값)이면 /인증코드 는 운영자 오류로 응답한다.
POROMON_AUTH_URL: str = os.getenv("POROMON_AUTH_URL", "http://127.0.0.1:25580")
POROMON_AUTH_KEY: str = os.getenv("POROMON_AUTH_KEY", "")

GUILD_ID: int = int(os.environ["GUILD_ID"])

# ─── 봇 SQLite (T12, 인스턴스 로컬 — gitignored) ─────────────────────
BOT_DB_PATH: str = os.getenv("BOT_DB_PATH", "poro_bot.sqlite3")

# [DEPRECATED] CHANNEL_AUTH_ID — 구 RPG 단일서버 약관 채널(modules/rpg/auth.py 폐기, DL-138).
# 신 온보딩은 active 서버의 온보딩 카테고리 채널을 사용(panels.py). optional 로 완화.
CHANNEL_AUTH_ID: int = int(os.getenv("CHANNEL_AUTH_ID", "0") or "0")
CHANNEL_FIELD_BOSS_ID: int = int(os.environ["CHANNEL_FIELD_BOSS_ID"])
# 운영/감사 로그 채널 (mod_log 게시 대상). 미설정(0)이면 DB 적재만, 게시 생략.
CHANNEL_MODLOG_ID: int = int(os.getenv("CHANNEL_MODLOG_ID", "0") or "0")

# ─── 전역 단일 active 서버 모델 (2026-06-09, task.md §5) ────────────────
# 시즌 사이 공백 역할 + 통합(공지·채팅) 카테고리. 운영자 사전 구성.
# 미설정(0)이면 생애주기 일괄 역할 전이가 해당 부분을 graceful skip 한다.
ROLE_서버준비_ID: int = int(os.getenv("ROLE_서버준비_ID", "0") or "0")
CATEGORY_통합_ID: int = int(os.getenv("CATEGORY_통합_ID", "0") or "0")

# ─── 커뮤니티 레벨 (T13, community_level.md §6) ─────────────────────────
# 채팅·음성 활동 XP 튜닝. 레벨업 알림 채널 + XP 제외 채널/AFK.
CHAT_XP_PER_MSG: int      = int(os.getenv("CHAT_XP_PER_MSG", "15") or "15")
CHAT_XP_COOLDOWN_SEC: int = int(os.getenv("CHAT_XP_COOLDOWN_SEC", "60") or "60")
VOICE_XP_PER_TICK: int    = int(os.getenv("VOICE_XP_PER_TICK", "5") or "5")
VOICE_TICK_SEC: int       = int(os.getenv("VOICE_TICK_SEC", "60") or "60")
CHANNEL_LEVELUP_ID: int   = int(os.getenv("CHANNEL_LEVELUP_ID", "0") or "0")
AFK_CHANNEL_ID: int       = int(os.getenv("AFK_CHANNEL_ID", "0") or "0")
# XP 제외 채널(명령/음성-텍스트 등) — 쉼표 구분 ID 목록.
XP_EXCLUDE_CHANNEL_IDS: set[int] = {
    int(x) for x in os.getenv("XP_EXCLUDE_CHANNEL_IDS", "").replace(" ", "").split(",") if x
}
# [DEPRECATED] TERMS_MESSAGE_ID — 구 RPG 약관 메시지 ID(폐기). 미참조.
TERMS_MESSAGE_ID: int | None = int(os.environ["TERMS_MESSAGE_ID"]) if os.getenv("TERMS_MESSAGE_ID") else None

# ─── [DEPRECATED] 구 RPG 단일서버 온보딩 역할 (미인증→접속대기→인증유저) ──────
# modules/rpg/auth.py·role_poll.py 폐기(DL-138). 신 온보딩 3역할은 서버별 DB 생성
# (servers.access/pending/player_role_id, panels.py). 미참조 — optional 로 완화.
ROLE_미인증_ID: int  = int(os.getenv("ROLE_미인증_ID",  "0") or "0")
ROLE_접속대기_ID: int = int(os.getenv("ROLE_접속대기_ID", "0") or "0")
ROLE_인증유저_ID: int = int(os.getenv("ROLE_인증유저_ID", "0") or "0")

# ─── 운영 권한 역할 (수동 지급 전용 — 봇 자동 지급 금지) ─────────────
# Owner/Admin/매니저/Support 는 절대 버튼·이모지·자동 로직으로 지급하지 않는다.
# 운영진이 디스코드에서 직접 부여한다. (core/permissions.py 정책 참조)
PERMISSION_ROLE_IDS: dict[str, int] = {
    "owner":           int(os.getenv("ROLE_OWNER_ID",           "0") or "0"),
    "admin":           int(os.getenv("ROLE_ADMIN_ID",           "0") or "0"),
    "rpg_manager":     int(os.getenv("ROLE_RPG_MANAGER_ID",     "0") or "0"),
    "poromon_manager": int(os.getenv("ROLE_POROMON_MANAGER_ID", "0") or "0"),
    "event_manager":   int(os.getenv("ROLE_EVENT_MANAGER_ID",   "0") or "0"),
    "support":         int(os.getenv("ROLE_SUPPORT_ID",         "0") or "0"),
}

# ─── 클래스 역할 (미설정 시 0 — 해당 버튼 숨김) ─────────────────
CLASS_ROLE_IDS: dict[str, int] = {
    "sword":    int(os.getenv("ROLE_검사_ID",       "0") or "0"),
    "axe":      int(os.getenv("ROLE_도끼전사_ID",   "0") or "0"),
    "spear":    int(os.getenv("ROLE_창기사_ID",     "0") or "0"),
    "crossbow": int(os.getenv("ROLE_석궁사_ID",     "0") or "0"),
    "scythe":   int(os.getenv("ROLE_낫사냥꾼_ID",   "0") or "0"),
    "staff":    int(os.getenv("ROLE_지팡이술사_ID", "0") or "0"),
}

# ─── 알림 구독 역할 (자동 지급 가능 — 미설정 시 0) ──────────────────
# 알림 역할은 플레이어가 /알림설정 등으로 자유롭게 토글한다(자동 지급 허용).
# 권한 역할과 분리: 알림 역할에는 어떤 운영 권한도 부여하지 않는다.
NOTIFY_ROLE_IDS: dict[str, int] = {
    "필드보스알림": int(os.getenv("ROLE_필드보스알림_ID", "0") or "0"),
    "시즌보스알림": int(os.getenv("ROLE_시즌보스알림_ID", "0") or "0"),
    "월드보스알림": int(os.getenv("ROLE_월드보스알림_ID", "0") or "0"),
    "포로몬알림":   int(os.getenv("ROLE_포로몬알림_ID",   "0") or "0"),
    "이벤트알림":   int(os.getenv("ROLE_이벤트알림_ID",   "0") or "0"),
    "점검알림":     int(os.getenv("ROLE_점검알림_ID",     "0") or "0"),
    "업데이트알림": int(os.getenv("ROLE_업데이트알림_ID", "0") or "0"),
}


def _role(name: str) -> int:
    return int(os.getenv(name, "0") or "0")


# ─── [DEPRECATED] 멀티서버 온보딩 레지스트리 (env 코드 기반) ─────────────
# ⚠ 2026-06-09 SUPERSEDE: 온보딩(panels.py)은 이제 DB 레지스트리(servers.get_any_active
#   의 3역할 + server_categories 온보딩 카테고리)에서 역할/채널을 읽는다. 전역 단일 active
#   모델(task.md §5)이라 서버 선택·env 매핑 불필요. 이 dict 는 더 이상 코드에서 참조하지
#   않는다(레거시 .env 키 보존용으로만 잔존 — 제거 가능). verify 라우팅 = panels._verifiers.
ONBOARDING_SERVERS: dict[str, dict[str, int | str]] = {
    "poromon": {
        "display_name": "포로몬",
        "access_role":  _role("ROLE_포로몬접근_ID"),
        "pending_role": _role("ROLE_포로몬인증전_ID"),
        "player_role":  _role("ROLE_포로몬플레이어_ID"),
        "terms_channel": _role("CHANNEL_포로몬약관_ID"),
        "auth_channel":  _role("CHANNEL_포로몬인증_ID"),
    },
    # "rpg": { ... }  # RPG 는 코드방향 이관(auth.py) 후 등록 — 현재 기존 흐름 유지
}
