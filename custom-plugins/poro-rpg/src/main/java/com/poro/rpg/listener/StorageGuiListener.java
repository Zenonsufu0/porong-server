package com.poro.rpg.listener;

import com.poro.rpg.combat.weapon.WeaponTypeResolver;
import com.poro.rpg.growth.island.IslandStorage;
import com.poro.rpg.growth.island.IslandStorageStore;
import com.poro.rpg.gui.StorageGui;
import com.poro.rpg.gui.TerritoryHubGui;
import com.poro.rpg.tutorial.TutorialService;
import org.bukkit.Material;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 영지 저장고 GUI 클릭 처리.
 * 페이지 상태는 UUID → page(0-indexed) 맵으로 추적.
 */
public class StorageGuiListener implements Listener {

    private static final String PREFIX = TutorialService.PREFIX;
    private static final NumberFormat FMT = NumberFormat.getNumberInstance(Locale.KOREA);

    private final IslandStorageStore storageStore;
    private final Map<UUID, Integer> currentPage = new ConcurrentHashMap<>();

    public StorageGuiListener(IslandStorageStore storageStore) {
        this.storageStore = storageStore;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onStorage(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!StorageGui.isTitle(event.getView().title())) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();
        UUID uuid = player.getUniqueId();
        IslandStorage storage = storageStore.getOrCreate(uuid);
        int page = currentPage.getOrDefault(uuid, 0);
        Inventory inv = event.getView().getTopInventory();

        // ── 내비게이션 ─────────────────────────────────────────
        if (slot == StorageGui.SLOT_BACK) {
            currentPage.remove(uuid);
            TerritoryHubGui.open(player);
            return;
        }
        if (slot == StorageGui.SLOT_PREV) {
            int newPage = Math.max(0, page - 1);
            currentPage.put(uuid, newPage);
            StorageGui.render(inv, storage, newPage);
            return;
        }
        if (slot == StorageGui.SLOT_NEXT) {
            int maxPage = Math.max(0, (int) Math.ceil((double) storage.materialList().size()
                    / StorageGui.ITEMS_PER_PAGE) - 1);
            int newPage = Math.min(maxPage, page + 1);
            currentPage.put(uuid, newPage);
            StorageGui.render(inv, storage, newPage);
            return;
        }

        // ── 전체 입금 ──────────────────────────────────────────
        if (slot == StorageGui.SLOT_DEPOSIT_ALL) {
            depositAll(player, storage);
            StorageGui.render(inv, storage, page);
            return;
        }

        // ── 플레이어 인벤 아이템 클릭 — 입금 (좌클릭=1개 / 우클릭=그 종류 전부) ──
        if (slot >= inv.getSize()) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;
            if (isBlockedForStorage(clicked)) {
                player.sendMessage(PREFIX + "§c무기·메뉴 아이템은 창고에 넣을 수 없습니다.");
                return;
            }
            Material mat = clicked.getType();
            if (event.getClick() == ClickType.RIGHT) {
                long total = 0;
                org.bukkit.inventory.PlayerInventory pinv = player.getInventory();
                for (int i = 0; i < pinv.getSize(); i++) {
                    ItemStack it = pinv.getItem(i);
                    if (it != null && it.getType() == mat && !isBlockedForStorage(it)) {
                        total += it.getAmount();
                        pinv.setItem(i, null);
                    }
                }
                if (total > 0) {
                    storage.add(mat, total);
                    player.sendMessage(PREFIX + "§a입금: §f" + titleCase(mat.name()) + " ×" + FMT.format(total));
                }
            } else { // 좌클릭 — 1개
                storage.add(mat, 1);
                clicked.setAmount(clicked.getAmount() - 1);
                if (clicked.getAmount() <= 0) event.setCurrentItem(null);
            }
            StorageGui.render(inv, storage, page);
            return;
        }

        // ── 아이템 슬롯 (0~44) — 출금 ─────────────────────────
        if (slot >= 0 && slot < StorageGui.ITEMS_PER_PAGE) {
            List<Material> materials = storage.materialList();
            int index = page * StorageGui.ITEMS_PER_PAGE + slot;
            if (index >= materials.size()) return;

            Material mat = materials.get(index);
            long withdrawAmount = switch (event.getClick()) {
                case RIGHT -> 1L;
                case SHIFT_LEFT, SHIFT_RIGHT -> 256L;
                default -> 64L; // 좌클릭
            };
            long actual = storage.withdraw(mat, withdrawAmount);
            if (actual <= 0) return;

            // 인벤토리 여유 체크 후 지급
            int stackSize = mat.getMaxStackSize();
            List<ItemStack> toGive = splitIntoStacks(mat, (int) Math.min(actual, Integer.MAX_VALUE), stackSize);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(toGive.toArray(new ItemStack[0]));
            if (!leftover.isEmpty()) {
                // 받지 못한 수량은 다시 반환
                long returned = leftover.values().stream().mapToLong(ItemStack::getAmount).sum();
                storage.add(mat, returned);
                player.sendMessage(PREFIX + "§c인벤토리가 가득 찼습니다.");
            }
            StorageGui.render(inv, storage, page);
        }
    }

    // ─── helper ──────────────────────────────────────────────────

    private void depositAll(Player player, IslandStorage storage) {
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        Map<Material, Long> deposited = new LinkedHashMap<>();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) continue;
            // CMD가 있는 커스텀 아이템(무기·장비·소비재 등) + 무기·나침반 제외
            if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) continue;
            if (isBlockedForStorage(item)) continue;
            Material mat = item.getType();
            long amount = item.getAmount();
            storage.add(mat, amount);
            deposited.merge(mat, amount, Long::sum);
            inv.setItem(i, null);
        }
        if (deposited.isEmpty()) {
            player.sendMessage(PREFIX + "§7입금할 아이템이 없습니다.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        deposited.forEach((mat, qty) -> {
            if (sb.length() > 0) sb.append(", ");
            sb.append(titleCase(mat.name())).append(" ×").append(FMT.format(qty));
        });
        player.sendMessage(PREFIX + "§a창고에 입금했습니다: §f" + sb);
    }

    /** 창고 입금 금지 아이템 — 메뉴 아이템 + 무기(PDC 태그). */
    private static boolean isBlockedForStorage(ItemStack item) {
        if (com.poro.rpg.init.ClassInitService.isMenuItem(item)) return true; // 메뉴 아이템
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer()
                .has(WeaponTypeResolver.WEAPON_TYPE_KEY, PersistentDataType.STRING)) return true; // 무기
        return false;
    }

    private static String titleCase(String name) {
        String raw = name.replace('_', ' ').toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(raw.length());
        boolean cap = true;
        for (char c : raw.toCharArray()) {
            sb.append(cap ? Character.toUpperCase(c) : c);
            cap = c == ' ';
        }
        return sb.toString();
    }

    private static List<ItemStack> splitIntoStacks(Material mat, int totalAmount, int stackSize) {
        List<ItemStack> result = new ArrayList<>();
        int remaining = totalAmount;
        while (remaining > 0) {
            int batch = Math.min(remaining, stackSize);
            result.add(new ItemStack(mat, batch));
            remaining -= batch;
        }
        return result;
    }
}
