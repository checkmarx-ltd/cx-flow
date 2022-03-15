package com.checkmarx.flow.cucumber.integration.jira.manualconfig;


import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.properties.JiraProperties;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {CxFlowApplication.class})
@ActiveProfiles({ "jiramanualconfig" })
public class JiraManualConfigTestSteps {

    @Autowired
    private JiraProperties jiraProperties;


    @Given("There are values for jira closed and open statuses in yml")
    public void dummy() {

    }

    @Then("we should use te values from yml")
    public void verifyOpenAndClosedStatuses() {
        Assert.assertNotNull("Did not load closed statuses from YML (null)",jiraProperties.getClosedStatus());
        Assert.assertNotNull("Did not load open statuses from YML",jiraProperties.getOpenStatus());
        Assert.assertTrue("Did not load closed statuses from YML (empty list)",jiraProperties.getClosedStatus().size() > 0);
        Assert.assertTrue("Did not load open statuses from YML (empty list)",jiraProperties.getOpenStatus().size() > 0);
        for (String status: jiraProperties.getOpenStatus()) {
            Assert.assertTrue(status.contains("TEST-OPEN"));
        }
        for (String status: jiraProperties.getClosedStatus()) {
            Assert.assertTrue(status.contains("TEST-CLOSED"));
        }
    }
}
