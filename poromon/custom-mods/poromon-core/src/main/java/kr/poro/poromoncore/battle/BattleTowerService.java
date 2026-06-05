package kr.poro.poromoncore.battle;

import com.cobblemon.mod.common.api.npc.NPCClass;
import com.cobblemon.mod.common.api.npc.NPCClasses;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.storage.party.NPCPartyStore;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleStartResult;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kr.poro.poromoncore.PoroMonCore;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/**
 * 배틀타워 실배틀 트리거 — 근본원인(battle_tower_design §4-N) 적용:
 * /summon NPC는 배틀 파티 미초기화 → pvn NoPartyError. 그래서 **파티를 코드로 빌드해
 * BattleBuilder.pvn(player, npc, …, party)로 명시 주입**한다(자동 getPartyForChallenge 우회).
 *
 * 0.1 검증판: 층 파티 하드코딩(20층=첫 메가). 향후 config/검증 §3 데이터로 대체.
 */
public final class BattleTowerService {
    private BattleTowerService() {}

    // 20층(첫 메가) 파티 — battle_tower_design §3 / jar 검증된 종·기술·아이템
    private static final String[] FLOOR_20 = {
            "garchomp level=100 ability=roughskin nature=jolly moves=earthquake,dragonclaw,stealthrock,swordsdance",
            "rotom level=100 nature=modest moves=thunderbolt,voltswitch,shadowball,willowisp",
            "scizor level=100 held_item=mega_showdown:scizorite nature=adamant moves=bulletpunch,uturn,swordsdance,roost",
            "breloom level=100 held_item=cobblemon:focus_sash nature=jolly moves=spore,machpunch,bulletseed,swordsdance",
            "volcarona level=100 held_item=cobblemon:life_orb nature=timid moves=quiverdance,fierydance,bugbuzz,gigadrain",
            "tyranitar level=100 held_item=cobblemon:leftovers nature=adamant moves=stoneedge,crunch,earthquake,thunderwave"
    };

    /** 지정 층 배틀 시작(0.1: 20층만). 성공 시 true. */
    public static boolean startFloor(ServerPlayerEntity player, int floor) {
        try {
            String[] specs = FLOOR_20; // 0.1: 층 무관 20층 사용(검증용)

            ServerWorld world = player.getServerWorld();

            // 1) NPC 엔티티 생성 + 클래스(스킬 출처) 부여 + 스폰
            //    ⚠️ 플레이어와 같은 좌표에 두면 배틀의 방향벡터 정규화가 0벡터→NaN→invalid position.
            //    → 플레이어가 바라보는 방향 3칸 앞에 스폰(분리 보장).
            NPCEntity npc = new NPCEntity(world);
            NPCClass npcClass = NPCClasses.getByIdentifier(Identifier.of("cobblemon", "standard"));
            if (npcClass != null) {
                npc.setNpc(npcClass);
            }
            double rad = Math.toRadians(player.getYaw());
            double nx = player.getX() - Math.sin(rad) * 3.0;
            double nz = player.getZ() + Math.cos(rad) * 3.0;
            float facing = player.getYaw() + 180.0f; // 플레이어를 마주보게
            npc.refreshPositionAndAngles(nx, player.getY(), nz, facing, 0.0f);
            world.spawnEntity(npc);

            // 2) 층 파티를 코드로 빌드(명시 주입용)
            NPCPartyStore party = new NPCPartyStore(npc);
            int added = 0;
            for (String spec : specs) {
                PokemonProperties props = PokemonProperties.Companion.parse(spec, " ", "=");
                Pokemon pokemon = props.create();
                if (party.add(pokemon)) added++;
            }
            npc.setParty(party);
            PoroMonCore.LOGGER.info("[BattleTower] 층 {} 파티 {}마리 빌드", floor, added);

            // 3) pvn 호출(파티 명시 주입 → getPartyForChallenge 우회)
            BattleFormat format = BattleFormat.Companion.getGEN_9_SINGLES();
            BattleStartResult result = BattleBuilder.INSTANCE.pvn(
                    player, npc, npc.getUuid(), format, /*cloneParties*/ true, /*healFirst*/ true, party);

            String type = result.getClass().getSimpleName();
            PoroMonCore.LOGGER.info("[BattleTower] pvn 결과: {}", type);
            boolean ok = type.toLowerCase().contains("success");
            if (!ok) {
                npc.discard(); // 실패 시 NPC 정리
            }
            return ok;
        } catch (Throwable t) {
            PoroMonCore.LOGGER.error("[BattleTower] startFloor 실패", t);
            return false;
        }
    }
}
