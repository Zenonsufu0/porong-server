package kr.poro.poromoncore.event;

import kr.poro.poromoncore.data.PoroMonState;
import net.minecraft.server.MinecraftServer;

/**
 * 전역 이벤트 부스트 (IB-002): 경험치/골드 ×2 토글. 상태는 PoroMonState(영속).
 * 서버 참조를 캐시해 이벤트 훅(경험치 등)에서 부스트 배수를 조회한다.
 */
public final class EventManager {
    private EventManager() {}

    private static final double BOOST = 2.0;
    private static MinecraftServer server;

    public static void setServer(MinecraftServer s) { server = s; }

    private static PoroMonState state() {
        return server == null ? null : PoroMonState.get(server);
    }

    public static boolean isXpBoost() {
        PoroMonState s = state();
        return s != null && s.xpBoost;
    }

    public static boolean isGoldBoost() {
        PoroMonState s = state();
        return s != null && s.goldBoost;
    }

    public static double xpMultiplier() { return isXpBoost() ? BOOST : 1.0; }
    public static double goldMultiplier() { return isGoldBoost() ? BOOST : 1.0; }

    public static void toggleXp() {
        PoroMonState s = state();
        if (s != null) { s.xpBoost = !s.xpBoost; s.markDirty(); }
    }

    public static void toggleGold() {
        PoroMonState s = state();
        if (s != null) { s.goldBoost = !s.goldBoost; s.markDirty(); }
    }
}
