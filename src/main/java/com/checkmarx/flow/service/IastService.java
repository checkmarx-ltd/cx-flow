package com.checkmarx.flow.service;

import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.config.IastProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.custom.ADOIssueTracker;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.iast.CreateIssue;
import com.checkmarx.flow.dto.iast.manager.dto.ResultInfo;
import com.checkmarx.flow.dto.iast.manager.dto.Scan;
import com.checkmarx.flow.dto.iast.manager.dto.ScanVulnerabilities;
import com.checkmarx.flow.dto.iast.manager.dto.VulnerabilityInfo;
import com.checkmarx.flow.dto.iast.ql.utils.Severity;
import com.checkmarx.flow.exception.*;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;


@Slf4j
@Service
public class IastService {

    private final Map<Severity, String> jiraSeverityToPriority = new HashMap<>();

    private final Map<Severity, AtomicInteger> thresholdsSeverity = new HashMap<>(7);

    private final IastProperties iastProperties;

    private final JiraProperties jiraProperties;

    private final JiraService jiraService;

    private final IastServiceRequests iastServiceRequests;

    private final HelperService helperService;

    private final ADOIssueTracker azureService;

    private final ADOProperties adoProperties;

    private final GitHubService gitHubService;

    @Autowired
    public IastService(JiraProperties jiraProperties,
                       JiraService jiraService,
                       IastProperties iastProperties,
                       IastServiceRequests iastServiceRequests,
                       HelperService helperService,
                       GitHubService gitHubService,
                       ADOIssueTracker azureService,
                       ADOProperties adoProperties) {
        this.jiraProperties = jiraProperties;
        this.jiraService = jiraService;
        this.iastProperties = iastProperties;
        this.iastServiceRequests = iastServiceRequests;
        this.helperService = helperService;
        this.azureService = azureService;
        this.adoProperties = adoProperties;
        this.gitHubService = gitHubService;

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
            throw new IastScanRequestMustProvideException(
                    "ScanRequest is null. Something went wrong. Please contact with IAST support.");
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
        if (!Pattern.matches("^[a-zA-Z0-9\\:\\.\\s\\-]{1,256}$", scanTag)) {
            throw new IastValidationScanTagFailedException(
                    "The scan tag is invalid. The scan tag must contain only [a-zA-Z0-9\\:\\.\\s\\-] and the size is less than 256.");
        }
    }

    private void getVulnerabilitiesAndCreateIssue(ScanRequest request, Scan scan) throws IOException {

        final ScanVulnerabilities scanVulnerabilities = iastServiceRequests.apiScanVulnerabilities(scan.getScanId());
        List<VulnerabilityInfo> vulnerabilities = scanVulnerabilities.getVulnerabilities();

        for (VulnerabilityInfo vulnerability : vulnerabilities) {
            if (vulnerability.getNewCount() != 0) {

                final List<ResultInfo> scansResultsQuery;
                try {
                    scansResultsQuery = iastServiceRequests.apiScanResults(scan.getScanId(), vulnerability.getId());
                } catch (IOException e) {
                    throw new IOException("Can't send api request", e);
                }

                for (ResultInfo scansResultQuery : scansResultsQuery) {
                    if (scansResultQuery.isNewResult() && filterSeverity(scansResultQuery)) {
                        try {
                            switch (request.getBugTracker().getType()) {
                                case JIRA:
                                    createJiraIssue(scanVulnerabilities, request, scansResultQuery, vulnerability,
                                            scan);
                                    break;
                                case GITHUBISSUE:
                                    createGithubIssue(scanVulnerabilities, request, scansResultQuery, vulnerability,
                                            scan);
                                    break;
                                case AZURE:
                                    createAzureIssue(scanVulnerabilities, request, scansResultQuery, vulnerability,
                                            scan);
                                    break;
                                default:
                                    throw new NotImplementedException(request.getBugTracker().getType().getType() +
                                            ". That bug tracker not implemented.");
                            }
                        } catch (NotImplementedException e) {
                            throw new NotImplementedException(
                                    request.getBugTracker().getType().getType() + ". That bug tracker not implemented.");
                        } catch (Exception e) {
                            log.error("Can't create issue", e);
                        }
                    }
                }
            }
        }

        thresholdsSeverity(scanVulnerabilities);
    }

    /**
     * create an exception if the severity thresholds are exceeded
     */
    private void thresholdsSeverity(ScanVulnerabilities scanVulnerabilities) {

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
                    "\n High:   " + thresholdsSeverity.get(Severity.HIGH).get() + " / " +
                    iastProperties.getThresholdsSeverity().get(Severity.HIGH) +
                    "\n Medium: " + thresholdsSeverity.get(Severity.MEDIUM).get() + " / " +
                    iastProperties.getThresholdsSeverity().get(Severity.MEDIUM) +
                    "\n Low:    " + thresholdsSeverity.get(Severity.LOW).get() + " / " +
                    iastProperties.getThresholdsSeverity().get(Severity.LOW) +
                    "\n Info:   " + thresholdsSeverity.get(Severity.INFO).get() + " / " +
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

    private void createAzureIssue(ScanVulnerabilities scanVulnerabilities,
                                  ScanRequest request,
                                  ResultInfo scansResultQuery,
                                  VulnerabilityInfo vulnerability,
                                  Scan scan) {

        String title = createIssueTitle(request, scansResultQuery);
        String description = createIssueDescription(scanVulnerabilities, request, scansResultQuery, vulnerability, scan, true);

        request.putAdditionalMetadata(Constants.ADO_ISSUE_BODY_KEY, description);
        request.putAdditionalMetadata(Constants.ADO_ISSUE_KEY, adoProperties.getIssueType());

        try {
            azureService.createIssue(request, title, description, Arrays.asList(scan.getTag()));
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
        return createIssueDescription(scanVulnerabilities,
                request,
                scansResultQuery,
                vulnerability,
                scan,
                false);
    }

    private String createIssueDescription(ScanVulnerabilities scanVulnerabilities,
                                          ScanRequest request,
                                          ResultInfo scansResultQuery,
                                          VulnerabilityInfo vulnerability,
                                          Scan scan,
                                          boolean html) {
        String newLine = html ? "<br>" : System.lineSeparator();
        StringBuilder description = new StringBuilder();
        description.append(generateIastLinkToVulnerability(scanVulnerabilities, scansResultQuery, vulnerability))
                .append(newLine).append(newLine)
                .append("Scan Tag: ").append(scan.getTag());

        description.append(newLine).append("Severity: ").append(scansResultQuery.getSeverity().getName());

        if (request.getRepoName() != null) {
            description.append(newLine).append("Repository: ").append(request.getRepoName());
        }

        if (request.getBranch() != null) {
            description.append(newLine).append("Branch: ").append(request.getBranch());
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

    public ScanRequest getAzureScanRequest(Map<String, String> queryParams) {
        return getAzureScanRequest(queryParams.get("azureprojectname"), queryParams.get("azurenamespace"), null, null, null);
    }

    public ScanRequest getAzureScanRequest(CreateIssue body, Map<String, String> queryParams) {
        return getAzureScanRequest(queryParams.get("azurenamespace"), queryParams.get("azureprojectname"), body.getRepoName(),
                body.getNamespace(), body.getAssignee());
    }

    public ScanRequest getAzureScanRequest(String azureNamespace, String azureProjectName, String repoName,
                                           String namespace, String assignee) {

        if (Strings.isEmpty(azureNamespace)) {
            throw new IastThatPropertiesIsRequiredException("Property \"azureNamespace\" is required");
        }
        if (Strings.isEmpty(azureProjectName)) {
            throw new IastThatPropertiesIsRequiredException("Property \"azureProjectName\" is required");
        }

        Map<String, String> map = new HashMap<>();
        map.put(ADOIssueTracker.ADO_NAMESPACE, azureNamespace);

        BugTracker bt = BugTracker.builder()
                .type(BugTracker.Type.AZURE)
                .assignee(assignee)
                .build();

        return ScanRequest.builder()
                .bugTracker(bt)
                .altProject(azureProjectName)
                .additionalMetadata(map)
                .product(ScanRequest.Product.CX)
                .repoName(repoName)
                .namespace(namespace)
                .build();
    }

    public List<?> searchIssueByDescription(String bugTracker, Map<String, String> queryParams)
            throws MachinaException {

        switch (bugTracker) {
            case "jira":
                return jiraService.searchIssueByDescription(queryParams.get("description"));
            case "azure":
                ScanRequest scanRequest = getAzureScanRequest(queryParams);
                Map<String, String> map = new HashMap<>();
                map.put(Constants.ADO_ISSUE_BODY_KEY, "Description");
                scanRequest.setAdditionalMetadata(map);
                return azureService.searchIssuesByDescription(queryParams.get("description"), scanRequest);
            default:
                throw new NotImplementedException(bugTracker + ". That bug tracker not implemented.");
        }
    }

}
