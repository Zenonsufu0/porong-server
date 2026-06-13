package kr.zenon.rpg.tutorial;

import kr.zenon.rpg.common.flag.PlayerFlagRepository;
import kr.zenon.rpg.common.logging.DomainLogger;
import kr.zenon.rpg.hotbar.HotbarService;
import kr.zenon.rpg.storage.PlayerDataManager;
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
