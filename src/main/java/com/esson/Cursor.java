package com.esson;

import com.doopy.exceptions.JsonParserException;
import com.doopy.exceptions.ValueParserException;
import com.doopy.exceptions.NumberParserException;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Responsible for collecting values from a JSON string
 */
class Cursor {
    private final String json;
    private final int jsonLength;
    private int index; // Where in the string the cursor is pointing to
    private int depth; // How deep the cursor currently is
    final Options options; // To decide how strict you want parsing to be, eg allow leading zeroes for numbers
    // is thrown

    /**
     * Creates a new Cursor object with options set that may not fully follow EMCA-404 standards
     * @param json the JSON string to be parsed
     * @param options the parsing options object
     */
    Cursor(String json, Options options) {
        index = 0;
        this.json = json;
        jsonLength = json.length();
        this.options = options;
    }


    /*
     * Methods to throw exception if reached the end of the json string without finishing parsing
     */

    private void throwIfReachedEndPrematurely() {
        throwIfReachedEndPrematurely(
                0,
                true,
                "Reached end of JSON string without finishing parsing a string/boolean/null/number"
        );
    }

    private void throwIfReachedEndPrematurely(int offset, boolean inclusive) {
        throwIfReachedEndPrematurely(
                offset,
                inclusive,
                "Reached end of JSON string without finishing parsing a string/boolean/null/number"
        );
    }

    private void throwIfReachedEndPrematurely(int offset, boolean inclusive, String errMsg) {
        if ((inclusive) ? index + offset >= jsonLength : index + offset > jsonLength)
            throw new JsonParserException(errMsg);
    }


    /*
     * Helper methods
     */

    /**
     * Helper method to convert a character to a readable representation -- ASCII if it's not a control character
     *  and within the ASCII range, otherwise a unicode format
     * @param c the character to convert
     * @return a String containing the representation of the character in ASCII or unicode format
     */
    String getPrintableCharacter(char c) {
        if (c >= 31 && c <= 127)
            return Character.toString(c);
        else
            return  "\\u" + Integer.toHexString(c | 0x10000).substring(1);
    }

    /**
     * Helper method to return the index of the JSON string where an exception occurs, and the characters surrounding
     *  said index (to make fixing it easier). Will show up to 20 characters on each side, or the entire string if it
     *  is 20 characters or less
     * @return a String containing the index of the JSON string where the exception occurs, the characters surrounding it
     */
    String locationErrMsgHelper() {
        int MAX_CHARS_TO_SHOW = 20;
        int leftIndex = Math.min(index, Math.max(0, index - MAX_CHARS_TO_SHOW));
        int rightIndex = Math.min(index + MAX_CHARS_TO_SHOW, jsonLength);

        String location = (jsonLength <= MAX_CHARS_TO_SHOW)
                ? json
                : "||..." + json.substring(leftIndex, rightIndex) + "...||";
        return " (index of " + index + ", location: " + location + ")";
    }


    /*
     * Getters
     */

    /**
     * Returns the current array/object depth that the cursor is located in. A value of 0 means not in a nested array/object,
     *  and a value of -1 means that the parser is near the end of parsing
     * @return the current depth
     */
    public int getDepth() {
        return depth;
    }


    /**
     * Returns the current character of the internal JSON string that the parser object is at
     * @return the current character of the internal JSON string that the parser object is at
     * @throws JsonParserException if the internal index is greater than or equal to the length of the internal JSON string
     */
    public char currentChar() throws JsonParserException {
        throwIfReachedEndPrematurely(0, true, "Reached end of JSON string prematurely (did you forget to close an array/object?)");
        return json.charAt(index);
    }

    /**
     * Gets the last character from the current cursor position that is not whitespace
     * @return a nonwhitespace char value (or a value of 0 if nothing can be found)
     */
    public char previousNonwhitespaceChar() {
        for (int i = index - 1; ; i--) {
            if (i <= 0)
                return 0;

            char c = json.charAt(i);

            if (c == ' ' || c == '\n' || c == '\r' || c == '\t')
                continue;

            return c;
        }
    }

    /**
     * Checks if the cursor has reached the end of the JSON string
     * @param inclusive if true, additionally checks if the cursor is pointing to the last character of the JSON string
     * @return true if the end has been reached, false otherwise
     */
    public boolean atEndOfJson(boolean inclusive) {
        return (inclusive) ? index + 1 >= jsonLength : index + 1 > jsonLength;
    }


    /*
     * Depth
     */

    public void increaseDepth() {
        depth++;
    }

    public void decreaseDepth() {
        depth--;
    }


    /*
     * Cursor moving
     */

    /**
     * Increases the internal index by 1
     * @throws JsonParserException if the internal index is greater than or equal to the length of the internal JSON string
     */
    public void moveCursorFoward() throws JsonParserException {
        throwIfReachedEndPrematurely();
        index++;
    }

    /**
     * Moves the index forward to the first non-whitespace character (space, carriage return, linefeed/newline, tab)
     * @throws JsonParserException if the end of the internal JSON string is reached without encountering a
     *  non-whitespace character
     */
    public void moveToFirstNonWhitespace() throws JsonParserException {
        while (index < jsonLength) {
            char c = json.charAt(index);

            if (c==' ' || c=='\r' || c=='\n' || c=='\t') {
                index++;
                continue;
            }

            return;
        }

        throw new JsonParserException("Reached end of JSON string without encountering a non-whitespace character");
    }


    /*
     * String parsing
     */

    /**
     * Throws an exception if a given character is one of the following control characters:
     * <ul>
     * <li type="circle">backspace (\b)</li>
     * <li type="circle">formfeed (\f)</li>
     * <li type="circle">linefeed (\n)</li>
     * <li type="circle">carriage return (\r)</li>
     * <li type="circle">horizontal tab (\t)</li>
     * </ul>
     * @param c the character to check
     * @throws ValueParserException if c is a control character
     */
    private void checkForControlCharacter(char c) throws ValueParserException {
        char cc = 0;

        switch(c) {
            case '\b': cc = 'b'; break;
            case '\f': cc = 'f'; break;
            case '\n': cc = 'n'; break;
            case '\r': cc = 'r'; break;
            case '\t': cc = 't'; break;
        }

        if (cc > 0)
            throw new ValueParserException("Encountered illegal control character '\\" + cc + "' while collecting a string" +
                    locationErrMsgHelper());
    }

    /**
     * Collects a String value from the internal JSON, starting at the internal index
     * @param enclosingQuote What character delineates the string. Can be a single or double quote
     * @return a String value
     * @throws JsonParserException if the internal index is greater than or equal to the length of the internal JSON string
     */
    public String collectString(char enclosingQuote) throws JsonParserException {
        StringBuilder collectedStr = new StringBuilder().append(enclosingQuote);
        index++;

        while (true) {
            throwIfReachedEndPrematurely();
            char c = json.charAt(index);
            checkForControlCharacter(c); // Prevent control characters in strings

            collectedStr.append(c);
            index++;

            // Escaped characters
            if (c == '\\') {
                char escapeChar = currentChar();
                checkForControlCharacter(escapeChar);

                // Check for legal escapes
                switch (escapeChar) {
                    case '\'':
                    case '"':
                    case '\\':
                    case '/':
                    case 'u':
                    case 'b':
                    case 'f':
                    case 'r':
                    case 'n':
                    case 't':
                        break;
                    default:
                        throw new ValueParserException("Encountered an illegal escape character '" +
                                getPrintableCharacter(escapeChar) + "'" + locationErrMsgHelper());
                }

                switch (escapeChar) {
                    case 'u':
                        throwIfReachedEndPrematurely(5, true, "Attempted to collect a unicode character, reached end of JSON string");

                        collectedStr.deleteCharAt(collectedStr.length() - 1); // Remove backslash
                        index++; // Skip u character
                        collectedStr.append(json, index, index + 4); // Collect next 4 number characters
                        int len = collectedStr.length();

                        // Check that they are hex digits
                        for (int i = 4; i > 0; i--) {
                            char digit = collectedStr.charAt(len - i);

                            if (!(
                                    // 0-9
                                    (digit >= 48 && digit <= 57) ||

                                    // A-F/a-f
                                    ((digit >= 65 && digit <= 70) || (digit >= 97 && digit <= 102))
                            ))
                                throw new ValueParserException("Encountered illegal hex digit while parsing a unicode " +
                                        "value, was '" + digit + "'" + locationErrMsgHelper());
                        }

                        // Convert to hex, then unicode (and remove last 4 number characters)
                        int unum = Integer.parseInt(collectedStr.substring(len - 4, len), 16);
                        collectedStr.delete(len - 4, len);
                        collectedStr.append((char) unum);

                        // Finally, move on
                        index += 4;
                        break;

                    case '\'':
                    case '"':
                        collectedStr.deleteCharAt(collectedStr.length() - 1); // Remove backslash

                    default:
                        collectedStr.append(json.charAt(index++));
                }
            }

            // Terminate string collection
            else if (c == enclosingQuote)
                break;
        }

        return collectedStr.toString().replaceAll("^" + enclosingQuote + "|" + enclosingQuote + "$", "");
    }


    /*
     * Boolean parsing
     */

    /**
     * Collects a Boolean value from the internal JSON, starting at the internal index
     * @return a Boolean value
     * @throws JsonParserException if the internal index is greater than or equal to the length of the internal JSON string
     * @throws ValueParserException if the value collected is not "true" or "false"
     */
    public Boolean collectBoolean() throws JsonParserException {
        int boolLetterCounter = (json.charAt(index) == 't') ? 4 : 5;
        throwIfReachedEndPrematurely(boolLetterCounter, false);

        StringBuilder collectedStr = new StringBuilder().append(json, index, index + boolLetterCounter);

        if (Stream.of("true", "false").noneMatch(bool -> bool.contentEquals(collectedStr)))
            throw new ValueParserException("Failed to collect a boolean value, was '" +
                    collectedStr + "'" + locationErrMsgHelper());

        index += boolLetterCounter;
        return Boolean.valueOf(collectedStr.toString());
    }


    /*
     * Null parsing
     */

    /**
     * Collects a null value from the internal JSON, starting at the internal index
     * @return a null value
     * @throws JsonParserException if the internal index is greater than or equal to the length of the internal JSON string
     * @throws ValueParserException if the value collected is not "null"
     */
    public Object collectNull() throws JsonParserException {
        throwIfReachedEndPrematurely(4, false);

        if (!json.startsWith("null", index))
            throw new ValueParserException("Failed to read null value, " +
                    "found '" + json.substring(index, index +4) + "' instead" + locationErrMsgHelper());

        index += 4;
        return null;
    }


    /*
     * Number parsing
     */
    /**
     * Non-digit characters which upon encountering legally stop number collection
     */
    private final static Set<Character> LEGAL_NUMBER_STOP = Set.of(',', ']', '}', ' ', '\r', '\n', '\t');

    private boolean isDigit(char c) {
        return c >= 48 && c <= 57;
    }

    private boolean isNumberSymbol(char c) {
        return c=='+' || c=='-' || c=='.' || c=='e' || c=='E';
    }

    // Removes extra leading zeroes from a number
    private String removeLeadingZeros(String str) {
        Pattern zeroRemoval = Pattern.compile("^(?:-|\\+)?(0+(?=[1-9])|0+(?=0\\.))");
        Matcher matcher = zeroRemoval.matcher(str);

        if (matcher.find()) {
            if (!options.leadingZeroes)
                throw new NumberParserException("Cannot have leading zeroes in a number" + locationErrMsgHelper());

            str = str.replaceFirst(matcher.group(1), "");
        }

        return str;
    }

    /**
     * Converts a sequence of characters into a Long/Double. Must follow the following format, in order:
     * <li type="circle">may start with a + or -</li>
     * <li type="circle">has 1+ digits</li>
     * <li type="circle">may have a . here -- if so, then must be followed by 1+ digits</li>
     * <li type="circle">may have an e or E character denoting exponent, followed by a + or -, then by 1+ digits</li>
     * @return a Number that is either a Long or a Double
     * @throws NumberParserException if the number being parsed does not follow the above format
     */
    public Number collectNumber() throws JsonParserException {
        int startIndex = index; // For +/- in wrong places
        StringBuilder collector = new StringBuilder();
        StringBuilder eCollector = new StringBuilder();

        char currentChar = 0;
        char prevChar = 0;
        boolean hasDecimal = false;
        boolean afterE = false;

        while (true) {
            // Stop collecting if the end of the JSON string has been reached
            if (index >= jsonLength)
                break;

            currentChar = json.charAt(index);

            // Stop collecting once a non-digit, non-number symbol is reached
            if (!isDigit(currentChar) && !isNumberSymbol(currentChar)) {
                if (currentChar == 'x')
                    throw new NumberParserException("Cannot parse hex numbers" + locationErrMsgHelper());

                else if (!LEGAL_NUMBER_STOP.contains(currentChar))
                    throw new NumberParserException("Encountered the character " + currentChar + " while " +
                            "collecting a number" + locationErrMsgHelper());
                break;
            }

            // +/- in wrong places
            if ((currentChar == '-' || currentChar == '+') && index > startIndex && prevChar != 'E' && prevChar != 'e') {
                String plusMinus = (options.plusAtFront) ? "'+/-'" : "'-'";
                throw new NumberParserException("A " + plusMinus + " can only be placed at the start of a number or after" +
                        " a E/e character" + locationErrMsgHelper());
            }

            // Decimal in wrong places
            if (currentChar == '.') {
                if (hasDecimal)
                    throw new NumberParserException("Cannot have 2 decimal characters in a number" + locationErrMsgHelper());
                else if (afterE)
                    throw new NumberParserException("Exponent cannot get a decimal character" + locationErrMsgHelper());
                else if (prevChar == '+' || prevChar == '-')
                    throw new NumberParserException("Decimal must follow a number, was instead '" + prevChar + "'" +
                            locationErrMsgHelper());
            }

            // Exponent
            if (currentChar == 'E' || currentChar == 'e') {
                // Can only follow digits
                if (!isDigit(prevChar))
                    throw new NumberParserException("Exponent can only follow digits" + locationErrMsgHelper());

                if (!afterE) {
                    afterE = true;
                    eCollector.append("e");
                    prevChar = currentChar;
                    index++;
                    continue;
                }

                throw new NumberParserException("Cannot have two exponent characters in a single number" +
                        locationErrMsgHelper());
            }

            // Append to correct collector
            if (afterE)
                eCollector.append(currentChar);
            else
                collector.append(currentChar);

            if (currentChar == '.')
                hasDecimal = true;

            prevChar = currentChar;
            index++;
        }

        /*
         * Additional checking
         */
        // Digit not followed by number
        if (prevChar == '.' && !isDigit(currentChar))
            throw new NumberParserException("Must have at least 1 digit after a decimal character" + locationErrMsgHelper());

        if (collector.length() == 1 && (collector.charAt(0) == '-' || collector.charAt(0) == '+'))
            throw new NumberParserException("Number cannot consist solely of '" + collector.charAt(0) + "'" +
                    locationErrMsgHelper());

        if (eCollector.length() == 1)
            throw new NumberParserException("Must include number after exponent character" + locationErrMsgHelper());

        char lastEChar = (eCollector.length() > 1) ? eCollector.charAt(eCollector.length() - 1) : 0;
        if (lastEChar == '+' || lastEChar == '-')
            throw new NumberParserException("Exponent cannot only consist of a '+/-' character" + locationErrMsgHelper());

        String numberString = removeLeadingZeros(collector.toString());
        String exponentString = (eCollector.length() == 0) ? "" : removeLeadingZeros(eCollector.deleteCharAt(0).toString());

        // Finally, convert from string
        if (exponentString.isBlank()) {
            if (hasDecimal) // Done this way because of how Java handles autoboxing/unboxing with a ternary operator
                return Double.parseDouble(numberString);
            else
                return Long.parseLong(numberString);

        } else {
            Double expVal = Math.pow(Double.parseDouble(numberString), Long.valueOf(exponentString).doubleValue());

            if (!expVal.isInfinite() && expVal.toString().split("\\.")[1].equals("0"))
                return expVal.longValue();
            else
                return expVal;
        }
    }
}
