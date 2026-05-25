package com.poro.empire.persistence;

import com.poro.empire.combat.weapon.WeaponType;
import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.engine.EquipmentSlot;
import com.poro.empire.growth.engine.ItemGrade;
import com.poro.empire.growth.engine.PlayerEquipmentItem;
import com.poro.empire.growth.engine.PlayerGrowthState;
import com.poro.empire.growth.engine.PotentialGrade;
import com.poro.empire.growth.engine.PotentialLine;
import com.poro.empire.growth.engine.PotentialProfile;
import com.poro.empire.growth.island.IslandRank;
import com.poro.empire.growth.island.IslandStorage;
import com.poro.empire.growth.island.IslandStorageStore;
import com.poro.empire.growth.island.IslandTerritoryState;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import com.poro.empire.storage.PlayerDataManager;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 모든 플레이어 스토어를 조율해 JSON 저장/로드를 처리한다.
 */
public final class PlayerPersistenceService {

    private final PlayerDataRepository     repo;
    private final PlayerDataManager        playerDataManager;
    private final GrowthStateStore         growthStore;
    private final IslandTerritoryStateStore territoryStore;
    private final IslandStorageStore        storageStore;
    private final Logger logger;

    public PlayerPersistenceService(PlayerDataRepository repo,
                                    PlayerDataManager playerDataManager,
                                    GrowthStateStore growthStore,
                                    IslandTerritoryStateStore territoryStore,
                                    IslandStorageStore storageStore,
                                    Logger logger) {
        this.repo              = repo;
        this.playerDataManager = playerDataManager;
        this.growthStore       = growthStore;
        this.territoryStore    = territoryStore;
        this.storageStore      = storageStore;
        this.logger            = logger;
    }

    // ─── 로드 ──────────────────────────────────────────────────────

    public void load(UUID uuid, String playerName) {
        repo.load(uuid).ifPresent(rawData -> {
            PlayerSaveData data = migrate(uuid, rawData);
            if (data.schemaVersion() != rawData.schemaVersion()) {
                repo.save(uuid, data);
            }
            applyWeaponType(uuid, data);
            applyGrowthState(uuid, data);
            applyTerritory(uuid, playerName, data);
            applyStorage(uuid, data);
        });
    }

    private PlayerSaveData migrate(UUID uuid, PlayerSaveData raw) {
        int version = raw.schemaVersion();
        if (version >= PlayerSaveData.CURRENT_VERSION) return raw;

        PlayerSaveData current = raw;

        if (version < 2) {
            Map<String, Long> wallet = migrateWalletV1(current.wallet());
            current = new PlayerSaveData(2,
                    current.weaponType(), current.classId(), current.classEngravingId(),
                    wallet, current.equippedSlots(), current.inventory(), current.equippedRunes(),
                    current.commonEngravings(), current.territory(), current.storage(), current.customItems(),
                    current.workshopJobs(),
                    current.playerLevel(), current.unspentPts(), current.critPts(), current.specPts(),
                    current.endurPts(), current.currentExp(), current.ceilingCounters(),
                    current.ilWarningCount(), current.mobIlHitCount(), current.catalystBonusPct());
            logger.info("[Migration] " + uuid + " v" + version + " → v2");
        }

        if (current.schemaVersion() < 3) {
            Map<String, String> slots = migrateEquippedSlotsV2(current.equippedSlots());
            current = new PlayerSaveData(3,
                    current.weaponType(), current.classId(), current.classEngravingId(),
                    current.wallet(), slots, current.inventory(), current.equippedRunes(),
                    current.commonEngravings(), current.territory(), current.storage(), current.customItems(),
                    current.workshopJobs(),
                    current.playerLevel(), current.unspentPts(), current.critPts(), current.specPts(),
                    current.endurPts(), current.currentExp(), current.ceilingCounters(),
                    current.ilWarningCount(), current.mobIlHitCount(), current.catalystBonusPct());
            logger.info("[Migration] " + uuid + " v2 → v3");
        }

        if (current.schemaVersion() < 4) {
            current = new PlayerSaveData(4,
                    current.weaponType(), current.classId(), current.classEngravingId(),
                    current.wallet(), current.equippedSlots(), current.inventory(), current.equippedRunes(),
                    current.commonEngravings(), current.territory(), current.storage(), current.customItems(),
                    current.workshopJobs() != null ? current.workshopJobs() : List.of(),
                    current.playerLevel(), current.unspentPts(), current.critPts(), current.specPts(),
                    current.endurPts(), current.currentExp(), current.ceilingCounters(),
                    current.ilWarningCount(), current.mobIlHitCount(), current.catalystBonusPct());
            logger.info("[Migration] " + uuid + " v3 → v4");
        }

        return current;
    }

    private Map<String, Long> migrateWalletV1(Map<String, Long> wallet) {
        if (wallet == null) return Map.of();
        Map<String, Long> result = new LinkedHashMap<>(wallet);
        Long stones = result.remove("enhancement_stone");
        if (stones != null) {
            result.merge("mat_stone_enhance", stones, Long::sum);
        }
        return result;
    }

    private Map<String, String> migrateEquippedSlotsV2(Map<String, String> slots) {
        if (slots == null) return Map.of();
        Map<String, String> result = new LinkedHashMap<>();
        slots.forEach((key, value) -> {
            try {
                result.put(EquipmentSlot.from(key).name(), value);
            } catch (IllegalArgumentException ignored) {
                result.put(key, value);
            }
        });
        return result;
    }

    private void applyWeaponType(UUID uuid, PlayerSaveData data) {
        if (data.weaponType() != null) {
            try {
                playerDataManager.setWeaponType(uuid, WeaponType.valueOf(data.weaponType()));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void applyGrowthState(UUID uuid, PlayerSaveData data) {
        String classId = data.classId() != null ? data.classId() : "";
        PlayerGrowthState state = growthStore.getOrCreate(uuid, classId);

        // 지갑
        if (data.wallet() != null) {
            data.wallet().forEach((code, amount) -> state.addCurrency(code, amount));
        }

        // 각인
        if (data.classEngravingId() != null) state.setClassEngravingId(data.classEngravingId());
        if (data.commonEngravings() != null) {
            data.commonEngravings().forEach((slotStr, eg) -> {
                try {
                    int slot = Integer.parseInt(slotStr);
                    state.equipCommonEngraving(slot,
                            new PlayerGrowthState.EquippedCommonEngraving(eg.engravingId(), eg.level()));
                } catch (NumberFormatException ignored) {}
            });
        }

        // 룬
        if (data.equippedRunes() != null) {
            data.equippedRunes().forEach((slotStr, runeId) -> {
                try {
                    state.equipRune(Integer.parseInt(slotStr), runeId);
                } catch (NumberFormatException ignored) {}
            });
        }

        // 인벤토리
        if (data.inventory() != null) {
            data.inventory().forEach(itemDto -> {
                try {
                    PlayerEquipmentItem item = PlayerEquipmentItem.restore(
                            itemDto.instanceId(),
                            itemDto.itemId(),
                            itemDto.enhanceLevel(),
                            parseEnum(ItemGrade.class, itemDto.grade(), ItemGrade.COMMON),
                            toPotentialProfile(itemDto.potential()),
                            toPotentialLines(itemDto.substats())
                    );
                    state.addInventoryItem(item);
                } catch (Exception e) {
                    logger.warning("[Persistence] 아이템 복원 실패: " + e.getMessage());
                }
            });
        }

        // 장착 슬롯
        if (data.equippedSlots() != null) {
            data.equippedSlots().forEach((slotName, instanceId) -> {
                try {
                    state.equipItem(EquipmentSlot.from(slotName), instanceId);
                } catch (IllegalArgumentException ignored) {}
            });
        }

        // 스탯 배분 포인트 (schemaVersion 2+; 이전 파일은 0으로 역직렬화되므로 기본값 유지)
        if (data.playerLevel() > 0) state.setPlayerLevel(data.playerLevel());
        state.setUnspentPts(data.unspentPts());
        state.setCritPts(data.critPts());
        state.setSpecPts(data.specPts());
        state.setEndurPts(data.endurPts());
        state.setCurrentExp(data.currentExp());

        // 강화 천장 카운터
        if (data.ceilingCounters() != null) {
            data.ceilingCounters().forEach((k, v) -> {
                if (v != null && v > 0) state.setCeilingCounter(k, v);
            });
        }

        // IL 경고 카운터
        if (data.ilWarningCount() > 0) state.setIlWarningCount(data.ilWarningCount());
        if (data.mobIlHitCount()  > 0) state.setMobIlHitCount(data.mobIlHitCount());

        // 강화 촉진제 보너스 (schemaVersion 3+; 이전 파일은 0으로 역직렬화)
        if (data.catalystBonusPct() > 0) state.setCatalystBonusPct(data.catalystBonusPct());
    }

    private void applyTerritory(UUID uuid, String playerName, PlayerSaveData data) {
        if (data.territory() == null) return;
        PlayerSaveData.TerritorySaveData t = data.territory();
        IslandTerritoryState state = territoryStore.getOrCreate(uuid, playerName);
        if (t.ownerName() != null && !t.ownerName().isBlank()) {
            state.setIslandName(t.ownerName());
        }

        try {
            state.setRank(IslandRank.valueOf(t.rankName()));
        } catch (IllegalArgumentException ignored) {}

        state.setConvenienceUnlocks(t.convenienceUnlocks());
        state.setReaperCount(t.reaperCount());
        state.setStorageCount(t.storageCount());

        // 커스텀 아이템 (큐브 파편, 흔적 등)
        if (data.customItems() != null) {
            data.customItems().forEach((id, qty) -> state.addCustomItem(id, qty));
        }

        // 공방 대기열
        if (data.workshopJobs() != null) {
            data.workshopJobs().forEach(dto ->
                    state.addWorkshopJob(new com.poro.empire.growth.island.WorkshopJob(
                            dto.recipeId(), dto.startedAt(), dto.completeAt())));
        }
    }

    private void applyStorage(UUID uuid, PlayerSaveData data) {
        if (data.storage() == null) return;
        IslandStorage storage = storageStore.getOrCreate(uuid);
        data.storage().forEach((matName, qty) -> {
            try {
                storage.add(Material.valueOf(matName), qty);
            } catch (IllegalArgumentException ignored) {}
        });
    }

    // ─── 저장 ──────────────────────────────────────────────────────

    public void save(UUID uuid) {
        String classId = "";
        WeaponType wt = playerDataManager.getWeaponType(uuid);
        PlayerGrowthState growth = growthStore.get(uuid).orElse(null);
        if (growth != null) classId = growth.classId();

        IslandTerritoryState territory = territoryStore.get(uuid).orElse(null);
        IslandStorage storage = storageStore.get(uuid).orElse(null);

        PlayerSaveData data = new PlayerSaveData(
                PlayerSaveData.CURRENT_VERSION,
                wt != null ? wt.name() : WeaponType.NONE.name(),
                classId,
                growth != null ? growth.classEngravingId() : "",
                growth != null ? new LinkedHashMap<>(growth.walletSnapshot()) : Map.of(),
                toEquippedSlotsDto(growth),
                toInventoryDto(growth),
                toRunesDto(growth),
                toEngravingsDto(growth),
                toTerritoryDto(territory),
                toStorageDto(storage),
                toCustomItemsDto(territory),
                toWorkshopJobsDto(territory),
                growth != null ? growth.playerLevel()  : 1,
                growth != null ? growth.unspentPts()   : 0,
                growth != null ? growth.critPts()      : 0,
                growth != null ? growth.specPts()      : 0,
                growth != null ? growth.endurPts()     : 0,
                growth != null ? growth.currentExp()   : 0L,
                growth != null ? new LinkedHashMap<>(growth.ceilingCountersSnapshot()) : Map.of(),
                growth != null ? growth.ilWarningCount()    : 0,
                growth != null ? growth.mobIlHitCount()     : 0,
                growth != null ? growth.catalystBonusPct()  : 0
        );
        repo.save(uuid, data);
    }

    public void saveAll(Iterable<UUID> uuids) {
        uuids.forEach(this::save);
    }

    // ─── 직렬화 헬퍼 ──────────────────────────────────────────────

    private Map<String, String> toEquippedSlotsDto(PlayerGrowthState state) {
        if (state == null) return Map.of();
        Map<String, String> map = new LinkedHashMap<>();
        state.equippedItems().forEach((slot, id) -> map.put(slot.name(), id));
        return map;
    }

    private List<PlayerSaveData.ItemSaveData> toInventoryDto(PlayerGrowthState state) {
        if (state == null) return List.of();
        List<PlayerSaveData.ItemSaveData> list = new ArrayList<>();
        state.inventorySnapshot().values().forEach(item -> {
            PlayerSaveData.PotentialSaveData potential = null;
            if (item.potentialProfile() != null) {
                PotentialProfile pp = item.potentialProfile();
                potential = new PlayerSaveData.PotentialSaveData(
                        pp.grade().name(),
                        pp.lines().stream()
                                .map(l -> new PlayerSaveData.PotentialLineSaveData(l.lineNo(), l.grade().name(), l.optionCode(), l.value()))
                                .toList()
                );
            }
            List<PlayerSaveData.PotentialLineSaveData> substats = item.substatLines().stream()
                    .map(l -> new PlayerSaveData.PotentialLineSaveData(l.lineNo(), l.grade().name(), l.optionCode(), l.value()))
                    .toList();
            list.add(new PlayerSaveData.ItemSaveData(
                    item.itemInstanceId(), item.itemId(), item.enhanceLevel(),
                    item.grade().name(), potential, substats
            ));
        });
        return list;
    }

    private Map<String, String> toRunesDto(PlayerGrowthState state) {
        if (state == null) return Map.of();
        Map<String, String> map = new LinkedHashMap<>();
        state.equippedRunes().forEach((slot, id) -> map.put(String.valueOf(slot), id));
        return map;
    }

    private Map<String, PlayerSaveData.CommonEngravingSaveData> toEngravingsDto(PlayerGrowthState state) {
        if (state == null) return Map.of();
        Map<String, PlayerSaveData.CommonEngravingSaveData> map = new LinkedHashMap<>();
        state.commonEngravings().forEach((slot, eg) ->
                map.put(String.valueOf(slot), new PlayerSaveData.CommonEngravingSaveData(eg.engravingId(), eg.level())));
        return map;
    }

    private PlayerSaveData.TerritorySaveData toTerritoryDto(IslandTerritoryState t) {
        if (t == null) return new PlayerSaveData.TerritorySaveData("", IslandRank.FRONTIER.name(), 0, 0, 0);
        return new PlayerSaveData.TerritorySaveData(
                t.islandName(), t.rank().name(),
                t.convenienceUnlocks(), t.reaperCount(), t.storageCount()
        );
    }

    private Map<String, Long> toStorageDto(IslandStorage storage) {
        if (storage == null) return Map.of();
        Map<String, Long> map = new LinkedHashMap<>();
        storage.materialList().forEach(mat -> {
            long qty = storage.getAmount(mat);
            if (qty > 0) map.put(mat.name(), qty);
        });
        return map;
    }

    private Map<String, Long> toCustomItemsDto(IslandTerritoryState t) {
        if (t == null) return Map.of();
        return new LinkedHashMap<>(t.customItemsSnapshot());
    }

    private List<PlayerSaveData.WorkshopJobSaveData> toWorkshopJobsDto(IslandTerritoryState t) {
        if (t == null) return List.of();
        return t.workshopJobsSnapshot().stream()
                .map(j -> new PlayerSaveData.WorkshopJobSaveData(j.recipeId(), j.startedAt(), j.completeAt()))
                .toList();
    }

    // ─── 역직렬화 헬퍼 ─────────────────────────────────────────────

    private PotentialProfile toPotentialProfile(PlayerSaveData.PotentialSaveData dto) {
        if (dto == null) return null;
        return new PotentialProfile(
                parseEnum(PotentialGrade.class, dto.grade(), PotentialGrade.COMMON),
                toPotentialLines(dto.lines())
        );
    }

    private List<PotentialLine> toPotentialLines(List<PlayerSaveData.PotentialLineSaveData> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream()
                .map(d -> new PotentialLine(d.lineNo(),
                        parseEnum(PotentialGrade.class, d.grade(), PotentialGrade.COMMON),
                        d.optionCode(), d.value()))
                .toList();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> cls, String name, E fallback) {
        if (name == null) return fallback;
        try {
            return Enum.valueOf(cls, name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
