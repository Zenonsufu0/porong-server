package kr.zenon.moncore.battle;

import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.npc.NPCClass;
import com.cobblemon.mod.common.api.npc.NPCClasses;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.storage.party.NPCPartyStore;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.BattleStartResult;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.npc.NPCBattleActor;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.github.yajatkaul.mega_showdown.gimmick.MegaGimmick;
import kr.zenon.moncore.ZenonMonCore;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 배틀타워 실배틀 트리거 — 근본원인(battle_tower_design §4-N) 적용:
 * /summon NPC는 배틀 파티 미초기화 → pvn NoPartyError. 그래서 **파티를 코드로 빌드해
 * BattleBuilder.pvn(player, npc, …, party)로 명시 주입**한다(자동 getPartyForChallenge 우회).
 *
 * 0.1 검증판: 층 파티 하드코딩(20층=첫 메가). 향후 config/검증 §3 데이터로 대체.
 */
public final class BattleTowerService {
    private BattleTowerService() {}

    /** 진행 중 타워 배틀의 플레이어 UUID — tick에서 NPC 메가 발동/아이템드롭 차단. */
    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();
    /** 이미 메가 발동한 포켓몬 UUID — 매 틱 재발동(무한 메가 애니 루프) 방지. */
    private static final Set<UUID> MEGA_DONE = ConcurrentHashMap.newKeySet();

    // 20층(첫 메가) 파티 — battle_tower_design §3 / jar 검증된 종·기술·아이템
    private static final String[] FLOOR_20 = {
            "garchomp level=100 ability=roughskin nature=jolly moves=earthquake,dragonclaw,stealthrock,swordsdance",
            "rotom level=100 nature=modest moves=thunderbolt,voltswitch,shadowball,willowisp",
            "scizor level=100 held_item=mega_showdown:scizorite nature=adamant moves=bulletpunch,uturn,swordsdance,roost",
            "breloom level=100 held_item=cobblemon:focus_sash nature=jolly moves=spore,machpunch,bulletseed,swordsdance",
            "volcarona level=100 held_item=cobblemon:life_orb nature=timid moves=quiverdance,fierydance,bugbuzz,gigadrain",
            "tyranitar level=100 held_item=cobblemon:leftovers nature=adamant moves=stoneedge,crunch,earthquake,thunderwave"
    };

    /** 운영자 강제 해제: 배틀타워 추적 제거(배틀 종료는 호출측에서 battle.end). */
    public static boolean forceEnd(ServerPlayerEntity player) {
        return ACTIVE.remove(player.getUuid());
    }

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
            // ⚠️ skill: StrongBattleAI checkSwitchOutSkill 임계가 skill5=1.0(매턴 스위칭)·skill1≈0(거의 안바꿈).
            //    DataTracker 반영 위해 spawn 후 설정. 1=무한 스위칭 억제, 공격 위주.
            npc.setSkill(1);

            // 2) 층 파티를 코드로 빌드(명시 주입용)
            NPCPartyStore party = new NPCPartyStore(npc);
            int added = 0;
            for (String spec : specs) {
                PokemonProperties props = PokemonProperties.Companion.parse(spec, " ", "=");
                Pokemon pokemon = props.create();
                if (party.add(pokemon)) added++;
            }
            npc.setParty(party); // NPC(타워) 파티
            ZenonMonCore.LOGGER.info("[BattleTower] 층 {} 파티 {}마리 빌드", floor, added);

            // 3) pvn 호출. ⚠️ 마지막 PartyStore = "플레이어" 파티 → 반드시 플레이어 본인 파티.
            //    (NPC 파티를 넘기면 플레이어가 타워 팀으로 싸우는 버그). NPC 파티는 위 setParty로 전달.
            BattleFormat format = BattleFormat.Companion.getGEN_9_SINGLES();
            BattleStartResult result = BattleBuilder.INSTANCE.pvn(
                    player, npc, npc.getUuid(), format, /*cloneParties*/ true, /*healFirst*/ true,
                    com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(player));

            String type = result.getClass().getSimpleName();
            ZenonMonCore.LOGGER.info("[BattleTower] pvn 결과: {}", type);
            boolean ok = type.toLowerCase().contains("success");
            if (ok) {
                ACTIVE.add(player.getUuid()); // tick에서 메가/드롭차단 관리
            } else {
                npc.discard(); // 실패 시 NPC 정리
            }
            return ok;
        } catch (Throwable t) {
            ZenonMonCore.LOGGER.error("[BattleTower] startFloor 실패", t);
            return false;
        }
    }

    /**
     * 진행 중 타워 배틀 점검(ZenonMonCore가 주기 호출): ① NPC 활성 포켓몬이 메가스톤 보유 시
     * MegaGimmick.megaEvolveInBattle 강제 발동(NPC 자동메가 미지원 보완) ② 양측 활성 포켓몬
     * canDropHeldItem=false(기절 시 held item 바닥 드롭 방지). 배틀 종료 시 추적 제거.
     */
    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        ACTIVE.removeIf(uuid -> {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player == null) return true;
            PokemonBattle battle = BattleRegistry.getBattleByParticipatingPlayer(player);
            if (battle == null) return true; // 배틀 종료 → 추적 제거
            try {
                for (BattleActor actor : battle.getActors()) {
                    boolean isNpc = actor instanceof NPCBattleActor;
                    for (ActiveBattlePokemon active : actor.getActivePokemon()) {
                        BattlePokemon bp = active.getBattlePokemon();
                        if (bp == null) continue;
                        Pokemon p = bp.getEffectedPokemon();
                        p.setCanDropHeldItem$common(false); // 아이템 드롭 차단(양측)
                        // NPC 메가: 메가스톤 보유 + 아직 안 했음(중복 발동=무한 애니 루프 방지)
                        if (isNpc && holdsMegaStone(p) && !MegaGimmick.isMega(p)
                                && !MEGA_DONE.contains(p.getUuid())) {
                            MegaGimmick.megaEvolveInBattle(p, bp);
                            MEGA_DONE.add(p.getUuid());
                            ZenonMonCore.LOGGER.info("[BattleTower] NPC 메가 발동: {}", p.getSpecies().getName());
                        }
                    }
                }
            } catch (Throwable t) {
                ZenonMonCore.LOGGER.error("[BattleTower] tick 처리 실패", t);
            }
            return false;
        });
        if (ACTIVE.isEmpty()) MEGA_DONE.clear(); // 모든 배틀 종료 시 정리
    }

    private static boolean holdsMegaStone(Pokemon p) {
        try {
            Identifier id = Registries.ITEM.getId(p.heldItem().getItem());
            return id.getNamespace().equals("mega_showdown")
                    && id.getPath().endsWith("ite")
                    && !id.getPath().contains("pouch"); // tera_pouch_white 오검출 제외
        } catch (Throwable t) {
            return false;
        }
    }
}
