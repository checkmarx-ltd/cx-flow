package com.checkmarx.flow.controller;

import org.junit.Test;

import static org.junit.Assert.*;

public class FlowControllerTest {

    private static final FlowController flowController = new FlowController(null, null);

    @Test
    public void tmpWithNullParameters() {
        String tmp = flowController.tmp(null, null);
        assert tmp.equals("xyz");
    }

    @Test
    public void tmpWithNullToken() {
        String tmp = flowController.tmp(null, "body");
        assert tmp.equals("xyz");
    }

    @Test
    public void tmpWithNullBody() {
        String tmp = flowController.tmp("token", null);
        assert tmp.equals("xyz");
    }

    @Test
    public void tmpWithParameters() {
        String tmp = flowController.tmp("token", "body");
        assert tmp.equals("xyz");
    }
}