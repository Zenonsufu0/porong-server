package com.poro.rpg.combat.effect;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * 2D 스킬/보스 이펙트를 ItemDisplay(빌보드)로 띄우는 서비스 (skill_effect_2d_integration_v1).
 *
 * 리소스팩: carrier 아이템 {@code firework_star} + {@code custom_model_data}(문자열 케이스) → poro:effect/* 평면 모델.
 * ModelEngine 없이 바닐라만으로 동작. 짧은 수명 + 스케일-팝 + 풀밝기.
 */
public final class EffectDisplayService {

    /** 디스플레이 전용 운반 아이템(플레이어에게 지급하지 않음). 이펙트 cmd 네임스페이스 격리용. */
    private static final Material CARRIER = Material.FIREWORK_STAR;

    private final JavaPlugin plugin;

    public EffectDisplayService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private ItemStack carrier(int cmd) {
        ItemStack item = new ItemStack(CARRIER);
        ItemMeta meta = item.getItemMeta();
        CustomModelDataComponent comp = meta.getCustomModelDataComponent();
        comp.setStrings(List.of(String.valueOf(cmd)));   // select property "custom_model_data" strings[0] 매칭
        meta.setCustomModelDataComponent(comp);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 플레이어 전방에 평면 이펙트를 띄운다. 빌보드 VERTICAL(시전자/관전자 정면), 등장 스케일-팝, {@code lifeTicks} 후 제거.
     * (PoC: 베기/직선 검증용. 방향 정렬 DIRECTIONAL은 후속.)
     */
    public ItemDisplay spawnFront(int cmd, Player player, double distance, float scale, int lifeTicks) {
        Location eye = player.getEyeLocation();
        Location loc = eye.clone().add(eye.getDirection().clone().normalize().multiply(distance));
        loc.setYaw(0f);
        loc.setPitch(0f);

        ItemDisplay display = player.getWorld().spawn(loc, ItemDisplay.class, e -> {
            e.setItemStack(carrier(cmd));
            e.setBillboard(Display.Billboard.VERTICAL);
            e.setBrightness(new Display.Brightness(15, 15));   // 풀밝기(글로우)
            e.setShadowRadius(0f);
            e.setShadowStrength(0f);
            e.setPersistent(false);
            float s0 = scale * 0.3f;
            e.setTransformation(transform(s0));                // 시작은 작게
        });

        // 다음 틱: 풀 스케일로 보간(팝)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!display.isValid()) return;
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(3);
            display.setTransformation(transform(scale));
        }, 1L);

        // 수명 후 제거 (누수 방지)
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> { if (display.isValid()) display.remove(); }, Math.max(1, lifeTicks));

        return display;
    }

    private static Transformation transform(float scale) {
        return new Transformation(
                new Vector3f(0f, 0f, 0f),
                new AxisAngle4f(0f, 0f, 0f, 1f),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f(0f, 0f, 0f, 1f));
    }
}
