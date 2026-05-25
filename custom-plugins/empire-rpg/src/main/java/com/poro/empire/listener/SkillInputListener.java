package com.poro.empire.listener;

import com.poro.empire.combat.SkillService;
import com.poro.empire.combat.weapon.WeaponType;
import com.poro.empire.combat.weapon.WeaponTypeResolver;
import com.poro.empire.field.SafeZoneService;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public final class SkillInputListener implements Listener {
    private final SkillService skillService;
    private final SafeZoneService safeZoneService;

    public SkillInputListener(SkillService skillService, SafeZoneService safeZoneService) {
        this.skillService = skillService;
        this.safeZoneService = safeZoneService;
    }

    /** LMB 공격 → slot1 기본기 */
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (safeZoneService.isSafeZone(player.getLocation())) return;
        WeaponType type = WeaponTypeResolver.resolve(player);
        String key = slot1Key(type);
        if (key != null) skillService.useSkill(player, key);
    }

    /** RMB → slot2 이동기 / Shift+RMB → slot3 특수기 */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        if (event.useItemInHand() == Event.Result.DENY) return;

        Player player = event.getPlayer();
        if (safeZoneService.isSafeZone(player.getLocation())) return;
        WeaponType type = WeaponTypeResolver.resolve(player);
        String key = player.isSneaking() ? slot3Key(type) : slot2Key(type);
        if (key == null) return;

        boolean used = skillService.useSkill(player, key);
        if (used) {
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setCancelled(true);
        }
    }

    /** F키 → slot4 핵심기 */
    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (safeZoneService.isSafeZone(event.getPlayer().getLocation())) return;
        String key = slot4Key(WeaponTypeResolver.resolve(event.getPlayer()));
        if (key == null) return;
        event.setCancelled(true);
        skillService.useSkill(event.getPlayer(), key);
    }

    private String slot1Key(WeaponType t) {
        return switch (t) {
            case SWORD    -> "sword:flash_slash";
            case AXE      -> "axe:smash";
            case SPEAR    -> "spear:thrust";
            case CROSSBOW -> "crossbow:rapid_fire";
            case SCYTHE   -> "scythe:death_slash";
            case STAFF    -> "staff:arcane_orb";
            case NONE     -> null;
        };
    }

    private String slot2Key(WeaponType t) {
        return switch (t) {
            case SWORD    -> "sword:triple_strike";
            case AXE      -> "axe:crush_charge";
            case SPEAR    -> "spear:crescent";
            case CROSSBOW -> "crossbow:evade_fire";
            case SCYTHE   -> "scythe:shadow_spin";
            case STAFF    -> "staff:elemental_burst";
            case NONE     -> null;
        };
    }

    private String slot3Key(WeaponType t) {
        return switch (t) {
            case SWORD    -> "sword:guard_counter";
            case AXE      -> "axe:unyielding";
            case SPEAR    -> "spear:charge";
            case CROSSBOW -> "crossbow:pierce_bolt";
            case SCYTHE   -> "scythe:grim_strike";
            case STAFF    -> "staff:arcane_rush";
            case NONE     -> null;
        };
    }

    private String slot4Key(WeaponType t) {
        return switch (t) {
            case SWORD    -> "sword:final_strike";
            case AXE      -> "axe:colossal_drop";
            case SPEAR    -> "spear:thunderstrike";
            case CROSSBOW -> "crossbow:sniper";
            case SCYTHE   -> "scythe:execution";
            case STAFF    -> "staff:starburst";
            case NONE     -> null;
        };
    }
}
