package com.poro.empire.market;

import com.poro.empire.common.db.ConnectionProvider;
import com.poro.empire.common.db.TransactionHelper;

import java.util.logging.Logger;

public final class AuctionStore {
    private final ConnectionProvider connectionProvider;
    private final TransactionHelper transactionHelper;
    private final Logger logger;

    public AuctionStore(ConnectionProvider connectionProvider, TransactionHelper transactionHelper, Logger logger) {
        this.connectionProvider = connectionProvider;
        this.transactionHelper = transactionHelper;
        this.logger = logger;
    }

    public Logger logger() {
        return logger;
    }
}
