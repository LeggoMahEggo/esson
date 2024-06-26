/*
 Copyright 2024 Yehuda Broderick
 */
/*
 This file is part of esson.

 esson is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License
  as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

 esson is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License along with esson. If not, see
  <https://www.gnu.org/licenses/>.
 */
package com.LeggoMahEggo.esson;

/**
 * Represents some legal JSON value, wrapping a Java value. Valid types are null, Boolean, String, Long, Double, Integer
 * (cast to Long), JsonList (backed by an ArrayList), and JsonMap (backed by a LinkedHashMap)
 */
public class JsonValue {
    Object internal; // Wrapped value
    private final static String[] CONTROL_CHARACTERS_FROM = {"\\n", "\\r", "\\t", "\\f", "\\b"};
    private final static String[] CONTROL_CHARACTERS_TO = {"\n", "\r", "\t", "\f", "\b"};

    /**
     * Factory method to return a JsonValue object with its internal value set
     * @param value a Boolean, String, Integer, Long, Double, JsonMap, JsonList, or a null value
     * @return a JsonValue object
     * @throws IllegalArgumentException if value is not a supported JSON value
     */
    public static JsonValue valueOf(Object value) throws IllegalArgumentException {
        // Make sure value is legal JSON value
        if (value != null &&
                !(value instanceof String) &&
                !(value instanceof Integer) &&
                !(value instanceof Long) &&
                !(value instanceof Double) &&
                !(value instanceof Boolean) &&
                !(value instanceof JsonList) &&
                !(value instanceof JsonMap)
        )
            throw new IllegalArgumentException("Illegal value to wrap: must be a null, String, Boolean, Integer, Long, " +
                    "Double, JsonList, or JsonMap (was of type " + value.getClass().getSimpleName() + ")");

        JsonValue jv = new JsonValue();
        jv.internal = (value instanceof Integer) ? Long.valueOf((Integer) value) : value;
        return jv;
    }

    private void throwIfNotClass(Class<?> clazz) {
        if (!clazz.isInstance(internal)) {
            String isOf = (internal == null) ? "a null" : "an instance of " + internal.getClass().getSimpleName();

            throw new ClassCastException("Tried to get a " + clazz.getSimpleName() + " value from JsonValue, but the " +
                    "internal object is " + isOf);
        }
    }

    /**
     * Escapes a String according to JSON specifications
     * @param str the String to escape
     * @return a String escaped according to JSON specifications
     */
    static String escapeString(String str) {
        // Was using StringUtils.replaceEach, removed it to avoid multiple licensing headaches
        for (int i = 0; i < CONTROL_CHARACTERS_TO.length; i++)
            str = str.replace(CONTROL_CHARACTERS_TO[i], CONTROL_CHARACTERS_FROM[i]);

        return str.replace("\"", "\\\"");
    }


    /*
     * Getters
     */

    /**
     * The type of value of the JasonValue's internal object. Mainly used to differentiate between JsonMap and
     *  JsonList values
     */
    public enum ValueType {
        LIST, MAP, OTHER
    }

    /**
     * Returns if the internal object is a JsonMap, a JsonList, or any other type of value (includes null). Useful for
     *  traversing the JSON structure
     * @return the ValueType of the internal object
     */
    public ValueType getValueType() {
        if (internal instanceof JsonMap)
            return ValueType.MAP;

        if (internal instanceof JsonList)
            return ValueType.LIST;

        return ValueType.OTHER;
    }

    /**
     * Returns true if the internal object is null
     * @return true if the internal object is null, false otherwise
     */
    public boolean isNullValue() {
        return internal == null;
    }

    /**
     * Returns the internal object as a String
     * @return the internal object cast to a String
     * @throws ClassCastException if the internal object is null or not an instance of String
     */
    public String getAsString() throws ClassCastException {
        throwIfNotClass(String.class);
        String str = (String) internal;

        if (!str.contains("\\"))
            return str;

        // Was using StringUtils.replaceEach, removed it to avoid multiple licensing headaches
        for (int i = 0; i < CONTROL_CHARACTERS_FROM.length; i++)
            str = str.replace(CONTROL_CHARACTERS_FROM[i], CONTROL_CHARACTERS_TO[i]);

        return str;
    }

    /**
     * Returns the internal object as a Boolean
     * @return the internal object cast to a Boolean
     * @throws ClassCastException if the internal object is null or not an instance of Boolean
     */
    public Boolean getAsBoolean() throws ClassCastException {
        throwIfNotClass(Boolean.class);
        return (Boolean) internal;
    }

    /**
     * Returns the internal object as a Number
     * @return the internal object cast to a Number
     * @throws ClassCastException if the internal object is null or not an instance of Number
     */
    public Number getAsNumber() throws ClassCastException {
        throwIfNotClass(Number.class);
        return (Number) internal;
    }

    /**
     * Returns the internal object as a JsonMap
     * @return the internal object cast to a JsonMap
     * @throws ClassCastException if the internal object is null or not an instance of JsonMap
     */
    public JsonMap getAsMap() {
        throwIfNotClass(JsonMap.class);
        return (JsonMap) internal;
    }

    /**
     * Returns the internal object as a JsonList
     * @return the internal object cast to a JsonList
     * @throws ClassCastException if the internal object is null or not an instance of JsonList
     */
    public JsonList getAsList() {
        throwIfNotClass(JsonList.class);
        return (JsonList) internal;
    }


    /*
     * Overriden methods
     */

    @Override
    public boolean equals(Object other) {
        if (other == null)
            return false;

        if (other == this)
            return true;

        if (!(other instanceof JsonValue))
            return false;

        JsonValue otherValue = (JsonValue) other;

        // Null values
        if (otherValue.internal == null && internal == null)
            return true;

        if (otherValue.internal == null ^ internal == null)
            return false;

        // String
        if (otherValue.internal instanceof String && internal instanceof String)
            return otherValue.getAsString().equals(getAsString());

        // Boolean
        if (otherValue.internal instanceof Boolean && internal instanceof Boolean)
            return otherValue.getAsBoolean() == getAsBoolean();

        // Long
        if (otherValue.internal instanceof Long && internal instanceof Long)
            return otherValue.getAsNumber().longValue() == getAsNumber().longValue();

        // Double
        if (otherValue.internal instanceof Double && internal instanceof Double) {
            // Was using Precision.equals, removed it to avoid multiple licensing headaches
            double epsilon = 0.000001d;
            Double double1 = otherValue.getAsNumber().doubleValue();
            Double double2 = getAsNumber().doubleValue();

            // Infinite values
            if (double1.isInfinite() || double2.isInfinite())
                return false;

            // Not a number
            if (double1.isNaN() || double2.isNaN())
                return false;

            return Math.abs(double1 - double2) < epsilon;
        }

        // List
        if (otherValue.internal instanceof JsonList && internal instanceof JsonList)
            return otherValue.getAsList().equals(getAsList());

        // Map
        if (otherValue.internal instanceof JsonMap && internal instanceof JsonMap)
            return otherValue.getAsMap().equals(getAsMap());

        // Unequal types
        return false;
    }

    /**
     * As Object.toString, except that String objects are wrapped with '', and null values are returned as "null"
     * @return a String representation of the internal object
     */
    @Override
    public String toString() {
        if (internal == null)
            return "null";

        if (internal instanceof String)
            return "'" + internal + "'";

        return internal.toString();
    }
}
