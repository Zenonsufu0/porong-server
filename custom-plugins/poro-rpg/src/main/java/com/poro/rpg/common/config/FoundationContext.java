package com.poro.rpg.common.config;

import com.poro.rpg.common.db.ConnectionProvider;
import com.poro.rpg.common.db.DatabaseBootstrapper;
import com.poro.rpg.common.db.TransactionHelper;
import com.poro.rpg.common.logging.CommonPluginLogger;
import com.poro.rpg.common.registry.RegistryBootstrapper;
import com.poro.rpg.common.time.TimeProvider;

public record FoundationContext(
        CommonConfig config,
        TimeProvider timeProvider,
        CommonPluginLogger logger,
        ConnectionProvider connectionProvider,
        TransactionHelper transactionHelper,
        RegistryBootstrapper registryBootstrapper,
        DatabaseBootstrapper databaseBootstrapper
) {
}
