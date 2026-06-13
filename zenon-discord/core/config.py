"""
봇 설정 — .env 파일에서 로드한다.
실제 배포 전 .env.example을 복사해 .env 를 생성하고 값을 채울 것.
"""
import os
from dotenv import load_dotenv

load_dotenv()

DISCORD_TOKEN: str = os.environ["DISCORD_TOKEN"]

# ─── ZenonRPG 게임서버 HTTP API 연동 ────────────────────────────────
# 전환기 호환: 새 이름(ZENON_RPG_API_*)을 우선 읽고, 없으면 구 이름(PORONG_API_*)을
# 폴백으로 읽는다. 구 이름은 차후 제거 예정.
ZENON_RPG_API_URL: str = (
    os.getenv("ZENON_RPG_API_URL")
    or os.getenv("PORONG_API_URL")
    or "http://localhost:8765"
)
_zenon_rpg_api_key = os.getenv("ZENON_RPG_API_KEY") or os.getenv("PORONG_API_KEY")
if not _zenon_rpg_api_key:
    raise RuntimeError(
        "ZENON_RPG_API_KEY 가 .env 에 설정되지 않았습니다 "
        "(구 PORONG_API_KEY 폴백도 비어 있음)."
    )
ZENON_RPG_API_KEY: str = _zenon_rpg_api_key

# ─── Zenon Mon 인증 API (RPG 와 분리된 별도 연동) ──────────────────────
# 봇·MC 동일 호스트 전제 → 루프백(127.0.0.1) 바인드. 키는 secret 으로만 로드.
# 전환기 호환: 새 이름(ZENON_MON_AUTH_*)을 우선 읽고, 없으면 구 이름(POROMON_AUTH_*)을
# 폴백으로 읽는다. 구 이름은 차후 제거 예정.
# ZENON_MON_AUTH_KEY 미설정(빈 값)이면 /인증코드 는 운영자 오류로 응답한다.
ZENON_MON_AUTH_URL: str = (
    os.getenv("ZENON_MON_AUTH_URL")
    or os.getenv("POROMON_AUTH_URL")
    or "http://127.0.0.1:25580"
)
ZENON_MON_AUTH_KEY: str = os.getenv("ZENON_MON_AUTH_KEY") or os.getenv("POROMON_AUTH_KEY") or ""

GUILD_ID: int = int(os.environ["GUILD_ID"])

# ─── 봇 SQLite (T12, 인스턴스 로컬 — gitignored) ─────────────────────
BOT_DB_PATH: str = os.getenv("BOT_DB_PATH", "yuki_bot.sqlite3")

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

# 티켓(1:1 문의) 생성 카테고리 (T16). 미설정(0)이면 카테고리 없이 생성.
CATEGORY_티켓_ID: int = int(os.getenv("CATEGORY_티켓_ID", "0") or "0")

# 버그제보 게시 채널 (T16). 미설정(0)이면 /버그제보 비활성(안내). 봇 DB 미저장 — 채널 임베드만.
CHANNEL_BUGREPORT_ID: int = int(os.getenv("CHANNEL_BUGREPORT_ID", "0") or "0")

# ─── 인바운드 알림 수신 (T1, notifications.md / DL-133) ─────────────────
# 게임서버 → 봇 push 리스너. 보안: SECRET·PORT 둘 다 설정될 때만 기동(미설정 시 비활성 —
# 무인증 엔드포인트를 절대 열지 않는다). HMAC-SHA256(body) + X-Timestamp 신선도 + IP 허용.
INBOUND_SECRET: str = os.getenv("INBOUND_SECRET", "")          # 빈 값 → 리스너 비활성
INBOUND_HOST: str   = os.getenv("INBOUND_HOST", "127.0.0.1")   # 기본 루프백(같은 호스트)
INBOUND_PORT: int   = int(os.getenv("INBOUND_PORT", "0") or "0")  # 0 → 비활성
INBOUND_TS_TOLERANCE: int = int(os.getenv("INBOUND_TS_TOLERANCE", "300") or "300")
# 허용 출발 IP(쉼표 구분). 비우면 IP 필터 생략(방화벽에 위임).
INBOUND_ALLOW_IPS: set[str] = {
    ip for ip in os.getenv("INBOUND_ALLOW_IPS", "").replace(" ", "").split(",") if ip
}

# 알림 라우팅 공지 채널 (notifications.md ③). 미설정(0)이면 해당 알림 graceful skip.
CHANNEL_NOTICE_ID: int         = int(os.getenv("CHANNEL_NOTICE_ID", "0") or "0")
CHANNEL_ZENON_MON_NOTICE_ID: int = int(
    os.getenv("CHANNEL_ZENON_MON_NOTICE_ID") or os.getenv("CHANNEL_POROMON_NOTICE_ID") or "0"
)

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

# 출석/일일보상 (T14). 보상 XP = BASE + min(streak, CAP) * PER_STREAK.
ATTENDANCE_XP_BASE: int       = int(os.getenv("ATTENDANCE_XP_BASE", "100") or "100")
ATTENDANCE_XP_PER_STREAK: int = int(os.getenv("ATTENDANCE_XP_PER_STREAK", "10") or "10")
ATTENDANCE_STREAK_CAP: int    = int(os.getenv("ATTENDANCE_STREAK_CAP", "30") or "30")
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
    "poromon_manager": int(os.getenv("ROLE_ZENON_MON_MANAGER_ID") or os.getenv("ROLE_POROMON_MANAGER_ID") or "0"),
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
        "display_name": "Zenon Mon",
        "access_role":  _role("ROLE_포로몬접근_ID"),
        "pending_role": _role("ROLE_포로몬인증전_ID"),
        "player_role":  _role("ROLE_포로몬플레이어_ID"),
        "terms_channel": _role("CHANNEL_포로몬약관_ID"),
        "auth_channel":  _role("CHANNEL_포로몬인증_ID"),
    },
    # "rpg": { ... }  # RPG 는 코드방향 이관(auth.py) 후 등록 — 현재 기존 흐름 유지
}
