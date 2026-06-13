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
GUILD_ID: int = int(os.environ["GUILD_ID"])
CHANNEL_AUTH_ID: int = int(os.environ["CHANNEL_AUTH_ID"])
CHANNEL_FIELD_BOSS_ID: int = int(os.environ["CHANNEL_FIELD_BOSS_ID"])
TERMS_MESSAGE_ID: int | None = int(os.environ["TERMS_MESSAGE_ID"]) if os.getenv("TERMS_MESSAGE_ID") else None

# ─── 온보딩 권한 역할 (시스템 자동 — 인증 단계 승급) ────────────────
# 인증 플로우(약관 동의 → 코드 인증)에 따라 봇이 자동 승급한다.
ROLE_미인증_ID: int  = int(os.getenv("ROLE_미인증_ID",  "0") or "0")
ROLE_접속대기_ID: int = int(os.environ["ROLE_접속대기_ID"])
ROLE_인증유저_ID: int = int(os.environ["ROLE_인증유저_ID"])

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
