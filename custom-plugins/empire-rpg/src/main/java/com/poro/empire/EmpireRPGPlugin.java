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
import com.poro.empire.combat.CombatStateService;
import com.poro.empire.field.FieldBossRespawnScheduler;
import com.poro.empire.field.FieldTeleportService;
import com.poro.empire.leveling.LevelingService;
import com.poro.empire.boss.room.BossRoomManager;
import com.poro.empire.listener.AfkMonitorListener;
import com.poro.empire.listener.BossDefenseListener;
import com.poro.empire.listener.AuctionGuiListener;
import com.poro.empire.listener.BossRoomListener;
import com.poro.empire.market.AuctionStore;
import com.poro.empire.market.ShopGui;
import com.poro.empire.combat.CooldownManager;
import com.poro.empire.combat.ResourceTracker;
import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.SkillService;
import com.poro.empire.combat.skills.crossbow.CrossbowEvadeFireSkill;
import com.poro.empire.combat.skills.hammer.HammerColossalDropSkill;
import com.poro.empire.combat.skills.scythe.ScytheDeathSlashSkill;
import com.poro.empire.combat.skills.staff.StaffArcaneOrbSkill;
import com.poro.empire.combat.skills.staff.StaffArcaneRushSkill;
import com.poro.empire.combat.skills.staff.StaffElementalBurstSkill;
import com.poro.empire.combat.skills.staff.StaffStarburstSkill;
import com.poro.empire.combat.skills.scythe.ScytheExecutionSkill;
import com.poro.empire.combat.skills.scythe.ScytheGrimStrikeSkill;
import com.poro.empire.combat.skills.scythe.ScytheShadowSpinSkill;
import com.poro.empire.combat.skills.hammer.HammerCrushChargeSkill;
import com.poro.empire.combat.skills.hammer.HammerSmashSkill;
import com.poro.empire.combat.skills.hammer.HammerUnyieldingSkill;
import com.poro.empire.combat.skills.crossbow.CrossbowPierceBoltSkill;
import com.poro.empire.combat.skills.crossbow.CrossbowRapidFireSkill;
import com.poro.empire.combat.skills.crossbow.CrossbowSniperSkill;
import com.poro.empire.combat.skills.spear.SpearChargeSkill;
import com.poro.empire.combat.skills.spear.SpearCrescentSkill;
import com.poro.empire.combat.skills.spear.SpearThunderstrikeSkill;
import com.poro.empire.combat.skills.spear.SpearThrustSkill;
import com.poro.empire.combat.skills.sword.SwordFinalStrikeSkill;
import com.poro.empire.combat.skills.sword.SwordFlashSlashSkill;
import com.poro.empire.combat.skills.sword.SwordGuardCounterSkill;
import com.poro.empire.combat.skills.sword.SwordTripleStrikeSkill;
import com.poro.empire.growth.engine.GrowthEngineBootstrap;
import com.poro.empire.growth.engine.GrowthEngineRuntime;
import com.poro.empire.life.engine.LifeEngineBootstrap;
import com.poro.empire.life.engine.LifeEngineRuntime;
import com.poro.empire.listener.CombatStateListener;
import com.poro.empire.listener.CrossbowArrowListener;
import com.poro.empire.listener.HealthHudListener;
import com.poro.empire.listener.HungerLockListener;
import com.poro.empire.listener.SkillInputListener;
import com.poro.empire.listener.StaffProjectileListener;
import com.poro.empire.listener.SwordParryListener;
import com.poro.empire.common.flag.PlayerFlagRepository;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.island.IslandStorageStore;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import com.poro.empire.gui.ExploreHubGui;
import com.poro.empire.gui.ExploreHubRefresher;
import com.poro.empire.hotbar.HotbarService;
import com.poro.empire.listener.FarmGuiListener;
import com.poro.empire.listener.FieldDropListener;
import com.poro.empire.listener.GrowthGuiListener;
import com.poro.empire.listener.HeirloomGuiListener;
import com.poro.empire.listener.HotbarInteractListener;
import com.poro.empire.listener.MainHubListener;
import com.poro.empire.listener.PlayerJoinListener;
import com.poro.empire.listener.StorageGuiListener;
import com.poro.empire.listener.TerritoryStatusGuiListener;
import com.poro.empire.listener.ConsumableUseListener;
import com.poro.empire.listener.WorkshopGuiListener;
import com.poro.empire.listener.WeaponSelectionGuiListener;
import com.poro.empire.growth.engine.CatalystConfig;
import com.poro.empire.growth.island.MachineProductionScheduler;
import com.poro.empire.scoreboard.ScoreboardService;
import com.poro.empire.npc.citizens.NpcSyncBootstrap;
import com.poro.empire.npc.citizens.NpcSyncRuntime;
import com.poro.empire.operations.query.OperationsQueryBootstrap;
import com.poro.empire.operations.query.OperationsQueryRuntime;
import com.poro.empire.quest.engine.QuestAchievementBootstrap;
import com.poro.empire.quest.engine.QuestAchievementRuntime;
import com.poro.empire.persistence.PlayerDataRepository;
import com.poro.empire.persistence.PlayerPersistenceService;
import com.poro.empire.reputation.ReputationManager;
import com.poro.empire.storage.PlayerDataManager;
import com.poro.empire.tutorial.TutorialService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
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
    private ResourceTracker resourceTracker;
    private HotbarService hotbarService;
    private TutorialService tutorialService;
    private ScoreboardService scoreboardService;
    private GrowthStateStore growthStateStore;
    private IslandStorageStore islandStorageStore;
    private IslandTerritoryStateStore islandTerritoryStateStore;
    private PlayerPersistenceService playerPersistenceService;
    private CooldownManager cooldownManager;
    private CombatStateService combatStateService;
    private LevelingService levelingService;
    private AuctionStore     auctionStore;
    private BossRoomManager  bossRoomManager;

    @Override
    public void onEnable() {
        getLogger().info("EmpireRPG enabled.");
        saveDefaultConfig();
        CatalystConfig.reload(getConfig());

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
        this.hotbarService = new HotbarService(this);
        this.scoreboardService = new ScoreboardService();
        this.growthStateStore = new GrowthStateStore();
        this.islandStorageStore = new IslandStorageStore();
        this.islandTerritoryStateStore = new IslandTerritoryStateStore();

        PlayerDataRepository playerDataRepository = new PlayerDataRepository(this);
        this.playerPersistenceService = new PlayerPersistenceService(
                playerDataRepository, playerDataManager, growthStateStore,
                islandTerritoryStateStore, islandStorageStore, getLogger());

        PlayerFlagRepository flagRepo = new PlayerFlagRepository(
                foundationContext.transactionHelper(),
                foundationContext.logger().domain("tutorial-flag")
        );
        this.tutorialService = new TutorialService(
                this, flagRepo, playerDataManager, hotbarService,
                foundationContext.logger().domain("tutorial")
        );

        this.cooldownManager = new CooldownManager();
        this.combatStateService = new CombatStateService();
        this.levelingService = new LevelingService();
        this.bossRoomManager = new BossRoomManager();
        this.auctionStore = new AuctionStore(
                foundationContext.connectionProvider(),
                foundationContext.transactionHelper(),
                getLogger());
        ShopGui.reloadItems(this);
        this.resourceTracker = new ResourceTracker();
        SkillContext skillContext = new SkillContext(
                playerDataManager, this.cooldownManager, this.resourceTracker,
                growthStateStore,
                masterRegistryContext.itemMasters(),
                growthEngineRuntime.potentialService());

        this.skillService = new SkillService(skillContext);
        skillService.registerSkill(new SwordFlashSlashSkill());
        skillService.registerSkill(new SwordTripleStrikeSkill(this));
        skillService.registerSkill(new SwordGuardCounterSkill(this));
        skillService.registerSkill(new SwordFinalStrikeSkill());
        skillService.registerSkill(new SpearThrustSkill());
        skillService.registerSkill(new SpearCrescentSkill());
        skillService.registerSkill(new SpearChargeSkill());
        skillService.registerSkill(new SpearThunderstrikeSkill());
        skillService.registerSkill(new CrossbowRapidFireSkill(this));
        skillService.registerSkill(new CrossbowEvadeFireSkill(this));
        skillService.registerSkill(new CrossbowPierceBoltSkill(this));
        skillService.registerSkill(new CrossbowSniperSkill(this));
        skillService.registerSkill(new HammerSmashSkill());
        skillService.registerSkill(new HammerCrushChargeSkill());
        skillService.registerSkill(new HammerUnyieldingSkill(this));
        skillService.registerSkill(new HammerColossalDropSkill());
        skillService.registerSkill(new ScytheDeathSlashSkill());
        skillService.registerSkill(new ScytheShadowSpinSkill(this));
        skillService.registerSkill(new ScytheGrimStrikeSkill());
        skillService.registerSkill(new ScytheExecutionSkill());
        skillService.registerSkill(new StaffArcaneOrbSkill(this));
        skillService.registerSkill(new StaffElementalBurstSkill(this));
        skillService.registerSkill(new StaffArcaneRushSkill());
        skillService.registerSkill(new StaffStarburstSkill(this));

        registerCommands();
        FieldBossRespawnScheduler fieldBossScheduler = getServer().getPluginManager().isPluginEnabled("MythicMobs")
                ? new FieldBossRespawnScheduler(this) : null;
        ExploreHubGui.FieldStateProvider fieldStateProvider = fieldBossScheduler != null
                ? fieldBossScheduler : buildFieldStateProvider();
        registerListeners(fieldStateProvider, fieldBossScheduler);
        ExploreHubRefresher.start(this, fieldStateProvider); // uses the scheduler as provider

        // 10분(12000틱)마다 영지 기계 생산
        new MachineProductionScheduler(this, islandTerritoryStateStore).start();

        // 30초(600틱)마다 마력 과부하 경고
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                islandTerritoryStateStore.get(p.getUniqueId()).ifPresent(territory -> {
                    if (!territory.isOverloaded()) return;
                    p.sendMessage("§f[§e포로§f] §c⚠ 마력 과부하! 소비 기계 전부 정지. 발전기를 추가하거나 기계를 줄이세요.");
                    p.sendActionBar(Component.text("§c⚡ 마력 과부하! — 기계 전부 정지 중"));
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                });
            }
        }, 600L, 600L);

        // 5분(6000틱)마다 온라인 플레이어 자동 저장
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () ->
                playerPersistenceService.saveAll(
                        Bukkit.getOnlinePlayers().stream()
                                .map(p -> p.getUniqueId())
                                .toList()
                ), 6000L, 6000L);
    }

    @Override
    public void onDisable() {
        // 서버 종료 시 온라인 전원 저장
        playerPersistenceService.saveAll(
                Bukkit.getOnlinePlayers().stream()
                        .map(p -> p.getUniqueId())
                        .toList()
        );
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

    private void registerListeners(ExploreHubGui.FieldStateProvider fieldStateProvider,
                                   FieldBossRespawnScheduler fieldBossScheduler) {

        getServer().getPluginManager().registerEvents(
                new PlayerJoinListener(this, playerDataManager, hotbarService, tutorialService, scoreboardService, playerPersistenceService, growthStateStore, auctionStore), this);
        getServer().getPluginManager().registerEvents(
                new WeaponSelectionGuiListener(playerDataManager, tutorialService, scoreboardService, growthStateStore, cooldownManager, combatStateService), this);
        getServer().getPluginManager().registerEvents(
                new CombatStateListener(combatStateService), this);
        getServer().getPluginManager().registerEvents(
                new HotbarInteractListener(hotbarService, fieldStateProvider, combatStateService), this);
        FieldTeleportService fieldTeleportService = new FieldTeleportService(this);
        BossRoomListener bossRoomListenerInstance =
                new BossRoomListener(this, bossRoomManager, masterRegistryContext.bossMasters());
        getServer().getPluginManager().registerEvents(
                new MainHubListener(this, fieldStateProvider, growthStateStore, growthEngineRuntime, scoreboardService, playerDataManager, islandStorageStore, islandTerritoryStateStore, fieldTeleportService, combatStateService, auctionStore,
                        bossRoomListenerInstance), this);
        getServer().getPluginManager().registerEvents(bossRoomListenerInstance, this);
        getServer().getPluginManager().registerEvents(
                new GrowthGuiListener(growthStateStore, growthEngineRuntime, playerDataManager, scoreboardService,
                        masterRegistryContext.itemMasters(), combatStateService, this), this);
        getServer().getPluginManager().registerEvents(
                new StorageGuiListener(islandStorageStore), this);
        getServer().getPluginManager().registerEvents(
                new TerritoryStatusGuiListener(islandTerritoryStateStore, islandStorageStore, growthStateStore, playerDataManager), this);
        getServer().getPluginManager().registerEvents(
                new WorkshopGuiListener(islandTerritoryStateStore, this), this);
        getServer().getPluginManager().registerEvents(
                new HeirloomGuiListener(islandTerritoryStateStore, growthStateStore, playerDataManager), this);
        getServer().getPluginManager().registerEvents(
                new FarmGuiListener(islandTerritoryStateStore, islandStorageStore), this);
        if (getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            getServer().getPluginManager().registerEvents(
                    new FieldDropListener(growthStateStore, islandTerritoryStateStore, playerDataManager,
                            levelingService, fieldBossScheduler), this);
            getServer().getPluginManager().registerEvents(new BossDefenseListener(), this);
            getLogger().info("MythicMobs detected — FieldDropListener + BossDefenseListener registered.");
        } else {
            getLogger().warning("MythicMobs not found — field mob drops + boss DEF scaling disabled.");
        }
        getServer().getPluginManager().registerEvents(new AfkMonitorListener(this), this);
        getServer().getPluginManager().registerEvents(new HungerLockListener(), this);
        getServer().getPluginManager().registerEvents(
                new ConsumableUseListener(bossRoomManager, this), this);
        getServer().getPluginManager().registerEvents(new HealthHudListener(this), this);
        getServer().getPluginManager().registerEvents(new SwordParryListener(), this);
        getServer().getPluginManager().registerEvents(new CrossbowArrowListener(resourceTracker), this);
        getServer().getPluginManager().registerEvents(new StaffProjectileListener(resourceTracker), this);
        getServer().getPluginManager().registerEvents(new SkillInputListener(skillService), this);
        getServer().getPluginManager().registerEvents(
                new AuctionGuiListener(this, growthStateStore, auctionStore), this);
    }

    /** BossEngineRuntime 연동 전 stub — 모든 필드보스를 리스폰 대기 상태로 반환. */
    private ExploreHubGui.FieldStateProvider buildFieldStateProvider() {
        return new ExploreHubGui.FieldStateProvider() {
            @Override
            public BossStatus status(String fieldId) {
                return BossStatus.RESPAWNING;
            }
            @Override
            public int respawnMinutes(String fieldId) {
                return 30;
            }
            @Override
            public int playerCount(String fieldId) {
                return 0;
            }
        };
    }

    private void registerCommands() {
        EmpireCommand empireCommand = new EmpireCommand(playerDataManager, skillService, reputationManager, this);

        if (getCommand("empire") == null) {
            getLogger().severe("Failed to register /empire command. Check plugin.yml.");
            return;
        }

        getCommand("empire").setExecutor(empireCommand);
        getCommand("empire").setTabCompleter(empireCommand);
    }
}
