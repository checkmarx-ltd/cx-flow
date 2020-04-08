package com.checkmarx.flow.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
// No args - to avoid deserialization error.
@NoArgsConstructor
public class OperationResult {
    private OperationStatus status;
    private String message;

    public static OperationResult successful() {
        return new OperationResult(OperationStatus.SUCCESS, null);
    }
}
