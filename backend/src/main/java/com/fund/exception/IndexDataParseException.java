package com.fund.exception;

public class IndexDataParseException extends RuntimeException {
    public IndexDataParseException(String message) {
        super(message);
    }

    public IndexDataParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
