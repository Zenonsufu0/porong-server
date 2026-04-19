package com.poro.empire.common.flag;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 플레이어 플래그 네임스페이스 키.
 *
 * <p>형식: {@code 도메인.카테고리.키} 3단(각 세그먼트 {@code [a-z][a-z0-9_]*}).
 * 세그먼트는 소문자 시작 + 소문자·숫자·언더스코어만 허용. 공백·대문자·기타 특수문자 금지.
 * 전체 길이는 64자 이내.
 *
 * <p>참조:
 * <ul>
 *   <li>{@code docs/00_index_and_execution/poro_flag_store_v01_sprint_plan.md} §PR2</li>
 *   <li>2026-04-19 Q3 C안 — {@link FlagValueType} JSON은 v0.2에서 추가</li>
 * </ul>
 */
public record FlagKey(String value) {

    public static final int MAX_LENGTH = 64;

    private static final Pattern SEGMENT = Pattern.compile("[a-z][a-z0-9_]*");

    public FlagKey {
        Objects.requireNonNull(value, "value");
        if (value.isEmpty()) {
            throw new IllegalArgumentException("FlagKey must not be empty");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "FlagKey length " + value.length() + " exceeds " + MAX_LENGTH + ": " + value);
        }
        String[] parts = value.split("\\.", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException(
                    "FlagKey must have 3 dot-separated segments (도메인.카테고리.키): " + value);
        }
        for (String part : parts) {
            if (part.isEmpty()) {
                throw new IllegalArgumentException("FlagKey has empty segment: " + value);
            }
            if (!SEGMENT.matcher(part).matches()) {
                throw new IllegalArgumentException(
                        "FlagKey segment '" + part + "' must match [a-z][a-z0-9_]*: " + value);
            }
        }
    }

    /** Factory helper — 정책상 정적 팩토리 사용을 권장한다. */
    public static FlagKey of(String value) {
        return new FlagKey(value);
    }

    public String domain() {
        return value.split("\\.", -1)[0];
    }

    public String category() {
        return value.split("\\.", -1)[1];
    }

    public String key() {
        return value.split("\\.", -1)[2];
    }

    @Override
    public String toString() {
        return value;
    }
}
