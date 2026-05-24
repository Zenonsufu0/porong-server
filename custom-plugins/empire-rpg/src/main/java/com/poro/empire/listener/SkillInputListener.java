package com.poro.empire.listener;

import com.poro.empire.combat.SkillService;
import com.poro.empire.combat.weapon.WeaponType;
import com.poro.empire.combat.weapon.WeaponTypeResolver;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public final class SkillInputListener implements Listener {
    private final SkillService skillService;

    public SkillInputListener(SkillService skillService) {
        this.skillService = skillService;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.useItemInHand() == Result.DENY) {
            return;
        }
        String skillKey = basicSkillKey(WeaponTypeResolver.resolve(event.getPlayer()));
        if (skillKey != null) {
            event.setCancelled(skillService.useSkill(event.getPlayer(), skillKey));
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        String skillKey = specialSkillKey(WeaponTypeResolver.resolve(event.getPlayer()));
        if (skillKey == null) {
            return;
        }
        event.setCancelled(true);
        skillService.useSkill(event.getPlayer(), skillKey);
    }

    private String basicSkillKey(WeaponType weaponType) {
        return switch (weaponType) {
            case SWORD -> "sword:flash_slash";
            case AXE -> "axe:smash";
            case SPEAR -> "spear:thrust";
            case CROSSBOW -> "crossbow:rapid_fire";
            case SCYTHE -> "scythe:death_slash";
            case STAFF -> "staff:arcane_orb";
            case NONE -> null;
        };
    }

    private String specialSkillKey(WeaponType weaponType) {
        return switch (weaponType) {
            case SWORD -> "sword:final_strike";
            case AXE -> "axe:colossal_drop";
            case SPEAR -> "spear:thunderstrike";
            case CROSSBOW -> "crossbow:sniper";
            case SCYTHE -> "scythe:execution";
            case STAFF -> "staff:starburst";
            case NONE -> null;
        };
    }
}
