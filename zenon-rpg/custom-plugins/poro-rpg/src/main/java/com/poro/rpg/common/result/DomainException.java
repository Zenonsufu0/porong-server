package com.poro.rpg.common.result;

public class DomainException extends RuntimeException {
    private final ErrorCode errorCode;

    public DomainException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage(), null);
    }

    public DomainException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public DomainException(ErrorCode errorCode, String message, Throwable cause) {
        super(message == null ? errorCode.defaultMessage() : message, cause);
        this.errorCode = errorCode == null ? ErrorCode.UNKNOWN : errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
