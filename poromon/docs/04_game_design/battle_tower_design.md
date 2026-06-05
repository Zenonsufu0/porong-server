# PoroMon 배틀타워 50층 설계 v0.1

> 기준: Cobblemon 1.21.1 / Mega Showdown / SimpleTMs 기반 PoroMon 서버 설계.
> 목적: **허브 관장 8명(8배지) 클리어 이후** 도전하는 50층 엔드컨텐츠.
> 상태: v0.1 → **레포 편입(2026-06-05)**. **데이터 검증 완료**(종족 119·기술 172·아이템 100% 실재 — §4). 입장 조건 CANON 8관장 상향(결정 028). 남은 검증 = NPC 메가/held item/AI **동작**(§5, 실배틀 테스트 필요).
> 상위 CANON: `league_season_design.md §3`(배틀타워 골격). 본 문서는 그 "층별 세부 구성 미정"분을 채운다.

---

## 0. 기본 규칙

| 항목 | 기준 |
|---|---|
| 입장 조건 | **허브 관장 8명(8배지) 클리어** 이후 (CANON 상향, 결정 028 — 기존 배지 4) |
| 층수 | 50층 |
| 레벨 | **상대(NPC) 파티 전층 Lv.100 고정**. 플레이어는 **실제 레벨 그대로(정규화 없음)** — 엔드콘텐츠라 직접 ~100까지 육성해서 도전하는 의도. (정규리그의 Lv50 정규화와 대비) |
| 상대 파티 | 전층 6마리 고정 |
| NPC 메가진화 | 허용 |
| NPC held item | 허용 |
| 전설/환상 | 허용 |
| AI | Cobblemon 기본 AI 기준. 실제 테스트 후 조정 |
| 난이도 상승 축 | 기술폭, 아이템, 랭크업, 선공기, 상태이상, 스텔스록, 메가, 전설/환상 체급 |

---

## 1. 난이도 곡선

| 구간 | 설계 방향 |
|---|---|
| 1~10층 | 기본 상성/기술폭 체크. 약한 아이템. 메가/전설 없음 |
| 11~20층 | 날씨, 상태이상, 스텔스록 도입. 20층 첫 메가 |
| 21~30층 | 실전 아이템, 랭크업, 선공기, 막이 구조 도입. 30층 메가 |
| 31~40층 | 600족/실전 포켓몬 중심. 메가 본격 허용 |
| 41~45층 | 하급/중급 전설 허용. 메가+전설 혼합 |
| 46~49층 | 컨셉 보스 파티. 상급/최상급 전설 일부 허용 |
| 50층 | 최종 마스터. 최상급 전설/환상/메가 허용 |

---

## 2. 작성 규칙

- 포켓몬명은 유저 표시 기준 한국어.
- 기술명/아이템명은 구현 검증을 위해 우선 영어명.
- `후보`, `TODO`, `지원 확인 필요`가 붙은 항목은 실제 모드팩 species/move/item 검증 전까지 확정하지 않는다.
- AI가 복잡한 교체 심리전을 못 할 가능성이 있으므로, 난이도는 단순 화력/랭크업/선공기/상태이상/회복기/전설 체급 중심으로 올린다.
- 수면기, 막이, 회복기가 지나치게 불쾌하면 테스트 후 일부 기술을 교체한다.

---

## 3. 층별 상세 설계

### 1층 — 입문 밸런스
- **난이도 태그:** No Mega / No Legendary
- **파티:** 피죤투 @Sharp Beak: Air Slash, Quick Attack, Roost, U-turn; 라이츄 @Magnet: Thunderbolt, Quick Attack, Nuzzle, Grass Knot; 윈디 @Charcoal: Flamethrower, Crunch, Extreme Speed, Will-O-Wisp; 샤미드 @Mystic Water: Surf, Ice Beam, Aqua Ring, Protect; 코리갑 @Soft Sand: Earthquake, Rock Slide, Rapid Spin, Knock Off; 잠만보 @Sitrus Berry: Body Slam, Heavy Slam, Rest, Sleep Talk
- **난이도 메모:** 기본기 구간. 플레이어가 상성, 선공기, 상태이상, 회복기, 랭크업을 익히는 단계.

### 2층 — 초반 상성 체크
- **난이도 태그:** No Mega / No Legendary
- **파티:** 괴력몬 @Black Belt: Dynamic Punch, Knock Off, Rock Slide, Bulk Up; 팬텀 @Spell Tag: Shadow Ball, Sludge Bomb, Will-O-Wisp, Hex; 라프라스 @Never-Melt Ice: Surf, Ice Beam, Perish Song, Protect; 나시 @Miracle Seed: Energy Ball, Psychic, Sleep Powder, Leech Seed; 켄타로스 @Silk Scarf: Body Slam, Earthquake, Zen Headbutt, Work Up; 쥬피썬더 @Magnet: Thunderbolt, Volt Switch, Shadow Ball, Thunder Wave
- **난이도 메모:** 기본기 구간. 플레이어가 상성, 선공기, 상태이상, 회복기, 랭크업을 익히는 단계.

### 3층 — 물/전기/풀 순환
- **난이도 태그:** No Mega / No Legendary
- **파티:** 로파파 @Damp Rock: Surf, Giga Drain, Ice Beam, Rain Dance; 전룡 @Magnet: Thunderbolt, Dragon Pulse, Cotton Guard, Thunder Wave; 버섯모 @Focus Sash: Seed Bomb, Mach Punch, Spore, Swords Dance; 대짱이 @Soft Sand: Earthquake, Waterfall, Ice Punch, Stealth Rock; 파이어로 @Sharp Beak: Brave Bird, Flare Blitz, Roost, U-turn; 밀로틱 @Leftovers: Scald, Ice Beam, Recover, Haze
- **난이도 메모:** 기본기 구간. 플레이어가 상성, 선공기, 상태이상, 회복기, 랭크업을 익히는 단계.

### 4층 — 속도전 입문
- **난이도 태그:** No Mega / No Legendary
- **파티:** 찌르호크 @Choice Scarf: Brave Bird, Close Combat, Quick Attack, U-turn; 썬더볼트 로토무 @Magnet: Thunderbolt, Flamethrower, Volt Switch, Thunder Wave; 쥬레곤 @Never-Melt Ice: Surf, Ice Beam, Aqua Jet, Encore; 루카리오 @Expert Belt: Aura Sphere, Flash Cannon, Extreme Speed, Swords Dance; 글라이온 @Sitrus Berry: Earthquake, Acrobatics, Roost, Knock Off; 에브이 @Twisted Spoon: Psychic, Dazzling Gleam, Shadow Ball, Calm Mind
- **난이도 메모:** 기본기 구간. 플레이어가 상성, 선공기, 상태이상, 회복기, 랭크업을 익히는 단계.

### 5층 — 1차 미니보스: 기본 실전형
- **난이도 태그:** No Mega / No Legendary
- **파티:** 로토무 워시 @Leftovers: Hydro Pump, Volt Switch, Thunderbolt, Will-O-Wisp; 핫삼 @Metal Coat: Bullet Punch, X-Scissor, Swords Dance, Roost; 마릴리 @Sitrus Berry: Play Rough, Aqua Jet, Liquidation, Belly Drum; 팬텀 @Focus Sash: Shadow Ball, Sludge Bomb, Destiny Bond, Will-O-Wisp; 망나뇽 @Lum Berry: Dragon Claw, Fire Punch, Extreme Speed, Dragon Dance; 해피너스 @Leftovers: Seismic Toss, Soft-Boiled, Thunder Wave, Stealth Rock
- **난이도 메모:** 기본기 구간. 플레이어가 상성, 선공기, 상태이상, 회복기, 랭크업을 익히는 단계.

### 6층 — 불꽃 압박
- **난이도 태그:** No Mega / No Legendary
- **파티:** 헬가 @Black Glasses: Flamethrower, Dark Pulse, Nasty Plot, Will-O-Wisp; 윈디 @Charcoal: Flare Blitz, Wild Charge, Extreme Speed, Crunch; 불카모스 @Silver Powder: Flamethrower, Bug Buzz, Quiver Dance, Giga Drain; 샹델라 @Spell Tag: Shadow Ball, Flamethrower, Energy Ball, Calm Mind; 초염몽 @Expert Belt: Close Combat, Flare Blitz, Mach Punch, U-turn; 리자몽 @Charcoal: Flamethrower, Air Slash, Dragon Pulse, Roost
- **난이도 메모:** 기본기 구간. 플레이어가 상성, 선공기, 상태이상, 회복기, 랭크업을 익히는 단계.

### 7층 — 물 타입 운영
- **난이도 태그:** No Mega / No Legendary
- **파티:** 밀로틱 @Leftovers: Scald, Ice Beam, Recover, Haze; 대짱이 @Soft Sand: Earthquake, Waterfall, Ice Punch, Stealth Rock; 갸라도스 @Mystic Water: Waterfall, Crunch, Ice Fang, Dragon Dance; 로토무 워시 @Leftovers: Hydro Pump, Volt Switch, Will-O-Wisp, Pain Split; 독파리 @Black Sludge: Sludge Bomb, Surf, Toxic Spikes, Rapid Spin; 킹드라 @Scope Lens: Surf, Dragon Pulse, Ice Beam, Focus Energy
- **난이도 메모:** 기본기 구간. 플레이어가 상성, 선공기, 상태이상, 회복기, 랭크업을 익히는 단계.

### 8층 — 풀/독 상태이상
- **난이도 태그:** No Mega / No Legendary
- **파티:** 이상해꽃 @Black Sludge: Giga Drain, Sludge Bomb, Sleep Powder, Leech Seed; 뽀록나 @Black Sludge: Giga Drain, Sludge Bomb, Spore, Clear Smog; 로즈레이드 @Focus Sash: Energy Ball, Sludge Bomb, Toxic Spikes, Sleep Powder; 너트령 @Rocky Helmet: Power Whip, Gyro Ball, Leech Seed, Stealth Rock; 브리가론 @Rocky Helmet: Wood Hammer, Drain Punch, Spiky Shield, Bulk Up; 달코퀸 @Miracle Seed: Trop Kick, High Jump Kick, Rapid Spin, U-turn
- **난이도 메모:** 기본기 구간. 플레이어가 상성, 선공기, 상태이상, 회복기, 랭크업을 익히는 단계.

### 9층 — 땅/바위 입문
- **난이도 태그:** No Mega / No Legendary
- **파티:** 한카리아스 @Soft Sand: Earthquake, Dragon Claw, Rock Slide, Swords Dance; 하마돈 @Leftovers: Earthquake, Rock Slide, Slack Off, Stealth Rock; 마기라스 @Smooth Rock: Stone Edge, Crunch, Earthquake, Dragon Dance; 몰드류 @Life Orb: Earthquake, Iron Head, Rock Slide, Rapid Spin; 거대코뿌리 @Hard Stone: Earthquake, Stone Edge, Megahorn, Stealth Rock; 맘모꾸리 @Never-Melt Ice: Earthquake, Icicle Crash, Ice Shard, Stealth Rock
- **난이도 메모:** 기본기 구간. 플레이어가 상성, 선공기, 상태이상, 회복기, 랭크업을 익히는 단계.

### 10층 — 1차 벽: 밸런스 실전 파티
- **난이도 태그:** No Mega / No Legendary
- **파티:** 로토무 워시 @Leftovers: Hydro Pump, Volt Switch, Will-O-Wisp, Pain Split; 너트령 @Rocky Helmet: Power Whip, Gyro Ball, Leech Seed, Stealth Rock; 망나뇽 @Lum Berry: Dragon Dance, Dragon Claw, Fire Punch, Extreme Speed; 팬텀 @Focus Sash: Shadow Ball, Sludge Bomb, Focus Blast, Destiny Bond; 마릴리 @Sitrus Berry: Play Rough, Aqua Jet, Liquidation, Belly Drum; 마기라스 @Leftovers: Stone Edge, Crunch, Earthquake, Thunder Wave
- **난이도 메모:** 기본기 구간. 플레이어가 상성, 선공기, 상태이상, 회복기, 랭크업을 익히는 단계.

### 11층 — 비 파티 입문
- **난이도 태그:** No Mega / No Legendary
- **파티:** 패리퍼 @Damp Rock: Hurricane, Surf, Tailwind, Roost; 로파파 @Life Orb: Hydro Pump, Giga Drain, Ice Beam, Fake Out; 킹드라 @Scope Lens: Hydro Pump, Dragon Pulse, Ice Beam, Rain Dance; 대짱이 @Leftovers: Waterfall, Earthquake, Ice Punch, Stealth Rock; 로토무 워시 @Leftovers: Thunderbolt, Volt Switch, Hydro Pump, Will-O-Wisp; 갸라도스 @Mystic Water: Waterfall, Crunch, Ice Fang, Dragon Dance
- **난이도 메모:** 날씨/타입 시너지 구간. 파티 전체 컨셉을 상대하는 감각을 준다.

### 12층 — 모래 파티 입문
- **난이도 태그:** No Mega / No Legendary
- **파티:** 마기라스 @Smooth Rock: Stone Edge, Crunch, Earthquake, Stealth Rock; 몰드류 @Life Orb: Earthquake, Iron Head, Rock Slide, Rapid Spin; 하마돈 @Leftovers: Earthquake, Rock Slide, Slack Off, Whirlwind; 글라이온 @Toxic Orb: Earthquake, Knock Off, Roost, Toxic; 루가루암 @Focus Sash: Stone Edge, Accelerock, Crunch, Swords Dance; 한카리아스 @Yache Berry: Earthquake, Dragon Claw, Fire Fang, Swords Dance
- **난이도 메모:** 날씨/타입 시너지 구간. 파티 전체 컨셉을 상대하는 감각을 준다.

### 13층 — 쾌청 파티
- **난이도 태그:** No Mega / No Legendary
- **파티:** 코터스 @Heat Rock: Lava Plume, Solar Beam, Yawn, Stealth Rock; 이상해꽃 @Black Sludge: Solar Beam, Sludge Bomb, Sleep Powder, Growth; 리자몽 @Charcoal: Flamethrower, Air Slash, Solar Beam, Roost; 드레디어 @Miracle Seed: Energy Ball, Sleep Powder, Quiver Dance, Pollen Puff; 윈디 @Charcoal: Flare Blitz, Wild Charge, Extreme Speed, Morning Sun; 불카모스 @Life Orb: Fiery Dance, Bug Buzz, Giga Drain, Quiver Dance
- **난이도 메모:** 날씨/타입 시너지 구간. 파티 전체 컨셉을 상대하는 감각을 준다.

### 14층 — 얼음 압박
- **난이도 태그:** No Mega / No Legendary
- **파티:** 눈설왕 @Light Clay: Blizzard, Giga Drain, Ice Shard, Aurora Veil; 맘모꾸리 @Never-Melt Ice: Earthquake, Icicle Crash, Ice Shard, Stealth Rock; 라프라스 @Leftovers: Freeze-Dry, Surf, Ice Beam, Perish Song; 포푸니라 @Focus Sash: Triple Axel, Knock Off, Ice Shard, Swords Dance; 글레이시아 @Choice Specs: Ice Beam, Shadow Ball, Calm Mind, Protect; 드닐레이브 @Life Orb: Glaive Rush, Icicle Crash, Earthquake, Dragon Dance
- **난이도 메모:** 날씨/타입 시너지 구간. 파티 전체 컨셉을 상대하는 감각을 준다.

### 15층 — 2차 벽: 날씨 혼합
- **난이도 태그:** No Mega / No Legendary
- **파티:** 마기라스 @Smooth Rock: Stone Edge, Crunch, Thunder Wave, Stealth Rock; 패리퍼 @Damp Rock: Hurricane, Surf, Tailwind, Roost; 몰드류 @Life Orb: Earthquake, Iron Head, Rock Slide, Rapid Spin; 킹드라 @Scope Lens: Hydro Pump, Dragon Pulse, Ice Beam, Rain Dance; 불카모스 @Life Orb: Fiery Dance, Bug Buzz, Giga Drain, Quiver Dance; 망나뇽 @Lum Berry: Dragon Dance, Dragon Claw, Fire Punch, Extreme Speed
- **난이도 메모:** 날씨/타입 시너지 구간. 파티 전체 컨셉을 상대하는 감각을 준다.

### 16층 — 독/막이 운영
- **난이도 태그:** No Mega / No Legendary
- **파티:** 더시마사리 @Black Sludge: Scald, Recover, Haze, Toxic Spikes; 글라이온 @Toxic Orb: Earthquake, Toxic, Roost, Knock Off; 해피너스 @Leftovers: Seismic Toss, Soft-Boiled, Thunder Wave, Heal Bell; 너트령 @Rocky Helmet: Power Whip, Gyro Ball, Leech Seed, Stealth Rock; 팬텀 @Focus Sash: Hex, Sludge Bomb, Will-O-Wisp, Taunt; 밀로틱 @Leftovers: Scald, Recover, Ice Beam, Haze
- **난이도 메모:** 날씨/타입 시너지 구간. 파티 전체 컨셉을 상대하는 감각을 준다.

### 17층 — 격투 러시
- **난이도 태그:** No Mega / No Legendary
- **파티:** 괴력몬 @Flame Orb: Dynamic Punch, Knock Off, Stone Edge, Bulk Up; 루카리오 @Life Orb: Close Combat, Meteor Mash, Extreme Speed, Swords Dance; 버섯모 @Focus Sash: Mach Punch, Bullet Seed, Spore, Swords Dance; 초염몽 @Expert Belt: Close Combat, Flare Blitz, Mach Punch, U-turn; 엘레이드 @Scope Lens: Psycho Cut, Close Combat, Knock Off, Swords Dance; 노보청 @Assault Vest: Drain Punch, Mach Punch, Knock Off, Bulk Up
- **난이도 메모:** 날씨/타입 시너지 구간. 파티 전체 컨셉을 상대하는 감각을 준다.

### 18층 — 고스트/악 압박
- **난이도 태그:** No Mega / No Legendary
- **파티:** 팬텀 @Focus Sash: Shadow Ball, Sludge Bomb, Focus Blast, Destiny Bond; 샹델라 @Choice Specs: Shadow Ball, Flamethrower, Energy Ball, Calm Mind; 따라큐 @Life Orb: Play Rough, Shadow Claw, Shadow Sneak, Swords Dance; 절각참 @Black Glasses: Iron Head, Sucker Punch, Knock Off, Swords Dance; 마기라스 @Leftovers: Crunch, Stone Edge, Earthquake, Dragon Dance; 삼삼드래 @Life Orb: Dark Pulse, Dragon Pulse, Flamethrower, Nasty Plot
- **난이도 메모:** 날씨/타입 시너지 구간. 파티 전체 컨셉을 상대하는 감각을 준다.

### 19층 — 강철 코어
- **난이도 태그:** No Mega / No Legendary
- **파티:** 핫삼 @Metal Coat: Bullet Punch, U-turn, Swords Dance, Roost; 너트령 @Rocky Helmet: Power Whip, Gyro Ball, Leech Seed, Stealth Rock; 메타그로스 @Life Orb: Meteor Mash, Zen Headbutt, Earthquake, Agility; 킬가르도 @Leftovers: Shadow Ball, Sacred Sword, King's Shield, Shadow Sneak; 몰드류 @Life Orb: Earthquake, Iron Head, Rapid Spin, Swords Dance; 아머까오 @Leftovers: Brave Bird, Body Press, Roost, Defog
- **난이도 메모:** 날씨/타입 시너지 구간. 파티 전체 컨셉을 상대하는 감각을 준다.

### 20층 — 2막 보스: 첫 메가
- **난이도 태그:** Mega Scizor / No Legendary
- **파티:** 한카리아스 @Rocky Helmet: Earthquake, Dragon Claw, Stealth Rock, Swords Dance; 로토무 워시 @Leftovers: Hydro Pump, Volt Switch, Will-O-Wisp, Pain Split; 핫삼 @Scizorite: Bullet Punch, U-turn, Swords Dance, Roost; 버섯모 @Focus Sash: Spore, Mach Punch, Bullet Seed, Swords Dance; 불카모스 @Life Orb: Quiver Dance, Fiery Dance, Bug Buzz, Giga Drain; 마기라스 @Leftovers: Stone Edge, Crunch, Earthquake, Thunder Wave
- **난이도 메모:** 날씨/타입 시너지 구간. 파티 전체 컨셉을 상대하는 감각을 준다.

### 21층 — 스텔스록 압박
- **난이도 태그:** No Mega / Candidate Legendary
- **파티:** 랜드로스 후보 @Rocky Helmet: Earthquake, U-turn, Stone Edge, Stealth Rock; 한카리아스 @Yache Berry: Earthquake, Dragon Tail, Fire Fang, Stealth Rock; 너트령 @Rocky Helmet: Power Whip, Gyro Ball, Leech Seed, Spikes; 로토무 워시 @Leftovers: Hydro Pump, Volt Switch, Will-O-Wisp, Pain Split; 망나뇽 @Lum Berry: Dragon Dance, Dragon Claw, Fire Punch, Extreme Speed; 팬텀 @Focus Sash: Shadow Ball, Sludge Bomb, Taunt, Destiny Bond
- **난이도 메모:** 실전 요소 도입 구간. 스텔스록, 랭크업, 막이, 선공기 대응이 필요하다.

### 22층 — 수면/상태이상
- **난이도 태그:** No Mega / No Legendary
- **파티:** 버섯모 @Focus Sash: Spore, Mach Punch, Bullet Seed, Swords Dance; 뽀록나 @Black Sludge: Spore, Giga Drain, Sludge Bomb, Clear Smog; 팬텀 @Focus Sash: Hypnosis, Hex, Sludge Bomb, Will-O-Wisp; 로토무 워시 @Leftovers: Will-O-Wisp, Hydro Pump, Volt Switch, Pain Split; 밀로틱 @Leftovers: Scald, Recover, Haze, Ice Beam; 절각참 @Black Glasses: Sucker Punch, Iron Head, Knock Off, Swords Dance
- **난이도 메모:** 실전 요소 도입 구간. 스텔스록, 랭크업, 막이, 선공기 대응이 필요하다.

### 23층 — 랭크업 스위퍼
- **난이도 태그:** No Mega / No Legendary
- **파티:** 갸라도스 @Lum Berry: Dragon Dance, Waterfall, Crunch, Ice Fang; 망나뇽 @Lum Berry: Dragon Dance, Dragon Claw, Fire Punch, Extreme Speed; 루카리오 @Life Orb: Swords Dance, Close Combat, Meteor Mash, Extreme Speed; 불카모스 @Life Orb: Quiver Dance, Fiery Dance, Bug Buzz, Giga Drain; 마릴리 @Sitrus Berry: Belly Drum, Aqua Jet, Play Rough, Liquidation; 메타그로스 @Weakness Policy: Agility, Meteor Mash, Zen Headbutt, Earthquake
- **난이도 메모:** 실전 요소 도입 구간. 스텔스록, 랭크업, 막이, 선공기 대응이 필요하다.

### 24층 — 드래곤 압박
- **난이도 태그:** No Mega / No Legendary
- **파티:** 망나뇽 @Lum Berry: Dragon Dance, Dragon Claw, Fire Punch, Extreme Speed; 보만다 @Life Orb: Dragon Dance, Dragon Claw, Earthquake, Fire Fang; 한카리아스 @Yache Berry: Earthquake, Dragon Claw, Stone Edge, Swords Dance; 삼삼드래 @Choice Specs: Dark Pulse, Dragon Pulse, Flamethrower, Nasty Plot; 미끄래곤 @Assault Vest: Dragon Pulse, Flamethrower, Thunderbolt, Sludge Bomb; 드래펄트 @Life Orb: Dragon Darts, Phantom Force, U-turn, Dragon Dance
- **난이도 메모:** 실전 요소 도입 구간. 스텔스록, 랭크업, 막이, 선공기 대응이 필요하다.

### 25층 — 3차 벽: 준실전 밸런스
- **난이도 태그:** No Mega / No Legendary
- **파티:** 로토무 워시 @Leftovers: Hydro Pump, Volt Switch, Will-O-Wisp, Pain Split; 아머까오 @Leftovers: Brave Bird, Body Press, Roost, Defog; 한카리아스 @Rocky Helmet: Earthquake, Dragon Claw, Stealth Rock, Swords Dance; 불카모스 @Life Orb: Quiver Dance, Fiery Dance, Bug Buzz, Giga Drain; 대도각참 @Black Glasses: Kowtow Cleave, Iron Head, Sucker Punch, Swords Dance; 마릴리 @Sitrus Berry: Play Rough, Aqua Jet, Liquidation, Belly Drum
- **난이도 메모:** 실전 요소 도입 구간. 스텔스록, 랭크업, 막이, 선공기 대응이 필요하다.

### 26층 — 더블 스위퍼 구조
- **난이도 태그:** No Mega / No Legendary
- **파티:** 킬가르도 @Leftovers: King's Shield, Shadow Ball, Sacred Sword, Shadow Sneak; 절각참 @Black Glasses: Sucker Punch, Iron Head, Knock Off, Swords Dance; 팬텀 @Focus Sash: Shadow Ball, Sludge Bomb, Focus Blast, Destiny Bond; 삼삼드래 @Life Orb: Nasty Plot, Dark Pulse, Dragon Pulse, Flamethrower; 따라큐 @Life Orb: Swords Dance, Play Rough, Shadow Claw, Shadow Sneak; 불카모스 @Life Orb: Quiver Dance, Fiery Dance, Bug Buzz, Psychic
- **난이도 메모:** 실전 요소 도입 구간. 스텔스록, 랭크업, 막이, 선공기 대응이 필요하다.

### 27층 — 물리막이+특수스위퍼
- **난이도 태그:** No Mega / Candidate Legendary
- **파티:** 하마돈 @Leftovers: Earthquake, Stealth Rock, Slack Off, Whirlwind; 아머까오 @Leftovers: Body Press, Iron Defense, Roost, Brave Bird; 더시마사리 @Black Sludge: Scald, Recover, Haze, Toxic Spikes; 팬텀 @Focus Sash: Shadow Ball, Sludge Bomb, Taunt, Destiny Bond; 불카모스 @Life Orb: Quiver Dance, Fiery Dance, Bug Buzz, Giga Drain; 라티오스 후보 @Soul Dew 후보: Draco Meteor, Psychic, Aura Sphere, Recover
- **난이도 메모:** 실전 요소 도입 구간. 스텔스록, 랭크업, 막이, 선공기 대응이 필요하다.

### 28층 — 고속 압박
- **난이도 태그:** No Mega / No Legendary
- **파티:** 드래펄트 @Life Orb: Dragon Darts, Phantom Force, U-turn, Dragon Dance; 포푸니라 @Focus Sash: Triple Axel, Knock Off, Ice Shard, Swords Dance; 쥬피썬더 @Choice Specs: Thunderbolt, Volt Switch, Shadow Ball, Thunder Wave; 팬텀 @Focus Sash: Shadow Ball, Sludge Bomb, Focus Blast, Destiny Bond; 파이어로 @Sharp Beak: Brave Bird, Flare Blitz, Roost, U-turn; 개굴닌자 후보 @Life Orb: Hydro Pump, Dark Pulse, Ice Beam, U-turn
- **난이도 메모:** 실전 요소 도입 구간. 스텔스록, 랭크업, 막이, 선공기 대응이 필요하다.

### 29층 — 막이 파괴
- **난이도 태그:** No Mega / No Legendary
- **파티:** 노보청 @Flame Orb: Drain Punch, Mach Punch, Knock Off, Bulk Up; 마릴리 @Sitrus Berry: Belly Drum, Aqua Jet, Play Rough, Liquidation; 대도각참 @Black Glasses: Swords Dance, Kowtow Cleave, Iron Head, Sucker Punch; 몰드류 @Life Orb: Earthquake, Iron Head, Rock Slide, Swords Dance; 한카리아스 @Yache Berry: Swords Dance, Earthquake, Dragon Claw, Fire Fang; 불카모스 @Life Orb: Quiver Dance, Fiery Dance, Bug Buzz, Giga Drain
- **난이도 메모:** 실전 요소 도입 구간. 스텔스록, 랭크업, 막이, 선공기 대응이 필요하다.

### 30층 — 3막 보스: 메가 한카리아스
- **난이도 태그:** Mega Garchomp / Candidate Legendary
- **파티:** 랜드로스 후보 @Rocky Helmet: Earthquake, U-turn, Stealth Rock, Stone Edge; 로토무 워시 @Leftovers: Hydro Pump, Volt Switch, Will-O-Wisp, Pain Split; 너트령 @Rocky Helmet: Power Whip, Gyro Ball, Leech Seed, Spikes; 드래펄트 @Life Orb: Dragon Darts, Phantom Force, U-turn, Dragon Dance; 불카모스 @Life Orb: Quiver Dance, Fiery Dance, Bug Buzz, Giga Drain; 한카리아스 @Garchompite: Earthquake, Dragon Claw, Stone Edge, Swords Dance
- **난이도 메모:** 실전 요소 도입 구간. 스텔스록, 랭크업, 막이, 선공기 대응이 필요하다.

### 31층 — 비 실전 강화
- **난이도 태그:** Mega Swampert / Candidate Legendary
- **파티:** 패리퍼 @Damp Rock: Hurricane, Surf, Tailwind, Roost; 킹드라 @Scope Lens: Hydro Pump, Draco Meteor, Ice Beam, Flash Cannon; 로파파 @Life Orb: Hydro Pump, Giga Drain, Ice Beam, Fake Out; 대짱이 @Swampertite: Waterfall, Earthquake, Ice Punch, Stealth Rock; 썬더 후보 @Leftovers: Thunder, Hurricane, Roost, Volt Switch; 마릴리 @Sitrus Berry: Belly Drum, Aqua Jet, Play Rough, Liquidation
- **난이도 메모:** 실전 파티 구간. 메가진화와 600족/고성능 포켓몬을 본격적으로 사용한다.

### 32층 — 모래 실전 강화
- **난이도 태그:** Mega Tyranitar / No Legendary
- **파티:** 마기라스 @Tyranitarite: Stone Edge, Crunch, Earthquake, Stealth Rock; 몰드류 @Life Orb: Earthquake, Iron Head, Rock Slide, Rapid Spin; 하마돈 @Leftovers: Earthquake, Slack Off, Whirlwind, Stealth Rock; 한카리아스 @Yache Berry: Earthquake, Dragon Claw, Fire Fang, Swords Dance; 글라이온 @Toxic Orb: Earthquake, Knock Off, Roost, Toxic; 대도각참 @Black Glasses: Kowtow Cleave, Iron Head, Sucker Punch, Swords Dance
- **난이도 메모:** 실전 파티 구간. 메가진화와 600족/고성능 포켓몬을 본격적으로 사용한다.

### 33층 — 독막이 고난도
- **난이도 태그:** Mega Gengar 후보 / No Legendary
- **파티:** 더시마사리 @Black Sludge: Scald, Recover, Haze, Toxic Spikes; 뽀록나 @Black Sludge: Spore, Giga Drain, Sludge Bomb, Clear Smog; 글라이온 @Toxic Orb: Earthquake, Toxic, Roost, Knock Off; 해피너스 @Leftovers: Seismic Toss, Soft-Boiled, Thunder Wave, Heal Bell; 아머까오 @Leftovers: Body Press, Iron Defense, Roost, Brave Bird; 팬텀 @Gengarite: Hex, Sludge Bomb, Will-O-Wisp, Taunt
- **난이도 메모:** 실전 파티 구간. 메가진화와 600족/고성능 포켓몬을 본격적으로 사용한다.

### 34층 — 강철 난전
- **난이도 태그:** Mega Metagross / No Legendary
- **파티:** 아머까오 @Leftovers: Iron Defense, Body Press, Roost, Brave Bird; 너트령 @Rocky Helmet: Power Whip, Gyro Ball, Leech Seed, Stealth Rock; 메타그로스 @Metagrossite: Meteor Mash, Zen Headbutt, Earthquake, Agility; 킬가르도 @Leftovers: King's Shield, Shadow Ball, Sacred Sword, Shadow Sneak; 대도각참 @Black Glasses: Kowtow Cleave, Iron Head, Sucker Punch, Swords Dance; 핫삼 @Life Orb: Bullet Punch, U-turn, Swords Dance, Roost
- **난이도 메모:** 실전 파티 구간. 메가진화와 600족/고성능 포켓몬을 본격적으로 사용한다.

### 35층 — 4차 벽: 스위퍼 연속
- **난이도 태그:** Mega Gyarados / No Legendary
- **파티:** 망나뇽 @Lum Berry: Dragon Dance, Dragon Claw, Fire Punch, Extreme Speed; 갸라도스 @Gyaradosite: Dragon Dance, Waterfall, Crunch, Ice Fang; 불카모스 @Life Orb: Quiver Dance, Fiery Dance, Bug Buzz, Giga Drain; 루카리오 @Life Orb: Swords Dance, Close Combat, Meteor Mash, Extreme Speed; 마릴리 @Sitrus Berry: Belly Drum, Aqua Jet, Play Rough, Liquidation; 드래펄트 @Life Orb: Dragon Dance, Dragon Darts, Phantom Force, U-turn
- **난이도 메모:** 실전 파티 구간. 메가진화와 600족/고성능 포켓몬을 본격적으로 사용한다.

### 36층 — 드래곤 챔버
- **난이도 태그:** Mega Salamence / No Legendary
- **파티:** 보만다 @Salamencite: Dragon Dance, Dragon Claw, Earthquake, Fire Fang; 한카리아스 @Yache Berry: Swords Dance, Earthquake, Dragon Claw, Stone Edge; 삼삼드래 @Choice Specs: Nasty Plot, Dark Pulse, Dragon Pulse, Flamethrower; 드래펄트 @Life Orb: Dragon Darts, Phantom Force, U-turn, Dragon Dance; 미끄래곤 @Assault Vest: Dragon Pulse, Flamethrower, Thunderbolt, Sludge Bomb; 드닐레이브 @Life Orb: Glaive Rush, Icicle Crash, Earthquake, Dragon Dance
- **난이도 메모:** 실전 파티 구간. 메가진화와 600족/고성능 포켓몬을 본격적으로 사용한다.

### 37층 — 선공기 압박
- **난이도 태그:** Mega Scizor / No Legendary
- **파티:** 망나뇽 @Lum Berry: Extreme Speed, Dragon Claw, Fire Punch, Dragon Dance; 핫삼 @Scizorite: Bullet Punch, U-turn, Swords Dance, Roost; 마릴리 @Sitrus Berry: Aqua Jet, Play Rough, Liquidation, Belly Drum; 포푸니라 @Focus Sash: Ice Shard, Triple Axel, Knock Off, Swords Dance; 대도각참 @Black Glasses: Sucker Punch, Kowtow Cleave, Iron Head, Swords Dance; 루카리오 @Life Orb: Extreme Speed, Close Combat, Meteor Mash, Swords Dance
- **난이도 메모:** 실전 파티 구간. 메가진화와 600족/고성능 포켓몬을 본격적으로 사용한다.

### 38층 — 트릭룸 후보
- **난이도 태그:** No Mega / No Legendary
- **파티:** 야도란 @Leftovers: Trick Room, Scald, Psychic, Slack Off; 브리무음 후보 @Life Orb: Trick Room, Psychic, Dazzling Gleam, Mystical Fire; 하리뭉 @Assault Vest: Close Combat, Knock Off, Heavy Slam, Bullet Punch; 거대코뿌리 @Weakness Policy: Earthquake, Stone Edge, Megahorn, Hammer Arm; 샹델라 @Choice Specs: Shadow Ball, Flamethrower, Energy Ball, Trick Room; 잠만보 @Leftovers: Body Slam, Heavy Slam, Rest, Curse
- **난이도 메모:** 실전 파티 구간. 메가진화와 600족/고성능 포켓몬을 본격적으로 사용한다.

### 39층 — 대면 조작
- **난이도 태그:** No Mega / Candidate Legendary
- **파티:** 로토무 워시 @Leftovers: Volt Switch, Hydro Pump, Will-O-Wisp, Pain Split; 랜드로스 후보 @Rocky Helmet: U-turn, Earthquake, Stone Edge, Stealth Rock; 핫삼 @Life Orb: U-turn, Bullet Punch, Swords Dance, Roost; 드래펄트 @Life Orb: U-turn, Dragon Darts, Phantom Force, Will-O-Wisp; 조로아크 @Focus Sash: Night Daze, Flamethrower, Focus Blast, U-turn; 아머까오 @Leftovers: U-turn, Brave Bird, Roost, Defog
- **난이도 메모:** 실전 파티 구간. 메가진화와 600족/고성능 포켓몬을 본격적으로 사용한다.

### 40층 — 4막 보스: 준최종 밸런스
- **난이도 태그:** Mega Metagross / Candidate Legendary
- **파티:** 랜드로스 후보 @Rocky Helmet: Earthquake, U-turn, Stealth Rock, Stone Edge; 로토무 워시 @Leftovers: Hydro Pump, Volt Switch, Will-O-Wisp, Pain Split; 아머까오 @Leftovers: Iron Defense, Body Press, Roost, Brave Bird; 불카모스 @Life Orb: Quiver Dance, Fiery Dance, Bug Buzz, Giga Drain; 드래펄트 @Life Orb: Dragon Darts, Phantom Force, U-turn, Dragon Dance; 메타그로스 @Metagrossite: Meteor Mash, Zen Headbutt, Earthquake, Agility
- **난이도 메모:** 실전 파티 구간. 메가진화와 600족/고성능 포켓몬을 본격적으로 사용한다.

### 41층 — 하급 전설 혼합
- **난이도 태그:** No Mega / Legendary
- **파티:** 프리져 @Leftovers: Ice Beam, Hurricane, Roost, Tailwind; 썬더 @Leftovers: Thunderbolt, Hurricane, Roost, Volt Switch; 파이어 @Heavy-Duty Boots: Flamethrower, Hurricane, Roost, Will-O-Wisp; 라이코 @Magnet: Thunderbolt, Volt Switch, Aura Sphere, Calm Mind; 앤테이 @Charcoal: Sacred Fire, Extreme Speed, Stone Edge, Iron Head; 스이쿤 @Leftovers: Scald, Ice Beam, Calm Mind, Rest
- **난이도 메모:** 전설 입문 구간. 하급/중급 전설과 일반 실전 포켓몬을 섞어 압박한다.

### 42층 — 레지/호수 수호자
- **난이도 태그:** No Mega / Legendary
- **파티:** 레지락 @Leftovers: Stone Edge, Earthquake, Stealth Rock, Thunder Wave; 레지아이스 @Assault Vest: Ice Beam, Thunderbolt, Focus Blast, Amnesia; 레지스틸 @Leftovers: Iron Head, Body Press, Stealth Rock, Thunder Wave; 유크시 @Light Clay: Psychic, Thunder Wave, Stealth Rock, U-turn; 엠라이트 @Expert Belt: Psychic, Ice Beam, Thunderbolt, Healing Wish; 아그놈 @Focus Sash: Psychic, Flamethrower, Nasty Plot, Explosion
- **난이도 메모:** 전설 입문 구간. 하급/중급 전설과 일반 실전 포켓몬을 섞어 압박한다.

### 43층 — 중급 전설 입문
- **난이도 태그:** No Mega / Legendary
- **파티:** 라티아스 @Soul Dew 후보: Dragon Pulse, Psychic, Recover, Calm Mind; 라티오스 @Soul Dew 후보: Draco Meteor, Psychic, Aura Sphere, Recover; 히드런 @Leftovers: Magma Storm, Earth Power, Flash Cannon, Stealth Rock; 크레세리아 @Leftovers: Moonblast, Psychic, Calm Mind, Moonlight; 레지기가스 @Leftovers: Body Slam, Earthquake, Knock Off, Drain Punch; 케르디오 후보 @Choice Specs: Hydro Pump, Secret Sword, Icy Wind, Calm Mind
- **난이도 메모:** 전설 입문 구간. 하급/중급 전설과 일반 실전 포켓몬을 섞어 압박한다.

### 44층 — 성검/수호자
- **난이도 태그:** No Mega / Legendary
- **파티:** 코바르온 @Leftovers: Iron Head, Close Combat, Stone Edge, Swords Dance; 테라키온 @Life Orb: Close Combat, Stone Edge, Earthquake, Swords Dance; 비리디온 @Life Orb: Leaf Blade, Close Combat, Stone Edge, Swords Dance; 케르디오 후보 @Choice Specs: Hydro Pump, Secret Sword, Icy Wind, Calm Mind; 자시안 후보 @Rusted Sword 후보: Behemoth Blade, Play Rough, Close Combat, Swords Dance; 자마젠타 후보 @Rusted Shield 후보: Behemoth Bash, Close Combat, Crunch, Iron Defense
- **난이도 메모:** 전설 입문 구간. 하급/중급 전설과 일반 실전 포켓몬을 섞어 압박한다.

### 45층 — 5차 벽: 전설+실전 혼합
- **난이도 태그:** Mega Latios 후보 / Legendary
- **파티:** 라티오스 @Latiosite 후보: Draco Meteor, Psychic, Aura Sphere, Recover; 히드런 @Leftovers: Magma Storm, Earth Power, Flash Cannon, Stealth Rock; 스이쿤 @Leftovers: Scald, Ice Beam, Calm Mind, Rest; 한카리아스 @Yache Berry: Earthquake, Dragon Claw, Fire Fang, Swords Dance; 아머까오 @Leftovers: Iron Defense, Body Press, Roost, Brave Bird; 대도각참 @Black Glasses: Kowtow Cleave, Iron Head, Sucker Punch, Swords Dance
- **난이도 메모:** 전설 입문 구간. 하급/중급 전설과 일반 실전 포켓몬을 섞어 압박한다.

### 46층 — 컨셉 보스: 하늘의 균열
- **난이도 태그:** Mega Rayquaza 후보 / Legendary
- **파티:** 라티아스 @Soul Dew 후보: Dragon Pulse, Psychic, Recover, Calm Mind; 라티오스 @Soul Dew 후보: Draco Meteor, Psychic, Aura Sphere, Recover; 루기아 @Leftovers: Aeroblast, Psychic, Recover, Calm Mind; 칠색조 @Heavy-Duty Boots: Sacred Fire, Brave Bird, Recover, Earthquake; 썬더 @Leftovers: Thunderbolt, Hurricane, Roost, Volt Switch; 레쿠쟈 후보 @Life Orb: Dragon Ascent, Extreme Speed, Earthquake, Dragon Dance
- **난이도 메모:** 컨셉 보스 구간. 테마 조우권과 연결 가능한 전설 파티로 구성한다.

### 47층 — 컨셉 보스: 대지와 심해
- **난이도 태그:** No Mega / Legendary
- **파티:** 그란돈 후보 @Red Orb 후보: Precipice Blades, Stone Edge, Fire Punch, Swords Dance; 가이오가 후보 @Blue Orb 후보: Origin Pulse, Ice Beam, Thunder, Calm Mind; 히드런 @Leftovers: Magma Storm, Earth Power, Flash Cannon, Stealth Rock; 랜드로스 후보 @Rocky Helmet: Earthquake, U-turn, Stone Edge, Stealth Rock; 스이쿤 @Leftovers: Scald, Ice Beam, Calm Mind, Rest; 지가르데 후보 @Leftovers: Thousand Arrows, Extreme Speed, Dragon Dance, Coil
- **난이도 메모:** 컨셉 보스 구간. 테마 조우권과 연결 가능한 전설 파티로 구성한다.

### 48층 — 컨셉 보스: 시간과 공간
- **난이도 태그:** No Mega / Legendary
- **파티:** 디아루가 후보 @Adamant Orb 후보: Draco Meteor, Flash Cannon, Earth Power, Stealth Rock; 펄기아 후보 @Lustrous Orb 후보: Spacial Rend, Hydro Pump, Earth Power, Thunderbolt; 기라티나 후보 @Griseous Orb 후보: Shadow Force, Dragon Claw, Will-O-Wisp, Rest; 크레세리아 @Leftovers: Moonblast, Psychic, Calm Mind, Moonlight; 후파 후보 @Life Orb: Hyperspace Hole, Psychic, Dark Pulse, Nasty Plot; 코스모그 후보 @Eviolite 후보: Cosmic Power, Psychic, Teleport 후보, Protect
- **난이도 메모:** 컨셉 보스 구간. 테마 조우권과 연결 가능한 전설 파티로 구성한다.

### 49층 — 컨셉 보스: 반전과 재앙
- **난이도 태그:** No Mega / Legendary
- **파티:** 다크라이 후보 @Life Orb: Dark Pulse, Nasty Plot, Dark Void 후보, Sludge Bomb; 이벨타르 후보 @Life Orb: Oblivion Wing, Dark Pulse, Sucker Punch, Roost; 기라티나 후보 @Griseous Orb 후보: Shadow Force, Dragon Claw, Will-O-Wisp, Rest; 무한다이노 후보 @Black Sludge: Dynamax Cannon, Sludge Bomb, Flamethrower, Recover; 파오젠 후보 @Life Orb: Icicle Crash, Crunch, Ice Shard, Swords Dance; 위유이 후보 @Choice Specs: Dark Pulse, Flamethrower, Nasty Plot, Psychic
- **난이도 메모:** 컨셉 보스 구간. 테마 조우권과 연결 가능한 전설 파티로 구성한다.

### 50층 — 최종 보스: 포로몬 마스터
- **난이도 태그:** Mega Rayquaza/Mewtwo 후보 / 최상급 Legendary/Mythical
- **파티:** 레쿠쟈 후보 @Life Orb: Dragon Ascent, Extreme Speed, Earthquake, Dragon Dance; 뮤츠 후보 @Mewtwonite Y 후보: Psystrike, Aura Sphere, Ice Beam, Calm Mind; 디아루가 후보 @Adamant Orb 후보: Draco Meteor, Flash Cannon, Earth Power, Stealth Rock; 펄기아 후보 @Lustrous Orb 후보: Spacial Rend, Hydro Pump, Thunderbolt, Earth Power; 기라티나 후보 @Griseous Orb 후보: Shadow Force, Dragon Claw, Will-O-Wisp, Rest; 아르세우스 후보 @Legend Plate 후보: Judgment, Extreme Speed, Earthquake, Recover
- **난이도 메모:** 최종층. 서버 최상위 엔드컨텐츠 보스로 설계한다.


---

## 4. 검증 결과 (2026-06-05, jar 기준)

> ✅ **데이터 존재 검증 완료**: 전 50층 파싱 — **종족 119종 / 기술 172종 매칭 실패 0**, §4.1 메가스톤 10종·§4.2 아이템 전수·§4.3 시그니처기 18종 **전부 실재**(`jar_registry_reference.md`, Cobblemon `cobblemon.move.*` 926키, MSD lang). 아래 목록은 모두 ✅.
> ⚠️ **미검증(동작 — 실배틀 테스트 필요, §5)**: NPC 메가진화 실제 발동 / NPC held item 전투 반영 / AI의 셋업·해저드·상태이상 운용. MSD jar에 npc·trainer 문자열 없음 → 코드/전투 시점 동작이라 정적 확인 불가.

### 4.1 Mega Showdown 메가 아이템 — ✅ 전부 실재

- Scizorite
- Garchompite
- Swampertite
- Tyranitarite
- Gengarite
- Metagrossite
- Gyaradosite
- Salamencite
- Latiosite
- Mewtwonite Y
- Rayquaza 메가진화 조건

### 4.2 전설/환상 및 전용 아이템

- Red Orb / Blue Orb
- Adamant Orb / Lustrous Orb / Griseous Orb
- Rusted Sword / Rusted Shield
- Legend Plate
- Soul Dew
- Heavy-Duty Boots 지원 여부
- Choice item / Life Orb / Focus Sash / Assault Vest / Rocky Helmet 지원 여부

### 4.3 전용기/주요 실전기

- Dragon Ascent
- Psystrike
- Behemoth Blade / Behemoth Bash
- Precipice Blades / Origin Pulse
- Spacial Rend / Shadow Force
- Dynamax Cannon
- Hyperspace Hole
- Judgment
- Stealth Rock / Spikes / Toxic Spikes
- U-turn / Volt Switch / Knock Off
- Spore / Sleep Powder / Will-O-Wisp / Thunder Wave

---

## 5. 구현 TODO

- [x] Cobblemon species 존재 여부 확인 — **119종 전수 ✅**
- [x] SimpleTMs/Cobblemon move 지원 여부 확인 — **172종 전수 ✅**(시그니처기 포함)
- [x] 41~50층 전설/환상 species 검증 — **전수 실재 ✅**(`encounter_pool_design.md`와 동일 ID)
- [x] 메가스톤·전용 아이템 존재 — **§4.1/§4.2 전수 ✅**
- [ ] **NPC Mega Evolution 작동 확인** — 실배틀 테스트(최대 리스크). MSD의 NPC 메가 발동 조건 확인
- [ ] **NPC held item 전투 반영 확인** — 실배틀 테스트
- [ ] **AI 운용 확인** — 랭크업/회복/스텔스록/상태이상을 AI가 어떻게 쓰는지(곡선 좌우)
- [ ] 1~50층 trainer config 스키마 확정(`seasons.json`/PoroMonCore)
- [ ] 한국어 GUI/입장 조건/보상 안내 문구 작성
- [ ] 배틀타워 보상 설계 작성(10층 단위 체크포인트 — `league_season_design.md §3`, `economy_design.md`)
- [ ] 클리어 기록/랭킹/시즌 보상 연동 여부 결정(`PlayerProgress.battleTower.highestClearedFloor`)

---

## 6. 후속 설계 메모

- 배틀타워는 `관장 8명 이후 → 50층 도전 → 왕중왕전/시즌 랭킹`으로 연결한다.
- 10층 단위로 보상과 체크포인트를 둘 수 있다.
- 20/30/40/45/50층은 미니보스 또는 보스층으로 연출한다.
- 46~49층 컨셉 보스는 컨셉 조우권 10종과 연결하면 좋다.
- 50층 보스는 서버 시즌 최종 목표 또는 왕중왕전 예선 조건으로 활용 가능하다.
