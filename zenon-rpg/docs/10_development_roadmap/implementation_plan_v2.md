# 구현 계획서 v2

> **[STATUS: ACTIVE]**
> 최초 작성: 2026-05-28 / 전수 조사 기반 재수립
> v1(`implementation_design_plan.md`) §1~6 완료 이후 잔여 미구현 항목 정리

---

## 0. 갭 분석 요약

전수 조사(2026-05-28) 결과, 아래 항목이 설계서 기준 미구현이거나 잘못 구현되어 있음.

| 분류 | 항목 | 설계서 기준 | 현재 상태 |
|---|---|---|---|
| **HUD** | HP/XP 바 항상 표시 | 1틱 글리프 action bar | 이벤트 시만 텍스트 전송 → 3초 후 소멸 |
| **HUD** | 쿨타임 바 (4스킬) | 글리프 cd_010~cd_100, 1틱 갱신 | `§e스킬명 §c초.초s` 텍스트만 |
| **HUD** | 스택 게이지 | 글리프 filled/empty, 1틱 갱신 | 미구현 |
| **HUD** | XP 바 | 글리프 xp_000~xp_100, 1틱 갱신 | 미구현 |
| **스코어보드** | 사이드바 전체 | 골드/강화석/큐브/레벨/IL/위치 | `refresh()` 빈 메서드 |
| **스킬** | 낫 월영회전 이동 방향 | 키 입력 방향 (W/A/S/D) | `dashSideways()` 고정 |
| **커맨드** | `/메뉴` | MainHubGui 열기 | stub "준비 중" |
| **커맨드** | `/각인` `/캐릭터` `/영지` `/영지이동` `/작물` `/상점` `/영지설정` `/보스정보` `/클리어` | 해당 GUI 열기 | stub "준비 중" |
| **핫바** | HotbarInteractListener | 핫바 아이템으로 메뉴 열기 | `onInteract()` 빈 메서드 |
| **영지** | IridiumSkyblock 섬 크기 확장 | 작위 승급 시 XZ 한도 확장 | TODO 주석만 존재 |

---

## 1. 우선순위

| 순위 | 항목 | 이유 |
|---|---|---|
| P0 | HUD 5레이어 1틱 갱신 | 바닐라 하트/XP가 icons.png로 투명 처리 → 커스텀 HUD 없으면 체력/경험치 불표시 |
| P0 | 스코어보드 사이드바 | 골드·강화석·큐브 잔량 유일 표시 수단 |
| P1 | 낫 월영회전 방향 | 기획서 명시, 현재 우측 고정으로 오작동 |
| P1 | `/메뉴` → MainHubGui | 플레이어가 GUI 진입 불가 |
| P2 | HotbarInteractListener | compass로 메뉴 열기 (현재 MainHubListener에서 처리 중이므로 실제 블로커 아님) |
| P3 | stub 커맨드들 | `/각인` 등 1차 시즌 외 항목 포함 → 선별 구현 |

---

## 2. Phase A — HUD 5레이어 1틱 갱신

### 2.1 설계 기준 (gui_hud_spec.md)

Action Bar 하나에 커스텀 폰트 글리프 5행을 negative ascent로 위치 분리. `sendActionBar()` 1회 호출로 전체 HUD 갱신.

| 행 | 내용 | 글리프 범위 | ascent | 조건 |
|---|---|---|---|---|
| 1 | 스택 자원게이지 | U+E140~E14B | +4 | 무기 착용 시만 |
| 2 | 쿨타임 Row1 (LC / RC) | U+E120~E129 | -6 | 무기 착용 시만 |
| 3 | 쿨타임 Row2 (SRC / F) | U+E130~E139 | -16 | 무기 착용 시만 |
| 4 | XP바 + Lv | U+E150~E164 | -26 | 항상 |
| 5 | HP바 + 현재/최대 | U+E100~E114 | -36 | 항상 |

**갱신 방식:**
- 쿨타임: 1틱(0.05초) 주기 반복 태스크
- HP/XP: 이벤트 발생 시 즉시 + 1틱 태스크 겸용
- 스택: 자원 변경 시 즉시 + 1틱 태스크 겸용

### 2.2 글리프 선택 로직

**HP 글리프 (U+E100 ~ U+E114, 5% 단위 21개):**
```java
int hpPct = (int)(player.getHealth() / maxHp * 100);
int step  = Math.min(20, hpPct / 5);          // 0~20
char hpGlyph = (char)(0xE100 + step);
```

**XP 글리프 (U+E150 ~ U+E164, 5% 단위 21개):**
```java
int step = Math.min(20, (int)(player.getExp() * 20)); // getExp() = 0.0~1.0
char xpGlyph = (char)(0xE150 + step);
```

**쿨타임 글리프 (U+E120~E129 Row1, U+E130~E139 Row2, 10% 단위 10개):**
```java
// remaining: 남은 쿨 ms, total: 전체 쿨 ms
int pct  = (int)(remaining * 100.0 / total);    // 0~100
int step = Math.min(9, (100 - pct) / 10);       // 진행: 0=빈, 9=꽉참
char cdGlyph = (char)(0xE120 + step);           // Row1 기준
// 수치 색상
String numStr = remaining <= 5000
    ? "§c" + fmt(remaining) + "s"
    : "§e" + fmt(remaining) + "s";
// 즉시 사용 가능
String numStr = "§a-";
```

**스택 글리프 (무기별, U+E140~E14B):**
```java
// weaponIdx: 검=0, 도끼=1, 스태프=2, 석궁=3, 낫=4, 창=5
char filled = (char)(0xE140 + weaponIdx * 2);
char empty  = (char)(0xE141 + weaponIdx * 2);
```

### 2.3 구현 대상 파일

| 파일 | 변경 내용 |
|---|---|
| `listener/HealthHudListener.java` | 전면 재작성: 1틱 repeating task + 5레이어 HUD 빌더 |
| `util/HealthHudFormatter.java` | 5레이어 action bar 문자열 생성 로직으로 교체 |

### 2.4 특수 상태 알림 오버라이드 (gui_hud_spec.md §6)

| 이벤트 | 메시지 | 복귀 |
|---|---|---|
| 강화 성공 | `§a✔ 강화 성공! §e{N}강` | 3초 후 HUD 복귀 |
| 강화 실패 | `§c✘ 강화 실패` | 3초 후 HUD 복귀 |
| 레벨업 | `§6★ 레벨 업! §eLv. {N}` | 3초 후 HUD 복귀 |
| 필드보스 등장 | `§c⚔ {보스명} 출현!` | 3초 후 HUD 복귀 |

현재 `GrowthGuiListener.attemptEnhancement()`에서 action bar를 직접 전송하는 부분이 있음. HUD 오버라이드 플래그(`overrideUntil: Map<UUID, Long>`) 도입 필요.

---

## 3. Phase B — 스코어보드 사이드바

### 3.1 설계 기준 (gui_hud_spec.md §7)

```
§6Zenon 서버
§7──────────
§e검사  §7수호검
§7──────────
 §e12,450§7G
 §b84§7개
 §55§7개
§7──────────
§7Lv.§f42  §e73%  §a+3§7포인트
§7IL §f87
§7──────────
§7수도 외곽 평원
```

### 3.2 항목별 데이터 소스

| 항목 | 소스 | 갱신 시점 |
|---|---|---|
| 무기 클래스 | `PlayerDataManager.getWeaponType()` | join / 클래스 변경 시 |
| 각인명 | `IslandTerritoryState.classEngravingId()` | join / 각인 변경 시 |
| 골드 | `PlayerGrowthState.wallet("gold")` | 변경 시 즉시 |
| 강화석 | `wallet("mat_stone_enhance")` | 변경 시 즉시 |
| 큐브 | `wallet("mat_cube")` | 변경 시 즉시 |
| 레벨 | `PlayerData.level()` | 변경 시 즉시 |
| 경험치% | `PlayerData.expPercent()` | 변경 시 즉시 |
| 스탯 포인트 | `PlayerData.unspentPts()` | 0이면 해당 줄 생략 |
| 평균 IL | 장착 슬롯 5종 강화 합산 ÷ 5 × 5 | 강화/장비 교체 시 |
| 현재 위치 | 플레이어 월드/리전 이름 | 구역 이동 시 |

### 3.3 구현 대상 파일

| 파일 | 변경 내용 |
|---|---|
| `scoreboard/ScoreboardService.java` | `refresh(Player)` 실 구현 (Bukkit Scoreboard API) |
| 호출 지점 추가 | `PlayerJoinListener.onJoin()` 이미 호출 중 — 내용만 채우면 됨 |
| 월렛 변경 시점 | `GrowthGuiListener`, `FieldDropListener`, `BossRewardService`에서 골드/재화 변경 후 `scoreboardService.refresh()` 호출 |

---

## 4. Phase C — 스킬 수정

### 4.1 낫 월영회전 이동 방향

**설계 기준 (weapon_skills_v1.md §이동기 거리/방향):**
```
| 낫 | 월영회전 | 측면 (입력 방향) | 2.5블럭 | 0.20초 |
```

"입력 방향" = 플레이어가 누르고 있는 키 기반 이동 벡터.
Bukkit에는 키 입력 직접 조회 API 없음 → `player.getWalkSpeed()` + 이동 벡터 근사 또는 `PlayerMoveEvent` 마지막 이동 벡터로 판단.

**구현 방안:**
```java
// BaseWeaponSkill에 dashInInputDirection() 추가
protected void dashInInputDirection(Player player, double blocks) {
    Vector vel = player.getVelocity().setY(0);
    // 이동 중이면 현재 속도 방향 사용, 정지 중이면 시선 기준 우측
    Vector dir = vel.lengthSquared() > 0.01
        ? vel.normalize()
        : sideways(player);
    player.setVelocity(dir.multiply(blocks * 0.6));
}
```

**대상 파일:** `combat/skills/scythe/ScytheShadowSpinSkill.java`
- `dashSideways(player, 2.5)` → `dashInInputDirection(player, 2.5)`
- `BaseWeaponSkill.java`에 `dashInInputDirection()` 헬퍼 추가

### 4.2 기타 스킬 검토 필요 항목

설계서(`weapon_skills_v1.md`)와 현재 구현 간 검증이 필요한 항목:

| 무기 | 스킬 | 설계 | 현재 상태 |
|---|---|---|---|
| 낫 | 사신베기 | 쿨 3초, arc 3.0 150° | 구현됨 (쿨 4초 — 불일치 확인 필요) |
| 낫 | 그믐참 | 쿨 10초, cone 4블럭 60° | 구현됨 (쿨 8초 — 불일치 확인 필요) |
| 낫 | 처형낫 | 쿨 16초, 체력비 계수 분기 | 구현됨 (체력 30% 분기 확인 필요) |
| 전체 | 스택 상한 | 소모형 3스택, 유지형 6스택 | 각 파일 max 값 확인 필요 |

> 쿨타임 수치는 스킬 파일 직접 확인 후 weapon_skills_v1.md 기준으로 정정.

---

## 5. Phase D — 커맨드 및 GUI 연결

### 5.1 `/메뉴` 연결

**현재:** `case "메뉴" -> stub(player, "메인 메뉴")`
**수정:** `case "메뉴" -> MainHubGui.open(player, ...)`

`MainHubGui.open()`에 필요한 의존성(`fieldStateProvider` 등)을 `PlayerCommandRouter` 생성자에 추가 또는 `MainHubListener`에 `openMainHub(Player)` public 메서드 위임.

**대상 파일:** `command/PlayerCommandRouter.java`, `listener/MainHubListener.java`

### 5.2 1차 시즌 구현 대상 stub 커맨드

| 커맨드 | 구현 여부 | 비고 |
|---|---|---|
| `/메뉴` | **구현 필요** | MainHubGui 연결 |
| `/각인` | **stub 유지** | 1차 시즌 공용각인 제외 |
| `/캐릭터` | **stub 유지** | 캐릭터 GUI §7+ |
| `/영지` | **구현 검토** | TerritoryHubGui 이미 존재 |
| `/영지이동` | **stub 유지** | 영지 이동 GUI §7+ |
| `/작물` | **구현 검토** | FarmGuiListener 존재 |
| `/상점` | **구현 검토** | ShopGui 존재 |
| `/영지설정` | **stub 유지** | §7+ 예정 |
| `/보스정보` | **stub 유지** | §7+ 예정 |
| `/클리어` | **stub 유지** | §7+ 예정 |

### 5.3 HotbarInteractListener

현재 `onInteract()`가 빈 메서드. `MainHubListener`에서 이미 slot 8 COMPASS 클릭을 처리 중이므로 기능적 블로커 아님. 정리 대상.

---

## 6. Phase E — 영지 IridiumSkyblock 연동

### 6.1 작위 승급 시 섬 크기 확장

**설계 기준 (economy_numbers_v2.md §1):**

| 작위 | XZ 한도 |
|---|---|
| 개척지(시작) | 50×50 |
| 기사 | 60×60 |
| 준남작 | 70×70 |
| 남작 | 80×80 |
| 자작 | 90×90 |
| 백작 | 100×100 |
| 후작 | 120×120 |
| 공작 | 150×150 |

**현재 상태:** `TerritoryStatusGuiListener.handleRankUpgrade()`에 TODO 주석만 존재. IridiumSkyblock API JAR 미포함으로 §7+ 연동 예정.

**대상 파일:** `listener/TerritoryStatusGuiListener.java`

---

## 7. Phase F — 보스 인스턴스 스코어보드 (파티 섹션)

**설계 기준 (gui_hud_spec.md §7-2):**

보스 인스턴스(`world_boss`) 진입 시 스코어보드 하단에 파티 섹션 추가:
```
§7──────────
§e파티방 이름  §c♥3
§7포로전사  §a850§7/1,190
§7전우      §c200§7/1,000   ← HP ≤ 30% 빨간색
```

**데스카운트:**
| 인원 | 초기값 |
|---|---|
| 1인 | 3 |
| 2인 | 4 |
| 3인 | 5 |

**구현 조건:** Phase B ScoreboardService 구현 후 연동.

---

## 8. 구현 순서 요약

| 순서 | 작업 | 대상 파일 | 예상 크기 |
|---|---|---|---|
| **A-1** | HealthHudListener 1틱 갱신 + HP/XP 글리프 | `HealthHudListener.java`, `HealthHudFormatter.java` | 중 |
| **A-2** | 쿨타임 글리프 HUD 통합 (`SkillService` action bar 제거) | `SkillService.java`, `HealthHudListener.java` | 소 |
| **A-3** | 스택 게이지 글리프 HUD 통합 | `HealthHudListener.java`, `ResourceTracker.java` | 소 |
| **A-4** | HUD 오버라이드 플래그 (강화 성공/실패 알림) | `HealthHudListener.java`, `GrowthGuiListener.java` | 소 |
| **B-1** | ScoreboardService.refresh() 구현 | `ScoreboardService.java` | 중 |
| **B-2** | 재화 변경 시점에서 refresh() 호출 연결 | 각 listener | 소 |
| **C-1** | 낫 월영회전 입력 방향 dash | `ScytheShadowSpinSkill.java`, `BaseWeaponSkill.java` | 소 |
| **C-2** | 낫 스킬 쿨타임 수치 정정 | `ScytheDeathSlashSkill.java`, `ScytheGrimStrikeSkill.java` | 소 |
| **D-1** | `/메뉴` → MainHubGui 연결 | `PlayerCommandRouter.java` | 소 |
| **D-2** | `/영지`, `/작물`, `/상점` stub → GUI 연결 | `PlayerCommandRouter.java` | 소 |
| **E-1** | IridiumSkyblock 섬 크기 확장 연동 | `TerritoryStatusGuiListener.java` | 미정 (API 의존) |
| **F-1** | 보스 인스턴스 파티 스코어보드 | `ScoreboardService.java` | 중 |

---

## 9. 검증 계획

| 시나리오 | 기대 결과 |
|---|---|
| 접속 직후 가만히 있기 (3초 이상) | HP바 / XP바 사라지지 않고 유지 |
| 낫 클래스로 몹 근처에서 RMB | 이동 방향이 W/A/S/D 입력 방향과 일치 |
| 강화 시도 후 성공 | action bar에 `§a✔ 강화 성공! §e{N}강` 2~3초 표시 후 HUD 복귀 |
| 스킬 사용 후 쿨 진행 | action bar 쿨타임 행에 cd 글리프 바 감소 표시 |
| `/메뉴` 입력 | MainHubGui 54슬롯 열림 |
| 스코어보드 | 우측 사이드바에 골드/강화석/큐브/레벨/IL/위치 항상 표시 |
| 골드 획득 | 스코어보드 골드 수치 즉시 갱신 |

---

## 10. 1차 시즌 미구현 확정 목록 (stub 유지)

아래 항목은 1차 시즌 범위 밖으로 확정. stub 또는 "준비 중" 메시지 유지.

- 도감 / 컬렉션
- 공용 각인 (직업 각인만 1차 시즌)
- 악세서리 장비 슬롯
- 세트 장비 시스템
- 웹 대시보드 UI
- 캐릭터 세부 GUI (장착·치장·스탯 트리)
- 영지 이동 GUI
- 영지 설정 GUI
- 보스 정보 GUI
- 클리어 기록 GUI
- ModelEngine / BetterModel / FMM 보스 모델
- IridiumSkyblock 섬 크기 확장 API 연동 (JAR 미포함)
