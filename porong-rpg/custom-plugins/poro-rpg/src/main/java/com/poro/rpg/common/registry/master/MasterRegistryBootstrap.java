package com.poro.rpg.common.registry.master;

import com.poro.rpg.common.config.FoundationContext;
import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.result.Result;
import com.poro.rpg.common.seed.CsvSeedLoader;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.List;

public final class MasterRegistryBootstrap {
    private MasterRegistryBootstrap() {
    }

    public static Result<MasterRegistryContext> bootstrap(JavaPlugin plugin, FoundationContext foundationContext) {
        DomainLogger logger = foundationContext.logger().domain("master-registry");

        Result<Void> installResult = DefaultMasterSeedInstaller.install(plugin, logger);
        if (installResult.isFailure()) {
            return Result.failure(installResult.errorCode(), installResult.message(), installResult.cause());
        }

        MasterRegistryContext context = new MasterRegistryContext(
                new ItemMasterRegistry(),
                new SkillMasterRegistry(),
                new BossMasterRegistry(),
                new QuestMasterRegistry(),
                new AchievementMasterRegistry(),
                new RegionMasterRegistry(),
                new TownMasterRegistry(),
                new NpcMasterRegistry()
        );

        Path seedPath = foundationContext.config().seedPath();
        Result<Void> loadResult = foundationContext.registryBootstrapper().bootstrap(List.of(
                com.poro.rpg.common.registry.RegistryBootstrapper.task(
                        "item_master",
                        new CsvSeedLoader<>("item_master", seedPath.resolve("item_master.csv"), MasterCsvMappers::item),
                        list -> list.forEach(context.itemMasters()::register)
                ),
                com.poro.rpg.common.registry.RegistryBootstrapper.task(
                        "skill_master",
                        new CsvSeedLoader<>("skill_master", seedPath.resolve("skill_master.csv"), MasterCsvMappers::skill),
                        list -> list.forEach(context.skillMasters()::register)
                ),
                com.poro.rpg.common.registry.RegistryBootstrapper.task(
                        "boss_master",
                        new CsvSeedLoader<>("boss_master", seedPath.resolve("boss_master.csv"), MasterCsvMappers::boss),
                        list -> list.forEach(context.bossMasters()::register)
                ),
                com.poro.rpg.common.registry.RegistryBootstrapper.task(
                        "quest_master",
                        new CsvSeedLoader<>("quest_master", seedPath.resolve("quest_master.csv"), MasterCsvMappers::quest),
                        list -> list.forEach(context.questMasters()::register)
                ),
                com.poro.rpg.common.registry.RegistryBootstrapper.task(
                        "achievement_master",
                        new CsvSeedLoader<>("achievement_master", seedPath.resolve("achievement_master.csv"), MasterCsvMappers::achievement),
                        list -> list.forEach(context.achievementMasters()::register)
                ),
                com.poro.rpg.common.registry.RegistryBootstrapper.task(
                        "region_master",
                        new CsvSeedLoader<>("region_master", seedPath.resolve("region_master.csv"), MasterCsvMappers::region),
                        list -> list.forEach(context.regionMasters()::register)
                ),
                com.poro.rpg.common.registry.RegistryBootstrapper.task(
                        "town_master",
                        new CsvSeedLoader<>("town_master", seedPath.resolve("town_master.csv"), MasterCsvMappers::town),
                        list -> list.forEach(context.townMasters()::register)
                ),
                com.poro.rpg.common.registry.RegistryBootstrapper.task(
                        "npc_master",
                        new CsvSeedLoader<>("npc_master", seedPath.resolve("npc_master.csv"), MasterCsvMappers::npc),
                        list -> list.forEach(context.npcMasters()::register)
                )
        ));
        if (loadResult.isFailure()) {
            logger.error("Failed to load master registries: " + loadResult.message(), loadResult.cause());
            return Result.failure(loadResult.errorCode(), loadResult.message(), loadResult.cause());
        }

        Result<Void> validationResult = MasterSeedValidator.validate(context, foundationContext.logger().domain("master-validation"));
        if (validationResult.isFailure()) {
            return Result.failure(validationResult.errorCode(), validationResult.message(), validationResult.cause());
        }

        logger.info("Master registries loaded successfully. items=" + context.itemMasters().size()
                + ", skills=" + context.skillMasters().size()
                + ", bosses=" + context.bossMasters().size()
                + ", quests=" + context.questMasters().size()
                + ", achievements=" + context.achievementMasters().size()
                + ", regions=" + context.regionMasters().size()
                + ", towns=" + context.townMasters().size()
                + ", npcs=" + context.npcMasters().size());
        return Result.success(context);
    }
}
