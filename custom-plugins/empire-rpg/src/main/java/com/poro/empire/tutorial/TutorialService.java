package com.poro.empire.tutorial;

import com.poro.empire.common.flag.PlayerFlagRepository;
import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.hotbar.HotbarService;
import com.poro.empire.storage.PlayerDataManager;
import org.bukkit.plugin.Plugin;

public final class TutorialService {
    public static final String PREFIX = "§8[§e포로§8] ";

    public TutorialService(
            Plugin plugin,
            PlayerFlagRepository repository,
            PlayerDataManager playerDataManager,
            HotbarService hotbarService,
            DomainLogger logger
    ) {
    }
}
