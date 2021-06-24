package com.checkmarx.flow.service;

import com.checkmarx.flow.config.IastProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.iast.manager.dto.ResultInfo;
import com.checkmarx.flow.dto.iast.manager.dto.Scan;
import com.checkmarx.flow.dto.iast.manager.dto.ScanVulnerabilities;
import com.checkmarx.flow.dto.iast.manager.dto.VulnerabilityInfo;
import com.checkmarx.flow.dto.iast.ql.utils.Severity;
import com.checkmarx.flow.exception.*;
import com.checkmarx.flow.utils.ScanUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;


@Slf4j
@Service
public class IastService {

    private final Map<Severity, String> jiraSeverityToPriority = new HashMap<>();

    private final IastProperties iastProperties;

    private final JiraProperties jiraProperties;

    private final JiraService jiraService;

    private final IastServiceRequests iastServiceRequests;

    private final HelperService helperService;

    private final GitHubService gitHubService;

    private final GitLabService gitLabService;

    @Autowired
    public IastService(JiraProperties jiraProperties,
                       JiraService jiraService,
                       IastProperties iastProperties,
                       IastServiceRequests iastServiceRequests,
                       HelperService helperService,
                       GitHubService gitHubService,
                       GitLabService gitLabService) {
        this.jiraProperties = jiraProperties;
        this.jiraService = jiraService;
        this.iastProperties = iastProperties;
        this.iastServiceRequests = iastServiceRequests;
        this.helperService = helperService;
        this.gitHubService = gitHubService;
        this.gitLabService = gitLabService;

        checkRequiredParameters();

        jiraSeverityToPriority.put(Severity.INFO, "Info");
        jiraSeverityToPriority.put(Severity.LOW, "Low");
        jiraSeverityToPriority.put(Severity.MEDIUM, "Medium");
        jiraSeverityToPriority.put(Severity.HIGH, "High");
    }

    private void checkRequiredParameters() {
        if (iastProperties == null) {
            throw new IastPropertiesNotSetupException("IAST properties doesn't setup.");
        }
        if (ScanUtils.empty(iastProperties.getUrl())
                || ScanUtils.empty(iastProperties.getUsername())
                || ScanUtils.empty(iastProperties.getPassword())
                || ScanUtils.empty(iastProperties.getManagerPort())
                || ScanUtils.emptyObj(iastProperties.getUpdateTokenSeconds())
                || iastProperties.getFilterSeverity().isEmpty()) {
            throw new IastPropertiesNotSetupException("not all IAST properties setup.");
        }
        for (Severity severity : Severity.values()) {
            iastProperties.getThresholdsSeverity().putIfAbsent(severity, -1);
        }
    }

    public String generateUniqTag() {
        return "cx-flow-" + LocalDateTime.now() + "-" + helperService.getShortUid();
    }

    public void stopScanAndCreateIssue(ScanRequest request, String scanTag)
            throws IOException, JiraClientException {
        log.debug("start stopScanAndCreateIssueFromIastSummary with scanTag:" + scanTag);
        validateScanTag(scanTag);

        if (request == null) {
            log.error("ScanRequest is null. Something went wrong.");
            throw new IastScanRequestMustProvideException("ScanRequest is null. Something went wrong. Please contact with IAST support.");
        }

        if (request.getBugTracker() == null) {
            log.error("BugTracker is not provide. Please provide a bug tracker");
        }

        Scan scan = null;
        try {
            scan = iastServiceRequests.apiScansScanTagFinish(scanTag);
        } catch (FileNotFoundException e) {
            log.warn("Can't find scan with current tag: " + scanTag, e);
        }

        if (scan == null) {
            return;
        }

        getVulnerabilitiesAndCreateIssue(request, scan);
    }

    private void validateScanTag(String scanTag) {
        //Regex validation for a data having a simple format
        if (!Pattern.matches("[a-zA-Z0-9\\s\\-]{1,256}", scanTag)) {
            throw new IastValidationScanTagFailedException("The scan tag is invalid. The scan tag must contain only [a-zA-Z0-9\\s\\-] and the size is less than 256.");
        }
    }

    private void getVulnerabilitiesAndCreateIssue(ScanRequest request, Scan scan)
            throws IOException, JiraClientException {
        try {
            final ScanVulnerabilities scanVulnerabilities =
                    iastServiceRequests.apiScanVulnerabilities(scan.getScanId());
            List<VulnerabilityInfo> vulnerabilities = scanVulnerabilities.getVulnerabilities();

            for (VulnerabilityInfo vulnerability : vulnerabilities) {
                if (vulnerability.getNewCount() != 0) {
                    final List<ResultInfo> scansResultsQuery =
                            iastServiceRequests.apiScanResults(scan.getScanId(), vulnerability.getId());

                    for (ResultInfo scansResultQuery : scansResultsQuery) {
                        if (scansResultQuery.isNewResult() && filterSeverity(scansResultQuery)) {
                            switch (request.getBugTracker().getType()) {
                                case JIRA:
                                    createJiraIssue(scanVulnerabilities, request, scansResultQuery, vulnerability, scan);
                                    break;
                                case githubissue:
                                case GITHUBISSUE:
                                    createGithubIssue(scanVulnerabilities, request, scansResultQuery, vulnerability, scan);
                                    break;
                                case gitlabissue:
                                case GITLABISSUE:
                                    createGitlabIssue(scanVulnerabilities, request, scansResultQuery, vulnerability, scan);
                                    break;
                                default:
                                    throw new NotImplementedException(request.getBugTracker().getType().getType() + ". That bug tracker not implemented.");

                            }
                        }
                    }
                }
            }

            thresholdsSeverity(scanVulnerabilities);
        } catch (NotImplementedException e) {
            throw new NotImplementedException(request.getBugTracker().getType().getType() + ". That bug tracker not implemented.");
        } catch (RuntimeException e) {
            throw new IastBugTrackerClientException("Can't create issue", e);
        } catch (IOException e) {
            throw new IOException("Can't send api request", e);
        }
    }

    /**
     * create an exception if the severity thresholds are exceeded
     */
    private void thresholdsSeverity(ScanVulnerabilities scanVulnerabilities) {
        Map<Severity, AtomicInteger> thresholdsSeverity = new HashMap<>(7);
        for (Severity severity : Severity.values()) {
            thresholdsSeverity.put(severity, new AtomicInteger(0));
        }
        boolean throwThresholdsSeverity = false;
        for (int i = 0; i < scanVulnerabilities.getVulnerabilities().size(); i++) {
            VulnerabilityInfo vulnerabilityInfo = scanVulnerabilities.getVulnerabilities().get(i);
            int countSeverityVulnerabilities =
                    thresholdsSeverity.get(vulnerabilityInfo.getHighestSeverity()).incrementAndGet();
            Integer countPossibleVulnerability =
                    iastProperties.getThresholdsSeverity().get(vulnerabilityInfo.getHighestSeverity());
            if (countPossibleVulnerability != -1
                    && countSeverityVulnerabilities >= countPossibleVulnerability) {
                throwThresholdsSeverity = true;
            }
        }

        if (throwThresholdsSeverity) {
            log.warn("\nThresholds severity are exceeded. " +
                    "\n High:   " + thresholdsSeverity.get(Severity.HIGH).incrementAndGet() + " / " +
                    iastProperties.getThresholdsSeverity().get(Severity.HIGH) +
                    "\n Medium: " + thresholdsSeverity.get(Severity.MEDIUM).incrementAndGet() + " / " +
                    iastProperties.getThresholdsSeverity().get(Severity.MEDIUM) +
                    "\n Low:    " + thresholdsSeverity.get(Severity.LOW).incrementAndGet() + " / " +
                    iastProperties.getThresholdsSeverity().get(Severity.LOW) +
                    "\n Info:   " + thresholdsSeverity.get(Severity.INFO).incrementAndGet() + " / " +
                    iastProperties.getThresholdsSeverity().get(Severity.INFO));

            throw new IastThresholdsSeverityException();
        }
    }

    /**
     * @return false if not need to create issue
     */
    private boolean filterSeverity(ResultInfo scansResultQuery) {
        return iastProperties.getFilterSeverity().contains(scansResultQuery.getSeverity());
    }

    private void createGithubIssue(ScanVulnerabilities scanVulnerabilities,
                                   ScanRequest request,
                                   ResultInfo scansResultQuery,
                                   VulnerabilityInfo vulnerability,
                                   Scan scan) {

        String title = createIssueTitle(request, scansResultQuery);
        String description = createIssueDescription(scanVulnerabilities, request, scansResultQuery, vulnerability, scan);

        String assignee = null;

        if (request.getBugTracker() != null) {
            BugTracker bugTracker = request.getBugTracker();
            assignee = bugTracker.getAssignee();
        }
        String priority = scansResultQuery.getSeverity().getName();

        gitHubService.createIssue(request, title, description, assignee, priority);
    }

    private void createGitlabIssue(ScanVulnerabilities scanVulnerabilities,
                                   ScanRequest request,
                                   ResultInfo scansResultQuery,
                                   VulnerabilityInfo vulnerability,
                                   Scan scan) {

        String title = createIssueTitle(request, scansResultQuery);
        String description = createIssueDescription(scanVulnerabilities, request, scansResultQuery, vulnerability, scan);

        String assignee = null;

        if (request.getBugTracker() != null) {
            BugTracker bugTracker = request.getBugTracker();
            assignee = bugTracker.getAssignee();
        }
        String priority = scansResultQuery.getSeverity().getName();

        gitLabService.createIssue(request, title, description, assignee, priority);
    }

    private void createJiraIssue(ScanVulnerabilities scanVulnerabilities,
                                 ScanRequest request,
                                 ResultInfo scansResultQuery,
                                 VulnerabilityInfo vulnerability,
                                 Scan scan) throws JiraClientException {
        String assignee;
        String issueType;
        String project;
        if (request != null && request.getBugTracker() != null) {
            BugTracker bugTracker = request.getBugTracker();
            assignee = bugTracker.getAssignee() != null ? bugTracker.getAssignee()
                    : jiraProperties.getUsername();
            issueType = bugTracker.getIssueType() != null ? bugTracker.getIssueType()
                    : jiraProperties.getIssueType();
            project = bugTracker.getProjectKey() != null ? bugTracker.getProjectKey()
                    : jiraProperties.getProject();
        } else {
            assignee = jiraProperties.getUsername();
            issueType = jiraProperties.getIssueType();
            project = jiraProperties.getProject();
        }

        String title = createIssueTitle(request, scansResultQuery);
        String description = createIssueDescription(scanVulnerabilities, request, scansResultQuery, vulnerability, scan);

        String issueKey;
        try {
            issueKey = jiraService.createIssue(
                    project,
                    title,
                    description,
                    assignee,
                    jiraSeverityToPriority.get(scansResultQuery.getSeverity()),
                    issueType);

        } catch (JiraClientException e) {
            throw new JiraClientException("Can't create Jira issue.", e);
        }

        log.info("Create task: " + jiraProperties.getUrl() + "/browse/" + issueKey);
    }

    private String generateIastLinkToVulnerability(ScanVulnerabilities scanVulnerabilities,
                                                   ResultInfo scansResultQuery,
                                                   VulnerabilityInfo vulnerability
    ) {
        return iastProperties.getUrl() + ":" + iastProperties.getManagerPort()
                + "/iast-ui/#!/project/" + scanVulnerabilities.getProjectId()
                + "/scan/" + scanVulnerabilities.getScanId()
                + "?rid=" + scansResultQuery.getResultId()
                + "&vid=" + vulnerability.getId();
    }

    private String createIssueDescription(ScanVulnerabilities scanVulnerabilities,
                                          ScanRequest request,
                                          ResultInfo scansResultQuery,
                                          VulnerabilityInfo vulnerability,
                                          Scan scan) {

        StringBuilder description = new StringBuilder();
        description.append(generateIastLinkToVulnerability(scanVulnerabilities, scansResultQuery, vulnerability)).append("\n\nScan Tag: ").append(scan.getTag());

        description.append("\nSeverity: ").append(scansResultQuery.getSeverity().getName());

        if (request.getRepoName() != null) {
            description.append("\nRepository: ").append(request.getRepoName());
        }

        if (request.getBranch() != null) {
            description.append("\nBranch: ").append(request.getBranch());
        }
        return description.toString();
    }


    private String createIssueTitle(ScanRequest request,
                                    ResultInfo scansResultQuery) {
        StringBuilder title = new StringBuilder();
        title.append(scansResultQuery.getName());

        if (scansResultQuery.getUrl() != null) {
            title.append(" @ ").append(scansResultQuery.getUrl());
        }
        if (request.getBranch() != null) {
            title.append(" [").append(request.getBranch()).append("]");
        }
        return title.toString();
    }

}
