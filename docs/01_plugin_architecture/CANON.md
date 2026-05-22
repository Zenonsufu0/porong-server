# 01. 플러그인 아키텍처 — CANON

> **[STATUS: CANON]** — 이 폴더 도메인의 단일 정답 소스.  
> 다른 문서와 충돌 시 이 문서(및 final_master_plan.md §1-3)가 우선.

---

## 역할

플러그인 스택 구성, 모듈 분리 원칙, EmpireRPG Bootstrap 순서, 관리자 명령어 구조의 확정 소스.

이 폴더의 다른 문서들은 계산 참조 또는 구현 초안이며, 충돌 시 이 CANON이 우선합니다.

---

## Canonical Source

| 섹션 | 위치 | 내용 |
|------|------|------|
| 플러그인 스택 | `final_master_plan.md §1` | 사용 플러그인 목록 + 역할 |
| EmpireRPG 핵심 원칙 | `final_master_plan.md §2` | 소유 원칙, 플러그인 간 경계 |
| Bootstrap 순서 | `final_master_plan.md §3` | 8단계 초기화 시퀀스 |
| 관리자 명령어 | `final_master_plan.md §17` | /empire 명령어 전체 스펙 |

---

## 참조 문서

| 문서 | 상태 | 역할 |
|------|------|------|
| `index.md` | DRAFT | 플러그인 구조 개요 (Citizens 충돌 존재) |
| `empire_rpg_design_intent.md` | DRAFT | EmpireRPG 설계 의도 |
| `empire_rpg_module_design.md` | DRAFT | 모듈별 설계 상세 |
| `admin_command_spec.md` | DRAFT | 관리자 명령어 스펙 |
| `final_설계_plan.md` (docs/) | DRAFT→이동예정 | 구현 레퍼런스 (Bootstrap 순서 포함) |

---

## TODO

- [ ] `index.md`에서 Citizens 플러그인 항목 제거 (final_master_plan §1에서 Citizens 제거 확정됨)
- [ ] `final_설계_plan.md` → `01_plugin_architecture/implementation_reference.md` 로 이동 (PHASE 2)
- [ ] Bootstrap 8단계 순서를 이 CANON에 직접 기재
- [ ] 관리자 명령어 (`admin_command_spec.md`) 내용을 CANON으로 통합 또는 참조 정리
- [ ] `empire_rpg_module_design.md` 내용 검증 후 CANON 반영 또는 archive
