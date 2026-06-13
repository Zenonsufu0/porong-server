package kr.zenon.rpg.market;

import java.util.UUID;

public record AuctionListing(
        long id,
        UUID sellerUuid,
        String sellerName,
        String itemId,
        int quantity,
        long price,
        long listedAt,
        long expiresAt,
        String status,
        Long soldAt,
        String itemPayload   // 흔적 인스턴스 거래용 JSON payload (DL-129 추가#38, P5). 일반 매물은 null.
) {
    public boolean isTrace() { return itemPayload != null && !itemPayload.isBlank(); }

    public String remainingText() {
        long remaining = expiresAt - System.currentTimeMillis();
        if (remaining <= 0) return "만료됨";
        long hours = remaining / 3_600_000L;
        long minutes = (remaining % 3_600_000L) / 60_000L;
        if (hours >= 24) return (hours / 24) + "일 " + (hours % 24) + "시간";
        return hours + "시간 " + minutes + "분";
    }
}
