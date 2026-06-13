# zenongun-core 구현 계획서 (2026-06-10~, 작성 중)

## 0. 🔑 작성 원칙 (최우선)
- **기획서 = 단일 진실:** [`concept.md`](concept.md) · `design/`(economy·base_raid·survival·world·finale) · [`launch_plan.md`](launch_plan.md) · [`ops_policy.md`](ops_policy.md). 구현은 **설계를 그대로 따른다.**
- **🚫 설계에 없는 동작·수치·정책은 임의 결정 금지 → 사용자 질문**(이 문서에 ❓로 표시·누적). 추측으로 채우지 않는다.
- 설계 간 충돌·누락 발견 시 **보고**(임의 해소 X).
- 구현은 **MVP 순서**(M0→M3, launch_plan §3) 따라 점진.

## 1. 기술 스택
- **Forge 1.20.1** (dev 서버 47.4.10) / **Java 17 target**(빌드, 확정 — 1.20.1 표준, dev 구동은 Java 21 호환).
- Gradle + ForgeGradle, 단일 모드 **zenongun-core**(포트폴리오 묵직 OK).
- **의존 모드(API/이벤트 hook):** TaCZ(총·탄·청사진)·OPaC(권한·파티·문)·First Aid(부위 HP)·Lost Cities(POI 상자)·Undead Nights/Zombies B&B(좀비)·Tactical 3D Armor.

## 2. 데이터 영속 (확정 2026-06-10)
- **🔑 분리 구조:** ①**게임 상태(코어·기지 레벨·블록 내구·화폐·보안칸 내용·보호막 타이머) = 월드 `SavedData`(NBT)** ②**인증 매핑·통계·계정 연결 = 외부 DB**(`data-schema` 에이전트 설계 DB와 연동, 웹/봇 조회) ③**config = 수치 튜닝**(toml/datapack — 블록 내구·드랍률 등 플레이테스트 노브).
- **🧱 블록 내구 = 영역 일괄 배율 (확정):** 영역 내 블록은 코어 레벨로 **동일 내구**(배율), **손상된 블록만 좌표별 HP 추적**(자가복구·수리 대상) → 메모리↓·단순. 멀쩡한 블록은 추적 X(코어 레벨 배율로 즉석 계산).

## 3. 모듈 목록 (launch_plan §2 + 인증)
| 모듈 | 책임 | 주요 의존 | MVP |
|---|---|---|---|
| **M-Core** | 코어 설치/레벨/영역 64×64/자물쇠/자가복구 | OPaC | M2 |
| **M-Block** | 영역 내 블록 내구·효율 채굴·폭약/RPG 데미지 | — | M2 |
| **M-Material** | 커스텀 재료 4종·레시피 오버라이드 | TaCZ datapack | M1 |
| **M-Currency** | 물리 화폐·스택 한도·상점/GUI(코어 우클릭·Shift+F) | — | M1 |
| **M-Inv** | 무게 점유칸·보안칸·유리 식별 GUI 🔴 | TaCZ 아이템 | M1 |
| **M-POI** | 상자 후보 풀·리필 재롤·드랍 테이블·청사진 | Lost Cities | M1 |
| **M-World** | Y25 리셋·신규 보호막(12h 인벤세이브)·로비·[TIP] | — | M1 |
| **M-Auth** | 인게임 코드 인증·zenon-discord 연동(RCON/DB) | zenon-discord | M1 |
| **M-Scav** | 스캐브 랭크 HP/무장/갑옷/드랍 | TACZ NPCs | M1 |
| **M-Finale** | 결전 웨이브·헬리패드·탈출 부품/티켓·와이프 이월 | Undead Nights | M3 |
| **M-Raid좀비** | 결전 좀비-블록·평소 약한 갉기(BlockBreak hook) | Zombies B&B | M2/M3 |

## 4. 모듈별 구현 설계
> 각 모듈: 책임·주요 클래스·데이터(NBT/DB)·이벤트/hook·의존·❓. **설계 근거([`design/`]) 인용, 없으면 ❓.**

### M-Core (기지 코어 — base_raid 근거)
**책임:** 코어 아이템·설치/검증·레벨(1~5)·영역 64×64·코어 자물쇠·자가복구·코어 파괴 초기화.
- **주요 클래스/시스템:**
  - `CoreItem` — 설치 아이템(우클릭 → 설치 검증 → 코어 등록). LV1 제작(철블록2+레드스톤블록1+철4), 이미 코어/연합 코어 보유 시 제작·설치 불가(개인 1코어).
  - `CoreBlockEntity` — 코어 블록(옵시디언급 텍스처·내구 최고). 좌표·소유자(UUID)/연합(OPaC 파티 ID)·레벨·보안칸·손상 블록 맵 보유.
  - `CoreManager` (월드 `SavedData` NBT) — 전 코어 목록·영역 인덱스(좌표→코어 조회). 자물쇠·자가복구·설치 겹침 판정의 단일 출처.
- **데이터(NBT):** `[{pos, owner/party, level, securitySlots, damagedBlocks:{pos→hp}}]`. 영역=코어 중심 ±32(64×64), 멀쩡 블록 내구=레벨 배율 즉석(§2).
- **이벤트/hook:**
  - `BlockPlaceEvent`(코어) → 설치 검증: ①POI 내 불가 ②영역 64×64 월드보더 안 ③기존 영역 겹침 금지(CoreManager 인덱스). 위반 시 취소·메시지.
  - 컨테이너 `Open`/접근 → **자물쇠**: 영역 내 컨테이너 + 코어 생존 + 외부인(비소유·비연합) → 취소(OPaC 권한 연동).
  - `BlockBreakEvent`(코어) → 완파·레벨 초기화·자물쇠 해제(러스트 TC).
  - 서버 tick(주기) → **자가복구**: 코어 생존 + 최근 공격 없음(쿨다운) → damagedBlocks HP 회복.
- **의존:** OPaC(연합 파티 ID·권한·문). **설계 근거:** base_raid「코어 제작·레벨 v2」·「코어 자물쇠」·「자가복구」·「코어 설치 제약」·「연합」.
- **❓ M-Core:**
  - **자가복구 속도·공격 쿨다운 N초** = 설계 "#2/플레이테스트"라 미정 → **config 노브로 두고 기본값만 임시?** 기본값 제안 필요하면 질문.
  - **코어 블록 = BlockEntity vs 단순 블록+SavedData?** 설계는 "옵시디언급 블록"만 명시 → 구현 선택(BlockEntity가 자물쇠·자가복구 tick에 편리) — 이대로 가도 되나?

### M-Material (커스텀 재료·레시피 — economy #1-B 근거)
**책임:** 커스텀 재료 4종·레시피 오버라이드·작업대 게이트.
- **클래스/데이터:** `TungstenItem·DepletedUraniumItem·MilitaryAlloyItem·ElectronicPartItem`(순수 Item). 텅스텐·열화우라늄=작업대 제작(datapack 레시피, 철9구리9 / 텅스텐4다이아4). 군용·전자=**drop-only**(레시피 없음, 드랍은 M-POI/M-Scav 루트).
- **hook:** TaCZ datapack 오버라이드(탄약 구경별·총·방어판 = 네더라이트/blaze → 군용/전자 치환). 배틀라이플 .308=텅스텐 강화(파밍).
- **의존:** TaCZ(datapack). **근거:** economy #1-B 3층 트리·치환맵·#2-A 탄약. **❓ 없음**(수치=config, drop은 POI/Scav 루트).

### M-Currency (화폐·상점·GUI — economy 근거)
**책임:** 물리 화폐(단일 코인 스택~6000)·상점(구매/매입)·GUI 탭.
- **클래스:** `CoinItem`(maxStackSize 커스텀)·`ShopMenu/Screen`·GUI 탭(상점·배낭·기지관리·연합·정보). 코어 우클릭/Shift+F(영역 내)=거점 GUI / 정보 탭=어디서나 키.
- **데이터:** 화폐=인벤 NBT. 상점가·매입가=config. 거래=인벤 차감.
- **hook:** 코어 우클릭(M-Core)·Shift+F 영역 판정(M-Core). 효율 인챈트북·T1 폭약·탄(싸게)=상점 품목.
- **의존:** M-Core(영역)·M-Inv(보안칸 화폐). **근거:** economy 화폐·상점 v2·GUI 탭. **❓ 없음**(가격=config).

### M-Inv (무게·보안칸·유리 식별 GUI 🔴 — survival 근거)
**책임:** 무게 점유칸·보안칸(잃지 않는 칸)·유리 가림 식별. **최대 기술 리스크(launch_plan §1-6).**
- **클래스:** 커스텀 인벤 GUI 레이어(무게 점유=유리 막 / 보안칸=봉인 유리 / 식별=가림+마우스오버). 무게→속도(M-World 합산?).
- **데이터:** 보안칸 내용=코어/플레이어 영속(NBT, 죽어도 유지). 아이템 무게=lore/태그.
- **hook:** 인벤 렌더·슬롯 제어(TaCZ 총+부착물 아이템 엮임 = 🔴 프로토타입 우선). 데스박스/POI/보안칸 공통 유리 레이어.
- **의존:** TaCZ 아이템. **근거:** survival 인벤·무게·보안함·상자 식별. **❓ 없음**(둔화 상한 80%=config, 구현=M0 프로토타입).

### M-POI (상자·리필·드랍·청사진 — world/economy 근거)
**책임:** 상자 후보 풀·리필 전체 재롤(50%)·드랍 테이블·청사진 배치·POI 티어.
- **클래스:** `PoiManager`(후보 풀=Lost Cities 상자 위치 자동 흡수)·`LootTable`(티어별 카테고리 확률)·리필 스케줄(현실 24h).
- **데이터:** 후보 풀 좌표·POI 티어 지정(config/datapack). 리필=재롤(이전 무시). 드랍률=config.
- **hook:** Lost Cities 상자 위치 스캔·루트 테이블 덮어쓰기. POI 내 코어 설치 불가(M-Core 연동).
- **의존:** Lost Cities. **근거:** world POI 상자 리필·크기 티어·economy 드랍 테이블. **❓ 없음**(확률=config).

### M-World (Y25·보호막·로비·TIP — world/economy 근거)
**책임:** Y25 리셋·신규 보호막(12h·인벤세이브)·로비·[TIP]·무게→속도·굶주림.
- **클래스:** `Y25Reset`(손 탄 청크 추적·재생성)·`NewbieProtect`(12h 타이머·PvP 면역·파티클·인벤세이브)·`LobbyManager`(별도 월드/격리, 인증 전 가둠)·`TipBroadcaster`(10분 스케줄)·무게→속도(M-Inv 적재율 + 다리·갑옷 합산).
- **데이터:** 보호막 타이머=DB(계정·실시간 12h). 손 탄 청크=NBT. 로비=별도 월드.
- **hook:** 사망(보호막 중=인벤세이브)·이동속도·굶주림(바닐라).
- **의존:** —. **근거:** world Y25·이동·난이도 / economy 신규 보호막·로비·TIP. **❓ 없음**(12h·둔화=config·DB).

### M-Auth (인증 — economy 온보딩 근거)
**책임:** 인게임 코드 발행·디스코드 봇 연동·화이트리스트(모드 게이트)·계정 연결.
- **클래스:** `AuthManager`(미인증=로비 가둠, 코드 발행)·봇↔서버 통신(RCON or 공유 DB/REST)·인증 플래그→본섭 텔포.
- **데이터:** 코드·계정 연결·인증 상태=**외부 DB**(zenon-discord 봇 공유). 코드=시간 만료.
- **hook:** 로그인(미인증 판정)·로비 격리·인증 완료 텔포.
- **의존:** zenon-discord(봇·별도 폴더, 착수 시). **근거:** economy 온보딩 B(인게임 코드 양방향). **❓ 없음**(봇↔서버 통신 방식=착수 구현 편의).

### M-Scav (스캐브 — world 근거)
**책임:** 스캐브 랭크별 HP/무장/갑옷/드랍.
- **클래스/데이터:** [TACZ] NPCs 설정(config/datapack) 4랭크 HP 20/30/42/55·무장(권총~고급총)·갑옷(없음~Heavy)·화폐 20/45/110/280·비파밍(고랭크 저확률). 보스 스캐브 HP180.
- **hook:** TACZ NPCs 스폰·드랍 hook(갑옷 약탈 O/총 드랍 X). POI 티어 배치(M-POI).
- **의존:** TACZ NPCs. **근거:** world 스캐브 랭크 상세. **❓ 착수 검증:** TACZ NPCs AI·설정 범위(랭크 차등 지원?).

### M-Block (블록 내구·채굴·폭약 — base_raid v2 근거)
**책임:** 영역 내 블록 내구(영역 배율)·효율 채굴(효율로만)·폭약/RPG 데미지(5×5×5).
- **클래스:** `BlockDurabilityService`(멀쩡=코어 레벨 배율 즉석, 손상만 좌표 HP 추적)·`MiningHandler`(BlockBreak 가로채 = 영역 내는 hardness·도구 종류 무시, 효율 레벨5~10만으로 시간)·`ExplosiveBlock`(도화선·5×5×5: 중심 3×3×3 완전+외곽 약화).
- **hook:** `BlockBreakEvent`/채굴 진행(영역 내 가로채기)·폭약 BlockEntity·총격 보조(~0.3)·대물 관통(블록 1~2칸+뒤 타격).
- **의존:** M-Core(영역·레벨). **근거:** base_raid 「블록 부수기 v2」·효율 5~10·폭약 5×5×5·농성. **❓ 없음**(수치·DPS=config).

### M-Raid좀비 (좀비-블록 — base_raid 근거)
**책임:** 평소 약한 갉기 + 결전 강한 데미지(좀비 BlockBreak 강도 조절).
- **클래스:** `ZombieBlockHandler`(Zombies B&B 좀비 BlockBreak 가로채 → 영역 내 우리 블록 내구로, 평소 약·결전 강 일반0.5/elite5/demolition30)·결전 모드 플래그(M-Finale).
- **hook:** Zombies B&B 블록 파괴 이벤트·자가복구(M-Core)와 상쇄.
- **의존:** Zombies B&B. **근거:** base_raid 「좀비-블록」. **❓ 착수 검증:** Zombies B&B 제어 범위(config·이벤트 API — 우리 내구로 가로채기 가능?).

### M-Finale (결전·탈출 — finale 근거)
**책임:** 결전 웨이브·헬리패드 추출·탈출 부품/티켓·와이프 이월.
- **클래스:** `FinaleScheduler`(시즌 마지막날 ~21시 발동·6웨이브 점증)·`HelipadManager`(POI 옥상 후보 ~5~8 평소 떡밥, 결전날 1~2 랜덤 활성·신호기 호출)·`EscapePartBlueprint`(커스텀 recipe-unlock, 무선/연료/데이터/티켓)·`WipeCarryover`(탈출=배낭 전부/농성=보안함).
- **hook:** 결전 발동·웨이브 스폰(Undead Nights demolition/elite)·추출 좌석(탑승 순)·시즌 와이프.
- **의존:** Undead Nights. **근거:** finale 「#2 결전 수치」·탈출 경로. **❓ 없음**(좌석6~8·웨이브=config).

## 5. 모드 연동 맵
| 모드 | 연동 방식 | 착수 검증 |
|---|---|---|
| **TaCZ** | datapack 레시피 오버라이드(재료 치환)·armor_ignore 튜닝·탄약 구경별 | 데미지·armor_ignore 실측 |
| **OPaC** | 파티=연합(M-Core 권한)·문 권한 잠금·컨테이너 잠금 예외 hook | 파티원 예외·영역 화이트리스트 hook 범위 |
| **First Aid** | 부위 HP·의료 효능 config(server.toml)·바닐라 armor 자동 부위 매핑 | TTK·회복량 인게임 실측 |
| **Tactical 3D Armor** | 방어 포인트 datapack 오버라이드(L12/M18/H24) | datapack 오버라이드 가능 여부 |
| **Lost Cities** | 상자 위치 자동 흡수(후보 풀)·POI 티어 designate | 상자 위치 스캔·티어 지정 |
| **Undead Nights** | demolition/elite 좀비=결전 매핑·크리퍼 스폰율(화약) | 크리퍼 스폰율 |
| **Zombies B&B** | 좀비 BlockBreak 가로채(M-Raid좀비) | 제어 범위(config/API) |
| **[TACZ] NPCs** | 스캐브 랭크 HP/무장/갑옷/드랍 설정 | AI 품질·설정 범위 |

## 6. ❓ 질문 누적 (설계에 없어 결정 필요)
1. ✅ **빌드 Java target = 17**(확정 2026-06-10).
2. ✅ **데이터 영속 = 게임 NBT / 인증·통계 DB(data-schema 연동) / config 노브**(확정).
3. ✅ **블록 내구 = 영역 일괄 배율 + 손상 블록만 좌표 추적**(확정).
4. **🎉 모듈 설계 결과(2026-06-10): 게임플레이 동작 공백 ❓ 거의 없음 = 설계가 충실.** 남은 것은 임의 결정 대상 아님:
   - **구현 선택(동작 무관):** 코어=BlockEntity(자물쇠·tick 편의).
   - **수치 노브(config, 설계가 플레이테스트로 미룸):** 자가복구 속도·블록 내구/DPS 절대값·드랍률·둔화 상한·12h·공급량·가격.
   - **착수 검증(dev 서버 실측, launch_plan §1):** 🔴인벤 GUI 프로토타입·TACZ NPCs AI·Zombies B&B 제어 범위·datapack 오버라이드(방어/armor_ignore)·First Aid TTK·크리퍼 스폰율·OPaC hook 범위.
