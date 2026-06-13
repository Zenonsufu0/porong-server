package com.poro.rpg.common.result;

import java.util.Objects;
import java.util.function.Function;

public final class Result<T> {
    private final boolean ok;
    private final T value;
    private final ErrorCode errorCode;
    private final String message;
    private final Throwable cause;

    private Result(boolean ok, T value, ErrorCode errorCode, String message, Throwable cause) {
        this.ok = ok;
        this.value = value;
        this.errorCode = errorCode;
        this.message = message;
        this.cause = cause;
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(true, value, null, null, null);
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> failure(ErrorCode errorCode) {
        ErrorCode safeCode = errorCode == null ? ErrorCode.UNKNOWN : errorCode;
        return new Result<>(false, null, safeCode, safeCode.defaultMessage(), null);
    }

    public static <T> Result<T> failure(ErrorCode errorCode, String message) {
        ErrorCode safeCode = errorCode == null ? ErrorCode.UNKNOWN : errorCode;
        String safeMessage = message == null || message.isBlank() ? safeCode.defaultMessage() : message;
        return new Result<>(false, null, safeCode, safeMessage, null);
    }

    public static <T> Result<T> failure(ErrorCode errorCode, String message, Throwable cause) {
        ErrorCode safeCode = errorCode == null ? ErrorCode.UNKNOWN : errorCode;
        String safeMessage = message == null || message.isBlank() ? safeCode.defaultMessage() : message;
        return new Result<>(false, null, safeCode, safeMessage, cause);
    }

    public boolean isOk() {
        return ok;
    }

    public boolean isFailure() {
        return !ok;
    }

    public T value() {
        return value;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public String message() {
        return message;
    }

    public Throwable cause() {
        return cause;
    }

    public T orElse(T fallback) {
        return ok ? value : fallback;
    }

    public T orElseThrow() {
        if (ok) {
            return value;
        }
        throw new DomainException(errorCode, message, cause);
    }

    public <R> Result<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        if (isFailure()) {
            return Result.failure(errorCode, message, cause);
        }
        try {
            return Result.success(mapper.apply(value));
        } catch (Exception exception) {
            return Result.failure(ErrorCode.UNKNOWN, "Failed to map result value", exception);
        }
    }

    public <R> Result<R> flatMap(Function<? super T, Result<R>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        if (isFailure()) {
            return Result.failure(errorCode, message, cause);
        }
        try {
            Result<R> mapped = mapper.apply(value);
            return mapped == null
                    ? Result.failure(ErrorCode.UNKNOWN, "Mapper returned null result")
                    : mapped;
        } catch (Exception exception) {
            return Result.failure(ErrorCode.UNKNOWN, "Failed to flat-map result value", exception);
        }
    }
}
