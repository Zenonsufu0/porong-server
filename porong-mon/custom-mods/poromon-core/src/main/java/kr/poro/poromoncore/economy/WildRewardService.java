package kr.poro.poromoncore.economy;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import kr.poro.poromoncore.PoroMonCore;
import kr.poro.poromoncore.config.ConfigManager;
import kr.poro.poromoncore.config.EconomyConfig.PokemonRewards;
import kr.poro.poromoncore.event.EventManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * 야생 포켓몬 골드 보상 (economy_design §3, 결정). 야생만·레벨 비례.
 * 처치 = 야생 배틀 승리(BATTLE_VICTORY, loser=WILD) 레벨합×defeatPerLevel.
 * 포획 = POKEMON_CAPTURED 레벨×capturePerLevel. 골드 부스트(EventManager) 반영.
 */
public final class WildRewardService {
    private WildRewardService() {}

    public static void registerEvents() {
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, WildRewardService::onCapture);
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, WildRewardService::onVictory);
    }

    private static void onCapture(PokemonCapturedEvent e) {
        try {
            PokemonRewards cfg = ConfigManager.economy().pokemonRewards;
            if (!cfg.enabled) return;
            ServerPlayerEntity player = e.getPlayer();
            if (player == null) return;
            long gold = Math.round((long) e.getPokemon().getLevel() * cfg.capturePerLevel * EventManager.goldMultiplier());
            if (gold > 0) {
                EconomyBridge.deposit(player, gold, "wild_capture");
                player.sendMessage(Text.literal("§6+" + gold + " 골드 §7(포획)"), true);
            }
        } catch (Throwable t) {
            PoroMonCore.LOGGER.error("[WildReward] onCapture 실패", t);
        }
    }

    private static void onVictory(BattleVictoryEvent e) {
        try {
            PokemonRewards cfg = ConfigManager.economy().pokemonRewards;
            if (!cfg.enabled) return;
            int wildLevelSum = 0;
            for (BattleActor loser : e.getLosers()) {
                if (loser.getType() != ActorType.WILD) continue; // 야생만(트레이너/관장 제외)
                for (BattlePokemon bp : loser.getPokemonList()) {
                    wildLevelSum += bp.getEffectedPokemon().getLevel();
                }
            }
            if (wildLevelSum <= 0) return;
            long gold = Math.round((long) wildLevelSum * cfg.defeatPerLevel * EventManager.goldMultiplier());
            if (gold <= 0) return;
            for (BattleActor winner : e.getWinners()) {
                if (winner instanceof PlayerBattleActor pba) {
                    ServerPlayerEntity player = pba.getEntity();
                    if (player == null) continue;
                    EconomyBridge.deposit(player, gold, "wild_defeat");
                    player.sendMessage(Text.literal("§6+" + gold + " 골드 §7(처치)"), true);
                }
            }
        } catch (Throwable t) {
            PoroMonCore.LOGGER.error("[WildReward] onVictory 실패", t);
        }
    }
}
