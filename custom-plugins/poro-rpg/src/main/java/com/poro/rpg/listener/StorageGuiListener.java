package com.poro.rpg.listener;

import com.poro.rpg.combat.weapon.WeaponTypeResolver;
import com.poro.rpg.growth.island.IslandStorage;
import com.poro.rpg.growth.island.IslandStorageStore;
import com.poro.rpg.growth.island.IslandTerritoryState;
import com.poro.rpg.growth.island.IslandTerritoryStateStore;
import com.poro.rpg.gui.CustomItemModel;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 영지 저장고 GUI 클릭 처리 (DL-129 추가#29 — 커스텀 재료(생산·드랍) 통합).
 * 페이지 상태는 UUID → page(0-indexed) 맵으로 추적.
 */
public class StorageGuiListener implements Listener {

    private static final String PREFIX = TutorialService.PREFIX;
    private static final NumberFormat FMT = NumberFormat.getNumberInstance(Locale.KOREA);

    private final IslandStorageStore storageStore;
    private final IslandTerritoryStateStore territoryStore;
    private final Map<UUID, Integer> currentPage = new ConcurrentHashMap<>();

    public StorageGuiListener(IslandStorageStore storageStore, IslandTerritoryStateStore territoryStore) {
        this.storageStore = storageStore;
        this.territoryStore = territoryStore;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onStorage(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!StorageGui.isTitle(event.getView().title())) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();
        UUID uuid = player.getUniqueId();
        IslandStorage storage = storageStore.getOrCreate(uuid);
        IslandTerritoryState territory = territoryStore.getOrCreate(uuid);
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
            StorageGui.render(inv, territory, storage, newPage);
            return;
        }
        if (slot == StorageGui.SLOT_NEXT) {
            int maxPage = Math.max(0, (int) Math.ceil((double) StorageGui.entries(territory, storage).size()
                    / StorageGui.ITEMS_PER_PAGE) - 1);
            int newPage = Math.min(maxPage, page + 1);
            currentPage.put(uuid, newPage);
            StorageGui.render(inv, territory, storage, newPage);
            return;
        }

        // ── 전체 입금 ──────────────────────────────────────────
        if (slot == StorageGui.SLOT_DEPOSIT_ALL) {
            depositAll(player, territory, storage);
            StorageGui.render(inv, territory, storage, page);
            return;
        }

        // ── 플레이어 인벤 아이템 클릭 — 입금 ──
        if (slot >= inv.getSize()) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;

            // 1) 창고에서 출금한 커스텀 재료(PDC id 태그) — 정확한 id 기준 입금 (우클릭=같은 id 전부)
            String customId = CustomItemModel.readStorageId(clicked);
            if (customId != null) {
                net.kyori.adventure.text.Component name =
                        net.kyori.adventure.text.Component.text(com.poro.rpg.gui.WorkshopRecipeRegistry.displayName(customId));
                if (event.getClick() == ClickType.RIGHT) {
                    long total = 0;
                    var pinv = player.getInventory();
                    for (int i = 0; i < pinv.getSize(); i++) {
                        ItemStack it = pinv.getItem(i);
                        if (it != null && customId.equals(CustomItemModel.readStorageId(it))) {
                            total += it.getAmount();
                            pinv.setItem(i, null);
                        }
                    }
                    if (total > 0) { territory.addCustomItem(customId, total); sendDeposit(player, name, total); }
                } else {
                    territory.addCustomItem(customId, 1);
                    clicked.setAmount(clicked.getAmount() - 1);
                    if (clicked.getAmount() <= 0) event.setCurrentItem(null);
                    sendDeposit(player, name, 1);
                }
                StorageGui.render(inv, territory, storage, page);
                return;
            }

            // 2) 무기·메뉴·기타 커스텀 모델 아이템 — Material로 쓸어담지 않음 (이중입금 버그 차단)
            if (isBlockedForStorage(clicked) || hasCustomModel(clicked)) {
                player.sendMessage(PREFIX + "§c이 아이템은 창고에 넣을 수 없습니다.");
                return;
            }

            // 3) 바닐라 Material 입금 (좌클릭=1개 / 우클릭=같은 Material 전부). 이름은 번역키(한국어 클라).
            Material mat = clicked.getType();
            net.kyori.adventure.text.Component vname =
                    net.kyori.adventure.text.Component.translatable(mat.translationKey());
            if (event.getClick() == ClickType.RIGHT) {
                long total = 0;
                var pinv = player.getInventory();
                for (int i = 0; i < pinv.getSize(); i++) {
                    ItemStack it = pinv.getItem(i);
                    if (it != null && it.getType() == mat && !isBlockedForStorage(it) && !hasCustomModel(it)) {
                        total += it.getAmount();
                        pinv.setItem(i, null);
                    }
                }
                if (total > 0) { storage.add(mat, total); sendDeposit(player, vname, total); }
            } else {
                storage.add(mat, 1);
                clicked.setAmount(clicked.getAmount() - 1);
                if (clicked.getAmount() <= 0) event.setCurrentItem(null);
                sendDeposit(player, vname, 1);
            }
            StorageGui.render(inv, territory, storage, page);
            return;
        }

        // ── 아이템 슬롯 (0~44) — 출금 ─────────────────────────
        if (slot >= 0 && slot < StorageGui.ITEMS_PER_PAGE) {
            List<StorageGui.Entry> entries = StorageGui.entries(territory, storage);
            int index = page * StorageGui.ITEMS_PER_PAGE + slot;
            if (index >= entries.size()) return;
            StorageGui.Entry entry = entries.get(index);

            // 흔적 인스턴스는 표시 전용 — 전승 GUI/경매장에서 사용(물리 출금 없음, DL-129 추가#38).
            if (entry.isTrace()) {
                player.sendMessage(PREFIX + "§7흔적은 §f전승 GUI§7에서 장비에 적용하거나 §f경매장§7에서 거래하세요.");
                return;
            }

            long requested = switch (event.getClick()) {
                case RIGHT -> 1L;
                case SHIFT_LEFT, SHIFT_RIGHT -> 256L;
                default -> 64L; // 좌클릭
            };

            if (entry.isCustom()) {
                long held = territory.getCustomItem(entry.customId());
                long take = Math.min(requested, held);
                if (take <= 0) return;
                territory.withdrawCustomItem(entry.customId(), take);
                giveCustom(player, territory, entry.customId(), take);
            } else {
                Material mat = entry.material();
                long actual = storage.withdraw(mat, requested);
                if (actual <= 0) return;
                List<ItemStack> toGive = splitIntoStacks(mat, (int) Math.min(actual, Integer.MAX_VALUE), mat.getMaxStackSize());
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(toGive.toArray(new ItemStack[0]));
                if (!leftover.isEmpty()) {
                    long returned = leftover.values().stream().mapToLong(ItemStack::getAmount).sum();
                    storage.add(mat, returned);
                    player.sendMessage(PREFIX + "§c인벤토리가 가득 찼습니다.");
                }
            }
            StorageGui.render(inv, territory, storage, page);
        }
    }

    // ─── helper ──────────────────────────────────────────────────

    /** 입금 안내 — 이름(커스텀=한글 텍스트 / 바닐라=번역키)을 한국어 클라에서 자연스럽게 표시 (DL-129#34). */
    private void sendDeposit(Player player, net.kyori.adventure.text.Component name, long amount) {
        var legacy = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection();
        player.sendMessage(legacy.deserialize(PREFIX + "§a입금: ")
                .append(name.color(net.kyori.adventure.text.format.NamedTextColor.WHITE))
                .append(legacy.deserialize(" §7×" + FMT.format(amount))));
    }

    /** 커스텀 재료 출금 지급 — PAPER 64개 단위로 분할, 못 받은 수량은 창고에 반환. */
    private void giveCustom(Player player, IslandTerritoryState territory, String id, long amount) {
        List<ItemStack> stacks = new ArrayList<>();
        long remaining = amount;
        while (remaining > 0) {
            int batch = (int) Math.min(remaining, 64);
            stacks.add(CustomItemModel.buildStack(id, batch));
            remaining -= batch;
        }
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stacks.toArray(new ItemStack[0]));
        if (!leftover.isEmpty()) {
            long ret = leftover.values().stream().mapToLong(ItemStack::getAmount).sum();
            territory.addCustomItem(id, ret);
            player.sendMessage(PREFIX + "§c인벤토리가 가득 찼습니다.");
        }
    }

    private void depositAll(Player player, IslandTerritoryState territory, IslandStorage storage) {
        var inv = player.getInventory();
        int customKinds = 0;
        int vanillaKinds = 0;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) continue;
            long amount = item.getAmount();
            // 창고 출금 커스텀 재료(PDC id) → customItems 복귀
            String id = CustomItemModel.readStorageId(item);
            if (id != null) {
                territory.addCustomItem(id, amount);
                inv.setItem(i, null);
                customKinds++;
                continue;
            }
            // 기타 커스텀 모델(무기·소비재 등) → 제외
            if (hasCustomModel(item) || isBlockedForStorage(item)) continue;
            // 바닐라
            storage.add(item.getType(), amount);
            inv.setItem(i, null);
            vanillaKinds++;
        }
        if (customKinds == 0 && vanillaKinds == 0) {
            player.sendMessage(PREFIX + "§7입금할 아이템이 없습니다.");
            return;
        }
        player.sendMessage(PREFIX + "§a창고에 입금했습니다.");
    }

    /** 컴포넌트 기반 커스텀 모델(CMD 문자열) 보유 여부 — 바닐라 입금 차단용. */
    private static boolean hasCustomModel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var comp = item.getItemMeta().getCustomModelDataComponent();
        return comp != null && comp.getStrings() != null && !comp.getStrings().isEmpty();
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
