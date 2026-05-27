"""
봇 설정 — .env 파일에서 로드한다.
실제 배포 전 .env.example을 복사해 .env 를 생성하고 값을 채울 것.
"""
import os
from dotenv import load_dotenv

load_dotenv()

DISCORD_TOKEN: str = os.environ["DISCORD_TOKEN"]
EMPIRE_API_URL: str = os.getenv("EMPIRE_API_URL", "http://localhost:8765")
EMPIRE_API_KEY: str = os.environ["EMPIRE_API_KEY"]
GUILD_ID: int = int(os.environ["GUILD_ID"])
CHANNEL_AUTH_ID: int = int(os.environ["CHANNEL_AUTH_ID"])
CHANNEL_FIELD_BOSS_ID: int = int(os.environ["CHANNEL_FIELD_BOSS_ID"])
TERMS_MESSAGE_ID: int | None = int(os.environ["TERMS_MESSAGE_ID"]) if os.getenv("TERMS_MESSAGE_ID") else None

# ─── 권한 역할 ────────────────────────────────────────────────────
ROLE_미인증_ID: int  = int(os.getenv("ROLE_미인증_ID",  "0") or "0")
ROLE_접속대기_ID: int = int(os.environ["ROLE_접속대기_ID"])
ROLE_인증유저_ID: int = int(os.environ["ROLE_인증유저_ID"])

# ─── 클래스 역할 (미설정 시 0 — 해당 버튼 숨김) ─────────────────
CLASS_ROLE_IDS: dict[str, int] = {
    "sword":    int(os.getenv("ROLE_검사_ID",       "0") or "0"),
    "axe":      int(os.getenv("ROLE_도끼전사_ID",   "0") or "0"),
    "spear":    int(os.getenv("ROLE_창기사_ID",     "0") or "0"),
    "crossbow": int(os.getenv("ROLE_석궁사_ID",     "0") or "0"),
    "scythe":   int(os.getenv("ROLE_낫사냥꾼_ID",   "0") or "0"),
    "staff":    int(os.getenv("ROLE_지팡이술사_ID", "0") or "0"),
}

# ─── 알림 구독 역할 (미설정 시 0) ────────────────────────────────
NOTIFY_ROLE_IDS: dict[str, int] = {
    "필드보스알림": int(os.getenv("ROLE_필드보스알림_ID", "0") or "0"),
    "시즌보스알림": int(os.getenv("ROLE_시즌보스알림_ID", "0") or "0"),
    "업데이트알림": int(os.getenv("ROLE_업데이트알림_ID", "0") or "0"),
}
