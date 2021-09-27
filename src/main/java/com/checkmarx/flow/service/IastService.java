package com.checkmarx.flow.service;

import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.config.IastProperties;
import com.checkmarx.flow.custom.GitHubIssueTracker;
import com.checkmarx.flow.custom.GitLabIssueTracker;
import com.checkmarx.flow.custom.ADOIssueTracker;
import com.checkmarx.flow.custom.IssueTracker;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.iast.manager.dto.ResultInfo;
import com.checkmarx.flow.dto.iast.manager.dto.Scan;
import com.checkmarx.flow.dto.iast.manager.dto.ScanVulnerabilities;
import com.checkmarx.flow.dto.iast.manager.dto.VulnerabilityInfo;
import com.checkmarx.flow.dto.iast.manager.dto.description.VulnerabilityDescription;
import com.checkmarx.flow.dto.iast.ql.utils.Severity;
import com.checkmarx.flow.exception.*;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.dto.ScanResults;
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
import java.util.stream.Collectors;


@Slf4j
@Service
public class IastService {

    private static final String DEFAULT_LANG = "JAVA";

    private final Map<Severity, String> jiraSeverityToPriority = new HashMap<>();

    private final IastProperties iastProperties;

    private final JiraService jiraService;

    private final IastServiceRequests iastServiceRequests;

    private final HelperService helperService;

    private final ADOIssueTracker azureIssueTracker;

    private final ADOProperties adoProperties;

    private final GitHubIssueTracker gitHubIssueTracker;

    private final GitLabIssueTracker gitLabIssueTracker;


    @Autowired
    public IastService(JiraService jiraService,
                       IastProperties iastProperties,
                       IastServiceRequests iastServiceRequests,
                       HelperService helperService,
                       GitHubIssueTracker gitHubIssueTracker,
                       GitLabIssueTracker gitLabIssueTracker,
                       ADOIssueTracker azureIssueTracker,
                       ADOProperties adoProperties) {
        this.jiraService = jiraService;
        this.iastProperties = iastProperties;
        this.iastServiceRequests = iastServiceRequests;
        this.helperService = helperService;
        this.gitLabIssueTracker = gitLabIssueTracker;
        this.gitHubIssueTracker = gitHubIssueTracker;
        this.azureIssueTracker = azureIssueTracker;
        this.adoProperties = adoProperties;

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
        checkRequiredParameters();

        if (request == null) {
            log.error("ScanRequest is null. Something went wrong.");
            throw new IastScanRequestMustProvideException("ScanRequest is null. Something went wrong. Please contact with IAST support.");
        }

        if (request.getBugTracker() == null) {
            log.error("BugTracker is not provide. Please provide a bug tracker");
        }

        Scan scan = finishScan(scanTag);
        createIssue(request, scan);
    }

    private void validateScanTag(String scanTag) {
        //Regex validation for a data having a simple format
        if (!Pattern.matches("^[a-zA-Z0-9\\:\\.\\s\\-]{1,256}$", scanTag)) {
            throw new IastValidationScanTagFailedException("The scan tag is invalid. The scan tag must contain only [a-zA-Z0-9\\:\\.\\s\\-] and the size is less than 256.");
        }
    }

    private Scan finishScan(String scanTag) throws IOException {
        Scan scan = null;
        try {
            scan = iastServiceRequests.apiScansScanTagFinish(scanTag);
        } catch (FileNotFoundException e) {
            log.warn("Can't find scan with current tag: " + scanTag, e);
        }

        return scan;
    }

    private void createIssue(ScanRequest request, Scan scan)
            throws IOException {
        try {
            final ScanVulnerabilities scanVulnerabilities =
                    iastServiceRequests.apiScanVulnerabilities(scan.getScanId());
            List<VulnerabilityInfo> vulnerabilities = scanVulnerabilities.getVulnerabilities();

            for (VulnerabilityInfo vulnerability : vulnerabilities) {
                if (vulnerability.getNewCount() != 0) {
                    final List<ResultInfo> scansResultsQuery =
                            iastServiceRequests.apiScanResults(scan.getScanId(), vulnerability.getId());

                    for (ResultInfo scansResultQuery: scansResultsQuery.stream().filter(scansResultQuery ->
                            scansResultQuery.isNewResult() && filterSeverity(scansResultQuery)).collect(Collectors.toList())) {
                        createIssue(scanVulnerabilities, request, scansResultQuery, vulnerability, scan);
                    }
                }
            }

            thresholdsSeverity(scanVulnerabilities);
        } catch (NotImplementedException e) {
            throw new NotImplementedException(request.getBugTracker().getType().getType() + ". That bug tracker not implemented.");
        } catch (IOException e) {
            throw new IOException("Can't send api request", e);
        }
    }

    private void createIssue(ScanVulnerabilities scanVulnerabilities,
                             ScanRequest request,
                             ResultInfo scansResultQuery,
                             VulnerabilityInfo vulnerability,
                             Scan scan) {
        try {
            Issue issue;
            IssueTracker issueTracker;
            boolean htmlDescription = false;
            switch (request.getBugTracker().getType()) {
                case JIRA:
                    String jiraIssue = postIssueToJira(scanVulnerabilities, request, scansResultQuery, vulnerability, scan);
                    if (jiraService.getJiraProperties() != null) {
                        log.info("Create jira issue: " + jiraService.getJiraProperties().getUrl() + "/browse/" + jiraIssue);
                    }
                    //  jiraService is not an instance of IssueTracker, because of that the "return" here is a shortcut to stop the execution
                    return;
                case GITHUBCOMMIT:
                    issueTracker = gitHubIssueTracker;
                    break;
                case GITLABCOMMIT:
                    issueTracker = gitLabIssueTracker;
                    break;
                case adopull:
                case ADOPULL:
                    issueTracker = azureIssueTracker;
                    htmlDescription = true;
                    request.putAdditionalMetadata(Constants.ADO_ISSUE_BODY_KEY, "Description");
                    request.putAdditionalMetadata(Constants.ADO_ISSUE_KEY, adoProperties.getIssueType());
                    break;
                default:
                    throw new NotImplementedException(request.getBugTracker().getType().getType() + ". That bug tracker not implemented.");
            }

            issue = postIssueToTracker(scanVulnerabilities, request, scansResultQuery, vulnerability, scan, issueTracker, htmlDescription);
            log.info("Create {} issue: {}", request.getBugTracker().getType().getType(), issue.getUrl());

        } catch (MachinaException e) {
            log.error("Problem with creating issue.", e);
        } catch (RuntimeException e) {
            throw new IastBugTrackerClientException("Can't create issue", e);
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

    private Issue postIssueToTracker(ScanVulnerabilities scanVulnerabilities,
                                     ScanRequest request,
                                     ResultInfo scansResultQuery,
                                     VulnerabilityInfo vulnerability,
                                     Scan scan,
                                     IssueTracker issueTracker,
                                     boolean htmlDescription) throws MachinaException {

        ScanResults.XIssue xIssue = generateXIssue(scanVulnerabilities, scansResultQuery, scan, vulnerability, htmlDescription);
        return issueTracker.createIssue(xIssue, request);
    }

    private ScanResults.XIssue generateXIssue(ScanVulnerabilities scanVulnerabilities,
                                              ResultInfo scansResultQuery,
                                              Scan scan,
                                              VulnerabilityInfo vulnerability,
                                              boolean htmlDescription) {
        return ScanResults.XIssue.builder()
                .cwe(scansResultQuery.getCwe().toString())
                .description(generateDescription(vulnerability, scan, htmlDescription))
                .severity(scansResultQuery.getSeverity().getName())
                .link(generateIastLinkToVulnerability(scanVulnerabilities, scansResultQuery, vulnerability))
                .file(scansResultQuery.getUrl())
                .vulnerability(vulnerability.getName())
                .build();
    }

    private String generateDescription(VulnerabilityInfo vulnerability,
                                       Scan scan,
                                       boolean htmlDescription) {
        StringBuilder result = new StringBuilder();
        String lineSeparator = System.lineSeparator();
        if (htmlDescription) {
            lineSeparator = "<br>";
        }

        try {
            VulnerabilityDescription vulnerabilityDescription = iastServiceRequests.apiVulnerabilitiesDescription(vulnerability.getId(), DEFAULT_LANG);
            result.append(vulnerabilityDescription.getRisk()).append(lineSeparator);
        } catch (IOException | RuntimeException e) {
            log.error("Can't get information about vulnerability", e);
        }
        result.append("Scan Tag: ").append(scan.getTag());
        return result.toString();
    }

    private String postIssueToJira(ScanVulnerabilities scanVulnerabilities,
                                   ScanRequest request,
                                   ResultInfo scansResultQuery,
                                   VulnerabilityInfo vulnerability,
                                   Scan scan) throws JiraClientException {

        ScanResults.XIssue xIssue = generateXIssue(scanVulnerabilities, scansResultQuery, scan, vulnerability, false);
        return jiraService.createIssue(xIssue, request);
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

}
