package com.poro.rpg.common.flag;

import java.util.Objects;

/**
 * 플래그 값 holder. v0.1은 BOOL / LONG / STRING 3종 (Q3 C안).
 *
 * <p>factory로 타입별 생성, {@link #type()} 확인 후 대응 accessor 호출.
 * 타입 불일치 접근은 {@link IllegalStateException}을 던진다.
 * v0.2에서 JSON 타입 추가 시 factory와 accessor만 확장하면 된다.
 */
public final class FlagValue {
    private final FlagValueType type;
    private final Boolean boolValue;
    private final Long longValue;
    private final String stringValue;

    private FlagValue(FlagValueType type, Boolean boolValue, Long longValue, String stringValue) {
        this.type = Objects.requireNonNull(type, "type");
        this.boolValue = boolValue;
        this.longValue = longValue;
        this.stringValue = stringValue;
    }

    public static FlagValue ofBool(boolean v) {
        return new FlagValue(FlagValueType.BOOL, v, null, null);
    }

    public static FlagValue ofLong(long v) {
        return new FlagValue(FlagValueType.LONG, null, v, null);
    }

    public static FlagValue ofString(String v) {
        Objects.requireNonNull(v, "value");
        return new FlagValue(FlagValueType.STRING, null, null, v);
    }

    public FlagValueType type() {
        return type;
    }

    public boolean asBool() {
        if (type != FlagValueType.BOOL) {
            throw new IllegalStateException("FlagValue is not BOOL: " + type);
        }
        return boolValue;
    }

    public long asLong() {
        if (type != FlagValueType.LONG) {
            throw new IllegalStateException("FlagValue is not LONG: " + type);
        }
        return longValue;
    }

    public String asString() {
        if (type != FlagValueType.STRING) {
            throw new IllegalStateException("FlagValue is not STRING: " + type);
        }
        return stringValue;
    }

    /** DDL 컬럼 매핑용 raw getter — Repository 전용. */
    String rawText() {
        return stringValue;
    }

    Long rawLong() {
        return longValue;
    }

    Boolean rawBool() {
        return boolValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlagValue other)) return false;
        return type == other.type
                && Objects.equals(boolValue, other.boolValue)
                && Objects.equals(longValue, other.longValue)
                && Objects.equals(stringValue, other.stringValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, boolValue, longValue, stringValue);
    }

    @Override
    public String toString() {
        return switch (type) {
            case BOOL -> "FlagValue[BOOL=" + boolValue + "]";
            case LONG -> "FlagValue[LONG=" + longValue + "]";
            case STRING -> "FlagValue[STRING=\"" + stringValue + "\"]";
        };
    }
}
