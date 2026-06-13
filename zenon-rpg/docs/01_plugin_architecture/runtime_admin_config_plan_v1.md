# 런타임 운영자 설정 시스템 기획안 v1

> **[STATUS: DRAFT — 기능 기획]** — 플러그인 재배포 없이 인게임 명령어로 몹 스탯·상점을 핫에딧하는 운영 시스템.
> 출처: 사용자 아이디어 (INBOX-010, 2026-06-01). 확정 시 `01_plugin_architecture/CANON.md` + `02_database_api_stats/CANON.md` + decision_log 반영.

---

## 1. 배경 / 목표

**문제:** 상점 물품/가격, 몬스터 HP/DEF/ATK를 바꾸려면 매번 플러그인 jar 재컴파일·재배포가 필요해 불편하다.

**목표:** 운영자가 **인게임 명령어로 즉시** 조정. 45일 시즌 서버 특성상 "실측 → 즉시 너프/버프" 루프(DL-116 실측 보정 계획)와 직결.

**핵심 설계 원칙:**
- **정본 ↔ 런타임 일원화** — DL-116 등 정본 수치가 곧 런타임 시드값. 문서와 실제가 갈라지지 않게.
- **변경 추적 = 패치노트** — 모든 런타임 변경을 감사 로그에 남기고, 그게 디스코드/웹 패치노트 피드가 되게 한다(사용자가 우려한 "일괄 배포가 패치노트 쓰기 편하다"를 자동화로 해소).
- **안전** — 운영자 권한 게이트 + 롤백 + 비정상값 클램프.

---

## 2. 범위

| 포함 | 비포함(현 단계) |
|---|---|
| 몹 스탯(HP/DEF/평타ATK) 런타임 오버라이드 | 보스 **스킬 패턴** 데미지(§4.1-C 제약 참조) |
| 상점 물품 추가/제거/가격 변경 | 드랍 테이블 런타임 편집(별도) |
| 변경 감사 로그 + 패치노트 피드 | 강화 확률·경제 곡선 런타임 편집(별도) |
| 운영자 권한·롤백·값 클램프 | 웹 GUI 편집(INBOX-009 웹 대시보드와 연계 후속) |

---

## 3. 현황 (코드 조사 결과, 2026-06-01)

| 영역 | 현황 | 함의 |
|---|---|---|
| 상점 | `market/ShopGui` — config.yml `shop.{material,block,cosmetic,special}` 로드 + `reloadItems(Plugin)` 보유 | 이미 config 기반. 명령으로 config write+reload 또는 DB 이전이면 핫에딧 가능 |
| 몹 스탯 | MythicMobs YAML(`Damage:`/`Health`)만. 시드 CSV에 없음(`boss_master.csv`는 메타데이터만, HP/DEF/ATK 없음) | 런타임 오버라이드 레이어 신규 필요 |
| 스폰 경로 | `FieldSpawnService` → `mythicSpawner` BiFunction(ZenonRPGPlugin:642~). 람다가 스폰 **Entity** 보유 | **스폰 직후 어트리뷰트 주입 지점 존재** |
| 몹 식별 | scoreboard tag(`zenon_rpg_field_N`/`zenon_rpg_rank_elite`/`zenon_rpg_type_field_boss`) + mobId | 오버라이드 키로 사용 |
| 명령 패턴 | `AdminTogglesCommand` + `AdminTogglesService`(상태 보유 서비스 + 명령) | 신규 명령의 모델 |
| DB 패턴 | `*Ddl`/`*Migration`/`*Repository` 정형 (Auction·Pvp·EconomyFlow 등) | 신규 테이블 정형 따름 |
| 운영 쿼리 | `operations/query` — Discord 어댑터 + `PublicSnapshotQueryService` | 패치노트 피드·INBOX-009 상태 표시 재사용 |

---

## 4. 설계

### 4.1 축 A — 몹 스탯 런타임 오버라이드 레이어

**구조:** ZenonRPG가 스폰 시 DB 오버라이드를 적용(MythicMobs reload 불필요).

```
[DB] mob_stat_override         [스폰] MythicMobSpawnEvent 리스너 (전 경로 커버)
  mob_key  PK                    → 스폰 mobId(internalName)로 override 조회
  max_hp                         → 1틱 지연 후 Entity 어트리뷰트 적용:
  def                                · MAX_HEALTH = max_hp
  atk  (= 평타)                       · ATTACK_DAMAGE = atk
  (updated_by, updated_at)        → def는 PlayerDefenseListener가 피격 시 참조(2단계)
```

> **구현 메모(DL-117):** 주입 지점은 `mythicSpawner` 람다가 아니라 **`MythicMobSpawnEvent` 리스너**(reflection 격리). 필드보스가 람다를 거치지 않고 MythicMobs 네이티브로 스폰되기 때문 — 리스너는 동적 스폰·보스룸·네이티브 명령/스포너 등 **모든 경로**를 커버한다.

- **mob_key**: `field1_normal` / `field1_elite` / `field1_boss` … 또는 MythicMobs mobId 직접.
- **시드**: `mob_stat_override` 시드 CSV에 **DL-116 정본값**을 초기 적재 → 정본 = 런타임 초기상태.

**티어별 적용 가능성 (중요 — 정직하게):**

| 대상 | 런타임 변경 | 방법 |
|---|---|---|
| 몹 HP (전체) | ✅ 즉시 | `GENERIC_MAX_HEALTH` 어트리뷰트 |
| 일반/정예몹 평타 ATK | ✅ 즉시 | `GENERIC_ATTACK_DAMAGE` 어트리뷰트 |
| 보스 기본공격(평타) | ✅ 즉시 | 동일 |
| 몹 DEF(피격 경감) | ✅ 즉시 | `PlayerDefenseListener`가 몹별 DEF override 맵 참조하도록 확장 |
| **보스 일반/강 패턴** | ⚠️ **불가(현 구조)** | MythicMobs 스킬 `damage{a=N}` = YAML 상수. §4.1-C |

**§4.1-C 보스 패턴 데미지 제약 및 대안:**
- 보스의 강타/폭발 등은 MythicMobs 스킬에서 발동 → 데미지가 YAML에 박힘. 런타임 어트리뷰트로 못 바꿈.
- 대안 (택1, 후속 결정):
  - **(C-1) 글로벌 배율 placeholder** — MythicMobs 스킬을 `damage{a=<배율변수>×기본}` 형태로 바꾸고, ZenonRPG가 PlaceholderAPI/Mythic 변수로 보스별 패턴 배율을 노출. 런타임 명령으로 그 변수만 조정. (가장 유연)
  - **(C-2) YAML 배포 유지** — 패턴 데미지는 DL-116 §6대로 YAML 배포로 두고, 런타임은 HP+평타만. (MVP 단순)
- **권고:** MVP는 (C-2). 패턴 배율 핫에딧 수요가 확인되면 (C-1) 도입.

**명령:** `/rpg-mobstat <mob_key> <hp|def|atk> <value>` · `/rpg-mobstat list` · `/rpg-mobstat reset <mob_key>`

### 4.2 축 B — 상점 런타임 편집

이미 config 기반이라 두 경로:

- **(B-1) config write + reload** — 명령이 config.yml의 `shop.*`를 수정하고 `ShopGui.reloadItems()` 호출. 최소 변경.
- **(B-2) DB 이전** — `shop_item` 테이블로 이전, 명령이 DB write. 감사 로그·다중 서버·웹 편집과 정합. (권장 — 축 C와 일원화)

**명령:** `/rpg-shop add <category> <item_id> <price> [amount]` · `/rpg-shop setprice <item_id> <price>` · `/rpg-shop remove <item_id>` · `/rpg-shop reload`

### 4.3 축 C — 변경 감사 로그 → 패치노트 피드

```
[DB] config_change_log
  id PK / changed_at / changed_by / domain(mob|shop) / target_key
  field / old_value / new_value / note
```

- 모든 A·B 명령이 이 테이블에 기록.
- `operations/query` 파이프라인(Discord 어댑터) 재사용 → 디스코드 `#패치노트` 채널 자동 포스트 + 웹 변경 이력 페이지.
- **효과:** "런타임 편의 ↔ 패치노트 추적" 트레이드오프를 자동화로 동시 충족.

### 4.4 안전 / 권한

- 운영자 permission(`zenon.rpg.admin.config`) 또는 op 게이트.
- 값 클램프: HP/ATK 음수·과대값 거부(예: ATK 0~1000, HP 1~100000).
- 롤백: `config_change_log` 기반 `/rpg-config undo <id>` (old_value 복원).
- 적용 범위: 오버라이드는 **신규 스폰**부터 반영(기존 개체 미소급) — 명시.

---

## 5. 단계별 우선순위

| 단계 | 내용 | 의존 |
|---|---|---|
| **1 (MVP)** ✅ **완료 [DL-117]** | `mob_stat_override`+`config_change_log` 테이블 + DL-116 ATK 시드 + `MythicMobSpawnEvent` 리스너(전 스폰 경로, HP·평타 적용) + `/rpg-mobstat` | DB 패턴 |
| 2 | `PlayerDefenseListener` 몹별 DEF override 연동 | 1 |
| 3 | 상점 DB 이전(B-2) + `/rpg-shop` + 감사 로그 | DB 패턴 |
| 4 | 패치노트 디스코드/웹 피드(축 C 파이프라인) | operations/query |
| 5 | 보스 패턴 배율 placeholder(C-1) — 수요 확인 후 | MythicMobs 연동 |
| 6 | 웹 GUI 편집(INBOX-009 대시보드 연계) | 웹 |

---

## 6. 오픈 질문

1. **mob_key 입도** — 필드/등급 단위(`field1_elite`)인가, MythicMobs mobId 단위인가, 개별 몹 종 단위인가? (정본표 §3은 필드×등급 단위 → 그 입도 권장)
2. **보스 패턴 배율** — C-1(placeholder) vs C-2(YAML 유지). MVP는 C-2.
3. **상점 경로** — B-1(config) vs B-2(DB). 축 C 정합상 B-2 권장.
4. **DL-116 §6 YAML 적용을 건너뛰나?** — 축 A 시드가 정본값을 적재하면 일반/정예/HP/평타는 YAML 적용 불필요. 단 보스 패턴(강/일반)은 C-2 선택 시 여전히 YAML 적용 필요.
5. **롤백 입도** — 단건 undo만? 시점 스냅샷 복원까지?

---

## 7. INBOX-009(웹/디코 실시간 상태)와의 접점

- 두 기능 모두 `operations/query`(Discord 어댑터 + PublicSnapshotQueryService) 파이프라인을 공유.
- 패치노트 피드(축 C)와 서버 상태 표시(INBOX-009)는 같은 봇·웹 채널 인프라 → **묶어서 구현 시 효율적**.

---

## 관련

- `INBOX-010`(원안), `INBOX-009`(상태 표시)
- `06_fields_bosses/mob_attack_stats_v1.md`(DL-116 — 몹 스탯 시드 원천)
- 구현 참조: `field/FieldSpawnService`, `command/AdminTogglesCommand`+`admin/AdminTogglesService`, `market/ShopGui`, `common/db/*Ddl·*Migration`, `operations/query/*`
</content>
