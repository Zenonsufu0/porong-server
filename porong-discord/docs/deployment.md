# 배포 / 운영 런북 (T8)

> **[STATUS: ACTIVE]** — 봇 구동·스테이징 검증 절차. 호스팅 위치는 무관(오라클·VPS·집서버·도커
> 어디든). 봇은 호스팅 비종속이며, 위치에 따라 달라지는 건 코드가 아니라 `.env` 값뿐이다.
> 코드 사실 기준(2026-06-10): discord.py 2.3.2 / Python 3.12 / aiosqlite·aiohttp·mcstatus.

## 0. 전제

- 게임 호스팅과 **분리**된 상시 프로세스(봇은 디스코드 측 운영 허브, 게임 로직 아님 — DL-133).
- 단일 길드 전제(`GUILD_ID`). 멀티 길드 미지원.
- 비밀정보(토큰·키·시크릿)는 `.env`(gitignored)에만. **절대 커밋 금지.**

## 1. 런타임 설치

```bash
python3.12 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt          # discord.py·aiohttp·python-dotenv·aiosqlite·mcstatus
cp .env.example .env                      # 값 채우기(§3)
python -m compileall main.py core integrations modules   # 구문 점검(선택)
python main.py                            # 기동
```

프로세스 관리(상시): systemd 서비스/`Restart=on-failure` 또는 docker `restart: unless-stopped`.
표준출력 로그 수집 + 재기동 정책 권장. (mcstatus 미설치 시 접속정보 기능만 graceful 비활성.)

## 2. 디스코드 봇 설정 (Developer Portal)

### 2.1 특권 인텐트 (Privileged Gateway Intents)
| 인텐트 | 필요? | 용도 |
|---|---|---|
| **SERVER MEMBERS** | ✅ ON | 온보딩 역할전이·생애주기 일괄 전이·`on_member_join` 자동배정 (`intents.members`) |
| MESSAGE CONTENT | ❌ OFF | **미사용 정책**(§9.2). 메시지 내용 미열람 — 켜지 말 것 |
| PRESENCE | ❌ OFF | 불필요 |

> `voice_states`(임시음성·음성 XP)는 **비특권**이라 `Intents.default()`에 포함 — 별도 설정 불필요.

### 2.2 봇 권한 (역할 또는 초대 URL 스코프)
실제 호출 API 기준 필요 권한:

| 권한 | 쓰는 기능 |
|---|---|
| **Manage Channels** | 서버신설(카테고리·채널 생성), 티켓/임시음성 채널 생성·삭제, 가시성(`set_permissions`) |
| **Manage Roles** | 온보딩 3역할 생성·전이, 임시역할 부여/회수, 클래스/알림 역할 토글 |
| **Moderate Members** | `/타임아웃` |
| **Kick Members** | `/추방` |
| **Ban Members** | `/차단`·`/차단해제` |
| **Move Members** | 임시음성 개인방으로 이동 |
| View Channels · Send Messages · Embed Links · Read Message History | 알림·패널·임베드 기본 |

> ⚠ **역할 위계:** 봇 최상위 역할이 봇이 다루는 역할(온보딩·임시·제재 대상)보다 **위**에 있어야
> 한다. 아니면 `Forbidden`(코드가 graceful 안내). 권한 역할(owner/admin/매니저)은 봇이 절대
> 자동 지급하지 않음 — 운영진 수동 부여(권한 상승 통로 차단).
> Manage Nicknames(닉 `[LV.nn]` prefix)는 현재 **보류** 기능이라 미필요.

## 3. `.env` 설정

### 3.1 필수 (없으면 기동 실패 — `os.environ`)
| 키 | 설명 |
|---|---|
| `DISCORD_TOKEN` | 봇 토큰 |
| `GUILD_ID` | 운영 길드 ID |
| `CHANNEL_FIELD_BOSS_ID` | 필드보스 알림 채널 |
| `PORONG_API_KEY` | RPG 게임서버 API 키(X-Api-Key) |

> ⚠ 브랜드 변경(2026-06-10): 구 `PORO_API_URL/KEY` → **`PORONG_API_URL/KEY`**. 기존 `.env` 갱신 필요.

### 3.2 선택 (미설정 시 0/기본값 → 해당 기능 graceful 비활성)
- **API:** `PORONG_API_URL`(기본 localhost:8765), `POROMON_AUTH_URL/KEY`
- **DB:** `BOT_DB_PATH`(기본 `porong_bot.sqlite3`, 인스턴스 로컬·gitignored)
- **채널:** `CHANNEL_MODLOG_ID`(운영로그)·`CHANNEL_NOTICE_ID`(공지)·`CHANNEL_POROMON_NOTICE_ID`·
  `CHANNEL_BUGREPORT_ID`·`CHANNEL_LEVELUP_ID`·`AFK_CHANNEL_ID`·`CATEGORY_티켓_ID`
- **전역 단일 active:** `ROLE_서버준비_ID`·`CATEGORY_통합_ID`(생애주기 일괄 전이용)
- **권한 역할:** `ROLE_OWNER_ID`·`ROLE_ADMIN_ID`·`ROLE_RPG_MANAGER_ID`·`ROLE_POROMON_MANAGER_ID`·
  `ROLE_EVENT_MANAGER_ID`·`ROLE_SUPPORT_ID` — **운영 명령 권한 판정에 필수적**(미설정 시 owner만 통과)
- **알림 역할:** `ROLE_필드보스알림_ID`·`시즌보스`·`월드보스`·`포로몬`·`이벤트`·`점검`·`업데이트알림_ID`
- **클래스 역할:** `ROLE_검사_ID` 등 6종
- **튜닝:** `CHAT_XP_*`·`VOICE_*`·`ATTENDANCE_XP_*`·`XP_EXCLUDE_CHANNEL_IDS`
- **온보딩(포로몬):** `ROLE_포로몬접근/인증전/플레이어_ID`·`CHANNEL_포로몬약관/인증_ID`

### 3.3 인바운드 알림 리스너 (선택 — 게임서버 push 수신, T1)
`INBOUND_SECRET`·`INBOUND_PORT` **둘 다 설정해야 기동**(미설정 시 무인증 엔드포인트 안 엶).
- `INBOUND_SECRET`(강엔트로피, 게임서버와 공유) · `INBOUND_HOST`(기본 127.0.0.1) · `INBOUND_PORT`
- `INBOUND_ALLOW_IPS`(게임 호스팅 IP, 방화벽과 이중) · `INBOUND_TS_TOLERANCE`(기본 300s)
- ⚠ 외부 노출 시 **방화벽/보안그룹으로 게임 IP만 허용** + 시크릿 + (가능하면) 역프록시 TLS.

### 3.4 DEPRECATED (미참조·optional — 비워둬도 됨)
`CHANNEL_AUTH_ID`·`ROLE_미인증/접속대기/인증유저_ID`·`TERMS_MESSAGE_ID` (구 RPG 단일서버 온보딩, DL-138 폐기).

## 4. 운영 런북 (서버 시즌 시작 절차)

전역 단일 active 모델 — 한 시점에 한 게임만 운영(task.md §5).

1. `/서버신설 <도메인> <시즌> <표시명> [접속주소]` — 레지스트리 `prep` + 카테고리/3역할 자동 전개.
2. `/약관설정` — 약관 본문 입력(모달).
3. `/서버시작 <id>` — `prep→active` + 카테고리 가시화 + **온보딩 패널 자동 게시** + 일괄 역할 전이.
4. (선택) `/서버주소`로 접속주소 등록 → `접속정보` 채널 라이브 갱신.
5. 시즌 종료: `/서버종료 <id> <사유>` — 아카이브 + `서버준비` 일괄 부여.

> 신규 유저: 가입 → 자동 임시역할 → 약관 동의 버튼 → 인증 버튼/모달(인게임 `/인증` 코드) → 플레이어 승급.

## 5. 스테이징 e2e 체크리스트 (실길드 — 이 환경에선 미검증)

- [ ] 봇 기동·슬래시 동기화(`/핑`) / 인텐트·권한 경고 없는지 로그 확인
- [ ] `/서버신설`→`/서버시작` 카테고리·역할 자동 생성 + 가시성
- [ ] 온보딩: 약관 버튼→인증전 역할, 인증 모달→verify(RPG 게임서버 `/auth/verify` 준비 시)→플레이어
- [ ] 생애주기 일괄 역할 전이(`/서버종료`·`/서버시작`) — 인원 많으면 수십초(레이트리밋)
- [ ] 커뮤니티: 채팅/음성 XP·레벨업 알림·`/레벨`·`/리더보드`·`/칭호`·`/출석`(KST 경계)
- [ ] 임시역할: `/임시역할부여` → 만료 tick 회수 / 임시음성: 허브 입장→개인방→비면 삭제
- [ ] 모더레이션: `/경고`·`/타임아웃`·`/추방`·`/차단` + `#운영로그` 적재
- [ ] 지원: `/문의`(티켓)·`/티켓종료`·`/faq`(패널)·`/FAQ추가`·`/버그제보`(상태버튼·DM)
- [ ] 알림: `/공지`·`/점검`·`/보스알림`·`/이벤트알림` → 공지 채널 게시 + 역할 멘션
- [ ] (선택) 인바운드: 게임서버 push → `/events` 200·서명검증·dedup (게임서버 송신 구현 후)
- [ ] 접속정보: mcstatus SLP 핑 라이브 갱신(3분)

## 6. DB / 백업

- 단일 SQLite 파일(`BOT_DB_PATH`, 기본 `porong_bot.sqlite3`) — 인스턴스 로컬, gitignored.
- 증분 마이그레이션 v1~v13 자동 적용(기동 시 `schema_meta.version`). 다운그레이드 미지원.
- 백업 = 파일 주기 복사(WAL 모드 — `.sqlite3`·`-wal`·`-shm` 함께) 또는 `sqlite3 .backup`.
