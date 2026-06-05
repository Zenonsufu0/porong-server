package kr.poro.poromoncore.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kr.poro.poromoncore.PoroMonCore;
import kr.poro.poromoncore.data.PlayerProgress;
import kr.poro.poromoncore.data.PoroMonState;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * /poromon 명령 트리 (commands.md, 0.1 부분 — 진행 조회 + 배틀타워 진행 setter).
 * 관리자: op 레벨 2. 플레이어: 누구나.
 */
public final class PoroMonCommand {
    private PoroMonCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("poromon")
                .executes(PoroMonCommand::root)
                .then(CommandManager.literal("progress").executes(PoroMonCommand::ownProgress))
                .then(CommandManager.literal("admin")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("reload").executes(PoroMonCommand::reload))
                        .then(CommandManager.literal("progress")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(PoroMonCommand::targetProgress)))
                        .then(CommandManager.literal("tower")
                                .then(CommandManager.literal("set")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("floor", IntegerArgumentType.integer(0, 50))
                                                        .executes(PoroMonCommand::towerSet)))))));
    }

    private static int root(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> Text.literal("§e[PoroMon]§r 0.1 — /poromon progress | /poromon admin …"), false);
        return 1;
    }

    private static int ownProgress(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        PoroMonState state = PoroMonState.get(player.getServer());
        PlayerProgress p = state.getOrCreate(player.getUuid());
        sendProgress(ctx.getSource(), player.getGameProfile().getName(), p);
        return 1;
    }

    private static int targetProgress(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        PoroMonState state = PoroMonState.get(ctx.getSource().getServer());
        PlayerProgress p = state.getOrCreate(target.getUuid());
        sendProgress(ctx.getSource(), target.getGameProfile().getName(), p);
        return 1;
    }

    private static int towerSet(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        int floor = IntegerArgumentType.getInteger(ctx, "floor");
        MinecraftServer server = ctx.getSource().getServer();
        PoroMonState state = PoroMonState.get(server);
        PlayerProgress p = state.getOrCreate(target.getUuid());
        p.battleTowerHighestClearedFloor = floor;
        state.markDirty();
        PoroMonCore.LOGGER.info("[PoroMonCore] battleTower.highestClearedFloor={} ({})", floor, target.getGameProfile().getName());
        ctx.getSource().sendFeedback(() -> Text.literal("§a[PoroMon]§r " + target.getGameProfile().getName()
                + " 배틀타워 최고층 = " + floor + " (저장됨)"), true);
        return 1;
    }

    private static int reload(CommandContext<ServerCommandSource> ctx) {
        // 0.1 스캐폴드: config 시스템은 추후. 자리표시 피드백.
        ctx.getSource().sendFeedback(() -> Text.literal("§e[PoroMon]§r 리로드 (스캐폴드 — config 미구현)"), true);
        return 1;
    }

    private static void sendProgress(ServerCommandSource src, String name, PlayerProgress p) {
        src.sendFeedback(() -> Text.literal(
                "§e[PoroMon] " + name + "§r — 골드: " + p.balance
                        + " / 배틀타워 최고층: " + p.battleTowerHighestClearedFloor
                        + " / 리그패스: " + (p.leaguePassGiven ? "O" : "X")
                        + " / 첫접속: " + p.firstJoinEpoch), false);
    }
}
