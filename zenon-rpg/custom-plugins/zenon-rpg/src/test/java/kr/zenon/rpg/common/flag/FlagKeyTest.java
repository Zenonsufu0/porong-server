package kr.zenon.rpg.common.flag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PR2 — FlagKey 검증기 단위 테스트.
 *
 * <p>스펙: 정상 키 통과 / 금지 문자 거부 / 빈 문자열·64자 초과 거부.
 * Bukkit 의존 없음(순수 JUnit 5).
 */
class FlagKeyTest {

    @Test
    void acceptsCanonicalThreeSegmentKey() {
        FlagKey key = FlagKey.of("quest.capital.reach_radius");
        assertEquals("quest", key.domain());
        assertEquals("capital", key.category());
        assertEquals("reach_radius", key.key());
        assertEquals("quest.capital.reach_radius", key.toString());
    }

    @Test
    void acceptsAllowedCharacters() {
        assertDoesNotThrow(() -> FlagKey.of("estate.build.permission"));
        assertDoesNotThrow(() -> FlagKey.of("engraving.slot.activated"));
        assertDoesNotThrow(() -> FlagKey.of("a.b.c"));
        assertDoesNotThrow(() -> FlagKey.of("quest.act2_main.m16_theme_choice"));
    }

    @Test
    void rejectsEmptyString() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> FlagKey.of(""));
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> FlagKey.of(null));
    }

    @Test
    void rejectsExceedingMaxLength() {
        // 64자 정확히 = 허용
        String at64 = "a".repeat(20) + "." + "b".repeat(20) + "." + "c".repeat(22);
        assertEquals(64, at64.length());
        assertDoesNotThrow(() -> FlagKey.of(at64));

        // 65자 = 거부
        String at65 = "a".repeat(20) + "." + "b".repeat(20) + "." + "c".repeat(23);
        assertEquals(65, at65.length());
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> FlagKey.of(at65));
        assertTrue(ex.getMessage().contains("exceeds"));
    }

    @Test
    void rejectsWrongSegmentCount() {
        assertThrows(IllegalArgumentException.class, () -> FlagKey.of("only_two.parts"));
        assertThrows(IllegalArgumentException.class, () -> FlagKey.of("four.dotted.parts.here"));
        assertThrows(IllegalArgumentException.class, () -> FlagKey.of("no_dots_here"));
    }

    @Test
    void rejectsEmptySegments() {
        assertThrows(IllegalArgumentException.class, () -> FlagKey.of(".middle.tail"));
        assertThrows(IllegalArgumentException.class, () -> FlagKey.of("head..tail"));
        assertThrows(IllegalArgumentException.class, () -> FlagKey.of("head.middle."));
    }

    @Test
    void rejectsUppercase() {
        assertThrows(IllegalArgumentException.class, () -> FlagKey.of("Quest.capital.reach"));
        assertThrows(IllegalArgumentException.class, () -> FlagKey.of("quest.Capital.reach"));
        assertThrows(IllegalArgumentException.class, () -> FlagKey.of("quest.capital.REACH"));
    }

    @Test
    void rejectsForbiddenCharacters() {
        assertThrows(IllegalArgumentException.class, () -> FlagKey.of("quest.cap ital.reach")); // space
        assertThrows(IllegalArgumentException.class, () -> FlagKey.of("quest.capital.reach-radius")); // hyphen
        assertThrows(IllegalArgumentException.class, () -> FlagKey.of("quest.capital.reach/radius")); // slash
        assertThrows(IllegalArgumentException.class, () -> FlagKey.of("quest.capital.한글"));         // non-ascii
    }

    @Test
    void rejectsSegmentStartingWithDigit() {
        // 첫 문자는 소문자여야 함
        assertThrows(IllegalArgumentException.class, () -> FlagKey.of("1quest.capital.reach"));
        assertThrows(IllegalArgumentException.class, () -> FlagKey.of("quest.2capital.reach"));
    }

    @Test
    void flagValueTypeEnumHasThreeValues() {
        // Q3 C안 반영 — v0.1은 BOOL / LONG / STRING 3종만
        assertEquals(3, FlagValueType.values().length);
        assertNotNull(FlagValueType.valueOf("BOOL"));
        assertNotNull(FlagValueType.valueOf("LONG"));
        assertNotNull(FlagValueType.valueOf("STRING"));
    }
}
