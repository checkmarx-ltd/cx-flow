package com.checkmarx.flow.dto.iast.common.model.enums;

import java.io.Serializable;

public enum QueryDisplayType implements Serializable {
    SHOW_ALL_VECTORS,
    REQUEST,
    RESPONSE,
    SINK,
    SIMPLE,
    SINK_FORMATTED,
    RESPONSE_FORMATTED,
    REQUEST_FORMATTED;

    private static final String FORMATTED_SUFFIX = "_FORMATTED";

    public static QueryDisplayType toDisplayType(QueryDisplayType type) {
        final String name = type.name();

        if (name.endsWith(FORMATTED_SUFFIX)) {
            return QueryDisplayType.valueOf(name.replace(FORMATTED_SUFFIX, ""));
        }
        return type;
    }

    public boolean isFormatted() {
        return this.name().endsWith("_FORMATTED");
    }
}
