package com.fund.exception;

public class KlineDataException extends RuntimeException {
    public KlineDataException(String message) {
        super(message);
    }

    public KlineDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
