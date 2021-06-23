package com.checkmarx.flow.service;

import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.config.IastProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.custom.ADOIssueTracker;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.iast.manager.dto.ResultInfo;
import com.checkmarx.flow.dto.iast.manager.dto.Scan;
import com.checkmarx.flow.dto.iast.manager.dto.ScanVulnerabilities;
import com.checkmarx.flow.dto.iast.manager.dto.VulnerabilityInfo;
import com.checkmarx.flow.dto.iast.ql.utils.Severity;
import com.checkmarx.flow.exception.IastPropertiesNotSetupException;
import com.checkmarx.flow.exception.IastThresholdsSeverityException;
import com.checkmarx.flow.exception.IastValidationScanTagFailedException;
import com.checkmarx.flow.exception.JiraClientException;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Slf4j
@Service
public class IastService {

    private final Map<Integer, String> severityToPriority = new HashMap<>();

    private final IastProperties iastProperties;

    private final JiraProperties jiraProperties;

    private final JiraService jiraService;

    private final IastServiceRequests iastServiceRequests;

    private final HelperService helperService;

    private final ADOIssueTracker azureService;

    private final ADOProperties adoProperties;

    @Autowired
    public IastService(JiraProperties jiraProperties,
                       JiraService jiraService,
                       IastProperties iastProperties,
                       IastServiceRequests iastServiceRequests,
                       HelperService helperService,
                       ADOIssueTracker azureService,
                       ADOProperties adoProperties) {
        this.jiraProperties = jiraProperties;
        this.jiraService = jiraService;
        this.iastProperties = iastProperties;
        this.iastServiceRequests = iastServiceRequests;
        this.helperService = helperService;
        this.azureService = azureService;
        this.adoProperties = adoProperties;

        checkRequiredParameters();

        severityToPriority.put(0, "Low");
        severityToPriority.put(1, "Low");
        severityToPriority.put(2, "Medium");
        severityToPriority.put(3, "High");
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

    public void stopScanAndCreateJiraIssueFromIastSummary(ScanRequest request, String scanTag)
            throws IOException, JiraClientException {
        log.debug("start stopScanAndCreateJiraIssueFromIastSummary with scanTag:" + scanTag);
        validateScanTag(scanTag);
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

    private void validateScanTag(String scanTag) {
        //Regex validation for a data having a simple format
        if (!Pattern.matches("[a-zA-Z0-9\\s\\-]{1,256}", scanTag)) {
            throw new IastValidationScanTagFailedException("The scan tag is invalid. The scan tag must contain only [a-zA-Z0-9\\s\\-] and the size is less than 256.");
        }
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

                    for (ResultInfo scansResultQuery : scansResultsQuery.stream().filter(ResultInfo::isNewResult).collect(
                            Collectors.toList())) {
                        switch (request.getBugTracker().getType()) {
                            case JIRA:
                                createJiraIssue(scanVulnerabilities, request, scansResultQuery, vulnerability, scan);
                                break;
                            case AZUREISSUE:
                                createAzureIssue(scanVulnerabilities, request, scansResultQuery, vulnerability, scan);
                                break;
                            default:
                                throw new NotImplementedException(request.getBugTracker().getType().getType() + ". That bug tracker not implemented.");

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

    private void createAzureIssue(ScanVulnerabilities scanVulnerabilities,
                                  ScanRequest request,
                                  ResultInfo scansResultQuery,
                                  VulnerabilityInfo vulnerability,
                                  Scan scan) {

        String title = createIssueTitle(request, scansResultQuery);
        String description = createIssueDescription(scanVulnerabilities, request, scansResultQuery, vulnerability, scan, true);

        ScanResults.XIssue issue = ScanResults.XIssue.builder()
                .description(description)
                .vulnerability(vulnerability.getName())
                .build();

        request.putAdditionalMetadata(Constants.ADO_ISSUE_BODY_KEY, description);
        request.putAdditionalMetadata(Constants.ADO_ISSUE_KEY, adoProperties.getIssueType());

        try {
            azureService.createIssue(title, issue, request, Arrays.asList(scan.getTag()));
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                                          Scan scan,
                                          boolean html) {
        String newLine = html ? "<br>" : System.lineSeparator();
        String description = generateIastLinkToVulnerability(scanVulnerabilities, scansResultQuery, vulnerability)
                + newLine + newLine + "Scan Tag: " + scan.getTag();

        if (request != null) {
            if (request.getRepoName() != null) {
                description += newLine + " Repository: " + request.getRepoName();
            }

            if (request.getBranch() != null) {
                description += newLine + " Branch: " + request.getBranch();
            }
        }
        return description;
    }

    private String createIssueTitle(ScanRequest request,
                                         ResultInfo scansResultQuery) {
        String title = scansResultQuery.getName();

        if (scansResultQuery.getUrl() != null) {
            title += ": " + scansResultQuery.getUrl();
        }
        if (request != null) {
            if (request.getBranch() != null) {
                title += " [" + request.getBranch() + "]";
            }
        }
        return title;
    }
}
