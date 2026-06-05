package kr.poro.poromoncore;

import kr.poro.poromoncore.command.PoroMonCommand;
import kr.poro.poromoncore.data.PlayerProgress;
import kr.poro.poromoncore.data.PoroMonState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PoroMonCore 진입점 (ModInitializer). 0.1 스캐폴드:
 *  - PlayerProgress 영속화(PersistentState) + 첫 접속 엔트리 생성
 *  - /poromon 명령(진행 조회 + 관리자 배틀타워 진행 setter = 영속화 테스트)
 * 추후: 배틀타워 pvn 오케스트레이션, 티켓/룸/허브/메뉴 등(module_structure.md).
 */
public class PoroMonCore implements ModInitializer {
    public static final String MOD_ID = "poromoncore";
    public static final Logger LOGGER = LoggerFactory.getLogger("PoroMonCore");

    @Override
    public void onInitialize() {
        LOGGER.info("[PoroMonCore] 0.1 스캐폴드 초기화");

        // 명령 등록
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                PoroMonCommand.register(dispatcher));

        // 첫 접속 시 진행 엔트리 생성(firstJoinEpoch 기록)
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            initPlayer(server, player);
        });
    }

    private static void initPlayer(MinecraftServer server, ServerPlayerEntity player) {
        PoroMonState state = PoroMonState.get(server);
        PlayerProgress progress = state.getOrCreate(player.getUuid());
        if (progress.firstJoinEpoch == 0L) {
            progress.firstJoinEpoch = System.currentTimeMillis() / 1000L;
            state.markDirty();
            LOGGER.info("[PoroMonCore] 신규 플레이어 진행 생성: {}", player.getGameProfile().getName());
        }
    }
}
