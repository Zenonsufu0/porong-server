package kr.poro.poromoncore.gym;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.api.npc.NPCClass;
import com.cobblemon.mod.common.api.npc.NPCClasses;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.storage.party.NPCPartyStore;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleStartResult;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kr.poro.poromoncore.PoroMonCore;
import kr.poro.poromoncore.data.PlayerProgress;
import kr.poro.poromoncore.data.PoroMonState;
import kr.poro.poromoncore.economy.EconomyBridge;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 관장 실배틀 (gym_badge_design.md, 결정 030: 도전은 메뉴에서 트리거).
 * 배틀타워 패턴 재사용 — 관장 NPC를 코드로 빌드해 pvn 명시 주입.
 * 승리 감지는 Cobblemon BATTLE_VICTORY 이벤트 → 최초 승리 시 배지 + 골드 지급.
 */
public final class GymBattleService {
    private GymBattleService() {}

    private record Active(String gymId, UUID npcUuid) {}
    private static final Map<UUID, Active> ACTIVE = new ConcurrentHashMap<>();

    /** 관장별 파티(타입 맞춤, 레벨=레벨캡). 0.1 하드코딩 — 향후 gyms.json. */
    private static final Map<String, String[]> PARTIES = Map.of(
            "gym_bug",      new String[]{"butterfree", "beedrill", "venomoth"},
            "gym_rock",     new String[]{"graveler", "onix", "rhyhorn"},
            "gym_electric", new String[]{"raichu", "magneton", "electabuzz"},
            "gym_grass",    new String[]{"vileplume", "victreebel", "tangela"},
            "gym_water",    new String[]{"gyarados", "starmie", "lapras"},
            "gym_fire",     new String[]{"arcanine", "rapidash", "magmar"},
            "gym_psychic",  new String[]{"alakazam", "hypno", "exeggutor"},
            "gym_dragon",   new String[]{"dragonite", "kingdra", "flygon"}
    );

    /** PoroMonCore.onInitialize 에서 1회 — 승리 이벤트 구독. */
    public static void registerEvents() {
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, GymBattleService::onVictory);
    }

    /** 관장 도전 시작. 성공 시 true. */
    public static boolean startChallenge(ServerPlayerEntity player, String gymId) {
        GymInfo.Gym gym = GymInfo.byId(gymId);
        String[] specs = PARTIES.get(gymId);
        if (gym == null || specs == null) return false;

        PoroMonState state = PoroMonState.get(player.getServer());
        PlayerProgress p = state.getOrCreate(player.getUuid());

        // 순차 강제: 이전 관장 전원 클리어해야 도전 가능(이미 깬 관장은 재도전 허용)
        if (!p.badges.contains(gymId)) {
            for (GymInfo.Gym prev : GymInfo.GYMS) {
                if (prev.order() < gym.order() && !p.badges.contains(prev.id())) {
                    player.sendMessage(Text.literal("§c[관장] 이전 관장을 먼저 클리어하세요."), false);
                    return false;
                }
            }
        }
        if (ACTIVE.containsKey(player.getUuid())) {
            player.sendMessage(Text.literal("§e[관장] 이미 배틀 중입니다."), false);
            return false;
        }

        try {
            ServerWorld world = player.getServerWorld();
            NPCEntity npc = new NPCEntity(world);
            NPCClass npcClass = NPCClasses.getByIdentifier(Identifier.of("cobblemon", "standard"));
            if (npcClass != null) npc.setNpc(npcClass);
            double rad = Math.toRadians(player.getYaw());
            double nx = player.getX() - Math.sin(rad) * 3.0;
            double nz = player.getZ() + Math.cos(rad) * 3.0;
            npc.refreshPositionAndAngles(nx, player.getY(), nz, player.getYaw() + 180.0f, 0.0f);
            world.spawnEntity(npc);
            npc.setSkill(1);

            NPCPartyStore party = new NPCPartyStore(npc);
            for (String species : specs) {
                PokemonProperties props = PokemonProperties.Companion.parse(
                        species + " level=" + gym.levelCap(), " ", "=");
                Pokemon pokemon = props.create();
                party.add(pokemon);
            }
            npc.setParty(party); // NPC(관장) 파티

            // ⚠️ pvn의 마지막 PartyStore = "플레이어" 파티. 반드시 플레이어 본인 파티를 넘긴다
            //    (NPC 파티를 넘기면 플레이어가 관장 팀으로 싸우는 버그). NPC 파티는 위 setParty로 전달됨.
            BattleFormat format = BattleFormat.Companion.getGEN_9_SINGLES();
            BattleStartResult result = BattleBuilder.INSTANCE.pvn(
                    player, npc, npc.getUuid(), format, true, true,
                    Cobblemon.INSTANCE.getStorage().getParty(player));
            boolean ok = result.getClass().getSimpleName().toLowerCase().contains("success");
            if (ok) {
                ACTIVE.put(player.getUuid(), new Active(gymId, npc.getUuid()));
                player.sendMessage(Text.literal("§e[관장] " + gym.typeKo() + " 관장과의 배틀 시작! (Lv" + gym.levelCap() + ")"), false);
            } else {
                npc.discard();
                player.sendMessage(Text.literal("§c[관장] 배틀을 시작하지 못했습니다(파티 확인)."), false);
            }
            return ok;
        } catch (Throwable t) {
            PoroMonCore.LOGGER.error("[Gym] startChallenge 실패", t);
            player.sendMessage(Text.literal("§c[관장] 오류로 배틀을 시작하지 못했습니다."), false);
            return false;
        }
    }

    private static void onVictory(BattleVictoryEvent event) {
        try {
            for (BattleActor actor : event.getWinners()) resolve(actor, true);
            for (BattleActor actor : event.getLosers()) resolve(actor, false);
        } catch (Throwable t) {
            PoroMonCore.LOGGER.error("[Gym] onVictory 처리 실패", t);
        }
    }

    private static void resolve(BattleActor actor, boolean won) {
        if (!(actor instanceof PlayerBattleActor pba)) return;
        ServerPlayerEntity player = pba.getEntity();
        if (player == null) return;
        Active a = ACTIVE.remove(player.getUuid());
        if (a == null) return; // 관장 배틀이 아님

        discardNpc(player.getServer(), a.npcUuid);
        GymInfo.Gym gym = GymInfo.byId(a.gymId);
        if (gym == null) return;

        if (!won) {
            player.sendMessage(Text.literal("§c[관장] " + gym.typeKo() + " 관장에게 패배했습니다. 다시 도전하세요."), false);
            return;
        }
        PoroMonState state = PoroMonState.get(player.getServer());
        PlayerProgress p = state.getOrCreate(player.getUuid());
        boolean firstClear = p.badges.add(a.gymId);
        if (firstClear) {
            state.markDirty();
            long gold = 500L * gym.order();
            EconomyBridge.deposit(player, gold, "gym_clear:" + a.gymId);
            player.sendMessage(Text.literal("§a[관장] 승리! §6" + gym.badgeKo() + "§a 획득 + " + gold + "골드!"), false);
            PoroMonCore.LOGGER.info("[Gym] {} 클리어: {} (+{}골드)", player.getGameProfile().getName(), a.gymId, gold);
        } else {
            player.sendMessage(Text.literal("§a[관장] 승리! §7(이미 보유한 배지 — 재도전 보상 없음)"), false);
        }
    }

    private static void discardNpc(MinecraftServer server, UUID npcUuid) {
        if (npcUuid == null) return;
        for (ServerWorld world : server.getWorlds()) {
            Entity e = world.getEntity(npcUuid);
            if (e != null) { e.discard(); return; }
        }
    }

    /** 안전망: 배틀이 끝났는데(이벤트 누락 등) ACTIVE에 남은 항목 정리. */
    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        ACTIVE.entrySet().removeIf(en -> {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(en.getKey());
            if (player == null) {
                discardNpc(server, en.getValue().npcUuid());
                return true;
            }
            return false;
        });
    }
}
