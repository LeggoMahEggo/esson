package com.doopy.exceptions;

public class ValueParserException extends JsonParserException {
    public ValueParserException(String message) {
        super(message);
    }

    public ValueParserException(String message, Throwable cause) {
        super(message, cause);
    }
}
