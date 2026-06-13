package kr.zenon.moncore;

import kr.zenon.moncore.battle.BattleTowerService;
import kr.zenon.moncore.command.ZenonMonCommand;
import kr.zenon.moncore.config.ConfigManager;
import kr.zenon.moncore.config.CoreConfig;
import kr.zenon.moncore.data.PlayerProgress;
import kr.zenon.moncore.data.ZenonMonState;
import kr.zenon.moncore.home.HomeManager;
import kr.zenon.moncore.wild.WildManager;
import kr.zenon.moncore.item.MenuItemManager;
import kr.zenon.moncore.menu.MenuGuiManager;
import kr.zenon.moncore.util.ChatInputManager;
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
 * ZenonMonCore 진입점 (ModInitializer). 0.1:
 *  - config(core.json) 로드 + PlayerProgress 영속화
 *  - 리그 패스 지급/복원/보호 + 우클릭 메뉴 GUI(허브 텔레포트·진행 조회)
 *  - 배틀타워 pvn 오케스트레이션(별도 §4b)
 */
public class ZenonMonCore implements ModInitializer {
    public static final String MOD_ID = "zenonmoncore";
    public static final Logger LOGGER = LoggerFactory.getLogger("ZenonMonCore");

    @Override
    public void onInitialize() {
        LOGGER.info("[ZenonMonCore] 0.1 초기화");

        // 설정 로드(없으면 기본값 생성)
        ConfigManager.load();

        // 관장 배틀 승리 이벤트 구독(Cobblemon)
        kr.zenon.moncore.gym.GymBattleService.registerEvents();
        // 야생 포켓몬 골드 보상(처치·포획)
        kr.zenon.moncore.economy.WildRewardService.registerEvents();
        // 정규리그 승리 이벤트 구독(점수 반영)
        kr.zenon.moncore.league.LeagueManager.registerEvents();
        // 챔피언스리그 승리 이벤트 구독(토너먼트 진행)
        kr.zenon.moncore.league.ChampionsManager.registerEvents();

        // 마개조 해금석: 포켓몬 우클릭 → 그 포켓몬 영구 마개조 해제(소모). 각인은 기술머신 메뉴(결정033-a)
        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((p, world, hand, entity, hit) -> {
            if (world.isClient() || hand != net.minecraft.util.Hand.MAIN_HAND) return net.minecraft.util.ActionResult.PASS;
            if (!(p instanceof ServerPlayerEntity sp)) return net.minecraft.util.ActionResult.PASS;
            net.minecraft.item.ItemStack held = sp.getStackInHand(hand);
            kr.zenon.moncore.item.MakeoverStone.Kind kind =
                    kr.zenon.moncore.item.MakeoverStone.kindOf(held);
            if (kind == null) return net.minecraft.util.ActionResult.PASS;
            if (!(entity instanceof com.cobblemon.mod.common.entity.pokemon.PokemonEntity pe)) return net.minecraft.util.ActionResult.PASS;
            com.cobblemon.mod.common.pokemon.Pokemon target =
                    kr.zenon.moncore.shop.MakeoverService.findPartyPokemon(sp, pe.getPokemon().getUuid());
            if (target == null) {
                sp.sendMessage(net.minecraft.text.Text.literal("§c[포로공학] 본인 파티 포켓몬에만 사용할 수 있습니다."), true);
                return net.minecraft.util.ActionResult.SUCCESS;
            }
            boolean ability = kind == kr.zenon.moncore.item.MakeoverStone.Kind.ABILITY;
            boolean unlocked = ability
                    ? kr.zenon.moncore.shop.MakeoverService.unlockAbility(sp, target)
                    : kr.zenon.moncore.shop.MakeoverService.unlock(sp, target);
            if (!unlocked) {
                sp.sendMessage(net.minecraft.text.Text.literal("§e[포로공학] 이미 해제된 포켓몬입니다."), true);
                return net.minecraft.util.ActionResult.SUCCESS;
            }
            held.decrement(1);
            String tail = ability ? " §a해제 완료! 메뉴 → 포로공학에서 특성을 새길 수 있습니다."
                                  : " §a해제 완료! 메뉴 → 포로공학에서 기술을 각인할 수 있습니다.";
            sp.sendMessage(net.minecraft.text.Text.literal("§a[포로공학] ")
                    .append(target.getSpecies().getTranslatedName())
                    .append(net.minecraft.text.Text.literal(tail)), false);
            return net.minecraft.util.ActionResult.SUCCESS;
        });

        // 서버 참조 캐시(이벤트 부스트용) + 네더 월드보더 적용 + 경험치 부스트 훅
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            kr.zenon.moncore.event.EventManager.setServer(server);
            kr.zenon.moncore.dimension.NetherManager.applyBorder(server);
            kr.zenon.moncore.auth.AuthManager.setServer(server);
            kr.zenon.moncore.auth.AuthHttpServer.start();
        });
        // 디스코드 인증 HTTP API 정리
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server ->
                kr.zenon.moncore.auth.AuthHttpServer.stop());

        // 차원 리다이렉트(결정 039): 네더=허브/진입좌표 복귀 · 엔드=바깥섬 허브
        net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, dest) -> {
            kr.zenon.moncore.dimension.NetherManager.onChangeWorld(player, origin, dest);
            kr.zenon.moncore.dimension.EndManager.onChangeWorld(player, origin, dest);
        });

        // 엔드 드래곤 제거(드래곤전 비활성, 결정 039)
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, world) ->
                kr.zenon.moncore.dimension.EndManager.onEntityLoad(entity, world));

        // 네더 허브 보호: 허브 반경 내 블록 파괴 차단(포탈·블레이즈 스포너)
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
            if (player instanceof ServerPlayerEntity sp)
                return !kr.zenon.moncore.dimension.NetherManager.isProtectedBreak(world, sp, pos);
            return true;
        });
        com.cobblemon.mod.common.api.events.CobblemonEvents.EXPERIENCE_GAINED_EVENT_PRE.subscribe(
                com.cobblemon.mod.common.api.Priority.NORMAL, e -> {
                    if (kr.zenon.moncore.event.EventManager.isXpBoost()) {
                        e.setExperience((int) (e.getExperience() * kr.zenon.moncore.event.EventManager.xpMultiplier()));
                    }
                });

        // 명령 등록
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ZenonMonCommand.register(dispatcher));

        // 접속: 진행 엔트리 생성 + 리그 패스 지급/복원
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                onJoin(server, handler.player));

        // 접속 종료: 정규리그 큐/진행 정리(노카운트 + 아레나 철거·상대 복귀)
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                kr.zenon.moncore.league.LeagueManager.onDisconnect(server, handler.player.getUuid()));

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
                kr.zenon.moncore.gym.GymBattleService.tick(server);
                kr.zenon.moncore.encounter.EncounterService.tick(server);
                kr.zenon.moncore.encounter.FieldEventManager.tick(server);
                kr.zenon.moncore.tpa.TpaManager.cleanup(server.getTicks());
                kr.zenon.moncore.league.LeagueManager.tick(server);
                kr.zenon.moncore.league.ChampionsManager.tick(server);
                kr.zenon.moncore.dimension.NetherManager.trackOverworld(server);
                kr.zenon.moncore.auth.AuthManager.tickConfine(server);
            }
        });
    }

    private static void onJoin(MinecraftServer server, ServerPlayerEntity player) {
        ZenonMonState state = ZenonMonState.get(server);
        PlayerProgress progress = state.getOrCreate(player.getUuid());

        boolean changed = false;
        if (progress.firstJoinEpoch == 0L) {
            progress.firstJoinEpoch = System.currentTimeMillis() / 1000L;
            changed = true;
            LOGGER.info("[ZenonMonCore] 신규 플레이어 진행 생성: {}", player.getGameProfile().getName());
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

        // 정규리그 대전 중 끊겼던 경우 원위치 복귀(허공 로그인 방지)
        kr.zenon.moncore.league.LeagueManager.checkPendingReturn(player);

        // 디스코드 미인증 안내(결정 041): 인증 전 허브 감금
        if (!kr.zenon.moncore.auth.AuthManager.isVerified(player)) {
            player.sendMessage(net.minecraft.text.Text.literal(
                    "§6§l[디스코드 인증 필요] §r§7인증 전에는 허브만 이용할 수 있습니다. §e/인증 §7으로 코드를 발급받으세요."), false);
        }
    }
}
