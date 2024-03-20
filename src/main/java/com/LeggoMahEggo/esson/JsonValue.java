package com.LeggoMahEggo.esson;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Precision;

/**
 * Represents some legal JSON value. Valid types are null, Boolean, String, Long, Double, Integer (cast to Long), JsonList (backed
 *  by an ArrayList), and JsonMap (backed by a LinkedHashMap)
 */
public class JsonValue {
    private Object internal; // Wrapped value
    private final static String[] CONTROL_CHARACTERS_FROM = {"\\n", "\\r", "\\t", "\\f", "\\b", "\\\\"};
    private final static String[] CONTROL_CHARACTERS_TO = {"\n", "\r", "\t", "\f", "\b", "\\"};

    /**
     * Factory method to return a JsonValue object with its internal value set
     * @param value a Boolean, String, Number (Integer/Long/Double), JsonMap, JsonList, or null value
     * @return a JsonValue object
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


    /*
     * Getters
     */

    /**
     * The type of value of the JasonValue's internal object. Mainly used to differentiate between JsonMap and
     *  JsonList values
     */
    public enum ValueType {
        REGULAR, MAP, LIST
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

        return ValueType.REGULAR;
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

        return StringUtils.replaceEach(str, CONTROL_CHARACTERS_FROM, CONTROL_CHARACTERS_TO); // Properly escape escaped characters
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
        if (otherValue.internal instanceof Double && internal instanceof Double)
            return Precision.equals(
                    otherValue.getAsNumber().doubleValue(),
                    getAsNumber().doubleValue(),
                    0.000001d
            );

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
