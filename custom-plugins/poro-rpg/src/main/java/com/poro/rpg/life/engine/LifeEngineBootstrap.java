package com.poro.rpg.life.engine;

import com.poro.rpg.common.config.FoundationContext;
import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.registry.RegistryBootstrapper;
import com.poro.rpg.common.registry.master.MasterRegistryContext;
import com.poro.rpg.common.registry.master.model.ItemMaster;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;
import com.poro.rpg.common.seed.CsvSeedLoader;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class LifeEngineBootstrap {
    private LifeEngineBootstrap() {
    }

    public static Result<LifeEngineRuntime> bootstrap(
            JavaPlugin plugin,
            FoundationContext foundationContext,
            MasterRegistryContext masterRegistryContext
    ) {
        DomainLogger logger = foundationContext.logger().domain("life-engine");

        Result<Void> installResult = DefaultLifeSeedInstaller.install(plugin, logger);
        if (installResult.isFailure()) {
            return Result.failure(installResult.errorCode(), installResult.message(), installResult.cause());
        }

        LifeGatherNodeRegistry gatherNodeRegistry = new LifeGatherNodeRegistry();
        LifeRecipeRegistry recipeRegistry = new LifeRecipeRegistry();
        LifeSkillExpRegistry skillExpRegistry = new LifeSkillExpRegistry();
        EstateUnlockRuleRegistry unlockRuleRegistry = new EstateUnlockRuleRegistry();
        EstateFacilityRegistry facilityRegistry = new EstateFacilityRegistry();
        EstateFacilityLevelRegistry facilityLevelRegistry = new EstateFacilityLevelRegistry();

        Path seedPath = foundationContext.config().seedPath();
        Result<Void> loadResult = foundationContext.registryBootstrapper().bootstrap(List.of(
                RegistryBootstrapper.task(
                        "life_gather_node",
                        new CsvSeedLoader<>("life_gather_node", seedPath.resolve("life_gather_node.csv"), LifeSeedMappers::gatherNode),
                        list -> list.forEach(gatherNodeRegistry::register)
                ),
                RegistryBootstrapper.task(
                        "life_recipe_master",
                        new CsvSeedLoader<>("life_recipe_master", seedPath.resolve("life_recipe_master.csv"), LifeSeedMappers::recipe),
                        list -> list.forEach(recipeRegistry::register)
                ),
                RegistryBootstrapper.task(
                        "life_skill_exp_table",
                        new CsvSeedLoader<>("life_skill_exp_table", seedPath.resolve("life_skill_exp_table.csv"), LifeSeedMappers::skillExpRule),
                        list -> list.forEach(skillExpRegistry::register)
                ),
                RegistryBootstrapper.task(
                        "estate_unlock_rule",
                        new CsvSeedLoader<>("estate_unlock_rule", seedPath.resolve("estate_unlock_rule.csv"), LifeSeedMappers::estateUnlockRule),
                        list -> list.forEach(unlockRuleRegistry::register)
                ),
                RegistryBootstrapper.task(
                        "estate_facility_master",
                        new CsvSeedLoader<>("estate_facility_master", seedPath.resolve("estate_facility_master.csv"), LifeSeedMappers::estateFacilityMaster),
                        list -> list.forEach(facilityRegistry::register)
                ),
                RegistryBootstrapper.task(
                        "estate_facility_level_rule",
                        new CsvSeedLoader<>("estate_facility_level_rule", seedPath.resolve("estate_facility_level_rule.csv"), LifeSeedMappers::estateFacilityLevelRule),
                        list -> list.forEach(facilityLevelRegistry::register)
                )
        ));
        if (loadResult.isFailure()) {
            return Result.failure(loadResult.errorCode(), loadResult.message(), loadResult.cause());
        }

        Result<Void> validation = validate(
                masterRegistryContext,
                gatherNodeRegistry,
                recipeRegistry,
                skillExpRegistry,
                unlockRuleRegistry,
                facilityRegistry,
                facilityLevelRegistry,
                logger
        );
        if (validation.isFailure()) {
            return Result.failure(validation.errorCode(), validation.message(), validation.cause());
        }

        InMemoryLifeCraftLogHook craftLogHook = new InMemoryLifeCraftLogHook();
        InMemoryEstateHarvestLogHook harvestLogHook = new InMemoryEstateHarvestLogHook();
        LifeSkillProgressionService progressionService = new LifeSkillProgressionService(skillExpRegistry, new NoopLifeLevelUnlockHook());
        EstateFacilityService estateFacilityService = new EstateFacilityService(
                facilityRegistry,
                facilityLevelRegistry,
                progressionService,
                new NoopEstateProductionAmountHook(),
                foundationContext.timeProvider()
        );
        EstateHarvestService estateHarvestService = new EstateHarvestService(
                estateFacilityService,
                harvestLogHook,
                foundationContext.timeProvider(),
                new ThreadLocalRandomProvider()
        );
        EstateSnapshotBuilder snapshotBuilder = new EstateSnapshotBuilder(estateHarvestService, foundationContext.timeProvider());

        LifeGatherService gatherService = new LifeGatherService(
                gatherNodeRegistry,
                progressionService,
                new ThreadLocalRandomProvider()
        );
        LifeCraftService craftService = new LifeCraftService(
                recipeRegistry,
                craftLogHook,
                foundationContext.timeProvider()
        );
        EstateUnlockService estateUnlockService = new EstateUnlockService(
                unlockRuleRegistry,
                new AllowAllEstateUnlockQuestHook(),
                foundationContext.timeProvider()
        );

        logger.info("Life/estate engine bootstrap completed. gather_nodes=" + gatherNodeRegistry.size()
                + ", recipes=" + recipeRegistry.size()
                + ", skill_exp_rows=" + skillExpRegistry.size()
                + ", estate_unlock_rules=" + unlockRuleRegistry.size()
                + ", facilities=" + facilityRegistry.size()
                + ", facility_level_rules=" + facilityLevelRegistry.size());

        return Result.success(new LifeEngineRuntime(
                gatherService,
                craftService,
                progressionService,
                estateUnlockService,
                estateFacilityService,
                estateHarvestService,
                snapshotBuilder,
                gatherNodeRegistry,
                recipeRegistry,
                skillExpRegistry,
                unlockRuleRegistry,
                facilityRegistry,
                facilityLevelRegistry,
                craftLogHook,
                harvestLogHook
        ));
    }

    private static Result<Void> validate(
            MasterRegistryContext masterRegistryContext,
            LifeGatherNodeRegistry gatherNodeRegistry,
            LifeRecipeRegistry recipeRegistry,
            LifeSkillExpRegistry skillExpRegistry,
            EstateUnlockRuleRegistry unlockRuleRegistry,
            EstateFacilityRegistry facilityRegistry,
            EstateFacilityLevelRegistry facilityLevelRegistry,
            DomainLogger logger
    ) {
        List<String> blockingErrors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (LifeGatherNode node : gatherNodeRegistry.all().values()) {
            if (masterRegistryContext.itemMasters().find(node.baseItemId()).isEmpty()) {
                blockingErrors.add("life_gather_node references missing base_item_id: gather_id=" + node.gatherId()
                        + ", base_item_id=" + node.baseItemId());
            }
            if (!isBlank(node.rareItemId()) && !"-".equals(node.rareItemId())
                    && masterRegistryContext.itemMasters().find(node.rareItemId()).isEmpty()) {
                blockingErrors.add("life_gather_node references missing rare_item_id: gather_id=" + node.gatherId()
                        + ", rare_item_id=" + node.rareItemId());
            }
            if (node.sourceType() != LifeSourceType.FIELD) {
                warnings.add("life_gather_node has non-field source type. gather_id=" + node.gatherId()
                        + ", source_type=" + node.sourceType().code());
            }
            if (node.rareChanceMin() < 3.0d || node.rareChanceMax() > 8.0d) {
                warnings.add("Field gather rare chance is outside recommended 3~8%. gather_id=" + node.gatherId()
                        + ", range=" + node.rareChanceMin() + "~" + node.rareChanceMax());
            }
        }

        for (LifeRecipe recipe : recipeRegistry.all().values()) {
            if (isUnknownItem(masterRegistryContext, recipe.resultItemId())) {
                blockingErrors.add("life_recipe_master references missing result_item_id: recipe_id=" + recipe.recipeId()
                        + ", result_item_id=" + recipe.resultItemId());
            }
            for (String materialId : recipe.materials().keySet()) {
                if (isUnknownItem(masterRegistryContext, materialId)) {
                    blockingErrors.add("life_recipe_master references missing material item_id: recipe_id=" + recipe.recipeId()
                            + ", item_id=" + materialId);
                }
            }
            // 공방(crafting/workshop) 가공은 생활 스킬 레벨링이 없어 exp table 면제 (DL-098)
            if (!isExpExempt(recipe.lifeType().code()) && skillExpRegistry.rules(recipe.lifeType()).isEmpty()) {
                blockingErrors.add("life_recipe_master references life_type without exp table: recipe_id=" + recipe.recipeId()
                        + ", life_type=" + recipe.lifeType().code());
            }
        }

        for (EstateUnlockRule unlockRule : unlockRuleRegistry.all().values()) {
            String questId = normalize(unlockRule.unlockQuestId());
            if (!questId.isBlank() && !"none".equals(questId) && !"-".equals(questId)
                    && masterRegistryContext.questMasters().find(questId).isEmpty()) {
                warnings.add("estate_unlock_rule references unknown unlock_quest_id: estate_id=" + unlockRule.estateId()
                        + ", unlock_quest_id=" + unlockRule.unlockQuestId());
            }
            if (!isBlank(unlockRule.firstInstallFacilityId())
                    && !"-".equals(unlockRule.firstInstallFacilityId())
                    && facilityRegistry.find(unlockRule.firstInstallFacilityId()).isEmpty()) {
                warnings.add("estate_unlock_rule references unknown first_install_facility_id: estate_id=" + unlockRule.estateId()
                        + ", first_install_facility_id=" + unlockRule.firstInstallFacilityId());
            }
        }

        for (EstateFacilityMaster facility : facilityRegistry.all().values()) {
            // base_item_id "-"/공백은 물리 아이템 없는 추상 시설(공방 등) — 면제 (DL-098)
            if (!isBlank(facility.baseItemId()) && !"-".equals(facility.baseItemId())
                    && isUnknownItem(masterRegistryContext, facility.baseItemId())) {
                blockingErrors.add("estate_facility_master references missing base_item_id: facility_id=" + facility.facilityId()
                        + ", base_item_id=" + facility.baseItemId());
            }
            if (!isBlank(facility.rareItemId()) && !"-".equals(facility.rareItemId())
                    && isUnknownItem(masterRegistryContext, facility.rareItemId())) {
                blockingErrors.add("estate_facility_master references missing rare_item_id: facility_id=" + facility.facilityId()
                        + ", rare_item_id=" + facility.rareItemId());
            }
            // 공방(crafting/workshop)은 생활 스킬 레벨링 없어 exp table 면제 (DL-098)
            if (!isExpExempt(facility.lifeType().code()) && skillExpRegistry.rules(facility.lifeType()).isEmpty()) {
                blockingErrors.add("estate_facility_master references life_type without exp table: facility_id=" + facility.facilityId()
                        + ", life_type=" + facility.lifeType().code());
            }

            List<EstateFacilityLevelRule> levelRules = facilityLevelRegistry.levels(facility.facilityId());
            for (int level = 1; level <= facility.maxLevel(); level++) {
                if (facilityLevelRegistry.find(facility.facilityId(), level).isEmpty()) {
                    blockingErrors.add("Missing estate_facility_level_rule: facility_id=" + facility.facilityId() + ", level=" + level);
                }
            }

            validateFacilityNumbers(facility, levelRules, warnings, blockingErrors);
        }

        for (ItemMaster item : masterRegistryContext.itemMasters().all().values()) {
            String slotType = normalize(item.slotType());
            if ("material".equals(slotType) || "consumable".equals(slotType)) {
                continue;
            }
            if (item.itemId().startsWith("res_") || item.itemId().startsWith("mat_") || item.itemId().startsWith("con_")) {
                warnings.add("life-related item id should use slot_type material/consumable. item_id=" + item.itemId()
                        + ", slot_type=" + item.slotType());
            }
        }

        warnings.forEach(logger::warn);
        if (!blockingErrors.isEmpty()) {
            blockingErrors.forEach(logger::error);
            return Result.failure(
                    ErrorCode.MASTER_SEED_VALIDATION_FAILED,
                    "Life/estate seed validation failed with " + blockingErrors.size() + " blocking errors."
            );
        }
        return Result.success();
    }

    /** 생활 스킬 레벨링이 없는 life_type(공방 가공) — exp table 검증 면제 (DL-098). */
    private static boolean isExpExempt(String lifeTypeCode) {
        return "crafting".equals(lifeTypeCode) || "workshop".equals(lifeTypeCode);
    }

    /** 커스텀 item_master에도 없고 바닐라 Bukkit Material도 아니면 true (레시피는 바닐라 재료 사용 가능, DL-098). */
    private static boolean isUnknownItem(MasterRegistryContext ctx, String itemId) {
        if (itemId == null || itemId.isBlank()) return true;
        if (ctx.itemMasters().find(itemId).isPresent()) return false;
        return org.bukkit.Material.matchMaterial(itemId) == null;
    }

    private static void validateFacilityNumbers(
            EstateFacilityMaster facility,
            List<EstateFacilityLevelRule> levelRules,
            List<String> warnings,
            List<String> blockingErrors
    ) {
        EstateFacilityLevelRule level1 = findRule(levelRules, 1);
        EstateFacilityLevelRule level2 = findRule(levelRules, 2);
        EstateFacilityLevelRule level3 = findRule(levelRules, 3);

        if (level1 != null) {
            if (level1.baseMin() < 2 || level1.baseMax() > 3) {
                warnings.add("Lv1 production is outside recommended 2~3 per 4h. facility_id=" + facility.facilityId());
            }
            if (level1.rareChancePercent() > 0.0d) {
                blockingErrors.add("Lv1 rare drop must be disabled. facility_id=" + facility.facilityId());
            }
        }
        if (level2 != null) {
            if (level2.baseMin() < 3 || level2.baseMax() > 4) {
                warnings.add("Lv2 production is outside recommended 3~4 per 4h. facility_id=" + facility.facilityId());
            }
            if (level2.rareChancePercent() > 0.0d) {
                blockingErrors.add("Lv2 rare drop must be disabled. facility_id=" + facility.facilityId());
            }
            if (level2.upgradeGoldCost() <= 0L) {
                warnings.add("Lv1->Lv2 upgrade should have gold cost. facility_id=" + facility.facilityId());
            }
        }
        if (level3 != null) {
            if (level3.baseMin() < 4 || level3.baseMax() > 5) {
                warnings.add("Lv3 production is outside recommended 4~5 per 4h. facility_id=" + facility.facilityId());
            }
            if (level3.rareChancePercent() <= 0.0d) {
                blockingErrors.add("Lv3 must unlock rare byproduct chance. facility_id=" + facility.facilityId());
            }
            if (level3.rareChancePercent() < 2.0d || level3.rareChancePercent() > 6.0d) {
                warnings.add("Lv3 rare chance is outside recommended 2~6%. facility_id=" + facility.facilityId());
            }
            if (level2 != null && level3.upgradeGoldCost() <= level2.upgradeGoldCost()) {
                warnings.add("Lv2->Lv3 upgrade cost should be greater than Lv1->Lv2. facility_id=" + facility.facilityId());
            }
        }
    }

    private static EstateFacilityLevelRule findRule(List<EstateFacilityLevelRule> rules, int level) {
        return rules.stream().filter(rule -> rule.level() == level).findFirst().orElse(null);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
