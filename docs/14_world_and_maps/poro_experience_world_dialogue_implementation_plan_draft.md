# 체험 월드 NPC 대화 구현 계획 드래프트 (문지기 · 길잡이)

> 문서 버전: 2026-04-19 초안 (implementation-reviewer)
> 상위 문서: `./poro_experience_world_level_design_draft.md` (권장안 B)
> 관련 시스템: EmpireRPG Citizens 동기화 파이프라인 + BetonQuest 대화 브리지
> 관련 약관 플로우: `../00_index_and_execution/poro_user_agreement_v1_draft.md` §UX 플로우, `../00_index_and_execution/poro_discord_first_agreement_flow_draft.md`

---

## ⚠️ 상태 주석 — 2026-04-19 사용자 결정: C안 채택 (1차 범위 축소)

본 드래프트는 BetonQuest 재활용 전제로 작성됐으나, 사용자 결정으로 **1차 범위에서는 채택하지 않습니다**. 마스터플래닝의 "BetonQuest 본 시즌 미사용" 원칙 유지.

- **1차 적용 범위**: 체험 월드 NPC 5명(문지기·길잡이·관찰자·수도 속삭임·세계수 목소리) 모두 **대화 트리 없이 단순 click-handler**로 구현. "한 문장 메시지 + (문지기만) `player.teleport()` + (길잡이만) 디스코드 초대 링크 발송" 수준. 상태 플래그 연동·분기·BetonQuest 패키지 **전부 미사용**.
- **2차 승격 계획**: 추후 EmpireRPG 커스텀 대화 UI 에픽(선택지 B)이 도입되는 시점에 본 드래프트의 §3·§4·§6 설계를 그 엔진 위로 포팅.
- **본 드래프트 취급**: 2차 B안 참고 자료로 보관. §5 Java 예시·§6 BetonQuest 스크립트 초안·§11 PR 분해는 1차에서 미사용. §1 목표·§4 상태 플래그 매트릭스는 2차 설계 시 재활용 가능.

---

## 0. 선행 조사 결과 — 용어 정정 (중요)

요청의 "`DialogueRegistry` 포맷"이라는 표현을 그대로 받아 기획을 확정하면 **실제 코드 구조와 충돌**한다. 현재 `custom-plugins/empire-rpg` 에는 `DialogueRegistry` 라는 이름의 클래스가 존재하지 않는다. 실제로 가동 중인 대화 파이프라인은 다음 조합이다.

- `CitizensNpcSeed` (record) — NPC 시드에 `beton_conversation_id` 필드가 있다.
- `MetadataBetonQuestConversationHook` — 시드의 `beton_conversation_id` 값을 Citizens NPC 메타데이터 `empire.beton_conversation_id` 로 박아 넣는다.
- `CitizensBetonQuestConversationListener` — 플레이어가 Citizens NPC 를 우클릭하면 메타데이터에 저장된 이벤트 ID 로 `q event <player> <event_id>` 콘솔 명령을 디스패치해 BetonQuest 대화를 연다.
- 대화 본문(대사, 분기, 조건, 응답)은 **BetonQuest yml 패키지** 쪽에 정의한다. 자바 코드가 분기 트리를 들고 있지 않다.

즉 "EmpireRPG 의 대화 레지스트리에 대화 트리를 등록" 하는 설계는 현재 범위를 벗어나며, 1차 버전은 **"Citizens 시드 + BetonQuest 패키지"** 조합을 재활용하는 방향으로 간다. 본 문서 이후부터 "DialogueRegistry" 라는 표현을 쓰면, 그것은 `CitizensNpcSeed` + `MetadataBetonQuestConversationHook` + BetonQuest 대화 정의 3세트를 묶은 논리적 레이어를 가리키는 것으로 해석한다.

만약 사용자가 "자바 측에 독자적인 DialogueRegistry 클래스를 두고 BetonQuest 의존을 끊는다" 로 가고 싶다면, 그것은 별도 에픽(대화 엔진 자체 구현)으로 분리되어야 한다. 현재 범위에서는 권장하지 않는다. 이 항목은 오픈 질문으로 남긴다.

---

## 1. 목표

1. 체험 월드 권장안 B 의 NPC 5명 중 **길잡이 · 문지기 2명의 대화**를 실제 플레이 가능한 수준으로 구현한다.
2. 플레이어의 디스코드 연동 / OTP 상태에 따라 **세 갈래(미연동 / OTP 대기 / OTP 성공)** 분기를 제공한다.
3. 메인 월드 이관 트리거 (`player.teleport()`) 는 **문지기 NPC 에 한해** 발생한다.
4. 기존 `CitizensNpcGateway` · `CitizensNpcSyncService` · `CitizensBetonQuestConversationListener` 를 재활용하고 **새 Gateway 를 만들지 않는다**.
5. BetonQuest 의존이 추후 변경되더라도 대화 정의만 교체하면 되도록, 자바 코드에 대사 하드코딩은 하지 않는다.

---

## 2. 선행 조건

구현 착수 전 반드시 존재해야 할 요소.

- **체험 월드 맵** 확정 (`experience_world` 스폰 + 남/서 복도 NPC 스폰 좌표). `poro_experience_world_level_design_draft.md` §2 참조.
- **플래그 저장소 v0.1** 의 최소 읽기 API. 구체적으로 다음 두 키에 대한 동기 read 경로가 필요하다.
  - `player.agreement.v1_agreed` (bool)
  - `player_discord_link.is_verified` (bool)
  - 플래그 저장소가 아직 없으면 1차 버전은 **폴백**으로 LuckPerms 임시 노드(`poro.agreement.v1`, `poro.discord.verified`) 로 대체한다 (§6 참조).
- **`/confirm <OTP>` 명령어** 가 채팅 입력을 받아 `is_verified` 를 true 로 갱신하는 경로. 이 경로 자체는 본 문서 범위 밖이지만 훅이 발화되는 타이밍은 §4 에 명시.
- **BetonQuest 플러그인** 가동. `plugin.yml` 의 `softdepend` 에 이미 선언되어 있어 추가 설정은 필요 없음.
- **Citizens 플러그인** 가동 및 `npc_spawn_seed.csv` 동기화 정상 동작 확인 (기존 `NpcSyncBootstrap` 이 수행).
- `experience_world` 용 `region_code` · `town_id` 마스터 등록. 현재 마스터 레지스트리에는 수도 · 남부만 있어 체험 월드 값(`exp_world` · `exp_plaza` 잠정) 추가 필요.

---

## 3. 작업 분해

### 3.1 시드 확장 (CSV · 마스터 레지스트리)

- `npc_spawn_seed.csv` 에 2행 추가.
  - `npc_exp_gatekeeper` (문지기)
  - `npc_exp_guide` (길잡이)
- 각 행의 `beton_conversation_id` 는 BetonQuest 패키지에 선언할 이벤트 ID 와 1:1 매칭.
  - `conv_exp_gatekeeper`
  - `conv_exp_guide`
- `region_code=exp_world`, `town_id=exp_plaza`, `world_name=experience_world`.
- `npc_master_id` 는 마스터 레지스트리에 선등록 필요. 레오니드 · 영지 관리관과 동일 패턴.
- 좌표는 체험 월드 맵 확정본을 따른다 (남쪽 길잡이, 서쪽 문지기 — 초안 문서 §2 NPC 배치도).

### 3.2 BetonQuest 대화 패키지 정의

패키지 위치 잠정: `plugins/BetonQuest/QuestPackages/poro/experience_world/` 에 아래 두 파일을 둔다.

- `conversations/guide.yml`
- `conversations/gatekeeper.yml`

두 대화 모두 **플래그 조건으로 진입 대사를 고르는 구조** 로 설계한다 (§4 분기 설계 표 참조). 조건 · 이벤트 · 오브젝티브 선언은 package root 의 `conditions.yml` · `events.yml` 에 공용으로 모은다.

### 3.3 플래그 ↔ BetonQuest 조건 브리지

BetonQuest 는 LuckPerms 노드와 점수(`point`)를 조건식으로 쓸 수 있다. 포로 플래그 저장소 접근을 위해서는 둘 중 하나가 필요.

- **A안 (권장, 1차)**: EmpireRPG 측이 플래그 변경 시점에 `bq point <player> poro_agreement_v1 1 +` · `bq point <player> poro_discord_verified 1 +` 를 동기 호출한다. BetonQuest 쪽 조건은 `point poro_discord_verified 1` 로 체크.
- **B안 (장기)**: BetonQuest 용 커스텀 Condition Integration 을 `empire-rpg` 에 신규 추가. 구현 비용이 높고, 1차 범위 초과.

1차 버전은 A안 으로 고정한다. 브리지 호출 훅 위치는 §3.5.

### 3.4 Citizens → BetonQuest 우클릭 브리지 확장

현재 `CitizensBetonQuestConversationListener` 는 **seedId 가 `npc_capital_leonid_main` 인 경우에만** 브리지를 작동시키는 하드코딩이 있다(line 63). 체험 월드 NPC 두 개를 추가로 허용하도록 **화이트리스트화** 하는 최소 리팩터링이 필요하다.

- 현행: 한 줄 if 체크.
- 개선: 시드의 `role_type` 을 읽어 `"dialogue_npc"` 또는 `"experience_npc"` 이면 브리지 허용. seed CSV `role_type` 을 `exp_gatekeeper` / `exp_guide` 로 지정.
- 리팩터링 범위를 **화이트리스트 Set 추가** 수준으로 국한한다. 대규모 정리는 하지 않는다.

### 3.5 플래그 갱신 시점 — BetonQuest 포인트 동기화 훅

- 약관 동의 완료 핸들러(디스코드 봇 콜백 수신 측) 직후 → `bq point <player> poro_agreement_v1 1 +`
- `/confirm <OTP>` 성공 핸들러 직후 → `bq point <player> poro_discord_verified 1 +`
- 메인 월드 이관 직전에 위 포인트를 최종 확인 (실패 시 이관 거부).

훅 위치는 이미 설계되어 있는 디스코드 봇 콜백 처리 모듈 또는 `/confirm` 명령 핸들러이며, 본 문서에서는 "발화 타이밍" 만 정의한다.

### 3.6 이관 연출 이벤트

- BetonQuest `event` 정의로 `exp_teleport_main` 선언.
- 내부 구현: `folder` 이벤트로 `notify` (페이드 인 연출) → 2s delay → `teleport main_world_spawn` 순서.
- 문지기 대화 성공 분기 종단에서 이 event 를 호출한다.
- 페이드 연출은 BetonQuest 기본 notify + title 조합으로 충분. 셰이더는 1차 범위 밖.

### 3.7 상시 안내 (액션바)

- 체험 월드 진입 시 OTP 대기 여부에 따라 액션바를 상시 표시한다.
- 구현은 **Listener** 1개: `PlayerChangedWorldEvent` 에서 world 가 `experience_world` 이면 3초 주기 액션바 task 등록, 떠날 때 해제.
- 메시지는 포인트 상태에 따라 3종. 액션바 메시지는 `messages.yml` 에서 읽는다 (하드코딩 금지).

---

## 4. 상태 플래그 ↔ 대화 분기 설계

세 가지 플레이어 상태 정의 (A · B · C).

| 상태 | poro_agreement_v1 | poro_discord_verified | 의미 |
| --- | --- | --- | --- |
| A | 0 | 0 | 디스코드 연동 전, 갓 스폰 |
| B | 1 | 0 | 약관 동의 완료, OTP 입력 전 |
| C | 1 | 1 | OTP 성공, 메인 이관 대기 |

분기 매트릭스:

**길잡이 NPC (`conv_exp_guide`)**

- A → 디스코드 초대 Book GUI 제공, "저 너머로 가려면 목소리를 등록해야 해" 톤.
- B → "이미 봇이 너에게 코드를 보냈을 거다. 서쪽 문지기에게 가라" 로 안내 방향만 전환. 초대 링크 재제공은 선택지에 숨김.
- C → "이미 길은 열렸다. 문지기가 기다린다" 1줄 + 대화 종료.

**문지기 NPC (`conv_exp_gatekeeper`)**

- A → "아직 저쪽 세계와 말을 트지 않았구나. 먼저 남쪽 길잡이에게 가라" 로 **역방향 유도**. 이관 이벤트 호출 금지.
- B → "`/confirm` 다음에 여섯 자리를 말해봐" 로 OTP 입력 안내. 대화 종료 후 채팅 입력 대기.
- C → "너의 자리는 준비됐다" → 페이드 2초 → 메인 월드 이관.

대화 트리 무한 루프 방지 장치 (§7 리스크 항목과 연동):

- 모든 선택지는 반드시 "대화 종료" 옵션 또는 조건부 종료(`event: stop`) 로 빠져나갈 수 있어야 한다.
- 동일 NPC 재상호작용 시 상태 C 가 true 이면 이관 이벤트가 **한 번만** 발화하도록 포인트 `poro_exp_teleport_fired` 로 가드.

---

## 5. Java 코드 예시 (발췌 수준)

### 5.1 화이트리스트 확장 (CitizensBetonQuestConversationListener)

```java
// 기존 line 63 하드코딩 교체 예시. 5줄 이내.
private static final Set<String> DIALOGUE_SEEDS = Set.of(
        "npc_capital_leonid_main",
        "npc_exp_gatekeeper",
        "npc_exp_guide"
);
// ...
if (!DIALOGUE_SEEDS.contains(seedId == null ? "" : seedId.toLowerCase(Locale.ROOT))) {
    return;
}
```

이 이상의 리팩터링은 1차 범위 밖이다.

### 5.2 포인트 동기화 훅 (의사코드)

```java
// /confirm <OTP> 성공 핸들러 내부. 실제 파일은 본 문서 범위 밖.
public void onOtpVerified(Player player) {
    flagStore.setBool(player.getUniqueId(), "player_discord_link.is_verified", true);
    Bukkit.dispatchCommand(
        Bukkit.getConsoleSender(),
        "bq point " + player.getName() + " poro_discord_verified 1 +"
    );
}
```

### 5.3 NPC 시드 CSV (2행 추가 예시)

```
npc_exp_guide,npc_exp_guide,exp_world,exp_plaza,experience_world,0.0,65.0,12.0,180.0,0.0,PLAYER,길잡이,name,ExpGuideSkin,exp_guide,exp_guide_profile,NONE,conv_exp_guide,true,true,true
npc_exp_gatekeeper,npc_exp_gatekeeper,exp_world,exp_plaza,experience_world,-12.0,65.0,0.0,90.0,0.0,PLAYER,문지기,name,ExpGateKeeperSkin,exp_gatekeeper,exp_gatekeeper_profile,NONE,conv_exp_gatekeeper,true,true,true
```

좌표는 맵 확정본에서 교체. 스킨 이름은 운영 자산 명명 규약을 따른다.

---

## 6. BetonQuest 대화 스크립트 초안

### 6.1 `conv_exp_guide` (길잡이)

```yaml
conversations:
  exp_guide:
    quester: "&e길잡이"
    first: "start_a,start_b,start_c"
    NPC_options:
      start_a:
        text: "여기서 끝은 아니야. 저 너머로 가려면 먼저 너의 목소리를 등록해야 해."
        conditions: "!agreed_v1"
        pointer: "ask_invite"
      start_b:
        text: "이미 봇이 너에게 코드를 보냈을 거다. 서쪽 문지기에게 가라."
        conditions: "agreed_v1,!verified"
        pointer: "guide_gate"
      start_c:
        text: "이미 길은 열렸다. 문지기가 기다린다."
        conditions: "verified"
      ask_invite:
        text: "아래 초대 링크로 디스코드 서버에 들어가. 봇이 너를 맞이할 거다."
        pointer: "open_invite,skip"
      guide_gate:
        text: "코드가 없다면 다시 봇을 불러라. `/resend` 로 재발급된다."
    player_options:
      open_invite:
        text: "(디스코드 초대 링크 열기)"
        event: "give_invite_book"
      skip:
        text: "나중에."
```

### 6.2 `conv_exp_gatekeeper` (문지기)

```yaml
conversations:
  exp_gatekeeper:
    quester: "&b문지기"
    first: "need_agreement,need_otp,ready"
    NPC_options:
      need_agreement:
        text: "아직 저쪽 세계와 말을 트지 않았구나. 먼저 남쪽 길잡이에게 가라."
        conditions: "!agreed_v1"
      need_otp:
        text: "코드를 가져왔나? `/confirm` 다음에 여섯 자리를 말해봐."
        conditions: "agreed_v1,!verified"
      ready:
        text: "좋아. 너의 자리는 준비됐다."
        conditions: "verified,!teleport_fired"
        event: "exp_teleport_main,mark_teleport_fired"
      already_fired:
        text: "길은 이미 열렸다. 잠시 뒤 다시 서게 될 것이다."
        conditions: "teleport_fired"
```

공용 조건 (`conditions.yml`):

```yaml
conditions:
  agreed_v1: "point poro_agreement_v1 1"
  verified: "point poro_discord_verified 1"
  teleport_fired: "point poro_exp_teleport_fired 1"
```

공용 이벤트 (`events.yml`):

```yaml
events:
  give_invite_book: "give exp_discord_invite_book"
  exp_teleport_main: "folder fade_notify,wait_2s,tp_main period:0"
  mark_teleport_fired: "point poro_exp_teleport_fired 1 +"
  fade_notify: "notify io:title text:메인_월드로_이동합니다"
  wait_2s: "folder dummy period:2"
  tp_main: "teleport main_world_spawn"
```

대사 문구는 초안 수준이며 최종 QA 시점에 운영·시나리오 작가 리뷰 필요.

---

## 7. 기술 리스크

- **Citizens 플러그인 API 변경**: `ReflectionCitizensNpcGateway` 가 리플렉션 기반이라 Citizens 메이저 업데이트 시 단번에 깨질 수 있다. 완화책은 Citizens 버전 핀 + 메인 테스트 시나리오에 "Citizens 업데이트 후 스모크" 추가. 본 PR 에서 해결하지 않는다.
- **BetonQuest 의존**: 현재 `softdepend` 라 BetonQuest 가 비활성 상태면 브리지 리스너 자체가 등록되지 않는다(기존 부트스트랩 line 63 로그). 체험 월드에서는 BetonQuest 를 필수 의존으로 간주하고, 미존재 시 운영 경고를 강화한다.
- **플래그 저장소 v0.1 미완**: 동의 · OTP 플래그 기록 대상이 없으면 BetonQuest 포인트 동기화 훅을 호출할 주체가 없다. 플래그 저장소 완성 전에는 §6 스크립트가 항상 "미동의(A)" 분기로만 들어가는 폴백 상태가 된다. 이 경우 1차 버전도 "A 상태 대화 + Book GUI + 콘솔 안내" 까지만 닫고, B/C 분기는 후속 PR 에서 활성화한다.
- **무한 루프 가능성**: 대화 중 이관 이벤트가 발화되는 순간 플레이어가 월드 전환되지만, `teleport_fired` 포인트 가드가 없으면 페이드 직후 중복 상호작용으로 재호출 위험. §4 종단 포인트 가드로 차단. **테스트 필수.**
- **`/confirm` 발화와 문지기 대화의 타이밍 경합**: 플레이어가 문지기와 대화 중(BetonQuest 대화 세션 오픈 상태)에 `/confirm` 을 입력하면 BetonQuest 내부 lock 으로 포인트가 즉시 반영되지 않을 수 있다. UX 가이드로 "대화를 닫고 채팅창에 입력" 을 명시하고, BetonQuest 측 `first:` 재평가는 "대화를 닫았다가 다시 열 때" 시점으로 처리한다.
- **초대 링크 Book 아이템 드랍/보관**: 체험 월드는 `item-drop: deny`, `item-pickup: deny` 라 Book 지급 시점에 인벤토리 슬롯이 꽉 차면 손실된다. `give` 이벤트 대신 커스텀 "책 보여주기" 이벤트가 필요할 수 있음. 1차는 인벤토리가 비었다는 전제(체험 월드 스폰 시 강제 clear) 로 단순화하고, 오픈 질문으로 남긴다.
- **경기룰 충돌**: `experience_world` 는 `mobSpawning=false` 이지만 Citizens NPC 는 게임룰을 우회한다. 정상 동작. 다만 `EntityType.PLAYER` 가 아닌 엔티티타입 선택 시 일부 AI 가 떠날 수 있으므로 PLAYER 유지 권장.

---

## 8. 테스트 포인트

### 8.1 상태 분기 3종 검증

- T1: A 상태 플레이어 → 길잡이 상호작용 → "목소리를 등록해야 해" 대사 출력 → Book GUI 수령.
- T2: B 상태 플레이어 → 길잡이 상호작용 → "서쪽 문지기에게 가라" 분기 출력. 초대 Book 재지급 없음.
- T3: C 상태 플레이어 → 문지기 상호작용 → 2초 후 메인 월드 스폰 좌표로 순간이동.

### 8.2 방지 검증

- T4: A 상태 플레이어가 문지기에게 접근 → "남쪽 길잡이에게 가라" 만 뜨고 이관 발화 안 함.
- T5: B 상태에서 `/confirm` 성공 순간 문지기 대화창이 열려 있었다 → 대화창 닫고 재개 시 C 분기로 진입.
- T6: C 상태 문지기 상호작용 후 `teleport_fired` 플래그가 true → 재방문 시 "길은 이미 열렸다" 로만 분기, 이관 이벤트 미발화.

### 8.3 운영·에러 시나리오

- T7: BetonQuest 비활성 상태에서 서버 부팅 → 경고 로그 확인, 체험 월드 진입 자체는 허용되나 NPC 우클릭 시 조용히 무시(기존 리스너 조기 리턴 동작 그대로).
- T8: `npc_spawn_seed.csv` 에서 `beton_conversation_id` 오타 → 동기화 성공, 우클릭 시 로그에 경고만.
- T9: 체험 월드 TPS 모니터링 — NPC 2명 추가 후 TPS 영향 5% 이내.

### 8.4 재진입

- T10: 메인 월드 이관 직후 `/experience` 로 재진입 → 문지기가 C 상태여도 "이미 열렸다" 분기로만 응답. 이관 이벤트 중복 금지.

---

## 9. 1차 버전 권장안 (MVP)

1차에 **꼭** 들어가야 하는 것.

- CSV 시드 2행 (길잡이 · 문지기) 추가 + 마스터 레지스트리 region/town 마스터 등록.
- BetonQuest 대화 패키지 2개 (`conv_exp_guide`, `conv_exp_gatekeeper`) + 공용 conditions/events.
- `CitizensBetonQuestConversationListener` 화이트리스트 확장 (3~5줄 수정).
- 메인 월드 이관 이벤트 (`exp_teleport_main`).
- 상태 C 의 `teleport_fired` 가드 포인트.
- 테스트 시나리오 T1 · T3 · T4 · T6 통과.

1차에 **필수는 아니지만 같이 들어가면 좋은 것**.

- 액션바 상시 안내 (§3.7).
- 초대 Book GUI 지급 이벤트 (§6.1 `give_invite_book`).
- T2 · T5 테스트 시나리오.

---

## 10. 나중으로 미뤄도 되는 것

- **자바 측 독립 대화 엔진 (가칭 `DialogueRegistry`)**: BetonQuest 의존 제거. 별도 에픽으로 분리. 1차 범위에서는 BetonQuest 재활용.
- **QR 코드 지도 아이템**: 리소스팩 의존. 레벨 디자인 초안 §오픈 질문에 이미 보류됨.
- **v1.1 재동의 강등 복귀 분기**: 문지기 NPC 대사 재사용 가능하지만, 상태 분기 매트릭스가 한 차원 늘어난다. 본 문서 분기 A/B/C 위에 "재동의 대기" 차원 추가 시점에 재설계.
- **관찰자 · 수도 속삭임 · 세계수 NPC 3종 대화**: 톤 장식용이라 우선순위 낮음. 본 문서는 기능 경로 상의 길잡이 · 문지기만 커버한다.
- **대화 분석 로깅 (어떤 분기를 얼마나 탔는지)**: 2차 메트릭스 연동 시점에.
- **문지기 NPC 의 OTP 실패 재발급 안내 자동화**: 봇 `/resend` 연동. 1차는 "봇을 다시 불러라" 대사 안내만.

---

## 11. PR 분해

총 4개 PR. 각 PR 은 독립적으로 리뷰 가능한 크기.

### PR-1: NPC 시드 · 마스터 레지스트리 확장

- `npc_spawn_seed.csv` 2행 추가.
- `region_master` · `town_master` 에 `exp_world` · `exp_plaza` 추가.
- 유닛 테스트: `CitizensNpcSyncSampleTest` 에 2 NPC 등록 케이스 확장.
- 추정: **0.5 man-day**.

### PR-2: Citizens 브리지 화이트리스트 확장

- `CitizensBetonQuestConversationListener` line 63 하드코딩을 `DIALOGUE_SEEDS` Set 으로 교체.
- 유닛 테스트: seedId 가 3종 중 하나일 때 브리지 허용, 그 외엔 무시.
- 추정: **0.5 man-day**.

### PR-3: BetonQuest 대화 패키지 (길잡이 · 문지기)

- `QuestPackages/poro/experience_world/` 하위 yml 3개 (conversations/guide.yml, conversations/gatekeeper.yml, 공용 conditions/events).
- Book GUI 용 아이템 정의 (`exp_discord_invite_book`).
- 플래그 ↔ 포인트 동기화 훅 설계서 (구현은 PR-4 에서).
- 수동 테스트: 샌드박스 서버에서 상태 A/B/C 별 대화 수동 분기 확인.
- 추정: **1.5 man-day**.

### PR-4: 플래그 연동 훅 + 통합 테스트

- 약관 동의 완료 / `/confirm` 성공 지점에서 BetonQuest `point` 동기화 호출 추가.
- `teleport_fired` 가드 포인트 운용.
- 액션바 상시 안내 Listener (§3.7).
- 통합 테스트 시나리오 T1~T6 자동화 또는 수동 체크리스트.
- 추정: **2.0 man-day**.

**총합: 4 PR · 4.5 man-day.** 병렬화 가능 부분 (PR-1 과 PR-3) 을 고려하면 순수 작업일 3.5 man-day 수준.

---

## 12. 오픈 질문

- `DialogueRegistry` 를 실제로 신설한다 (BetonQuest 의존 제거) 는 계획이 중장기 로드맵에 있는가? 있다면 본 문서 §10 을 에픽화해 우선순위 확정 필요.
- 플래그 저장소 v0.1 의 타깃 완료일은? PR-4 의 실효 테스트는 v0.1 완료에 종속된다.
- `experience_world` 의 `region_code` · `town_id` 공식 값은 본 문서 잠정값(`exp_world`, `exp_plaza`) 을 확정해도 되는가, 별도 네이밍 규약이 있는가?
- 체험 월드 스폰 시 **인벤토리 강제 clear** 정책 여부 — 초대 Book 지급이 인벤토리 가득참 상황과 충돌할 수 있음.
- 문지기 NPC 대사 톤 — 세계수·관찰자 대역 NPC 톤과 분리해 "중립 관리관" 으로 유지할지, "관찰자 변주체" 로 엮을지. 초안 문서 §핵심규칙 "관찰자 프롤로그 대역 분리" 와 충돌 여부 확인 필요.

---

## 상위 문서 참조

- 체험 월드 레벨 디자인: `./poro_experience_world_level_design_draft.md`
- 약관 v1 UX: `../00_index_and_execution/poro_user_agreement_v1_draft.md`
- 디스코드 봇 파이프라인: `../00_index_and_execution/poro_discord_first_agreement_flow_draft.md`
- Citizens 동기화 코드 (참조): `custom-plugins/empire-rpg/src/main/java/com/poro/empire/npc/citizens/`
