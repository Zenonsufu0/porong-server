# 08. 리소스팩 / 치장 무기 제작 파이프라인

> **[STATUS: REFERENCE]** — 리소스팩과 GUI 제작 파이프라인 참조. 에셋 작업 경로는 루트 `AGENTS.md`와 `CLAUDE.md`가 우선.

## 관련 문서

- [GUI 제작 전체 체크리스트](gui_todo_list.md) — 배경 PNG·아이콘 제작 목록, 화면별 설계 상태, 슬롯 배치 빠른 참조
- [GUI 비트맵 배경 스펙](gui_bitmap_spec.md) — Custom Font Bitmap 방식, 인벤토리 배경 PNG 연동 구조
- [GUI 허브 구조 설계](gui_hub_structure.md) — 메인 허브 6구역 3×3, 서브 허브 4종, 보스 방 시스템, 접근 제한 정책 (DL-073)
- [GUI 장비 패널 설계](gui_equipment_panel.md) — 54슬롯 장착·치장·외형 토글·스탯 배분 패널
- [2D 스킬/보스 이펙트 리소스팩 통합 설계](skill_effect_2d_integration_v1.md) — 배경 제거된 13개 이펙트 PNG를 ItemDisplay 빌보드 + custom_model_data(400xxx)로 인게임 연출 통합하는 설계(DRAFT)

> GUI 세부 설계는 최신 커밋 기준으로 전체 완료 상태다. 배경 PNG 4개(menu_main/equipment/territory/boss.png)는 현행 구현에서 **미사용** — 모든 허브 GUI가 색상 유리판 + 중앙 아이콘 방식으로 전환됨(DL-073). 나머지 GUI는 chest + 유리판/바닐라 아이템/lore 구성.

---

## 1차 스타일

깔끔한 RPG풍. 다크판타지, 고어, 공포 톤은 지양한다.

## 우선순위

1. 재료 아이콘
2. GUI 아이콘
3. 영지/가공기 GUI
4. 마인팜 기계 외형
5. 무기 6종 치장
6. 날개 치장 1종

## 제외

- 고퀄 보스 모델
- 도감 GUI
- 외부 몬스터 모델

## 역할 분담

| 도구 | 역할 |
|---|---|
| Claude/Claude Design | 구조 설계/검수/문서 |
| Figma | GUI/아이콘/패널 |
| GPT 이미지 | 컨셉/텍스처 초안 |
| Blockbench (사용자 수동) | 모델/UV/display 확인 및 확정 |
| Codex/MCP | json, 폴더, CustomModelData, registry 연결, geometry/texture/UV 동기화 |

## 원칙

무기/기계는 2D~2.5D 중심. 보스 모델은 1차에서 바닐라 강화형으로 처리한다.

---

## 경로 기준 (변경 불가)

| 구분 | 경로 | 역할 |
|---|---|---|
| **WSL source (.bbmodel 작업 중 원본)** | `~/dev/poro-server/assets/source/items/weapons/_season1_2_5d/<카테고리>/<asset_id>/source/<asset_id>.bbmodel` | **작업 중 원본 (source of truth)** |
| Blockbench Save As (UNC) | `\\wsl.localhost\Ubuntu\home\zenonsufu1\dev\poro-server\assets\source\items\weapons\_season1_2_5d\...` | Blockbench에서 직접 저장하는 경로 |
| C드라이브 백업 (수정 금지) | `C:\Users\User\Project\poro-assets-work\_season1_2_5d` | 최종 완료 후 보관 전용 |
| WSL export 출력 | `~/dev/poro-server/assets/export/resourcepack/` | 리소스팩 빌드 결과 |

> **원칙 (2026-05-10 정책 변경)**
> - `.bbmodel`의 작업 중 원본(source of truth)은 **WSL source 경로**다.
> - Blockbench에서 UNC 경로(`\\wsl.localhost\Ubuntu\...`)로 직접 Save As하면 WSL 파일이 갱신된다.
> - C드라이브 경로는 최종 완료 후 백업/보관 전용이며, 작업 중 직접 수정하지 않는다.
> - rsync 단계는 더 이상 불필요하다.

---

## WSL source 운용 방식

### 이유

Blockbench는 `\\wsl.localhost\Ubuntu\...` UNC 경로를 통해 WSL 파일에 직접 Save As 가능하다.
이를 활용하면 Windows 네이티브 앱인 Blockbench에서 직접 WSL source 경로에 저장할 수 있어
rsync 동기화 단계가 불필요하고, source/export 불일치 문제가 사라진다.

### 흐름

```
[Blockbench (Windows 앱)]
        │
        │  File → Save As → \\wsl.localhost\Ubuntu\... (UNC 경로 직접 저장)
        ▼
[WSL source .bbmodel]  ← 작업 중 원본 (source of truth)
        │
        │  export/register Phase
        ▼
[WSL export/resourcepack]  ← 리소스팩 빌드 결과
        │
        │  작업 완전 완료 후 (선택적 백업)
        ▼
[C드라이브 백업]  ← 보관 전용, 수정 금지
```

---

## WSL → Windows 작업 폴더 복사

전체 무기 폴더 동기화:

```bash
mkdir -p /mnt/c/Users/User/Project/poro-assets-work/_season1_2_5d

cp -a \
  ~/dev/poro-server/assets/source/items/weapons/_season1_2_5d/. \
  /mnt/c/Users/User/Project/poro-assets-work/_season1_2_5d/
```

특정 에셋만 복사 (예: scythe_adventurer_poro_01):

```bash
cp -a \
  ~/dev/poro-server/assets/source/items/weapons/_season1_2_5d/05_scythe/scythe_adventurer_poro_01/. \
  "/mnt/c/Users/User/Project/poro-assets-work/_season1_2_5d/05_scythe/scythe_adventurer_poro_01/"
```

---

## Windows 작업본 → WSL source 반영

전체 폴더 반영:

```bash
cp -a \
  /mnt/c/Users/User/Project/poro-assets-work/_season1_2_5d/. \
  ~/dev/poro-server/assets/source/items/weapons/_season1_2_5d/
```

특정 에셋만 반영 (예: scythe_adventurer_poro_01):

```bash
cp -a \
  "/mnt/c/Users/User/Project/poro-assets-work/_season1_2_5d/05_scythe/scythe_adventurer_poro_01/". \
  ~/dev/poro-server/assets/source/items/weapons/_season1_2_5d/05_scythe/scythe_adventurer_poro_01/
```

별도 저장 위치에서 낫 수동 저장본을 WSL source로 반영하는 예시:

```bash
# .bbmodel 반영
cp "/mnt/c/Users/User/Project/포로서버 에셋/낫/scythe_adventurer_poro_01.bbmodel" \
  ~/dev/poro-server/assets/source/items/weapons/_season1_2_5d/05_scythe/scythe_adventurer_poro_01/source/scythe_adventurer_poro_01.bbmodel

# texture png 반영
cp "/mnt/c/Users/User/Project/포로서버 에셋/낫/scythe_adventurer_poro_01.png" \
  ~/dev/poro-server/assets/source/items/weapons/_season1_2_5d/05_scythe/scythe_adventurer_poro_01/textures/scythe_adventurer_poro_01.png
```

---

## Blockbench 저장 주의사항

1. Blockbench에서 반드시 **WSL source 경로**에 UNC 경로로 Save As한다.
   - 경로 형식: `\\wsl.localhost\Ubuntu\home\zenonsufu1\dev\poro-server\assets\source\items\weapons\_season1_2_5d\<카테고리>\<asset_id>\source\<asset_id>.bbmodel`
2. C드라이브 경로(`C:\Users\User\Project\poro-assets-work\...`)에는 작업 중 직접 저장하지 않는다.
3. rsync 없이 UNC 경로로 직접 저장하면 WSL source가 즉시 갱신된다.
4. Blockbench 포맷은 반드시 **Minecraft Java Block/Item** 이어야 한다.
   - 정상 UI: `Edit / Paint / Display` 탭이 보여야 한다.
   - **비정상:** `Edit / Paint / Animate` 탭만 보이면 Generic/Animation 계열 포맷 → 즉시 `Convert Project`로 복구 또는 정상 Java item `.bbmodel` 템플릿 기준으로 재작성.
5. Display 조정은 사용자가 직접 **Blockbench Display 탭**에서 눈으로 조정한 뒤 저장한 값을 기준으로 한다.

---

## MCP/Claude/Codex 작업 금지사항

> 이 항목을 어기면 "source에 없는 모델"이 export되는 심각한 파이프라인 오염이 발생한다.

- **새 임시 Blockbench 프로젝트를 기준으로 export하지 말 것.**
- 저장되지 않은 탭(임시 탭) 기준으로 export하지 말 것.
- WSL source에 반영되지 않은 Windows 임시 파일을 기준으로 export하지 말 것.
- GUI/display 값을 자동으로 추정해서 최종 확정하지 말 것.
- `active` 등록은 인게임 테스트 전까지 하지 말 것.
- Codex/Claude는 **geometry/texture/UV/export 동기화와 값 반영**을 담당한다. display 확정은 사용자 몫이다.

---

## 무기 형태별 geometry 전략

직선 무기 (검, 창, 도끼):
- geometry 중심으로 제작한다.
- blade, shaft, guard, handle 모두 큐브 geometry로 표현한다.

곡선 무기 (낫 등):
- **자루/소켓/그립**: geometry (큐브) 중심으로 처리한다.
- **곡선 날(blade)**: alpha texture plane 방식을 사용할 수 있다.
  - blade의 곡선 실루엣이 geometry로 계단식 근사만으로 부족할 경우,
    transparent PNG(alpha channel)를 사용한 평면 plane element로 표현한다.
  - blade_plane_main 1개 element에 초승달 형태 alpha PNG를 UV 매핑하는 방식.
  - 이 경우 textures/png는 날 곡선 실루엣을 포함한 64×64 이상 alpha PNG여야 한다.
- 낫처럼 blade 곡선이 핵심 실루엣인 무기는 이 예외 규칙을 우선 적용한다.

---

## export/register 전 체크리스트

Phase 5(bb-export-register) 진입 전 반드시 확인:

- [ ] 1. Blockbench에서 WSL source UNC 경로로 Save As하여 `.bbmodel`이 저장되었는가?
- [ ] 2. WSL source의 `.bbmodel`을 읽었을 때 실제 모델 데이터가 보이는가?
- [ ] 3. `.bbmodel` 포맷이 **Java Block/Item**이며 `Display` 탭이 보이는가?
- [ ] 4. `source/` 폴더 또는 `textures/` 폴더의 texture `.png`가 존재하는가?
- [ ] 5. `export/resourcepack`에 model json과 texture png가 반영되었는가?
- [ ] 6. item definition의 CMD가 올바른 모델을 가리키는가?
- [ ] 7. resourcepack zip에 실제 파일이 포함되었는가?
- [ ] 8. 인게임 테스트 후에만 registry status를 `active`로 바꿀 것.

---

## scythe_adventurer_poro_01 — Phase 5 재진행 지시 (예시)

사용자가 Windows에서 `.bbmodel`과 `.png`를 수정한 뒤 WSL source로 복사한 상태라면,
아래 순서로 Phase 5를 재진행한다.

### 1) WSL source 복사 확인

```bash
# 낫 수동 저장본 → WSL source 반영
cp "/mnt/c/Users/User/Project/포로서버 에셋/낫/scythe_adventurer_poro_01.bbmodel" \
  ~/dev/poro-server/assets/source/items/weapons/_season1_2_5d/05_scythe/scythe_adventurer_poro_01/source/scythe_adventurer_poro_01.bbmodel

cp "/mnt/c/Users/User/Project/포로서버 에셋/낫/scythe_adventurer_poro_01.png" \
  ~/dev/poro-server/assets/source/items/weapons/_season1_2_5d/05_scythe/scythe_adventurer_poro_01/textures/scythe_adventurer_poro_01.png
```

### 2) Phase 5 bb-export-register 지시

```
bb-export-register 실행:

- source: source/scythe_adventurer_poro_01.bbmodel (WSL source 기준)
- texture: textures/scythe_adventurer_poro_01.png (WSL source 기준)
- base item: minecraft:netherite_hoe
- CMD: 100502
- namespace: poro
- model_path: poro:item/weapons/scythe_adventurer_poro_01
- export 대상: assets/export/resourcepack/assets/poro/models/item/weapons/scythe_adventurer_poro_01.json
- texture export: assets/export/resourcepack/assets/poro/textures/item/weapons/scythe_adventurer_poro_01.png
- netherite_hoe.json: 100501 보존 + 100502 추가
- status: export_ready  ← active 처리 금지, 인게임 테스트 후 변경
```

### 3) display 값

Codex/Claude는 display 값을 자동 확정하지 않는다.  
사용자가 Blockbench Display 탭에서 조정·저장한 값이 `.bbmodel`에 내장되어 있으면 그 값을 그대로 반영한다.  
없으면 `display` 블록을 빈 상태로 두거나 참고용 carnivoret 값(notes.md 기록)을 주석으로 남긴다. 자동 확정 금지.

---

## 인게임 테스트 명령어

scythe_adventurer_poro_01 (CMD 100502):

```
/minecraft:give @s minecraft:netherite_hoe[minecraft:custom_model_data={strings:["100502"]}] 1
```

> `minecraft:give` 형식을 사용한다 (단순 `/give` 아님).

---

## 리소스팩 압축 및 탐색기 열기

```bash
cd ~/dev/poro-server/assets/export/resourcepack

rm -f ~/dev/poro-server/tmp/poro-resourcepack-test.zip
mkdir -p ~/dev/poro-server/tmp

zip -r ~/dev/poro-server/tmp/poro-resourcepack-test.zip pack.mcmeta assets

explorer.exe "$(wslpath -w ~/dev/poro-server/tmp)"
```

압축 후 Windows 탐색기에서 `poro-resourcepack-test.zip`을 확인하고 서버에 배포한다.

---

## Phase 워크플로우 요약

```
[Phase 0] 스캐폴드 생성
    ↓
[Phase 1] bb-ref-curator / bb-asset-brief
    레퍼런스 분석, asset_brief.md, ref_spec.json
    ↓
[Phase 2] 설계 확정 (bb-asset-brief 검토)
    ↓
[Phase 3] bb-build-blockout
    ├── 직선무기: geometry 중심
    └── 곡선무기(낫): shaft/socket/grip = geometry, blade = alpha plane
    ↓
[Phase 4] bb-texture-pass
    사용자가 Windows Blockbench에서 texture 최종 확인·저장
    → Windows 작업본 → WSL source 복사
    ↓
[Phase 5] bb-export-register
    체크리스트 확인 → WSL source 기준 export
    status: export_ready
    ↓
[인게임 테스트]
    /minecraft:give 명령어로 확인
    ↓
[status: active 변경]  ← 테스트 통과 후에만
```
