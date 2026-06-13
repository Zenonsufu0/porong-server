# 메가 / 테라 해금 설계 (Mega / Tera Unlock)

메가 진화는 Cobblemon: Mega Showdown을 통해 포함한다.

단, 접근은 ZenonMonCore가 진행도 기반으로 게이트한다.

> ✅ **기믹 정책 확정(2026-06-07, 결정 037)**: 서버 전역 **메가 전용**. 테라스탈·(거)다이맥스·Z무브·울트라버스트 **off**(MSD `config/mega_showdown/config.json`: teralization/dynamax/zMoves/outSideUltraBurst=false, teraShard 드롭 0). 전 배틀(리그·야생·관장) 공통. 아래 "테라" 관련 서술은 보류/미적용.

## 일반 정책

- 메가 진화 허용
- 메가팔찌(Mega bracelet)는 진행도 요구 **+ 골드 비용**(진행도 게이트 + 골드 게이트 병행, `economy_design.md` §4)
- 메가스톤(Mega stone)은 구매 또는 플레이를 통해 해금(골드)
- 골드 비용은 **의도적으로 높게** — 포켓몬 포획 + 바닐라 활동을 적극 유도하는 핵심 sink
- ⚠️ **골드 독점 강제(결정 042)**: MSD 기본은 메가스톤·키스톤을 크래프팅/구조물/광석으로 무료 획득시킴 → 데이터팩 `zenonmon_mega_control`로 MSD 구조물 5종 생성 차단(베이스 재료 공급원 = 구조물 내부 광석뿐이라 이걸로 100% 차단). 상점 골드가 유일 경로가 됨.
- 리자몽나이트 X/Y(Charizardite X/Y)는 후반부
- 레이쿠자/메가 레이쿠자(Rayquaza/Mega Rayquaza)는 특수 엔드게임 콘텐츠
- 테라(Tera), 다이맥스(Dynamax), Z무브(Z-Moves) 등 기타 메커니즘은 자유 활성화 전에 검토

## 해금 흐름(제안)

### 초반(Early Game)

- 메가/테라 사용 불가
- 포획과 짐에 집중

### 중반(Mid Game)

- 메가팔찌 해금
- 기본 메가스톤 이용 가능

### 후반(Late Game)

- 강력한 메가스톤 해금
- 리자몽나이트 X/Y 해금
- 고급 전설 티켓 해금

### 엔드게임(Endgame)

- 레이쿠자(Rayquaza)
- 메가 레이쿠자(Mega Rayquaza)
- 챔피언 토너먼트
- 시즌 특별 보상
