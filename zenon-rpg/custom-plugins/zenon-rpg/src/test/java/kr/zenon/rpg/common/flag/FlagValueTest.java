package kr.zenon.rpg.common.flag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlagValueTest {

    @Test
    void ofBoolProducesBoolType() {
        FlagValue v = FlagValue.ofBool(true);
        assertEquals(FlagValueType.BOOL, v.type());
        assertTrue(v.asBool());
    }

    @Test
    void ofLongProducesLongType() {
        FlagValue v = FlagValue.ofLong(42L);
        assertEquals(FlagValueType.LONG, v.type());
        assertEquals(42L, v.asLong());
    }

    @Test
    void ofStringProducesStringType() {
        FlagValue v = FlagValue.ofString("hello");
        assertEquals(FlagValueType.STRING, v.type());
        assertEquals("hello", v.asString());
    }

    @Test
    void accessorsRejectTypeMismatch() {
        FlagValue bool = FlagValue.ofBool(false);
        assertThrows(IllegalStateException.class, bool::asLong);
        assertThrows(IllegalStateException.class, bool::asString);

        FlagValue lng = FlagValue.ofLong(1L);
        assertThrows(IllegalStateException.class, lng::asBool);
        assertThrows(IllegalStateException.class, lng::asString);

        FlagValue str = FlagValue.ofString("x");
        assertThrows(IllegalStateException.class, str::asBool);
        assertThrows(IllegalStateException.class, str::asLong);
    }

    @Test
    void ofStringRejectsNull() {
        assertThrows(NullPointerException.class, () -> FlagValue.ofString(null));
    }

    @Test
    void equalsAndHashCodeConsistent() {
        assertEquals(FlagValue.ofBool(true), FlagValue.ofBool(true));
        assertEquals(FlagValue.ofLong(42), FlagValue.ofLong(42));
        assertEquals(FlagValue.ofString("a"), FlagValue.ofString("a"));
        assertNotEquals(FlagValue.ofBool(true), FlagValue.ofBool(false));
        assertNotEquals(FlagValue.ofBool(true), FlagValue.ofLong(1));
        assertEquals(FlagValue.ofLong(42).hashCode(), FlagValue.ofLong(42).hashCode());
    }
}
