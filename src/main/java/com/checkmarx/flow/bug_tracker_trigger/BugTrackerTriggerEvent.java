package com.checkmarx.flow.bug_tracker_trigger;

import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.ADOService;
import com.checkmarx.flow.service.BitBucketService;
import com.checkmarx.flow.service.GitHubService;
import com.checkmarx.flow.service.GitLabService;
import com.checkmarx.sdk.config.CxProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BugTrackerTriggerEvent {

    private static final String SCAN_MESSAGE = "Scan submitted to Checkmarx";

    private final GitLabService gitLabService;
    private final GitHubService gitService;
    private final BitBucketService bbService;
    private final ADOService adoService;
    private final CxProperties cxProperties;

    public BugTracker.Type triggerBugTrackerEvent(ScanRequest request) {
        boolean eventsWereTriggered = true;
        BugTracker.Type bugTrackerType = request.getBugTracker().getType();

        switch (bugTrackerType) {
            case GITLABMERGE:
                gitLabService.sendMergeComment(request, SCAN_MESSAGE);
                gitLabService.startBlockMerge(request);
                break;

            case GITLABCOMMIT:
                gitLabService.sendCommitComment(request, SCAN_MESSAGE);
                break;

            case GITHUBPULL:
                gitService.sendMergeComment(request, SCAN_MESSAGE);
                gitService.startBlockMerge(request, cxProperties.getUrl());
                break;

            case BITBUCKETPULL:
                bbService.sendMergeComment(request, SCAN_MESSAGE);
                break;

            case BITBUCKETSERVERPULL:
                bbService.sendServerMergeComment(request, SCAN_MESSAGE);
                bbService.setBuildStartStatus(request);
                break;

            case ADOPULL:
                adoService.sendMergeComment(request, SCAN_MESSAGE);
                adoService.startBlockMerge(request);
                break;

            case JIRA:
            case CUSTOM:
                eventsWereTriggered = false;
                break; // No action is needed

            default:
                eventsWereTriggered = false;
                log.warn("Bug-Tracker type: {} is not supported", bugTrackerType);
        }

        if (eventsWereTriggered) {
            log.debug("Completed triggering events for the '{}' bug tracker.", bugTrackerType);
        }
        else {
            log.debug("Bug tracker events were not triggered, because bug tracker type is '{}'.", bugTrackerType);
        }

        return bugTrackerType;
    }
}