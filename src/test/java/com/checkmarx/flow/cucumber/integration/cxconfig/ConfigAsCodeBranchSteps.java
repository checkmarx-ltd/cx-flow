package com.checkmarx.flow.cucumber.integration.cxconfig;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.config.ScmConfigOverrider;
import com.checkmarx.flow.controller.GitHubController;
import com.checkmarx.flow.dto.github.PullEvent;
import com.checkmarx.flow.service.*;
import com.checkmarx.sdk.config.CxProperties;
import io.cucumber.java.en.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@RequiredArgsConstructor
public class ConfigAsCodeBranchSteps {
    private static final int BRANCH_ARGUMENT_INDEX = 7;

    private final GitHubProperties gitHubProperties;
    private final FlowProperties flowProperties;
    private final CxProperties cxProperties;
    private final HelperService helperService;
    private final FilterFactory filterFactory;
    private final ConfigurationOverrider configOverrider;
    private final ScmConfigOverrider scmConfigOverrider;

    private String defaultBranch;
    private String actualBranch;

    @Given("use-config-as-code-from-default-branch property in application.yml is set to {string}")
    public void useConfigAsCodeFromDefaultBranch(String useDefaultBranch) {
        boolean parsedPropValue = Boolean.parseBoolean(useDefaultBranch);
        gitHubProperties.setUseConfigAsCodeFromDefaultBranch(parsedPropValue);
    }

    @And("GitHub repo default branch is {string}")
    public void githubRepoDefaultBranchIs(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    @When("GitHub notifies CxFlow that a pull request was created for the {string} branch")
    public void githubNotifiesCxFlow(String srcBranch) {
        log.info("Creating RestTemplate mock.");
        RestTemplate restTemplateMock = mock(RestTemplate.class);
        when(gettingFileFromRepo(restTemplateMock)).thenAnswer(this::interceptConfigAsCodeBranch);

        PullEvent pullEvent = CxConfigSteps.createPullEventDto(srcBranch, defaultBranch, gitHubProperties);

        GitHubController controllerSpy = getGitHubControllerSpy(restTemplateMock);
        CxConfigSteps.sendPullRequest(pullEvent, controllerSpy, srcBranch);
    }

    private GitHubController getGitHubControllerSpy(RestTemplate restTemplateMock) {
        log.info("Creating GitHub controller spy.");

        // Don't start automation.
        FlowService flowServiceMock = mock(FlowService.class);

        GitHubService gitHubService = new GitHubService(restTemplateMock, gitHubProperties, flowProperties, null, null);

        GitHubController gitHubControllerSpy = Mockito.spy(new GitHubController(gitHubProperties,
                flowProperties,
                cxProperties,
                null,
                flowServiceMock,
                helperService,
                gitHubService,
                null,
                filterFactory,
                configOverrider,
                scmConfigOverrider));
        doNothing().when(gitHubControllerSpy).verifyHmacSignature(any(), any(), any());

        return gitHubControllerSpy;
    }

    @Then("CxFlow should get config-as-code from the {string} branch")
    public void cxflowShouldGetConfigAsCodeFromTheBranch(String expectedBranch) {
        assertEquals(expectedBranch,
                actualBranch,
                "CxFlow has tried to get config-as-code file from an incorrect branch.");
    }

    private static ResponseEntity<String> gettingFileFromRepo(RestTemplate restTemplateMock) {
        return restTemplateMock.exchange(anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                ArgumentMatchers.<Class<String>>any(),
                anyString(),
                anyString(),
                anyString(),
                anyString());   // expecting branch name here
    }

    private Object interceptConfigAsCodeBranch(InvocationOnMock invocation) {
        assertEquals(BRANCH_ARGUMENT_INDEX + 1,
                invocation.getArguments().length,
                "Unexpected argument count for the restTemplate call.");

        actualBranch = invocation.getArgument(BRANCH_ARGUMENT_INDEX);
        return null;
    }
}
