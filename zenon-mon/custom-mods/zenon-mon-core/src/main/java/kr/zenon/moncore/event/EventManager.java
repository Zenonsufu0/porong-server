package kr.zenon.moncore.event;

import kr.zenon.moncore.data.ZenonMonState;
import net.minecraft.server.MinecraftServer;

/**
 * 전역 이벤트 부스트 (IB-002): 경험치/골드 ×2 토글. 상태는 ZenonMonState(영속).
 * 서버 참조를 캐시해 이벤트 훅(경험치 등)에서 부스트 배수를 조회한다.
 */
public final class EventManager {
    private EventManager() {}

    private static final double BOOST = 2.0;
    private static MinecraftServer server;

    public static void setServer(MinecraftServer s) { server = s; }

    private static ZenonMonState state() {
        return server == null ? null : ZenonMonState.get(server);
    }

    public static boolean isXpBoost() {
        ZenonMonState s = state();
        return s != null && s.xpBoost;
    }

    public static boolean isGoldBoost() {
        ZenonMonState s = state();
        return s != null && s.goldBoost;
    }

    public static boolean isApexBoost() {
        ZenonMonState s = state();
        return s != null && s.apexBoost;
    }

    public static boolean isFieldEventFast() {
        ZenonMonState s = state();
        return s != null && s.fieldEventFast;
    }

    public static double xpMultiplier() { return isXpBoost() ? BOOST : 1.0; }
    public static double goldMultiplier() { return isGoldBoost() ? BOOST : 1.0; }
    public static double apexMultiplier() { return isApexBoost() ? BOOST : 1.0; }

    public static void toggleXp() {
        ZenonMonState s = state();
        if (s != null) { s.xpBoost = !s.xpBoost; s.markDirty(); }
    }

    public static void toggleGold() {
        ZenonMonState s = state();
        if (s != null) { s.goldBoost = !s.goldBoost; s.markDirty(); }
    }

    public static void toggleApex() {
        ZenonMonState s = state();
        if (s != null) { s.apexBoost = !s.apexBoost; s.markDirty(); }
    }

    public static void toggleFieldEventFast() {
        ZenonMonState s = state();
        if (s != null) { s.fieldEventFast = !s.fieldEventFast; s.markDirty(); }
    }
}
