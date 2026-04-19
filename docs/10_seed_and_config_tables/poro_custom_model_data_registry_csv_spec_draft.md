# 포로 서버 CustomModelData 전역 레지스트리 CSV 포맷 1차 설계

> 작성일: 2026-04-19
> 배경: 리소스팩·커스텀 UI·무기·치장 1차 초안(`docs/14_world_and_maps/poro_resourcepack_custom_ui_weapon_cosmetic_draft.md`) §1.3에서 7자리 CustomModelData 인코딩 `ABCDDEE` + 도메인 예약 구간표가 확정됨. 번호 충돌이 리소스팩 설계 최우선 리스크로 식별되어 본 스펙을 data-schema 역할로 작성.
> 본 문서는 **CSV 단일 파일 + Git 브랜치 리뷰** 기본 흐름을 유지하는 1차 스펙이며, 과한 재설계·별도 서비스 구축을 하지 않는다.

---

## 1. 개요

포로 서버 클라이언트 리소스팩이 사용하는 모든 CustomModelData 번호(이하 **CMD**)를 단일 CSV로 관리하는 전역 레지스트리의 1차 스펙을 정의한다. CSV는 기획 리포(`docs/` 하위)에 위치하고, 모든 추가·변경은 PR 리뷰를 거친다. 리소스팩 빌드 파이프라인과 EmpireRPG 플러그인 모두가 본 CSV를 단일 진실 공급원(SSOT)으로 참조한다.

## 2. 목표

- 7자리 CMD `ABCDDEE` 인코딩과 도메인 예약 구간을 그대로 CSV 스키마로 수용한다.
- 같은 번호·같은 경로·예약 구간 이탈을 **PR 머지 이전**에 기계적으로 차단한다.
- 웹/디스코드 봇/운영 도구가 재사용 가능한 형태로 도메인·카테고리·상태를 조회할 수 있게 한다.
- 시즌2 이후 새 도메인 추가·v0.2 JSON 플래그 도입과의 호환 여지를 남긴다.

## 3. 핵심 규칙

### 3.1 파일 위치·네이밍

- 경로: `docs/10_seed_and_config_tables/poro_custom_model_data_registry.csv`
- 인코딩: UTF-8, 줄바꿈 LF, 헤더 1행 필수.
- 정렬 규칙: `customModelData` 오름차순 고정. 리뷰 diff 가독성을 위해 PR 단위로 정렬을 유지한다.
- 쉼표 충돌 회피를 위해 `notes` 등 자유 텍스트 필드는 큰따옴표로 감싼다(RFC 4180).

### 3.2 CSV 컬럼 스키마 (11 컬럼)

| # | 컬럼명 | 타입 | 필수 | 설명 |
|---|-------|-----|-----|------|
| 1 | `customModelData` | int(7) | Y | PK. 1000000~9999999. `ABCDDEE` 규칙 준수. |
| 2 | `domain` | enum | Y | `weapon` / `cosmetic` / `ui` / `block` / `consumable` / `reserved` |
| 3 | `category` | string | Y | 도메인 하위 분류(예: `warrior_greatsword`, `wallet_bg`, `blueprint_2x2`). snake_case. |
| 4 | `item_id` | string | Y | 포로 내부 아이템 식별자. 네임스페이스 포함(예: `poro:weapon/warrior/greatsword_t1_s1`). UI·블록도 내부 식별자 필수. |
| 5 | `asset_path` | string | Y | 리소스팩 내 상대경로. `assets/poro/` 기준 상대(예: `models/item/weapon/greatsword/t1_s1.json`). |
| 6 | `owner` | string | Y | 담당 기획자/에이전트 핸들(예: `@zenonsufu0`, `agent:content-designer`). |
| 7 | `status` | enum | Y | `reserved` / `designed` / `implemented` / `deprecated` |
| 8 | `version_added` | semver | Y | 추가된 리소스팩 버전(예: `s1-v0.1.0`). |
| 9 | `version_deprecated` | semver | N | deprecated 전환 버전. 비어 있으면 현역. |
| 10 | `flag_key` | string | N | v0.2 JSON 플래그 저장소 연동 키. 비어 있으면 플래그 없음. (§8 확장 포인트) |
| 11 | `notes` | string | N | 자유 메모. 쉼표·줄바꿈 포함 시 큰따옴표 이스케이프. |
| 12 | `axis` | enum | N | 경매 시세 분류 축(`equipment` / `cosmetic` / `material` / `consumable`). 경매 대시보드 `auction_listing.axis` 필드 매핑 소스 (2026-04-19 C-2 A안 추가). 비-아이템(UI·block 등)은 비어 있음. |
| 13 | `cosmetic_source` | enum | N | 치장 획득 경로 분류(`fixed_purchase` / `random_box` / `event` / `ranker`). `domain=cosmetic` 행에만 필수, 그 외는 비어 있음. 경매 대시보드 `auction_listing.cosmetic_source` 필드 매핑 소스 (2026-04-19 C-2 A안 추가). |

### 3.3 예약 구간 정합 규약

리소스팩 드래프트 §1.3을 그대로 수용한다. CSV 린트 스크립트는 `customModelData` 앞자리(`A`)와 `domain` 조합을 아래 표에 대입해 불일치 시 에러를 낸다.

- `A=1` / 1000000~1999999 → `domain=weapon`
- `A=2` / 2000000~2999999 → `domain=cosmetic`
- `A=3` / 3000000~3099999 → `domain=ui`
- `A=4` / 4000000~4099999 → `domain=block`
- `A=5` / 5000000~5099999 → `domain=consumable`
- `A=9` / 9000000~9999999 → `domain=reserved`

추가로 `B`(직업·계열), `C`(무기종·하위), `DD`(등급·티어), `EE`(연번) 인코딩은 무기 도메인에서 §3.3 리소스팩 드래프트 예약표(1111000~ 등)를 그대로 따른다. 린트는 **도메인별 부가 규칙**을 플러그인처럼 확장 가능하게 설계한다(§6 도구 요구사항).

## 4. 범위 (1차)

- 위 11 컬럼 단일 CSV 1파일.
- PR 리뷰 체크리스트 + CI 린트 스크립트 요구사항.
- 중복·충돌 감지 규칙 3종 및 "다음 번호 추천" 유틸 요구사항.
- 샘플 10~15행.

## 5. 비범위 (1차)

- 별도 웹 UI·관리자 콘솔 구축.
- DB 이관(본 CSV 자체가 1차 SSOT).
- 자동 번호 예약 API(수동 PR + 추천 스크립트까지만).
- 실제 린트 코드 구현(요구사항 수준까지만).

## 6. 구현 포인트

### 6.1 CSV 샘플 (13행)

```csv
customModelData,domain,category,item_id,asset_path,owner,status,version_added,version_deprecated,flag_key,notes
1111000,weapon,warrior_greatsword_t1_s1,poro:weapon/warrior/greatsword_t1_s1,models/item/weapon/greatsword/t1_s1.json,@zenonsufu0,designed,s1-v0.1.0,,,"T1 대검 S1 프로토타입"
1111020,weapon,warrior_greatsword_t1_s2,poro:weapon/warrior/greatsword_t1_s2,models/item/weapon/greatsword/t1_s2.json,agent:content-designer,reserved,s1-v0.1.0,,,
1112000,weapon,warrior_katana_t1_s1,poro:weapon/warrior/katana_t1_s1,models/item/weapon/katana/t1_s1.json,agent:content-designer,reserved,s1-v0.1.0,,,
1113000,weapon,warrior_spear_t1_s1,poro:weapon/warrior/spear_t1_s1,models/item/weapon/spear/t1_s1.json,agent:content-designer,reserved,s1-v0.1.0,,,
1121000,weapon,warrior_greatsword_t2_s1,poro:weapon/warrior/greatsword_t2_s1,models/item/weapon/greatsword/t2_s1.json,agent:content-designer,reserved,s1-v0.1.0,,,
2021000,cosmetic,weapon_skin_greatsword_flame,poro:cosmetic/weapon/greatsword_flame,models/item/cosmetic/weapon/greatsword_flame.json,@zenonsufu0,designed,s1-v0.1.0,,,"무기 치장 변형 레이어"
2022000,cosmetic,cape_basic_blue,poro:cosmetic/cape/basic_blue,models/item/cosmetic/cape/basic_blue.json,@zenonsufu0,reserved,s1-v0.1.0,,,
2023000,cosmetic,mount_skin_wolf_white,poro:cosmetic/mount/wolf_white,models/item/cosmetic/mount/wolf_white.json,@zenonsufu0,reserved,s1-v0.1.0,,,
3000001,ui,glyph_common_back,poro:ui/common/back,font/common/back.png,@zenonsufu0,designed,s1-v0.1.0,,,"공용 뒤로가기 글리프"
3000002,ui,glyph_common_confirm,poro:ui/common/confirm,font/common/confirm.png,@zenonsufu0,designed,s1-v0.1.0,,,
3001001,ui,screen_wallet_bg_main,poro:ui/wallet/bg_main,font/ui/wallet/bg_main.png,@zenonsufu0,designed,s1-v0.1.0,,,"월렛 메인 배경"
4000001,block,blueprint_2x2_base,poro:block/blueprint/2x2_base,models/block/blueprint/2x2_base.json,agent:feature-planner,reserved,s1-v0.1.0,,ff_custom_block_v1,"2×2 청사진 블록 검토 중"
5000001,consumable,food_basic_bread,poro:consumable/food/basic_bread,models/item/consumable/food/basic_bread.json,@zenonsufu0,reserved,s1-v0.1.0,,,
9000001,reserved,tmp_prototype_slot,poro:reserved/tmp/prototype_01,,agent:implementation-reviewer,reserved,s1-v0.1.0,,,"임시 프로토타입용 예비 번호"
```

### 6.2 중복·충돌 감지 규칙 (3종 + 부가)

모든 규칙은 **PR 단위 린트 + 병합 전 CI** 양쪽에서 실행된다.

#### 규칙 R1 — `customModelData` 중복 불가 (PK 무결성)
- 예시 충돌: 두 행 모두 `1111000`.
- 감지 의사코드
```
seen = {}
for row in csv:
    if row.customModelData in seen:
        error("DUP_CMD", row, prev=seen[row.customModelData])
    seen[row.customModelData] = row
```

#### 규칙 R2 — `asset_path` 중복 불가 (공백 제외)
- 예시 충돌: 서로 다른 CMD가 동일 모델 JSON을 가리켜 리소스팩 빌드 시 덮어씀 발생.
- 감지 의사코드
```
seen_path = {}
for row in csv:
    if not row.asset_path: continue   # UI 공용 등 의도적 공백 허용
    if row.asset_path in seen_path:
        error("DUP_ASSET", row, prev=seen_path[row.asset_path])
    seen_path[row.asset_path] = row
```

#### 규칙 R3 — `item_id` ↔ `customModelData` 1:1
- 예시 충돌: 같은 `item_id`가 두 개의 CMD에 매핑(외형 변형은 별도 `item_id`로 분리해야 함).
- 감지 의사코드
```
seen_item = {}
for row in csv:
    if row.item_id in seen_item and seen_item[row.item_id] != row.customModelData:
        error("DUP_ITEM_ID", row, prev_cmd=seen_item[row.item_id])
    seen_item[row.item_id] = row.customModelData
```

#### 부가 규칙 R4 — 예약 구간 이탈 금지
- 예시 충돌: `domain=ui`인데 `customModelData=1234567`(무기 구간).
- 감지 의사코드
```
RANGE = {
  "weapon":     (1000000, 1999999),
  "cosmetic":   (2000000, 2999999),
  "ui":         (3000000, 3099999),
  "block":      (4000000, 4099999),
  "consumable": (5000000, 5099999),
  "reserved":   (9000000, 9999999),
}
for row in csv:
    lo, hi = RANGE[row.domain]
    if not (lo <= row.customModelData <= hi):
        error("OUT_OF_RANGE", row, expected=RANGE[row.domain])
```

#### 부가 규칙 R5 — enum/semver 형식 검증
- `domain`, `status`는 허용 목록 밖 값 금지.
- `version_added`는 `s{int}-v{major}.{minor}.{patch}` 정규식 강제.
- `version_deprecated`가 비어있지 않으면 `status=deprecated`여야 함(역도 성립).

### 6.3 워크플로 (PR 리뷰 규약)

**PR 리뷰어 필수 체크리스트**

1. 추가·변경된 모든 행의 `customModelData`가 `ABCDDEE` 규칙을 만족하는가.
2. `domain` ↔ 구간(R4) 및 무기 인코딩(`A=1, B, C, DD, EE`) 불일치 없음.
3. `asset_path`가 실제 리소스팩 폴더 구조(`assets/poro/...`) 컨벤션을 따르는가.
4. `status=implemented`로 바뀌는 행은 대응 에셋 파일이 같은 PR 혹은 직전 PR에 존재하는가.
5. `status=deprecated` 전환 시 `version_deprecated` 필수 + 회수 정책(§6.4.2) 명시.
6. `notes`에 민감 정보(외부 URL, 비밀값, 운영자 실명 등) 없음.
7. CSV가 `customModelData` 오름차순 정렬 유지.
8. 린트 CI가 녹색인가(R1~R5 전부 통과).

**병합 충돌 해결 규약 (같은 번호를 두 PR이 동시 예약한 경우)**

- Git 레벨 병합 충돌이 발생하면 **먼저 머지된 PR 우선(FIFO)**.
- 나중 PR 작성자는 §6.4.1의 "다음 사용 가능한 번호 추천" 스크립트로 새 번호를 재할당한다.
- 재할당 후 동일 리뷰어가 차이만 재확인하는 **fast-track 재리뷰**(24시간 이내).
- 충돌이 잦은 구간(예: T2 대검 연번)은 owner 필드로 "섹션 담당자"를 지정해 사전 분배한다.

### 6.4 도구·스크립트 요구사항

#### 6.4.1 신규 번호 추천 스크립트 (`suggest-cmd`)
- 입력: `domain`, `category`(선택), 기존 CSV.
- 출력: 해당 도메인·카테고리 내 가장 작은 빈 번호 1개.
- 동작: 도메인 예약 구간 내에서 이미 사용 중인 CMD 집합을 제외하고, 같은 카테고리 앞자리(`ABC`) 뒤의 `DD`, `EE`를 최소값부터 스캔.
- 출력 예시: `suggest-cmd --domain=weapon --category=warrior_greatsword_t1_s1 → 1111001`

#### 6.4.2 deprecated 번호 회수 정책
- `status=deprecated` 전환 후 **최소 1시즌(= 1 major)** 동안 재사용 금지 — 플래그 이행·구 클라이언트 캐시를 위한 유예 구간.
- 회수(재사용)는 별도 PR에서 `status=reserved`로 되돌리며, 기존 행은 **삭제하지 않고** `item_id`만 새 값으로 치환하고 `notes`에 회수 이력을 남긴다(감사 로그).
- 린트가 `version_deprecated`와 현재 리소스팩 `version_added` 간 시즌 차이를 검증한다.

#### 6.4.3 리소스팩 빌드 시 정합성 검증
- 빌드 스크립트가 CSV를 읽어 모든 `asset_path`가 `assets/poro/` 하위에 실제로 존재하는지 확인(단, `status=reserved`는 선택적, `designed` 이상은 필수).
- 반대로 `assets/poro/` 아래 에셋 중 CSV에 등록되지 않은 것은 경고(고아 에셋).
- 결과는 빌드 리포트로 출력하고, CI는 에러 1건 이상 시 실패 처리.

## 7. 테스트 포인트 (1차)

- 샘플 CSV(§6.1)로 R1~R5 린트를 돌려 통과 확인.
- 의도적 오염 케이스 5종(중복 CMD, 중복 path, 1:1 위반, 구간 이탈, 잘못된 semver)을 CI 회귀 스위트로 고정.
- `suggest-cmd` 출력이 예약표의 인코딩 규칙과 충돌하지 않는지 무기·UI·치장 각 1건씩 스모크 테스트.

## 8. 오픈 질문

- CSV 저장 위치가 `docs/10_seed_and_config_tables/`로 충분한가, 아니면 리소스팩 리포가 별도 생성되면 그쪽으로 이동해야 하는가(리소스팩 드래프트 §7 오픈 질문 연계).
- `owner` 표기 컨벤션을 디스코드 핸들 기준으로 할지, 깃 유저명 기준으로 할지.
- `flag_key`를 통한 v0.2 JSON 플래그 저장소 연동 시, 플래그 미적용 CMD의 기본 동작을 "숨김"으로 할지 "노출"으로 할지.
- `category` 값 집합을 느슨한 자유 문자열로 둘지, 별도 허용 목록으로 강제할지(1차는 자유 문자열, 시즌2에 enum화 검토).
- 치장 변형 레이어가 기능 아이템의 `item_id`를 참조하는 경우, 참조 관계를 CSV 별도 컬럼(`ref_item_id` 등)으로 명시해야 하는가 — 현재는 `notes`로만 기술.

## 9. 확장 포인트 (시즌2 이후)

- **새 도메인 추가**: 현재 `A=6,7,8`이 비어 있음. 탈것·펫·악기 등 신규 도메인 추가 시 숫자 하나 배정 + 구간 1000000 확보. 린트의 `RANGE` 테이블과 예약 구간표만 동기 수정.
- **v0.2 JSON 플래그 연동**: `flag_key` 컬럼을 통해 플래그 저장소의 on/off가 리소스팩 노출 여부를 제어하도록 확장. 1차는 컬럼만 선점하고 동작은 미구현.
- **시즌 아카이빙**: `version_deprecated`가 시즌 전환 기준으로 묶이면 과거 시즌 CSV를 `archive/s1/` 하위에 스냅샷으로 떠서 보존.
- **CSV → DB 이관**: 운영 규모가 커지면 본 CSV를 그대로 `custom_model_data_registry` 테이블로 마이그레이션(컬럼 1:1 대응). PK·unique 제약은 R1~R3 규칙과 동치.
- **웹/디스코드 봇 조회**: `domain`·`category`·`status` 필터로 간단 조회 엔드포인트 제공. 1차는 봇 측에서 CSV를 직접 파싱해도 충분.

---

## 다음 작업 제안

1. 본 스펙에 맞춘 **빈 CSV 스켈레톤**(헤더 + 샘플 3행)을 `docs/10_seed_and_config_tables/poro_custom_model_data_registry.csv`로 커밋.
2. 린트 스크립트 R1~R5 구현 위임(implementation-reviewer) — Python/Node 중 운영 친화적 쪽 선택.
3. `suggest-cmd` 유틸 프로토타입(stdout 기반 CLI)으로 1차 검증.
4. v0.2 JSON 플래그 저장소 스펙이 확정되는 시점에 `flag_key` 컬럼의 동작 규약 본절(§8) 재정의.
