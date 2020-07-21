package com.checkmarx.flow.cucumber.integration.cli;

import com.checkmarx.flow.CxFlowRunner;
import com.checkmarx.flow.custom.JsonProperties;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

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
