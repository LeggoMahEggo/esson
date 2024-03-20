package com.LeggoMahEggo.esson;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class extends {@link LinkedHashMap}<{@link String},{@link JsonValue}>
 */
public class JsonMap extends LinkedHashMap<String, JsonValue> {

    /**
     * Converts a Map of objects into a JsonMap containing JsonValue objects. The following types are permissible to
     *  convert:
     * <ul>
     * <li type="circle">null</li>
     * <li type="circle">String</li>
     * <li type="circle">Boolean</li>
     * <li type="circle">Integer (will be converted to Long)</li>
     * <li type="circle">Long</li>
     * <li type="circle">Double</li>
     * <li type="circle">List (of any of the above types, including the Map listed next)</li>
     * <li type="circle">Map (key of type String and a value of any of the above types)</li>
     * </ul>
     * @param map the map to convert
     * @return a JsonList
     * @throws ClassCastException if an object in the Map is not one of the above types
     */
    @SuppressWarnings("unchecked")
    public static JsonMap fromMap(Map<String, Object> map) {
        JsonMap jmap = new JsonMap();

        for (Entry<String, ?> entry : map.entrySet()) {
            Object obj = entry.getValue();

            // Make sure value is legal JsonValue
            if (obj != null &&
                !(obj instanceof String) &&
                !(obj instanceof Integer) &&
                !(obj instanceof Long) &&
                !(obj instanceof Double) &&
                !(obj instanceof Boolean) &&
                !(obj instanceof List<?>) &&
                !(obj instanceof Map<?, ?>)
            )
                throw new ClassCastException("Illegal value to convert: must be a null, String, Boolean, Integer, Long, " +
                        "Double, or a List or a Map<String, ?> of those types");

            // Convert to JsonMap
            String key = entry.getKey();

            if (obj instanceof List<?>)
                jmap.put(key, JsonValue.valueOf(JsonList.fromList((List<Object>) obj)));

            else if (obj instanceof Map<?, ?>)
                jmap.put(key, JsonValue.valueOf(JsonMap.fromMap((Map<String,Object>) obj)));

            else
                jmap.put(entry.getKey(), JsonValue.valueOf(obj));
        }

        return jmap;
    }
}
