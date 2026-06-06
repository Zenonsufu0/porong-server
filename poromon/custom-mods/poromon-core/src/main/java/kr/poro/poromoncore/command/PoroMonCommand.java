package kr.poro.poromoncore.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kr.poro.poromoncore.PoroMonCore;
import kr.poro.poromoncore.battle.BattleTowerService;
import kr.poro.poromoncore.config.ConfigManager;
import kr.poro.poromoncore.data.PlayerProgress;
import kr.poro.poromoncore.data.PoroMonState;
import kr.poro.poromoncore.economy.EconomyBridge;
import kr.poro.poromoncore.gym.GymInfo;
import kr.poro.poromoncore.hub.HubManager;
import kr.poro.poromoncore.item.MenuItemManager;
import kr.poro.poromoncore.menu.MenuGuiManager;
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
                .then(CommandManager.literal("menu").executes(PoroMonCommand::openMenu))
                .then(CommandManager.literal("hub").executes(PoroMonCommand::hub))
                .then(CommandManager.literal("home").executes(PoroMonCommand::home))
                .then(CommandManager.literal("wild").executes(PoroMonCommand::wild))
                .then(CommandManager.literal("tpa")
                        .then(CommandManager.literal("accept").executes(PoroMonCommand::tpaAccept))
                        .then(CommandManager.literal("deny").executes(PoroMonCommand::tpaDeny)))
                .then(CommandManager.literal("admin")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("reload").executes(PoroMonCommand::reload))
                        .then(CommandManager.literal("pass")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(PoroMonCommand::adminPass)))
                        .then(CommandManager.literal("progress")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(PoroMonCommand::targetProgress)))
                        .then(CommandManager.literal("badge")
                                .then(CommandManager.literal("give")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("gym", StringArgumentType.word())
                                                        .suggests((c, b) -> {
                                                            for (GymInfo.Gym g : GymInfo.GYMS) b.suggest(g.id());
                                                            return b.buildFuture();
                                                        })
                                                        .executes(PoroMonCommand::badgeGive))))
                                .then(CommandManager.literal("clear")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(PoroMonCommand::badgeClear))))
                        .then(CommandManager.literal("encounter")
                                .then(CommandManager.argument("pool", StringArgumentType.string())
                                        .suggests((c, b) -> {
                                            ConfigManager.encounter().pools.keySet().forEach(b::suggest);
                                            return b.buildFuture();
                                        })
                                        .executes(ctx -> encounter(ctx, false))
                                        .then(CommandManager.literal("shiny")
                                                .executes(ctx -> encounter(ctx, true)))))
                        .then(CommandManager.literal("economy")
                                .then(CommandManager.literal("balance")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(PoroMonCommand::economyBalance)))
                                .then(CommandManager.literal("give")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("amount", LongArgumentType.longArg(1))
                                                        .executes(PoroMonCommand::economyGive))))
                                .then(CommandManager.literal("set")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("amount", LongArgumentType.longArg(0))
                                                        .executes(PoroMonCommand::economySet)))))
                        .then(CommandManager.literal("tower")
                                .then(CommandManager.literal("set")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("floor", IntegerArgumentType.integer(0, 50))
                                                        .executes(PoroMonCommand::towerSet))))
                                .then(CommandManager.literal("start")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("floor", IntegerArgumentType.integer(1, 50))
                                                        .executes(PoroMonCommand::towerStart)))))));
    }

    private static int root(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> Text.literal("§e[PoroMon]§r 0.1 — /poromon menu | hub | home | wild | tpa | progress | admin …"), false);
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

    private static int towerStart(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        int floor = IntegerArgumentType.getInteger(ctx, "floor");
        boolean ok = BattleTowerService.startFloor(target, floor);
        ctx.getSource().sendFeedback(() -> Text.literal(ok
                ? "§a[PoroMon]§r 배틀타워 " + floor + "층 배틀 시작: " + target.getGameProfile().getName()
                : "§c[PoroMon]§r 배틀 시작 실패(로그 확인) — " + target.getGameProfile().getName()), true);
        return ok ? 1 : 0;
    }

    private static int openMenu(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        MenuGuiManager.open(player);
        return 1;
    }

    private static int hub(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        return HubManager.teleportToHub(player) ? 1 : 0;
    }

    private static int home(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        kr.poro.poromoncore.home.HomeMenu.open(player);
        return 1;
    }

    private static int wild(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        kr.poro.poromoncore.wild.WildManager.requestTeleport(ctx.getSource().getPlayerOrThrow());
        return 1;
    }

    private static int tpaAccept(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        kr.poro.poromoncore.tpa.TpaManager.accept(ctx.getSource().getPlayerOrThrow());
        return 1;
    }

    private static int tpaDeny(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        kr.poro.poromoncore.tpa.TpaManager.deny(ctx.getSource().getPlayerOrThrow());
        return 1;
    }

    private static int adminPass(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        boolean has = MenuItemManager.ensure(target);
        if (has) {
            PoroMonState state = PoroMonState.get(target.getServer());
            PlayerProgress p = state.getOrCreate(target.getUuid());
            if (!p.leaguePassGiven) {
                p.leaguePassGiven = true;
                state.markDirty();
            }
        }
        ctx.getSource().sendFeedback(() -> Text.literal(has
                ? "§a[PoroMon]§r 리그 패스 지급/복원: " + target.getGameProfile().getName()
                : "§c[PoroMon]§r 리그 패스 지급 실패(인벤 가득?) — " + target.getGameProfile().getName()), true);
        return has ? 1 : 0;
    }

    private static int badgeGive(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        String gymId = StringArgumentType.getString(ctx, "gym");
        GymInfo.Gym gym = GymInfo.byId(gymId);
        if (gym == null) {
            ctx.getSource().sendError(Text.literal("§c알 수 없는 관장 id: " + gymId));
            return 0;
        }
        PoroMonState state = PoroMonState.get(target.getServer());
        PlayerProgress p = state.getOrCreate(target.getUuid());
        boolean added = p.badges.add(gym.id());
        if (added) state.markDirty();
        ctx.getSource().sendFeedback(() -> Text.literal("§a[PoroMon]§r " + target.getGameProfile().getName()
                + " — " + gym.badgeKo() + (added ? " 지급" : " (이미 보유)") + " (배지 " + p.badges.size() + "/8)"), true);
        return 1;
    }

    private static int badgeClear(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        PoroMonState state = PoroMonState.get(target.getServer());
        PlayerProgress p = state.getOrCreate(target.getUuid());
        p.badges.clear();
        state.markDirty();
        ctx.getSource().sendFeedback(() -> Text.literal("§a[PoroMon]§r " + target.getGameProfile().getName()
                + " 배지 전체 초기화"), true);
        return 1;
    }

    private static int encounter(CommandContext<ServerCommandSource> ctx, boolean shiny) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        String pool = StringArgumentType.getString(ctx, "pool");
        boolean ok = kr.poro.poromoncore.encounter.EncounterService.start(player, pool, shiny);
        return ok ? 1 : 0;
    }

    private static int economyBalance(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        String unit = ConfigManager.economy().currencyDisplay;
        long bal = EconomyBridge.getBalance(target);
        ctx.getSource().sendFeedback(() -> Text.literal("§e[PoroMon]§r " + target.getGameProfile().getName()
                + " 잔액: §6" + bal + " " + unit), false);
        return 1;
    }

    private static int economyGive(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        long amount = LongArgumentType.getLong(ctx, "amount");
        EconomyBridge.deposit(target, amount, "admin_give");
        ctx.getSource().sendFeedback(() -> Text.literal("§a[PoroMon]§r " + target.getGameProfile().getName()
                + " 에게 " + amount + " 지급 (잔액 " + EconomyBridge.getBalance(target) + ")"), true);
        return 1;
    }

    private static int economySet(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        long amount = LongArgumentType.getLong(ctx, "amount");
        EconomyBridge.set(target, amount, "admin_set");
        ctx.getSource().sendFeedback(() -> Text.literal("§a[PoroMon]§r " + target.getGameProfile().getName()
                + " 잔액 = " + EconomyBridge.getBalance(target)), true);
        return 1;
    }

    private static int reload(CommandContext<ServerCommandSource> ctx) {
        boolean ok = ConfigManager.reload();
        ctx.getSource().sendFeedback(() -> Text.literal(ok
                ? "§a[PoroMon]§r 설정 리로드 완료 (core.json·economy.json / 아이템 등록성 변경은 재시작 필요)"
                : "§c[PoroMon]§r 리로드 실패 — 기존 설정 유지(로그 확인)"), true);
        return ok ? 1 : 0;
    }

    private static void sendProgress(ServerCommandSource src, String name, PlayerProgress p) {
        src.sendFeedback(() -> Text.literal(
                "§e[PoroMon] " + name + "§r — 골드: " + p.balance
                        + " / 배틀타워 최고층: " + p.battleTowerHighestClearedFloor
                        + " / 리그패스: " + (p.leaguePassGiven ? "O" : "X")
                        + " / 첫접속: " + p.firstJoinEpoch), false);
    }
}
