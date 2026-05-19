package com.poro.empire.growth.engine;

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

    void setEnhanceLevel(int enhanceLevel) {
        this.enhanceLevel = Math.max(0, enhanceLevel);
    }

    void setPotentialProfile(PotentialProfile potentialProfile) {
        this.potentialProfile = potentialProfile;
    }

    public ItemGrade grade() {
        return grade;
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
        PlayerEquipmentItem item = new PlayerEquipmentItem(instanceId, itemId);
        item.setEnhanceLevel(enhanceLevel);
        item.setGrade(grade);
        item.setPotentialProfile(potential);
        item.setSubstatLines(substats != null ? substats : List.of());
        return item;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
