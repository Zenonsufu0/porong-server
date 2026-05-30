package com.poro.empire.common.db;

/**
 * 통화 흐름 로그 — 골드 인플레이션/싱크(발행량 vs 소각량) 판단용 (INBOX-004 #2 / DL-080).
 * 지갑의 실제 게임플레이 변동(획득=inflow / 소모=outflow)만 기록한다. 로드/복원은 제외(restoreCurrency).
 * net(inflow-outflow)이 통화 발행/소각 추세를 정확히 반영. 경매 등 플레이어간 이체는 buyer/seller가 net 상쇄됨.
 */
public final class EconomyFlowDdl {
    private EconomyFlowDdl() {}

    public static final String CREATE_ECONOMY_FLOW = """
        CREATE TABLE IF NOT EXISTS economy_flow (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            player_uuid TEXT    NOT NULL,
            direction   TEXT    NOT NULL,   -- 'inflow' / 'outflow'
            currency    TEXT    NOT NULL,   -- 'gold' 등 정규화 코드
            amount      INTEGER NOT NULL,
            occurred_at INTEGER NOT NULL,   -- epoch ms
            flow_date   TEXT    NOT NULL    -- 'YYYY-MM-DD' (occurred_at 기준 서버 TZ, 일별 집계용)
        )
        """;

    public static final String CREATE_INDEX_ECON_CURRENCY_DATE =
            "CREATE INDEX IF NOT EXISTS idx_econ_currency_date ON economy_flow (currency, flow_date)";
}
