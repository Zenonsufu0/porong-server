package com.poro.rpg.growth.engine;

public enum ItemGrade {
    COMMON("커먼"),
    RARE("레어"),
    EPIC("에픽"),
    UNIQUE("유니크"),
    LEGENDARY("레전더리");

    private final String displayName;

    ItemGrade(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
