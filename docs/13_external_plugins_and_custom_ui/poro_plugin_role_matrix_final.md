# 포로 서버 외부 플러그인 역할 매트릭스 최종판

## 역할 매트릭스

| 플러그인 | 유지 여부 | 핵심 역할 | 하지 말아야 할 역할 |
|---|---|---|---|
| EmpireRPG | 유지 | 서버 핵심 시스템 전부 (상태·대사·판정·보상·해금 단독 주체) | 없음 |
| Citizens | 유지 | NPC 배치/외형/클릭 진입점 | 퀘스트 상태/보상 핵심 처리, 대사 |
| ~~BetonQuest~~ | **제거 (마스터플래닝 확정)** | 없음 | 포로에서 사용 안 함 |
| MythicMobs | 유지 | 몹/보스 외형/스폰/연출·스킬 이펙트 | 결전/극상위 clear/fail 핵심 판정 |
| PacketEvents | 유지 | 패킷 훅(키 이벤트·커스텀 UI 렌더) | 게임 로직 |
| Vault | 유지 | 경제 연결 | 내부 핵심 성장 로직 대체 |
| LuckPerms | 유지 | 권한·플래그 저장(일부) | 게임 데이터 핵심 로직 |
| WorldGuard | 조건부 | 구역 보호(수도 보호·던전 보호·빌드 리전) | 핵심 전투/퀘스트 데이터. per-player WorldBorder 우선 사용 |
| WorldEdit | 유지 | 빌드/편집 (운영·맵 제작용) | 게임 로직 |
| Multiverse-Core | 유지 | 월드 분리(운영/개발/보스 테스트/던전 별도 월드) | 게임 데이터 판정 |
| EssentialsX | 조건부 | 테스트/운영 유틸 | 탐험 동선 무력화 수준의 자유 이동 (`/home`·`/tpa`·`/warp` 운영 상수로 제한) |
| AuraSkills | 제거 | 없음 | 성장/생활/전투 progression 중복 |

## 포로 핵심 원칙

EmpireRPG는 **모든 핵심 판정**(상태·대사·보상·해금)을 단독 처리한다. BetonQuest를 제거하면서 대사·연출·읽기 전용 GUI도 EmpireRPG 커스텀 대화 UI(`DialogueRegistry` / `SessionService` / `TriggerService`)로 통합. Citizens는 NPC 껍데기로만 쓰고 click-to-dialogue는 EmpireRPG `InteractionProfile`로 위임. MythicMobs는 몹/보스 외형·스폰·스킬 연출만 담당하고 clear/fail·드랍 테이블·경매 트리거는 EmpireRPG 전담.

## 운영 체크리스트 (Citizens / MythicMobs / EssentialsX / WorldGuard)

### Citizens 체크리스트
- [ ] NPC 역할표 확정 (시스템형 vs 연출형 분리: 시스템형 = EmpireRPG interaction profile, 연출형 = EmpireRPG 커스텀 대화 UI)
- [ ] 수도 핵심 NPC 배치 seed 작성
- [ ] 지역 안내 NPC 배치 seed 작성
- [ ] 영지/생활 NPC 배치 seed 작성
- [ ] 보스 입장 NPC 배치 seed 작성
- [ ] 인게임 수동 생성보다 seed 자동 생성 우선
- [ ] 클릭 이벤트 중복 방지 (Citizens 기본 traits + EmpireRPG 훅 동시 발화 차단)

### MythicMobs 체크리스트
- [ ] 일반 필드 몹 정의
- [ ] 정예몹 정의
- [ ] 결전 보스 외형 정의
- [ ] 극상위 보스 외형 정의
- [ ] 장판/투사체/소환 연출 스킬 정의
- [ ] clear/fail 판정은 EmpireRPG만 하도록 역할 분리
- [ ] 보스 death event 처리 중복 여부 점검 (MM skill death trigger ↔ EmpireRPG `BossKillEvent`)
- [ ] 보스 ID 매핑표 작성 (MM mob ID ↔ EmpireRPG boss_id)

### EssentialsX 체크리스트
- [ ] 테스트 단계 사용 범위 확인
- [ ] `/home` 제한 여부 결정 (영지 내부 한정 권장)
- [ ] `/back` 제한 여부 결정 (보스전 중 비활성 필수)
- [ ] `/tpa` 제한 여부 결정 (전투·인스턴스 중 차단)
- [ ] `/warp` 제한 여부 결정 (운영 전용)

### WorldGuard / Multiverse 체크리스트
- [ ] 운영 월드 / 개발 월드 / 보스 테스트 월드 분리
- [ ] 수도 보호 구역
- [ ] 보스방 보호 구역
- [ ] 던전/영지 구역 보호
- [ ] 수도 반경 제한은 per-player WorldBorder가 주축, WorldGuard는 PvP·블록 보호 전용

## 한 줄 요약

**Citizens는 NPC 껍데기, MythicMobs는 몹/보스 외형과 연출, EmpireRPG는 모든 핵심 판정·대사·상태·보상을 단독 처리한다.** BetonQuest는 포로에서 제거되었으며, 대사 자산은 EmpireRPG Dialogue Seed로 마이그레이션 진행 중.

## 상위 문서 참조
- 마스터플래닝: `../poro_master_planning.md` (BetonQuest 제거 섹션·커스텀 대화 UI 통합 섹션)
- Citizens NPC seed: `poro_citizens_npc_seed_structure_draft.md`
- 대화 UI 아키텍처: `poro_dialogue_ui_architecture_draft.md`
