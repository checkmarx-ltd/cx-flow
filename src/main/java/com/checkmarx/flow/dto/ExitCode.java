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
    BUILD_INTERRUPTED(10);

    private final int value;
}
