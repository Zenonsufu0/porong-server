package kr.zenon.gun.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import kr.zenon.gun.ZenonGunCore;
import kr.zenon.gun.inv.BackpackProtoMenu;
import kr.zenon.gun.inv.WeightUtil;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

/**
 * 프로토타입 검증용 커맨드.
 *   /zenongun invtest        — 인벤 GUI 프로토타입 화면 열기
 *   /zenongun setweight &lt;n&gt; — 손에 든 아이템 무게 태그 설정(TaCZ 총에 얹어 점유칸 확인)
 */
@Mod.EventBusSubscriber(modid = ZenonGunCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ZenonGunCommands {

    private ZenonGunCommands() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("zenongun")
                .then(Commands.literal("invtest").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    NetworkHooks.openScreen(player, new SimpleMenuProvider(
                            (id, inv, p) -> new BackpackProtoMenu(id, inv),
                            Component.literal("ZenonGun 인벤 프로토타입")));
                    return 1;
                }))
                .then(Commands.literal("setweight")
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, 9)).executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int n = IntegerArgumentType.getInteger(ctx, "n");
                            ItemStack held = player.getMainHandItem();
                            if (held.isEmpty()) {
                                ctx.getSource().sendFailure(Component.literal("손에 든 아이템이 없음"));
                                return 0;
                            }
                            WeightUtil.setWeight(held, n);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "무게=" + n + " 설정: " + held.getHoverName().getString()), false);
                            return 1;
                        }))));
    }
}
