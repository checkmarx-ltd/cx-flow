package com.custodela.machina.service;

import com.atlassian.jira.rest.client.api.*;
import com.atlassian.jira.rest.client.api.domain.*;
import com.atlassian.jira.rest.client.api.domain.input.ComplexIssueInputFieldValue;
import com.atlassian.jira.rest.client.api.domain.input.FieldInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.api.domain.input.TransitionInput;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.util.concurrent.Promise;
import com.custodela.machina.config.JiraProperties;
import com.custodela.machina.config.MachinaProperties;
import com.custodela.machina.dto.BugTracker;
import com.custodela.machina.dto.ScanRequest;
import com.custodela.machina.dto.ScanResults;
import com.custodela.machina.exception.JiraClientException;
import com.custodela.machina.exception.MachinaRuntimeException;
import com.custodela.machina.utils.ScanUtils;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import javax.annotation.PostConstruct;
import java.beans.ConstructorProperties;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;


@Service
public class JiraService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(JiraService.class);
    private JiraRestClient client;
    private IssueRestClient issueClient;
    private ProjectRestClient projectClient;
    private MetadataRestClient metaClient;
    private URI jiraURI;
    private final JiraProperties jiraProperties;
    private final MachinaProperties machinaProperties;
    private static final int MAX_JQL_RESULTS = 1000000;

    @ConstructorProperties({"jiraProperties", "machinaProperties"})
    public JiraService(JiraProperties jiraProperties, MachinaProperties machinaProperties) {
        this.jiraProperties = jiraProperties;
        this.machinaProperties = machinaProperties;
    }

    @PostConstruct
    public void init() {
        JiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();

        try {
            this.jiraURI = new URI(jiraProperties.getUrl());
        } catch (URISyntaxException e) {
            log.error("Error constructing URI for JIRA");
        }
        this.client = factory.createWithBasicHttpAuthentication(jiraURI, jiraProperties.getUsername(), jiraProperties.getToken());
        this.issueClient = this.client.getIssueClient();
        this.projectClient = this.client.getProjectClient();
        this.metaClient = this.client.getMetadataClient();
    }

    private List<Issue> getIssues(ScanRequest request) {
        log.info("Executing getIssues API call");
        List<Issue> issues = new ArrayList<>();
        String jql;
        /*Namespace/Repo/Branch provided*/
        if(!ScanUtils.empty(request.getNamespace()) && !ScanUtils.empty(request.getRepoName()) && !ScanUtils.empty(request.getBranch())) {
            jql = String.format("project = %s and issueType = \"%s\" and (\"%s\" = \"%s\" and \"%s\" = \"%s:%s\" and \"%s\" = \"%s:%s\" and \"%s\" = \"%s:%s\")",
                    request.getBugTracker().getProjectKey(),
                    request.getBugTracker().getIssueType(),
                    jiraProperties.getLabelTracker(),
                    request.getProduct().getProduct(),
                    jiraProperties.getLabelTracker(),
                    jiraProperties.getOwnerLabelPrefix(), request.getNamespace(),
                    jiraProperties.getLabelTracker(),
                    jiraProperties.getRepoLabelPrefix(), request.getRepoName(),
                    jiraProperties.getLabelTracker(),
                    jiraProperties.getBranchLabelPrefix(), request.getBranch()
            );
        }/*Only application provided*/
        else if(!ScanUtils.empty(request.getApplication())){
            jql = String.format("project = %s and issueType = \"%s\" and (\"%s\" = \"%s\" and \"%s\" = \"%s:%s\")",
                    request.getBugTracker().getProjectKey(),
                    request.getBugTracker().getIssueType(),
                    jiraProperties.getLabelTracker(),
                    request.getProduct().getProduct(),
                    jiraProperties.getLabelTracker(),
                    jiraProperties.getAppLabelPrefix(), request.getApplication()
            );
        }
        else{
            throw new MachinaRuntimeException();
        }
        log.debug(jql);
        Promise<SearchResult> searchJqlPromise = this.client.getSearchClient().searchJql(jql, MAX_JQL_RESULTS, 0, null);
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
        while(issueTypes.hasNext()){
            IssueType it = issueTypes.next();
            if(it.getName().equals(type)){
                return it;
            }
        }
        log.error("Issue type {} not found for project key {}", projectKey, issueTypes);
        throw new JiraClientException("Issue type not found");
    }


    private String createIssue(ScanResults.XIssue issue, ScanRequest request) throws JiraClientException{
        log.debug("Retrieving issuetype object for project {}, type {}", request.getBugTracker().getProjectKey(), request.getBugTracker().getIssueType());
        try{
            IssueType issueType = this.getIssueType(request.getBugTracker().getProjectKey(), request.getBugTracker().getIssueType());
            IssueInputBuilder issueBuilder = new IssueInputBuilder(request.getBugTracker().getProjectKey(), issueType.getId());
            String issuePrefix = jiraProperties.getIssuePrefix();
            if(issuePrefix == null){
                issuePrefix = "";
            }

            String summary;
            if(!ScanUtils.empty(request.getNamespace()) && !ScanUtils.empty(request.getRepoName()) && !ScanUtils.empty(request.getBranch())) {
                summary = String.format(ScanUtils.JIRA_ISSUE_KEY, issuePrefix, issue.getVulnerability(), issue.getFilename(), request.getBranch());
            }
            else{
                summary = String.format(ScanUtils.JIRA_ISSUE_KEY_2, issuePrefix, issue.getVulnerability(), issue.getFilename());
            }
            String fileUrl = ScanUtils.getFileUrl(request,issue.getFilename());

            /* Summary can only be 255 chars */
            if (summary.length() > 255) {
                summary = summary.substring(0, 254);
            }
            issueBuilder.setSummary(summary);

            issueBuilder.setDescription(this.getBody(issue, request, fileUrl));

            if(request.getBugTracker().getAssignee() != null && !request.getBugTracker().getAssignee().isEmpty()){
                try{
                    User assignee = getAssignee(request.getBugTracker().getAssignee());
                    issueBuilder.setAssignee(assignee);
                }catch(RestClientException e) {
                    log.error("Error occurred while assigning to user {}", request.getBugTracker().getAssignee());
                    log.error(ExceptionUtils.getStackTrace(e));
                }
            }

            if(request.getBugTracker().getPriorities().containsKey(issue.getSeverity())) {
                issueBuilder.setFieldValue("priority", ComplexIssueInputFieldValue.with("name",
                        request.getBugTracker().getPriorities().get(issue.getSeverity())));
            }

            /*Add labels for tracking existing issues*/
            List<String> labels = new ArrayList<>();
            if(!ScanUtils.empty(request.getNamespace()) && !ScanUtils.empty(request.getRepoName()) && !ScanUtils.empty(request.getBranch())) {
                labels.add(request.getProduct().getProduct());
                labels.add(jiraProperties.getOwnerLabelPrefix().concat(":").concat(request.getNamespace()));
                labels.add(jiraProperties.getRepoLabelPrefix().concat(":").concat(request.getRepoName()));
                labels.add(jiraProperties.getBranchLabelPrefix().concat(":").concat(request.getBranch()));
            }
            else if(!ScanUtils.empty(request.getApplication())){
                labels.add(request.getProduct().getProduct());
                labels.add(jiraProperties.getAppLabelPrefix().concat(":").concat(request.getApplication()));
            }
            log.debug("Adding tracker labels: {} - {}", jiraProperties.getLabelTracker(), labels);
            if(!jiraProperties.getLabelTracker().equals("labels")){
                String customField = getCustomFieldByName(request.getBugTracker().getProjectKey(),
                        request.getBugTracker().getIssueType(), jiraProperties.getLabelTracker());
                issueBuilder.setFieldValue(customField, labels);
            }
            else{
                issueBuilder.setFieldValue("labels", labels);
            }

            log.debug("Creating JIRA issue");

            mapCustomFields(request, issue, issueBuilder);

            log.debug("Creating JIRA issue");
            log.debug(issueBuilder.toString());
            BasicIssue basicIssue = this.issueClient.createIssue(issueBuilder.build()).claim();
            log.debug("JIRA issue {} created", basicIssue.getKey());
            return basicIssue.getKey();
        }catch (RestClientException e){
            log.error("Error occurred while creating JIRA issue. {}", e.getMessage());
            log.error(ExceptionUtils.getStackTrace(e));
            throw new JiraClientException();
        }
    }

    private Issue updateIssue(String bugId, ScanResults.XIssue issue, ScanRequest request) throws JiraClientException{
        Issue jiraIssue = this.getIssue(bugId);
        if(request.getBugTracker().getClosedStatus().contains(jiraIssue.getStatus().getName())){
            this.transitionIssue(bugId, request.getBugTracker().getOpenTransition());
        }
        IssueInputBuilder issueBuilder = new IssueInputBuilder();
        String fileUrl = ScanUtils.getFileUrl(request,issue.getFilename());
        issueBuilder.setDescription(this.getBody(issue, request, fileUrl));

        if(request.getBugTracker().getPriorities().containsKey(issue.getSeverity())) {
            issueBuilder.setFieldValue("priority", ComplexIssueInputFieldValue.with("name",
                    request.getBugTracker().getPriorities().get(issue.getSeverity())));
        }

        log.info("Updating issue #{}", bugId);

        mapCustomFields(request, issue, issueBuilder);

        log.debug("Updating JIRA issue");
        log.debug(issueBuilder.toString());
        try{
            this.issueClient.updateIssue(bugId, issueBuilder.build()).claim();
        }catch (RestClientException e){
            throw new JiraClientException();
        }

        return this.getIssue(bugId);
    }

    /**
     * Map custom JIRA fields to specific values (Custom Cx fields, Issue result fields, static fields
     *
     * @param request
     * @param issue
     * @param issueBuilder
     */
    private void mapCustomFields(ScanRequest request, ScanResults.XIssue issue, IssueInputBuilder issueBuilder){
        log.debug("Handling custom field mappings");
        if(request.getBugTracker().getFields() == null){
            return;
        }
        for(com.custodela.machina.dto.Field f: request.getBugTracker().getFields()){
            String projectKey = request.getBugTracker().getProjectKey();
            String issueTypeStr = request.getBugTracker().getIssueType();
            String customField = getCustomFieldByName(projectKey, issueTypeStr, f.getJiraFieldName());
            String value;

            /*cx | static | other - specific values that can be linked from scan request or the issue details*/
            switch (f.getType()) {
                case "cx":
                    log.debug("Checkmarx custom field {}", f.getName());
                    if (request.getCxFields() != null) {
                        log.debug("Checkmarx custom field");
                        value = request.getCxFields().get(f.getName());
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
                    /*known values we can use*/
                    switch (f.getName()) {
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
                        case "recommendation":
                            StringBuilder recommendation = new StringBuilder();
                            if (issue.getLink() != null && !issue.getLink().isEmpty()) {
                                recommendation.append("Issue Link: ").append(issue.getLink()).append(ScanUtils.CRLF);
                            }
                            if(!ScanUtils.empty(issue.getCwe())) {
                                recommendation.append("Mitre Details: ").append(String.format(machinaProperties.getMitreUrl(), issue.getCwe())).append(ScanUtils.CRLF);
                            }
                            if (!ScanUtils.empty(machinaProperties.getCodebashUrl())) {
                                recommendation.append("Training: ").append(machinaProperties.getCodebashUrl()).append(ScanUtils.CRLF);
                            }
                            if (!ScanUtils.empty(machinaProperties.getWikiUrl())) {
                                recommendation.append("Guidance: ").append(machinaProperties.getWikiUrl()).append(ScanUtils.CRLF);
                            }
                            value = recommendation.toString();
                            break;
                        case "loc":
                            value = "";
                            List<String> lines = new ArrayList<>();
                            if(issue.getDetails() != null && !issue.getDetails().isEmpty()) {
                                for (Map.Entry<Integer, String> entry : issue.getDetails().entrySet()) {
                                    if (entry.getKey() != null && entry.getValue() != null) {
                                        lines.add(entry.getKey().toString());
                                    }
                                }
                                Collections.sort(lines);
                                value = StringUtils.join(lines, ",");
                                log.debug("loc: {}", value);
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
            if(!ScanUtils.empty(value)) {
                List<String> list;
                switch (f.getJiraFieldType()) {
                    case "security":
                        log.debug("Security field");
                        SecurityLevel securityLevel = getSecurityLevel(projectKey, issueTypeStr, value);
                        if(securityLevel != null) {
                            log.warn("JIRA Security level was not found: {}", value);
                            issueBuilder.setFieldValue("security", securityLevel);
                        }
                        break;
                    case "text":
                        log.debug("text field");
                        issueBuilder.setFieldValue(customField, value);
                        break;
                    case "label": /*csv to array | replace space with _ */
                        log.debug("label field");
                        String[] l = StringUtils.split(value,",");
                        list = new ArrayList<>();
                        for(String x: l){
                            list.add((x.replaceAll(" ","_")).trim());
                        }
                        if(!ScanUtils.empty(list)) {
                            issueBuilder.setFieldValue(customField, list);
                        }
                        break;
                    case "single-select":
                        log.debug("single select field");
                        issueBuilder.setFieldValue(customField, ComplexIssueInputFieldValue.with("value",value));
                        break;
                    case "multi-select":
                        log.debug("multi select field");
                        String[] selected = StringUtils.split(value,",");
                        List<ComplexIssueInputFieldValue> fields = new ArrayList<>();
                        for(String s : selected){
                            ComplexIssueInputFieldValue fieldValue =  ComplexIssueInputFieldValue.with("value", s.trim());
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

    private SecurityLevel getSecurityLevel(String projectKey, String issueType, String name){
        GetCreateIssueMetadataOptions options;
        options = new GetCreateIssueMetadataOptionsBuilder().withExpandedIssueTypesFields().withIssueTypeNames(issueType).withProjectKeys(projectKey).build();
        Iterable<CimProject> metadata = this.issueClient.getCreateIssueMetadata(options).claim();

        CimProject project = metadata.iterator().next();

        CimProject cim = metadata.iterator().next();

        Map<String,CimFieldInfo> fields = cim.getIssueTypes().iterator().next().getFields();
        CimFieldInfo security = fields.get("security");
        if (security != null) {
            Iterable<Object> allowedValues = security.getAllowedValues();
            if(allowedValues != null) {
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
        } catch(RestClientException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            log.error("There was a problem transitioning issue {}. ", bugId, e);
            throw new JiraClientException();
        }
        return issue;
    }

    private Issue transitionCloseIssue(String bugId, String transitionName, BugTracker bt) throws JiraClientException{
        Issue issue;
        try {
            issue = this.issueClient.getIssue(bugId).claim();

            URI transitionURI = issue.getTransitionsUri();
            final Iterable<Transition> transitions = this.issueClient.getTransitions(transitionURI).claim();
            final Transition transition = getTransitionByName(transitions, transitionName);
            if (transition != null) {
                //No input for transition
                if(ScanUtils.empty(bt.getCloseTransitionField()) &&
                        ScanUtils.empty(bt.getCloseTransitionValue())){
                    this.issueClient.transition(issue.getTransitionsUri(), new TransitionInput(transition.getId())).claim();
                }//Input required for transition
                else{
                    this.issueClient.transition(issue.getTransitionsUri(), new TransitionInput(transition.getId(),
                            Collections.singletonList(new FieldInput(bt.getCloseTransitionField(), ComplexIssueInputFieldValue.with("name", bt.getCloseTransitionValue()))))).claim();
                }
            } else {
                log.warn("Issue cannot't be transitioned to {}.  Transition is not applicable to issue {}.  Available transitions: {}",
                transitionName, bugId, transitions.toString());
            }
        } catch(RestClientException e) {
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
    private void addCommentToBug(String bugId, String comment){
        try{
            Issue issue = this.issueClient.getIssue(bugId).claim();
            this.issueClient.addComment(issue.getCommentsUri(), Comment.valueOf(comment)).claim();
        }catch(RestClientException e){
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }


    /**
     * Returns Jira Transition object based on transition name from a list of transitions
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
            if(info.getName() != null && info.getName().equals(fieldName)){
                return info.getId();
            }
        }
        return null;
    }

    public void getCustomFields(){
        for (Field p1 : this.metaClient.getFields().claim()) {
            log.info(p1.toString());
        }
    }

    private Map<String, Issue> getJiraIssueMap(List<Issue> issues){
        Map<String, Issue> jiraMap = new HashMap<>();
        for(Issue issue : issues){
                jiraMap.put(issue.getSummary(),issue);
        }
        return jiraMap;
    }


    private Map<String, ScanResults.XIssue> getIssueMap(List<ScanResults.XIssue> issues, ScanRequest request){
        String issuePrefix = jiraProperties.getIssuePrefix();
        if(issuePrefix == null){
            issuePrefix = "";
        }
        Map<String, ScanResults.XIssue> map = new HashMap<>();

        if(!ScanUtils.empty(request.getNamespace()) && !ScanUtils.empty(request.getRepoName()) && !ScanUtils.empty(request.getBranch())) {
            for (ScanResults.XIssue issue : issues) {
                String key = String.format(ScanUtils.JIRA_ISSUE_KEY, issuePrefix, issue.getVulnerability(), issue.getFilename(), request.getBranch());
                map.put(key, issue);
            }
        }
        else{
            for (ScanResults.XIssue issue : issues) {
                String key = String.format(ScanUtils.JIRA_ISSUE_KEY_2, issuePrefix, issue.getVulnerability(), issue.getFilename());
                map.put(key, issue);
            }
        }
        return map;
    }

    private String getBody(ScanResults.XIssue issue, ScanRequest request, String fileUrl){
        StringBuilder body = new StringBuilder();
        if(!ScanUtils.empty(request.getNamespace()) && !ScanUtils.empty(request.getRepoName()) && !ScanUtils.empty(request.getBranch())) {
            body.append(String.format(ScanUtils.JIRA_ISSUE_BODY, issue.getVulnerability(), issue.getFilename(), request.getBranch())).append(ScanUtils.CRLF).append(ScanUtils.CRLF);
        }
        else{
            body.append(String.format(ScanUtils.JIRA_ISSUE_BODY_2, issue.getVulnerability(), issue.getFilename())).append(ScanUtils.CRLF).append(ScanUtils.CRLF);
        }
        /*if(!ScanUtils.empty(issue.getDescription())) {
            body.append("_").append(issue.getDescription().trim()).append("_").append(ScanUtils.CRLF).append(ScanUtils.CRLF);
        }*/
        if(!ScanUtils.empty(issue.getDescription())) {
            body.append(issue.getDescription().trim()).append(ScanUtils.CRLF).append(ScanUtils.CRLF);
        }

        if(!ScanUtils.empty(request.getNamespace())) {
            body.append("*Namespace:* ").append(request.getNamespace()).append(ScanUtils.CRLF);
        }
        if(!ScanUtils.empty(request.getApplication())) {
            body.append("*Application:* ").append(request.getApplication()).append(ScanUtils.CRLF);
        }
        if(!ScanUtils.empty(request.getProject())) {
            body.append("*Cx-Project:* ").append(request.getProject()).append(ScanUtils.CRLF);
        }
        if(!ScanUtils.empty(request.getTeam())) {
            body.append("*Cx-Team:* ").append(request.getTeam()).append(ScanUtils.CRLF);
        }
        if(!ScanUtils.empty(request.getRepoUrl())) {
            body.append("*Repository:* ").append(request.getRepoUrl()).append(ScanUtils.CRLF);
        }
        if(!ScanUtils.empty(request.getBranch())) {
            body.append("*Branch:* ").append(request.getBranch()).append(ScanUtils.CRLF);
        }
        if(!ScanUtils.empty(issue.getSeverity())) {
            body.append("*Severity:* ").append(issue.getSeverity()).append(ScanUtils.CRLF);
        }
        if(!ScanUtils.empty(issue.getCwe())) {
            body.append("*CWE:* ").append(issue.getCwe()).append(ScanUtils.CRLF);
        }

        body.append(ScanUtils.CRLF).append("*Addition Info*").append(ScanUtils.CRLF).append("----").append(ScanUtils.CRLF);
        if(issue.getLink() != null && !issue.getLink().isEmpty()){
            body.append("[Link|").append(issue.getLink()).append("]").append(ScanUtils.CRLF);
        }
        if(!ScanUtils.empty(issue.getCwe())) {
            body.append("[Mitre Details|").append(String.format(machinaProperties.getMitreUrl(), issue.getCwe())).append("]").append(ScanUtils.CRLF);
        }
        if(!ScanUtils.empty(machinaProperties.getCodebashUrl())) {
            body.append("[Training|").append(machinaProperties.getCodebashUrl()).append("]").append(ScanUtils.CRLF);
        }
        if(!ScanUtils.empty(machinaProperties.getWikiUrl())) {
            body.append("[Guidance|").append(machinaProperties.getWikiUrl()).append("]").append(ScanUtils.CRLF);
        }
        if(issue.getDetails() != null && !issue.getDetails().isEmpty()) {
            body.append("Lines: ");
            for (Map.Entry<Integer, String> entry : issue.getDetails().entrySet()) {
                if (!ScanUtils.empty(fileUrl)) {
                    if (entry.getKey() != null) {
                        body.append("[").append(entry.getKey()).append("|").append(fileUrl).append("#L").append(entry.getKey()).append("] ");
                    }
                } else {
                    if (entry.getKey() != null) {
                        body.append(entry.getKey()).append(" ");
                    }
                }
            }

            body.append(ScanUtils.CRLF).append(ScanUtils.CRLF);
            for (Map.Entry<Integer, String> entry : issue.getDetails().entrySet()){
                if(entry.getKey() != null && entry.getValue() != null){
                    body.append("----").append(ScanUtils.CRLF);
                    if(!ScanUtils.empty(fileUrl)) {
                        body.append("[Line #").append(entry.getKey()).append(":|").append(fileUrl).append("#L").append(entry.getKey()).append("]").append(ScanUtils.CRLF);
                    }
                    else {
                        body.append("Line #").append(entry.getKey()).append(ScanUtils.CRLF);
                    }
                    //todo handle bitbucket differences
                    body.append("{code}").append(ScanUtils.CRLF);
                    body.append(entry.getValue()).append(ScanUtils.CRLF);
                    body.append("{code}").append(ScanUtils.CRLF);
                }
            }
            body.append("----").append(ScanUtils.CRLF);
        }

        if(issue.getOsaDetails()!=null){
            for(ScanResults.OsaDetails o: issue.getOsaDetails()){
                body.append(ScanUtils.CRLF);
                if(!ScanUtils.empty(o.getCve())) {
                    body.append("h3.").append(o.getCve()).append(ScanUtils.CRLF);
                }
                body.append("{noformat}");
                if(!ScanUtils.empty(o.getSeverity())) {
                    body.append("Severity: ").append(o.getSeverity()).append(ScanUtils.CRLF);
                }
                if(!ScanUtils.empty(o.getVersion())) {
                    body.append("Version: ").append(o.getVersion()).append(ScanUtils.CRLF);
                }
                if(!ScanUtils.empty(o.getDescription())) {
                    body.append("Description: ").append(o.getDescription()).append(ScanUtils.CRLF);
                }
                if(!ScanUtils.empty(o.getRecommendation())){
                    body.append("Recommendation: ").append(o.getRecommendation()).append(ScanUtils.CRLF);
                }
                if(!ScanUtils.empty(o.getUrl())) {
                    body.append("URL: ").append(o.getUrl());
                }
                body.append("{noformat}");
                body.append(ScanUtils.CRLF);
            }
        }

        return body.toString();
    }


    Map<String, List<String>> process(ScanResults results, ScanRequest request) throws JiraClientException{
        Map<String, ScanResults.XIssue> map;
        Map<String, Issue> jiraMap;
        List<String> newIssues = new ArrayList<>();
        List<String> updatedIssues = new ArrayList<>();
        List<String> closedIssues = new ArrayList<>();

        log.info("Processing Results and publishing findings to Jira");

        List<Issue> issues = this.getIssues(request);
        map = this.getIssueMap(results.getXIssues(), request);
        jiraMap = this.getJiraIssueMap(issues);

        for (Map.Entry<String, ScanResults.XIssue> xIssue : map.entrySet()){
            try {
                ScanResults.XIssue currentIssue = xIssue.getValue();

                /*Issue already exists -> update and comment*/
                if (jiraMap.containsKey(xIssue.getKey())) {
                    Issue i = jiraMap.get(xIssue.getKey());

                    /*Ignore any with label indicating false positive*/
                    if (!i.getLabels().contains(jiraProperties.getFalsePositiveLabel())) {  //TODO handle FALSE_POSITIVE status
                        log.debug("Issue still exists.  Updating issue with key {}", xIssue.getKey());
                        Issue updatedIssue = this.updateIssue(i.getKey(), currentIssue, request);
                        if (updatedIssue != null) {
                            log.debug("Update completed for issue #{}", updatedIssue.getKey());
                            updatedIssues.add(updatedIssue.getKey());
                            addCommentToBug(i.getKey(), "Issue still remains");
                        }
                    } else {
                        log.info("Skipping issue marked as false-positive or has False Positive state with key {}", xIssue.getKey());
                    }
                }
                else {
                    /*Create the new issue*/
                    log.debug("Creating new issue with key {}", xIssue.getKey());
                    String newIssue =  this.createIssue(currentIssue, request);
                    newIssues.add(newIssue);
                    log.info("New issue created. #{}", newIssue);
                }
            }catch(RestClientException e){
                log.error("Error occurred while processing issue with key {}",xIssue.getKey(), e);
                throw new JiraClientException();
            }
        }

        /*Check if an issue exists in Jira but not within results and close if not*/
        for (Map.Entry<String, Issue> jiraIssue : jiraMap.entrySet()){
            try {
                if (!map.containsKey(jiraIssue.getKey())) {
                    if (request.getBugTracker().getOpenStatus().contains(jiraIssue.getValue().getStatus().getName())) {
                        /*Close the issue*/
                        log.info("Closing issue #{} with key {}", jiraIssue.getKey(), jiraIssue.getKey());
                        this.transitionCloseIssue(jiraIssue.getValue().getKey(),
                                request.getBugTracker().getCloseTransition(), request.getBugTracker());
                        closedIssues.add(jiraIssue.getValue().getKey());
                    }
                }
            }catch(HttpClientErrorException e){
                log.error("Error occurred while processing issue with key {} {}", jiraIssue.getKey(), e);
            }
        }

        return ImmutableMap.of(
                "new", newIssues,
                "updated", updatedIssues,
                "closed", closedIssues
        );
    }
}
