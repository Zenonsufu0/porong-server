# 서버 테스트 진입 체크리스트

> **[STATUS: ACTIVE]** — 서버 테스트 진입 전 처리해야 할 선행조건. INBOX-005 2차 감사(2026-05-31) + config/MM 점검(2026-05-31) 기반.
> 코드 블로커(#3~#10)는 DL-091~093으로 src 수정 완료. 이 문서는 **배포·서버 설정·맵** 선행조건을 다룬다.

---

## 0. ✅ 빌드/배포 동기화 — 완료 (2026-05-31, DL-098)

- [x] **jar 재빌드** — DL-091~097 코드 반영. `zenon-rpg-0.1.0.jar` 빌드(테스트 통과).
- [x] **런타임 jar 단일화** — server/plugins의 중복 jar 2개(ZenonRPG.jar + zenon-rpg-0.1.0.jar) 제거 후 새 빌드 1개 배치.
- [x] **stale seed 동기화** — src 정본을 `server/plugins/ZenonRPG/seeds/`에 덮어쓰기(25→27파일). boss_master(시즌6+최종3)·강화표·item_master·잠재풀 src 정합 확인.
- [x] **MM 셸 배포** — server-config/mythicmobs → 런타임. 옛 stale 파일(FieldBosses/SeasonBosses/PoroFieldMobs) 제거로 `rift_king` 중복 충돌 해소. Outpost_Knight 리네임 반영.
- [x] **부팅 검증** — 실서버 부팅 시 8개 bootstrap 전부 completed, disable 0, MythicMobs 감지 → 리스너 등록, Done. (부팅 중 드러난 검증 과엄격 4건 DL-098로 해소.)

> ⚠️ 운영 팁: 종료는 콘솔 `stop`으로 클린 셧다운. `kill`/timeout 종료는 `world/session.lock` 잔존 → 다음 부팅 크래시(`session.lock already locked`). 크래시 시 `find .local/server -name session.lock -delete`.

---

## 1. ✅ 월드·맵·좌표 — 완료 (2026-05-31, DL-099)

단일 평지 월드 `world`로 구성(§5 4월드 분리 간소화). server.properties flat(표면 y=64) + config.yml 배포(보스룸 30 슬롯·아레나 10 슬롯) + `/rpg-genrooms world 10000 64 10000`·`/rpg-genarenas world 20000 64 20000` 실행 완료. 보스룸 30·아레나 10 구조물 생성·슬롯 로드 검증됨. NPC 3명 스폰.

- [x] 평지 월드 `world` 생성, config 배포(런타임 config.yml stub→src 256줄), genrooms/genarenas 실행.
- [ ] (선택) 필드 5종 좌표 — 현재 더미값(평지 평면). 실제 필드 장식/구조는 필요 시 추가.
- [ ] (선택) `season-start-epoch` 실제 시즌 시작값.

<details><summary>구 내용 (4월드 분리 — 1차 미적용)</summary>

정본 월드(`final_master_plan §5`): `world_main`(수도·필드5·훈련장) / `world_farm`(영지) / `world_boss`(보스 인스턴스) / `world_test`. → 1차는 단일 `world`로 간소화(DL-099).
</details>

- [ ] 월드 4종 생성 (`world_main`/`world_farm`/`world_boss`/`world_test`).
- [ ] 맵 배치 (수도·필드5·보스 아레나).
- [ ] `fields` (plain·mine·sewer·outpost·ruins): `world_main` + 실제 좌표 5개.
- [ ] `field-bosses` (동일 5종): `world_main` + 실제 스폰 좌표 5개.
- [ ] `boss-room-slots` (29슬롯, 10000+ 그리드는 설계됨): `world_boss`로 월드명 교체 + `/rpg genrooms` 실행(해당 좌표에 방 생성).
- [ ] `pvp-room-slots`: 해당 월드 + `/rpg-genarenas` 실행.
- [ ] `season-start-epoch`: 실제 시즌 시작(예: 2026-06-01 = 1748736000). 현재 0 → 항상 1주차.
- [ ] `api-secret-key`: Discord 봇/웹 연동 시 설정(로컬만이면 비워둬도 API 503으로 안전).
- [ ] `debug: true` → 운영 시 false 고려.

---

## 2. 🔴 MythicMobs 셸 (server-config/mythicmobs)

- [x] **ID 충돌 해소** (2026-05-31, DL-093 점검): `field_outpost.yml`의 `Fallen_Knight`(필드4) → `Outpost_Knight`로 리네임(+표시명 `초소 기사장`). season_bosses의 `fallen_knight`(시즌1)와 대소문자 충돌 제거. boss_master(src) `outpost_knight` 정합.
- [ ] **MM 셸 배포**: `server-config/mythicmobs/`를 런타임 `.local/server/plugins/MythicMobs/`에 배포(현재 ExampleMobs 등 기본값만 존재). 보스/필드몹 셸 yml 설치 필수 — 없으면 전투/보스/필드 리스너 통째 미등록.
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
