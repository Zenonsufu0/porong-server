package com.poro.empire;

import com.poro.empire.boss.engine.BossEngineBootstrap;
import com.poro.empire.boss.engine.BossEngineRuntime;
import com.poro.empire.boss.engine.BossRewardService;
import com.poro.empire.combat.engine.CombatEngineBootstrap;
import com.poro.empire.combat.engine.CombatEngineRuntime;
import com.poro.empire.common.config.CommonFoundationBootstrap;
import com.poro.empire.common.config.FoundationContext;
import com.poro.empire.common.registry.master.MasterRegistryBootstrap;
import com.poro.empire.common.registry.master.MasterRegistryContext;
import com.poro.empire.common.result.Result;
import com.poro.empire.boss.room.BossRoomGenerationService;
import com.poro.empire.command.BossRoomGenCommand;
import com.poro.empire.command.PvpArenaGenCommand;
import com.poro.empire.pvp.PvpArenaGenerationService;
import com.poro.empire.pvp.PvpArenaManager;
import com.poro.empire.command.ClassAdminCommand;
import com.poro.empire.command.EmpireCommand;
import com.poro.empire.command.PlayerCommandRouter;
import com.poro.empire.init.ClassInitService;
import com.poro.empire.combat.CombatStateService;
import com.poro.empire.field.ContributionTracker;
import com.poro.empire.field.FieldBossRespawnScheduler;
import com.poro.empire.field.FieldTeleportService;
import com.poro.empire.field.NoopSafeZoneService;
import com.poro.empire.field.SafeZoneService;
import com.poro.empire.field.WorldGuardSafeZoneService;
import com.poro.empire.leveling.PlayerLevelingService;
import com.poro.empire.boss.room.BossRoomManager;
import com.poro.empire.listener.AfkMonitorListener;
import com.poro.empire.listener.DeathKeepInventoryListener;
import com.poro.empire.listener.IslandProtectionListener;
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
import com.poro.empire.combat.skills.axe.AxeColossalDropSkill;
import com.poro.empire.combat.skills.scythe.ScytheDeathSlashSkill;
import com.poro.empire.combat.skills.staff.StaffArcaneOrbSkill;
import com.poro.empire.combat.skills.staff.StaffArcaneRushSkill;
import com.poro.empire.combat.skills.staff.StaffElementalBurstSkill;
import com.poro.empire.combat.skills.staff.StaffStarburstSkill;
import com.poro.empire.combat.skills.scythe.ScytheExecutionSkill;
import com.poro.empire.combat.skills.scythe.ScytheGrimStrikeSkill;
import com.poro.empire.combat.skills.scythe.ScytheShadowSpinSkill;
import com.poro.empire.combat.skills.axe.AxeCrushChargeSkill;
import com.poro.empire.combat.skills.axe.AxeSmashSkill;
import com.poro.empire.combat.skills.axe.AxeUnyieldingSkill;
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
import com.poro.empire.boss.party.PartyManager;
import com.poro.empire.listener.BossHubListener;
import com.poro.empire.listener.FarmGuiListener;
import com.poro.empire.listener.FieldDropListener;
import com.poro.empire.listener.FieldHubListener;
import com.poro.empire.listener.GrowthGuiListener;
import com.poro.empire.listener.HeirloomGuiListener;
import com.poro.empire.listener.HotbarInteractListener;
import com.poro.empire.listener.MainHubListener;
import com.poro.empire.listener.PlayerJoinListener;
import com.poro.empire.listener.StorageGuiListener;
import com.poro.empire.listener.TerritorySettingsGuiListener;
import com.poro.empire.listener.PvpDamageListener;
import com.poro.empire.listener.PvpHubListener;
import com.poro.empire.listener.PvpTeleportListener;
import com.poro.empire.listener.ShopGuiListener;
import com.poro.empire.pvp.PvpFriendlyService;
import com.poro.empire.pvp.PvpMatchService;
import com.poro.empire.pvp.PvpRatingService;
import com.poro.empire.pvp.db.PvpMatchLogRepository;
import com.poro.empire.pvp.db.PvpRatingRepository;
import com.poro.empire.listener.TerritoryStatusGuiListener;
import com.poro.empire.listener.ConsumableUseListener;
import com.poro.empire.listener.WorkshopGuiListener;
import com.poro.empire.listener.WeaponSelectionGuiListener;
import com.poro.empire.listener.EmpireItemGuardListener;
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
    private com.poro.empire.boss.engine.BossPatternMythicMapping bossPatternMythicMapping;
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
    private ClassInitService classInitService;
    private PlayerLevelingService playerLevelingService;
    private AuctionStore        auctionStore;
    private AuctionGuiListener  auctionGuiListener;
    private GrowthGuiListener   growthGuiListener;
    private com.poro.empire.boss.party.PartyManager partyManager;
    private FieldHubListener    fieldHubListener;
    private BossHubListener     bossHubListener;
    private ShopGuiListener     shopGuiListener;
    private TerritorySettingsGuiListener territorySettingsGuiListener;
    private PvpHubListener      pvpHubListener;
    private PvpRatingService    pvpRatingService;
    private PvpArenaManager     pvpArenaManager;
    private PvpMatchService     pvpMatchService;
    private PvpMatchLogRepository pvpMatchLogRepo;
    private com.poro.empire.persistence.PlayerSessionRepository playerSessionRepo;
    private com.poro.empire.persistence.GrowthSnapshotRepository growthSnapshotRepo;
    private com.poro.empire.admin.AdminTogglesService adminTogglesService;
    private BossRoomManager     bossRoomManager;
    private com.poro.empire.boss.room.BossDamageTracker bossDamageTracker;
    private BossRewardService   bossRewardService;

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
        // NPC 동기화는 비핵심(편의 NPC) — 실패해도 플러그인을 죽이지 않고 NPC만 비활성 (DL-098).
        // 예: 월드 미생성(world_main 등)으로 NPC 배치 불가 시에도 코어(전투·성장·보스·GUI)는 정상 가동.
        if (npcSyncResult.isFailure()) {
            getLogger().warning("NPC sync 비활성 (핵심 기능은 정상): " + npcSyncResult.message()
                    + (npcSyncResult.cause() != null ? " — " + npcSyncResult.cause().getMessage() : ""));
            this.npcSyncRuntime = null;
        } else {
            this.npcSyncRuntime = npcSyncResult.value();
            this.foundationContext.logger().domain("npc-sync").info("NPC sync bootstrap completed.");
        }

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

        this.playerDataManager = new PlayerDataManager();
        this.growthStateStore = new GrowthStateStore();
        this.islandStorageStore = new IslandStorageStore();
        this.islandTerritoryStateStore = new IslandTerritoryStateStore();
        this.bossRoomManager = BossRoomManager.fromConfig(this);
        this.bossDamageTracker = new com.poro.empire.boss.room.BossDamageTracker();
        this.bossRewardService = new BossRewardService(
                growthStateStore, islandTerritoryStateStore, playerDataManager, bossRoomManager, getLogger());

        Result<BossEngineRuntime> bossEngineResult = BossEngineBootstrap.bootstrap(this, foundationContext, masterRegistryContext, this.bossRewardService, this::resolveBossParticipantSpec, this.bossDamageTracker, this.bossRoomManager);
        if (bossEngineResult.isFailure()) {
            getLogger().severe("Failed to initialize boss engine: " + bossEngineResult.message());
            if (bossEngineResult.cause() != null) {
                getLogger().severe("Cause: " + bossEngineResult.cause().getMessage());
            }
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.bossEngineRuntime = bossEngineResult.value();
        // 보스 패턴 ↔ Mythic 스킬 매핑 로드 (DL-077 잔여 3/3 — future hook용)
        this.bossPatternMythicMapping = new com.poro.empire.boss.engine.BossPatternMythicMapping();
        this.bossPatternMythicMapping.loadFromResource(this, "seeds/boss_pattern_mythic_mapping.csv");
        this.bossRewardService.setSeasonContext(
                this.bossEngineRuntime.bossSessionRepository(),
                foundationContext.config().seasonStartEpoch());
        // 보스 클리어 게이트 영속 복원 — boss_session에서 lazy 로드 (DL-097, 재시작 후 #5 게이트 유지)
        this.bossRoomManager.attachClearSource(
                uuid -> this.bossEngineRuntime.bossSessionRepository().clearedBossIds(uuid.toString()));
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

        this.reputationManager = new ReputationManager(playerDataManager);
        this.hotbarService = new HotbarService(this);
        this.scoreboardService = new ScoreboardService(growthStateStore, playerDataManager, islandTerritoryStateStore);
        this.scoreboardService.startLocationWatcher(this); // 위치(필드/보스룸/영지) 변경 시 스코어보드 갱신

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

        this.classInitService = new ClassInitService(
                this, playerDataManager, growthStateStore, playerPersistenceService);

        this.cooldownManager = new CooldownManager();
        this.combatStateService = new CombatStateService();
        this.playerLevelingService = new PlayerLevelingService();
        this.auctionStore = new AuctionStore(
                foundationContext.connectionProvider(),
                foundationContext.transactionHelper(),
                getLogger());
        // registerCommands() 전에 초기화해야 NPE 방지
        this.auctionGuiListener = new AuctionGuiListener(
                this, growthStateStore, islandTerritoryStateStore,
                auctionStore, masterRegistryContext.itemMasters(), combatStateService, scoreboardService);
        this.growthGuiListener = new GrowthGuiListener(
                growthStateStore, islandTerritoryStateStore, growthEngineRuntime,
                playerDataManager, scoreboardService,
                masterRegistryContext.itemMasters(), combatStateService, this);
        this.partyManager = new PartyManager();
        FieldBossRespawnScheduler fieldBossScheduler = getServer().getPluginManager().isPluginEnabled("MythicMobs")
                ? new FieldBossRespawnScheduler(this) : null;
        ExploreHubGui.FieldStateProvider fieldStateProvider = fieldBossScheduler != null
                ? fieldBossScheduler : buildFieldStateProvider();
        FieldTeleportService fieldTeleportService = new FieldTeleportService(this);
        this.fieldHubListener = new FieldHubListener(fieldStateProvider, fieldTeleportService);
        this.bossHubListener  = new BossHubListener(partyManager, bossRoomManager,
                bossEngineRuntime.bossSessionRepository(), islandTerritoryStateStore);
        ShopGui.reloadItems(this);
        this.shopGuiListener = new ShopGuiListener(growthStateStore, islandStorageStore, combatStateService, scoreboardService);
        this.territorySettingsGuiListener = new TerritorySettingsGuiListener(islandTerritoryStateStore, combatStateService, this);
        // 영지 설정·멤버·권한 DB 영속화 hook 주입 (foundationContext 초기화 완료 후)
        com.poro.empire.growth.island.db.IslandSettingsRepository islandSettingsRepo =
                new com.poro.empire.growth.island.db.IslandSettingsRepository(
                        foundationContext.connectionProvider(),
                        foundationContext.logger().domain("island.settings"));
        islandTerritoryStateStore.attachRepository(islandSettingsRepo);

        this.pvpRatingService   = new PvpRatingService();
        this.pvpArenaManager    = PvpArenaManager.fromConfig(this);
        this.pvpMatchService    = new PvpMatchService(this, pvpArenaManager, pvpRatingService);

        // DB 영속화 hook 주입
        PvpRatingRepository pvpRatingRepo = new PvpRatingRepository(
                foundationContext.connectionProvider(),
                foundationContext.logger().domain("pvp.rating"));
        pvpRatingService.attachRepository(pvpRatingRepo);
        this.pvpMatchLogRepo = new PvpMatchLogRepository(
                foundationContext.connectionProvider(),
                foundationContext.logger().domain("pvp.matchlog"));
        pvpMatchService.attachMatchLog(pvpMatchLogRepo);
        // 접속 세션 로그 (리텐션·DAU·플레이타임, INBOX-004 #1)
        this.playerSessionRepo = new com.poro.empire.persistence.PlayerSessionRepository(
                foundationContext.connectionProvider(),
                foundationContext.logger().domain("db.session"));
        // 통화 흐름 로그 (골드 인플레이션/싱크, INBOX-004 #2 / DL-080) — 모든 성장 상태에 흐름 리스너 부착
        growthStateStore.attachFlowListener(new com.poro.empire.persistence.EconomyFlowRepository(
                foundationContext.connectionProvider(),
                foundationContext.logger().domain("db.economy")));
        // 성장 시계열 스냅샷 (성장 곡선, INBOX-004 #7 / DL-083)
        this.growthSnapshotRepo = new com.poro.empire.persistence.GrowthSnapshotRepository(
                foundationContext.connectionProvider(),
                foundationContext.logger().domain("db.growth-snapshot"));
        pvpMatchService.attachGrowthState(growthStateStore);

        PvpFriendlyService pvpFriendlyService = new PvpFriendlyService(this, pvpMatchService);
        this.pvpHubListener     = new PvpHubListener(pvpRatingService, pvpMatchService, pvpFriendlyService);
        this.resourceTracker = new ResourceTracker();
        SkillContext skillContext = new SkillContext(
                playerDataManager, this.cooldownManager, this.resourceTracker,
                growthStateStore,
                masterRegistryContext.itemMasters(),
                growthEngineRuntime.potentialService(),
                this.bossDamageTracker);

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
        skillService.registerSkill(new AxeSmashSkill());
        skillService.registerSkill(new AxeCrushChargeSkill());
        skillService.registerSkill(new AxeUnyieldingSkill(this));
        skillService.registerSkill(new AxeColossalDropSkill());
        skillService.registerSkill(new ScytheDeathSlashSkill());
        skillService.registerSkill(new ScytheShadowSpinSkill(this));
        skillService.registerSkill(new ScytheGrimStrikeSkill());
        skillService.registerSkill(new ScytheExecutionSkill());
        skillService.registerSkill(new StaffArcaneOrbSkill(this));
        skillService.registerSkill(new StaffElementalBurstSkill(this));
        skillService.registerSkill(new StaffArcaneRushSkill());
        skillService.registerSkill(new StaffStarburstSkill(this));
        growthGuiListener.setSkillService(skillService);

        registerCommands();
        registerListeners(fieldStateProvider, fieldBossScheduler);

        // 보스룸 생성 커맨드 (관리자 전용, 서버 오픈 전 1회)
        BossRoomGenerationService genService = new BossRoomGenerationService(this);
        var genCmd = getCommand("empire-genrooms");
        if (genCmd != null) genCmd.setExecutor(new BossRoomGenCommand(genService));

        PvpArenaGenerationService arenaGenService = new PvpArenaGenerationService(this);
        var arenaGenCmd = getCommand("empire-genarenas");
        if (arenaGenCmd != null) arenaGenCmd.setExecutor(new PvpArenaGenCommand(arenaGenService));
        ExploreHubRefresher.start(this, fieldStateProvider); // uses the scheduler as provider

        // 20분(24000틱)마다 영지 자동재배기 생산
        new MachineProductionScheduler(this, islandTerritoryStateStore).start();

        // 1분(1200틱)마다 경매소 만료 처리
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            int expired = auctionStore.expireOld();
            if (expired > 0) getLogger().info("[Auction] 만료 처리 " + expired + "건");
        }, 1200L, 1200L);

        // 5분(6000틱)마다 온라인 플레이어 자동 저장
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            var onlinePlayerIds = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getUniqueId)
                    .toList();
            Bukkit.getScheduler().runTaskAsynchronously(this,
                    () -> playerPersistenceService.saveAll(onlinePlayerIds));
        }, 6000L, 6000L);

        // 30분(36000틱)마다 성장 시계열 스냅샷 (메인: 상태 읽기 → async: DB 일별 upsert, DL-083)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            long now = System.currentTimeMillis();
            var rows = Bukkit.getOnlinePlayers().stream()
                    .map(p -> buildGrowthSnapshotRow(p.getUniqueId()))
                    .filter(java.util.Objects::nonNull)
                    .toList();
            if (rows.isEmpty()) return;
            Bukkit.getScheduler().runTaskAsynchronously(this,
                    () -> rows.forEach(row -> growthSnapshotRepo.upsert(row, now)));
        }, 36000L, 36000L);

        // 보스 전투 타임아웃 (필드·시즌1~6: 15분 / 최종3종: 10분) — 경과 런 강제 종료 + 보스 디스폰 + 알림 (DL-093)
        // endRun(false) → onRunEnded → releaseByRunId로 슬롯 회수. mob UUID는 endRun(finalizeShares) 전에 캡처.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            var runService = bossEngineRuntime.runService();
            for (var run : runService.activeRunsSnapshot()) {
                if (!runService.isTimedOut(run)) continue;
                java.util.UUID mob = bossDamageTracker.mobForRun(run.runId());
                java.util.List<String> participants = java.util.List.copyOf(run.participants());
                runService.endRun(run.runId(), false, "timeout");
                if (mob != null) {
                    org.bukkit.entity.Entity e = Bukkit.getEntity(mob);
                    if (e != null) e.remove();
                }
                for (String uid : participants) {
                    try {
                        org.bukkit.entity.Player p = Bukkit.getPlayer(java.util.UUID.fromString(uid));
                        if (p != null) p.sendMessage("§c[보스] 전투 시간 초과(타임아웃) — 보상 없이 종료되었습니다. 보스룸에서 퇴장해 주세요.");
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                getLogger().info("Boss run timed out. run_id=" + run.runId() + ", boss_id=" + run.bossId());
            }
        }, 200L, 200L);
    }

    @Override
    public void onDisable() {
        // 서버 종료 시 온라인 전원 저장. 부팅 초기 실패로 disable된 경우 null일 수 있어 가드 (DL-098).
        if (playerPersistenceService != null) {
            playerPersistenceService.saveAll(
                    Bukkit.getOnlinePlayers().stream()
                            .map(p -> p.getUniqueId())
                            .toList()
            );
        }
        if (operationsQueryRuntime != null && operationsQueryRuntime.httpServer() != null) {
            operationsQueryRuntime.httpServer().stop();
        }
        // 정상 종료 시 남은 열린 세션 마감 (크래시 dangling 방지)
        if (playerSessionRepo != null) {
            int closed = playerSessionRepo.closeOpenSessions(System.currentTimeMillis());
            if (closed > 0) getLogger().info("Closed " + closed + " open player session(s) on shutdown.");
        }
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

    /**
     * 보스 입장 참여자 장비 스펙 해석 (DL-081) — DbBossRunRecordHook에 주입.
     * 5슬롯 강화도로 무기 강화·평균 강화·IL(강화 1당 5) 계산. 상태 없으면 ZERO.
     */
    private com.poro.empire.boss.db.BossParticipantSpec resolveBossParticipantSpec(String uuid) {
        com.poro.empire.growth.engine.PlayerGrowthState st;
        try {
            st = growthStateStore.get(java.util.UUID.fromString(uuid)).orElse(null);
        } catch (IllegalArgumentException e) {
            return com.poro.empire.boss.db.BossParticipantSpec.ZERO;
        }
        if (st == null) return com.poro.empire.boss.db.BossParticipantSpec.ZERO;
        com.poro.empire.growth.engine.EquipmentSlot[] slots = {
                com.poro.empire.growth.engine.EquipmentSlot.WEAPON,
                com.poro.empire.growth.engine.EquipmentSlot.HELMET,
                com.poro.empire.growth.engine.EquipmentSlot.CHESTPLATE,
                com.poro.empire.growth.engine.EquipmentSlot.LEGGINGS,
                com.poro.empire.growth.engine.EquipmentSlot.BOOTS
        };
        int total = 0, weapon = 0;
        for (com.poro.empire.growth.engine.EquipmentSlot slot : slots) {
            int enh = st.equippedItem(slot)
                    .map(com.poro.empire.growth.engine.PlayerEquipmentItem::enhanceLevel).orElse(0);
            total += enh;
            if (slot == com.poro.empire.growth.engine.EquipmentSlot.WEAPON) weapon = enh;
        }
        double avgEnhance = total / 5.0;
        double il = avgEnhance * 5.0;
        return new com.poro.empire.boss.db.BossParticipantSpec(weapon, avgEnhance, il);
    }

    /**
     * 성장 시계열 스냅샷 1행 구성 (DL-083) — 메인 스레드에서 상태 읽기. 상태 없으면 null.
     * level·평균 IL(강화 1당 5)·총 강화·골드.
     */
    private com.poro.empire.persistence.GrowthSnapshotRepository.SnapshotRow buildGrowthSnapshotRow(java.util.UUID uuid) {
        com.poro.empire.growth.engine.PlayerGrowthState st = growthStateStore.get(uuid).orElse(null);
        if (st == null) return null;
        com.poro.empire.growth.engine.EquipmentSlot[] slots = {
                com.poro.empire.growth.engine.EquipmentSlot.WEAPON,
                com.poro.empire.growth.engine.EquipmentSlot.HELMET,
                com.poro.empire.growth.engine.EquipmentSlot.CHESTPLATE,
                com.poro.empire.growth.engine.EquipmentSlot.LEGGINGS,
                com.poro.empire.growth.engine.EquipmentSlot.BOOTS
        };
        int total = 0;
        for (com.poro.empire.growth.engine.EquipmentSlot slot : slots) {
            total += st.equippedItem(slot)
                    .map(com.poro.empire.growth.engine.PlayerEquipmentItem::enhanceLevel).orElse(0);
        }
        double il = (total / 5.0) * 5.0;
        return new com.poro.empire.persistence.GrowthSnapshotRepository.SnapshotRow(
                uuid.toString(), st.playerLevel(), il, total, st.currency("gold"));
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
                new PlayerJoinListener(this, playerDataManager, hotbarService, tutorialService, scoreboardService, playerPersistenceService, growthStateStore, islandTerritoryStateStore, islandStorageStore, auctionStore, classInitService, partyManager, bossRoomManager, operationsQueryRuntime.dataStore(), growthEngineRuntime.snapshotBuilder(), resourceTracker, playerSessionRepo), this);
        getServer().getPluginManager().registerEvents(
                new WeaponSelectionGuiListener(playerDataManager, scoreboardService, classInitService), this);
        getServer().getPluginManager().registerEvents(
                new CombatStateListener(combatStateService), this);
        getServer().getPluginManager().registerEvents(
                new HotbarInteractListener(hotbarService, fieldStateProvider, combatStateService), this);
        FieldTeleportService fieldTeleportService = new FieldTeleportService(this);
        // MythicMobs 스폰 어댑터 — reflection으로 완전 격리, import 없음.
        // 스폰된 보스 mob의 UUID 반환(실패 시 null) — 데미지 기여 추적(DL-084)에 사용.
        java.util.function.BiFunction<String, org.bukkit.Location, java.util.UUID> mythicSpawner = null;
        if (getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            mythicSpawner = (mobId, loc) -> {
                try {
                    Object inst   = Class.forName("io.lumine.mythic.bukkit.MythicBukkit")
                            .getMethod("inst").invoke(null);
                    Object helper = inst.getClass().getMethod("getAPIHelper").invoke(inst);
                    Object result = helper.getClass()
                            .getMethod("spawnMythicMob", String.class, org.bukkit.Location.class)
                            .invoke(helper, mobId, loc);
                    // spawnMythicMob은 org.bukkit.entity.Entity를 반환 — UUID 추출
                    if (result instanceof org.bukkit.entity.Entity ent) return ent.getUniqueId();
                    return null;
                } catch (Exception e) {
                    getLogger().warning("[BossRoom] MythicMob 스폰 실패: " + mobId + " — " + e.getMessage());
                    return null;
                }
            };
        }
        // 동적 필드 스폰 (INBOX-006) — 필드 내 플레이어 주변 웨이브 스폰. 일반/정예 토글은 2차에서 연결(현재 전원 일반).
        new com.poro.empire.field.FieldSpawnService(this, mythicSpawner, uuid -> false).start();

        // 허브 월드(별도 평지) 보장 + 복귀 유저 접속 시 허브 이동 (INBOX-006 온보딩 코어)
        com.poro.empire.hub.HubWorldService hubWorldService = new com.poro.empire.hub.HubWorldService(this);
        hubWorldService.ensureHubWorld();
        getServer().getPluginManager().registerEvents(
                new com.poro.empire.listener.HubSpawnListener(this, hubWorldService), this);

        BossRoomListener bossRoomListenerInstance =
                new BossRoomListener(bossRoomManager, masterRegistryContext.bossMasters(), partyManager, bossEngineRuntime, mythicSpawner, adminTogglesService, bossDamageTracker);
        getServer().getPluginManager().registerEvents(shopGuiListener, this);
        getServer().getPluginManager().registerEvents(pvpHubListener, this);
        getServer().getPluginManager().registerEvents(new DeathKeepInventoryListener(), this); // 1차 시즌 사망 시 템·경험치 유지
        getServer().getPluginManager().registerEvents(new IslandProtectionListener(islandTerritoryStateStore), this); // 영지 농작물 보호(trample)
        getServer().getPluginManager().registerEvents(
                new MainHubListener(growthGuiListener, islandStorageStore, islandTerritoryStateStore,
                        auctionGuiListener, fieldHubListener, bossHubListener, shopGuiListener,
                        pvpHubListener, combatStateService), this);
        getServer().getPluginManager().registerEvents(bossRoomListenerInstance, this);
        getServer().getPluginManager().registerEvents(growthGuiListener, this);
        getServer().getPluginManager().registerEvents(
                new StorageGuiListener(islandStorageStore), this);
        getServer().getPluginManager().registerEvents(
                new TerritoryStatusGuiListener(islandTerritoryStateStore, islandStorageStore, growthStateStore, playerDataManager, scoreboardService), this);
        getServer().getPluginManager().registerEvents(
                this.territorySettingsGuiListener, this);
        getServer().getPluginManager().registerEvents(
                new WorkshopGuiListener(islandTerritoryStateStore, islandStorageStore, this), this);
        getServer().getPluginManager().registerEvents(
                new HeirloomGuiListener(islandTerritoryStateStore, growthStateStore, playerDataManager), this);
        getServer().getPluginManager().registerEvents(
                new FarmGuiListener(islandTerritoryStateStore, islandStorageStore), this);
        // 바닐라 XP 바 억제 — 커스텀 레벨링만 노출 (DL-085)
        getServer().getPluginManager().registerEvents(new com.poro.empire.listener.VanillaExpSuppressListener(), this);
        if (getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            getServer().getPluginManager().registerEvents(
                    new FieldDropListener(growthStateStore, islandTerritoryStateStore, playerDataManager,
                            playerLevelingService, fieldBossScheduler, bossRewardService, new ContributionTracker(), scoreboardService, adminTogglesService), this);
            getServer().getPluginManager().registerEvents(new BossDefenseListener(bossDamageTracker), this);
            getServer().getPluginManager().registerEvents(
                    new com.poro.empire.listener.BossInstanceDamageListener(bossDamageTracker, bossEngineRuntime.runService()), this);
            getLogger().info("MythicMobs detected — FieldDropListener + BossDefenseListener + BossInstanceDamageListener registered.");
        } else {
            getLogger().warning("MythicMobs not found — field mob drops + boss DEF scaling disabled.");
        }
        getServer().getPluginManager().registerEvents(new AfkMonitorListener(this), this);
        getServer().getPluginManager().registerEvents(new HungerLockListener(), this);
        getServer().getPluginManager().registerEvents(
                new ConsumableUseListener(bossRoomManager, this), this);
        HealthHudListener healthHudListener = new HealthHudListener(this, cooldownManager, resourceTracker, growthStateStore, playerDataManager);
        getServer().getPluginManager().registerEvents(healthHudListener, this);
        growthGuiListener.setHealthHudListener(healthHudListener);
        getServer().getPluginManager().registerEvents(new SwordParryListener(), this);
        getServer().getPluginManager().registerEvents(new EmpireItemGuardListener(), this);
        getServer().getPluginManager().registerEvents(new StaffProjectileListener(resourceTracker), this);
        boolean worldGuardEnabled = getServer().getPluginManager().isPluginEnabled("WorldGuard");
        SafeZoneService safeZoneService = worldGuardEnabled
                ? new WorldGuardSafeZoneService()
                : new NoopSafeZoneService();
        getServer().getPluginManager().registerEvents(new SkillInputListener(skillService, safeZoneService), this);
        getServer().getPluginManager().registerEvents(auctionGuiListener, this);
        getServer().getPluginManager().registerEvents(fieldHubListener, this);
        getServer().getPluginManager().registerEvents(bossHubListener, this);
        // PvP — 친선 영지 검증 hook (WorldGuard 없으면 검증 우회) + 데미지 게이트
        pvpHubListener.friendlyService().attachSafeZone(safeZoneService, worldGuardEnabled);
        getServer().getPluginManager().registerEvents(new PvpDamageListener(pvpMatchService, pvpArenaManager), this);
        getServer().getPluginManager().registerEvents(new PvpTeleportListener(pvpMatchService, pvpArenaManager), this);
    }

    /** BossEngineRuntime 연동 전 stub — 모든 필드보스를 리스폰 대기 상태로 반환. */
    private ExploreHubGui.FieldStateProvider buildFieldStateProvider() {
        return new ExploreHubGui.FieldStateProvider() {
            @Override
            public ExploreHubGui.BossStatus status(String fieldId) {
                return ExploreHubGui.BossStatus.RESPAWNING;
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
        EmpireCommand empireCommand = new EmpireCommand(playerDataManager, skillService, reputationManager, this, growthStateStore);

        if (getCommand("empire") == null) {
            getLogger().severe("Failed to register /empire command. Check plugin.yml.");
            return;
        }
        getCommand("empire").setExecutor(empireCommand);
        getCommand("empire").setTabCompleter(empireCommand);

        // 한글 단축 커맨드
        PlayerCommandRouter router = new PlayerCommandRouter(islandStorageStore, islandTerritoryStateStore, auctionGuiListener, growthGuiListener, fieldHubListener, bossHubListener, shopGuiListener, combatStateService, territorySettingsGuiListener, pvpHubListener);
        String[] koreanCommands = {
            "메뉴", "장비", "강화", "잠재", "각인", "캐릭터", "전승",
            "영지", "영지이동", "영지상태", "영지초대", "창고", "공방", "작물", "상점", "경매장", "영지설정",
            "보스", "파티", "파티목록", "보스정보", "클리어", "필드",
            "대전", "대전랭킹", "친선"
        };
        for (String cmd : koreanCommands) {
            var registered = getCommand(cmd);
            if (registered != null) {
                registered.setExecutor(router);
            } else {
                getLogger().warning("커맨드 /" + cmd + " plugin.yml 미등록 — 건너뜀.");
            }
        }

        // 운용자 전용 한글 커맨드
        ClassAdminCommand classAdminCommand = new ClassAdminCommand(classInitService, playerDataManager);
        var setClassCmd = getCommand("empire-setclass");
        if (setClassCmd != null) {
            setClassCmd.setExecutor(classAdminCommand);
            setClassCmd.setTabCompleter(classAdminCommand);
        } else {
            getLogger().warning("커맨드 /empire-setclass plugin.yml 미등록 — 건너뜀.");
        }

        // 관리자 GUI 허브 (Phase 1 + Phase 2 toggles)
        this.adminTogglesService = new com.poro.empire.admin.AdminTogglesService();
        // Step 2b — 운영 토글을 게임 로직에 연결
        pvpMatchService.attachToggles(adminTogglesService);
        growthEngineRuntime.enhancementService().setEnhanceBoostSupplier(
                () -> adminTogglesService.isOn(com.poro.empire.admin.AdminTogglesService.Toggle.ENHANCE_BOOST));
        com.poro.empire.listener.AdminGuiListener adminGuiListener =
                new com.poro.empire.listener.AdminGuiListener(
                        playerDataManager, growthStateStore, islandTerritoryStateStore,
                        pvpRatingService, pvpMatchService, pvpArenaManager, bossRoomManager,
                        adminTogglesService,
                        growthEngineRuntime.enhancementLogHook(), auctionStore, pvpMatchLogRepo,
                        bossEngineRuntime.runService(),
                        foundationContext.config().seasonStartEpoch());
        getServer().getPluginManager().registerEvents(adminGuiListener, this);
        var adminCmd = getCommand("empire-admin");
        if (adminCmd != null) {
            adminCmd.setExecutor(new com.poro.empire.command.AdminHubCommand(adminGuiListener));
        } else {
            getLogger().warning("커맨드 /empire-admin plugin.yml 미등록 — 건너뜀.");
        }

        // 운영자 단건 변경 명령 8종 — 동일 핸들러 공유
        com.poro.empire.command.AdminPlayerCommand adminPlayerCmd =
                new com.poro.empire.command.AdminPlayerCommand(
                        playerDataManager, growthStateStore, islandTerritoryStateStore,
                        islandStorageStore, pvpRatingService, pvpMatchService);
        for (String c : new String[]{
                "empire-give", "empire-currency", "empire-rank", "empire-enhance",
                "empire-level", "empire-pvp-score", "empire-cleanse", "empire-island-reset"}) {
            var registered = getCommand(c);
            if (registered != null) registered.setExecutor(adminPlayerCmd);
            else getLogger().warning("커맨드 /" + c + " plugin.yml 미등록 — 건너뜀.");
        }

        // 운영 토글 명령 (Phase 2 Step 2)
        var toggleCmd = getCommand("empire-toggle");
        if (toggleCmd != null) {
            toggleCmd.setExecutor(new com.poro.empire.command.AdminTogglesCommand(adminTogglesService));
        } else {
            getLogger().warning("커맨드 /empire-toggle plugin.yml 미등록 — 건너뜀.");
        }

        // 로그/감시 명령 (Phase 2 Step 3)
        var logCmd = getCommand("empire-log");
        if (logCmd != null) {
            logCmd.setExecutor(new com.poro.empire.command.AdminLogCommand(
                    growthEngineRuntime.enhancementLogHook(), auctionStore, pvpMatchLogRepo));
        } else {
            getLogger().warning("커맨드 /empire-log plugin.yml 미등록 — 건너뜀.");
        }

        // 보스 디버그 명령 (Phase 2 Step 4) — 두 명령 동일 핸들러 공유 (라벨 분기)
        com.poro.empire.command.AdminBossCommand adminBossCmd =
                new com.poro.empire.command.AdminBossCommand(bossEngineRuntime.runService());
        for (String c : new String[]{"empire-boss-list", "empire-boss-end"}) {
            var registered = getCommand(c);
            if (registered != null) registered.setExecutor(adminBossCmd);
            else getLogger().warning("커맨드 /" + c + " plugin.yml 미등록 — 건너뜀.");
        }
    }
}
