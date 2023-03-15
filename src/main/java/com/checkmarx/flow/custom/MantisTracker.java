package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.mantis.Incident;
import com.checkmarx.flow.dto.mantis.project.MantisProject;
import com.checkmarx.flow.dto.mantis.project.MantisProjectResponse;
import com.checkmarx.flow.dto.mantis.Project;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.checkmarx.flow.dto.mantis.Category;
import com.checkmarx.flow.dto.mantis.Severity;
import com.checkmarx.flow.dto.mantis.Status;
import com.checkmarx.flow.dto.mantis.incident.IncidentResponse;
import com.checkmarx.flow.dto.mantis.incident.IncidentsResponse;
import com.checkmarx.flow.utils.HTMLHelper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.checkmarx.flow.config.MantisProperties;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.sdk.dto.ScanResults;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestOperations;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.List;
import java.util.stream.Collectors;


@Service("Mantis")
public class MantisTracker implements IssueTracker {
    private static final String TRANSITION_RESOLVED = "resolved";
    private static final String TRANSITION_CLOSED = "closed";
    private static final String TRANSITION_OPEN = "new";
    private static final Logger log = LoggerFactory.getLogger(MantisTracker.class);
    private final MantisProperties properties;
    private RestOperations restOperations;

    private MantisProject MainProject;

    @Autowired
    public MantisTracker(MantisProperties properties) {
        this.properties = properties;
    }
    @Autowired
    private FlowProperties flowProperties;

    @Override
    public void init(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Initializing MantisTracker...");
        // Necessary property checks
        if (ScanUtils.empty(properties.getApiUrl())) {
            throw new MachinaException("Mantis API Url must be provided in property config");
        }
    
        if (ScanUtils.empty(properties.getApiToken())) {
            throw new MachinaException("Mantis API Rest Call requires api token");
        }
    
        if (ScanUtils.empty(properties.getProjectID())) {
            throw new MachinaException("Mantis requires Main Project ID");
        }

        // Configure restOperations for future API requests
        restOperations = new RestTemplateBuilder()
                .additionalMessageConverters(new MappingJackson2HttpMessageConverter())
                .defaultHeader("Authorization", properties.getApiToken())
                .build();
    
        // Getting of the main mantis project from its ID (id defined in the Cxflow configuration parameters)
        log.info("Getting Mantis Project for ID " + properties.getProjectID() + "...");
        try {
            String query = properties.getApiUrl() + "/api/rest/projects/" + properties.getProjectID();
            ResponseEntity<MantisProjectResponse> response = restOperations.exchange(query, HttpMethod.GET, null, MantisProjectResponse.class);
            MantisProjectResponse projectResponse = response.getBody();
            MainProject = projectResponse.getProject();
            //si le projet n'existe pas
            if(MainProject == null){
                throw new MachinaException("Mantis project does not exist");
            }
            if (MainProject.getEnabled() == false) {
                throw new MachinaException("Mantis project is not enabled");
            }
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while getting Mantis project");
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaRuntimeException();
        }

    }    

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Finalizing Mantis Defect Processing");
    }

    @Override
    public String getFalsePositiveLabel() throws MachinaException {
        return properties.getFalsePositiveLabel();
    }

    @Override
    public List<Issue> getIssues(ScanRequest request) throws MachinaException {
        // We retrieve the incidents of the project
        String query = properties.getApiUrl() + "/api/rest/issues?project_id=" + properties.getProjectID(); //api url to get incidents from a project
        ResponseEntity<IncidentsResponse> response = restOperations.exchange(query, HttpMethod.GET, null, IncidentsResponse.class);
        IncidentsResponse incidentsResponse = response.getBody();
        List<Incident> incidents = incidentsResponse.getIncidents();
        // Convert the list of Incidents to a list of Issues for CxFlow
        List<Issue> issues = incidents.stream().map(incident -> convertIncidenttoIssue(incident, request)).collect(Collectors.toList());
        return issues;
    }

    @Override
    public Issue createIssue(ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        log.debug("Executing createIssue Mantis API call");
        String errorMessage = "Error occurred while creating Mantis Issue";        

        // Send request to Mantis API to create a new incident
        try{
            String query = properties.getApiUrl() + "/api/rest/issues"; //api url to create a new incident
            Incident incident = convertIssuetoIncident(resultIssue, request);

            IncidentResponse response = restOperations.postForObject(query, incident, IncidentResponse.class);
            Incident createdIncident = response.getIncident();
            
            log.info("Mantis API call was sent to create Issue");

            // Convert Incidents to Issues for CxFlow
            Issue issue = convertIncidenttoIssue(createdIncident, request);
            return issue;
        }catch (HttpClientErrorException e) {
            log.error(errorMessage);
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaRuntimeException();
        }
    }

    /**
     * Create an Issue of CxFlow from a Mantis Issue
     * @param resultIssue
     * @param request
     * @return Incident object (Mantis Issue)
     */
    public Issue convertIncidenttoIssue(Incident incident, ScanRequest request){
        String id = incident.getId();
        String url = properties.getApiUrl() + "/view.php?id=" + incident.getId();
        String title = incident.getSummary();
        String body = incident.getDescription();
        String state = incident.getStatus().getName();
        Issue issue = new Issue(id, url, title, body, state, Lists.newArrayList(), Maps.newHashMap());
        
        return issue;
    }

    /**
     * Create Mantis object out of the Issue/ScanRequest.
     * @param resultIssue
     * @param request
     * @return Incident object (Mantis Issue)
     */
    public Incident convertIssuetoIncident(ScanResults.XIssue resultIssue, ScanRequest request) {
        Incident incident = new Incident();
        String title = HTMLHelper.getScanRequestIssueKeyWithDefaultProductValue(request, this, resultIssue);
        String body  = HTMLHelper.getTextBody(resultIssue, request, flowProperties);

        incident.setSummary(title);
        incident.setDescription(body);
        Severity severity = new Severity(convertSeverityToMantis(resultIssue.getSeverity()));
        incident.setSeverity(severity);
        Project project = new Project(MainProject.getName());
        incident.setProject(project);
        Category category = new Category("Vulnerability");
        incident.setCategory(category);
        Status status = new Status("10", "new", "new", "#fcbdbd");
        incident.setStatus(status);

        return incident;
    }

    /**
     * Convert Severity of Checkmarx to a Severity of Mantis
     * @param request
     */
    public String convertSeverityToMantis(String severity) {
        switch (severity.toLowerCase()) {
            case "high":
                return "crash";
            case "medium":
                return "major";
            case "low":
                return "minor";
            case "info":
                return "text";
            default:
                throw new IllegalArgumentException("Invalid severity: " + severity);
        }
    }

    /**
     * Get an Issue of Mantis by an Id
     * @param request
     * @return Incident object (Mantis Issue)
     */
    public Incident getMantisIssueById(String id){
        String query = properties.getApiUrl() + "/api/rest/issues/" + id; // Api url to get a Mantis Issue by its Id
        ResponseEntity<IncidentsResponse> response = restOperations.getForEntity(query, IncidentsResponse.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            log.info("Mantis API call was sent for getting Issue");
        } else {
            log.error("Error occurred while getting Mantis Issue - HttpStatus code: " + response.getStatusCodeValue());
            throw new MachinaRuntimeException();
        }
        
        Incident incident = response.getBody().getIncidents().get(0);
        return incident;
    }

    @Override
    public void closeIssue(Issue issue, ScanRequest request) throws MachinaException {
        try {
            // We getting the data of the Mantis issue
            Incident incident = getMantisIssueById(issue.getId());
            
            // We modify the status of the incident
            Status closedStatus = new Status(null,TRANSITION_RESOLVED,TRANSITION_RESOLVED,"#d2f5b0");
            incident.setStatus(closedStatus);
        
            // Modify the incident using a PATCH request (PUT request does not exist)
            String query = properties.getApiUrl() + "/api/rest/issues/" + issue.getId(); // Api url to modify a mantis issue
            HttpEntity<Incident> requestEntity = new HttpEntity<>(incident);
            ResponseEntity<IncidentsResponse> response = restOperations.exchange(query, HttpMethod.PATCH, requestEntity, IncidentsResponse.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Mantis API call was sent for closing Issue");
            } else {
                log.error("Error occurred while updating Mantis Issue - HttpStatus code: " + response.getStatusCodeValue());
                throw new MachinaRuntimeException();
            }

        } catch (HttpClientErrorException e) {
            log.error("Error occurred while closing Mantis Issue");
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaRuntimeException();
        }
    }

    @Override
    public Issue updateIssue(Issue issue, ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        try {
            // Convert Issue of CxFlow to an Issue of Mantis
            Incident incident = convertIssuetoIncident(resultIssue, request);
        
            // Modify the incident using a PATCH request (PUT request does not exist)
            String query = properties.getApiUrl() + "/api/rest/issues/" + issue.getId(); // Api url to modify a mantis issue
            HttpEntity<Incident> requestEntity = new HttpEntity<>(incident);
            ResponseEntity<IncidentsResponse> response = restOperations.exchange(query, HttpMethod.PATCH, requestEntity, IncidentsResponse.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Mantis API call was sent for updating Issue");
            } else {
                log.error("Error occurred while updating Mantis Issue - HttpStatus code: " + response.getStatusCodeValue());
                throw new MachinaRuntimeException();
            }

            // the api returns a list of issues containing a single issue which is the one we have modified,
            // so we must retrieve the first incident in incidents response from the list
            IncidentsResponse incidentsResponse = response.getBody();
            List<Incident> incidents = incidentsResponse.getIncidents();
            incident = incidents.get(0);
        
            // Convert Incident from Mantis to Issue of CxFlow
            Issue updatedIssue = convertIncidenttoIssue(incident, request);
            return updatedIssue;
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while updating Mantis Issue");
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaRuntimeException();
        }
        
    }

    @Override
    public String getIssueKey(Issue issue, ScanRequest request) {
        return issue.getTitle();
    }

    @Override
    public String getXIssueKey(ScanResults.XIssue issue, ScanRequest request) {
        if(flowProperties.isTrackApplicationOnly() || ScanUtils.empty(request.getBranch())){
            return String.format(ScanUtils.ISSUE_TITLE_KEY, request.getProduct().getProduct(), issue.getVulnerability(), issue.getFilename());
        }
        else {
            return ScanUtils.isSAST(issue)
                    ? String.format(ScanUtils.ISSUE_TITLE_KEY_WITH_BRANCH, request.getProduct().getProduct(), issue.getVulnerability(), issue.getFilename(), request.getBranch())
                    : ScanUtils.getScaSummaryIssueKey(request, issue);
        }
    }

    @Override
    public boolean isIssueClosed(Issue issue, ScanRequest request) {
        if(issue.getState() == null){
            return true;
        }
        if(issue.getState().equals(TRANSITION_CLOSED) || issue.getState().equals(TRANSITION_RESOLVED)){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean isIssueOpened(Issue issue, ScanRequest request) {
        if(issue.getState() == null){
            return true;
        }
        return issue.getState().equals(TRANSITION_OPEN);
    }

}
