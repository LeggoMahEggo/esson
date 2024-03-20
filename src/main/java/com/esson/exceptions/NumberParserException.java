package com.esson.exceptions;

public class NumberParserException extends ValueParserException {
    public NumberParserException(String message) {
        super(message);
    }

    public NumberParserException(String message, Throwable cause) {
        super(message, cause);
    }
}
