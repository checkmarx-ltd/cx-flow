package com.checkmarx.flow.cucumber.integration.jira.autoconfig;


import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.properties.JiraProperties;
import io.cucumber.java.en.Then;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;



@SpringBootTest(classes = {CxFlowApplication.class})
public class JiraAutoConfigTestSteps {

    @Autowired
    private JiraProperties jiraProperties;

    @Then("we should have open and closed statuses in jira properties bean")
    public void verifyJiraClosedAndOpenStatus() {
        List<String> closedStatus = jiraProperties.getClosedStatus();
        Assert.assertNotNull(closedStatus);
        Assert.assertFalse(closedStatus.isEmpty());
    }


}
