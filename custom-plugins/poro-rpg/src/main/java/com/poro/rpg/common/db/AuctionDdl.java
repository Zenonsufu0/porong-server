package com.poro.rpg.common.db;

public final class AuctionDdl {
    private AuctionDdl() {}

    public static final String CREATE_LISTINGS = """
        CREATE TABLE IF NOT EXISTS auction_listings (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            seller_uuid TEXT    NOT NULL,
            seller_name TEXT    NOT NULL,
            item_id     TEXT    NOT NULL,
            quantity    INTEGER NOT NULL DEFAULT 1,
            price       INTEGER NOT NULL,
            listed_at   INTEGER NOT NULL,
            expires_at  INTEGER NOT NULL,
            status      TEXT    NOT NULL DEFAULT 'active',
            sold_at     INTEGER,
            item_payload TEXT
        )
        """;

    public static final String CREATE_INDEX_STATUS_ITEM =
        "CREATE INDEX IF NOT EXISTS idx_al_status_item ON auction_listings (status, item_id)";

    public static final String CREATE_INDEX_SELLER =
        "CREATE INDEX IF NOT EXISTS idx_al_seller ON auction_listings (seller_uuid, status)";

    public static final String CREATE_INDEX_EXPIRES =
        "CREATE INDEX IF NOT EXISTS idx_al_expires ON auction_listings (expires_at, status)";

    public static final String CREATE_PENDING_DELIVERY = """
        CREATE TABLE IF NOT EXISTS auction_pending_delivery (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            player_uuid TEXT    NOT NULL,
            item_id     TEXT,
            quantity    INTEGER NOT NULL DEFAULT 0,
            gold        INTEGER NOT NULL DEFAULT 0,
            created_at  INTEGER NOT NULL,
            item_payload TEXT
        )
        """;

    public static final String CREATE_INDEX_PENDING_PLAYER =
        "CREATE INDEX IF NOT EXISTS idx_apd_player ON auction_pending_delivery (player_uuid)";
}
