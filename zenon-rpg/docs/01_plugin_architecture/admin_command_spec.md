# 관리자 커맨드 설계서 (`/poro`)

> **[STATUS: REFERENCE]** — `/poro` 관리자 명령어 상세 참조. 공식 방향성은 `CANON.md`와 `../final_master_plan.md`가 우선.

> 작성 기준: `../final_master_plan.md` §15 (2026-05-22 재정리 반영)  
> 권한 노드: `poro.admin`  
> 대상: ZenonRPG — Command Layer (`PoroCommand.java`)

---

## 1. 공통 규칙

- 모든 서브커맨드 형식: `/poro <category> <subcommand> [args]`
- `<player>` 생략 시 자신에게 적용
- 탭 자동완성 지원 (모든 인자)
- 실행 결과는 실행자 채팅으로 피드백

---

## 2. GUI 강제 오픈 (`/poro gui`)

| 커맨드 | 동작 |
|---|---|
| `/poro gui <player> main` | 메인 GUI 오픈 |
| `/poro gui <player> equipment` | 장비 GUI 오픈 |
| `/poro gui <player> territory` | 영지 GUI 오픈 |
| `/poro gui <player> boss` | 보스 GUI 오픈 |
| `/poro gui <player> storage` | 영지 저장고 GUI 오픈 |
| `/poro gui <player> workshop` | 공방 가공기 GUI 오픈 |
| `/poro gui <player> enhance` | 강화 GUI 오픈 |
| `/poro gui <player> potential` | 잠재 GUI 오픈 |
| `/poro gui <player> succession` | 전승 GUI 오픈 |
| `/poro gui <player> estate-setting` | 영지 설정 GUI 오픈 |
| `/poro gui <player> shop` | 상점 GUI 오픈 |

---

## 3. 경제 (`/poro gold`, `/poro item`)

| 커맨드 | 동작 |
|---|---|
| `/poro gold give <player> <amount>` | 골드 지급 |
| `/poro gold take <player> <amount>` | 골드 차감 |
| `/poro gold set <player> <amount>` | 골드 설정 |
| `/poro gold check <player>` | 보유 골드 확인 |
| `/poro item give <player> <item-id> [amount]` | 커스텀 아이템 지급 (인벤토리) |
| `/poro item storage give <player> <item-id> <amount>` | 영지 저장고에 아이템 추가 |

---

## 4. 영지 (`/poro estate`)

| 커맨드 | 동작 |
|---|---|
| `/poro estate rank set <player> <rank>` | 작위 설정 (개척지~공작령) |
| `/poro estate slot set <player> <slot-num> <herb\|ore\|workshop>` | 시설 슬롯 배정 |
| `/poro estate slot reset <player>` | 시설 슬롯 전체 초기화 |
| `/poro estate slot list <player>` | 슬롯 배정 현황 출력 |
| `/poro estate produce <player>` | 즉시 생산 사이클 1회 실행 (테스트용) |

---

## 5. 장비 성장 (`/poro enhance`, `/poro potential`, `/poro succession`)

| 커맨드 | 동작 |
|---|---|
| `/poro enhance set <player> <slot> <level>` | 강화 수치 강제 설정 |
| `/poro potential reroll <player> <slot>` | 잠재 강제 재롤 |
| `/poro potential set <player> <slot> <grade>` | 잠재 등급 강제 설정 |
| `/poro succession give <player> <equip-trace-id>` | 장비의 흔적 지급 |

> `<slot>`: WEAPON / HELMET / CHESTPLATE / LEGGINGS / BOOTS  
> `<grade>`: COMMON / RARE / EPIC / UNIQUE / LEGENDARY  
> `<equip-trace-id>`: equip_trace_broken / equip_trace_faded / equip_trace_glowing / equip_trace_radiant / equip_trace_brilliant

---

## 6. 포션·버프 (`/poro potion`, `/poro buff`)

| 커맨드 | 동작 |
|---|---|
| `/poro potion reset <player>` | 보스전 포션 사용 횟수 초기화 |
| `/poro buff clear <player>` | 모든 버프(만찬·포션) 제거 |
| `/poro buff check <player>` | 현재 적용 버프 목록 출력 |

---

## 7. 보스 (`/poro boss`)

| 커맨드 | 동작 |
|---|---|
| `/poro boss spawn <boss-id> [location]` | 보스 강제 소환 |
| `/poro boss kill <boss-id>` | 보스 즉시 처치 |
| `/poro boss clearrecord set <player> <boss-id>` | 클리어 기록 추가 |
| `/poro boss clearrecord reset <player> <boss-id>` | 클리어 기록 초기화 |
| `/poro boss drop simulate <boss-id>` | 드랍 테이블 시뮬레이션 (콘솔 출력) |

> `<boss-id>` 예시: field_boss_prairie / field_boss_mine / season_boss_1 / season_boss_final

---

## 8. 플레이어 상태 조회 (`/poro check`)

| 커맨드 | 동작 |
|---|---|
| `/poro check <player>` | 플레이어 전체 상태 요약 출력 (골드·작위·강화수치·버프·포션횟수) |
| `/poro check <player> estate` | 영지 슬롯 배정·저장고 현황 출력 |
| `/poro check <player> equipment` | 장착 장비 전체 수치 출력 |

---

## 9. 시스템 (`/poro reload`)

| 커맨드 | 동작 |
|---|---|
| `/poro reload` | 설정·레지스트리 전체 리로드 (서버 재시작 없이) |
| `/poro reload drops` | 드랍 테이블만 리로드 |
| `/poro reload recipes` | 공방 레시피만 리로드 |

---

## 10. 구현 참고사항

### TabCompleter 연결

`PoroCommand`의 `onTabComplete()`에서 depth별 자동완성 제공:

```
depth 0: gui / gold / item / estate / enhance / potential / succession / potion / buff / boss / check / reload
depth 1 (gui): <온라인 플레이어 목록>
depth 2 (gui): main / equipment / territory / boss / storage / workshop / enhance / potential / succession / estate-setting / shop
depth 1 (boss): spawn / kill / clearrecord / drop
depth 2 (boss spawn): <boss-id 목록>
...
```

### 권한 체크

모든 서브커맨드 진입 전 `sender.hasPermission("poro.admin")` 체크 → 없으면 `§c권한이 없습니다.`
