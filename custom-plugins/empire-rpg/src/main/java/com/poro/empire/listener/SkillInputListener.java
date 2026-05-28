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
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillInputListener implements Listener {
    private static final long SWING_WINDOW_MS = 200;

    private final SkillService skillService;
    private final SafeZoneService safeZoneService;
    /** 이번 스윙에서 이미 스킬을 발동한 플레이어 UUID → 발동 시각(ms). */
    private final Map<UUID, Long> swingFiredAt = new ConcurrentHashMap<>();

    public SkillInputListener(SkillService skillService, SafeZoneService safeZoneService) {
        this.skillService = skillService;
        this.safeZoneService = safeZoneService;
    }

    /** LMB 허공 스윙 → slot1 기본기 (논타겟). 엔티티 타격 시 onAttack과 중복 방지. */
    @EventHandler
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        Player player = event.getPlayer();
        if (safeZoneService.isSafeZone(player.getLocation())) return;
        String key = slot1Key(WeaponTypeResolver.resolve(player));
        if (key == null) return;
        if (skillService.useSkill(player, key)) {
            swingFiredAt.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    /**
     * LMB 엔티티 타격 → slot1 기본기.
     * 같은 스윙에서 onSwing이 이미 발동했으면 바닐라 공격만 취소.
     */
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (safeZoneService.isSafeZone(player.getLocation())) return;

        Long swungAt = swingFiredAt.remove(player.getUniqueId());
        if (swungAt != null && System.currentTimeMillis() - swungAt < SWING_WINDOW_MS) {
            // onSwing에서 이미 발동 — 바닐라 공격만 취소
            event.setCancelled(true);
            return;
        }

        String key = slot1Key(WeaponTypeResolver.resolve(player));
        if (key != null && skillService.useSkill(player, key)) {
            event.setCancelled(true);
        }
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

        skillService.useSkill(player, key);
        // 쿨다운 여부와 무관하게 RMB 블록 상호작용은 항상 차단
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setCancelled(true);
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
