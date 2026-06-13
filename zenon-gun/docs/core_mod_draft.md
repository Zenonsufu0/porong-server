# zenongun-core 설계 초안 (보류 — 셀렉터 방향 / 재활용 자산)

> **[STATUS: 보류 — 방향 전환됨]** 이 문서는 **(구) "멀티 게임모드 셀렉터" 방향**(라운드형 배로/서든데스/타르코프)
> 기준의 설계다. zenon-gun의 핵심 방향은 **단일 영속 서바이벌 월드**(마크형 러스트+타르코프)로 전환됐다 → [`concept.md`](concept.md).
>
> **그러나 폐기하지 않고 보존한다:** 여기 `GameMode` 인터페이스·룰셋 레이어·데이터팩 아레나/키트 설계는,
> 훗날 영속월드 코어를 떼어 **분리형 미니게임 모드(타르코프/러스트/배로 단판)로 재활용**할 때 그대로 참고할 자산이다.
>
> 영속월드 **본설계**는 MVP 범위 확정 후 별도 문서로 작성한다(현재 MVP 의도적 보류).
> 아직 코드/빌드 산출물 없음(구상 단계). 착수는 사용자 명시 요청 시에만.

## 0. 한 줄 정의
**총은 만들지 않는다.** 총기 모드(TaCZ: Refabricated)가 만든 총·탄·반동·부착물을 가져다 쓰고,
`zenongun-core`는 **그 위의 "게임 규칙"(매치 상태머신 + 게임모드 룰셋 + 데이터 기반 맵/키트)**만 만든다.

## 1. 책임 경계
```
TaCZ: Refabricated  →  총·탄·반동·부착물·발사·작업대   (가져다 씀, 내부는 안 건드림 / GPLv3)
─────────────────────────────────────────────────────────────────
zenongun-core (우리)  →  "그 위의 게임"
  1. 매치 상태머신     대기 → 카운트다운 → 진행 → 정산 → 종료
  2. 게임모드 룰셋     타르코프 / 배틀로얄 / 서든데스 (교체형)
  3. 플레이어 흐름     로비 입장 → 키트 지급 → 스폰 → 사망/탈출 → 정산
  4. 손실 규칙         사망 시 인벤토리 처리(타르코프=드랍, 서든데스=유지)
  5. 공간 규칙         스폰존·탈출구·자기장(원형 축소)
  6. 봇(스캐브)        filler 스폰 (바닐라 몹 + TaCZ 총 장착)
  7. 셀렉터/로비       "지금 열린 모드 입장" + 운영 명령어
  8. 영속 데이터       타르코프 스태시(개인 보관함) 저장
```
> GPLv3 전염 회피: TaCZ의 **공개 아이템/API만 바깥에서 호출**하고, 내부를 깊게 Mixin하지 않는다.

## 2. 아키텍처 — 공통 코어 + 모드 교체
핵심은 `GameMode` 인터페이스 하나. 모드 3종은 이 인터페이스 구현체일 뿐 → **코어를 안 건드리고 룰만 갈아끼움.**

```java
public interface GameMode {
    String id();                              // "extraction" | "br" | "suddendeath"
    void onMatchStart(Match m);               // 키트 지급, 스폰 배치
    void onTick(Match m);                     // 자기장 축소, 시간 체크
    void onPlayerDeath(Match m, ServerPlayer victim, DamageSource src);  // 손실 규칙
    void onPlayerExtract(Match m, ServerPlayer p);   // 타르코프 탈출(다른 모드는 no-op)
    MatchResult checkEndCondition(Match m);   // 승리/종료 판정
}
```

```
MatchManager (싱글톤)
  ├─ 현재 활성 Match 1개만 운영   ← 세션 로테이션(동접 분산 방지)
  ├─ activeMode: GameMode
  └─ 매 서버 틱마다 activeMode.onTick() 호출

ExtractionMode   implements GameMode   // 사망=장비 드랍, 탈출구 도달=생환+전리품 보존
BattleRoyaleMode implements GameMode   // 자기장 축소, 최후 1인
SuddenDeathMode  implements GameMode   // 짧은 라운드, 리스폰 or 즉사
```

## 3. Fabric 1.21.1 빌딩블록 (쓰는 훅)
전부 Fabric API 표준 이벤트 → 별도 의존 없이 가능 (zenon-mon 빌드환경 재사용).

| 기능 | Fabric 훅 |
|---|---|
| 매치 루프·자기장 | `ServerTickEvents.END_SERVER_TICK` |
| 명령어(`/gun join`, 운영) | `CommandRegistrationCallback` |
| 사망 시 손실 처리 | `ServerLivingEntityEvents.AFTER_DEATH` |
| 킬 집계/데미지 후킹 | `ServerLivingEntityEvents.ALLOW_DAMAGE` |
| 키트 지급 | TaCZ 총 `ItemStack`을 인벤토리에 직접 set |
| 스태시 영속화 | `PersistentState` (월드 저장에 묶임) |
| 봇 스캐브 | 바닐라 `Zombie`/`Husk` 스폰 + 메인핸드 TaCZ 총 |

## 4. 데이터 주도 설계 — 맵/키트는 코드 말고 JSON
맵 추가마다 코드 수정 = 지옥. 아레나·키트를 **데이터팩 JSON으로** 빼서 좌표만 적으면 맵 추가되게.

```jsonc
// data/zenongun/arenas/factory.json  (타르코프 맵 예시)
{
  "mode": "extraction",
  "spawns":   [[120, 65, -40], [118, 65, -45]],
  "extracts": [ { "name": "북문", "box": [[200,64,10],[205,68,15]] } ],
  "loot_tiers": "tier_mid",
  "scav_count": 6
}
```

## 5. 모드별 룰 요약 (초안)
| 모드 | 손실 | 승리/종료 | 특이 |
|---|---|---|---|
| **타르코프식(메인)** | 사망 시 장착 장비 드랍 | 탈출구 도달=생환(전리품 보존) | 스태시 영속, 스캐브 봇, 보험/시장(범위 미정) |
| **배틀로얄** | 매치 한정 | 최후 1인/팀 | 자기장 원형 축소 |
| **서든데스** | 유지(라운드 한정) | 라운드 스코어 | 짧은 리스폰/즉사 데스매치 |

## 6. 미확정 / 결정 필요
- 모드 운영: 세션 로테이션 vs 동시 오픈 vs 단일 모드 우선 출시.
- 타르코프 경제 강도: 스태시/보험/시장 도입 범위.
- 스캐브 봇 구현 수준(바닐라 몹 + 총 vs 기존 봇 모드 활용).
- 시즌 종료 후 스태시 데이터 처리(보존/초기화/이관).
- 자기장·탈출구 등 영역 정의 방식(JSON 좌표 vs 인게임 운영 명령으로 지정).

## 7. MVP 권장 범위 (2주·1인 기준)
1. `MatchManager` + `GameMode` 골격 + 로비/셀렉터 명령어.
2. **타르코프식 1모드 먼저** 완성(키트 지급·사망 드랍·탈출구·스태시).
3. 배틀로얄(자기장)·서든데스는 **경량 추가**.
4. 스캐브 봇은 "바닐라 몹 + TaCZ 총" 최소 구현부터.
> 셋을 동시에 깊게 만들지 말 것 — 타르코프식 우선, 나머지는 룰셋 교체로 가볍게.
