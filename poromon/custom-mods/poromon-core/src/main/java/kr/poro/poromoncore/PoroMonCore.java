package kr.poro.poromoncore;

import kr.poro.poromoncore.battle.BattleTowerService;
import kr.poro.poromoncore.command.PoroMonCommand;
import kr.poro.poromoncore.config.ConfigManager;
import kr.poro.poromoncore.config.CoreConfig;
import kr.poro.poromoncore.data.PlayerProgress;
import kr.poro.poromoncore.data.PoroMonState;
import kr.poro.poromoncore.home.HomeManager;
import kr.poro.poromoncore.wild.WildManager;
import kr.poro.poromoncore.item.MenuItemManager;
import kr.poro.poromoncore.menu.MenuGuiManager;
import kr.poro.poromoncore.util.ChatInputManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.TypedActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PoroMonCore 진입점 (ModInitializer). 0.1:
 *  - config(core.json) 로드 + PlayerProgress 영속화
 *  - 리그 패스 지급/복원/보호 + 우클릭 메뉴 GUI(허브 텔레포트·진행 조회)
 *  - 배틀타워 pvn 오케스트레이션(별도 §4b)
 */
public class PoroMonCore implements ModInitializer {
    public static final String MOD_ID = "poromoncore";
    public static final Logger LOGGER = LoggerFactory.getLogger("PoroMonCore");

    @Override
    public void onInitialize() {
        LOGGER.info("[PoroMonCore] 0.1 초기화");

        // 설정 로드(없으면 기본값 생성)
        ConfigManager.load();

        // 관장 배틀 승리 이벤트 구독(Cobblemon)
        kr.poro.poromoncore.gym.GymBattleService.registerEvents();

        // 명령 등록
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                PoroMonCommand.register(dispatcher));

        // 접속: 진행 엔트리 생성 + 리그 패스 지급/복원
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                onJoin(server, handler.player));

        // 리스폰: 리그 패스 복원
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            CoreConfig.MenuItem mi = ConfigManager.core().menuItem;
            if (mi.enabled && mi.restoreOnRespawn) {
                MenuItemManager.ensure(newPlayer);
            }
        });

        // 리그 패스 우클릭 → 메뉴 오픈
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity sp
                    && MenuItemManager.isPass(player.getStackInHand(hand))) {
                MenuGuiManager.open(sp);
                return TypedActionResult.success(player.getStackInHand(hand));
            }
            return TypedActionResult.pass(player.getStackInHand(hand));
        });

        // 홈 이름 변경 등 한 줄 채팅 입력 가로채기(대기 중이면 브로드캐스트 취소)
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) ->
                !ChatInputManager.handle(sender, message.getSignedContent()));

        // 매 틱: 리그 패스 슬롯 고정 + 홈 텔레포트 웜업 점검 + 20틱마다 배틀타워 점검
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                MenuItemManager.enforce(p);
            }
            HomeManager.tickWarmups(server);
            WildManager.tickWarmups(server);
            if (server.getTicks() % 20 == 0) {
                BattleTowerService.tick(server);
                kr.poro.poromoncore.gym.GymBattleService.tick(server);
                kr.poro.poromoncore.encounter.EncounterService.tick(server);
                kr.poro.poromoncore.tpa.TpaManager.cleanup(server.getTicks());
            }
        });
    }

    private static void onJoin(MinecraftServer server, ServerPlayerEntity player) {
        PoroMonState state = PoroMonState.get(server);
        PlayerProgress progress = state.getOrCreate(player.getUuid());

        boolean changed = false;
        if (progress.firstJoinEpoch == 0L) {
            progress.firstJoinEpoch = System.currentTimeMillis() / 1000L;
            changed = true;
            LOGGER.info("[PoroMonCore] 신규 플레이어 진행 생성: {}", player.getGameProfile().getName());
        }

        CoreConfig.MenuItem mi = ConfigManager.core().menuItem;
        if (mi.enabled) {
            boolean wantFirstGive = !progress.leaguePassGiven && mi.giveOnFirstJoin;
            boolean wantRestore = progress.leaguePassGiven && mi.restoreOnJoin;
            if (wantFirstGive || wantRestore) {
                boolean has = MenuItemManager.ensure(player);
                if (has && !progress.leaguePassGiven) {
                    progress.leaguePassGiven = true;
                    changed = true;
                }
            }
        }
        if (changed) state.markDirty();
    }
}
