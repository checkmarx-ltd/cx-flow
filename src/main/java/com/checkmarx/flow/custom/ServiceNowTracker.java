package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.ServiceNowProperties;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.servicenow.Incident;
import com.checkmarx.flow.dto.servicenow.Result;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service Now Issue Tracker custom integration. It provides Service Now
 * Incident/Issue management services: create, search, update, close.
 */
@Service("ServiceNow")
public class ServiceNowTracker implements IssueTracker {
    private static final String TRANSITION_CLOSE = "7";
    private static final String TRANSITION_OPEN = "1";
    private static final int MAX_RECORDS = 1000;

    private static final Logger log = LoggerFactory.getLogger(ServiceNowTracker.class);
    private static final String INCIDENTS = "/incident";
    private static final String CLOSE_CODE = "Closed/Resolved by Caller";
    private static final String CLOSING_NOTE = "Closing issue";

    private RestOperations restOperations;

    @Autowired
    private ServiceNowProperties properties;

    @Autowired
    private FlowProperties flowProperties;

    @Override
    public void init(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Initializing Service Now Tracker");

        if(ScanUtils.empty(request.getNamespace()) ||
                ScanUtils.empty(request.getRepoName()) ||
                ScanUtils.empty(request.getBranch())) {
            throw new MachinaException("Namespace / RepoName / Branch are required");
        }

        if(ScanUtils.empty(properties.getApiUrl())) {
            throw new MachinaException("Service Now API Url must be provided in property config");
        }

        if( ScanUtils.empty(properties.getUsername()) ||
                ScanUtils.empty(properties.getPassword() )){
            throw new MachinaException("Service Now API Rest Call requires username and password");
        }

        restOperations = new RestTemplateBuilder()
                            .basicAuthentication(properties.getUsername(), properties.getPassword())
                            .build();

        createSeviceNowTags(request);
    }

    /**
     * Create Service Now Tag List out of the ScanRequest.
     * @param request
     */
    private void createSeviceNowTags(ScanRequest request) {
        log.info("Creating Service Now tags list");

        String application = request.getApplication();
        String namespace = request.getNamespace();
        String repoName = request.getRepoName();
        String branch = request.getBranch();

        String appTag = properties.getAppLabelPrefix().concat(":").concat(application);
        String ownerTag = properties.getOwnerLabelPrefix().concat(":").concat(namespace);
        String repoTag = properties.getRepoLabelPrefix().concat(":").concat(repoName);
        String branchTag = properties.getBranchLabelPrefix().concat(":").concat(branch);

        JSONArray tagsList = new JSONArray();
        // product tag
        tagsList.put(request.getProduct().getProduct());

        // tracking by 'application' or 'repo'
        if (!flowProperties.isTrackApplicationOnly()
                && !ScanUtils.empty(namespace)
                && !ScanUtils.empty(repoName)
                && !ScanUtils.empty(branch)) {
            tagsList.put(ownerTag);
            tagsList.put(repoTag);
            tagsList.put(branchTag);
        } else if (!ScanUtils.empty(application)) {
            tagsList.put(appTag);
        } else {
            tagsList.put(appTag);
        }

        request.putAdditionalMetadata("tagsList", tagsList.toString());
    }

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Finalizing Service Now Defect Processing");
    }

    @Override
    public String getFalsePositiveLabel() throws MachinaException {
        return properties.getFalsePositiveLabel();
    }

    /**
     * Get Incidents/Issues from Service Now.
     * @param request
     * @return issues collection of data.
     * @throws MachinaException
     */
    @Override
    public List<Issue> getIssues(ScanRequest request) throws MachinaException {
        log.debug("Executing getIssues Service Now API call");
        String apiRequest = createServiceNowRequest(request);
        try {
            Optional<Result> res = Optional.ofNullable(restOperations.getForObject(apiRequest, Result.class));
            if (res.isPresent()) {
                return res.get().getIncidents()
                        .stream()
                        .map(i -> this.mapToIssue(i))
                        .collect(Collectors.toList());
            }
        } catch(RestClientException e) {
            log.error("Error occurred while fetching ServiceNow Issues");
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaRuntimeException();
        }
        return Lists.newArrayList();
    }

    /**
     * Create Service Now request based on the ScanRequest params.
     * @param request
     * @return query string value.
     */
    private String createServiceNowRequest(ScanRequest request) {
        if(ScanUtils.emptyObj(request)){
            throw new RuntimeException("ScanRequest object is empty");
        }
        String tag = createServiceNowTag(request);
        return String.format("%s%s?comments=%s&sysparm_limit=", properties.getApiUrl(), INCIDENTS, tag, MAX_RECORDS);
    }

    /**
     * Convert Incident object into Issue
     * @param incident
     * @return Issue object.
     */
    private Issue mapToIssue(Incident incident) {
        final Issue issue = new Issue();
        issue.setId(incident.getSysId());
        issue.setState(incident.getState());
        issue.setBody(incident.getDescription());
        issue.setTitle(incident.getShortDescription());
        issue.setLabels(Lists.newArrayList());
        issue.setMetadata(Maps.newHashMap());
        issue.setUrl(properties.getUrl());
        return issue;
    }

    @Override
    public Issue createIssue(ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        log.debug("Executing createIssue ServiceNow API call");
        try {
            Incident incident = getCreateIncident(resultIssue, request);
            String query = String.format("%s%s", properties.getApiUrl(), INCIDENTS);
            URI uri = restOperations.postForLocation(query, incident);
            String sysId = getSysID(uri.getPath());
            return getIncidentByIDConvertToIssue(sysId).get();
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while creating ServiceNow Issue");
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaRuntimeException();
        }
    }

    /**
     * Get Sys_ID param out of the URI
     * @param path
     * @return sys_id variable.
     */
    private String getSysID(String path) {
        if(path == null || path.length() == 0){
            throw new MachinaRuntimeException("Error - getSysID is null.");
        }
        int index = path.split("/").length;
        return index > 0 ? path.split("/")[index-1] : "";
    }

    /**
     * Find Incident By Sys ID and convert into Issue
     * @return Issue object.
     */
    private Optional<Issue> getIncidentByIDConvertToIssue(String sysId) {
        log.debug("Executing getIncidentByIDConvertToIssue");
        try {
            String apiRequest = String.format("%s%s?sys_id=%s", properties.getApiUrl(), INCIDENTS, sysId);
            Optional<Result> res = Optional.ofNullable(restOperations.getForObject(apiRequest, Result.class));
            if (res.isPresent()) {
                return res.get().getIncidents()
                        .stream()
                        .map(i -> this.mapToIssue(i))
                        .findFirst();
            }
        } catch(RestClientException e) {
            log.error("Error occurred while fetching ServiceNow Issue");
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaRuntimeException();
        }
        return Optional.empty();
    }

    /**
     * Generate Tag out of the ScanRequest object.
     * @param request
     * @return service now tag value.
     */
    private String createServiceNowTag(ScanRequest request) {
        String tag = "";

        String application = request.getApplication();
        String repoName    = request.getRepoName();
        String namespace   = request.getNamespace();
        String branch      = request.getBranch();

        if(!flowProperties.isTrackApplicationOnly()) {
            // This is the best layer of control
            tag += "((";
            tag += String.format("(Tags.Name = \"%s:%s\") AND ", properties.getOwnerLabelPrefix(), namespace);
            tag += String.format("(Tags.Name = \"%s:%s\")) AND ", properties.getRepoLabelPrefix(), repoName);
            tag += String.format("(Tags.Name = \"%s:%s\")", properties.getBranchLabelPrefix(), branch);
            tag += ")";
        } else if (!ScanUtils.empty(application)) {
            // We are only track in application name, this isn't as a good as the repo + branch
            tag += String.format("(Tags.Name = \"%s:%s\")", properties.getAppLabelPrefix(), application);
        } else {
            // In this event all we can do track if this is a Cx application tag, this weak though
            tag += String.format("(Tags.Name = \"%s\")", request.getProduct().getProduct());
        }
        return tag;
    }

    @Override
    public void closeIssue(Issue issue, ScanRequest request) throws MachinaException {
        log.info("Executing updateIssue Service Now API call");
        Incident incident = getCloseIncident(request);
        try {
            String query = String.format("%s%s/%s", properties.getApiUrl(), INCIDENTS, issue.getId());
            restOperations.put(query, incident);
        } catch (HttpClientErrorException e) {
            log.error("Error closing issue.Details are:" + e.getMessage());
            throw new MachinaRuntimeException(e);
        }
    }

    @Override
    public Issue updateIssue(Issue issue, ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        log.info("Executing updateIssue Service Now API call");
        Incident incident = updateIncidentFromIssue(issue, request, resultIssue);
        try {
            String query = String.format("%s%s/%s", properties.getApiUrl(), INCIDENTS, issue.getId());
            this.addComment(incident, resultIssue.getGitUrl(),"Issue still exists. ");
            restOperations.put(query, incident);
            return getIssues(request).stream().findFirst()
                    .orElseThrow(() -> new MachinaException("Incident record hasn't been found."));
        } catch (HttpClientErrorException e) {
            log.error("Error updating issue.");
            throw new MachinaRuntimeException();
        }
    }

    /**
     * Add a comment to an existing Service Now Issue
     *
     * @param incident
     * @param issueUrl URL for specific GitHub Issue
     * @param comment  Comment to append to the GitHub Issue
     */
    private void addComment(Incident incident, String issueUrl, String comment) {
        String note = String.format("Artifact - %s\r\nText - %s",issueUrl, comment);
        incident.setWorkNotes(note);
    }

    /**
     * Update Incident from Issue.
     *
     * @param issue
     * @param request
     * @param resultIssue
     * @return Incident object.
     */
    private Incident updateIncidentFromIssue(Issue issue, ScanRequest request, ScanResults.XIssue resultIssue) {
            Incident incident = new Incident();
            incident.setSysId(issue.getId());
            incident.setSeverity(resultIssue.getSeverity());
            incident.setState(TRANSITION_OPEN);
            return incident;
    }

    @Override
    public String getIssueKey(Issue issue, ScanRequest request) {
        return issue.getTitle();
    }

    @Override
    public String getXIssueKey(ScanResults.XIssue issue, ScanRequest request) {
        if(flowProperties.isTrackApplicationOnly() || ScanUtils.empty(request.getBranch())){
            return String.format("%s @ %s", issue.getVulnerability(), issue.getFilename());
        }
        else {
            return String.format("%s @ %s [%s]", issue.getVulnerability(), issue.getFilename(), request.getBranch());
        }
    }

    @Override
    public boolean isIssueClosed(Issue issue, ScanRequest request) {
        if(issue.getState() == null){
            return true;
        }
        return issue.getState().equals(TRANSITION_OPEN);
    }

    @Override
    public boolean isIssueOpened(Issue issue, ScanRequest request) {
        if(issue.getState() == null){
            return true;
        }
        return issue.getState().equals(TRANSITION_OPEN);
    }

    /**
     * Create Service Now object out of the Issue/ScanRequest for a new/update issue.
     * @param resultIssue
     * @param request
     * @return Incident object
     */
    private Incident getCreateIncident(ScanResults.XIssue resultIssue, ScanRequest request) {
        String tag   = createServiceNowTag(request);
        String title = getXIssueKey(resultIssue, request);
        String body  = ScanUtils.getTextBody(resultIssue, request, flowProperties);

        Incident incident = new Incident();
        incident.setShortDescription(title);
        incident.setDescription(convertToText(body));
        incident.setSeverity(resultIssue.getSeverity());
        incident.setComments(tag);
        incident.setWorkNotes(request.getAdditionalMetadata("tagsList"));
        incident.setState(TRANSITION_OPEN);

        return incident;
    }

    /**
     * Create Service Now object out of the Issue/ScanRequest for close issue.
     * @param request ScanRequest object
     * @return Incident object
     */
    private Incident getCloseIncident(ScanRequest request) {
        Incident incident = new Incident();
        incident.setSysId(request.getId());
        incident.setState(TRANSITION_CLOSE);
        incident.setCloseNotes(String.format("Closing reason: %s", CLOSING_NOTE));
        incident.setCloseCode(CLOSE_CODE);

        return incident;
    }

    /**
     * Strip off html tags from text
     * @param value
     * @return - string with stripped off html tags.
     */
    private String convertToText(String value) {
        return value.replaceAll("<.+?>", "");
    }
}
