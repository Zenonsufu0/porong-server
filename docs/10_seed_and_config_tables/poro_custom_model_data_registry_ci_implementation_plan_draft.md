# 포로 서버 CustomModelData 레지스트리 CSV CI 스크립트 실구현 계획 (1차)

> 작성일: 2026-04-19
> 역할: implementation-reviewer
> 선행 스펙: `docs/10_seed_and_config_tables/poro_custom_model_data_registry_csv_spec_draft.md`
> 본 문서는 스펙에 정의된 R1~R5 린트, `suggest-cmd`, 리소스팩 정합성 검증을 실제로 돌리기 위한 **PR 분해 명세**다. 새 런타임 도입은 최소화하고, 기존 EmpireRPG Gradle 빌드 체인과 정합을 우선한다.

---

## 1. 목표

- `poro_custom_model_data_registry.csv`가 추가·변경될 때 R1~R5를 **PR 단위에서 기계적으로 차단**한다.
- EmpireRPG 플러그인 빌드(`gradlew build`) 경로와 GitHub Actions CI 양쪽에서 동일 린트가 동작한다.
- 리소스팩 빌드 측(현재 리포 외부 가능)도 재사용할 수 있도록 **CSV 1파일 + 스크립트 1 엔트리포인트** 구조를 유지한다.
- 리뷰어가 실패 메시지만 보고 즉시 원인 행·컬럼·권장 수정안을 파악할 수 있는 출력 포맷을 확정한다.
- 과한 인프라(전용 서비스, DB, 별도 리포)는 1차에서 도입하지 않는다.

---

## 2. 선행조건

- 스펙 §3.1 파일 경로 `docs/10_seed_and_config_tables/poro_custom_model_data_registry.csv` 확정. (본 계획에서도 유지)
- 스펙 §6.1 샘플 13행이 그대로 회귀 픽스처로 재사용 가능해야 함.
- EmpireRPG 빌드가 JDK 21 기반(`build.gradle.kts` 확인)이며 Gradle 커스텀 태스크를 추가할 수 있음.
- 리포에 `.github/workflows/` 아직 없음 → 본 계획에서 신규 생성 (CI 부재가 아니라 **작성 공백**이라는 점을 가정).
- 리소스팩 `assets/poro/` 폴더 실체는 아직 이 리포에 존재하지 않음 → R6(에셋 정합성)은 "경로가 존재할 때만 검사"하는 **관대 모드**를 기본값으로 둔다.

---

## 3. 핵심 설계 결정

### 3.1 CSV 최종 배치 경로

- **본 위치**: `docs/10_seed_and_config_tables/poro_custom_model_data_registry.csv` (스펙 그대로 SSOT).
- **플러그인 런타임 참조**: 빌드 시 Gradle `processResources` 단계에서 `custom-plugins/empire-rpg/build/resources/main/seeds/custom_model_data_registry.csv`로 **복사**한다. 원본을 리소스 폴더에 두지 않아 SSOT가 둘로 갈라지는 문제를 차단한다.
- **리소스팩 빌드 참조**: 리소스팩 리포가 분리되면 git submodule 또는 CI artifact 방식으로 끌어온다(1차에서는 오픈 질문으로 남김).

루트·리소스 폴더 직접 배치는 모두 탈락. 근거: 스펙 §3.1과 정합, 기획 문서 리뷰 흐름(PR diff 위주)이 `docs/` 하위에서 일관됨.

### 3.2 Lint 스크립트 언어 선택

**권장: Python 3.11 단일 스크립트**, 실행은 Gradle 커스텀 태스크 + GitHub Actions 양쪽에서 얇게 래핑.

| 후보 | 장점 | 탈락 사유 |
|-----|------|---------|
| Bash | 진입 장벽 낮음 | R3·R5 같은 상태 추적·정규식 검증이 가독성 급락. 7자리 정수·semver 파싱 난도 높음. |
| **Python 3.11** | 표준 `csv`·`re`·`pathlib` 모듈로 무의존. Actions 러너·WSL 양쪽 기본 설치. 리뷰어가 읽기 쉬움. | (채택) |
| Node.js | 생태계 풍부 | 의존성(`package.json`) 신설이 부담. EmpireRPG 체인과 무관한 런타임 추가. |
| Gradle(Kotlin) 단독 | 빌드 체인 통합 최강 | CSV 파싱 라이브러리 의존성 필요(Apache Commons CSV 등). 리소스팩 측이 Gradle 없이도 호출하려면 결국 외부 스크립트 필요. |

**결론**: Python으로 린트 본체를 작성하고, Gradle은 `Exec` 태스크로 `python3 tools/cmd_registry_lint.py ...`를 호출하는 **얇은 래퍼**만 둔다. 러너가 Python 미설치 시를 대비해 Actions에서 `actions/setup-python@v5` 1줄로 고정한다.

### 3.3 에러 리포팅 포맷

```
[DUP_CMD] row=14 col=customModelData value=1111000
  previous: row=8 (item_id=poro:weapon/warrior/greatsword_t1_s1)
  current : row=14 (item_id=poro:weapon/warrior/greatsword_t1_alt)
  hint    : suggest-cmd --domain=weapon --category=warrior_greatsword_t1 → 1111001
```

고정 필드: `[CODE] row=N col=X value=V`, 2~3줄 본문(이전 행·현재 행·힌트). 종료 코드는 에러 1건 이상 시 `1`, 경고만 있으면 `0`+stderr 메시지.

---

## 4. 작업 분해 (PR 분해)

각 PR은 **독립 머지 가능**하도록 순차 의존만 남긴다. man-day는 실 코딩+리뷰 반영 기준.

### PR #1 — CSV 스켈레톤 + 샘플 시드 커밋 (0.5 md)

- 신규: `docs/10_seed_and_config_tables/poro_custom_model_data_registry.csv` (스펙 §6.1 13행 그대로).
- 변경: `docs/10_seed_and_config_tables/poro_custom_model_data_registry_csv_spec_draft.md` 상단에 "실물 CSV 위치" 한 줄 링크.
- 빌드 체인 영향: 없음.
- 테스트: 헤더 11 컬럼 수·UTF-8·LF·오름차순 수동 확인.
- 의존성: 없음. (이 PR이 나머지 모든 PR의 전제)

### PR #2 — Python 린트 본체 R1~R3 구현 (1.5 md)

- 신규: `tools/cmd_registry_lint/__init__.py`, `tools/cmd_registry_lint/__main__.py`, `tools/cmd_registry_lint/rules.py`.
- 신규: `tools/cmd_registry_lint/tests/test_rules_core.py` (R1~R3 positive/negative 각 1건 = 6 케이스).
- 신규: `tools/cmd_registry_lint/tests/fixtures/` 아래 오염 CSV 5종(스펙 §7 회귀 세트 중 3종).
- 빌드 체인 영향: 없음(Python 독립 실행).
- 테스트: `python3 -m tools.cmd_registry_lint docs/10_seed_and_config_tables/poro_custom_model_data_registry.csv` 로컬 확인.
- 의존성: PR #1.

### PR #3 — R4(예약 구간) + R5(enum·semver) 확장 (1.0 md)

- 변경: `tools/cmd_registry_lint/rules.py`에 `check_range`, `check_enum_and_semver` 추가.
- 변경: `tools/cmd_registry_lint/tests/test_rules_core.py`에 R4·R5 케이스 4건 추가.
- 신규: `tools/cmd_registry_lint/ranges.py` — 도메인 구간 상수 테이블(스펙 §3.3 그대로). 신규 도메인 추가 시 이 파일만 수정.
- 빌드 체인 영향: 없음.
- 테스트: 스펙 §7 오염 케이스 5종 전체 회귀 통과.
- 의존성: PR #2.

### PR #4 — 에러 리포트 포맷터 + CLI UX (0.5 md)

- 변경: `tools/cmd_registry_lint/__main__.py`에 §3.3 포맷 적용, `--format=human|json` 옵션 추가.
- 변경: `--summary` 플래그(에러·경고 합계만 출력, CI 서머리용).
- 신규 테스트: `test_report_format.py` — 고정된 오염 CSV에 대한 출력 스냅샷 비교.
- 빌드 체인 영향: 없음.
- 의존성: PR #3.

### PR #5 — Gradle 커스텀 태스크 `lintCmdRegistry` (1.0 md)

- 변경: `custom-plugins/empire-rpg/build.gradle.kts`에 `tasks.register<Exec>("lintCmdRegistry") { ... }` 추가.
- 변경: 같은 파일에 `tasks.named("check") { dependsOn("lintCmdRegistry") }` — `gradle check`/`build`가 자동으로 린트를 태움.
- 변경: `processResources` 시 CSV를 `seeds/custom_model_data_registry.csv`로 복사하는 `from(rootProject.file("docs/10_seed_and_config_tables/poro_custom_model_data_registry.csv"))` 설정.
- 빌드 체인 영향: **핵심**. `gradlew build` 시 Python 필요. README 또는 플러그인 루트 `HACKING.md`에 `python3 --version` 전제 1줄 추가.
- 테스트: 로컬 `./gradlew :empire-rpg:lintCmdRegistry` / 오염 CSV 임시 투입 시 빌드 실패 확인.
- 의존성: PR #4.

### PR #6 — GitHub Actions 워크플로 (0.5 md)

- 신규: `.github/workflows/cmd-registry-lint.yml`.
- 트리거: `pull_request` + 경로 필터(`docs/10_seed_and_config_tables/poro_custom_model_data_registry.csv`, `tools/cmd_registry_lint/**`).
- 스텝: `actions/checkout@v4` → `actions/setup-python@v5 (3.11)` → `python3 -m tools.cmd_registry_lint <csv> --format=human` → 실패 시 Job Summary에 `--format=json` 결과 첨부.
- 빌드 체인 영향: 없음(독립 잡).
- 테스트: 의도적 오염 PR을 드래프트로 열어 적색 확인 후 닫기.
- 의존성: PR #4. (PR #5와 병렬 가능)

### PR #7 — `suggest-cmd` CLI 프로토타입 (1.0 md)

- 신규: `tools/cmd_registry_lint/suggest.py` + `__main__` 엔트리.
- 기능: `python3 -m tools.cmd_registry_lint.suggest --domain=weapon --category=warrior_greatsword_t1_s1` → stdout에 다음 빈 CMD 1개.
- 기능: 인코딩 규칙(`ABCDDEE`) 중 `A`·`B`·`C` 고정 후 `DD`·`EE` 최소 빈 값 탐색. 카테고리 미지정 시 해당 도메인 전역 최소 빈 값.
- 빌드 체인 영향: 없음(수동 호출 전용).
- 테스트: 샘플 CSV 기준 weapon/cosmetic/ui 각 1건 스모크.
- 의존성: PR #3. (PR #5·#6과 병렬 가능)

### PR #8 — 리소스팩 에셋 정합성 검증 (관대 모드) (1.0 md)

- 변경: `tools/cmd_registry_lint/rules.py`에 R6(asset 존재) 추가. `--assets-root=<path>` 옵션이 주어졌을 때만 검사.
- 동작: `status∈{designed, implemented}` 행의 `asset_path`가 `<assets-root>/poro/<asset_path>`로 존재하는지 확인. 부재 시 에러, 반대 고아 에셋은 경고.
- 변경: Actions에서 리소스팩 경로가 아직 없으므로 스킵(옵션 미지정). 미래 리소스팩 리포 연동 시 이 옵션만 붙이면 활성화.
- 빌드 체인 영향: 리소스팩 리포 존재 시에만. 현 리포 단독 CI는 영향 없음.
- 테스트: 로컬에 더미 `tmp/assets/poro/models/...` 폴더 만들어 존재/부재 케이스 각각 확인.
- 의존성: PR #3.

### 총량·의존성 요약

```
PR#1 (0.5) ── PR#2 (1.5) ── PR#3 (1.0) ─┬─ PR#4 (0.5) ─┬─ PR#5 (1.0)
                                        │              └─ PR#6 (0.5)
                                        ├─ PR#7 (1.0)
                                        └─ PR#8 (1.0)
```

- 총 PR: 8개
- 총 man-day: 7.0 md (리뷰 왕복 +20% 버퍼 포함 시 8.4 md)
- 크리티컬 패스: PR#1 → #2 → #3 → #4 → #5 = 4.5 md

---

## 5. 핵심 스크립트 스니펫 (설계 수준, 10~20줄)

### 5.1 R1 중복 CMD 감지

```python
def check_duplicate_cmd(rows):
    seen, errors = {}, []
    for i, row in enumerate(rows, start=2):  # 2 = 헤더 다음 첫 행
        cmd = row["customModelData"]
        if cmd in seen:
            errors.append(LintError(
                code="DUP_CMD", row=i, col="customModelData", value=cmd,
                prev_row=seen[cmd], hint=f"suggest-cmd --domain={row['domain']}"
            ))
        else:
            seen[cmd] = i
    return errors
```

### 5.2 R4 예약 구간 이탈 검증

```python
def check_reserved_range(rows, ranges):
    errors = []
    for i, row in enumerate(rows, start=2):
        lo, hi = ranges[row["domain"]]
        cmd = int(row["customModelData"])
        if not (lo <= cmd <= hi):
            errors.append(LintError(
                code="OUT_OF_RANGE", row=i, col="customModelData", value=cmd,
                hint=f"domain={row['domain']} requires [{lo},{hi}]"
            ))
    return errors
```

### 5.3 `suggest-cmd` 다음 빈 번호 탐색

```python
def suggest_next_cmd(rows, domain, category, ranges):
    used = {int(r["customModelData"]) for r in rows if r["domain"] == domain}
    lo, hi = ranges[domain]
    prefix = derive_prefix(domain, category)  # ABC 3자리
    base = prefix * 10000  # DDEE 자리를 0으로
    for offset in range(10000):
        cand = base + offset
        if lo <= cand <= hi and cand not in used:
            return cand
    raise NoSlotAvailable(domain, category)
```

---

## 6. 기술 리스크

- **Python 런타임 가정**: 개발자 로컬·Actions 러너·WSL 모두에서 `python3` 동작 필요. WSL은 기본 설치돼 있으나, 최소 3.11 요구를 README에 명시하지 않으면 구버전(3.8)에서 타입 힌트 문법 차이로 실패 가능. 완화: `from __future__ import annotations` + 3.9 이상 문법만 사용.
- **CSV 파서 엣지 케이스**: `notes` 필드의 쉼표·줄바꿈·큰따옴표 이스케이프. 완화: Python 표준 `csv.DictReader(dialect="excel")` 사용 + 각 행에 개행 들어간 픽스처 테스트.
- **Gradle ↔ Python 경계**: `Exec` 태스크가 Windows에서 `python` vs `python3` 차이로 실패. 완화: Gradle에서 `OperatingSystem.current()` 분기 또는 `python`/`python3`/`py -3` 순차 탐색 헬퍼.
- **SSOT 복제 위험**: PR #5의 `processResources` 복사 경로가 원본과 엇나가면 플러그인이 오래된 CSV를 들고 다닐 수 있다. 완화: `Copy` 태스크에 `inputs.file(rootProject.file(...))`를 명시해 변경 감지 실패 방지.
- **대용량 처리**: 10000행 가정은 Python 표준 파서로 수백 ms 내 처리 가능. 별도 최적화 불요(현재 13행 → 1년 후 예상 수백 행 수준).
- **리소스팩 리포 분리 이슈**: R6(에셋 정합성)이 활성화되려면 리소스팩 경로를 CI에 전달해야 함. 1차 관대 모드로 미루되, 리포가 분리되는 시점에 워크플로 1개를 **cross-repo dispatch**로 추가하는 방향을 남겨둔다.
- **리뷰어 피로도**: 에러가 한 번에 20건 이상 뜨면 리포트가 길어진다. 완화: `--summary` 플래그로 상위 10건만 표시 + 총계 안내.

---

## 7. 테스트 포인트

- **유닛**: R1~R5 각각 positive 1건 + negative 1~2건(총 12케이스). PR #3 종료 시점까지 전부 초록.
- **회귀 픽스처**: 스펙 §7의 오염 케이스 5종을 `tools/cmd_registry_lint/tests/fixtures/`에 고정. CI에서 `pytest -q` 1회 실행.
- **출력 스냅샷**: PR #4에서 고정 CSV 입력에 대한 human/json 출력 골든 파일 비교. 포맷이 바뀌면 강제 리뷰.
- **Gradle 통합**: PR #5에서 로컬 `./gradlew :empire-rpg:lintCmdRegistry` 정상/실패 2케이스 수동. 가능하면 `testkit` 말고 README에 절차만 남겨 과투자 회피.
- **Actions E2E**: PR #6 머지 전, 의도적 오염 PR을 드래프트로 열어 적색 확인.
- **`suggest-cmd` 스모크**: weapon·cosmetic·ui 각 1건씩. 결과가 스펙 §3.3 예약표와 충돌 안 하는지 리뷰어가 수동 확인.
- **에셋 정합성**: PR #8에서 더미 `assets/poro/` 폴더 2~3 경로로 존재/부재 각 1건.

---

## 8. 1차 버전 권장 구성

**최소 출하 세트 = PR #1 + #2 + #3 + #4 + #6 (3.5 md)**.

이 조합만으로:
- CSV가 리포에 존재한다.
- R1~R5가 로컬·CI에서 자동 검증된다.
- PR 리뷰어는 고정 포맷 에러 메시지로 충돌을 즉시 파악한다.

Gradle 태스크(PR #5)와 `suggest-cmd`(PR #7)는 **편의 기능**이라 2차로 미뤄도 안전하다. 리소스팩 에셋 정합성(PR #8)은 리소스팩 리포가 분리·실물화되는 시점까지 미뤄둔다.

---

## 9. 나중으로 미뤄도 되는 것

- **DB 이관**: 스펙 §9의 `custom_model_data_registry` 테이블. 운영 규모가 커지기 전까지 CSV로 충분.
- **웹/디스코드 봇 조회 엔드포인트**: 스펙 §9 항목. 본 CI 계획 범위 밖.
- **자동 번호 예약 API**: 현재는 수동 PR + `suggest-cmd` CLI로 커버. 동시 PR 병합 충돌 빈도가 체감 수준 이상으로 올라가기 전까지 불필요.
- **deprecated 회수 린트 강화**: 스펙 §6.4.2의 "시즌 1개 유예" 검증을 자동화. 시즌 경계 규칙 확정 이후로 미룸.
- **리소스팩 리포 cross-repo CI**: 리포 분리 결정 후.
- **R4 무기 도메인 `B·C·DD·EE` 세부 인코딩 강제**: 1차는 구간(R4)만 검사. 세부 인코딩 린트는 무기 프로토타입이 실제로 3개 이상 쌓이면 도입(오픈 질문 §8의 `category` enum화와 동시 결정).

---

## 오픈 질문

- Actions 러너에서 Python 3.11을 고정 버전으로 둘지, `>=3.9` 범위를 허용할지.
- Gradle `check` 체인에 기본 포함 vs 별도 태스크로만 노출 — 로컬 `./gradlew build` 소요 시간 민감도에 따라 결정.
- `suggest-cmd`를 Discord 봇 슬래시 커맨드로 노출할지 여부(스펙 §9 웹·봇 조회 항목과 묶어 결정).
- CSV 파일이 `docs/` 아래 있어 문서 PR과 코드 PR이 한 리뷰에서 섞이는 문제를 용인할지, `data/` 최상위로 승격할지.

---

## 다음 추천 작업

1. PR #1 스켈레톤 커밋(스펙 §6.1 샘플 그대로).
2. PR #2 착수 전 `tools/` 최상위 디렉터리 정책(독립 스크립트 모음)이 맞는지 user 확인.
3. Actions 러너 Python 버전 정책(3.11 고정 vs 범위)을 오픈 질문으로 확정.
