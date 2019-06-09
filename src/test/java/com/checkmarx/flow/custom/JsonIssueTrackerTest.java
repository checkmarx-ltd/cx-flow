package com.checkmarx.flow.custom;

import com.checkmarx.flow.exception.MachinaException;
import org.junit.Test;

public class JsonIssueTrackerTest {


    @Test
    public void initWithNullPropertiesNullParameters() {
        JsonIssueTracker jsonIssueTracker = new JsonIssueTracker(null);
        try {
            jsonIssueTracker.init(null, null);
            assert false;
        } catch (MachinaException e) {
            assert true;
        }
    }

    @Test
    public void initNullParameters() {
        JsonProperties jsonProperties = new JsonProperties();
        JsonIssueTracker jsonIssueTracker = new JsonIssueTracker(jsonProperties);
        try {
            jsonIssueTracker.init(null, null);
            assert false;
        } catch (MachinaException e) {
            assert true;
        }
    }

    @Test
    public void completeNullParameters() {
        JsonProperties jsonProperties = new JsonProperties();
        JsonIssueTracker jsonIssueTracker = new JsonIssueTracker(jsonProperties);
        try {
            jsonIssueTracker.complete(null, null);
            assert false;
        } catch (MachinaException e) {
            assert true;
        }
    }

    @Test
    public void getIssueKeyNullParameters() {
        JsonProperties jsonProperties = new JsonProperties();
        JsonIssueTracker jsonIssueTracker = new JsonIssueTracker(jsonProperties);
        String issueKey = jsonIssueTracker.getIssueKey(null, null);
    }

    @Test
    public void getXIssueKeyNullParameters() {
        JsonProperties jsonProperties = new JsonProperties();
        JsonIssueTracker jsonIssueTracker = new JsonIssueTracker(jsonProperties);
        String xIssueKey = jsonIssueTracker.getXIssueKey(null, null);
    }

    @Test
    public void getFalsePositiveLabel() throws MachinaException {
        JsonProperties jsonProperties = new JsonProperties();
        JsonIssueTracker jsonIssueTracker = new JsonIssueTracker(jsonProperties);
        assert jsonIssueTracker.getFalsePositiveLabel() == null;
    }

    @Test
    public void getIssues() throws MachinaException {
        JsonProperties jsonProperties = new JsonProperties();
        JsonIssueTracker jsonIssueTracker = new JsonIssueTracker(jsonProperties);
        assert jsonIssueTracker.getIssues(null) == null;
    }

    @Test
    public void createIssue() throws MachinaException {
        JsonProperties jsonProperties = new JsonProperties();
        JsonIssueTracker jsonIssueTracker = new JsonIssueTracker(jsonProperties);
        assert jsonIssueTracker.createIssue(null, null) == null;
    }
    @Test
    public void closeIssue() throws MachinaException {
        JsonProperties jsonProperties = new JsonProperties();
        JsonIssueTracker jsonIssueTracker = new JsonIssueTracker(jsonProperties);
        jsonIssueTracker.closeIssue(null, null);
    }
    @Test
    public void updateIssue() throws MachinaException {
        JsonProperties jsonProperties = new JsonProperties();
        JsonIssueTracker jsonIssueTracker = new JsonIssueTracker(jsonProperties);
        assert jsonIssueTracker.updateIssue(null, null, null) == null;
    }
    @Test
    public void isIssueClosed() throws MachinaException {
        JsonProperties jsonProperties = new JsonProperties();
        JsonIssueTracker jsonIssueTracker = new JsonIssueTracker(jsonProperties);
        assert !jsonIssueTracker.isIssueClosed(null, null);
    }
    @Test
    public void isIssueOpened() throws MachinaException {
        JsonProperties jsonProperties = new JsonProperties();
        JsonIssueTracker jsonIssueTracker = new JsonIssueTracker(jsonProperties);
        assert !jsonIssueTracker.isIssueOpened(null, null);
    }


}