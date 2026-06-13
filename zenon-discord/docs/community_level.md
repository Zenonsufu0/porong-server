# 커뮤니티 레벨 설계 (T13)

> **[STATUS: DRAFT]** — 2026-06-06. 디스코드 활동 기반 레벨·칭호·리더보드. 게임 무관, 봇 DB.
> 저장 = [`data_model.md`](data_model.md) §2.2 `community_xp`·§2.3 `titles`. 인터페이스 단계(구현 전).

## 0. 목적 / 경계
- 디스코드 **활동량**으로 레벨이 오르는 커뮤니티 기능(채팅·음성). 게임 레벨과 무관.
- `discord_user_id` 기준, **길드 전역**(도메인 무관). 종료 서버 게이팅 영향 없음(공통 기능).
- **`message_content` 인텐트 미사용** — 메시지 *내용*은 안 읽고 "작성 이벤트"(작성자·채널)만 카운트.
- 음성 = `voice_states`(비특권, 기존 ON).

## 1. XP 소스

### 1.1 채팅 XP
- 메시지 작성 시 `CHAT_XP_PER_MSG` 지급. **쿨다운 `CHAT_XP_COOLDOWN_SEC`(예 60초)** 내 추가 메시지는 XP 0(도배 방지).
- 내용·길이 무관(내용 미열람). **제외:** 봇 메시지, 명령 입력, 제외 채널(명령/음성-텍스트 등 `XP_EXCLUDE_CHANNEL_IDS`).
- `community_xp.last_message_ts`로 쿨다운 판정.

### 1.2 음성 XP
- 음성채널 체류를 주기 tick(`VOICE_TICK_SEC`, 예 60초)으로 `VOICE_XP_PER_TICK` 적립 + `voice_seconds` 누적.
- **어뷰징 제외(지급 안 함):** self-mute/self-deaf · AFK 채널 · **혼자(같은 채널 타 인원 0)**.
- leave/이동 시 마지막 구간 정산. 재시작 시 진행 중 세션은 유실 허용(경미).

## 2. XP → 레벨 곡선
- 점증 곡선(고레벨일수록 더 필요). **제안 공식:** 레벨 `L` 도달 누적 XP = `5*L^2 + 50*L + 100` (MEE6 유사, 튜닝 가능).
- `community_xp.level`은 캐시 — `xp` 갱신 시 재계산. 단조 증가만(레벨 다운 없음, 운영 회수 제외).

## 3. 레벨업 / 칭호 / 닉네임 표시 (결정 반영)

### 3.1 닉네임 레벨 prefix `[LV.nn]`
> ⚠ **1차 보류 권장**(검토): Manage Nicknames 레이트리밋·상위역할 skip·base 닉 파싱 리스크가 코스메틱 대비 큼. 1차는 `/레벨` 카드 표시만으로 충분. 아래는 도입 시 설계.
- 디스코드 닉네임 앞에 `[LV.05] 닉네임` 자동 표시. 레벨업 시 갱신.
- 구현: 기존 닉네임에서 `^\[LV\.\d+\]\s*` 프리픽스를 제거 후 새 레벨로 재부여(별도 저장 불필요, 기존 닉을 base 로).
- **디스코드 제약(주의):**
  - 봇에 **Manage Nicknames** 권한 필요. **봇보다 상위 역할(운영자 등)의 닉은 변경 불가** → 그 경우 prefix 생략(조용히 skip).
  - **T11(`/닉네임변경`)과는 무관:** T11은 *마인크래프트 연동 닉*(게임서버 `/admin/relink`)을 바꾸는 것이고, `[LV.nn]`은 *디스코드 닉네임* prefix다. 서로 다른 축 — base 는 유저의 디스코드 닉(마크 닉과 무관). 봇은 기존 디스코드 닉에서 prefix만 떼고 재부여.

### 3.2 칭호 — 누적 보유 + 장착 1개 교체
- `titles.required_level` 도달 시 **보유 추가**(`user_titles`). 보유 칭호는 누적(회수 안 함).
- 유저는 보유 중 **1개를 장착**(`/칭호`로 교체). 장착 칭호는 `/레벨` 카드에서 레벨 옆에 표시.
- **칭호 ≠ 디스코드 역할.** 칭호는 봇이 관리하는 **순수 표시 데이터**(코스메틱) — 역할 생성·부여 없음(역할 난립 방지). 표시는 봇 임베드/카드에서만.
- 저장 = [`data_model.md`](data_model.md) §2.3 `user_titles`(owned + equipped).

### 3.3 레벨업/칭호 획득 알림 — 전용 채널 공지(확정)
- 레벨 상승·신규 칭호 획득 시 **전용 채널**(`CHANNEL_LEVELUP_ID`, 신규)에 축하 메시지 게시.
- 채널 ID 는 `config.py`+`.env`. notifier(T1) 경유 또는 community 모듈 직접 전송.

## 4. 명령어
| 명령 | 설명 | 게이트 |
|---|---|---|
| `/레벨 [유저]` | 본인/타인 레벨·XP·다음 레벨까지 + 장착 칭호 표시 | 공통(게이트 없음) |
| `/칭호` | 보유 칭호 목록 → 장착 1개 선택/교체(버튼) | 공통 |
| `/리더보드` | `community_xp` 상위 N(순위·레벨·XP) + 본인 순위, 버튼 페이지네이션 | 공통 |
| `/xp지급` `/xp회수` | 운영 보정(`mod_log` 기록) | `requires_permission` |

## 5. 어뷰징 방지 요약
- 채팅 쿨다운 · 음성 제외(mute/AFK/혼자) · 봇·명령채널 제외 · 운영 회수 명령(mod_log).

## 6. 튜닝 파라미터
- `config.py` 상수 + 일부 `.env`: `CHAT_XP_PER_MSG`·`CHAT_XP_COOLDOWN_SEC`·`VOICE_XP_PER_TICK`·`VOICE_TICK_SEC`·`XP_EXCLUDE_CHANNEL_IDS`·`AFK_CHANNEL_ID`·곡선 계수.
- 운영 중 조정이 잦으면 DB 설정 테이블로 승격 검토.

## 7. 확정 / 미확정
- ✅ 칭호 = **누적 보유 + 장착 1개 교체**(`/칭호`). 닉네임 `[LV.nn]` prefix 자동.
- ✅ 레벨업·칭호 알림 = **전용 채널 공지**(`CHANNEL_LEVELUP_ID`).
- ⬜ XP 수치·곡선 계수 최종값(밸런스) — 1차는 제안 디폴트.
- ⬜ XP 소스에 reaction 등 추가 포함 여부(현재 채팅·음성만).
- ⬜ 닉 prefix 갱신 빈도(레벨업 즉시 vs 주기 배치) — Manage Nicknames 레이트리밋 고려.

## 8. 임시 음성채널 (T13)
- "입장 시 생성" 패턴: 트리거 허브 채널 입장 → 봇이 **그 허브와 같은 카테고리 안에** 개인 음성방 생성·이동.
- 방이 **비면 자동 삭제**(orphan 정리). 재시작 시 빈 임시방 청소 루틴.
- `voice_states`(비특권)만 사용. 별도 DB 불필요(런타임 추적) — 필요 시 경량 매핑.
- ✅ **결정(2026-06-09): 허브 = 게임 카테고리마다 1개(다중 허브).** T17 서버 템플릿이 각 게임/시즌 카테고리에 `➕ 음성방 만들기` 허브를 자동 생성 → 그 게임 플레이어끼리 해당 카테고리 안에서 음성방 생성. (구 단일 전역 `VOICE_CREATE_HUB_ID` 단수 가정을 **다중 허브로 SUPERSEDE**.)
  - 허브 식별 방법(T13 구현 시 택1): ⓐ 허브 채널 ID 집합 보유(템플릿 생성 시 레지스트리/런타임에 등록) · ⓑ 채널 이름 매칭(`➕ 음성방 만들기`) · ⓒ servers 레지스트리에 hub_channel_id 컬럼 추가. 생성 음성방의 부모 카테고리 = 입장한 허브의 `category` 로 결정.
  - 전역 커뮤니티 라운지가 별도로 필요하면 공통 카테고리에도 허브 1개 추가 가능(템플릿과 무관하게 운영 설정).
  - ⚠ DL 동기화 대기(RPG worktree) — 디스코드 워크트리는 decision_log 읽기전용.

## 9. 출석 / 일일보상 (T14) 🟢 (2026-06-10 구현)
- 🟢 **`modules/community/attendance.py` + `core/attendance.py` + db v12 `attendance`.**
- `/출석` → 하루 1회(KST=UTC+9) 체크. `attendance`(streak·total·last_date) 갱신. last_date 가 어제면 streak+1, 그 외(공백·이틀 이상 공백)면 1로 리셋.
- 보상 XP = `ATTENDANCE_XP_BASE + min(streak, CAP) * ATTENDANCE_XP_PER_STREAK`(config, 기본 100+streak×10, 캡 30) → `community.add_xp` 가산. 레벨업 시 `CommunityLevelCog._handle_levelup` 위임(칭호 연동 재사용).
- 중복 출석은 "오늘 이미 출석" 안내(ephemeral). 정상 출석은 공개 임베드(streak·total).

## 10. 임시역할 자동만료 (T14) 🟢 (2026-06-10 구현)
- 🟢 **`modules/community/temp_roles.py` + `core/temp_roles.py` + db v13 `temp_roles`.**
- `/임시역할부여 <유저> <역할> <기간> <단위(분/시간/일)> [사유]`(admin) → 역할 부여 + `temp_roles`(expires_at) 적재 + mod_log(temp_role_grant). 위계/관리역할/@everyone 가드.
- tick 60초(`tasks.loop`)로 `expires_at <= now` 조회 → 역할 회수 + 행 삭제 + mod_log(temp_role_expire). 재시작 후에도 DB 기준 복구(before_loop=wait_until_ready). 멤버/역할 없거나 이미 회수돼도 행은 정리.
- 부여 경로 추가 = 이벤트 모듈 등에서 `temp_roles.grant()` 호출.
</content>
