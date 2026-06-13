# ZenonMonCore Commands (`/zenonmon` 명령 트리)

> `AdminCommandManager`가 Brigadier로 등록. 상위: `module_structure.md` §3(command).
> 원칙: 플레이어 명령은 누구나, 관리자 하위는 op 권한. **모든 상태 변경은 서버측 검증.**
> ⚠️ 코드 미생성. 본 문서는 구현 기준.

## 1. 권한 레벨
- 플레이어 명령: 권한 불필요(접속자 누구나)
- 관리자 명령: `/zenonmon admin …` → **op 레벨 2 이상**(`requires(src -> src.hasPermissionLevel(2))`)

## 2. 명령 트리 (0.1)

```
/zenonmon                      # 루트: 도움말 출력(또는 menu 안내)
/zenonmon menu                 # GUI 메뉴 열기 (League Pass 아이템 대체 — CLAUDE.md)
/zenonmon hub                  # 허브로 텔레포트 (core.json hub.spawn)
/zenonmon progress             # 내 진행 상황 표시(배지/해금/티켓 수)

/zenonmon admin reload         # config 전체 리로드 (ConfigManager.reload)
/zenonmon admin pass <player>  # League Pass 아이템 재지급/복원
/zenonmon admin ticket give <player> <ticketType>   # 티켓 발급(테스트/운영)
/zenonmon admin progress <player>                   # 대상 플레이어 진행 조회
```

| 명령 | 측 | 권한 | 동작 |
|---|---|---|---|
| `/zenonmon` | 서버 | 누구나 | 사용법/버전 출력 |
| `/zenonmon menu` | 서버→클라 GUI | 누구나 | MenuGuiManager 화면 오픈 패킷 |
| `/zenonmon hub` | 서버 | 누구나 | HubInteractionManager 텔레포트 |
| `/zenonmon progress` | 서버 | 누구나 | PlayerProgress 요약 채팅 출력 |
| `/zenonmon admin reload` | 서버 | op2 | 설정 재로드, 결과 피드백 |
| `/zenonmon admin pass <player>` | 서버 | op2 | MenuItemManager 아이템 지급 |
| `/zenonmon admin ticket give <player> <type>` | 서버 | op2 | EncounterTicketManager 발급 + AuditLog |
| `/zenonmon admin progress <player>` | 서버 | op2 | 대상 진행 조회 |

## 3. 인자 타입
- `<player>`: `EntityArgumentType.player()` (온라인). 오프라인 대상은 향후(UUID/GameProfile).
- `<ticketType>`: 커스텀 Suggestion = `tickets.json`의 `ticketType.id` 목록(`SuggestionProvider`).
- 좌표/숫자 인자는 0.1에 없음(허브는 config 기반).

## 4. 피드백 / 로깅
- 성공/실패는 `source.sendFeedback`/`sendError`로 명확히(한국어 메시지).
- 관리자 명령·티켓 발급·아이템 지급은 `AuditLogManager`에 기록(`logAdminCommand`, `logTicketUse`).
- 리로드 시 "레지스트리성 변경(아이템 등)은 재시작 필요" 안내.

## 5. 향후 명령 (참고, 0.1 제외)
```
/zenonmon badges                         # 배지 목록 (gym)
/zenonmon altar                          # 레전드 제단 정보/사용 (encounter)
/zenonmon admin room list|clear          # 룸 세션 관리 (room)
/zenonmon admin unlock <player> <mega|tera|…>   # 해금 부여 (mega)
/zenonmon admin reward <player> <id>     # 보상 지급 (reward)
/zenonmon league …                       # 리그/시즌 (season)
```

## 6. 등록 / 초기화
- `CommandRegistrationCallback.EVENT`에서 등록(서버측). `module_structure.md` 초기화 순서 7번.
- 클라 명령 자동완성은 서버가 보낸 command tree로 동작 → 별도 클라 등록 불필요.

## 7. 관련
- 모듈: `module_structure.md`
- 설정 참조: `config_structure.md` (menuItem/hub/tickets)
- 데이터: `database_schema.md` (progress/ticket)
- 스펙 0.1: `zenonmoncore_spec.md`
