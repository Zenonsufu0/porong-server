package com.poro.rpg.listener;

import com.poro.rpg.growth.island.IslandStorage;
import com.poro.rpg.growth.island.IslandStorageStore;
import com.poro.rpg.growth.island.IslandTerritoryState;
import com.poro.rpg.growth.island.IslandTerritoryStateStore;
import com.poro.rpg.growth.island.WorkshopJob;
import com.poro.rpg.gui.TerritoryHubGui;
import com.poro.rpg.gui.WorkshopGui;
import com.poro.rpg.gui.WorkshopGui.WorkshopTab;
import com.poro.rpg.gui.WorkshopRecipe;
import com.poro.rpg.gui.WorkshopRecipeRegistry;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Objects;

/** 공방 GUI 클릭 처리. */
public class WorkshopGuiListener implements Listener {

    private final IslandTerritoryStateStore stateStore;
    private final IslandStorageStore storageStore;
    @SuppressWarnings("unused")
    private final Plugin plugin;

    public WorkshopGuiListener(IslandTerritoryStateStore stateStore, IslandStorageStore storageStore, Plugin plugin) {
        this.stateStore = stateStore;
        this.storageStore = storageStore;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWorkshopClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!WorkshopGui.isTitle(event.getView().title())) return;

        event.setCancelled(true);
        settleCompleted(player);
        int slot = event.getRawSlot();
        WorkshopTab currentTab = WorkshopGui.tabFromTitle(event.getView().title());
        if (currentTab == null) currentTab = WorkshopTab.ESTATE;

        // 탭 클릭 (슬롯 0 ~ TABS.length-1)
        WorkshopTab[] tabs = WorkshopTab.values();
        if (slot >= 0 && slot < tabs.length) {
            WorkshopTab clicked = tabs[slot];
            if (clicked != currentTab) {
                IslandTerritoryState territory = stateStore.get(player.getUniqueId()).orElse(null);
                WorkshopGui.open(player, clicked, territory);
            }
            return;
        }

        // 레시피 클릭 (슬롯 9~17)
        if (slot >= 9 && slot <= 17) {
            int recipeIndex = slot - 9;
            List<WorkshopRecipe> recipes = WorkshopRecipeRegistry.getRecipes(currentTab);
            if (recipeIndex < recipes.size()) {
                handleRecipeEnqueue(player, currentTab, recipes.get(recipeIndex));
            }
            return;
        }

        // 뒤로
        if (slot == WorkshopGui.SLOT_BACK) {
            TerritoryHubGui.open(player);
            return;
        }
    }

    private void handleRecipeEnqueue(Player player, WorkshopTab tab, WorkshopRecipe recipe) {
        IslandTerritoryState territory = stateStore.getOrCreate(
                player.getUniqueId(), player.getName());

        if (territory.workshopFull()) {
            player.sendMessage("§c[공방] 대기열이 꽉 찼습니다. 공방 가공기를 더 설치하거나 완료를 기다리세요.");
            return;
        }

        // 재료 검증
        for (WorkshopRecipe.RecipeMaterial mat : recipe.materials()) {
            long needed = mat.amount();
            long have = mat.isVanilla()
                    ? countInInventory(player, mat.asMaterial())
                    : territory.getCustomItem(mat.itemId());
            if (have < needed) {
                player.sendMessage("§c[공방] 재료 부족: §e"
                        + WorkshopRecipeRegistry.displayName(mat.itemId())
                        + " §c" + needed + "개 필요, 보유 §e" + have + "개");
                return;
            }
        }

        // 재료 차감
        for (WorkshopRecipe.RecipeMaterial mat : recipe.materials()) {
            if (mat.isVanilla()) {
                removeFromInventory(player, mat.asMaterial(), mat.amount());
            } else {
                territory.withdrawCustomItem(mat.itemId(), mat.amount());
            }
        }

        // 대기열 등록
        long now = System.currentTimeMillis();
        long completeAt = now + recipe.durationMinutes() * 60_000L;
        territory.addWorkshopJob(new WorkshopJob(recipe.recipeId(), now, completeAt));

        player.sendMessage("§a[공방] §e" + recipe.displayName()
                + " §a제작 등록 완료! (" + recipe.durationMinutes() + "분)");
        WorkshopGui.open(player, tab, territory);
    }

    // ─── 인벤토리 유틸 ─────────────────────────────────────────────

    private static long countInInventory(Player player, Material material) {
        if (material == null) return 0L;
        long count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private static void removeFromInventory(Player player, Material material, long amount) {
        if (material == null) return;
        long remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != material) continue;
            long take = Math.min(item.getAmount(), remaining);
            remaining -= take;
            int left = item.getAmount() - (int) take;
            player.getInventory().setItem(i, left > 0 ? new ItemStack(material, left) : null);
        }
    }

    private void settleCompleted(Player player) {
        IslandTerritoryState territory = stateStore.get(player.getUniqueId()).orElse(null);
        if (territory == null) return;
        IslandStorage storage = storageStore.get(player.getUniqueId()).orElse(null);

        List<WorkshopJob> done = territory.collectCompletedJobs(System.currentTimeMillis());
        if (done.isEmpty()) return;

        for (WorkshopJob job : done) {
            WorkshopRecipeRegistry.getById(job.recipeId()).ifPresent(recipe -> {
                String resultId = recipe.resultItemId();
                long amount = recipe.resultAmount();
                if (Material.matchMaterial(resultId.toUpperCase()) != null && storage != null) {
                    storage.add(Material.valueOf(resultId.toUpperCase()), amount);
                } else {
                    territory.addCustomItem(resultId, amount);
                }
            });
        }

        player.sendMessage("§a[공방] 완료된 제작 §e" + done.size()
                + "§a건의 결과물이 저장고에 입금되었습니다.");
    }
}
