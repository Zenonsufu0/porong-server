package com.poro.empire.combat.skills;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.WeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public abstract class BaseWeaponSkill implements WeaponSkill {
    private final String key;
    private final String displayName;
    private final WeaponType weaponType;
    private final long cooldown;

    protected BaseWeaponSkill(String key, String displayName, WeaponType weaponType, long cooldown) {
        this.key         = key;
        this.displayName = displayName;
        this.weaponType  = weaponType;
        this.cooldown    = cooldown;
    }

    @Override public final String key()         { return key; }
    @Override public final String displayName()  { return displayName; }
    @Override public final WeaponType weaponType(){ return weaponType; }
    @Override public final long cooldown()       { return cooldown; }

    // --- damage ---

    protected double scaledDamage(SkillContext ctx, Player player, double coeff) {
        return ctx.weaponPower(player) * coeff;
    }

    protected double scaledDamageWithStacks(SkillContext ctx, Player player,
                                             double baseCoeff, double stackBonusPct) {
        int stacks = getStacks(ctx, player);
        return ctx.weaponPower(player) * (baseCoeff + stackBonusPct * stacks);
    }

    protected void dealDamage(Player attacker, LivingEntity target, double damage) {
        target.damage(Math.max(0.01, damage), attacker);
    }

    // --- stacks ---

    protected int getStacks(SkillContext ctx, Player player) {
        return ctx.getResourceTracker().getStack(player.getUniqueId());
    }

    protected void gainStack(SkillContext ctx, Player player, int max) {
        ctx.getResourceTracker().incrementStack(player.getUniqueId(), max);
    }

    protected void consumeStacks(SkillContext ctx, Player player) {
        ctx.getResourceTracker().resetStack(player.getUniqueId());
    }

    // --- movement ---

    protected void dashForward(Player player, double blocks) {
        Vector dir = player.getLocation().getDirection().setY(0);
        if (dir.lengthSquared() < 0.001) return;
        player.setVelocity(dir.normalize().multiply(blocks * 0.6));
    }

    protected void dashBackward(Player player, double blocks) {
        Vector dir = player.getLocation().getDirection().setY(0);
        if (dir.lengthSquared() < 0.001) return;
        player.setVelocity(dir.normalize().multiply(-blocks * 0.6));
    }

    protected void dashSideways(Player player, double blocks) {
        Vector facing = player.getLocation().getDirection().setY(0);
        if (facing.lengthSquared() < 0.001) return;
        Vector right = new Vector(-facing.normalize().getZ(), 0, facing.getX()).normalize();
        player.setVelocity(right.multiply(blocks * 0.6));
    }

    // --- utility ---

    protected void lifesteal(Player player, double amount) {
        var maxHpAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHpAttr == null) return;
        player.setHealth(Math.min(maxHpAttr.getValue(), player.getHealth() + amount));
    }
}
