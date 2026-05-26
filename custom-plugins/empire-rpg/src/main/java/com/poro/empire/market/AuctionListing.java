package com.poro.empire.market;

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
        Long soldAt
) {
    public String remainingText() {
        long remaining = expiresAt - System.currentTimeMillis();
        if (remaining <= 0) return "만료됨";
        long hours = remaining / 3_600_000L;
        long minutes = (remaining % 3_600_000L) / 60_000L;
        if (hours >= 24) return (hours / 24) + "일 " + (hours % 24) + "시간";
        return hours + "시간 " + minutes + "분";
    }
}
