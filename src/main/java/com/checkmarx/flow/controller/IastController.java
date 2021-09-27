package com.checkmarx.flow.controller;

import com.checkmarx.flow.CxFlowRunner;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.iast.CreateIssue;
import com.checkmarx.flow.exception.IastThatPropertiesIsRequiredException;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.exception.JiraClientException;
import com.checkmarx.flow.service.IastService;
import com.checkmarx.flow.utils.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.security.InvalidParameterException;

import static com.atlassian.sal.api.xsrf.XsrfHeaderValidator.TOKEN_HEADER;

@Slf4j
@RestController
@RequestMapping(value = "/iast")
@RequiredArgsConstructor
public class IastController {

    @Autowired
    private IastService iastService;
    @Autowired
    private CxFlowRunner cxFlowRunner;
    @Autowired
    private TokenUtils tokenUtils;

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
            @RequestBody @Valid CreateIssue body) {

        HttpStatus httpStatusReturn = HttpStatus.OK;
        String returnMessage = "OK";
        try {
            //Validate shared API token from header
            tokenUtils.validateToken(token);

            if (Strings.isBlank(bugTrackerName.trim())) {
                throw new InvalidParameterException("tracker parameter cannot be empty.");
            }

            if (Strings.isBlank(scanTag)) {
                throw new InvalidParameterException("scanTag parameter cannot be empty.");
            }

            ScanRequest request;
            BugTracker.Type bugTrackerType;
            switch (bugTrackerName.toLowerCase()) {
                case "jira":
                    bugTrackerType = BugTracker.Type.JIRA;
                    break;
                case "github":
                    bugTrackerType = BugTracker.Type.GITHUBCOMMIT;
                    break;
                case "gitlab":
                    bugTrackerType = BugTracker.Type.GITLABCOMMIT;
                    break;
                case "ado":
                case "azure":
                    bugTrackerType = BugTracker.Type.ADOPULL;
                    break;
                default:
                    throw new NotImplementedException(bugTrackerName + ". That bug tracker not implemented.");
            }

            request = getRepoScanRequest(body, bugTrackerType);

            iastService.stopScanAndCreateIssue(request, scanTag);
        } catch (InvalidTokenException e) {
            log.error(e.getMessage(), e);
            returnMessage = e.getMessage();
            httpStatusReturn = HttpStatus.FORBIDDEN;
        } catch (InvalidParameterException | NotImplementedException e) {
            log.error(e.getMessage(), e);
            returnMessage = e.getMessage();
            httpStatusReturn = HttpStatus.BAD_REQUEST;
        } catch (IOException | JiraClientException | RuntimeException e) {
            log.error(e.getMessage(), e);
            returnMessage = e.getMessage();
            httpStatusReturn = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(httpStatusReturn).body(EventResponse.builder()
                .message(returnMessage)
                .success(httpStatusReturn == HttpStatus.OK)
                .build());
    }

    private ScanRequest getRepoScanRequest(CreateIssue body, BugTracker.Type tracker) {

        checksForGitHub(body, tracker);
        checksForGitLab(body, tracker);
        checksForAzure(body, tracker);

        String assignee = body.getAssignee();
        BugTracker bt;
        if (BugTracker.Type.JIRA == tracker) {
            bt = cxFlowRunner.jiraPropertiesToBugTracker()
                    .type(BugTracker.Type.JIRA)
                    .assignee(assignee)
                    .build();
        } else {
            bt = BugTracker.builder()
                    .type(tracker)
                    .assignee(assignee)
                    .build();
        }

        String altFields = null;
        if(tracker == BugTracker.Type.ADOPULL || tracker == BugTracker.Type.adopull ) {
            if (!Strings.isEmpty(assignee)) {
                altFields = "System.AssignedTo:" + assignee;
            }
        }

        return ScanRequest.builder()
                .bugTracker(bt)
                .altProject(body.getBugTrackerProject())
                .repoName(body.getRepoName())
                .altFields(altFields)
                .namespace(body.getNamespace())
                .repoProjectId(body.getProjectId())
                .product(ScanRequest.Product.CX)
                .build();
    }

    private void checksForGitLab(CreateIssue body, BugTracker.Type tracker) {
        if (tracker == BugTracker.Type.GITLABCOMMIT && body.getProjectId() == null) {
            throw new IastThatPropertiesIsRequiredException("Property \"project-id\" is required");
        }
    }

    private void checksForAzure(CreateIssue body, BugTracker.Type tracker) {
        if (tracker == BugTracker.Type.ADOPULL) {
            if (body.getBugTrackerProject() == null) {
                throw new IastThatPropertiesIsRequiredException("Property \"bugTrackerProject\" is required");
            }
            if (body.getNamespace() == null) {
                throw new IastThatPropertiesIsRequiredException("Property \"namespace\" is required");
            }
        }
    }


    private void checksForGitHub(CreateIssue body, BugTracker.Type tracker) {
        if (tracker == BugTracker.Type.GITHUBCOMMIT) {
            if (body.getRepoName() == null) {
                throw new IastThatPropertiesIsRequiredException("Property \"repoName\" is required");
            }
            if (body.getNamespace() == null) {
                throw new IastThatPropertiesIsRequiredException("Property \"namespace\" is required");
            }
        }
    }

}
