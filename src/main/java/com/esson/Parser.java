package com.esson;

import com.doopy.exceptions.IllegalCharacterException;
import com.doopy.exceptions.JsonParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Responsible for parsing JSON strings in various forms to a Java class
 */
public class Parser {

    /**
     * Parses a JSON string according to EMCA-404 standards
     * @param json the string to parse
     * @return a JsonValue containing the entire parsed JSON
     * @throws JsonParserException if parsing fails, or some other unexpected error occurs while parsing
     *
     */
    public static JsonValue parseFromString(String json) throws JsonParserException {
        try {
            return parseFromString(new Cursor(json, new Options()));

        } catch (Exception e) {
            if (e instanceof JsonParserException)
                throw e;

            throw new JsonParserException("Encountered unexpected error while parsing: " + e.getMessage(), e);
        }
    }

    /**
     * Parses a JSON string with options to (potentially) ignore EMCA-404 standards (such as single-quotes enclosing strings)
     * @param json the string to parse
     * @param options the Options object
     * @return a JsonValue containing the entire parsed JSON
     * @throws JsonParserException if parsing fails, or some other unexpected error occurs while parsing
     *
     */
    public static JsonValue parseFromString(String json, Options options) throws JsonParserException {
        try {
            return parseFromString(new Cursor(json, options));

        } catch (Exception e) {
            if (e instanceof JsonParserException)
                throw e;

            throw new JsonParserException("Encountered unexpected error while parsing: " + e.getMessage(), e);
        }
    }

    /**
     * Parses a JSON from a file according to EMCA-404 standards
     * @param file the file to load
     * @return a JsonValue containing the entire parsed JSON
     * @throws JsonParserException if parsing fails, or some other unexpected error occurs while parsing
     */
    public static JsonValue parseFromFile(File file) throws JsonParserException {
        return parseFromFile(file, new Options());
    }

    /**
     * Parses a JSON from a file with options to (potentially) ignore EMCA-404 standards (such as single-quotes enclosing strings)
     * @param file the file to load
     * @param options the Options object
     * @return a JsonValue containing the entire parsed JSON
     * @throws JsonParserException if parsing fails, or some other unexpected error occurs while parsing
     */
    public static JsonValue parseFromFile(File file, Options options) throws JsonParserException {
        String json;

        try (FileInputStream is = new FileInputStream(file)) {
            int fileLen = (int) file.length();
            byte[] fileBytes = new byte[fileLen];
            is.read(fileBytes, 0, fileLen);

            json = new String(fileBytes, StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return parseFromString(json, options);
    }


    /*
     * Parsing magic
     */

    private static JsonValue parseFromString(Cursor cursor) {
        cursor.moveToFirstNonWhitespace();
        JsonValue value = null;

        if (cursor.currentChar() == '[') {
            JsonList jl = new JsonList();
            cursor.moveCursorFoward();
            cursor.moveToFirstNonWhitespace();

            while (true) {
                // Empty array
                char currentChar = cursor.currentChar();
                if (currentChar == ']') {
                    if (cursor.previousNonwhitespaceChar() == ',' && !cursor.options.commaAtEnd)
                        throw new IllegalCharacterException("Reached the end of an array with an extra comma" +
                                cursor.locationErrMsgHelper());

                    cursor.moveCursorFoward();
                    break;
                }

                // Get value
                jl.add(collectValue(cursor));

                // Continue if there are more elements in the array
                try {
                    cursor.moveToFirstNonWhitespace();
                } catch (JsonParserException e) {
                    // Need special message here
                    throw new JsonParserException("While checking for more elements in the array, " +
                            "reached the end of JSON string without encountering a non-whitespace character", e);
                }
                currentChar = cursor.currentChar();
                cursor.moveCursorFoward();

                if (currentChar == ',') {
                    cursor.moveToFirstNonWhitespace();
                    continue;
                }

                // Make sure the end character is a ]
                if (!cursor.atEndOfJson(true))
                    cursor.moveToFirstNonWhitespace();

                if (currentChar != ']')
                    throw new IllegalCharacterException("Expected to find a ',' to continue the array or a ']' character " +
                            "to end it, found '" + currentChar + "' instead" + cursor.locationErrMsgHelper());

                break; // Finish with array
            }

            cursor.decreaseDepth();
            value = JsonValue.valueOf(jl);

        // Add object data
        } else if (cursor.currentChar() == '{') {
            JsonMap object = new JsonMap();
            cursor.moveCursorFoward(); // Move cursor after {
            cursor.moveToFirstNonWhitespace(); // Stop at first nonwhitespace character

            while (true) {
                // Empty object
                if (cursor.currentChar() == '}') {
                    if (cursor.previousNonwhitespaceChar() == ',' && !cursor.options.commaAtEnd)
                        throw new IllegalCharacterException("Reached the end of an object with an extra comma" +
                                cursor.locationErrMsgHelper());

                    cursor.moveCursorFoward();
                    break;
                }
                char currentChar;

                // Get key (which is a string)
                cursor.moveToFirstNonWhitespace();
                currentChar = cursor.currentChar();

                if (currentChar != '"') {
                    boolean doThrow = true;
                    String expectedQuote = "\"";

                    if (cursor.options.singleQuoteString && currentChar == '\'')
                        doThrow = false;

                    else if (cursor.options.singleQuoteString)
                        expectedQuote += " or '";

                    if (doThrow)
                        throw new IllegalCharacterException("Expected to find a " + expectedQuote + " character to start " +
                                "the object's key" + ", found a '" + currentChar + "' instead" + cursor.locationErrMsgHelper());
                }

                String key = cursor.collectString(currentChar);

                // Move to value
                cursor.moveToFirstNonWhitespace();
                currentChar = cursor.currentChar();

                if (currentChar != ':')
                    throw new IllegalCharacterException("Expected to find a ':' character to start the object's value" +
                            ", found a '" + currentChar + "' instead" + cursor.locationErrMsgHelper());

                cursor.moveCursorFoward();

                // Get value
                cursor.moveToFirstNonWhitespace();
                object.put(key, collectValue(cursor));

                // Move to next key/value pair, or finish the object
                cursor.moveToFirstNonWhitespace(); // Should be ',' or '}'
                currentChar = cursor.currentChar();
                boolean hasMorePairs = currentChar == ',';
                cursor.moveCursorFoward();

                if (hasMorePairs)
                    continue;

                // Make sure the end character is a }
                if (!cursor.atEndOfJson(true))
                    cursor.moveToFirstNonWhitespace();

                if (currentChar != '}')
                    throw new IllegalCharacterException("Encountered an illegal character ('" +
                            currentChar + "') while collecting a value" + cursor.locationErrMsgHelper());

                break;
            }

            cursor.decreaseDepth();
            value = JsonValue.valueOf(object);

        // EMCA-404 allows for top-level values
        } else if (cursor.getDepth() == 0) {
            value = collectValue(cursor);
            cursor.decreaseDepth();
        }


        /*
         * At the end of parsing, handle things like close brackets/braces that might appear at the end of the JSON string
         */
        if (cursor.getDepth() == -1) {

            while (!cursor.atEndOfJson(false)) {
                char c = cursor.currentChar();

                // Ignore whitespace
                if (c == ' ' || c == '\r' || c == '\n' || c == '\t') {
                    cursor.moveCursorFoward();
                    continue;
                }

                throw new IllegalCharacterException("Found unexpected '" + cursor.getPrintableCharacter(c) + "' character " +
                        "at/near the end of the JSON string");
            }
        }

        return value;
    }

    /**
     * Collects a value from the current cursor position
     * @param cursor the Cursor object to collect with
     * @return a JsonValue with the collected value
     * @throws IllegalCharacterException if the character at the cursor's current position is not supported for collection
     */
    private static JsonValue collectValue(Cursor cursor) throws IllegalCharacterException {
        char currentChar = cursor.currentChar();

        if (currentChar == '\'' && !cursor.options.singleQuoteString)
            throw new IllegalCharacterException("Cannot collect a string that opens with a single-quote" +
                    cursor.locationErrMsgHelper());

        if (currentChar == '+' && !cursor.options.plusAtFront)
            throw new IllegalCharacterException("Cannot start a number with the + sign" + cursor.locationErrMsgHelper());

        if (currentChar == 'x')
            throw new IllegalCharacterException("Cannot parse hex numbers" + cursor.locationErrMsgHelper());

        if (currentChar == '[' || currentChar == '{')
            cursor.increaseDepth();

        // Done this way to support Java 11+
        switch (currentChar) {
            case '\'':
            case '"': return JsonValue.valueOf(cursor.collectString(currentChar));
            case 't':
            case 'f': return JsonValue.valueOf(cursor.collectBoolean());
            case 'n': return JsonValue.valueOf(cursor.collectNull());
            case '-':
            case '+':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9': return JsonValue.valueOf(cursor.collectNumber());
            case '[':
            case '{': return parseFromString(cursor);
            default: throw new IllegalCharacterException(
                    "Encountered an unknown character ('" + currentChar + "') while trying to determine the type of value " +
                            "to collect" + cursor.locationErrMsgHelper());
        }
    }
}
