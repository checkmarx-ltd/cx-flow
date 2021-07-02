package com.checkmarx.flow.controller;

import com.checkmarx.flow.CxFlowRunner;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.iast.CreateIssue;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.exception.IastThatPropertiesIsRequiredException;
import com.checkmarx.flow.exception.JiraClientException;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.IastService;
import com.checkmarx.flow.service.JiraService;
import com.checkmarx.flow.utils.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.atlassian.sal.api.xsrf.XsrfHeaderValidator.TOKEN_HEADER;

@Slf4j
@RestController
@RequestMapping(value = "/iast")
@RequiredArgsConstructor
public class IastController {

    @Autowired
    private IastService iastService;
    @Autowired
    private JiraProperties jiraProperties;
    @Autowired
    private FlowProperties flowProperties;
    @Autowired
    private CxFlowRunner cxFlowRunner;

    @Autowired
    private TokenUtils tokenUtils;
    @Autowired
    private JiraService jiraService;

    @PostMapping(value = {"/generate-tag"})
    public ResponseEntity<EventResponse> generateTag() {
        return ResponseEntity.accepted().body(EventResponse.builder()
                .message(iastService.generateUniqTag())
                .success(true)
                .build());
    }

    @PostMapping(value = {"/stop-scan-and-create-{tracker}-issue/{scanTag}"})
    public ResponseEntity<EventResponse> stopScanAndCreateIssue(
            @PathVariable(value = "scanTag", required = false) String scanTag,
            @PathVariable(value = "tracker", required = false) String bugTrackerName,
            @RequestHeader(value = TOKEN_HEADER) String token,
            @RequestBody CreateIssue body,
            @RequestParam(required = false) Map<String, String> queryParams) {
        //Validate shared API token from header
        tokenUtils.validateToken(token);
        try {
            ScanRequest request;
            switch (bugTrackerName.toLowerCase()) {
                case "jira":
                    request = getJiraScanRequest(body);
                    break;
                case "github":
                case "githubissue":
                    request = getGithubScanRequest(body);
                    break;
                case "azure":
                    request = iastService.getAzureScanRequest(body, queryParams);
                    break;
                default:
                    throw new NotImplementedException(bugTrackerName + ". That bug tracker not implemented.");
            }

            iastService.stopScanAndCreateIssue(request, scanTag);
            return ResponseEntity.accepted().body(EventResponse.builder()
                    .message("OK")
                    .success(true)
                    .build());

        } catch (IOException | JiraClientException | RuntimeException | ExitThrowable e) {
            log.error(e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(EventResponse.builder()
                    .message(e.getMessage())
                    .success(false)
                    .build());
        }
    }

    @GetMapping(value = {"/{tracker}/description/{description}"})
    public List<?> searchIssueByDescription(
            @PathVariable(value = "tracker", required = false) String tracker,
            @PathVariable(value = "description", required = false) String description,
            @RequestParam(required = false) Map<String, String> queryParams
    ) throws MachinaException {
        queryParams.put("description", description);
        return iastService.searchIssueByDescription(tracker, queryParams);
    }

    private ScanRequest getJiraScanRequest(CreateIssue body) throws ExitThrowable {
        String assignee = body.getAssignee() != null ? body.getAssignee()
                : jiraProperties.getUsername();

        BugTracker bt = cxFlowRunner.jiraPropertiesToBugTracker()
                .type(BugTracker.Type.JIRA)
                .assignee(assignee)
                .build();

        return ScanRequest.builder()
                .bugTracker(bt)
                .build();
    }

    private ScanRequest getGithubScanRequest(CreateIssue body) {

        if (body.getAssignee() == null) {
            throw new IastThatPropertiesIsRequiredException("Property \"assignee\" is required");
        }
        if (body.getRepoName() == null) {
            throw new IastThatPropertiesIsRequiredException("Property \"repoName\" is required");
        }
        if (body.getNamespace() == null) {
            throw new IastThatPropertiesIsRequiredException("Property \"namespace\" is required");
        }

        BugTracker.Type bugType = BugTracker.Type.GITHUBISSUE;
        String assignee = body.getAssignee();
        BugTracker bt = BugTracker.builder()
                .type(bugType)
                .assignee(assignee)
                .build();

        return ScanRequest.builder()
                .bugTracker(bt)
                .repoName(body.getRepoName())
                .namespace(body.getNamespace())
                .build();
    }
}
