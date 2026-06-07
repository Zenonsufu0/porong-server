package kr.poro.poromoncore.gym;

import java.util.List;

/**
 * 8개 관장(체육관) 정의 (gym_badge_design.md §2).
 * ⚠️ 타입/순서/배지명/레벨캡은 설계 문서의 현행 예시 — 향후 gyms.json으로 이전(하드코딩 임시).
 * 마지막(8번) 레벨캡 100 확정 / 순차 강제.
 */
public final class GymInfo {
    private GymInfo() {}

    public record Gym(String id, int order, String typeKo, int levelCap, String badgeKo) {}

    public static final List<Gym> GYMS = List.of(
            new Gym("gym_bug",      1, "벌레",   18,  "코쿤 배지"),
            new Gym("gym_rock",     2, "바위",   28,  "스톤 배지"),
            new Gym("gym_electric", 3, "전기",   38,  "볼트 배지"),
            new Gym("gym_grass",    4, "풀",     50,  "리프 배지"),
            new Gym("gym_water",    5, "물",     62,  "타이드 배지"),
            new Gym("gym_fire",     6, "불꽃",   75,  "플레임 배지"),
            new Gym("gym_psychic",  7, "에스퍼", 88,  "마인드 배지"),
            new Gym("gym_dragon",   8, "드래곤", 100, "드래곤 배지")
    );

    public static Gym byId(String id) {
        for (Gym g : GYMS) if (g.id.equals(id)) return g;
        return null;
    }
}
