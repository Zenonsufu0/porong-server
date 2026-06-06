package kr.poro.poromoncore.util;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 한 줄 채팅 입력 대기(홈 이름 변경 등). 다음 채팅 1건을 콜백으로 가로채고 브로드캐스트를 막는다.
 * 채팅 이벤트는 네트워크 스레드에서 오므로 콜백은 서버 스레드에서 실행한다.
 */
public final class ChatInputManager {
    private ChatInputManager() {}

    private static final Map<UUID, Consumer<String>> PENDING = new ConcurrentHashMap<>();

    /** 다음 채팅 입력을 콜백으로 받도록 등록. */
    public static void await(ServerPlayerEntity player, Consumer<String> callback) {
        PENDING.put(player.getUuid(), callback);
    }

    public static void cancel(ServerPlayerEntity player) {
        PENDING.remove(player.getUuid());
    }

    /**
     * 채팅 메시지 처리 시도. 대기 중이면 true(=브로드캐스트 취소). 콜백은 서버 스레드에서 실행.
     */
    public static boolean handle(ServerPlayerEntity player, String message) {
        Consumer<String> cb = PENDING.remove(player.getUuid());
        if (cb == null) return false;
        player.getServer().execute(() -> cb.accept(message.trim()));
        return true;
    }
}
