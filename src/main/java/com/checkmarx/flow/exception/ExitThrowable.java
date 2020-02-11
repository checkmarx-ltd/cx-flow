package com.checkmarx.flow.exception;

public class ExitThrowable extends Throwable {

    private final int exitCode;

    public static void exit(int exitCode) throws ExitThrowable {
        throw new ExitThrowable(exitCode);
    }

    public ExitThrowable(int exitCode) {
        super("Exit Code:" + exitCode);
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }
}
