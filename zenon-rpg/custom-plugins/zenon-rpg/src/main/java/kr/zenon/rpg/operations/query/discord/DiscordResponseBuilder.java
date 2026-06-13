package kr.zenon.rpg.operations.query.discord;

import kr.zenon.rpg.operations.query.service.HallOfFameQueryService;
import kr.zenon.rpg.operations.query.service.MarketStatisticsQueryService;
import kr.zenon.rpg.operations.query.service.PlayerDetailQueryService;

import java.util.ArrayList;
import java.util.List;

public final class DiscordResponseBuilder {
    private DiscordResponseBuilder() {
    }

    public static DiscordCardResponse playerInfo(String command, PlayerDetailQueryService.PlayerSnapshotProjection snapshot) {
        List<String> lines = new ArrayList<>();
        lines.add("닉네임: " + snapshot.nickname());
        lines.add("클래스: " + snapshot.classId());
        lines.add("총 강화합: +" + snapshot.enhancementSum());
        lines.add("최근 보스: " + (snapshot.latestBossId().isBlank() ? "-" : snapshot.latestBossId()));
        lines.add("영지 해금: " + (snapshot.estateUnlocked() ? "예" : "아니오"));
        lines.add("완료 퀘스트: " + snapshot.completedQuestCount());
        return new DiscordCardResponse(command, "내정보", List.copyOf(lines), "zenon discord snapshot");
    }

    public static DiscordCardResponse equipment(String command, PlayerDetailQueryService.EquipmentProjection equipment) {
        List<String> lines = new ArrayList<>();
        lines.add("장비: " + (equipment.equippedItems().isEmpty() ? "-" : String.join(", ", equipment.equippedItems())));
        lines.add("강화합: +" + equipment.enhancementSum());
        lines.add("세트: " + (equipment.setEffects().isEmpty() ? "-" : String.join(" | ", equipment.setEffects())));
        lines.add("룬: " + (equipment.runes().isEmpty() ? "-" : String.join(", ", equipment.runes())));
        lines.add("각인: " + (equipment.engravings().isEmpty() ? "-" : String.join(", ", equipment.engravings())));
        return new DiscordCardResponse(command, "내장비", List.copyOf(lines), "zenon equipment snapshot");
    }

    public static DiscordCardResponse bossRecords(String command, List<PlayerDetailQueryService.PlayerBossRecordProjection> records) {
        List<String> lines = new ArrayList<>();
        if (records.isEmpty()) {
            lines.add("최근 보스 기록이 없습니다.");
        } else {
            for (PlayerDetailQueryService.PlayerBossRecordProjection record : records.stream().limit(5).toList()) {
                lines.add(record.bossId()
                        + " | clear=" + record.clearSuccess()
                        + " | phase=" + record.phaseReached()
                        + " | time=" + record.clearTimeSeconds()
                        + "s | deaths=" + record.deaths());
            }
        }
        return new DiscordCardResponse(command, "보스기록", List.copyOf(lines), "zenon boss records");
    }

    public static DiscordCardResponse marketPrice(String command, MarketStatisticsQueryService.MarketPriceSnapshot market) {
        List<String> lines = new ArrayList<>();
        lines.add("아이템: " + market.itemId());
        lines.add("평균 시세: " + Math.round(market.avgPrice()));
        lines.add("거래량: " + market.tradeVolume());
        lines.add("공급량: " + market.supplyVolume());
        lines.add("변동률: " + String.format(java.util.Locale.ROOT, "%.2f%%", market.changeRate() * 100.0d));
        return new DiscordCardResponse(command, "시세", List.copyOf(lines), "zenon market snapshot");
    }

    public static DiscordCardResponse life(String command, PlayerDetailQueryService.LifeProjection life) {
        List<String> lines = new ArrayList<>();
        lines.add("영지 해금: " + (life.estateUnlocked() ? "예" : "아니오"));
        lines.add("시설: " + (life.facilities().isEmpty() ? "-" : String.join(", ", life.facilities())));
        lines.add("저장량: " + (life.currentStoredItems().isEmpty() ? "-" : life.currentStoredItems().toString()));
        lines.add("생활 레벨: " + (life.lifeLevels().isEmpty() ? "-" : life.lifeLevels().toString()));
        return new DiscordCardResponse(command, "내영지", List.copyOf(lines), "zenon life snapshot");
    }

    public static DiscordCardResponse hallOfFame(String command, HallOfFameQueryService.HallOfFameSnapshot hall) {
        List<String> lines = new ArrayList<>();
        lines.add("서버 최초: " + hall.serverFirst().stream().limit(3).map(HallOfFameQueryService.InMemoryEntry::userId).toList());
        lines.add("극상위 기록: " + hall.extremeRecords().stream().limit(3).map(HallOfFameQueryService.InMemoryEntry::userId).toList());
        lines.add("시즌 베스트: " + hall.seasonBest().stream().limit(3).map(record -> record.bossId() + ":" + record.clearTimeSeconds() + "s").toList());
        lines.add("무사망 기록: " + hall.noDeathRecords().stream().limit(3).map(record -> record.bossId() + ":" + record.runId()).toList());
        lines.add("전승 보유자: " + hall.transcendenceHolders());
        return new DiscordCardResponse(command, "명예의전당", List.copyOf(lines), "zenon hall of fame");
    }
}
