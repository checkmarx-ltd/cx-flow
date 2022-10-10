
package com.checkmarx.flow.jira9X;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;



public enum StandardOperation {

    SET("set"),
    ADD("add"),
    REMOVE("remove"),
    EDIT("edit");
    private final String value;
    private final static Map<String, StandardOperation> CONSTANTS = new HashMap<String, StandardOperation>();

    static {
        for (StandardOperation c: values()) {
            CONSTANTS.put(c.value, c);
        }
    }

    StandardOperation(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    @JsonValue
    public String value() {
        return this.value;
    }

    @JsonCreator
    public static StandardOperation fromValue(String value) {
        StandardOperation constant = CONSTANTS.get(value);
        if (constant == null) {
            throw new IllegalArgumentException(value);
        } else {
            return constant;
        }
    }

}
