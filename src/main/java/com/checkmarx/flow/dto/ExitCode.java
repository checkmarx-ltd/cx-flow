package com.checkmarx.flow.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Exit codes for the command line mode.
 * To be filled with the rest of the values.
 */
@Getter
@AllArgsConstructor
public enum ExitCode {
    SUCCESS(0),
    BUILD_INTERRUPTED_INTENTIONALLY(1),
    ARGUMENT_NOT_PROVIDED(2),
    CHECKMARX_EXCEPTION(3),
    BUILD_INTERRUPTED(10),
    BUILD_INTERRUPTED_DUE_TO_THRESHOLDS(11);

    private final int value;
}
