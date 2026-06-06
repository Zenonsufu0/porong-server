# PoroMon 아이디어 인박스 (미확정 DRAFT)

> 검토·확정 전 아이디어 보관소. 확정 시 `decisions.md`(DL 번호 부여)로 승격하고 여기엔 `[승격 → 029...]` 표시. 폐기 시 `[폐기 — 이유]`.
> (전역 인박스 `poro-rpg/docs/idea_inbox.md`는 RPG 전용/읽기전용이라, 포로몬 아이디어는 여기에 기록한다.)

---

## IB-001. 이로치(샤이니) 확정권 / 조우권 (2026-06-06 제안)

**제안:** 상점/보상에 이로치 관련 상품 추가.
- **이로치 조우권**: 사용 시 개인 조우방에서 **샤이니 확정** 포켓몬과 조우(포획 기회).
- **이로치 확정권**: 사용 시 **보유(파티) 포켓몬 1마리를 골라 그 자리에서 샤이니로 변환**(2026-06-06 정의 확정).

**모드 지원 확인(✅):** Cobblemon 기본 샤이니 지원.
- 샤이니 레이트 config(`shiny_rate` = 1/x), 야생 샤이니 연출/파티클, `first_shiny_catch` 도전과제, PC 필터 `shiny`.
- 낚시 미끼 효과 `shiny_reroll`(샤이니 확률 증가)도 기본 존재.
- `PokemonProperties`에 `shiny=true` 지정 가능 → **확정 샤이니 스폰 가능**(배틀타워 spec 빌드와 동일 경로).
- **`Pokemon.getShiny()/setShiny(boolean)` 존재(✅ jar 검증)** + `PlayerPartyStore`로 플레이어 파티 접근 가능 → **보유 포켓몬 샤이니 변환 가능**.

**구현 난이도(추정):**
- **이로치 확정권(보유 변환)** = 쉬움. 파티 6칸 선택 GUI(ServerMenuHandler 재사용) → 선택 포켓몬 `setShiny(true)` → 확정권 소비. 이미 보유한 포켓몬을 바꾸는 거라 스폰 훅 불필요.
- **이로치 조우권** = 쉬움/중. 기존 조우권 시스템(EncounterTicketManager + InstanceRoom + pvn) 재사용 + spec에 `shiny=true`. 배틀타워와 동일 패턴.

**검토 필요:**
- 가격/희소성(이벤트·후반 한정? 일반 판매?). egg_pool에 Shiny Egg는 이미 "이벤트/후반"으로 비추천(`egg_pool_design §7`) — 정책 일관성 맞출 것.
- 확정권 남용 시 샤이니 가치 희석 → 고가/제한/이벤트 게이트 검토.
- 전설/환상에도 적용할지(레쿠쟈 샤이니 등) — 강한 제한 필요.

**상태:** DRAFT. 조우권 시스템(§4c 5번) 구현 시 같이 검토. 확정 시 decisions 승격.

## IB-002. 운영자 부스트/이벤트 GUI (2026-06-07 제안)

운영자가 GUI로 토글하는 전역 부스트(이벤트). **전부 구현 가능 확인.**
- **경험치 ×N**: `CobblemonEvents.EXPERIENCE_GAINED_EVENT_PRE` 구독 → 경험치 배수.
- **골드 ×N (처치·상점 판매가)**: 전역 배수 — 매입소(SellShopMenu) 판매가 ×N, 야생 처치/포획 보상 ×N(처치 보상 미구현 → 구현 시 함께). EconomyBridge/RewardManager.
- **컨셉 최상급 확률 ×N**: `EncounterService.weightedPick`에서 이벤트 활성 시 apex(type=apex) 후보 weight ×N.
- **운영자 GUI**: 부스트 항목 토글/배수 설정 + 전역 상태(PersistentState `EventState`) 저장·영속. `/poromon admin event` 또는 메뉴.

구현 방식: 전역 이벤트 상태(부스트 플래그/배수) + 각 시스템에 배수 적용 훅 + 운영자 GUI. SeasonManager/EventManager(향후) 모듈. 0.1 범위 밖이나 소규모로 추가 가능.
상태: DRAFT. 야생 처치 골드 보상(economy_design §3)과 함께 구현하면 골드 부스트가 완성됨.

## IB-003. 커스텀 메뉴 GUI 화면(배경) 교체 (2026-06-07 제안)

현재 메뉴/상점 GUI는 바닐라 컨테이너(이중 상자) 기본 텍스처 + 회색 유리판 채움이라 투박함. 전용 GUI 배경으로 교체하고 싶음.

**가능 방식(다음 세션 검토):**
- **A. 클라 리소스팩 컨테이너 텍스처 교체**: `textures/gui/container/generic_54.png` 등 교체 → 전역(모든 9×6 컨테이너 공통 영향, 메뉴별 구분 불가). 모드팩 동봉.
- **B. 타이틀 이미지-폰트 트릭(서버측만)**: 커스텀 폰트(음수 공백 + 이미지 글리프)를 GUI 타이틀에 넣어 메뉴별 배경/장식을 그림. 클라 모드 불필요(리소스팩만), 메뉴별 커스텀 가능. (MMO 서버에서 흔한 기법.)
- **C. 클라 커스텀 Screen**: 별도 ScreenHandlerType + 클라 Screen 렌더. 가장 자유롭지만 **클라 모드 필요**(서버권위 GenericContainer 구조 변경).

추천 검토 순서: B(서버측·메뉴별 가능) → 부족하면 A(전역) 병행. 상태: DRAFT. 텍스처 디자인은 Windows source 원칙.
