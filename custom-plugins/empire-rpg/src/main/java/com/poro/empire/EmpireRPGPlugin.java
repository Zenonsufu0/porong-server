package com.poro.empire;

import com.poro.empire.boss.engine.BossEngineBootstrap;
import com.poro.empire.boss.engine.BossEngineRuntime;
import com.poro.empire.combat.engine.CombatEngineBootstrap;
import com.poro.empire.combat.engine.CombatEngineRuntime;
import com.poro.empire.common.config.CommonFoundationBootstrap;
import com.poro.empire.common.config.FoundationContext;
import com.poro.empire.common.registry.master.MasterRegistryBootstrap;
import com.poro.empire.common.registry.master.MasterRegistryContext;
import com.poro.empire.common.result.Result;
import com.poro.empire.command.EmpireCommand;
import com.poro.empire.combat.CooldownManager;
import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.SkillService;
import com.poro.empire.combat.skills.AssassinShadowstepSkill;
import com.poro.empire.combat.skills.MageFireboltSkill;
import com.poro.empire.combat.skills.WarriorSlashSkill;
import com.poro.empire.growth.engine.GrowthEngineBootstrap;
import com.poro.empire.growth.engine.GrowthEngineRuntime;
import com.poro.empire.life.engine.LifeEngineBootstrap;
import com.poro.empire.life.engine.LifeEngineRuntime;
import com.poro.empire.listener.HealthHudListener;
import com.poro.empire.listener.HungerLockListener;
import com.poro.empire.npc.citizens.NpcSyncBootstrap;
import com.poro.empire.npc.citizens.NpcSyncRuntime;
import com.poro.empire.operations.query.OperationsQueryBootstrap;
import com.poro.empire.operations.query.OperationsQueryRuntime;
import com.poro.empire.quest.engine.QuestAchievementBootstrap;
import com.poro.empire.quest.engine.QuestAchievementRuntime;
import com.poro.empire.reputation.ReputationManager;
import com.poro.empire.storage.PlayerDataManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class EmpireRPGPlugin extends JavaPlugin {
    private FoundationContext foundationContext;
    private MasterRegistryContext masterRegistryContext;
    private CombatEngineRuntime combatEngineRuntime;
    private BossEngineRuntime bossEngineRuntime;
    private GrowthEngineRuntime growthEngineRuntime;
    private LifeEngineRuntime lifeEngineRuntime;
    private QuestAchievementRuntime questAchievementRuntime;
    private NpcSyncRuntime npcSyncRuntime;
    private OperationsQueryRuntime operationsQueryRuntime;
    private PlayerDataManager playerDataManager;
    private SkillService skillService;
    private ReputationManager reputationManager;

    @Override
    public void onEnable() {
        getLogger().info("EmpireRPG enabled.");
        saveDefaultConfig();

        Result<FoundationContext> foundationResult = CommonFoundationBootstrap.bootstrap(this);
        if (foundationResult.isFailure()) {
            getLogger().severe("Failed to initialize common foundation: " + foundationResult.message());
            if (foundationResult.cause() != null) {
                getLogger().severe("Cause: " + foundationResult.cause().getMessage());
            }
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.foundationContext = foundationResult.value();
        this.foundationContext.logger().domain("foundation").info("Foundation bootstrap completed.");

        Result<Void> dbInitResult = this.foundationContext.databaseBootstrapper().initialize();
        if (dbInitResult.isFailure()) {
            getLogger().severe("Failed to initialize database: " + dbInitResult.message());
            if (dbInitResult.cause() != null) {
                getLogger().severe("Cause: " + dbInitResult.cause().getMessage());
            }
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.foundationContext.logger().domain("db.migration").info("Database initialize completed.");

        Result<MasterRegistryContext> masterRegistryResult = MasterRegistryBootstrap.bootstrap(this, foundationContext);
        if (masterRegistryResult.isFailure()) {
            getLogger().severe("Failed to load master registry: " + masterRegistryResult.message());
            if (masterRegistryResult.cause() != null) {
                getLogger().severe("Cause: " + masterRegistryResult.cause().getMessage());
            }
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.masterRegistryContext = masterRegistryResult.value();
        this.foundationContext.logger().domain("master-registry").info("Master registry bootstrap completed.");

        Result<NpcSyncRuntime> npcSyncResult = NpcSyncBootstrap.bootstrap(this, foundationContext, masterRegistryContext);
        if (npcSyncResult.isFailure()) {
            getLogger().severe("Failed to initialize NPC sync module: " + npcSyncResult.message());
            if (npcSyncResult.cause() != null) {
                getLogger().severe("Cause: " + npcSyncResult.cause().getMessage());
            }
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.npcSyncRuntime = npcSyncResult.value();
        this.foundationContext.logger().domain("npc-sync").info("NPC sync bootstrap completed.");

        Result<CombatEngineRuntime> combatEngineResult = CombatEngineBootstrap.bootstrap(this, foundationContext, masterRegistryContext);
        if (combatEngineResult.isFailure()) {
            getLogger().severe("Failed to initialize combat engine: " + combatEngineResult.message());
            if (combatEngineResult.cause() != null) {
                getLogger().severe("Cause: " + combatEngineResult.cause().getMessage());
            }
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.combatEngineRuntime = combatEngineResult.value();
        this.foundationContext.logger().domain("combat-engine").info("Combat engine bootstrap completed.");

        Result<BossEngineRuntime> bossEngineResult = BossEngineBootstrap.bootstrap(this, foundationContext, masterRegistryContext);
        if (bossEngineResult.isFailure()) {
            getLogger().severe("Failed to initialize boss engine: " + bossEngineResult.message());
            if (bossEngineResult.cause() != null) {
                getLogger().severe("Cause: " + bossEngineResult.cause().getMessage());
            }
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.bossEngineRuntime = bossEngineResult.value();
        this.foundationContext.logger().domain("boss-engine").info("Boss engine bootstrap completed.");

        Result<GrowthEngineRuntime> growthEngineResult = GrowthEngineBootstrap.bootstrap(this, foundationContext, masterRegistryContext);
        if (growthEngineResult.isFailure()) {
            getLogger().severe("Failed to initialize growth engine: " + growthEngineResult.message());
            if (growthEngineResult.cause() != null) {
                getLogger().severe("Cause: " + growthEngineResult.cause().getMessage());
            }
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.growthEngineRuntime = growthEngineResult.value();
        this.foundationContext.logger().domain("growth-engine").info("Growth engine bootstrap completed.");

        Result<LifeEngineRuntime> lifeEngineResult = LifeEngineBootstrap.bootstrap(this, foundationContext, masterRegistryContext);
        if (lifeEngineResult.isFailure()) {
            getLogger().severe("Failed to initialize life/estate engine: " + lifeEngineResult.message());
            if (lifeEngineResult.cause() != null) {
                getLogger().severe("Cause: " + lifeEngineResult.cause().getMessage());
            }
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.lifeEngineRuntime = lifeEngineResult.value();
        this.foundationContext.logger().domain("life-engine").info("Life/estate engine bootstrap completed.");

        Result<QuestAchievementRuntime> questAchievementResult = QuestAchievementBootstrap.bootstrap(this, foundationContext, masterRegistryContext);
        if (questAchievementResult.isFailure()) {
            getLogger().severe("Failed to initialize quest/achievement engine: " + questAchievementResult.message());
            if (questAchievementResult.cause() != null) {
                getLogger().severe("Cause: " + questAchievementResult.cause().getMessage());
            }
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.questAchievementRuntime = questAchievementResult.value();
        this.foundationContext.logger().domain("quest-achievement-engine").info("Quest/achievement engine bootstrap completed.");

        Result<OperationsQueryRuntime> operationsQueryResult = OperationsQueryBootstrap.bootstrap(
                this,
                foundationContext,
                masterRegistryContext,
                bossEngineRuntime,
                growthEngineRuntime,
                lifeEngineRuntime,
                questAchievementRuntime
        );
        if (operationsQueryResult.isFailure()) {
            getLogger().severe("Failed to initialize operations/query engine: " + operationsQueryResult.message());
            if (operationsQueryResult.cause() != null) {
                getLogger().severe("Cause: " + operationsQueryResult.cause().getMessage());
            }
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.operationsQueryRuntime = operationsQueryResult.value();
        this.foundationContext.logger().domain("operations-query").info("Operations/query bootstrap completed.");

        this.playerDataManager = new PlayerDataManager();
        this.reputationManager = new ReputationManager(playerDataManager);
        CooldownManager cooldownManager = new CooldownManager();
        SkillContext skillContext = new SkillContext(playerDataManager, cooldownManager);

        this.skillService = new SkillService(skillContext);
        skillService.registerSkill(new WarriorSlashSkill());
        skillService.registerSkill(new AssassinShadowstepSkill());
        skillService.registerSkill(new MageFireboltSkill());

        registerCommands();
        registerListeners();
    }

    @Override
    public void onDisable() {
        getLogger().info("EmpireRPG disabled.");
    }

    public FoundationContext getFoundationContext() {
        return foundationContext;
    }

    public MasterRegistryContext getMasterRegistryContext() {
        return masterRegistryContext;
    }

    public CombatEngineRuntime getCombatEngineRuntime() {
        return combatEngineRuntime;
    }

    public BossEngineRuntime getBossEngineRuntime() {
        return bossEngineRuntime;
    }

    public GrowthEngineRuntime getGrowthEngineRuntime() {
        return growthEngineRuntime;
    }

    public LifeEngineRuntime getLifeEngineRuntime() {
        return lifeEngineRuntime;
    }

    public QuestAchievementRuntime getQuestAchievementRuntime() {
        return questAchievementRuntime;
    }

    public NpcSyncRuntime getNpcSyncRuntime() {
        return npcSyncRuntime;
    }

    public OperationsQueryRuntime getOperationsQueryRuntime() {
        return operationsQueryRuntime;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public ReputationManager getReputationManager() {
        return reputationManager;
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new HungerLockListener(), this);
        getServer().getPluginManager().registerEvents(new HealthHudListener(this), this);
    }

    private void registerCommands() {
        EmpireCommand empireCommand = new EmpireCommand(playerDataManager, skillService, reputationManager);

        if (getCommand("empire") == null) {
            getLogger().severe("Failed to register /empire command. Check plugin.yml.");
            return;
        }

        getCommand("empire").setExecutor(empireCommand);
        getCommand("empire").setTabCompleter(empireCommand);
    }
}
