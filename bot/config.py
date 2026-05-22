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
ROLE_접속대기_ID: int = int(os.environ["ROLE_접속대기_ID"])
ROLE_인증유저_ID: int = int(os.environ["ROLE_인증유저_ID"])
TERMS_MESSAGE_ID: int | None = int(os.environ["TERMS_MESSAGE_ID"]) if os.getenv("TERMS_MESSAGE_ID") else None
