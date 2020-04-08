package com.checkmarx.flow.exception;

import com.checkmarx.flow.dto.ExitCode;

public class ExitThrowable extends Throwable {

    private final int exitCode;

    public static void exit(int exitCode) throws ExitThrowable {
        throw new ExitThrowable(exitCode);
    }

    public static void exit(ExitCode code) throws ExitThrowable {
        exit(code.getValue());
    }

    public ExitThrowable(int exitCode) {
        super("Exit Code:" + exitCode);
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }
}
