package com.poro.rpg.growth.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PlayerEquipmentItem {
    private final String itemInstanceId;
    private final String itemId;
    private int enhanceLevel;
    private PotentialProfile potentialProfile;
    private ItemGrade grade = ItemGrade.COMMON;
    private List<PotentialLine> substatLines = new ArrayList<>();
    /** 잠재 큐브 천장 카운터 — 현재 등급에서 승급 없이 사용한 큐브 수. 승급 시 0 리셋 (DL-129 추가#4, 영속). */
    private int pityCount;

    public PlayerEquipmentItem(String itemInstanceId, String itemId) {
        this.itemInstanceId = normalize(itemInstanceId);
        this.itemId = normalize(itemId);
    }

    public String itemInstanceId() {
        return itemInstanceId;
    }

    public String itemId() {
        return itemId;
    }

    public int enhanceLevel() {
        return enhanceLevel;
    }

    public PotentialProfile potentialProfile() {
        return potentialProfile;
    }

    public void setEnhanceLevel(int enhanceLevel) {
        this.enhanceLevel = Math.max(0, enhanceLevel);
    }

    void setPotentialProfile(PotentialProfile potentialProfile) {
        this.potentialProfile = potentialProfile;
    }

    public ItemGrade grade() {
        return grade;
    }

    public int pityCount() {
        return pityCount;
    }

    public void setPityCount(int pityCount) {
        this.pityCount = Math.max(0, pityCount);
    }

    public List<PotentialLine> substatLines() {
        return List.copyOf(substatLines);
    }

    void setGrade(ItemGrade grade) {
        this.grade = grade != null ? grade : ItemGrade.COMMON;
    }

    void setSubstatLines(List<PotentialLine> lines) {
        this.substatLines = lines != null ? new ArrayList<>(lines) : new ArrayList<>();
    }

    /** 퍼시스턴스 역직렬화용 팩토리 — 외부 패키지에서 호출 가능. */
    public static PlayerEquipmentItem restore(String instanceId, String itemId,
                                              int enhanceLevel, ItemGrade grade,
                                              PotentialProfile potential,
                                              List<PotentialLine> substats) {
        return restore(instanceId, itemId, enhanceLevel, grade, potential, substats, 0);
    }

    /** 퍼시스턴스 역직렬화용 팩토리 (천장 카운터 포함, DL-129 추가#4). */
    public static PlayerEquipmentItem restore(String instanceId, String itemId,
                                              int enhanceLevel, ItemGrade grade,
                                              PotentialProfile potential,
                                              List<PotentialLine> substats,
                                              int pityCount) {
        PlayerEquipmentItem item = new PlayerEquipmentItem(instanceId, itemId);
        item.setEnhanceLevel(enhanceLevel);
        item.setGrade(grade);
        item.setPotentialProfile(potential);
        item.setSubstatLines(substats != null ? substats : List.of());
        item.setPityCount(pityCount);
        return item;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
