# 봇 데이터 모델 — SQLite 스키마 (T12)

> **[STATUS: DRAFT]** — 봇 측 영속 저장소(SQLite)의 단일 진실원. 2026-06-06 1차안.
> 관련: 생애주기 [`task.md`](task.md) §11(T21) · 기능 백로그 §9 · 통신경계 [`integration_contract.md`](integration_contract.md).

## 0. 대원칙 — 봇 DB는 "디스코드 측 상태"만

게임 상태(프로필·영지·보스·화이트리스트·인증)는 **게임서버가 권위** → 봇 DB에 복제하지 않고 필요 시 HTTP 조회(`integrations/*_api.py`). 봇 SQLite는 **디스코드가 고유하게 만들어내는 데이터**만 보관한다.

| 저장 (봇 DB) | 저장 안 함 (원격 조회) |
|---|---|
| 커뮤니티 레벨·XP·칭호, 경고·제재로그·티켓·출석, 임시역할, 서버 레지스트리 | 플레이어 프로필·영지·보스, 화이트리스트·인증상태, 마크닉↔디스코드 매핑(권위=게임서버) |

- **유저 식별은 `discord_user_id`(정수, 디스코드 ID) 기준.** 마크 닉네임은 저장하지 않는다(필요 시 API 조회).
- 디스코드 길드는 1개 전제 → guild_id 컬럼 생략(필요해지면 추가).

## 1. 런타임 / 접근 계층

- **파일:** `<인스턴스 로컬>/porong_bot.sqlite3` — **gitignored**(런타임 데이터, 커밋 금지). 경로는 `.env`(`BOT_DB_PATH`).
- **드라이버:** `aiosqlite`(비동기) — discord 이벤트 루프 블로킹 방지. 동기 `sqlite3` 금지.
- **접근 계층:** `core/db.py` — 연결 풀/단일 커넥션 + 쿼리 헬퍼. 각 모듈은 `core/db.py` 경유로만 접근(도메인 모듈이 raw SQL 흩뿌리지 않음).
- **마이그레이션:** `schema_meta.version` 기반 단순 증분 러너(기동 시 현재 버전 → 목표 버전까지 순차 적용).

## 2. 테이블

### 2.0 `schema_meta` — 스키마 버전
| 컬럼 | 타입 | 설명 |
|---|---|---|
| key | TEXT PK | `version` 등 |
| value | TEXT | 값 |

### 2.1 `servers` — 서버 레지스트리 / 생애주기 (T21)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | INTEGER PK | |
| domain | TEXT | `rpg` / `poromon` / … (게임 종류) |
| season_no | INTEGER | 시즌 번호 |
| display_name | TEXT | 표시명(예: "RPG 시즌2") |
| state | TEXT | `prep` / `active` / `ended` |
| category_id | INTEGER | 디스코드 카테고리 ID |
| access_role_id | INTEGER | 접근 역할 ID(가시성) |
| api_env_key | TEXT | API URL/키가 담긴 `.env` 키 이름(값 자체는 저장 X) |
| created_at | INTEGER | epoch |
| ended_at | INTEGER NULL | 종료 시각 |
| pending_role_id | INTEGER NULL | 온보딩 인증전 역할(v4, 자동전개 시) |
| player_role_id | INTEGER NULL | 온보딩 플레이어 역할(v4, 자동전개 시) |

- `UNIQUE(domain, season_no)`. 새 시즌 = 행 추가(코드 변경 0). 새 게임 종류 = 모듈 코드 추가 필요(§11).
- 🟢 **v4(2026-06-09): 온보딩 3역할 + 다중 카테고리(T17 프리픽스 그룹).** `access_role_id`(=접근)·`pending_role_id`·`player_role_id` 3역할 + 아래 `server_categories`. 단수 `category_id`는 수동연결 경로 유지. 중첩 시즌/서버 미운영 확정(domain당 active 1)이라 프리픽스 카테고리 충돌 없음.

### 2.1b `server_categories` — 서버 다중 카테고리 (T17, v4)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| server_id | INTEGER | `servers.id` |
| group_key | TEXT | `onboarding`/`info`/`community`/`support` (템플릿 그룹) |
| category_id | INTEGER | 디스코드 카테고리 ID |

- `PRIMARY KEY(server_id, group_key)`. 자동전개(`/서버신설`)가 그룹별 카테고리 1개씩 적재.
- 게이팅 2층(`requires_category`)은 `get_active_category_ids`(단수 `category_id` ∪ 이 테이블)로 **집합 매칭**.
- 게이팅 3층의 ③서버상태가 이 `state`를 참조. `ended` = 모든 명령·토글 거부(완전 비활성, 확정).
- 🟢 **v6(2026-06-09): 전역 active 1개 강제** — `idx_servers_single_active`(UNIQUE ON `state` WHERE `state='active'`). RPG·포로몬 통틀어 항상 한 서버만 active(task.md §5 전역 단일 모델). 기존 per-domain 인덱스는 포섭됨.
- 🔴 **domain당 `active` 최대 1개 강제**(부분 유니크 인덱스 `WHERE state='active'` 또는 앱 가드). `requires_category(domain)`/`requires_server_active(domain)`이 **domain당 카테고리·active 단수**를 가정하므로, 같은 domain active 2개(예 S2 종료중 + S3 오픈)면 게이팅이 어느 카테고리를 기준할지 모호해짐.
  - **1차: 멀티시즌 동시운영 미사용**(시즌은 순차 — S2 `ended` 후 S3 `active`). 시즌 중첩 운영이 필요해지면 게이팅 키를 `domain`이 아니라 `category_id` 기준으로 재설계.

### 2.2 `community_xp` — 커뮤니티 레벨 (T13)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| discord_user_id | INTEGER PK | |
| xp | INTEGER | 누적 XP |
| level | INTEGER | 캐시된 레벨(xp 파생) |
| last_message_ts | INTEGER | 채팅 XP 쿨다운(어뷰징 방지) |
| voice_seconds | INTEGER | 누적 음성 활동(초) |
| updated_at | INTEGER | |

- XP는 길드 전역(디스코드 활동 기준), 도메인 무관. **메시지 내용 미열람** — 작성자·채널·voice state만 집계.
- 🟢 **구현(2026-06-09, v8): `core/community.py`(곡선·접근) + `modules/community/level.py`** — 채팅(쿨다운)·음성(tick, mute/AFK/혼자 제외) XP, `/레벨`·`/리더보드`, 레벨업 알림(`CHANNEL_LEVELUP_ID`). 곡선 = `5L²+50L+100`. 칭호(§2.3)·XP보정은 후속.

### 2.3 `titles` — 칭호 카탈로그 (T13)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | INTEGER PK | |
| key | TEXT UNIQUE | 내부 키 |
| display_name | TEXT | 표시명 |
| required_level | INTEGER NULL | 레벨 임계(자동 획득 조건) |

- **칭호는 디스코드 역할이 아니다.** 봇이 관리하는 순수 표시 데이터(코스메틱). 보유=`user_titles`, 장착(표시)=`user_titles.equipped`. 역할 부여·생성 없음(역할 난립 방지).

### 2.3b `user_titles` — 보유/장착 칭호 (T13)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| discord_user_id | INTEGER | |
| title_id | INTEGER | `titles.id` FK |
| acquired_at | INTEGER | 획득 시각 |
| equipped | INTEGER | 1=장착(표시), 0=보유만 |

- `PRIMARY KEY(discord_user_id, title_id)`. 보유는 누적(회수 안 함). **유저당 equipped=1 은 최대 1개**(교체 시 기존 0으로).
- 닉네임 `[LV.nn]` prefix 는 `community_xp.level` 파생(별도 저장 없음, 기존 닉을 base 로 재적용).
- 🟢 **구현(2026-06-09, v9): `core/titles.py`** — 카탈로그 시드 5종(Lv5~50), 레벨업 시 `newly_eligible`→`grant_title` 자동 획득, `/칭호`(Select 장착, equipped 유일), `/레벨` 카드 표시. 칭호=역할 아님(코스메틱).

### 2.4 `warnings` — 경고 (T15)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | INTEGER PK | |
| discord_user_id | INTEGER | 대상 |
| reason | TEXT | 사유 |
| operator_id | INTEGER | 처리 운영자 |
| created_at | INTEGER | |
| active | INTEGER | 1=유효, 0=철회 |

- 🟢 **구현(2026-06-09): v3 마이그레이션 = `warnings` 테이블**(`core/db.py`) + **`core/warnings.py`** 접근 계층(add/list/count_active/get/revoke). `/경고`·`/경고목록`·`/경고취소`(`modules/moderation/`)에서 사용. 철회=`active=0`(행 보존).

### 2.5 `mod_log` — 운영/감사 로그 (T15)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | INTEGER PK | |
| action | TEXT | `warn`/`timeout`/`kick`/`ban`/`server_start`/`server_end`/`relink`… |
| target_id | INTEGER NULL | 대상 유저(해당 시) |
| operator_id | INTEGER | 처리 운영자 |
| reason | TEXT NULL | |
| detail | TEXT NULL | JSON 등 부가정보 |
| created_at | INTEGER | |

- 모든 제재·서버 생애주기·운영명령은 여기에 적재 + `#운영로그` 채널 게시.
- 🟢 **구현(2026-06-09): v2 마이그레이션 = `mod_log` 테이블**(`core/db.py`) + **`core/mod_log.py` `record()` 헬퍼**(DB 적재 보장 + `#운영로그`(`CHANNEL_MODLOG_ID`) 임베드 게시 best-effort). 서버 생애주기(`/서버신설`·`/서버시작`·`/서버종료`) 배선 완료. `warnings`(§2.4)는 T15 모더레이션 모듈과 함께 v3 추가.

### 2.6 `tickets` — 티켓/문의 (T16)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | INTEGER PK | |
| channel_id | INTEGER | 생성된 비공개 채널 ID |
| opener_id | INTEGER | 개설자 |
| domain | TEXT NULL | 관련 서버(선택) |
| state | TEXT | `open` / `closed` |
| created_at | INTEGER | |
| closed_at | INTEGER NULL | |
| closed_by | INTEGER NULL | |

### 2.7 `attendance` — 출석 (T14)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| discord_user_id | INTEGER PK | |
| last_date | TEXT | `YYYY-MM-DD`(KST) |
| streak | INTEGER | 연속 출석일 |
| total | INTEGER | 누적 출석일 |

### 2.8 `temp_roles` — 임시역할 자동만료 (T14)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | INTEGER PK | |
| discord_user_id | INTEGER | |
| role_id | INTEGER | |
| expires_at | INTEGER | 만료 epoch — 도달 시 봇이 회수 |
| reason | TEXT NULL | |

### 2.9 `faq` — 자주 묻는 질문 (T16)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | INTEGER PK | |
| domain | TEXT NULL | NULL=공통, 아니면 서버별 |
| trigger | TEXT | 키워드/질문 |
| answer | TEXT | 답변 |
| updated_by | INTEGER NULL | |
| updated_at | INTEGER | |

- 미매칭 시 "운영진 문의"(티켓) 폴백. LLM 미사용.

### 2.10 `terms` — 서버별 약관 (T7 온보딩, v5)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| domain | TEXT PK | 서버 도메인(온보딩 키, domain당 1건) |
| content | TEXT | 약관 본문(최대 4000자 — 모달 입력 상한) |
| updated_by | INTEGER NULL | 수정 운영자 |
| updated_at | INTEGER | |

- 🟢 **구현(2026-06-09, v5): `core/terms.py` + `/약관설정`(모달)·`/약관보기`.** 약관은 서버마다/시즌마다 달라 코드 하드코딩 대신 봇 입력·DB 저장. 온보딩 약관 패널(`/온보딩패널`)이 저장본을 게시. 약관 수정은 `mod_log(action=terms_update)` 적재.

## 3. 안 넣는 것 (명시)
- 게임 상태(프로필·영지·보스·화이트리스트) — 원격 HTTP 조회.
- 마크 닉네임↔디스코드 매핑 — 권위=게임서버(`/auth/status`). 봇은 캐시하지 않음(필요 시 조회).
- Tier3(초대추적·건의투표) — 도입 확정 시 테이블 추가.

## 4. 미확정
- XP 곡선(레벨업 공식)·채팅/음성 XP 비율·쿨다운 값 — community 상세설계(T13)에서.
- 칭호 "선택 표시" 도입 여부(→ `user_titles`).
- 아카이브 시즌 누적 정리 정책(§11) — 운영 가이드.
- `core/db.py` 커넥션 전략(단일 vs 풀) — 구현 시 확정.
</content>
</invoke>
