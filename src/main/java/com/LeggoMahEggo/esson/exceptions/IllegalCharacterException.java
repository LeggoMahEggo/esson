package com.LeggoMahEggo.esson.exceptions;

public class IllegalCharacterException extends JsonParserException {
    public IllegalCharacterException(String message) {
        super(message);
    }

    public IllegalCharacterException(String message, Throwable cause) {
        super(message, cause);
    }
}
