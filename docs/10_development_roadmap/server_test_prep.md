# 서버 테스트 진입 체크리스트

> **[STATUS: ACTIVE]** — 서버 테스트 진입 전 처리해야 할 선행조건. INBOX-005 2차 감사(2026-05-31) + config/MM 점검(2026-05-31) 기반.
> 코드 블로커(#3~#10)는 DL-091~093으로 src 수정 완료. 이 문서는 **배포·서버 설정·맵** 선행조건을 다룬다.

---

## 0. 🔴 최우선 — 빌드/배포 동기화 (런타임이 stale)

런타임(`server/plugins/EmpireRPG/`)이 **옛 jar + 옛 seed**로 돌고 있다. 플러그인은 `saveResource(path, false)`로 seed를 추출하는데 `replace=false`라 **기존 seeds 폴더가 있으면 갱신하지 않는다.** 따라서 jar만 바꿔도 seed는 그대로다.

- [ ] **플러그인 jar 재빌드** — 이번 세션 코드 수정(#3 보스 보상 브리지, #4 원샷 클램프, #5 게이트, #6 ATK flat, #7 피해 공식, #10 타임아웃) + DL-086~090이 src에만 있음. 빌드 필요.
- [ ] **런타임 stale seed 제거** — `server/plugins/EmpireRPG/seeds/`가 DL-087 이전(boss_master에 void_herald·최종보스 없음, 옛 시즌3 earth_tyrant 등). **폴더를 비우고 재시작**해야 jar에서 최신 seed 재추출. (또는 src seed를 직접 복사)
  - 확인된 stale: `boss_master.csv`(로스터), `growth_enhancement_table.csv`(DL-086), `growth_potential_option_pool.csv`, `item_master.csv`. 그 외 estate_*·quest_*·life_* 등은 런타임에 존재하나 src 변경분 미반영 가능 — 전체 재추출 권장.
- [ ] **재빌드 jar를 `server/plugins/`에 배치 + 재시작**, 부팅 로그에서 8개 bootstrap 완료 + 12개 마이그레이션 통과 확인.

> ⚠️ `server-config/empire-rpg/seeds/`도 stale·불완전(4개만 존재, 대부분 누락)이나 이건 스테이징 사본 — 런타임 경로는 `server/plugins/EmpireRPG/seeds/`다. 배포 파이프라인이 어느 쪽을 쓰는지 확정 필요.

---

## 1. 🔴 월드·맵·좌표 (config.yml)

정본 월드(`final_master_plan §5`): `world_main`(수도·필드5·훈련장) / `world_farm`(영지) / `world_boss`(보스 인스턴스) / `world_test`. 현재 `config.yml`은 **전부 `world` + 더미 좌표**.

- [ ] 월드 4종 생성 (`world_main`/`world_farm`/`world_boss`/`world_test`).
- [ ] 맵 배치 (수도·필드5·보스 아레나).
- [ ] `fields` (plain·mine·sewer·outpost·ruins): `world_main` + 실제 좌표 5개.
- [ ] `field-bosses` (동일 5종): `world_main` + 실제 스폰 좌표 5개.
- [ ] `boss-room-slots` (29슬롯, 10000+ 그리드는 설계됨): `world_boss`로 월드명 교체 + `/empire genrooms` 실행(해당 좌표에 방 생성).
- [ ] `pvp-room-slots`: 해당 월드 + `/empire-genarenas` 실행.
- [ ] `season-start-epoch`: 실제 시즌 시작(예: 2026-06-01 = 1748736000). 현재 0 → 항상 1주차.
- [ ] `api-secret-key`: Discord 봇/웹 연동 시 설정(로컬만이면 비워둬도 API 503으로 안전).
- [ ] `debug: true` → 운영 시 false 고려.

---

## 2. 🔴 MythicMobs 셸 (server-config/mythicmobs)

- [x] **ID 충돌 해소** (2026-05-31, DL-093 점검): `field_outpost.yml`의 `Fallen_Knight`(필드4) → `Outpost_Knight`로 리네임(+표시명 `초소 기사장`). season_bosses의 `fallen_knight`(시즌1)와 대소문자 충돌 제거. boss_master(src) `outpost_knight` 정합.
- [ ] **MM 셸 배포**: `server-config/mythicmobs/`를 런타임 `server/plugins/MythicMobs/`에 배포(현재 ExampleMobs 등 기본값만 존재). 보스/필드몹 셸 yml 설치 필수 — 없으면 전투/보스/필드 리스너 통째 미등록.
- [x] season_bosses.yml 보스 내부명 9종 = boss_master 시즌+최종 id 정확히 일치 (검증됨).
- [ ] MM 보스 Health/Armor 확인: 피해 공식 DEF항은 1차 시즌 바닐라 armor에 위임(DL-092) — 보스 armor가 의도대로 설정됐는지.

---

## 3. 🟠 기획 확정 대기 (런타임 전 결정)

- [ ] 잠재(큐브) 등급 확률 모델 3원 분기 확정 (INBOX-005 #9) — 코드 승급확률 vs drop_tables 플랫 vs spec memorial.

---

## 4. 🟡 후속 (서버 테스트 차단 아님)

- [ ] boss_damage_increase 보스 판정 배선 (DL-092 후속).
- [ ] 보스 클리어 기록 영속화 (현재 in-memory, 재시작 소실 — #5 게이트 영향).
- [ ] 보스 페이즈/패턴 진행 틱 (#10 후속).
- [ ] 온보딩 영지 생성 트리거, `/캐릭터`·`/작물` 라우팅, SQLite busy_timeout/WAL.

---

## 5. 첫날 스모크 테스트 (배포·맵 완료 후)

1. 부팅 로그: bootstrap 9줄 + `MythicMobs detected` + 마이그레이션 통과.
2. 신규 유저: 무기 선택 → 스타터 장비 → (영지 진입).
3. 메뉴 6존 전수 클릭.
4. 강화 1회(골드·강화석 차감·로그) + 흔적 토글(+10강).
5. 영지: 공방 제작 → 오프라인 후 재접속 자동 입금.
6. **보스룸 입장 → 처치 → 보상 지급 + 슬롯 회수**(#3 검증).
7. **고강 유저 필드보스 1타 → 15% 이상 잔존**(#4 검증).
8. **보스룸 15분 방치 → 타임아웃 종료**(#10 검증).
9. **보스6 미클리어 상태 최종보스 입장 시도 → 차단**(#5 검증).
10. **잠재·치명 스탯 변경 → 스킬 피해 변화 측정**(#7 검증).
