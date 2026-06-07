# 한글화 정책 (Localization Policy)  — 초안(DRAFT)

> PoroMon은 한국어 유저 중심 서버다. **유저에게 보이는 텍스트는 최대한 한국어**로 제공한다.
> 대상: 상점 GUI/상점명, 아이템명·설명(lore), 명령 피드백, 안내 메시지.
> ⚠️ namespace·translation key·item id는 **추측 금지** → jar/리소스 확인 후 작성, 미확인은 `TODO`.

## 1. 목표 / 원칙
- **영어를 모르는 유저도 이해할 수 있게** 한국어 중심으로 작성.
- 상점 GUI·아이템 설명은 한국어 우선. 누락 시 영어 fallback(`en_us`).
- 정식 포켓몬/아이템 한국어 명칭을 따르되, 내부 ID·키는 영어 유지(코드/데이터는 영어).
- 한국어 리소스는 **공식 모드팩에 동봉**(유저 수동 설치 없이 적용, CLAUDE.md 배포 정책과 일치).

## 2. 한글화 대상과 방식
| 대상 | 방식 | 소유 |
|---|---|---|
| **PoroMonCore 커스텀 아이템**(조우권 등) | `ko_kr.json` 번역 키 + 아이템 **lore(설명)** 한글 작성 | PoroMonCore(직접) |
| PoroMonCore GUI/상점명/버튼/명령 메시지 | 서버측 텍스트를 한국어로(`Text`/Component) | PoroMonCore(직접) |
| **SimpleTMs** 아이템(TM/TR 등) | 리소스팩 언어 파일(`ko_kr.json`) 오버라이드 후보 | 리소스팩 |
| **Eggs Addon** 아이템(알 등) | 리소스팩 언어 파일 오버라이드 후보 | 리소스팩 |
| **Mega Showdown** 아이템(메가스톤/팔찌 등) | 리소스팩 언어 파일 오버라이드 후보 | 리소스팩 |
| **Cobblemon** 본체 | 한국어(`ko_kr`) 기본 제공 여부 **확인** → 미흡분만 보완 | 리소스팩 |

### 2.1 서버측 vs 클라측
- **서버측 텍스트**(상점 GUI 타이틀, 버튼, 명령 응답, 시스템 메시지): PoroMonCore가 **한국어 문자열로 직접 전송** → 클라 리소스팩 없이도 한국어 표시.
- **클라측 아이템명/툴팁**(바닐라/모드 아이템 표시): 클라가 자기 언어 파일로 렌더 → **한국어 리소스팩이 클라에 있어야** 한국어로 보임. 서버는 강제 불가 → **공식 팩에 한국어 리소스팩 동봉**으로 해결.
- PoroMonCore 커스텀 아이템은 `ko_kr.json`을 **모드 jar에 포함**하면 별도 리소스팩 없이도 한국어(클라 언어=ko_kr 시).

## 3. PoroMonCore 커스텀 아이템 한글화(직접 소유)
- 번역 키 예: `item.poromoncore.<id>` → `ko_kr.json`에 한국어명. (실제 키 네이밍은 구현 시 확정)
- lore(설명): 조우권 등은 **사용법을 한국어 lore로** 명시(예: "전설 제단에서 사용 — 개인 조우방 입장").
- en_us도 함께 제공(영어 유저/fallback).

## 4. 모드 아이템 한글화(리소스팩 오버라이드) — jar 실측 (`jar_feature_audit.md` §5)
| 모드 | namespace(확인) | ko_kr 기본 | 작업 |
|---|---|---|---|
| Cobblemon | `cobblemon` | ✅ 있음 | 누락만 보완 |
| Mega Showdown | `mega_showdown` | ✅ 있음(z_ring/tera_orb 등) | 메가스톤 커버리지 확인 후 누락만 |
| SimpleTMs | `simpletms` | ✅ 있음(UI/타입까지) | 거의 불필요 |
| Legendary Monuments | `legendarymonuments` | ❌ **없음**(en/es/ja/ru/zh) | **ko_kr 신규 작성 1순위** |
| Eggs Addon | `diesse`(data만) | ❌ lang 없음 | 표시 텍스트 거의 없음 |

- 공식 한국어 리소스팩(`PoroMon-Korean-Pack`)에 각 모드 `ko_kr.json`을 모아 오버라이드.
- **1순위 = Legendary Monuments**: `assets/legendarymonuments/lang/en_us.json` 키 전체(제단/항아리/피리/열쇠/툴팁 — 수백 개) 한국어 번역.
- **Language Reload**(모드팩 포함, 클라) 활용 → 재시작 없이 lang 갱신(번역 작업 효율).
- ✅ 세 코어 모드(Cobblemon/MSD/SimpleTMs) ko_kr 기본 포함 → **한글화 작업량 대폭 감소**(주작업 = LM + PoroMonCore 커스텀).

## 5. 운영 / 작업 흐름
1. 모드별 `en_us.json`(키 목록) 추출 → 한국어 번역 → `ko_kr.json` 작성.
2. PoroMon 한국어 리소스팩에 통합 → 공식 모드팩에 동봉.
3. PoroMonCore 커스텀 텍스트는 코드/리소스에서 한국어 직접 작성.
4. 검수: 영어 잔존 텍스트(상점/아이템/툴팁) 점검 → 보완.
5. 누락 키는 en_us fallback 허용하되 **유저 핵심 동선(상점/조우권/알/메가)** 은 우선 한글화.

## 5-1. 조우권 / 전설 이벤트 / 상점 GUI 한글화 (PoroMonCore 소유)
조우권·전설 이벤트는 PoroMonCore 커스텀이므로 **한국어 직접 작성**(서버측 텍스트 + 커스텀 아이템 ko_kr/lore).

**한국어로 표시할 조우권명:**
희귀 조우권 · 하급 전설 조우권 · 중급 전설 조우권 · 상급 전설 조우권 · 최상급 전설 조우권 · 하늘 조우권 · 심해 조우권 · 대지 조우권 · 시간 조우권 · 공간 조우권 · 반전 조우권 · 빛 조우권 · 용왕 조우권 · 수호자 조우권 · 영원 조우권

**필드 이벤트/특수 조우 공지(한국어, 좌표 비공개·힌트만):**
```text
[전설의 기척]
설원 어딘가에 프리져의 차가운 기운이 감돌기 시작했습니다.
제한 시간: 20분
```
```text
[하늘의 균열]
하늘 위 어딘가에서 레쿠쟈의 포효가 울려 퍼집니다.
이 조우는 특별한 조건을 만족한 트레이너만 도전할 수 있습니다.
```
```text
[영원의 문]
영원의 힘이 잠시 세계에 모습을 드러내려 합니다.
이 조우는 아직 잠겨 있습니다.
```
- 공지/제단 GUI/조우권 lore는 서버측 한국어로 전송 → 클라 리소스팩 없이도 한국어 표시.
- 풀의 `display_name_ko`·`biome_hint_ko`(`legendary_pools.yml`)를 공지/표시에 사용. species ID(영어)는 표시하지 않음.

## 6. 확인 필요 (TODO)
1. ~~각 모드 namespace / translation key~~ → ✅ 확인 완료(§4, `jar_feature_audit.md` §5).
2. Mega Showdown ko_kr **메가스톤 명칭 커버리지** 확인(누락분 보완).
3. 모드 아이템명 리소스팩 오버라이드가 정상 적용되는지(우선순위) 검증.
4. PoroMonCore 커스텀 아이템 번역 키 네이밍 규칙 확정.
5. 한국어 리소스팩 배포 형태(모드팩 동봉 / 서버 리소스팩 강제 여부).

## 7. 관련 문서
- 상점: `../04_game_design/shop_design.md` / `shop_catalog_0.1.md`
- 조우권(커스텀 아이템): `../04_game_design/encounter_pool_design.md`
- 배포 정책: `CLAUDE.md`(Modpack Policy) · 모드 목록: `../01_modpack/modpack_list.md`
- 유저 설치: `user_install_guide.md`(TODO)

---

## 8. 실행 계획 — 2026-06-07 전수조사 반영 (구현 전, 계획만)

### 8.1 전수조사 결과 (현황 갱신)
- **클라 언어 = `ko_kr`** 확정(PoroMon 0.1 Dev `options.txt: lang:ko_kr`).
- **Cobblemon / SimpleTMs / Mega Showdown 모두 ko_kr 내장** → 인게임 **인벤토리/툴팁**의 포켓몬명·기술명·볼·메가스톤·아이템은 **이미 한국어로 표시**됨(클라가 ko_kr 렌더).
- Cobblemon 번역은 **Weblate 커뮤니티 번역**(lang.cobblemon.com, 한국어 프로젝트) → 빌드시점 스냅샷이라 **누락분은 영어 폴백** 가능. 포켓몬 공식 한글명 출처: Bulbapedia 한국어 포켓몬명 / 포켓몬코리아.
- §4의 **Legendary Monuments 1순위·Eggs Addon 항목은 무효**: LM=완전 비활성(결정023, 표시 텍스트 노출 없음), Eggs=제거(결정032).

### 8.2 진짜 남은 작업 = PoroMonCore GUI의 영어 "박제" 제거 (핵심)
우리 서버 GUI는 모드 아이템 이름을 `item.getName().getString()`(=서버 locale, **영어**)로 **문자열 고정**해 CUSTOM_NAME에 넣는다 → 클라가 ko_kr여도 **우리 메뉴에서만 영어**로 보임. 박제 지점 **9곳**:
| 파일 | 건수 |
|---|---|
| `shop/CategoryShopMenu.java` | 3 |
| `shop/BuyShopMenu.java` | 2 |
| `shop/SellShopMenu.java` | 2 |
| `shop/EngineeringMenu.java` | 1 |
| `shop/TmCatalog.java` | 1 (move displayName) |

**해결 방향(구현 시):** `MenuIcons.icon`에 `Text` 이름을 받는 오버로드 추가 → `item.getName()`(translatable Text) 또는 `MoveTemplate.getDisplayName()`(translatable)을 **그대로** 전달(색은 `Text.empty().append(...)` 또는 styled prefix). 그러면 클라(ko_kr)가 자동 한글 렌더. **추가 번역 데이터 없이** GUI 한글화 완료.
- 단, `EngineeringMenu`/`TmCatalog`의 **검색은 영문 키 기준** 유지(한글 검색은 별도) — displayName이 한글이 되면 한글 검색도 부분일치로 동작 가능(검토).

### 8.3 누락 번역 처리 (필요 시)
- 모드 ko_kr에 빠진 항목(영어로 뜨는 것)만 **자체 `ko_kr.json` 오버라이드**(공식 한국어 리소스팩 `PoroMon-Korean-Pack`)로 보완. 키는 해당 모드 `en_us.json`에서 추출.
- 출처: ① 모드 Weblate ko 최신 ② 포켓몬/기술 공식 한글명(Bulbapedia/포켓몬코리아) ③ **사용자 제공 공식 번역본**.
- PoroMonCore 커스텀(리그패스/정수/배지/조우권/메뉴/메시지)은 **이미 한국어 하드코딩** → 작업 불필요(영어 잔재만 점검).

### 8.4 작업 순서 (구현 단계 — 승인 후)
1. 인게임 ko_kr 캡처로 **실제 영어 잔존 지점 확정**(우리 GUI 9곳 + 혹시 영어로 뜨는 모드 항목 목록화).
2. ✅ **완료(2026-06-07)** `MenuIcons` Text 오버로드(`icon(Item,Text,lore)`·`iconCount`·`named(Formatting,Text)`) 추가 → 박제 9곳 + 추가 발견분(EngineeringMenu 종족/기술명, TmCatalog `displayText`) translatable 전환 → 빌드 성공 + 헤드리스 부팅 검증. **잔여 박제=서버 로그 2곳(유저 비노출)**. 상세=task.md §4k.
3. (있으면) 누락 모드 번역만 `PoroMon-Korean-Pack` ko_kr override 작성 → 모드팩 동봉. **← 다음(클라 캡처로 잔존 확정 후)**
4. 검수 체크리스트: 메뉴 전체 / 상점(매입·편의·성장·실전·기술머신·메가·제단·포로공학) / 아이템 lore / 명령 응답.

### 8.5 검증
- 클라 ko_kr에서 각 GUI 캡처 → 영어 잔존 0 목표(핵심 동선 우선).
- 누락은 en_us 폴백 허용하되 핵심 동선(상점/조우권/포로공학)은 한글 강제.
