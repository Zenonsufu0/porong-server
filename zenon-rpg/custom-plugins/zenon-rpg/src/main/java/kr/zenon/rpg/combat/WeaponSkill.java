package kr.zenon.rpg.combat;

import kr.zenon.rpg.combat.weapon.WeaponType;
import org.bukkit.entity.Player;

public interface WeaponSkill {
    String key();
    String displayName();
    WeaponType weaponType();
    long cooldown();
    boolean execute(Player player, SkillContext context);
}
