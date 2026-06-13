package kr.zenon.moncore.economy;

import kr.zenon.moncore.ZenonMonCore;
import kr.zenon.moncore.config.ConfigManager;
import kr.zenon.moncore.data.PlayerProgress;
import kr.zenon.moncore.data.ZenonMonState;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 골드 잔액 API (economy_design.md §8, 단일 화폐 골드).
 * 저장은 PlayerProgress.balance. 모든 증감은 출처 태그와 함께 감사 로깅(§6 텔레메트리 1차).
 * 외부 경제 모드 대비 추상화 — 현재는 내부 PersistentState 직결.
 */
public final class EconomyBridge {
    private EconomyBridge() {}

    private static PlayerProgress progress(ServerPlayerEntity player) {
        return ZenonMonState.get(player.getServer()).getOrCreate(player.getUuid());
    }

    public static long getBalance(ServerPlayerEntity player) {
        return progress(player).balance;
    }

    /** 입금(보상·매입). 음수/0은 무시. */
    public static void deposit(ServerPlayerEntity player, long amount, String source) {
        if (amount <= 0) return;
        ZenonMonState state = ZenonMonState.get(player.getServer());
        PlayerProgress p = state.getOrCreate(player.getUuid());
        p.balance += amount;
        state.goldIn.merge(group(source), amount, Long::sum); // 텔레메트리
        state.markDirty();
        audit(player, "+" + amount, p.balance, source);
    }

    /** 출금(구매·해금). 잔액 부족 시 false(차감 안 함). */
    public static boolean withdraw(ServerPlayerEntity player, long amount, String source) {
        if (amount <= 0) return true;
        ZenonMonState state = ZenonMonState.get(player.getServer());
        PlayerProgress p = state.getOrCreate(player.getUuid());
        if (p.balance < amount) return false;
        p.balance -= amount;
        state.goldOut.merge(group(source), amount, Long::sum); // 텔레메트리
        state.markDirty();
        audit(player, "-" + amount, p.balance, source);
        return true;
    }

    /** 관리자 강제 설정. */
    public static void set(ServerPlayerEntity player, long amount, String source) {
        ZenonMonState state = ZenonMonState.get(player.getServer());
        PlayerProgress p = state.getOrCreate(player.getUuid());
        p.balance = Math.max(0, amount);
        state.markDirty();
        audit(player, "=" + p.balance, p.balance, source);
    }

    /** 출처 태그를 그룹키로(콜론 앞). 예 sell:minecraft:diamond → sell. */
    private static String group(String source) {
        if (source == null) return "기타";
        int i = source.indexOf(':');
        return i > 0 ? source.substring(0, i) : source;
    }

    private static void audit(ServerPlayerEntity player, String delta, long balance, String source) {
        if (!ConfigManager.core().logging.auditEnabled) return;
        ZenonMonCore.LOGGER.info("[Economy] {} {} (잔액 {}) src={}",
                player.getGameProfile().getName(), delta, balance, source);
    }
}
