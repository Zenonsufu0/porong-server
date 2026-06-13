package kr.zenon.rpg.common.config;

import kr.zenon.rpg.common.db.ConnectionProvider;
import kr.zenon.rpg.common.db.DatabaseBootstrapper;
import kr.zenon.rpg.common.db.TransactionHelper;
import kr.zenon.rpg.common.logging.CommonPluginLogger;
import kr.zenon.rpg.common.registry.RegistryBootstrapper;
import kr.zenon.rpg.common.time.TimeProvider;

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
