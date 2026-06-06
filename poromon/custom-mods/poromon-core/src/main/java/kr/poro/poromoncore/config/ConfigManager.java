package kr.poro.poromoncore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kr.poro.poromoncore.PoroMonCore;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * config/poromoncore/*.json 로드·생성·리로드 (config_structure.md §1).
 * 파일이 없으면 기본값으로 생성. 서버 권위 — 서버측에서만 읽는다.
 */
public final class ConfigManager {
    private ConfigManager() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile CoreConfig core = new CoreConfig();
    private static volatile EconomyConfig economy = new EconomyConfig();
    private static volatile EncounterConfig encounter = new EncounterConfig();

    public static CoreConfig core() {
        return core;
    }

    public static EconomyConfig economy() {
        return economy;
    }

    public static EncounterConfig encounter() {
        return encounter;
    }

    private static Path file(String name) {
        return FabricLoader.getInstance().getConfigDir().resolve("poromoncore").resolve(name);
    }

    /** 최초 로드(없으면 기본값 생성). onInitialize 에서 1회. */
    public static void load() {
        core = loadOrCreate("core.json", CoreConfig.class, CoreConfig::new);
        economy = loadOrCreate("economy.json", EconomyConfig.class, EconomyConfig::new);
        encounter = loadOrCreateResource("legendary_pools.json", EncounterConfig.class,
                "/poromoncore/legendary_pools.json", EncounterConfig::new);
    }

    /** 디스크에서 전체 재로드(/poromon admin reload). 실패 시 해당 파일 기존 값 유지. */
    public static boolean reload() {
        core = loadOrCreate("core.json", CoreConfig.class, CoreConfig::new);
        economy = loadOrCreate("economy.json", EconomyConfig.class, EconomyConfig::new);
        encounter = loadOrCreateResource("legendary_pools.json", EncounterConfig.class,
                "/poromoncore/legendary_pools.json", EncounterConfig::new);
        int poolCount = encounter.pools == null ? 0 : encounter.pools.size();
        PoroMonCore.LOGGER.info("[Config] 전체 리로드 완료 (core v{}, economy v{}, pools {})",
                core.configVersion, economy.configVersion, poolCount);
        return true;
    }

    /** config에 없으면 jar 번들 리소스를 복사해 생성 후 로드(전설 풀 등 대용량 기본값). */
    private static <T> T loadOrCreateResource(String name, Class<T> type, String resourcePath, Supplier<T> fallback) {
        Path file = file(name);
        try {
            if (Files.notExists(file)) {
                Files.createDirectories(file.getParent());
                try (InputStream in = ConfigManager.class.getResourceAsStream(resourcePath)) {
                    if (in == null) {
                        PoroMonCore.LOGGER.error("[Config] 번들 리소스 없음: {} — 기본값", resourcePath);
                        return fallback.get();
                    }
                    Files.write(file, in.readAllBytes());
                    PoroMonCore.LOGGER.info("[Config] {} 번들에서 생성: {}", name, file);
                }
            }
            T parsed = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), type);
            return parsed != null ? parsed : fallback.get();
        } catch (Exception e) {
            PoroMonCore.LOGGER.error("[Config] {} 로드 실패 — 기본값 사용", name, e);
            return fallback.get();
        }
    }

    private static <T> T loadOrCreate(String name, Class<T> type, Supplier<T> defaults) {
        Path file = file(name);
        try {
            if (Files.notExists(file)) {
                Files.createDirectories(file.getParent());
                T def = defaults.get();
                Files.writeString(file, GSON.toJson(def));
                PoroMonCore.LOGGER.info("[Config] {} 기본값 생성: {}", name, file);
                return def;
            }
            T parsed = GSON.fromJson(Files.readString(file), type);
            if (parsed == null) {
                PoroMonCore.LOGGER.warn("[Config] {} 파싱 null — 기본값 사용", name);
                return defaults.get();
            }
            return parsed;
        } catch (Exception e) {
            PoroMonCore.LOGGER.error("[Config] {} 로드 실패 — 기본값 사용", name, e);
            return defaults.get();
        }
    }
}
