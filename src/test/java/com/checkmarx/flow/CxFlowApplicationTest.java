package com.checkmarx.flow;

import org.junit.Test;

import static org.junit.Assert.*;

public class CxFlowApplicationTest {

    @Test
    public void mainWithNullArgs() {
        CxFlowApplication cxFlowApplication = new CxFlowApplication();
        cxFlowApplication.main(null);
    }

    /*@Test
    public void mainWithEmptyListArgs() {
        CxFlowApplication cxFlowApplication = new CxFlowApplication();
        cxFlowApplication.main(new String[]{});
    }*/
}