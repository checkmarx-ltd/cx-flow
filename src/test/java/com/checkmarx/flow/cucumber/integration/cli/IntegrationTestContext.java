package com.checkmarx.flow.cucumber.integration.cli;

import com.checkmarx.flow.CxFlowRunner;
import com.checkmarx.flow.custom.JsonProperties;
import org.springframework.stereotype.Component;

/**
 * State that is passed between different step implementation classes for the 'parse' feature.
 */
@Component
public class IntegrationTestContext {

    private final CxFlowRunner cxFlowRunner;

    /**
     * Used in scenarios that check error handling.
     */
    private Throwable cxFlowExecutionException;

    public IntegrationTestContext(CxFlowRunner cxFlowRunner, JsonProperties jsonProperties) {
        this.cxFlowRunner = cxFlowRunner;
    }

    public void reset() {
        cxFlowExecutionException = null;
    }

    public CxFlowRunner getCxFlowRunner() {
        return cxFlowRunner;
    }

    public void setCxFlowExecutionException(Throwable cxFlowExecutionException) {
        this.cxFlowExecutionException = cxFlowExecutionException;
    }

    public Throwable getCxFlowExecutionException() {
        return cxFlowExecutionException;
    }

}