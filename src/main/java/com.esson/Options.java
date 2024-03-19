package com.esson;

/**
 * Class for setting options for the parser to ignore EMCA-404 standards (such as enclosing a string with a single-quote)
 */
public class Options {
    boolean leadingZeroes; // If a number can have leading zeroes
    boolean plusAtFront; // If a number can start with a + sign
    boolean singleQuoteString; // If a string can be enclosed with single-quotes
    boolean commaAtEnd; // If an array/object can have a single comma at the end

    Options() {
        leadingZeroes = false;
        plusAtFront = false;
        singleQuoteString = false;
        commaAtEnd = false;
    }

    /**
     * Default Options object -- follows the EMCA-404 standard
     */
    public static Options defaultOptions() {
        return new Options();
    }

    /**
     * Creates an Options object allowing a single extra comma to be placed at the end of arrays/objects
     * @return an Options object
     */
    public static Options commaAtEnd() {
        return OptionsBuilder.newBuilder().commaAtEnd().build();
    }

    /**
     * Creates an Options object allowing numbers to have leading zeroes
     * @return an Options object
     */
    public static Options leadingZeroes() {
        return OptionsBuilder.newBuilder().leadingZeroes().build();
    }

    /**
     * Creates an Options object allowing numbers to have a + sign at front
     * @return an Options object
     */
    public static Options plusAtFront() {
        return OptionsBuilder.newBuilder().plusAtFront().build();
    }

    /**
     * Creates an Options object allowing strings to be enclosed with single-quotes
     * @return an Options object
     */
    public static Options singleQuoteString() {
        return OptionsBuilder.newBuilder().singleQuoteString().build();
    }

    /**
     * Creates an Options object with all its possible options for ignoring the EMCA-404 standard enabled
     * @return an Options object
     */
    public static Options mostPermissive() {
        return OptionsBuilder.newBuilder()
                .leadingZeroes()
                .plusAtFront()
                .singleQuoteString()
                .commaAtEnd()
                .build();
    }

    @Override
    public String toString() {
        return "|leadingZeroes: " + leadingZeroes + "|plusAtFront: " + plusAtFront + "|singleQuoteString: " +
                singleQuoteString + "|commaAtEnd: " + commaAtEnd + "|";
    }


    /**
     * Builder class for creating a custom Options object
     */
    public static class OptionsBuilder {
        private boolean leadingZeroes;
        private boolean plusAtFront;
        private boolean singleQuoteString;
        private boolean commaAtEnd;

        private OptionsBuilder() {
        }

        public static OptionsBuilder newBuilder() {
            return new OptionsBuilder();
        }

        /**
         * Enables numbers to have leading zeroes
         * @return a reference to the builder object
         */
        public OptionsBuilder leadingZeroes() {
            leadingZeroes = true;
            return this;
        }

        /**
         * Enables numbers to be prefixed with the + sign
         * @return a reference to the builder object
         */
        public OptionsBuilder plusAtFront() {
            plusAtFront = true;
            return this;
        }

        /**
         * Enables strings to be enclosed with a single-quote
         * @return a reference to the builder object
         */
        public OptionsBuilder singleQuoteString() {
            singleQuoteString = true;
            return this;
        }

        /**
         * Enables arrays and objects to have a single comma at the end
         * @return a reference to the builder object
         */
        public OptionsBuilder commaAtEnd() {
            commaAtEnd = true;
            return this;
        }

        public Options build() {
            Options options = new Options();
            options.leadingZeroes = leadingZeroes;
            options.plusAtFront = plusAtFront;
            options.singleQuoteString = singleQuoteString;
            options.commaAtEnd = commaAtEnd;
            return options;
        }
    }
}
