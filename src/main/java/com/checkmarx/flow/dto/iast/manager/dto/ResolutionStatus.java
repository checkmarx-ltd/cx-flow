package com.checkmarx.flow.dto.iast.manager.dto;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum ResolutionStatus {

    NOT_RESOLVED(0), RESOLVED(1);

    private static final Map<Integer, ResolutionStatus> MAP = new HashMap<>();

    static {
        for (ResolutionStatus status : ResolutionStatus.values()) {
            MAP.put(status.toValue(), status);
        }
    }

    private final int val;

    ResolutionStatus(int val) {
        this.val = val;
    }

    @JsonValue
    public int toValue() {
        return val;
    }

    @JsonCreator
    public static ResolutionStatus fromValue(Integer val) {
        return MAP.get(val);
    }
}
