package com.LeggoMahEggo.esson;

/**
 * Interface to be implemented on array/object representations (see {@link JsonList} or {@link JsonMap})
 */
public interface JsonContainer {

    /**
     * Converts the container into an appropriate JSON representation
     * @return the container's contents converted to the JSON format
     */
    String toJsonString();
}
