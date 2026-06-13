package kr.zenon.gun.core;

import kr.zenon.gun.registry.ZenonGunItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * 코어 레벨 정의 (M-Core 2단계 — base_raid 「코어 제작·레벨 v2」).
 *
 * 레벨 1~5. 각 레벨이 부여하는 것:
 *  - 블록 방어 내구(LV1 100 ~ LV5 1000) — 실제 블록 부수기는 M-Raid에서 이 값을 참조.
 *  - 보안칸 수(2~6) — 실제 보안칸 동작은 M-Inv.
 *  - 공격 도구 효율 해금(5~10) — 상점 인챈트북 해금은 M-Raid/상점.
 *  - 업그레이드 비용(재료 + 화폐). LV1→2 … LV4→5.
 *
 * ⚠️ 수치 전부 잠정(base_raid·economy #2). 정식은 config(toml/datapack) 이전 예정 — impl_plan §2.
 */
public final class CoreLevels {

    public static final int MIN = 1;
    public static final int MAX = 5;

    private CoreLevels() {}

    /** 영역 내 블록 1개 방어 내구(M-Raid 참조). base_raid 「블록 내구」 표. */
    public static int blockDurability(int level) {
        return switch (clamp(level)) {
            case 1 -> 100;
            case 2 -> 200;
            case 3 -> 350;
            case 4 -> 600;
            default -> 1000; // LV5
        };
    }

    /** 보안칸 수(M-Inv). LV1=2 … LV5=6. */
    public static int securitySlots(int level) {
        return clamp(level) + 1;
    }

    /** 코어 레벨에서 깰 수 있는 최대 공격 도구 효율(상점 해금 상한). LV5는 효율10. */
    public static int toolEfficiency(int level) {
        return switch (clamp(level)) {
            case 1 -> 5;
            case 2 -> 6;
            case 3 -> 7;
            case 4 -> 8;
            default -> 10; // LV5 = 효율9+10
        };
    }

    /** 업그레이드 1회분 재료(아이템·개수). */
    public record Ingredient(Item item, int count) {}

    /**
     * level → level+1 업그레이드 비용. level이 MAX 이상이면 null(최고 레벨).
     * 재료 = base_raid 표(잠정), 화폐 부가 = economy #2(0.8k~8k 잠정).
     */
    public static UpgradeCost upgradeCost(int level) {
        return switch (level) {
            case 1 -> new UpgradeCost(800, List.of(
                    new Ingredient(Items.IRON_BLOCK, 8),
                    new Ingredient(ZenonGunItems.TUNGSTEN_ALLOY.get(), 16)));
            case 2 -> new UpgradeCost(2000, List.of(
                    new Ingredient(ZenonGunItems.TUNGSTEN_ALLOY.get(), 32),
                    new Ingredient(Items.DIAMOND, 8)));
            case 3 -> new UpgradeCost(4000, List.of(
                    new Ingredient(ZenonGunItems.TUNGSTEN_ALLOY.get(), 48),
                    new Ingredient(Items.DIAMOND, 12)));
            case 4 -> new UpgradeCost(8000, List.of(
                    new Ingredient(ZenonGunItems.TUNGSTEN_ALLOY.get(), 64),
                    new Ingredient(ZenonGunItems.MILITARY_ALLOY.get(), 16),
                    new Ingredient(ZenonGunItems.ELECTRONIC_PART.get(), 8)));
            default -> null; // LV5 = 최고
        };
    }

    /** 업그레이드 비용 = 화폐 + 재료 목록. */
    public record UpgradeCost(int coins, List<Ingredient> ingredients) {}

    private static int clamp(int level) {
        return Math.max(MIN, Math.min(MAX, level));
    }
}
