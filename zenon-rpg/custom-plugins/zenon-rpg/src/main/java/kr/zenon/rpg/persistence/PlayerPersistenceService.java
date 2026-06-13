package kr.zenon.rpg.persistence;

import kr.zenon.rpg.combat.weapon.WeaponType;
import kr.zenon.rpg.growth.GrowthStateStore;
import kr.zenon.rpg.growth.engine.EquipmentSlot;
import kr.zenon.rpg.growth.engine.ItemGrade;
import kr.zenon.rpg.growth.engine.PlayerEquipmentItem;
import kr.zenon.rpg.growth.engine.PlayerGrowthState;
import kr.zenon.rpg.growth.engine.PotentialGrade;
import kr.zenon.rpg.growth.engine.PotentialLine;
import kr.zenon.rpg.growth.engine.PotentialProfile;
import kr.zenon.rpg.growth.island.IslandRank;
import kr.zenon.rpg.growth.island.IslandStorage;
import kr.zenon.rpg.growth.island.IslandStorageStore;
import kr.zenon.rpg.growth.island.IslandTerritoryState;
import kr.zenon.rpg.growth.island.IslandTerritoryStateStore;
import kr.zenon.rpg.storage.PlayerDataManager;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
    /** 스택→인스턴스 마이그레이션(P6) 시 세부스탯 롤용. null이면 마이그레이션 생략. */
    private final kr.zenon.rpg.growth.engine.TraceSubstatRoller traceSubstatRoller;

    public PlayerPersistenceService(PlayerDataRepository repo,
                                    PlayerDataManager playerDataManager,
                                    GrowthStateStore growthStore,
                                    IslandTerritoryStateStore territoryStore,
                                    IslandStorageStore storageStore,
                                    Logger logger,
                                    kr.zenon.rpg.growth.engine.TraceSubstatRoller traceSubstatRoller) {
        this.repo              = repo;
        this.playerDataManager = playerDataManager;
        this.growthStore       = growthStore;
        this.territoryStore    = territoryStore;
        this.storageStore      = storageStore;
        this.logger            = logger;
        this.traceSubstatRoller = traceSubstatRoller;
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
                    current.weaponType(), current.classId(), current.classEngravingId(), current.classEngravingByClass(),
                    wallet, current.equippedSlots(), current.inventory(), current.equippedRunes(),
                    current.commonEngravings(), current.territory(), current.storage(), current.customItems(),
                    current.workshopJobs(),
                    current.playerLevel(), current.unspentPts(), current.critPts(), current.specPts(),
                    current.endurPts(), current.currentExp(), current.ceilingCounters(),
                    current.ilWarningCount(), current.mobIlHitCount(), current.catalystBonusPct(),
                    Map.of(), current.traceInstances());
            logger.info("[Migration] " + uuid + " v" + version + " → v2");
        }

        if (current.schemaVersion() < 3) {
            Map<String, String> slots = migrateEquippedSlotsV2(current.equippedSlots());
            current = new PlayerSaveData(3,
                    current.weaponType(), current.classId(), current.classEngravingId(), current.classEngravingByClass(),
                    current.wallet(), slots, current.inventory(), current.equippedRunes(),
                    current.commonEngravings(), current.territory(), current.storage(), current.customItems(),
                    current.workshopJobs(),
                    current.playerLevel(), current.unspentPts(), current.critPts(), current.specPts(),
                    current.endurPts(), current.currentExp(), current.ceilingCounters(),
                    current.ilWarningCount(), current.mobIlHitCount(), current.catalystBonusPct(),
                    current.cosmeticMaterials() != null ? current.cosmeticMaterials() : Map.of(), current.traceInstances());
            logger.info("[Migration] " + uuid + " v2 → v3");
        }

        if (current.schemaVersion() < 4) {
            current = new PlayerSaveData(4,
                    current.weaponType(), current.classId(), current.classEngravingId(), current.classEngravingByClass(),
                    current.wallet(), current.equippedSlots(), current.inventory(), current.equippedRunes(),
                    current.commonEngravings(), current.territory(), current.storage(), current.customItems(),
                    current.workshopJobs() != null ? current.workshopJobs() : List.of(),
                    current.playerLevel(), current.unspentPts(), current.critPts(), current.specPts(),
                    current.endurPts(), current.currentExp(), current.ceilingCounters(),
                    current.ilWarningCount(), current.mobIlHitCount(), current.catalystBonusPct(),
                    current.cosmeticMaterials() != null ? current.cosmeticMaterials() : Map.of(), current.traceInstances());
            logger.info("[Migration] " + uuid + " v3 → v4");
        }

        if (current.schemaVersion() < 5) {
            current = new PlayerSaveData(5,
                    current.weaponType(), current.classId(), current.classEngravingId(), current.classEngravingByClass(),
                    current.wallet(), current.equippedSlots(), current.inventory(), current.equippedRunes(),
                    current.commonEngravings(), current.territory(), current.storage(), current.customItems(),
                    current.workshopJobs() != null ? current.workshopJobs() : List.of(),
                    current.playerLevel(), current.unspentPts(), current.critPts(), current.specPts(),
                    current.endurPts(), current.currentExp(), current.ceilingCounters(),
                    current.ilWarningCount(), current.mobIlHitCount(), current.catalystBonusPct(),
                    Map.of(), current.traceInstances());
            logger.info("[Migration] " + uuid + " v4 → v5");
        }

        if (current.schemaVersion() < 6) {
            // 무기별 독립 각인(DL-110) — 기존 단일 classEngravingId를 현재 classId(무기) 키로 이관.
            Map<String, String> byClass = new LinkedHashMap<>();
            String legacyId = current.classEngravingId();
            String classKey = current.classId();
            if (legacyId != null && !legacyId.isBlank() && classKey != null && !classKey.isBlank()) {
                byClass.put(classKey.trim().toLowerCase(Locale.ROOT), legacyId.trim().toLowerCase(Locale.ROOT));
            }
            current = new PlayerSaveData(6,
                    current.weaponType(), current.classId(), current.classEngravingId(), byClass,
                    current.wallet(), current.equippedSlots(), current.inventory(), current.equippedRunes(),
                    current.commonEngravings(), current.territory(), current.storage(), current.customItems(),
                    current.workshopJobs() != null ? current.workshopJobs() : List.of(),
                    current.playerLevel(), current.unspentPts(), current.critPts(), current.specPts(),
                    current.endurPts(), current.currentExp(), current.ceilingCounters(),
                    current.ilWarningCount(), current.mobIlHitCount(), current.catalystBonusPct(),
                    current.cosmeticMaterials() != null ? current.cosmeticMaterials() : Map.of(), current.traceInstances());
            logger.info("[Migration] " + uuid + " v5 → v6 (classEngraving → byClass)");
        }

        if (current.schemaVersion() < 7) {
            // 흔적 인스턴스화(DL-129 추가#38) — v7 신규 필드. 구 세이브엔 인스턴스가 없으므로 빈 리스트로 초기화.
            // 기존 스택형 흔적(customItems의 equip_trace_*)의 인스턴스 변환은 P6 마이그레이션에서 별도 처리.
            current = new PlayerSaveData(7,
                    current.weaponType(), current.classId(), current.classEngravingId(), current.classEngravingByClass(),
                    current.wallet(), current.equippedSlots(), current.inventory(), current.equippedRunes(),
                    current.commonEngravings(), current.territory(), current.storage(), current.customItems(),
                    current.workshopJobs() != null ? current.workshopJobs() : List.of(),
                    current.playerLevel(), current.unspentPts(), current.critPts(), current.specPts(),
                    current.endurPts(), current.currentExp(), current.ceilingCounters(),
                    current.ilWarningCount(), current.mobIlHitCount(), current.catalystBonusPct(),
                    current.cosmeticMaterials() != null ? current.cosmeticMaterials() : Map.of(),
                    current.traceInstances() != null ? current.traceInstances() : List.of());
            logger.info("[Migration] " + uuid + " v6 → v7 (trace instances)");
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

        // 지갑 — 복원은 restoreCurrency로 경제 흐름 발생 제외 (DL-080)
        if (data.wallet() != null) {
            data.wallet().forEach((code, amount) -> state.restoreCurrency(code, amount));
        }

        // 각인 — 무기별 독립(DL-110). migrate()가 v6로 byClass를 채우지만, 안전망으로 legacy 단일값도 폴백.
        if (data.classEngravingByClass() != null && !data.classEngravingByClass().isEmpty()) {
            data.classEngravingByClass().forEach(state::restoreClassEngraving);
        } else if (data.classEngravingId() != null && !data.classEngravingId().isBlank()) {
            state.setClassEngravingId(data.classEngravingId());
        }
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
                            toPotentialLines(itemDto.substats()),
                            itemDto.pityCount()
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

        // 치장 재질 (schemaVersion 5+) — 키 형식: "HELMET", "weapon_SWORD" 등
        if (data.cosmeticMaterials() != null) {
            data.cosmeticMaterials().forEach((key, mat) -> state.setCosmeticMaterial(key, mat));
        }
    }

    private void applyTerritory(UUID uuid, String playerName, PlayerSaveData data) {
        if (data.territory() == null) return;
        PlayerSaveData.TerritorySaveData t = data.territory();
        IslandTerritoryState state = territoryStore.getOrCreate(uuid, playerName);
        // ownerName 슬롯은 실제로 영지명(islandName)을 담는다(레거시 필드명) — 영지명 복원.
        if (t.ownerName() != null && !t.ownerName().isBlank()) {
            state.setIslandName(t.ownerName());
        }

        try {
            state.setRank(IslandRank.valueOf(t.rankName()));
        } catch (IllegalArgumentException ignored) {}

        state.setConvenienceUnlocks(t.convenienceUnlocks());
        state.setTimeState(t.timeState());       // 영지 시간 고정 복원 (DL-129#33)
        state.setWeatherState(t.weatherState());
        state.setStorageCount(t.storageCount());
        state.setWorkshopMachineCount(t.workshopMachineCount());
        state.setLastProductionAt(t.lastProductionAt());
        // 시설 타임스탬프 (DL-129 추가#11): 신 세이브=리스트 그대로, 구 세이브=reaper/minerCount로 마이그레이션.
        long seed = t.lastProductionAt() > 0 ? t.lastProductionAt() : System.currentTimeMillis();
        if (t.herbProducedAt() != null) {
            state.setHerbProducedAt(t.herbProducedAt());
        } else {
            java.util.List<Long> herb = new java.util.ArrayList<>();
            for (int i = 0; i < t.reaperCount(); i++) herb.add(seed);
            state.setHerbProducedAt(herb);
        }
        if (t.oreProducedAt() != null) {
            state.setOreProducedAt(t.oreProducedAt());
        } else {
            java.util.List<Long> ore = new java.util.ArrayList<>();
            for (int i = 0; i < t.minerCount(); i++) ore.add(seed);
            state.setOreProducedAt(ore);
        }

        // 커스텀 아이템 (큐브 파편, 흔적 등)
        if (data.customItems() != null) {
            data.customItems().forEach((id, qty) -> state.addCustomItem(id, qty));
        }

        // 공방 대기열
        if (data.workshopJobs() != null) {
            data.workshopJobs().forEach(dto ->
                    state.addWorkshopJob(new kr.zenon.rpg.growth.island.WorkshopJob(
                            dto.recipeId(), dto.startedAt(), dto.completeAt())));
        }

        // 장비 흔적 인스턴스 (DL-129 추가#38) — setTraceInstances로 일괄 교체(중복 복원 방지).
        if (data.traceInstances() != null) {
            java.util.List<kr.zenon.rpg.growth.engine.TraceInstance> traces = data.traceInstances().stream()
                    .map(dto -> new kr.zenon.rpg.growth.engine.TraceInstance(
                            dto.instanceId(),
                            parseEnum(ItemGrade.class, dto.grade(), ItemGrade.COMMON),
                            toPotentialLines(dto.substats())))
                    .toList();
            state.setTraceInstances(traces);
        }

        // P6 마이그레이션 — 구 스택형 흔적(equip_trace_*)을 인스턴스로 일회성 변환 (DL-129 추가#38).
        // 변환된 스택은 customItems에서 제거 → 다음 로드 시 재변환 없음. 인스턴스는 다음 save에 영속.
        migrateStackTracesToInstances(state, uuid);
    }

    /** customItems의 equip_trace_* 스택을 흔적 인스턴스로 변환(등급별 세부스탯 롤). 변환 후 스택 제거. */
    private void migrateStackTracesToInstances(IslandTerritoryState state, UUID uuid) {
        if (traceSubstatRoller == null) return;
        int converted = 0;
        for (Map.Entry<String, Long> e : new LinkedHashMap<>(state.customItemsSnapshot()).entrySet()) {
            String id = e.getKey();
            if (!kr.zenon.rpg.growth.engine.SuccessionService.isKnownTrace(id)) continue;
            long count = e.getValue();
            if (count <= 0) continue;
            ItemGrade grade = kr.zenon.rpg.growth.engine.SuccessionService.traceGrade(id);
            for (long i = 0; i < count; i++) {
                state.addTraceInstance(new kr.zenon.rpg.growth.engine.TraceInstance(
                        "trace_" + java.util.UUID.randomUUID(),
                        grade,
                        traceSubstatRoller.roll(grade)));
                converted++;
            }
            state.withdrawCustomItem(id, count);
        }
        if (converted > 0) {
            logger.info("[Migration] " + uuid + " 스택 흔적 " + converted + "개 → 인스턴스 변환 (DL-129 추가#38)");
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
                growth != null ? growth.classEngravingId() : "", // legacy 단일(현재 무기) — 하위호환 표시용
                growth != null ? new LinkedHashMap<>(growth.classEngravingByClassSnapshot()) : Map.of(),
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
                growth != null ? growth.catalystBonusPct()  : 0,
                toCosmeticMaterialsDto(growth),
                toTraceInstancesDto(territory)
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
                    item.grade().name(), potential, substats, item.pityCount()
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
        if (t == null) return new PlayerSaveData.TerritorySaveData("", IslandRank.FRONTIER.name(), 0, 0, 0, 0, 0L);
        return new PlayerSaveData.TerritorySaveData(
                t.islandName(), t.rank().name(), // 첫 인자(ownerName 슬롯)에 영지명 저장 — applyTerritory가 setIslandName으로 복원
                t.convenienceUnlocks(), t.reaperCount(), t.storageCount(), t.minerCount(),
                t.lastProductionAt(), t.workshopMachineCount(),
                new java.util.ArrayList<>(t.herbProducedAt()), new java.util.ArrayList<>(t.oreProducedAt()),
                t.timeState(), t.weatherState() // 영지 시간/날씨 고정 영속 (DL-129#33)
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

    private List<PlayerSaveData.TraceInstanceSaveData> toTraceInstancesDto(IslandTerritoryState t) {
        if (t == null) return List.of();
        return t.traceInstancesSnapshot().stream()
                .map(tr -> new PlayerSaveData.TraceInstanceSaveData(
                        tr.instanceId(),
                        tr.grade().name(),
                        tr.substats().stream()
                                .map(l -> new PlayerSaveData.PotentialLineSaveData(l.lineNo(), l.grade().name(), l.optionCode(), l.value()))
                                .toList()))
                .toList();
    }

    private List<PlayerSaveData.WorkshopJobSaveData> toWorkshopJobsDto(IslandTerritoryState t) {
        if (t == null) return List.of();
        return t.workshopJobsSnapshot().stream()
                .map(j -> new PlayerSaveData.WorkshopJobSaveData(j.recipeId(), j.startedAt(), j.completeAt()))
                .toList();
    }

    private Map<String, String> toCosmeticMaterialsDto(PlayerGrowthState state) {
        if (state == null) return Map.of();
        return new LinkedHashMap<>(state.cosmeticMaterialsSnapshot());
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
