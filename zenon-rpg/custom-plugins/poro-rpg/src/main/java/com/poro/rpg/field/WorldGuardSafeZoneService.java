package com.poro.rpg.field;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;

public final class WorldGuardSafeZoneService implements SafeZoneService {
    @Override
    public boolean isSafeZone(Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(location.getWorld()));
        if (regions == null) return false;
        StateFlag.State state = regions
                .getApplicableRegions(BukkitAdapter.asBlockVector(location))
                .queryState(null, Flags.PVP);
        return state == StateFlag.State.DENY;
    }
}
