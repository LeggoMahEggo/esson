package com.esson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class extends {@link ArrayList}<{@link JsonValue}>
 */
public class JsonList extends ArrayList<JsonValue> {

    /**
     * Converts a List of objects into a JsonList containing JsonValue objects. The following types are permissible to
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
     * @param list the list to convert
     * @return a JsonList
     * @throws ClassCastException if an object in the List is not one of the above types
     */
    @SuppressWarnings("unchecked")
    public static JsonList fromList(List<Object> list) throws ClassCastException {
        JsonList jlist = new JsonList();

        for (Object obj : list) {

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

            if (obj instanceof List<?>)
                jlist.add(JsonValue.valueOf(JsonList.fromList((List<Object>) obj)));

            else if (obj instanceof Map<?, ?>)
                jlist.add(JsonValue.valueOf(JsonMap.fromMap((Map<String,Object>) obj)));

            else
                jlist.add(JsonValue.valueOf(obj));
        }

        return jlist;
    }
}
