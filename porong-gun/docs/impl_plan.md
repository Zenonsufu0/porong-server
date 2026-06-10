# porongun-core 구현 계획서 (2026-06-10~, 작성 중)

## 0. 🔑 작성 원칙 (최우선)
- **기획서 = 단일 진실:** [`concept.md`](concept.md) · `design/`(economy·base_raid·survival·world·finale) · [`launch_plan.md`](launch_plan.md) · [`ops_policy.md`](ops_policy.md). 구현은 **설계를 그대로 따른다.**
- **🚫 설계에 없는 동작·수치·정책은 임의 결정 금지 → 사용자 질문**(이 문서에 ❓로 표시·누적). 추측으로 채우지 않는다.
- 설계 간 충돌·누락 발견 시 **보고**(임의 해소 X).
- 구현은 **MVP 순서**(M0→M3, launch_plan §3) 따라 점진.

## 1. 기술 스택
- **Forge 1.20.1** (dev 서버 47.4.10) / **Java 17 target**(빌드, 확정 — 1.20.1 표준, dev 구동은 Java 21 호환).
- Gradle + ForgeGradle, 단일 모드 **porongun-core**(포트폴리오 묵직 OK).
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
| **M-Auth** | 인게임 코드 인증·porong-discord 연동(RCON/DB) | porong-discord | M1 |
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

## 5. 모드 연동 맵
> ⏳ 작성 예정 — TaCZ(datapack 오버라이드·armor_ignore)·OPaC(파티=연합·문·컨테이너 hook)·First Aid(부위 HP config)·Lost Cities(상자 위치 흡수)·Zombies B&B(BlockBreak 가로채기) 각 연동 방식·착수 검증.

## 6. ❓ 질문 누적 (설계에 없어 결정 필요)
1. ✅ **빌드 Java target = 17**(확정 2026-06-10).
2. ✅ **데이터 영속 = 게임 NBT / 인증·통계 DB(data-schema 연동) / config 노브**(확정).
3. ✅ **블록 내구 = 영역 일괄 배율 + 손상 블록만 좌표 추적**(확정).
4. *(이하 모듈 설계하며 누적)*
