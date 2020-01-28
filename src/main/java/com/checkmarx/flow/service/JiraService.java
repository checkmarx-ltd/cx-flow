package com.checkmarx.flow.service;

import com.atlassian.jira.rest.client.api.*;
import com.atlassian.jira.rest.client.api.domain.*;
import com.atlassian.jira.rest.client.api.domain.input.ComplexIssueInputFieldValue;
import com.atlassian.jira.rest.client.api.domain.input.FieldInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.api.domain.input.TransitionInput;
import com.atlassian.jira.rest.client.internal.async.CustomAsynchronousJiraRestClientFactory;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.JiraClientException;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import com.google.common.collect.ImmutableMap;
import io.atlassian.util.concurrent.Promise;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import javax.annotation.PostConstruct;
import java.beans.ConstructorProperties;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class JiraService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(JiraService.class);
    private JiraRestClient client;
    private IssueRestClient issueClient;
    private ProjectRestClient projectClient;
    private MetadataRestClient metaClient;
    private URI jiraURI;
    private final JiraProperties jiraProperties;
    private final FlowProperties flowProperties;
    private static final int MAX_JQL_RESULTS = 1000000;
    private static final int JIRA_MAX_DESCRIPTION = 32760;
    private final String ParentUrl;
    private final String GrandParentUrl;

    @ConstructorProperties({"jiraProperties", "flowProperties"})
    public JiraService(JiraProperties jiraProperties, FlowProperties flowProperties) {
        this.jiraProperties = jiraProperties;
        this.flowProperties = flowProperties;
        ParentUrl = jiraProperties.getParentUrl();
        GrandParentUrl = jiraProperties.getGrandParentUrl();
    }

    @PostConstruct
    public void init() {
        if (jiraProperties != null && !ScanUtils.empty(jiraProperties.getUrl())) {
            CustomAsynchronousJiraRestClientFactory factory = new CustomAsynchronousJiraRestClientFactory();
            try {
                this.jiraURI = new URI(jiraProperties.getUrl());
            } catch (URISyntaxException e) {
                log.error("Error constructing URI for JIRA");
            }
            this.client = factory.createWithBasicHttpAuthenticationCustom(jiraURI, jiraProperties.getUsername(), jiraProperties.getToken(), jiraProperties.getHttpTimeout());
            this.issueClient = this.client.getIssueClient();
            this.projectClient = this.client.getProjectClient();
            this.metaClient = this.client.getMetadataClient();
        }
    }

    private List<Issue> getIssues(ScanRequest request) {
        log.info("Executing getIssues API call");
        List<Issue> issues = new ArrayList<>();
        String jql;
        BugTracker bugTracker = request.getBugTracker();
        /*Namespace/Repo/Branch provided*/
        if (!flowProperties.isTrackApplicationOnly()
                && !flowProperties.isApplicationRepoOnly()
                && !ScanUtils.empty(request.getNamespace())
                && !ScanUtils.empty(request.getRepoName())
                && !ScanUtils.empty(request.getBranch())) {
            jql = String.format("project = %s and issueType = \"%s\" and (\"%s\" = \"%s\" and \"%s\" = \"%s:%s\" and \"%s\" = \"%s:%s\" and \"%s\" = \"%s:%s\")",
                    bugTracker.getProjectKey(),
                    bugTracker.getIssueType(),
                    jiraProperties.getLabelTracker(),
                    request.getProduct().getProduct(),
                    jiraProperties.getLabelTracker(),
                    jiraProperties.getOwnerLabelPrefix(), request.getNamespace(),
                    jiraProperties.getLabelTracker(),
                    jiraProperties.getRepoLabelPrefix(), request.getRepoName(),
                    jiraProperties.getLabelTracker(),
                    jiraProperties.getBranchLabelPrefix(), request.getBranch()
            );
        }/*Only application and repo provided */ else if (!ScanUtils.empty(request.getApplication()) && !ScanUtils.empty(request.getRepoName())) {

            jql = String.format("project = %s and issueType = \"%s\" and (\"%s\" = \"%s\" and \"%s\" = \"%s:%s\" and \"%s\" = \"%s:%s\")",
                    bugTracker.getProjectKey(),
                    bugTracker.getIssueType(),
                    jiraProperties.getLabelTracker(),
                    request.getProduct().getProduct(),
                    jiraProperties.getLabelTracker(),
                    jiraProperties.getAppLabelPrefix(), request.getApplication(),
                    jiraProperties.getLabelTracker(),
                    jiraProperties.getRepoLabelPrefix(), request.getRepoName()
            );

        }/*Only application provided*/ else if (!ScanUtils.empty(request.getApplication())) {
            jql = String.format("project = %s and issueType = \"%s\" and (\"%s\" = \"%s\" and \"%s\" = \"%s:%s\")",
                    bugTracker.getProjectKey(),
                    bugTracker.getIssueType(),
                    jiraProperties.getLabelTracker(),
                    request.getProduct().getProduct(),
                    jiraProperties.getLabelTracker(),
                    jiraProperties.getAppLabelPrefix(), request.getApplication()
            );
        } else {
            log.error("Namespace/Repo/Branch or App must be provided in order to properly track ");
            throw new MachinaRuntimeException();
        }
        log.debug(jql);
        HashSet<String> fields = new HashSet<String>();
        fields.add("key");
        fields.add("project");
        fields.add("issuetype");
        fields.add("summary");
        fields.add("labels");
        fields.add("created");
        fields.add("updated");
        fields.add("status");
        Promise<SearchResult> searchJqlPromise = this.client.getSearchClient().searchJql(jql, MAX_JQL_RESULTS, 0, fields);
        for (Issue issue : searchJqlPromise.claim().getIssues()) {
            issues.add(issue);
        }
        return issues;
    }

    private Issue getIssue(String bugId) {
        return this.issueClient.getIssue(bugId).claim();
    }

    private IssueType getIssueType(String projectKey, String type) throws RestClientException, JiraClientException {
        Project project = this.projectClient.getProject(projectKey).claim();
        Iterator<IssueType> issueTypes = project.getIssueTypes().iterator();
        while (issueTypes.hasNext()) {
            IssueType it = issueTypes.next();
            if (it.getName().equals(type)) {
                return it;
            }
        }
        log.error("Issue type {} not found for project key {}", projectKey, issueTypes);
        throw new JiraClientException("Issue type not found");
    }

    private String createIssue(ScanResults.XIssue issue, ScanRequest request) throws JiraClientException {
        log.debug("Retrieving issuetype object for project {}, type {}", request.getBugTracker().getProjectKey(), request.getBugTracker().getIssueType());
        try {
            BugTracker bugTracker = request.getBugTracker();
            String assignee = bugTracker.getAssignee();
            String projectKey = bugTracker.getProjectKey();
            String application = request.getApplication();
            String namespace = request.getNamespace();
            String repoName = request.getRepoName();
            String branch = request.getBranch();
            String filename = issue.getFilename();
            String vulnerability = issue.getVulnerability();
            String severity = issue.getSeverity();

            IssueType issueType = this.getIssueType(projectKey, bugTracker.getIssueType());
            IssueInputBuilder issueBuilder = new IssueInputBuilder(projectKey, issueType.getId());
            String issuePrefix = jiraProperties.getIssuePrefix();
            String issuePostfix = jiraProperties.getIssuePostfix();

            if (issuePrefix == null) {
                issuePrefix = "";
            }
            if (issuePostfix == null) {
                issuePostfix = "";
            }

            String summary;
            if (!flowProperties.isTrackApplicationOnly()
                    && !flowProperties.isApplicationRepoOnly()
                    && !ScanUtils.empty(namespace)
                    && !ScanUtils.empty(repoName)
                    && !ScanUtils.empty(branch)) {
                summary = String.format(ScanUtils.JIRA_ISSUE_KEY, issuePrefix, vulnerability, filename, branch, issuePostfix);
            } else {
                summary = String.format(ScanUtils.JIRA_ISSUE_KEY_2, issuePrefix, vulnerability, filename, issuePostfix);
            }
            String fileUrl = ScanUtils.getFileUrl(request, issue.getFilename());

            /* Summary can only be 255 chars */
            if (summary.length() > 255) {
                summary = summary.substring(0, 254);
            }
            issueBuilder.setSummary(summary);

            issueBuilder.setDescription(this.getBody(issue, request, fileUrl));

            if (assignee != null && !assignee.isEmpty()) {
                try {
                    User userAssignee = getAssignee(assignee);
                    issueBuilder.setAssignee(userAssignee);
                } catch (RestClientException e) {
                    log.error("Error occurred while assigning to user {}", assignee);
                    log.error(ExceptionUtils.getStackTrace(e));
                }
            }

            if (bugTracker.getPriorities() != null && bugTracker.getPriorities().containsKey(severity)) {
                issueBuilder.setFieldValue("priority", ComplexIssueInputFieldValue.with("name",
                        bugTracker.getPriorities().get(severity)));
            }

            /*Add labels for tracking existing issues*/
            List<String> labels = new ArrayList<>();
            if (!flowProperties.isTrackApplicationOnly()
                    && !flowProperties.isApplicationRepoOnly()
                    && !ScanUtils.empty(namespace)
                    && !ScanUtils.empty(repoName)
                    && !ScanUtils.empty(branch)) {
                labels.add(request.getProduct().getProduct());
                labels.add(jiraProperties.getOwnerLabelPrefix().concat(":").concat(namespace));
                labels.add(jiraProperties.getRepoLabelPrefix().concat(":").concat(repoName));
                labels.add(jiraProperties.getBranchLabelPrefix().concat(":").concat(branch));
            } else if (!ScanUtils.empty(application) && !ScanUtils.empty(repoName)) {
                labels.add(request.getProduct().getProduct());
                labels.add(jiraProperties.getAppLabelPrefix().concat(":").concat(application));
                labels.add(jiraProperties.getRepoLabelPrefix().concat(":").concat(repoName));
            } else if (!ScanUtils.empty(application)) {
                labels.add(request.getProduct().getProduct());
                labels.add(jiraProperties.getAppLabelPrefix().concat(":").concat(application));
            }
            log.debug("Adding tracker labels: {} - {}", jiraProperties.getLabelTracker(), labels);
            if (!jiraProperties.getLabelTracker().equals("labels")) {
                String customField = getCustomFieldByName(projectKey,
                        bugTracker.getIssueType(), jiraProperties.getLabelTracker());
                issueBuilder.setFieldValue(customField, labels);
            } else {
                issueBuilder.setFieldValue("labels", labels);
            }

            log.debug("Creating JIRA issue");

            mapCustomFields(request, issue, issueBuilder, false);

            log.debug("Creating JIRA issue");
            log.debug(issueBuilder.toString());
            BasicIssue basicIssue = this.issueClient.createIssue(issueBuilder.build()).claim();
            log.debug("JIRA issue {} created", basicIssue.getKey());
            return basicIssue.getKey();
        } catch (RestClientException e) {
            log.error("Error occurred while creating JIRA issue. {}", e.getMessage());
            log.error(ExceptionUtils.getStackTrace(e));
            throw new JiraClientException();
        }
    }

    private Issue updateIssue(String bugId, ScanResults.XIssue issue, ScanRequest request) throws JiraClientException {
        BugTracker bugTracker = request.getBugTracker();
        String severity = issue.getSeverity();
        Issue jiraIssue = this.getIssue(bugId);
        if (bugTracker.getClosedStatus().contains(jiraIssue.getStatus().getName())) {
            this.transitionIssue(bugId, bugTracker.getOpenTransition());
        }
        IssueInputBuilder issueBuilder = new IssueInputBuilder();
        String fileUrl = ScanUtils.getFileUrl(request, issue.getFilename());
        issueBuilder.setDescription(this.getBody(issue, request, fileUrl));

        if (bugTracker.getPriorities().containsKey(severity)) {
            issueBuilder.setFieldValue("priority", ComplexIssueInputFieldValue.with("name",
                    bugTracker.getPriorities().get(severity)));
        }

        log.info("Updating issue #{}", bugId);

        mapCustomFields(request, issue, issueBuilder, true);

        log.debug("Updating JIRA issue");
        log.debug(issueBuilder.toString());
        try {
            this.issueClient.updateIssue(bugId, issueBuilder.build()).claim();
        } catch (RestClientException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new JiraClientException();
        }

        return this.getIssue(bugId);
    }

    /**
     * Map custom JIRA fields to specific values (Custom Cx fields, Issue result
     * fields, static fields
     *
     * @param request
     * @param issue
     * @param issueBuilder
     */
    private void mapCustomFields(ScanRequest request, ScanResults.XIssue issue, IssueInputBuilder issueBuilder, boolean update) {
        BugTracker bugTracker = request.getBugTracker();

        log.debug("Handling custom field mappings");
        if (bugTracker.getFields() == null) {
            return;
        }

        String projectKey = bugTracker.getProjectKey();
        String issueTypeStr = bugTracker.getIssueType();

        for (com.checkmarx.flow.dto.Field f : bugTracker.getFields()) {

            String customField = getCustomFieldByName(projectKey, issueTypeStr, f.getJiraFieldName());
            String value;
            if (update && f.isSkipUpdate()) {
                log.debug("Skip update to field {}", f.getName());
                continue;
            }
            if (!ScanUtils.empty(customField)) {
                /*cx | static | other - specific values that can be linked from scan request or the issue details*/
                String fieldType = f.getType();
                if (ScanUtils.empty(fieldType)) {
                    log.warn("Field type not supplied for custom field: {}. Using 'result' by default.", customField);
                    // use default = result
                    fieldType = "result";
                }

                switch (fieldType) {
                    case "cx":
                        log.debug("Checkmarx custom field {}", f.getName());
                        if (request.getCxFields() != null) {
                            log.debug("Checkmarx custom field");
                            value = request.getCxFields().get(f.getName());
                            log.debug("Cx Field value: {}", value);
                            if (ScanUtils.empty(value) && !ScanUtils.empty(f.getJiraDefaultValue())) {
                                value = f.getJiraDefaultValue();
                                log.debug("JIRA default Value is {}", value);
                            }
                        } else {
                            log.debug("No value found for {}", f.getName());
                            value = "";
                        }
                        break;
                    case "static":
                        log.debug("Static value {} - {}", f.getName(), f.getJiraDefaultValue());
                        value = f.getJiraDefaultValue();
                        break;
                    default:  //result
                        String fieldName = f.getName();
                        if (fieldName == null) {
                            log.warn("Field name not supplied for custom field: {}. Skipping.", customField);
                            /* there is no default, move on to the next field */
                            continue;
                        }
                        /*known values we can use*/
                        switch (fieldName) {
                            case "application":
                                log.debug("application: {}", request.getApplication());
                                value = request.getApplication();
                                break;
                            case "project":
                                log.debug("project: {}", request.getProject());
                                value = request.getProject();
                                break;
                            case "namespace":
                                log.debug("namespace: {}", request.getNamespace());
                                value = request.getNamespace();
                                break;
                            case "repo-name":
                                log.debug("repo-name: {}", request.getRepoName());
                                value = request.getRepoName();
                                break;
                            case "repo-url":
                                log.debug("repo-url: {}", request.getRepoUrl());
                                value = request.getRepoUrl();
                                break;
                            case "branch":
                                log.debug("branch: {}", request.getBranch());
                                value = request.getBranch();
                                break;
                            case "severity":
                                log.debug("severity: {}", issue.getSeverity());
                                value = issue.getSeverity();
                                break;
                            case "category":
                                log.debug("category: {}", issue.getVulnerability());
                                value = issue.getVulnerability();
                                break;
                            case "cwe":
                                log.debug("cwe: {}", issue.getCwe());
                                value = issue.getCwe();
                                break;
                            case "cve":
                                log.debug("cve: {}", issue.getCve());
                                value = issue.getCve();
                                break;
                            case "system-date":
                                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                                LocalDateTime now = LocalDateTime.now().plusDays(f.getOffset());
                                value = dtf.format(now);
                                log.debug("system date: {}", value);
                                break;
                            case "recommendation":
                                StringBuilder recommendation = new StringBuilder();
                                if (issue.getLink() != null && !issue.getLink().isEmpty()) {
                                    recommendation.append("Checkmarx Link: ").append(issue.getLink()).append(ScanUtils.CRLF);
                                }
                                if (!ScanUtils.empty(issue.getCwe())) {
                                    recommendation.append("Mitre Details: ").append(String.format(flowProperties.getMitreUrl(), issue.getCwe())).append(ScanUtils.CRLF);
                                }
                                if (!ScanUtils.empty(flowProperties.getCodebashUrl())) {
                                    recommendation.append("Training: ").append(flowProperties.getCodebashUrl()).append(ScanUtils.CRLF);
                                }
                                if (!ScanUtils.empty(flowProperties.getWikiUrl())) {
                                    recommendation.append("Guidance: ").append(flowProperties.getWikiUrl()).append(ScanUtils.CRLF);
                                }
                                value = recommendation.toString();
                                break;
                            case "loc":
                                value = "";
                                List<Integer> lines = issue.getDetails().entrySet()
                                        .stream()
                                        .filter(x -> x.getKey() != null && x.getValue() != null && !x.getValue().isFalsePositive())
                                        .map(Map.Entry::getKey)
                                        .collect(Collectors.toList());
                                if (!lines.isEmpty()) {
                                    Collections.sort(lines);
                                    value = StringUtils.join(lines, ",");
                                    log.debug("loc: {}", value);
                                }
                                break;
                            case "not-exploitable":
                                value = "";
                                List<Integer> fpLines;
                                if (issue.getDetails() != null) {
                                    fpLines = issue.getDetails().entrySet()
                                            .stream()
                                            .filter(x -> x.getKey() != null && x.getValue() != null && x.getValue().isFalsePositive())
                                            .map(Map.Entry::getKey)
                                            .collect(Collectors.toList());
                                    if (!fpLines.isEmpty()) {
                                        Collections.sort(fpLines);
                                        value = StringUtils.join(fpLines, ",");
                                        log.debug("loc: {}", value);
                                    }
                                }
                                break;
                            case "site":
                                log.debug("site: {}", request.getSite());
                                value = request.getSite();
                                break;
                            case "issue-link":
                                log.debug("issue-link: {}", issue.getLink());
                                value = issue.getLink();
                                break;
                            case "filename":
                                log.debug("filename: {}", issue.getFilename());
                                value = issue.getFilename();
                                break;
                            case "language":
                                log.debug("language: {}", issue.getLanguage());
                                value = issue.getLanguage();
                                break;
                            default:
                                log.warn("field value for {} not found", f.getName());
                                value = "";
                        }
                        /*If the value is missing, check if a default value was specified*/
                        if (ScanUtils.empty(value)) {
                            log.debug("Value is empty, defaulting to configured default (if applicable)");
                            if (!ScanUtils.empty(f.getJiraDefaultValue())) {
                                value = f.getJiraDefaultValue();
                                log.debug("Default value is {}", value);
                            }
                        }
                        break;
                }
                /*Determine the expected custom field type within JIRA*/
                if (!ScanUtils.empty(value)) {
                    String jiraFieldType = f.getJiraFieldType();
                    if (ScanUtils.empty(jiraFieldType)) {
                        log.warn("JIRA field type not supplied for custom field: {}. Using 'text' by default.", f.getName());
                        // use default = text
                        jiraFieldType = "text";
                    }

                    List<String> list;
                    switch (jiraFieldType) {
                        case "security":
                            log.debug("Security field");
                            SecurityLevel securityLevel = getSecurityLevel(projectKey, issueTypeStr, value);
                            if (securityLevel != null) {
                                log.warn("JIRA Security level was not found: {}", value);
                                issueBuilder.setFieldValue("security", securityLevel);
                            }
                            break;
                        case "text":
                            log.debug("text field");
                            issueBuilder.setFieldValue(customField, value);
                            break;
                        case "component":
                            log.debug("component field");
                            issueBuilder.setComponentsNames(Collections.singletonList(value));
                            break;
                        case "label":
                            /*csv to array | replace space with _ */
                            log.debug("label field");
                            String[] l = StringUtils.split(value, ",");
                            list = new ArrayList<>();
                            for (String x : l) {
                                list.add(x.replaceAll("[^a-zA-Z0-9-_]+", "_"));
                            }

                            if (!ScanUtils.empty(list)) {
                                issueBuilder.setFieldValue(customField, list);
                            }
                            break;
                        case "single-select":
                            log.debug("single select field");
                            issueBuilder.setFieldValue(customField, ComplexIssueInputFieldValue.with("value", value));
                            break;
                        case "radio":
                            log.debug("radio field");
                            issueBuilder.setFieldValue(customField, ComplexIssueInputFieldValue.with("value", value));
                            break;
                        case "multi-select":
                            log.debug("multi select field");
                            String[] selected = StringUtils.split(value, ",");
                            List<ComplexIssueInputFieldValue> fields = new ArrayList<>();
                            for (String s : selected) {
                                ComplexIssueInputFieldValue fieldValue = ComplexIssueInputFieldValue.with("value", s.trim());
                                fields.add(fieldValue);
                            }
                            issueBuilder.setFieldValue(customField, fields);
                            break;
                        default:
                            log.warn("{} not a valid option for jira field type", f.getJiraFieldType());
                    }
                }
            }
        }
    }

    private SecurityLevel getSecurityLevel(String projectKey, String issueType, String name) {
        GetCreateIssueMetadataOptions options;
        options = new GetCreateIssueMetadataOptionsBuilder().withExpandedIssueTypesFields().withIssueTypeNames(issueType).withProjectKeys(projectKey).build();
        Iterable<CimProject> metadata = this.issueClient.getCreateIssueMetadata(options).claim();

        CimProject cim = metadata.iterator().next();

        Map<String, CimFieldInfo> fields = cim.getIssueTypes().iterator().next().getFields();
        CimFieldInfo security = fields.get("security");
        if (security != null) {
            Iterable<Object> allowedValues = security.getAllowedValues();
            if (allowedValues != null) {
                for (Object lvl : allowedValues) {
                    SecurityLevel secLevel = (SecurityLevel) lvl;
                    if (secLevel.getName().equals(name)) {
                        return secLevel;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Tranistions an issue based on the issue id and transition name
     *
     * TODO handle re-open transition fields
     *
     * @param bugId
     * @param transitionName
     * @return
     */
    private Issue transitionIssue(String bugId, String transitionName) throws JiraClientException {
        Issue issue;
        try {
            issue = this.issueClient.getIssue(bugId).claim();

            URI transitionURI = issue.getTransitionsUri();
            final Iterable<Transition> transitions = this.issueClient.getTransitions(transitionURI).claim();
            final Transition transition = getTransitionByName(transitions, transitionName);
            if (transition != null) {
                this.issueClient.transition(issue.getTransitionsUri(), new TransitionInput(transition.getId())).claim();
            } else {
                log.warn("Issue cannot be transitioned to {}.  Transition is not applicable to issue {}.  Available transitions: {}",
                        transitionName, bugId, transitions.toString());
            }
        } catch (RestClientException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            log.error("There was a problem transitioning issue {}. ", bugId, e);
            throw new JiraClientException();
        }
        return issue;
    }

    private Issue transitionCloseIssue(String bugId, String transitionName, BugTracker bt, boolean falsePositive) throws JiraClientException {
        Issue issue;
        try {
            issue = this.issueClient.getIssue(bugId).claim();

            URI transitionURI = issue.getTransitionsUri();
            final Iterable<Transition> transitions = this.issueClient.getTransitions(transitionURI).claim();
            final Transition transition = getTransitionByName(transitions, transitionName);
            if (transition != null) {
                //No input for transition
                if (ScanUtils.empty(bt.getCloseTransitionField())
                        && ScanUtils.empty(bt.getCloseTransitionValue())) {
                    this.issueClient.transition(issue.getTransitionsUri(), new TransitionInput(transition.getId())).claim();
                }//Input required for transition
                else {
                    String transitionValue = bt.getCloseTransitionValue();
                    if (falsePositive && !ScanUtils.empty(jiraProperties.getCloseFalsePositiveTransitionValue())) { //Allow for a separate resolution status if any of the issues are false positive
                        transitionValue = jiraProperties.getCloseFalsePositiveTransitionValue();  //TODO add to bt?
                    }
                    this.issueClient.transition(issue.getTransitionsUri(), new TransitionInput(transition.getId(),
                            Collections.singletonList(new FieldInput(bt.getCloseTransitionField(), ComplexIssueInputFieldValue.with("name", transitionValue))))).claim();
                }
            } else {
                log.warn("Issue cannot't be transitioned to {}.  Transition is not applicable to issue {}.  Available transitions: {}",
                        transitionName, bugId, transitions.toString());
            }
        } catch (RestClientException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            log.error("There was a problem transitioning issue {}. ", bugId, e);
            throw new JiraClientException("");
        }
        return issue;
    }

    /**
     *
     * @param assignee
     * @return
     */
    private User getAssignee(String assignee) {
        return client.getUserClient().getUser(assignee).claim();
    }

    /**
     *
     * @param bugId
     * @param comment
     */
    private void addCommentToBug(String bugId, String comment) {
        try {
            Issue issue = this.issueClient.getIssue(bugId).claim();
            this.issueClient.addComment(issue.getCommentsUri(), Comment.valueOf(comment)).claim();
        } catch (RestClientException e) {
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * Returns Jira Transition object based on transition name from a list of
     * transitions
     *
     * @param transitions
     * @param transitionName
     * @return
     */
    private Transition getTransitionByName(Iterable<Transition> transitions, String transitionName) {
        for (Transition transition : transitions) {
            if (transition.getName().equals(transitionName)) {
                return transition;
            }
        }
        return null;
    }

    /**
     *
     * @param project
     * @param issueType
     * @param fieldName
     * @return
     */
    private String getCustomFieldByName(String project, String issueType, String fieldName) {
        log.debug("Getting custom field {}", fieldName);
        GetCreateIssueMetadataOptions options;
        options = new GetCreateIssueMetadataOptionsBuilder()
                .withExpandedIssueTypesFields()
                .withIssueTypeNames(issueType)
                .withProjectKeys(project)
                .build();
        Iterable<CimProject> metadata = this.issueClient.getCreateIssueMetadata(options).claim();
        CimProject cim = metadata.iterator().next();
        for (CimFieldInfo info : cim.getIssueTypes().iterator().next().getFields().values()) {
            if (info.getName() != null && info.getName().equals(fieldName)) {
                return info.getId();
            }
        }
        return null;
    }

    public void getCustomFields() {
        for (Field p1 : this.metaClient.getFields().claim()) {
            log.info(p1.toString());
        }
    }

    private Map<String, Issue> getJiraIssueMap(List<Issue> issues) {
        Map<String, Issue> jiraMap = new HashMap<>();
        for (Issue issue : issues) {
            jiraMap.put(issue.getSummary(), issue);
        }
        return jiraMap;
    }

    private Map<String, ScanResults.XIssue> getIssueMap(List<ScanResults.XIssue> issues, ScanRequest request) {
        String issuePrefix = jiraProperties.getIssuePrefix();
        String issuePostfix = jiraProperties.getIssuePostfix();
        if (issuePrefix == null) {
            issuePrefix = "";
        }
        if (issuePostfix == null) {
            issuePostfix = "";
        }
        Map<String, ScanResults.XIssue> map = new HashMap<>();

        if (!flowProperties.isTrackApplicationOnly()
                && !ScanUtils.empty(request.getNamespace())
                && !ScanUtils.empty(request.getRepoName())
                && !ScanUtils.empty(request.getBranch())) {
            for (ScanResults.XIssue issue : issues) {
                String key = String.format(ScanUtils.JIRA_ISSUE_KEY, issuePrefix, issue.getVulnerability(), issue.getFilename(), request.getBranch(), issuePostfix);
                map.put(key, issue);
            }
        } else {
            for (ScanResults.XIssue issue : issues) {
                String key = String.format(ScanUtils.JIRA_ISSUE_KEY_2, issuePrefix, issue.getVulnerability(), issue.getFilename(), issuePostfix);
                map.put(key, issue);
            }
        }
        return map;
    }

    private String getBody(ScanResults.XIssue issue, ScanRequest request, String fileUrl) {
        StringBuilder body = new StringBuilder();
        if (!ScanUtils.empty(jiraProperties.getDescriptionPrefix())) {
            body.append(jiraProperties.getDescriptionPrefix());
        }
        if (!flowProperties.isTrackApplicationOnly()
                && !ScanUtils.empty(request.getNamespace())
                && !ScanUtils.empty(request.getRepoName())
                && !ScanUtils.empty(request.getBranch())) {
            body.append(String.format(ScanUtils.JIRA_ISSUE_BODY, issue.getVulnerability(), issue.getFilename(), request.getBranch())).append(ScanUtils.CRLF).append(ScanUtils.CRLF);
        } else {
            body.append(String.format(ScanUtils.JIRA_ISSUE_BODY_2, issue.getVulnerability(), issue.getFilename())).append(ScanUtils.CRLF).append(ScanUtils.CRLF);
        }
        if (!ScanUtils.empty(issue.getDescription())) {
            body.append(issue.getDescription().trim()).append(ScanUtils.CRLF).append(ScanUtils.CRLF);
        }

        if (!ScanUtils.empty(request.getNamespace())) {
            body.append("*Namespace:* ").append(request.getNamespace()).append(ScanUtils.CRLF);
        }
        if (!ScanUtils.empty(request.getRepoName())) {
            body.append("*Repository:* ").append(request.getRepoName()).append(ScanUtils.CRLF);
        }
        if (!ScanUtils.empty(request.getBranch())) {
            body.append("*Branch:* ").append(request.getBranch()).append(ScanUtils.CRLF);
        }
        if (!ScanUtils.empty(request.getRepoUrl())) {
            body.append("*Repository Url:* ").append(request.getRepoUrl()).append(ScanUtils.CRLF);
        }
        if (!ScanUtils.empty(request.getApplication())) {
            body.append("*Application:* ").append(request.getApplication()).append(ScanUtils.CRLF);
        }
        if (!ScanUtils.empty(request.getProject())) {
            body.append("*Cx-Project:* ").append(request.getProject()).append(ScanUtils.CRLF);
        }
        if (!ScanUtils.empty(request.getTeam())) {
            body.append("*Cx-Team:* ").append(request.getTeam()).append(ScanUtils.CRLF);
        }
        if (!ScanUtils.empty(issue.getSeverity())) {
            body.append("*Severity:* ").append(issue.getSeverity()).append(ScanUtils.CRLF);
        }
        if (!ScanUtils.empty(issue.getCwe())) {
            body.append("*CWE:* ").append(issue.getCwe()).append(ScanUtils.CRLF);
        }

        body.append(ScanUtils.CRLF).append("*Addition Info*").append(ScanUtils.CRLF).append("----").append(ScanUtils.CRLF);
        if (issue.getLink() != null && !issue.getLink().isEmpty()) {
            body.append("[Checkmarx|").append(issue.getLink()).append("]").append(ScanUtils.CRLF);
        }
        if (!ScanUtils.empty(issue.getCwe()) && !ScanUtils.empty(flowProperties.getMitreUrl())) {
            body.append("[Mitre Details|").append(String.format(flowProperties.getMitreUrl(), issue.getCwe())).append("]").append(ScanUtils.CRLF);
        }
        if (!ScanUtils.empty(flowProperties.getCodebashUrl())) {
            body.append("[Training|").append(flowProperties.getCodebashUrl()).append("]").append(ScanUtils.CRLF);
        }
        if (!ScanUtils.empty(flowProperties.getWikiUrl())) {
            body.append("[Guidance|").append(flowProperties.getWikiUrl()).append("]").append(ScanUtils.CRLF);
        }
        if (issue.getDetails() != null && !issue.getDetails().isEmpty()) {
            if (issue.getDetails().entrySet().stream().anyMatch(x -> x.getKey() != null && x.getValue() != null && !x.getValue().isFalsePositive())) {
                body.append("Lines: ");
            }
            issue.getDetails().entrySet().stream()
                    .filter(x -> x.getKey() != null && x.getValue() != null && !x.getValue().isFalsePositive())
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        if (!ScanUtils.empty(fileUrl)) {
                            if (request.getRepoType().equals(ScanRequest.Repository.BITBUCKETSERVER)) {
                                body.append("[").append(entry.getKey()).append("|").append(fileUrl).append("#").append(entry.getKey()).append("] ");
                            } else if (request.getRepoType().equals(ScanRequest.Repository.BITBUCKET)) { //BB Cloud
                                body.append("[").append(entry.getKey()).append("|").append(fileUrl).append("#lines-").append(entry.getKey()).append("] ");
                            } else {
                                body.append("[").append(entry.getKey()).append("|").append(fileUrl).append("#L").append(entry.getKey()).append("] ");
                            }
                        } else {
                            body.append(entry.getKey()).append(" ");
                        }
                    });

            if (flowProperties.isListFalsePositives()) {//List the false positives / not exploitable
                body.append(ScanUtils.CRLF);
                if (issue.getDetails().entrySet().stream().anyMatch(x -> x.getKey() != null && x.getValue() != null && x.getValue().isFalsePositive())) {
                    body.append("Lines Marked Not Exploitable: ");
                }
                issue.getDetails().entrySet().stream()
                        .filter(x -> x.getKey() != null && x.getValue() != null && x.getValue().isFalsePositive())
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> {
                            if (!ScanUtils.empty(fileUrl)) {
                                if (request.getRepoType().equals(ScanRequest.Repository.BITBUCKETSERVER)) {
                                    body.append("[").append(entry.getKey()).append("|").append(fileUrl).append("#").append(entry.getKey()).append("] ");
                                } else if (request.getRepoType().equals(ScanRequest.Repository.BITBUCKET)) { //BB Cloud
                                    body.append("[").append(entry.getKey()).append("|").append(fileUrl).append("#lines-").append(entry.getKey()).append("] ");
                                } else {
                                    body.append("[").append(entry.getKey()).append("|").append(fileUrl).append("#L").append(entry.getKey()).append("] ");
                                }
                            } else {
                                body.append(entry.getKey()).append(" ");
                            }
                        });
            }
            body.append(ScanUtils.CRLF).append(ScanUtils.CRLF);
            issue.getDetails().entrySet().stream()
                    .filter(x -> x.getKey() != null && x.getValue() != null && !x.getValue().isFalsePositive())
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        if (!ScanUtils.empty(entry.getValue().getCodeSnippet())) {
                            body.append("----").append(ScanUtils.CRLF);
                            if (!ScanUtils.empty(fileUrl)) {
                                if (request.getRepoType().equals(ScanRequest.Repository.BITBUCKETSERVER)) {
                                    body.append("[Line #").append(entry.getKey()).append(":|").append(fileUrl).append("#").append(entry.getKey()).append("]").append(ScanUtils.CRLF);
                                } else if (request.getRepoType().equals(ScanRequest.Repository.BITBUCKET)) { //BB Cloud
                                    body.append("[Line #").append(entry.getKey()).append(":|").append(fileUrl).append("#lines-").append(entry.getKey()).append("]").append(ScanUtils.CRLF);
                                } else {
                                    body.append("[Line #").append(entry.getKey()).append(":|").append(fileUrl).append("#L").append(entry.getKey()).append("]").append(ScanUtils.CRLF);
                                }
                            } else {
                                body.append("Line #").append(entry.getKey()).append(ScanUtils.CRLF);
                            }
                            body.append("{code}").append(ScanUtils.CRLF);
                            body.append(entry.getValue().getCodeSnippet()).append(ScanUtils.CRLF);
                            body.append("{code}").append(ScanUtils.CRLF);
                        }
                    });
            body.append("----").append(ScanUtils.CRLF);
        }

        if (issue.getOsaDetails() != null) {
            for (ScanResults.OsaDetails o : issue.getOsaDetails()) {
                body.append(ScanUtils.CRLF);
                if (!ScanUtils.empty(o.getCve())) {
                    body.append("h3.").append(o.getCve()).append(ScanUtils.CRLF);
                }
                body.append("{noformat}");
                if (!ScanUtils.empty(o.getSeverity())) {
                    body.append("Severity: ").append(o.getSeverity()).append(ScanUtils.CRLF);
                }
                if (!ScanUtils.empty(o.getVersion())) {
                    body.append("Version: ").append(o.getVersion()).append(ScanUtils.CRLF);
                }
                if (!ScanUtils.empty(o.getDescription())) {
                    body.append("Description: ").append(o.getDescription()).append(ScanUtils.CRLF);
                }
                if (!ScanUtils.empty(o.getRecommendation())) {
                    body.append("Recommendation: ").append(o.getRecommendation()).append(ScanUtils.CRLF);
                }
                if (!ScanUtils.empty(o.getUrl())) {
                    body.append("URL: ").append(o.getUrl());
                }
                body.append("{noformat}");
                body.append(ScanUtils.CRLF);
            }
        }
        if (!ScanUtils.empty(jiraProperties.getDescriptionPostfix())) {
            body.append(jiraProperties.getDescriptionPostfix());
        }
        return StringUtils.truncate(body.toString(), JIRA_MAX_DESCRIPTION);
    }

    Map<String, List<String>> process(ScanResults results, ScanRequest request) throws JiraClientException {
        Map<String, ScanResults.XIssue> map;
        Map<String, Issue> jiraMap;
        List<Issue> issuesParent;
        List<Issue> issuesGrandParent;
        List<String> newIssues = new ArrayList<>();
        List<String> updatedIssues = new ArrayList<>();
        List<String> closedIssues = new ArrayList<>();

        String application = request.getApplication();
        if (!ScanUtils.empty(application)) {
            application = application.replaceAll("[^a-zA-Z0-9-_.+]+", "_");
            request.setApplication(application);
        }

        if (this.jiraProperties.isChild()) {
            ScanRequest parent = new ScanRequest(request);
            ScanRequest Grandparent = new ScanRequest(request);
            BugTracker bugTracker;
            bugTracker = parent.getBugTracker();
            bugTracker.setProjectKey(ParentUrl);
            parent.setBugTracker(bugTracker);
            issuesParent = this.getIssues(parent);
            if (GrandParentUrl.length() == 0) {
                 log.info("Grandparent feild is empty");
                issuesGrandParent = this.getIssues(request);
            } else {
                BugTracker bugTrackerGrandParenet;
                bugTrackerGrandParenet = Grandparent.getBugTracker();
                bugTrackerGrandParenet.setProjectKey(GrandParentUrl);
                Grandparent.setBugTracker(bugTrackerGrandParenet);
                issuesGrandParent = this.getIssues(Grandparent);
            }
        } else {
            issuesParent = this.getIssues(request);
            issuesGrandParent = this.getIssues(request);
        }

        log.info("Processing Results and publishing findings to Jira");

        List<Issue> issues = this.getIssues(request);
        map = this.getIssueMap(results.getXIssues(), request);
        jiraMap = this.getJiraIssueMap(issues);

        for (Map.Entry<String, ScanResults.XIssue> xIssue : map.entrySet()) {
            try {
                ScanResults.XIssue currentIssue = xIssue.getValue();

                /*Issue already exists -> update and comment*/
                if (jiraMap.containsKey(xIssue.getKey())) {
                    Issue i = jiraMap.get(xIssue.getKey());
                    if (xIssue.getValue().isAllFalsePositive()) {
                        //All issues are false positive, so issue should be closed
                        log.debug("All issues are false positives");
                        Issue fpIssue;
                        if (flowProperties.isListFalsePositives()) { //Update the ticket if flag is set
                            log.debug("Issue is being updated to reflect false positive references.  Updating issue with key {}", xIssue.getKey());
                            fpIssue = this.updateIssue(i.getKey(), currentIssue, request);
                        } else { //otherwise simply get a reference to the issue
                            fpIssue = this.getIssue(i.getKey());
                        }
                        if (request.getBugTracker().getOpenStatus().contains(fpIssue.getStatus().getName())) { //If the status is of open state, close it
                            /*Close the issue*/
                            log.info("Closing issue with key {}", fpIssue.getKey());
                            this.transitionCloseIssue(fpIssue.getKey(), request.getBugTracker().getCloseTransition(), request.getBugTracker(), true);
                            closedIssues.add(fpIssue.getKey());
                        }
                    }/*Ignore any with label indicating false positive*/ else if (!i.getLabels().contains(jiraProperties.getFalsePositiveLabel())) {
                        log.debug("Issue still exists.  Updating issue with key {}", xIssue.getKey());
                        Issue updatedIssue = this.updateIssue(i.getKey(), currentIssue, request);
                        if (updatedIssue != null) {
                            log.debug("Update completed for issue #{}", updatedIssue.getKey());
                            updatedIssues.add(updatedIssue.getKey());
                            if (jiraProperties.isUpdateComment() && !ScanUtils.empty(jiraProperties.getUpdateCommentValue())) {
                                addCommentToBug(i.getKey(), jiraProperties.getUpdateCommentValue());
                            }
                        }
                    } else {
                        log.info("Skipping issue marked as false-positive or has False Positive state with key {}", xIssue.getKey());
                    }
                } else {
                    /*Create the new issue*/
                    if (!jiraProperties.isChild() || (!parentCheck(xIssue.getKey(), issuesParent) && !GrandparentCheck(xIssue.getKey(), issuesGrandParent))) {

                        if (jiraProperties.isChild()) {
                            log.info("Issue not found in parent creating issue for child");
                        }

                        log.debug("Creating new issue with key {}", xIssue.getKey());
                        String newIssue = this.createIssue(currentIssue, request);
                        newIssues.add(newIssue);
                        log.info("New issue created. #{}", newIssue);
                    }
                }
            } catch (RestClientException e) {
                log.error("Error occurred while processing issue with key {}", xIssue.getKey(), e);
                log.error(ExceptionUtils.getStackTrace(e));
                throw new JiraClientException();
            }
        }

        /*Check if an issue exists in Jira but not within results and close if not*/
        for (Map.Entry<String, Issue> jiraIssue : jiraMap.entrySet()) {
            try {
                if (!map.containsKey(jiraIssue.getKey())) {
                    if (request.getBugTracker().getOpenStatus().contains(jiraIssue.getValue().getStatus().getName())) {
                        /*Close the issue*/
                        log.info("Closing issue with key {}", jiraIssue.getKey());
                        this.transitionCloseIssue(jiraIssue.getValue().getKey(),
                                request.getBugTracker().getCloseTransition(), request.getBugTracker(), false); //No false positives
                        closedIssues.add(jiraIssue.getValue().getKey());
                    }
                }
            } catch (HttpClientErrorException e) {
                log.error("Error occurred while processing issue with key {} {}", jiraIssue.getKey(), e);
                log.error(ExceptionUtils.getStackTrace(e));
            }
        }

        return ImmutableMap.of(
                "new", newIssues,
                "updated", updatedIssues,
                "closed", closedIssues
        );
    }
    
    boolean parentCheck(String Key, List<Issue> issues) {
        Map<String, Issue> jiraMap;
        jiraMap = this.getJiraIssueMap(issues);
        if (this.jiraProperties.isChild()) {

            if (jiraMap.containsKey(Key)) {
                log.info("Issue ("+jiraMap.get(Key).getKey()+") found in parent("+ParentUrl+") not creating issue for child Issue");
                return true;
            }
        }
        return false;

    }
    
    boolean GrandparentCheck(String Key, List<Issue> issues) {
        Map<String, Issue> jiraMap;
        jiraMap = this.getJiraIssueMap(issues);
        if (this.jiraProperties.isChild()) {

            if (jiraMap.containsKey(Key)) {
                log.info("Issue ("+jiraMap.get(Key).getKey()+") found in GrandParent("+GrandParentUrl+") not creating issue for childIssue");
                return true;
            }
        }
        return false;

    }
}
