package com.checkmarx.flow.exception;

public class MachinaException extends Exception {

    private Step step;

    public MachinaException(){ super(); }

    public MachinaException(String message) {
        super(message);
    }

    public MachinaException(String message, Throwable t) {
        super(message, t);
    }

    public Step getStep() {
        return this.step;
    }

    public void setStep(Step step) {
        this.step = step;
    }

    public enum Step {
        SUBMITTED("SUBMITTED"),
        SCANNING("SCANNING"),
        SCAN_COMPLETE("SCAN_COMPLETE"),
        BUG_TRACKING_STARTED("BUG_TRACKING_STARTED"),
        BUG_TRACKING_COMPLETE("BUG_TRACKING_COMPLETE");

        private String step;

        Step(String step) {
            this.step = step;
        }

        public String getStep() {
            return step;
        }
    }

}
