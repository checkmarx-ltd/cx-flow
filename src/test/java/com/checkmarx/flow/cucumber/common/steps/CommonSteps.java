package com.checkmarx.flow.cucumber.common.steps;

import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;

public class CommonSteps {

    @Before
    @After
    public void changePropertiesBack() {
        TestUtils.changePropertiesBack();
    }

    @Then("do nothing")
    public void doNothing() {
    }
}
