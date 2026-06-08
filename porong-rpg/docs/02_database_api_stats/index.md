# 02. DB / API / 통계 계획

> **[STATUS: DRAFT]** — `CANON.md` 참조.

## 원칙

PoroRPG DB가 원본이다. 웹과 디스코드 봇은 API를 통해 조회한다.

## 핵심 테이블 후보

- player_profile
- account_link
- player_equipment
- equipment_instance
- enhancement_log
- potential_options
- island_profile
- island_storage
- island_machine
- crafting_queue
- boss_spawn_state
- boss_kill_log
- boss_contribution
- economy_log
- item_flow_log
- daily_server_stats
- daily_gold_flow
- daily_item_flow
- daily_boss_stats
- daily_weapon_stats

## API 후보

```text
GET /api/player/{uuid}
GET /api/player/{uuid}/equipment
GET /api/player/{uuid}/island
GET /api/boss/timers
GET /api/ranking
GET /api/admin/stats
POST /auth/verify          # 인게임 발급 코드 검증 (X-API-Key, {code, discordId} → {ok, uuid, name}). DL-132
# 폐기(DL-132): POST /auth/pending, GET /auth/role-queue, POST /auth/role-granted
```

## 통계 목표

관리자는 골드 총량, 골드 생성/소모, 강화 성공률, 큐브 사용량, 보스 클리어 수, 평균 클리어 시간, 무기별 사용률, 영지 레벨 분포, 마인팜 생산량을 확인할 수 있어야 한다.
