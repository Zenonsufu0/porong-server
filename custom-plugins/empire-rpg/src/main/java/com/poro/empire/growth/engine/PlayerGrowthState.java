package com.poro.empire.growth.engine;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class PlayerGrowthState {
    private final String userId;
    private final String classId;
    private final Map<String, PlayerEquipmentItem> inventory = new LinkedHashMap<>();
    private final Map<EquipmentSlot, String> equippedItemBySlot = new LinkedHashMap<>();
    private final Map<String, Long> wallet = new LinkedHashMap<>();
    private CurrencyFlowListener flowListener; // 경제 흐름(DL-080), nullable
    private final Map<Integer, String> equippedRunes = new LinkedHashMap<>();
    private final Map<Integer, EquippedCommonEngraving> commonEngravings = new LinkedHashMap<>();

    private final Map<String, Integer> ceilingCounters = new LinkedHashMap<>();

    private int ilWarningCount  = 0;
    private int mobIlHitCount   = 0;
    private int catalystBonusPct = 0;

    private String classEngravingId = "";

    private int  playerLevel = 1;
    private int  unspentPts  = 0;
    private int  critPts     = 0;
    private int  specPts     = 0;
    private int  endurPts    = 0;
    private long currentExp  = 0L;

    public PlayerGrowthState(String userId, String classId) {
        this.userId = normalize(userId);
        this.classId = normalize(classId);
    }

    public String userId() {
        return userId;
    }

    public String classId() {
        return classId;
    }

    public void addInventoryItem(PlayerEquipmentItem item) {
        inventory.put(item.itemInstanceId(), item);
    }

    public void removeInventoryItem(String instanceId) {
        inventory.remove(normalize(instanceId));
    }

    public Optional<PlayerEquipmentItem> inventoryItem(String itemInstanceId) {
        return Optional.ofNullable(inventory.get(normalize(itemInstanceId)));
    }

    public Map<String, PlayerEquipmentItem> inventorySnapshot() {
        return Map.copyOf(new LinkedHashMap<>(inventory));
    }

    public void equipItem(EquipmentSlot slot, String itemInstanceId) {
        equippedItemBySlot.put(slot, normalize(itemInstanceId));
    }

    public Optional<String> equippedItemInstanceId(EquipmentSlot slot) {
        return Optional.ofNullable(equippedItemBySlot.get(slot));
    }

    public Optional<PlayerEquipmentItem> equippedItem(EquipmentSlot slot) {
        return equippedItemInstanceId(slot).flatMap(this::inventoryItem);
    }

    public void unequipItem(EquipmentSlot slot) {
        equippedItemBySlot.remove(slot);
    }

    public Map<EquipmentSlot, String> equippedItems() {
        return Map.copyOf(new LinkedHashMap<>(equippedItemBySlot));
    }

    public long currency(String code) {
        return wallet.getOrDefault(normalize(code), 0L);
    }

    public void addCurrency(String code, long amount) {
        if (amount <= 0) {
            return;
        }
        String normalized = normalize(code);
        wallet.merge(normalized, amount, Long::sum);
        fireFlow(true, normalized, amount);
    }

    public boolean consumeCurrency(String code, long amount) {
        if (amount <= 0) {
            return true;
        }
        String normalized = normalize(code);
        long current = wallet.getOrDefault(normalized, 0L);
        if (current < amount) {
            return false;
        }
        wallet.put(normalized, current - amount);
        fireFlow(false, normalized, amount);
        return true;
    }

    /**
     * 저장 데이터 복원용 통화 적립 — 경제 흐름(CurrencyFlowListener)을 발생시키지 않는다.
     * 로그인 시 지갑 복원이 inflow로 오염되는 것을 방지 (DL-080).
     */
    public void restoreCurrency(String code, long amount) {
        if (amount <= 0) {
            return;
        }
        wallet.merge(normalize(code), amount, Long::sum);
    }

    private void fireFlow(boolean inflow, String normalizedCode, long amount) {
        if (flowListener != null) {
            flowListener.onFlow(userId, inflow, normalizedCode, amount);
        }
    }

    /** 경제 흐름 리스너 주입 (GrowthStateStore가 상태 생성 시 설정). */
    public void setFlowListener(CurrencyFlowListener flowListener) {
        this.flowListener = flowListener;
    }

    public Map<String, Long> walletSnapshot() {
        return Map.copyOf(new LinkedHashMap<>(wallet));
    }

    public int playerLevel()  { return playerLevel; }
    public int unspentPts()   { return unspentPts; }
    public int critPts()      { return critPts; }
    public int specPts()      { return specPts; }
    public int endurPts()     { return endurPts; }

    public void setPlayerLevel(int level)  { this.playerLevel = Math.max(1, level); }
    public void setUnspentPts(int pts)     { this.unspentPts  = Math.max(0, pts); }
    public void setCritPts(int pts)        { this.critPts     = Math.max(0, pts); }
    public void setSpecPts(int pts)        { this.specPts     = Math.max(0, pts); }
    public void setEndurPts(int pts)       { this.endurPts    = Math.max(0, pts); }

    public long currentExp()              { return currentExp; }
    public void setCurrentExp(long exp)   { this.currentExp   = Math.max(0L, exp); }
    public void addExp(long amount)        { if (amount > 0) this.currentExp += amount; }

    /** 잠재능력 GUI에서 '현재 유지' 롤백 용도. 엔진 패키지 내 전용. */
    public void updatePotentialProfile(String instanceId, PotentialProfile profile) {
        inventoryItem(instanceId).ifPresent(item -> item.setPotentialProfile(profile));
    }

    public void addUnspentPts(int amount) {
        if (amount > 0) unspentPts += amount;
    }

    /** 미배분 포인트를 1 소모해 해당 트리에 1포인트 배분. 포인트 없으면 false 반환. */
    public boolean allocatePt(String tree) {
        if (unspentPts <= 0) return false;
        switch (tree) {
            case "crit"  -> critPts++;
            case "spec"  -> specPts++;
            case "endur" -> endurPts++;
            default -> { return false; }
        }
        unspentPts--;
        return true;
    }

    /** 해당 트리에서 1포인트 환불 → 미배분 포인트로 반환. 포인트 없으면 false 반환. */
    public boolean deallocatePt(String tree) {
        switch (tree) {
            case "crit"  -> { if (critPts  <= 0) return false; critPts--;  }
            case "spec"  -> { if (specPts  <= 0) return false; specPts--;  }
            case "endur" -> { if (endurPts <= 0) return false; endurPts--; }
            default -> { return false; }
        }
        unspentPts++;
        return true;
    }

    public void equipRune(int slotNo, String runeId) {
        equippedRunes.put(slotNo, normalize(runeId));
    }

    public void unequipRune(int slotNo) {
        equippedRunes.remove(slotNo);
    }

    public Map<Integer, String> equippedRunes() {
        return Map.copyOf(new LinkedHashMap<>(equippedRunes));
    }

    public String classEngravingId() {
        return classEngravingId;
    }

    public void setClassEngravingId(String classEngravingId) {
        this.classEngravingId = normalize(classEngravingId);
    }

    public void equipCommonEngraving(int slotNo, EquippedCommonEngraving value) {
        commonEngravings.put(slotNo, value);
    }

    public void unequipCommonEngraving(int slotNo) {
        commonEngravings.remove(slotNo);
    }

    public Map<Integer, EquippedCommonEngraving> commonEngravings() {
        return Map.copyOf(new LinkedHashMap<>(commonEngravings));
    }

    public int getCeilingCounter(String key) {
        return ceilingCounters.getOrDefault(normalize(key), 0);
    }

    public void incrementCeilingCounter(String key) {
        ceilingCounters.merge(normalize(key), 1, Integer::sum);
    }

    public void setCeilingCounter(String key, int value) {
        if (value > 0) ceilingCounters.put(normalize(key), value);
        else ceilingCounters.remove(normalize(key));
    }

    public void resetCeilingCounter(String key) {
        ceilingCounters.remove(normalize(key));
    }

    public Map<String, Integer> ceilingCountersSnapshot() {
        return Map.copyOf(new LinkedHashMap<>(ceilingCounters));
    }

    // ─── IL 경고 카운터 ────────────────────────────────────────────

    public int  ilWarningCount()          { return ilWarningCount; }
    public void setIlWarningCount(int v)  { this.ilWarningCount = Math.max(0, v); }
    public void incrementIlWarning()      { this.ilWarningCount++; }

    public int  mobIlHitCount()           { return mobIlHitCount; }
    public void setMobIlHitCount(int v)   { this.mobIlHitCount = Math.max(0, v); }
    public void incrementMobIlHit()       { this.mobIlHitCount++; }
    public void resetMobIlHit()           { this.mobIlHitCount = 0; }

    // ─── 강화 촉진제 보너스 ────────────────────────────────────────

    public int  catalystBonusPct()              { return catalystBonusPct; }
    public void setCatalystBonusPct(int v)      { this.catalystBonusPct = Math.max(0, v); }
    /** 촉진제 사용 시 보너스 % 누적 (중첩 가능). */
    public void addCatalystBonus(int pct)       { this.catalystBonusPct += pct; }
    /** 강화 시도 시 보너스 전체 소모 → 소모량 반환. */
    public int drainCatalystBonus() {
        int v = catalystBonusPct;
        catalystBonusPct = 0;
        return v;
    }

    // ─── 치장 재질 ────────────────────────────────────────────────
    // 키 규칙: 방어구 = EquipmentSlot.name() (e.g. "HELMET"),
    //          무기 타입별 = "weapon_" + WeaponType.name() (e.g. "weapon_SWORD")

    private final Map<String, String> cosmeticMaterials = new LinkedHashMap<>();

    /** 임의 문자열 키 — 무기 타입별("weapon_SWORD") 또는 방어구("HELMET") */
    public void setCosmeticMaterial(String key, String mat) {
        if (key == null || key.isBlank()) return;
        if (mat == null || mat.isBlank()) cosmeticMaterials.remove(key);
        else cosmeticMaterials.put(key, normalize(mat));
    }

    /** 방어구 슬롯 헬퍼 (key = slot.name()) */
    public void setCosmeticMaterial(EquipmentSlot slot, String mat) {
        setCosmeticMaterial(slot.name(), mat);
    }

    public String getCosmeticMaterial(String key) {
        return key == null ? "" : cosmeticMaterials.getOrDefault(key, "");
    }

    /** 방어구 슬롯 헬퍼 */
    public String getCosmeticMaterial(EquipmentSlot slot) {
        return getCosmeticMaterial(slot.name());
    }

    public Map<String, String> cosmeticMaterialsSnapshot() {
        return Map.copyOf(new LinkedHashMap<>(cosmeticMaterials));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record EquippedCommonEngraving(
            String engravingId,
            int level
    ) {
        public EquippedCommonEngraving {
            engravingId = engravingId == null ? "" : engravingId.trim().toLowerCase(Locale.ROOT);
            level = Math.max(1, level);
        }
    }
}
