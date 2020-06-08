package com.checkmarx.flow.custom;

import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.FilenameFormatter;
import com.checkmarx.flow.service.SanitizingFilenameFormatter;
import org.junit.Test;

public class JsonIssueTrackerTest {


    @Test
    public void initWithNullPropertiesNullParameters() {
        JsonIssueTracker jsonIssueTracker = new JsonIssueTracker(null, null);
        try {
            jsonIssueTracker.init(null, null);
            assert false;
        } catch (MachinaException e) {
            assert true;
        }
    }

    @Test
    public void initNullParameters() {
        JsonIssueTracker jsonIssueTracker = getInstance();
        try {
            jsonIssueTracker.init(null, null);
            assert false;
        } catch (MachinaException e) {
            assert true;
        }
    }

    @Test
    public void completeNullParameters() {
        JsonIssueTracker jsonIssueTracker = getInstance();
        try {
            jsonIssueTracker.complete(null, null);
            assert false;
        } catch (MachinaException e) {
            assert true;
        }
    }

    @Test
    public void getIssueKeyNullParameters() {
        JsonIssueTracker jsonIssueTracker = getInstance();
        String issueKey = jsonIssueTracker.getIssueKey(null, null);
    }

    @Test
    public void getXIssueKeyNullParameters() {
        JsonIssueTracker jsonIssueTracker = getInstance();
        String xIssueKey = jsonIssueTracker.getXIssueKey(null, null);
    }

    @Test
    public void getFalsePositiveLabel() throws MachinaException {
        JsonIssueTracker jsonIssueTracker = getInstance();
        assert jsonIssueTracker.getFalsePositiveLabel() == null;
    }

    @Test
    public void getIssues() throws MachinaException {
        JsonIssueTracker jsonIssueTracker = getInstance();
        assert jsonIssueTracker.getIssues(null) == null;
    }

    @Test
    public void createIssue() throws MachinaException {
        JsonIssueTracker jsonIssueTracker = getInstance();
        assert jsonIssueTracker.createIssue(null, null) == null;
    }
    @Test
    public void closeIssue() throws MachinaException {
        JsonIssueTracker jsonIssueTracker = getInstance();
        jsonIssueTracker.closeIssue(null, null);
    }
    @Test
    public void updateIssue() throws MachinaException {
        JsonIssueTracker jsonIssueTracker = getInstance();
        assert jsonIssueTracker.updateIssue(null, null, null) == null;
    }
    @Test
    public void isIssueClosed() {
        JsonIssueTracker jsonIssueTracker = getInstance();
        assert !jsonIssueTracker.isIssueClosed(null, null);
    }
    @Test
    public void isIssueOpened() {
        JsonIssueTracker jsonIssueTracker = getInstance();
        assert !jsonIssueTracker.isIssueOpened(null, null);
    }

    private JsonIssueTracker getInstance() {
        JsonProperties jsonProperties = new JsonProperties();
        FilenameFormatter filenameFormatter = new SanitizingFilenameFormatter();
        return new JsonIssueTracker(jsonProperties, filenameFormatter);
    }
}