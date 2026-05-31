package com.poro.empire.listener;

import com.poro.empire.growth.island.IslandTerritoryState;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * 영지(IridiumSkyblock 월드) 농작물 보호 enforcement.
 *
 * <ul>
 *   <li><b>밟기(trample)</b> — {@code CONV_CROP_PROTECT} 켜진 영지에서 경작지(FARMLAND) 밟기를
 *       성장도와 무관하게 전부 차단(실수로 경작지가 망가지는 것 방지).</li>
 *   <li><b>손 파괴(BlockBreak)</b> — 덜 자란 작물(age &lt; max)만 차단, 다 자란 작물은 수확 허용.</li>
 *   <li><b>물 휩쓸림(BlockFromTo)</b> — 물이 작물 칸으로 흘러 파괴하는 것을 차단.
 *       액체 흐름 이벤트는 플레이어 정보가 없어 토글 연동이 불가, IridiumSkyblock 월드 단위로 적용.</li>
 * </ul>
 *
 * <p>밟기·손파괴 판정은 "행위 플레이어 본인의 영지 설정" 기준 — IS API JAR 미포함이라
 * 땅 소유자 역조회 불가. 솔로 플레이(본인 영지)에서는 정확하며, 방문자 정밀 판정은 IS API(§7+) 후속.</p>
 */
public final class IslandProtectionListener implements Listener {

    private static final String ISLAND_WORLD = "IridiumSkyblock";

    private final IslandTerritoryStateStore territoryStore;

    public IslandProtectionListener(IslandTerritoryStateStore territoryStore) {
        this.territoryStore = territoryStore;
    }

    /** 밟기 — 성장도 무관 경작지 밟기 전체 차단. */
    @EventHandler(ignoreCancelled = true)
    public void onTrample(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.FARMLAND) return;
        if (!block.getWorld().getName().equals(ISLAND_WORLD)) return;
        if (cropProtectOn(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /** 손 파괴 — 덜 자란 작물만 차단(다 자란 작물은 수확 허용). */
    @EventHandler(ignoreCancelled = true)
    public void onCropBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!block.getWorld().getName().equals(ISLAND_WORLD)) return;
        if (!(block.getBlockData() instanceof Ageable ageable)) return;
        if (ageable.getAge() >= ageable.getMaximumAge()) return; // 다 자란 작물 수확 허용
        if (cropProtectOn(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /** 물 휩쓸림 — 액체가 작물 칸으로 흐르는 것을 차단(월드 단위, 플레이어 정보 없음). */
    @EventHandler(ignoreCancelled = true)
    public void onLiquidFlow(BlockFromToEvent event) {
        Block to = event.getToBlock();
        if (!to.getWorld().getName().equals(ISLAND_WORLD)) return;
        if (to.getBlockData() instanceof Ageable) {
            event.setCancelled(true);
        }
    }

    private boolean cropProtectOn(java.util.UUID playerId) {
        return territoryStore.get(playerId)
                .map(state -> state.hasConvenience(IslandTerritoryState.CONV_CROP_PROTECT))
                .orElse(false);
    }
}
