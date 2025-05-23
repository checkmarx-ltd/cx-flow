package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanDetails;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.sdk.config.CxPropertiesBase;
import com.checkmarx.sdk.dto.ScanResults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BugTrackerEventTrigger {

    private static final String SCAN_MESSAGE = "Scan submitted to Checkmarx";
    private static final String SCAN_MESSAGE_INTERACTIVE = "Scan submitted to Checkmarx with Scan ID : ";
    private static final String SCAN_NOT_SUBMITTED_MESSAGE = "Scan not submitted to Checkmarx due to existing Active scan for the same project.";
    private static final String SCAN_FAILED_MESSAGE = "Scan failed due to some error.";

    private final GitLabService gitLabService;
    private final GitHubService gitService;
    private final BitBucketService bbService;
    private final ADOService adoService;
    private final CxPropertiesBase cxProperties;

    public BugTrackerEventTrigger(GitLabService gitLabService, GitHubService gitService, BitBucketService bbService, ADOService adoService, CxScannerService cxScannerService) {
        this.gitLabService = gitLabService;
        this.gitService = gitService;
        this.bbService = bbService;
        this.adoService = adoService;
        this.cxProperties = cxScannerService.getProperties();
    }

    public BugTracker.Type triggerScanStartedEvent(ScanRequest request) {
        boolean eventsWereTriggered = true;
        BugTracker.Type bugTrackerType = request.getBugTracker().getType();

        switch (bugTrackerType) {
            case GITLABMERGE:
                if (gitLabService.isScanSubmittedComment() && request.getScanSubmittedComment()) {
                    gitLabService.sendMergeComment(request, SCAN_MESSAGE,gitLabService.isCommentUpdate());
                }
                gitLabService.startBlockMerge(request);
                break;

            case GITLABCOMMIT:
                if (gitLabService.isScanSubmittedComment() && request.getScanSubmittedComment()) {
                    gitLabService.sendCommitComment(request, SCAN_MESSAGE);
                }
                break;

            case GITHUBPULL:
                if (gitService.isScanSubmittedComment() && request.getScanSubmittedComment()) {
                    gitService.sendMergeComment(request, SCAN_MESSAGE,gitService.isCommentUpdate());
                    gitService.startBlockMerge(request, cxProperties.getUrl());
                }
                break;

            case BITBUCKETPULL:
                if (bbService.isScanSubmittedComment() && request.getScanSubmittedComment()) {
                    bbService.sendMergeComment(request, SCAN_MESSAGE);
                }
                break;

            case BITBUCKETSERVERPULL:
                if (bbService.isScanSubmittedComment() && request.getScanSubmittedComment()) {
                    bbService.sendServerMergeComment(request, SCAN_MESSAGE);
                }
                bbService.setBuildStartStatus(request);
                break;

            case ADOPULL:
                if (adoService.isScanSubmittedComment() && request.getScanSubmittedComment()) {
                    adoService.sendMergeComment(request, SCAN_MESSAGE);
                    adoService.startBlockMerge(request);
                }
                break;

            case JIRA:
            case CUSTOM:
            case NONE:
            case WAIT:
            case wait:
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

    public BugTracker.Type triggerScanStartedEventForInteractive(ScanRequest request,int scanId) {
        boolean eventsWereTriggered = true;
        BugTracker.Type bugTrackerType = request.getBugTracker().getType();

        switch (bugTrackerType) {
            case GITLABMERGE:
                if (gitLabService.isScanSubmittedComment() && request.getScanSubmittedComment()) {
                    gitLabService.sendMergeComment(request, SCAN_MESSAGE,gitLabService.isCommentUpdate());
                }
                gitLabService.startBlockMerge(request);
                break;

            case GITLABCOMMIT:
                if (gitLabService.isScanSubmittedComment() && request.getScanSubmittedComment()) {
                    gitLabService.sendCommitComment(request, SCAN_MESSAGE);
                }
                break;

            case GITHUBPULL:
                if (gitService.isScanSubmittedComment() && request.getScanSubmittedComment()) {
                    if(gitService.getProperties().isEnableAddComment()){
                        gitService.addComment(request, SCAN_MESSAGE_INTERACTIVE+" "+scanId);
                    }else{
                        gitService.sendMergeComment(request, SCAN_MESSAGE_INTERACTIVE+" "+scanId,gitService.isCommentUpdate());
                    }
                    gitService.startBlockMerge(request, cxProperties.getUrl());
                }
                break;

            case BITBUCKETPULL:
                if (bbService.isScanSubmittedComment() && request.getScanSubmittedComment()) {
                    bbService.sendMergeComment(request, SCAN_MESSAGE);
                }
                break;

            case BITBUCKETSERVERPULL:
                if (bbService.isScanSubmittedComment() && request.getScanSubmittedComment()) {
                    bbService.sendServerMergeComment(request, SCAN_MESSAGE);
                }
                bbService.setBuildStartStatus(request);
                break;

            case ADOPULL:
                if (adoService.isScanSubmittedComment() && request.getScanSubmittedComment()) {
                    adoService.sendMergeComment(request, SCAN_MESSAGE);
                    adoService.startBlockMerge(request);
                }
                break;

            case JIRA:
            case CUSTOM:
            case NONE:
            case WAIT:
            case wait:
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

    public void triggerOffScanStartedEvent(ScanRequest scanRequest) {

        boolean eventsWereTriggered = true;

        BugTracker.Type bugTrackerType = scanRequest.getBugTracker().getType();

        switch (bugTrackerType) {
            case GITLABMERGE:
                if (gitLabService.isScanSubmittedComment()) {
                    gitLabService.sendMergeComment(scanRequest, SCAN_FAILED_MESSAGE,gitLabService.isCommentUpdate());
                }
                gitLabService.endBlockMerge(scanRequest);
                break;

            case GITLABCOMMIT:
                if (gitLabService.isScanSubmittedComment()) {
                    gitLabService.sendCommitComment(scanRequest, SCAN_FAILED_MESSAGE);
                }
                break;

            case GITHUBPULL:
                if (gitService.isScanSubmittedComment()) {
                    gitService.sendMergeComment(scanRequest, SCAN_FAILED_MESSAGE,gitService.isCommentUpdate());
                }
                String targetURL = cxProperties.getBaseUrl().concat(GitHubService.CX_USER_SCAN_QUEUE);
                gitService.errorBlockMerge(scanRequest, targetURL, SCAN_FAILED_MESSAGE);
                break;

            case BITBUCKETPULL:
                if (bbService.isScanSubmittedComment()) {
                    bbService.sendMergeComment(scanRequest, SCAN_FAILED_MESSAGE);
                }
                break;

            case BITBUCKETSERVERPULL:
                if (bbService.isScanSubmittedComment()) {
                    bbService.sendServerMergeComment(scanRequest, SCAN_FAILED_MESSAGE);
                }
                String buildName = "Existing Checkmarx Scan in progress.";
                String buildUrl = cxProperties.getBaseUrl().concat(BitBucketService.CX_USER_SCAN_QUEUE);
                bbService.setBuildFailedStatus(scanRequest, buildName, buildUrl, SCAN_FAILED_MESSAGE);
                break;

            case ADOPULL:
                if (adoService.isScanSubmittedComment()) {
                    adoService.sendMergeComment(scanRequest, SCAN_FAILED_MESSAGE);
                    adoService.startBlockMerge(scanRequest);
                    adoService.endBlockMergeFailed(scanRequest);
                }
                break;

            case JIRA:
            case CUSTOM:
                eventsWereTriggered = false;
                break; // No action is needed

            case NONE:
                log.warn("Bug tracker events were not triggered, because bug tracker type is '{}'.", bugTrackerType);
                break;

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

    }




    public void triggerScanNotSubmittedBugTrackerEvent(ScanRequest scanRequest, ScanResults scanResults) {

        boolean eventsWereTriggered = true;
        String description = "Existing scan in progress. Please try again after sometime.";

        BugTracker.Type bugTrackerType = scanRequest.getBugTracker().getType();

        switch (bugTrackerType) {
            case GITLABMERGE:
                if (gitLabService.isScanSubmittedComment()) {
                    gitLabService.sendMergeComment(scanRequest, SCAN_NOT_SUBMITTED_MESSAGE,gitLabService.isCommentUpdate());
                }
                gitLabService.endBlockMerge(scanRequest);
                break;

            case GITLABCOMMIT:
                if (gitLabService.isScanSubmittedComment()) {
                    gitLabService.sendCommitComment(scanRequest, SCAN_NOT_SUBMITTED_MESSAGE);
                }
                break;

            case GITHUBPULL:
                if (gitService.isScanSubmittedComment()) {
                    gitService.sendMergeComment(scanRequest, SCAN_NOT_SUBMITTED_MESSAGE,gitService.isCommentUpdate());
                }
                String targetURL = cxProperties.getBaseUrl().concat(GitHubService.CX_USER_SCAN_QUEUE);
                gitService.errorBlockMerge(scanRequest, targetURL, description);
                break;

            case BITBUCKETPULL:
                if (bbService.isScanSubmittedComment()) {
                    bbService.sendMergeComment(scanRequest, SCAN_NOT_SUBMITTED_MESSAGE);
                }
                break;

            case BITBUCKETSERVERPULL:
                if (bbService.isScanSubmittedComment()) {
                    bbService.sendServerMergeComment(scanRequest, SCAN_NOT_SUBMITTED_MESSAGE);
                }
                String buildName = "Existing Checkmarx Scan in progress.";
                String buildUrl = cxProperties.getBaseUrl().concat(BitBucketService.CX_USER_SCAN_QUEUE);
                bbService.setBuildFailedStatus(scanRequest, buildName, buildUrl, description);
                break;

            case ADOPULL:
                if (adoService.isScanSubmittedComment()) {
                    adoService.sendMergeComment(scanRequest, SCAN_NOT_SUBMITTED_MESSAGE);
                    adoService.startBlockMerge(scanRequest);
                    adoService.endBlockMerge(scanRequest, scanResults, new ScanDetails());
                }
                break;

            case JIRA:
            case CUSTOM:
                eventsWereTriggered = false;
                break; // No action is needed

            case NONE:
                log.warn("Bug tracker events were not triggered, because bug tracker type is '{}'.", bugTrackerType);
                break;

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

    }
}