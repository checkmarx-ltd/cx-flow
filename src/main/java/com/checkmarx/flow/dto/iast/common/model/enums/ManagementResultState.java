package com.checkmarx.flow.dto.iast.common.model.enums;

import lombok.Getter;

import java.io.Serializable;

public enum ManagementResultState implements Serializable {
    TO_VERIFY("To Verify"),
    CONFIRMED("Confirmed"),
    SUSPICIOUS("Suspicious"),
    NOT_A_PROBLEM("Not a Problem"),
    REMEDIATED("Remediated");

    @Getter
    private final String displayName;

    ManagementResultState(String displayName) {
        this.displayName = displayName;
    }
}
