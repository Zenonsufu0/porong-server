package kr.zenon.moncore.item;

import kr.zenon.moncore.ZenonMonCore;
import kr.zenon.moncore.config.ConfigManager;
import kr.zenon.moncore.config.CoreConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * 리그 패스 아이템 관리 (menu_design.md §2, MenuItemManager).
 * 책임: 패스 생성 / 식별(NBT 태그) / 인벤 보장(중복 회수 + 없으면 지급).
 * 모든 상태 변경은 서버측. 클라는 표시만.
 */
public final class MenuItemManager {
    private MenuItemManager() {}

    /** CUSTOM_DATA 식별 키 — 일반 시계와 구분, 복제/위조 방지. */
    public static final String TAG_KEY = "zenonmoncore_league_pass";

    /** 설정 기준 리그 패스 ItemStack 생성. */
    public static ItemStack createPass(CoreConfig.MenuItem cfg) {
        Item base = Items.CLOCK;
        Identifier id = Identifier.tryParse(cfg.itemId);
        if (id != null && Registries.ITEM.containsId(id)) {
            base = Registries.ITEM.get(id);
        } else {
            ZenonMonCore.LOGGER.warn("[MenuItem] itemId '{}' 미해석 → 기본 minecraft:clock 사용", cfg.itemId);
        }
        ItemStack stack = new ItemStack(base);

        NbtCompound tag = new NbtCompound();
        tag.putByte(TAG_KEY, (byte) 1);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));

        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(cfg.displayName).formatted(Formatting.GOLD, Formatting.BOLD));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("우클릭 — Zenon Mon 메뉴 열기").formatted(Formatting.GRAY),
                Text.literal("/zenonmon menu 로도 열 수 있습니다").formatted(Formatting.DARK_GRAY)
        )));
        return stack;
    }

    /** 정품 리그 패스 식별(NBT 태그 기준). */
    public static boolean isPass(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        return comp != null && comp.contains(TAG_KEY);
    }

    /**
     * 인벤토리에 정품 패스가 정확히 1개 있도록 보장.
     * 중복은 회수, 없으면 지정 핫바 슬롯(또는 빈 칸)에 지급. enabled=false면 무동작.
     * @return 보장 후 패스 보유 여부
     */
    public static boolean ensure(ServerPlayerEntity player) {
        CoreConfig.MenuItem cfg = ConfigManager.core().menuItem;
        if (!cfg.enabled) return false;

        PlayerInventory inv = player.getInventory();
        boolean found = false;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (!isPass(s)) continue;
            if (!found) {
                found = true;
                if (s.getCount() > 1) s.setCount(1); // 스택 1개로 정규화
            } else {
                inv.setStack(i, ItemStack.EMPTY); // 중복 회수(안티-듀프)
            }
        }
        if (found) return true;

        return place(player, inv, cfg);
    }

    /**
     * 슬롯 고정 백스톱(매 틱): lockSlot=true면 패스를 항상 지정 핫바 칸에 고정한다.
     * 다른 칸에 있으면 지정 칸으로 스왑, 중복 회수, 분실 시 재지급. onSlotClick 잠금이
     * 막지 못한 예외 경로(숫자키 스왑 등)·정합성 보정.
     */
    public static void enforce(ServerPlayerEntity player) {
        CoreConfig.MenuItem cfg = ConfigManager.core().menuItem;
        if (!cfg.enabled || !cfg.lockSlot) return;

        PlayerInventory inv = player.getInventory();
        int slot = cfg.hotbarSlot;
        if (slot < 0 || slot >= 9) return; // 잘못된 설정 시 고정 생략

        int passIdx = -1;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (!isPass(s)) continue;
            if (passIdx == -1) {
                passIdx = i;
                if (s.getCount() > 1) s.setCount(1);
            } else {
                inv.setStack(i, ItemStack.EMPTY); // 중복 회수
            }
        }

        if (passIdx == -1) {
            place(player, inv, cfg); // 분실 → 재지급
            return;
        }
        if (passIdx != slot) {
            ItemStack pass = inv.getStack(passIdx);
            ItemStack displaced = inv.getStack(slot);
            inv.setStack(slot, pass);
            inv.setStack(passIdx, displaced); // 지정 칸 점유물은 패스 옛 칸으로
        }
    }

    /** 핫바 지정 슬롯(비어있으면) → 첫 빈 칸 순으로 패스 배치. */
    private static boolean place(ServerPlayerEntity player, PlayerInventory inv, CoreConfig.MenuItem cfg) {
        ItemStack pass = createPass(cfg);
        int slot = cfg.hotbarSlot;
        if (slot >= 0 && slot < 9 && inv.getStack(slot).isEmpty()) {
            inv.setStack(slot, pass);
            return true;
        }
        // 지정 슬롯이 막혀있으면 첫 빈 칸
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isEmpty()) {
                inv.setStack(i, pass);
                return true;
            }
        }
        ZenonMonCore.LOGGER.warn("[MenuItem] 인벤토리 가득 — 리그 패스 지급 보류: {}",
                player.getGameProfile().getName());
        return false;
    }
}
