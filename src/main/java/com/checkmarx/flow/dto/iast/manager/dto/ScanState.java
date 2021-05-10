package com.checkmarx.flow.dto.iast.manager.dto;

import lombok.Getter;

@Getter
public enum ScanState {
    STATE_RUNNING(1),
    STATE_FINISHING(3),
    STATE_COMPLETED(2);

    private int stateNum;

    ScanState(int stateNum) {
        this.stateNum = stateNum;
    }
}
