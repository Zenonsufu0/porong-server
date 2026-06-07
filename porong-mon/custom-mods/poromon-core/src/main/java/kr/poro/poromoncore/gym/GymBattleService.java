package kr.poro.poromoncore.gym;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.api.npc.NPCClass;
import com.cobblemon.mod.common.api.npc.NPCClasses;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.storage.party.NPCPartyStore;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.BattleStartResult;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.npc.NPCBattleActor;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.github.yajatkaul.mega_showdown.gimmick.MegaGimmick;
import kr.poro.poromoncore.PoroMonCore;
import kr.poro.poromoncore.data.PlayerProgress;
import kr.poro.poromoncore.data.PoroMonState;
import kr.poro.poromoncore.economy.EconomyBridge;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
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
    /** 이미 메가 발동한 포켓몬 — 무한 메가 애니 방지. */
    private static final java.util.Set<UUID> MEGA_DONE = ConcurrentHashMap.newKeySet();

    /**
     * 관장별 파티 spec(타입 맞춤, 기술/특성/성격, 에이스=메가스톤). 레벨은 코드가 레벨캡 부착.
     * 초반 관장은 4~5마리, 후반 6마리. 0.1 하드코딩 — 향후 gyms.json.
     */
    private static final Map<String, String[]> PARTIES = Map.ofEntries(
            Map.entry("gym_bug", new String[]{
                    "butterfree ability=compoundeyes nature=modest moves=bugbuzz,airslash,sleeppowder,energyball",
                    "beedrill ability=swarm nature=adamant moves=xscissor,poisonjab,brickbreak,aerialace",
                    "venomoth ability=tintedlens nature=timid moves=bugbuzz,sludgebomb,quiverdance,sleeppowder",
                    "heracross held_item=mega_showdown:heracronite ability=guts nature=adamant moves=closecombat,megahorn,rockslide,earthquake"}),
            Map.entry("gym_rock", new String[]{
                    "graveler ability=sturdy nature=adamant moves=rockslide,earthquake,suckerpunch,explosion",
                    "onix ability=sturdy nature=adamant moves=rockslide,earthquake,ironhead,stealthrock",
                    "rhydon ability=lightningrod nature=adamant moves=earthquake,rockslide,megahorn,swordsdance",
                    "aggron held_item=mega_showdown:aggronite ability=rockhead nature=adamant moves=heavyslam,earthquake,rockslide,aquatail"}),
            Map.entry("gym_electric", new String[]{
                    "magneton ability=magnetpull nature=modest moves=thunderbolt,flashcannon,voltswitch,thunderwave",
                    "electrode ability=static nature=timid moves=thunderbolt,voltswitch,foulplay,thunderwave",
                    "lanturn ability=voltabsorb nature=modest moves=thunderbolt,surf,icebeam,voltswitch",
                    "electabuzz ability=vitalspirit nature=timid moves=thunderbolt,psychic,focusblast,icepunch",
                    "ampharos held_item=mega_showdown:ampharosite ability=static nature=modest moves=thunderbolt,dragonpulse,focusblast,voltswitch"}),
            Map.entry("gym_grass", new String[]{
                    "vileplume ability=chlorophyll nature=modest moves=energyball,sludgebomb,sleeppowder,gigadrain",
                    "victreebel ability=chlorophyll nature=adamant moves=powerwhip,poisonjab,suckerpunch,swordsdance",
                    "tangrowth ability=regenerator nature=relaxed moves=gigadrain,sludgebomb,rockslide,sleeppowder",
                    "jumpluff ability=infiltrator nature=timid moves=gigadrain,sleeppowder,acrobatics,encore",
                    "sceptile held_item=mega_showdown:sceptilite ability=overgrow nature=timid moves=leafstorm,dragonpulse,focusblast,gigadrain"}),
            Map.entry("gym_water", new String[]{
                    "starmie ability=naturalcure nature=timid moves=hydropump,icebeam,thunderbolt,psyshock",
                    "lapras ability=waterabsorb nature=modest moves=surf,icebeam,thunderbolt,freezedry",
                    "slowbro ability=regenerator nature=bold moves=scald,psychic,slackoff,icebeam",
                    "vaporeon ability=waterabsorb nature=bold moves=scald,icebeam,wish,protect",
                    "kingdra ability=swiftswim nature=modest moves=hydropump,dracometeor,icebeam,surf",
                    "gyarados held_item=mega_showdown:gyaradosite ability=intimidate nature=adamant moves=waterfall,crunch,earthquake,dragondance"}),
            Map.entry("gym_fire", new String[]{
                    "arcanine ability=intimidate nature=jolly moves=flareblitz,wildcharge,extremespeed,closecombat",
                    "rapidash ability=flashfire nature=jolly moves=flareblitz,wildcharge,megahorn,morningsun",
                    "magmortar ability=flamebody nature=modest moves=fireblast,thunderbolt,focusblast,psychic",
                    "ninetales ability=drought nature=timid moves=fireblast,solarbeam,nastyplot,willowisp",
                    "houndoom ability=flashfire nature=timid moves=fireblast,darkpulse,sludgebomb,nastyplot",
                    "charizard held_item=mega_showdown:charizardite_x ability=blaze nature=adamant moves=flareblitz,dragonclaw,earthquake,dragondance"}),
            Map.entry("gym_psychic", new String[]{
                    "slowbro ability=regenerator nature=bold moves=scald,psychic,slackoff,fireblast",
                    "exeggutor ability=harvest nature=modest moves=psychic,gigadrain,sludgebomb,sleeppowder",
                    "hypno ability=insomnia nature=calm moves=psychic,focusblast,thunderwave,nastyplot",
                    "xatu ability=magicbounce nature=timid moves=psychic,airslash,heatwave,roost",
                    "gardevoir ability=trace nature=modest moves=psychic,moonblast,focusblast,shadowball",
                    "alakazam held_item=mega_showdown:alakazite ability=magicguard nature=timid moves=psychic,focusblast,shadowball,energyball"}),
            Map.entry("gym_dragon", new String[]{
                    "dragonite held_item=cobblemon:leftovers ability=multiscale nature=adamant moves=dragondance,outrage,earthquake,extremespeed",
                    "kingdra ability=swiftswim nature=modest moves=dracometeor,hydropump,icebeam,surf",
                    "flygon ability=levitate nature=jolly moves=dragonclaw,earthquake,uturn,firepunch",
                    "haxorus ability=moldbreaker nature=jolly moves=outrage,earthquake,poisonjab,dragondance",
                    "altaria ability=naturalcure nature=adamant moves=return,earthquake,dragondance,roost",
                    "salamence held_item=mega_showdown:salamencite ability=intimidate nature=jolly moves=dragondance,outrage,earthquake,roost"})
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

    /** 운영자 강제 해제: 관장 배틀 추적 제거 + NPC 정리. */
    public static boolean forceEnd(ServerPlayerEntity player) {
        Active a = ACTIVE.remove(player.getUuid());
        if (a == null) return false;
        discardNpc(player.getServer(), a.npcUuid());
        return true;
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

    /**
     * 매 틱(20틱): 진행 중 관장 배틀의 NPC 메가 발동 + 아이템드롭 차단 + 오프라인 정리.
     * (배틀타워와 동일 패턴 — NPC 자동 메가 미지원 보완.)
     */
    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        ACTIVE.entrySet().removeIf(en -> {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(en.getKey());
            if (player == null) {
                discardNpc(server, en.getValue().npcUuid());
                return true;
            }
            PokemonBattle battle = BattleRegistry.getBattleByParticipatingPlayer(player);
            if (battle == null) return false; // 곧 BATTLE_VICTORY가 정리
            try {
                for (BattleActor actor : battle.getActors()) {
                    boolean isNpc = actor instanceof NPCBattleActor;
                    for (ActiveBattlePokemon active : actor.getActivePokemon()) {
                        BattlePokemon bp = active.getBattlePokemon();
                        if (bp == null) continue;
                        Pokemon p = bp.getEffectedPokemon();
                        p.setCanDropHeldItem$common(false);
                        if (isNpc && holdsMegaStone(p) && !MegaGimmick.isMega(p) && !MEGA_DONE.contains(p.getUuid())) {
                            MegaGimmick.megaEvolveInBattle(p, bp);
                            MEGA_DONE.add(p.getUuid());
                            PoroMonCore.LOGGER.info("[Gym] NPC 메가 발동: {}", p.getSpecies().getName());
                        }
                    }
                }
            } catch (Throwable t) {
                PoroMonCore.LOGGER.error("[Gym] tick 메가 처리 실패", t);
            }
            return false;
        });
        if (ACTIVE.isEmpty()) MEGA_DONE.clear();
    }

    private static boolean holdsMegaStone(Pokemon p) {
        try {
            var id = Registries.ITEM.getId(p.heldItem().getItem());
            if (!id.getNamespace().equals("mega_showdown")) return false;
            String path = id.getPath();
            // 일반 *ite + X/Y 변형(charizardite_x 등). pouch/tera 제외.
            boolean stone = path.endsWith("ite") || path.endsWith("ite_x") || path.endsWith("ite_y");
            return stone && !path.contains("pouch");
        } catch (Throwable t) {
            return false;
        }
    }
}
