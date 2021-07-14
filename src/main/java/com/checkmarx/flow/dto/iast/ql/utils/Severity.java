package com.checkmarx.flow.dto.iast.ql.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Severity {
    INFO,
    LOW,
    MEDIUM,
    HIGH;

    @JsonValue
    public int toValue() {
        return ordinal();
    }

    @JsonCreator
    public static Severity fromValue(Integer val) {
        return values()[val];
    }

    public String getName() {
        String name = this.toString().toLowerCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
