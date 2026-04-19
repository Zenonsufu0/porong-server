package com.poro.empire.npc.citizens;

import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.common.result.ErrorCode;
import com.poro.empire.common.result.Result;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ReflectionCitizensNpcGateway implements CitizensNpcGateway {
    private final JavaPlugin plugin;
    private final DomainLogger logger;
    private final boolean available;
    private final Class<?> citizensApiClass;
    private final Class<?> lookCloseTraitClass;
    private final Class<?> skinTraitClass;

    public ReflectionCitizensNpcGateway(JavaPlugin plugin, DomainLogger logger) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = Objects.requireNonNull(logger, "logger");

        Class<?> api = null;
        Class<?> lookClose = null;
        Class<?> skin = null;
        boolean resolved = false;

        try {
            Plugin citizensPlugin = plugin.getServer().getPluginManager().getPlugin("Citizens");
            if (citizensPlugin != null && citizensPlugin.isEnabled()) {
                api = Class.forName("net.citizensnpcs.api.CitizensAPI");
                Class.forName("net.citizensnpcs.api.npc.NPC");
                lookClose = Class.forName("net.citizensnpcs.trait.LookClose");
                skin = Class.forName("net.citizensnpcs.trait.SkinTrait");
                resolved = true;
            }
        } catch (Exception exception) {
            logger.warn("Citizens API classes are not available. NPC sync will be skipped.");
        }

        this.citizensApiClass = api;
        this.lookCloseTraitClass = lookClose;
        this.skinTraitClass = skin;
        this.available = resolved;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public Result<List<CitizensNpcHandle>> listManagedNpcs() {
        if (!available) {
            return Result.success(List.of());
        }
        try {
            Iterable<?> allNpcs = iterateRegistryNpcs();
            List<CitizensNpcHandle> managed = new ArrayList<>();
            for (Object npc : allNpcs) {
                String seedId = readMetadata(npc, META_SEED_ID);
                if (seedId.isBlank()) {
                    continue;
                }
                managed.add(toHandle(npc));
            }
            return Result.success(List.copyOf(managed));
        } catch (Exception exception) {
            return Result.failure(ErrorCode.UNKNOWN, "Failed to list managed Citizens NPCs", exception);
        }
    }

    @Override
    public Result<Optional<CitizensNpcHandle>> findNpcByEntity(Entity entity) {
        if (!available) {
            return Result.success(Optional.empty());
        }
        try {
            Object registry = getRegistry();
            Object npc = invokeOptional(registry, "getNPC", entity);
            if (npc == null) {
                return Result.success(Optional.empty());
            }
            return Result.success(Optional.of(toHandle(npc)));
        } catch (Exception exception) {
            return Result.failure(ErrorCode.UNKNOWN, "Failed to resolve Citizens NPC from clicked entity", exception);
        }
    }

    @Override
    public Result<CitizensNpcHandle> createNpc(CitizensNpcSeed seed) {
        if (!available) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Citizens is not available");
        }
        try {
            Object registry = getRegistry();
            Object npc = invoke(registry, "createNPC", seed.entityType(), seed.displayName());
            if (npc == null) {
                return Result.failure(ErrorCode.UNKNOWN, "Citizens createNPC returned null");
            }
            Result<Void> spawnResult = moveNpc(npc, seed);
            if (spawnResult.isFailure()) {
                return Result.failure(spawnResult.errorCode(), spawnResult.message(), spawnResult.cause());
            }
            return Result.success(toHandle(npc));
        } catch (Exception exception) {
            return Result.failure(ErrorCode.UNKNOWN, "Failed to create Citizens NPC: npc_seed_id=" + seed.npcSeedId(), exception);
        }
    }

    @Override
    public Result<CitizensNpcHandle> recreateNpc(CitizensNpcHandle existing, CitizensNpcSeed seed) {
        Result<Void> deleteResult = deleteNpc(existing);
        if (deleteResult.isFailure()) {
            return Result.failure(deleteResult.errorCode(), deleteResult.message(), deleteResult.cause());
        }
        return createNpc(seed);
    }

    @Override
    public Result<CitizensNpcHandle> updateNameAndLocation(CitizensNpcHandle existing, CitizensNpcSeed seed) {
        if (!available) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Citizens is not available");
        }
        try {
            Object npc = findNpcObject(existing.npcId());
            if (npc == null) {
                return createNpc(seed);
            }

            invoke(npc, "setName", seed.displayName());
            Result<Void> moveResult = moveNpc(npc, seed);
            if (moveResult.isFailure()) {
                return Result.failure(moveResult.errorCode(), moveResult.message(), moveResult.cause());
            }
            return Result.success(toHandle(npc));
        } catch (Exception exception) {
            return Result.failure(ErrorCode.UNKNOWN, "Failed to update Citizens NPC: npc_seed_id=" + seed.npcSeedId(), exception);
        }
    }

    @Override
    public Result<Void> applySkin(CitizensNpcHandle npc, CitizensNpcSeed seed) {
        if (!available || !seed.hasSkinValue()) {
            return Result.success();
        }
        try {
            Object npcObject = findNpcObject(npc.npcId());
            if (npcObject == null) {
                return Result.failure(ErrorCode.UNKNOWN, "Cannot find Citizens NPC to apply skin. npc_id=" + npc.npcId());
            }

            Object skinTrait = invoke(npcObject, "getOrAddTrait", skinTraitClass);
            if (skinTrait == null) {
                return Result.success();
            }

            String normalizedType = seed.normalizedSkinType();
            if ("name".equals(normalizedType)) {
                if (!invokeBestEffort(skinTrait, "setSkinName", seed.skinValue())) {
                    invokeBestEffort(skinTrait, "setSkinPersistent", seed.skinValue());
                }
            } else if ("value".equals(normalizedType)) {
                String[] parts = seed.skinValue().split("\\|");
                if (parts.length >= 3) {
                    if (!invokeBestEffort(skinTrait, "setSkinPersistent", parts[0], parts[1], parts[2])) {
                        invokeBestEffort(skinTrait, "setSkinPersistent", parts[0]);
                    }
                } else {
                    invokeBestEffort(skinTrait, "setSkinPersistent", seed.skinValue());
                }
            }

            setMetadata(npc, "empire.skin_type", seed.skinType());
            setMetadata(npc, "empire.skin_value", seed.skinValue());
            return Result.success();
        } catch (Exception exception) {
            return Result.failure(ErrorCode.UNKNOWN, "Failed to apply skin trait. npc_seed_id=" + seed.npcSeedId(), exception);
        }
    }

    @Override
    public Result<Void> setProtection(CitizensNpcHandle npc, boolean value) {
        if (!available) {
            return Result.success();
        }
        try {
            Object npcObject = findNpcObject(npc.npcId());
            if (npcObject == null) {
                return Result.failure(ErrorCode.UNKNOWN, "Cannot find Citizens NPC to set protection. npc_id=" + npc.npcId());
            }
            invoke(npcObject, "setProtected", value);
            return Result.success();
        } catch (Exception exception) {
            return Result.failure(ErrorCode.UNKNOWN, "Failed to set protection. npc_id=" + npc.npcId(), exception);
        }
    }

    @Override
    public Result<Void> setLookClose(CitizensNpcHandle npc, boolean value) {
        if (!available) {
            return Result.success();
        }
        try {
            Object npcObject = findNpcObject(npc.npcId());
            if (npcObject == null) {
                return Result.failure(ErrorCode.UNKNOWN, "Cannot find Citizens NPC to set lookclose. npc_id=" + npc.npcId());
            }
            Object trait = invoke(npcObject, "getOrAddTrait", lookCloseTraitClass);
            if (trait == null) {
                return Result.success();
            }
            if (!invokeBestEffort(trait, "lookClose", value)) {
                invokeBestEffort(trait, "setEnabled", value);
            }
            return Result.success();
        } catch (Exception exception) {
            return Result.failure(ErrorCode.UNKNOWN, "Failed to set lookclose trait. npc_id=" + npc.npcId(), exception);
        }
    }

    @Override
    public Result<Void> setMetadata(CitizensNpcHandle npc, String key, String value) {
        if (!available) {
            return Result.success();
        }
        try {
            Object npcObject = findNpcObject(npc.npcId());
            if (npcObject == null) {
                return Result.failure(ErrorCode.UNKNOWN, "Cannot find Citizens NPC to set metadata. npc_id=" + npc.npcId());
            }
            setMetadata(npcObject, key, value);
            return Result.success();
        } catch (Exception exception) {
            return Result.failure(ErrorCode.UNKNOWN, "Failed to set metadata: " + key + " for npc_id=" + npc.npcId(), exception);
        }
    }

    @Override
    public Result<String> getMetadata(CitizensNpcHandle npc, String key) {
        if (!available) {
            return Result.success("");
        }
        try {
            Object npcObject = findNpcObject(npc.npcId());
            if (npcObject == null) {
                return Result.success("");
            }
            return Result.success(readMetadata(npcObject, key));
        } catch (Exception exception) {
            return Result.failure(ErrorCode.UNKNOWN, "Failed to read metadata: " + key + " for npc_id=" + npc.npcId(), exception);
        }
    }

    @Override
    public Result<Void> deleteNpc(CitizensNpcHandle npc) {
        if (!available) {
            return Result.success();
        }
        try {
            Object npcObject = findNpcObject(npc.npcId());
            if (npcObject == null) {
                return Result.success();
            }
            if (!invokeBestEffort(npcObject, "destroy")) {
                Object registry = getRegistry();
                invokeBestEffort(registry, "deregister", npcObject);
                invokeBestEffort(registry, "remove", npcObject);
            }
            return Result.success();
        } catch (Exception exception) {
            return Result.failure(ErrorCode.UNKNOWN, "Failed to delete Citizens NPC. npc_id=" + npc.npcId(), exception);
        }
    }

    private Result<Void> moveNpc(Object npc, CitizensNpcSeed seed) {
        World world = plugin.getServer().getWorld(seed.worldName());
        if (world == null) {
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Unknown world_name in npc_spawn_seed.csv: world_name=" + seed.worldName()
            );
        }
        Location target = new Location(world, seed.x(), seed.y(), seed.z(), seed.yaw(), seed.pitch());
        try {
            Object entity = invokeOptional(npc, "getEntity");
            if (entity instanceof Entity bukkitEntity) {
                bukkitEntity.teleport(target, PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
            Object spawned = invokeOptional(npc, "isSpawned");
            boolean isSpawned = spawned instanceof Boolean b && b;
            if (!isSpawned) {
                Object result = invokeOptional(npc, "spawn", target);
                if (result instanceof Boolean b && !b) {
                    return Result.failure(ErrorCode.UNKNOWN, "Citizens spawn(target) returned false");
                }
                if (result == null) {
                    boolean fallbackOk = invokeBestEffort(npc, "spawn")
                            || invokeBestEffort(npc, "teleport", target, PlayerTeleportEvent.TeleportCause.PLUGIN)
                            || invokeBestEffort(npc, "setStoredLocation", target);
                    if (!fallbackOk) {
                        return Result.failure(ErrorCode.UNKNOWN, "Could not spawn or move Citizens NPC");
                    }
                }
            } else if (entity == null) {
                invokeBestEffort(npc, "teleport", target, PlayerTeleportEvent.TeleportCause.PLUGIN);
                invokeBestEffort(npc, "spawn", target);
            }
            return Result.success();
        } catch (Exception exception) {
            return Result.failure(ErrorCode.UNKNOWN, "Failed to move Citizens NPC: npc_seed_id=" + seed.npcSeedId(), exception);
        }
    }

    private CitizensNpcHandle toHandle(Object npc) throws Exception {
        int npcId = ((Number) invoke(npc, "getId")).intValue();
        String seedId = readMetadata(npc, META_SEED_ID);
        String name = String.valueOf(invoke(npc, "getName"));
        Object typeObject = invokeOptional(npc, "getEntityType");
        org.bukkit.entity.EntityType type = typeObject instanceof org.bukkit.entity.EntityType entityType
                ? entityType
                : org.bukkit.entity.EntityType.PLAYER;
        return new CitizensNpcHandle(npcId, seedId, type, name);
    }

    private Iterable<?> iterateRegistryNpcs() throws Exception {
        Object registry = getRegistry();
        if (registry instanceof Iterable<?> iterable) {
            return iterable;
        }
        Object sorted = invokeOptional(registry, "sorted");
        if (sorted instanceof Iterable<?> iterable) {
            return iterable;
        }
        Object values = invokeOptional(registry, "values");
        if (values instanceof Iterable<?> iterable) {
            return iterable;
        }
        return List.of();
    }

    private Object findNpcObject(int npcId) throws Exception {
        Object registry = getRegistry();
        Object byId = invokeOptional(registry, "getById", npcId);
        if (byId != null) {
            return byId;
        }
        for (Object npc : iterateRegistryNpcs()) {
            Object idObj = invokeOptional(npc, "getId");
            if (idObj instanceof Number number && number.intValue() == npcId) {
                return npc;
            }
        }
        return null;
    }

    private Object getRegistry() throws Exception {
        if (citizensApiClass == null) {
            throw new IllegalStateException("Citizens API class is not resolved");
        }
        return invokeStatic(citizensApiClass, "getNPCRegistry");
    }

    private String readMetadata(Object npc, String key) {
        try {
            Object data = invoke(npc, "data");
            Object value = invokeOptional(data, "get", key);
            if (value == null) {
                Object alt = invokeOptional(data, "get", key, "");
                value = alt;
            }
            String resolved = value == null ? "" : String.valueOf(value);
            if (!resolved.isBlank()) {
                return resolved;
            }

            if (key.startsWith("empire.")) {
                String shortKey = key.substring("empire.".length());
                Object shortValue = invokeOptional(data, "get", shortKey);
                if (shortValue == null) {
                    shortValue = invokeOptional(data, "get", shortKey, "");
                }
                if (shortValue != null && !String.valueOf(shortValue).isBlank()) {
                    return String.valueOf(shortValue);
                }

                Object empireRoot = invokeOptional(data, "get", "empire");
                if (empireRoot instanceof Map<?, ?> rootMap) {
                    Object nested = rootMap.get(shortKey);
                    if (nested != null) {
                        return String.valueOf(nested);
                    }
                }
            }
            return "";
        } catch (Exception exception) {
            logger.warn("Failed to read Citizens metadata key=" + key + " due to " + exception.getMessage());
            return "";
        }
    }

    private void setMetadata(Object npc, String key, String value) throws Exception {
        Object data = invoke(npc, "data");
        boolean wrote = invokeBestEffort(data, "setPersistent", key, value) || invokeBestEffort(data, "set", key, value);
        if (!wrote) {
            throw new IllegalStateException("Could not write Citizens metadata key=" + key);
        }

        if (key.startsWith("empire.")) {
            String shortKey = key.substring("empire.".length());
            invokeBestEffort(data, "setPersistent", shortKey, value);
            invokeBestEffort(data, "set", shortKey, value);
        }
    }

    private Object invokeStatic(Class<?> type, String methodName, Object... args) throws Exception {
        Method method = findMethod(type, methodName, args);
        return method.invoke(null, args);
    }

    private Object invoke(Object target, String methodName, Object... args) throws Exception {
        Method method = findMethod(target.getClass(), methodName, args);
        return method.invoke(target, args);
    }

    private Object invokeOptional(Object target, String methodName, Object... args) {
        try {
            return invoke(target, methodName, args);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean invokeBestEffort(Object target, String methodName, Object... args) {
        try {
            invoke(target, methodName, args);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private Method findMethod(Class<?> type, String methodName, Object... args) throws NoSuchMethodException {
        List<Method> candidates = new ArrayList<>();
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            if (method.getParameterCount() != args.length) {
                continue;
            }
            candidates.add(method);
        }
        for (Method candidate : candidates) {
            if (isAssignable(candidate.getParameterTypes(), args)) {
                candidate.setAccessible(true);
                return candidate;
            }
        }
        if (!candidates.isEmpty()) {
            Method fallback = candidates.get(0);
            fallback.setAccessible(true);
            return fallback;
        }
        throw new NoSuchMethodException("Method not found: " + type.getName() + "#" + methodName + "/" + args.length);
    }

    private boolean isAssignable(Class<?>[] parameterTypes, Object[] args) {
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = wrapPrimitive(parameterTypes[i]);
            Object argument = args[i];
            if (argument == null) {
                if (parameterType.isPrimitive()) {
                    return false;
                }
                continue;
            }
            if (!parameterType.isAssignableFrom(argument.getClass())) {
                return false;
            }
        }
        return true;
    }

    private Class<?> wrapPrimitive(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        Map<Class<?>, Class<?>> wrappers = new LinkedHashMap<>();
        wrappers.put(boolean.class, Boolean.class);
        wrappers.put(byte.class, Byte.class);
        wrappers.put(short.class, Short.class);
        wrappers.put(int.class, Integer.class);
        wrappers.put(long.class, Long.class);
        wrappers.put(float.class, Float.class);
        wrappers.put(double.class, Double.class);
        wrappers.put(char.class, Character.class);
        return wrappers.getOrDefault(type, type);
    }
}
