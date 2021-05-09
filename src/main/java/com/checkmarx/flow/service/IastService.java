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
import com.checkmarx.flow.exception.IastThresholdsSeverityException;
import com.checkmarx.flow.exception.JiraClientException;
import com.checkmarx.flow.utils.ScanUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@Service
public class IastService {

    private final Random random = new Random();

    private final Map<Integer, String> severityToPriority = new HashMap<>();
    private final IastProperties iastProperties;

    private final JiraProperties jiraProperties;

    private final JiraService jiraService;

    private final IastServiceRequests iastServiceRequests;

    @Autowired
    public IastService(JiraProperties jiraProperties,
                       JiraService jiraService,
                       IastProperties iastProperties,
                       IastServiceRequests iastServiceRequests) {
        this.iastProperties = iastProperties;
        this.jiraProperties = jiraProperties;
        this.jiraService = jiraService;
        this.iastServiceRequests = iastServiceRequests;

        checkRequiredParameters();

        severityToPriority.put(0, "Low");
        severityToPriority.put(1, "Low");
        severityToPriority.put(2, "Medium");
        severityToPriority.put(3, "High");
    }

    private void checkRequiredParameters() {
        if (iastProperties == null) {
            log.error("IAST properties doesn't setup.");
            throw new RuntimeException("IAST properties doesn't setup.");
        }
        if (ScanUtils.empty(iastProperties.getUrl())
                || ScanUtils.empty(iastProperties.getUsername())
                || ScanUtils.empty(iastProperties.getPassword())
                || ScanUtils.empty(iastProperties.getManagerPort())
                || ScanUtils.emptyObj(iastProperties.getUpdateTokenSeconds())
                || iastProperties.getFilterSeverity().isEmpty()) {
            log.error("not all IAST properties doesn't setup.");
            throw new RuntimeException("IAST properties doesn't setup.");
        }
        for (Severity severity : Severity.values()) {
            iastProperties.getThresholdsSeverity().putIfAbsent(severity, -1);
        }
    }

    public String generateUniqTag() {
        return "cx-flow-" + LocalDateTime.now() + "-" + Math.abs(random.nextLong());
    }

    public void stopScanAndCreateJiraIssueFromIastSummary(ScanRequest request, String scanTag)
            throws IOException, JiraClientException {
        log.debug("start stopScanAndCreateJiraIssueFromIastSummary with scanTag:" + scanTag);
        Scan scan = null;
        try {
            scan = iastServiceRequests.apiScansScanTagFinish(scanTag);
        } catch (FileNotFoundException e) {
            log.warn("Can't find scan with current tag: " + scanTag, e);
        }

        if (scan == null) {
            return;
        }

        getVulnerabilitiesAndCreateJiraIssue(request, scan);
    }

    public void stopScanAndCreateJiraIssueFromIastSummary(String scanTag) throws IOException, JiraClientException {
        stopScanAndCreateJiraIssueFromIastSummary(null, scanTag);
    }

    private void getVulnerabilitiesAndCreateJiraIssue(ScanRequest request, Scan scan)
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
                            createJiraIssue(scanVulnerabilities, request, scansResultQuery, vulnerability, scan);
                        }
                    }
                }
            }
            thresholdsSeverity(scanVulnerabilities);
        } catch (JiraClientException e) {
            throw new JiraClientException("Can't create Jira issue", e);
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

        String title = scansResultQuery.getName();

        if (scansResultQuery.getUrl() != null) {
            title += ": " + scansResultQuery.getUrl();
        }

        String description = iastProperties.getUrl() + ":" + iastProperties.getManagerPort()
                + "/iast-ui/#!/project/" + scanVulnerabilities.getProjectId()
                + "/scan/" + scanVulnerabilities.getScanId()
                + "?rid=" + scansResultQuery.getResultId()
                + "&vid=" + vulnerability.getId()
                + "\n\nScan Tag: " + scan.getTag();

        if (request != null) {
            if (request.getRepoName() != null) {
                description += "\n Repository: " + request.getRepoName();
            }

            if (request.getBranch() != null) {
                description += "\n Branch: " + request.getBranch();
                title += " [" + request.getBranch() + "]";
            }
        }

        String issueKey;
        try {
            issueKey = jiraService.createIssue(
                    project,
                    title,
                    description,
                    assignee,
                    severityToPriority.get(scansResultQuery.getSeverity().toValue()),
                    issueType);

        } catch (JiraClientException e) {
            throw new JiraClientException("Can't create Jira issue.", e);
        }

        log.info("Create task: " + jiraProperties.getUrl() + "/browse/" + issueKey);
    }
}