package com.poro.rpg.tutorial;

import com.poro.rpg.common.flag.PlayerFlagRepository;
import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.hotbar.HotbarService;
import com.poro.rpg.storage.PlayerDataManager;
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
