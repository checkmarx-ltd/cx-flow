package com.checkmarx.flow.custom;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.ITopProperties;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.itop.incident.CloseRequestIncident;
import com.checkmarx.flow.dto.itop.incident.CreateRequestIncident;
import com.checkmarx.flow.dto.itop.incident.Incident;
import com.checkmarx.flow.dto.itop.incident.IncidentResponse;
import com.checkmarx.flow.dto.itop.incident.UpdateRequestIncident;
import com.checkmarx.flow.dto.itop.incident.UserRequestResponse;
import com.checkmarx.flow.dto.itop.incident.UserRequests;
import com.checkmarx.flow.dto.itop.incident.CloseRequestIncident.CloseRequestIncidentFields;
import com.checkmarx.flow.dto.itop.incident.CloseRequestIncident.CloseRequestKey;
import com.checkmarx.flow.dto.itop.incident.CreateRequestIncident.CreateRequestFields;
import com.checkmarx.flow.dto.itop.incident.IncidentResponse.Objects.UserRequest;
import com.checkmarx.flow.dto.itop.incident.IncidentResponse.Objects.UserRequest.Fields;
import com.checkmarx.flow.dto.itop.incident.UpdateRequestIncident.UpdateRequestIncidentFields;
import com.checkmarx.flow.dto.itop.incident.UpdateRequestIncident.UpdateRequestKey;
import com.checkmarx.flow.dto.itop.incident.UserRequestResponse.UserRequestFields;
import com.checkmarx.flow.dto.itop.organization.OrganizationResponse;
import com.checkmarx.flow.dto.itop.service.ServiceResponse;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.flow.utils.HTMLHelper;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.logstash.logback.encoder.org.apache.commons.lang.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service("ITop")
public class ITopTracker implements IssueTracker {
    private static final String TRANSITION_RESOLVED = "resolved";
    private static final String TRANSITION_CLOSED = "closed";
    private static final String TRANSITION_OPEN = "new";
    private final ITopProperties properties;
    private RestOperations restOperations;
    private static final Logger log = LoggerFactory.getLogger(ITopTracker.class);

    @Autowired
    public ITopTracker(ITopProperties properties) {
        this.properties = properties;
    }
    @Autowired
    private FlowProperties flowProperties;

    public void init(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Initializing iTop Tracker...");
        // Necessary property checks
        if (ScanUtils.empty(properties.getRestEndpointUrl())) {
            throw new MachinaException("iTop API Url must be provided in property config (ex : http://localhost/itop/webservices/rest.php)");
        }
    
        if (ScanUtils.empty(properties.getApiUser())) {
            throw new MachinaException("iTop API Rest Call requires user with API access");
        }
    
        if (ScanUtils.empty(properties.getApiPassword())) {
            throw new MachinaException("iTop API Rest Call requires password for user with API access");
        }

        if (ScanUtils.empty(properties.getOrgName())) {
            throw new MachinaException("iTop API Rest Call requires organization name");
        }

        if (ScanUtils.empty(properties.getServiceName())) {
            throw new MachinaException("iTop API Rest Call requires service name");
        }

        // Configure restOperations for future API requests
        restOperations = new RestTemplateBuilder()
                .additionalMessageConverters(new MappingJackson2HttpMessageConverter(), new FormHttpMessageConverter())
                .basicAuthentication(properties.getApiUser(), properties.getApiPassword())
                .build();
        
        // Check if service exists
        String url = properties.getRestEndpointUrl() + "?version=1.4&json_data={\"operation\": \"core/get\", \"class\": \"Service\", \"key\": \"SELECT Service WHERE name LIKE '"+ properties.getServiceName() +"'\"}";
        ResponseEntity<ServiceResponse> responseEntity = restOperations.getForEntity(url, ServiceResponse.class);
        ServiceResponse response = responseEntity.getBody();

        // Check for success or failure
        if (response != null && response.getMessage().equals("Found: 1")) {
            log.info("Service found!");
        } else {
            throw new MachinaException("Service not found ! Please verify your configuration.");
        }

        // Check if organization exists
        url = properties.getRestEndpointUrl() + "?version=1.4&json_data={\"operation\": \"core/get\", \"class\": \"Organization\", \"key\": \"SELECT Organization WHERE name LIKE '"+ properties.getOrgName() +"'\"}";
        
        ResponseEntity<OrganizationResponse> responseEntity2 = restOperations.getForEntity(url, OrganizationResponse.class);
        
        OrganizationResponse response2 = responseEntity2.getBody();

        // Check for success or failure
        if (response2 != null && response2.getMessage().equals("Found: 1")) {
            log.info("Organization found!");
        } else {
            throw new MachinaException("Organization not found ! Please verify your configuration.");
        }

        return;
    }

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Finalizing iTop Defect Processing");
    }

    @Override
    public String getFalsePositiveLabel() throws MachinaException {
        return properties.getFalsePositiveLabel();
    }

    public List<Issue> getIssues(ScanRequest request) throws MachinaException {
        String query = properties.getRestEndpointUrl() + "?version=1.4&json_data={\"operation\": \"core/get\", \"class\": \"UserRequest\", \"key\": \"SELECT UserRequest WHERE service_name LIKE '"+ properties.getServiceName() +"'\"}";
        ResponseEntity<UserRequests> response = restOperations.getForEntity(query, UserRequests.class);
        UserRequests userRequests = response.getBody();

        List<Issue> issues = new ArrayList<Issue>();
        if(userRequests.getObjects() == null) {
            return issues;
        }
        Iterator<UserRequestResponse> iteratorUserRequests = userRequests.getObjects().values().iterator();
        for (int i = 0; i < userRequests.getObjects().values().size(); i++) {
            UserRequestResponse currentuserRequest = iteratorUserRequests.next();
            UserRequestFields fields = currentuserRequest.getFields();
            String id = String.valueOf(currentuserRequest.getKey());
            Incident createdIncident = new Incident(id,fields.getRef(),fields.getOrgName(),fields.getStatus(),fields.getTitle(),fields.getDescription(),fields.getPriority(),fields.getUrgency(),fields.getServiceName());
            Issue issueToAdd = convertIncidenttoIssue(createdIncident, request);
            issues.add(issueToAdd);
        }

        return issues;
    }

    public Issue createIssue(ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        log.debug("Executing createIssue iTop API call");
        String errorMessage = "Error occurred while creating Mantis Issue";
        
        Issue issueToReturn = new Issue();

        // Send request to iTop API to create a new incident
        try{
            String title = HTMLHelper.getScanRequestIssueKeyWithDefaultProductValue(request, this, resultIssue);
            String body = HTMLHelper.getTextBody(resultIssue, request, flowProperties);
            String severity = convertSeverityToItop(resultIssue.getSeverity());
            String query = properties.getRestEndpointUrl() + "?version=1.4";

            CreateRequestIncident createRequestIncident = new CreateRequestIncident();
            createRequestIncident.setOperation("core/create");
            createRequestIncident.setComment("Synchronization from CxFlow");
            createRequestIncident.setClassType("UserRequest");
            createRequestIncident.setFields(new CreateRequestFields());
            createRequestIncident.getFields().setOrg_id("SELECT Organization WHERE name = \"" + properties.getOrgName() + "\"");
            createRequestIncident.getFields().setService_id("SELECT Service WHERE name = \"" + properties.getServiceName() + "\"");
            createRequestIncident.getFields().setTitle(title);
            createRequestIncident.getFields().setDescription(body);
            createRequestIncident.getFields().setOrigin("monitoring");
            createRequestIncident.getFields().setPriority(Integer.parseInt(severity));
            createRequestIncident.getFields().setUrgency(Integer.parseInt(severity));

            MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
            map.add("json_data", new ObjectMapper().writeValueAsString(createRequestIncident));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(map, headers);

            ResponseEntity<ObjectNode> response = restOperations.exchange(query, HttpMethod.POST, entity, ObjectNode.class);
            String jsonResponse = response.getBody().toString();
            jsonResponse = jsonResponse.replaceAll("UserRequest::\\d+", "UserRequest");
            
            // Parsing de la réponse JSON dans l'objet IncidentResponse
            ObjectMapper mapper = new ObjectMapper();
            IncidentResponse incidentResponse = mapper.readValue(jsonResponse, IncidentResponse.class);
        
            UserRequest UserRequests = incidentResponse.getObjects().getUserRequest();
            //convert in to string id
            String id = String.valueOf(UserRequests.getKey());
            Fields fields = UserRequests.getFields();

            Incident createdIncident = new Incident(id,fields.getRef(),fields.getOrgName(),fields.getStatus(),fields.getTitle(),fields.getDescription(),fields.getPriority(),fields.getUrgency(),fields.getServiceName());
            issueToReturn = convertIncidenttoIssue(createdIncident, request);
            log.info("iTop API call was sent to create Issue");

        }catch (HttpClientErrorException e) {
            log.error(errorMessage);
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaRuntimeException();
        } catch (HttpMessageNotReadableException e) {
            log.error("Error parsing JSON response: " + e.getMessage());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return issueToReturn;
    }

    /**
     * Create an Issue of CxFlow from a iTop Issue
     * @param resultIssue
     * @param request
     * @return Incident object (Mantis Issue)
     */
    public Issue convertIncidenttoIssue(Incident incident, ScanRequest request){
        String id = incident.getId();
        String url = properties.getRestEndpointUrl();
        url = url.replace("webservices/rest.php", "pages/UI.php");
        url = url + "?operation=details&class=UserRequest&id=" + incident.getId() + "&c[menu]=UserRequest%3AOpenRequests";
        String title = incident.getTitle();
        String body = incident.getDescription();
        String state = incident.getStatus();
        Issue issue = new Issue(id, url, title, body, state, Lists.newArrayList(), Maps.newHashMap());
        
        return issue;
    }
    
    public void closeIssue(Issue issue, ScanRequest request) throws MachinaException {
        
        log.debug("Executing closeIssue iTop API call");
        String errorMessage = "Error occurred while closing Mantis Issue";

        // Send request to iTop API to delete an incident
        try{

            String query = properties.getRestEndpointUrl() + "?version=1.4";

            CloseRequestIncident closeRequestIncident= new CloseRequestIncident();
            closeRequestIncident.setOperation("core/update");
            closeRequestIncident.setComment("Synchronization from CxFlow");
            closeRequestIncident.setClassType("UserRequest");
            closeRequestIncident.setFields(new CloseRequestIncidentFields());
            closeRequestIncident.getFields().setStatus("closed");
            closeRequestIncident.setKey(new CloseRequestKey());
            closeRequestIncident.getKey().setTitle(issue.getTitle()); 


            MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
            map.add("json_data", new ObjectMapper().writeValueAsString(closeRequestIncident));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(map, headers);

            ResponseEntity<ObjectNode> response = restOperations.exchange(query, HttpMethod.POST, entity, ObjectNode.class);
            String jsonResponse = response.getBody().toString();

            //si la réponse contient updated parmis le json
            if(jsonResponse.contains("updated")){
                log.info("iTop API call was sent to close Issue");
            }
            else{
                log.error("iTop API call was sent to close Issue but the response is not correct");
            }


        }catch (HttpClientErrorException e) {
            log.error(errorMessage);
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaRuntimeException();
        } catch (HttpMessageNotReadableException e) {
            log.error("Error parsing JSON response: " + e.getMessage());
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public Issue updateIssue(Issue issue, ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException{

        log.debug("Executing updateIssue iTop API call");
        String errorMessage = "Error occurred while updating Mantis Issue";

        // Send request to iTop API to delete an incident
        try{
            Incident incident = convertIssuetoIncident(resultIssue, request);

            String query = properties.getRestEndpointUrl() + "?version=1.4";

            UpdateRequestIncident updateRequestIncident = new UpdateRequestIncident();
            updateRequestIncident.setOperation("core/update");
            updateRequestIncident.setComment("Synchronization from CxFlow");
            updateRequestIncident.setClassType("UserRequest");
            updateRequestIncident.setFields(new UpdateRequestIncidentFields());
            updateRequestIncident.getFields().setTitle(incident.getTitle());
            updateRequestIncident.getFields().setDescription(incident.getDescription());
            updateRequestIncident.getFields().setPriority(Integer.parseInt(incident.getPriority()));
            updateRequestIncident.getFields().setUrgency(Integer.parseInt(incident.getUrgency()));
            updateRequestIncident.getFields().setStatus(incident.getStatus());
            updateRequestIncident.setKey(new UpdateRequestKey());
            updateRequestIncident.getKey().setTitle(incident.getTitle()); 


            MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
            map.add("json_data", new ObjectMapper().writeValueAsString(updateRequestIncident));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(map, headers);

            ResponseEntity<ObjectNode> response = restOperations.exchange(query, HttpMethod.POST, entity, ObjectNode.class);
            String jsonResponse = response.getBody().toString();
            
            //si la réponse contient updated parmis le json
            if(jsonResponse.contains("updated")){
                log.info("iTop API call was sent to update Issue");
                return issue;
            }
            else{
                log.error("iTop API call was sent to update Issue but the response is not correct");
            }
        }catch (HttpClientErrorException e) {
            log.error(errorMessage);
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaRuntimeException();
        } catch (HttpMessageNotReadableException e) {
            log.error("Error parsing JSON response: " + e.getMessage());
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Create iTop object out of the Issue/ScanRequest.
     * @param resultIssue
     * @param request
     * @return Incident object (iTop Issue)
     */
    public Incident convertIssuetoIncident(ScanResults.XIssue resultIssue, ScanRequest request) {
        Incident incident = new Incident();
        String title = HTMLHelper.getScanRequestIssueKeyWithDefaultProductValue(request, this, resultIssue);
        String body  = HTMLHelper.getTextBody(resultIssue, request, flowProperties);

        String severity = convertSeverityToItop(resultIssue.getSeverity());

        incident.setTitle(title);
        incident.setDescription(body);
        incident.setServiceName(properties.getServiceName());
        incident.setOrgName(properties.getOrgName());
        incident.setPriority(severity);
        incident.setUrgency(severity);
        incident.setStatus("new");
        
        return incident;
    }

    /**
     * Convert Severity of Checkmarx to a Severity of iTop
     * @param request
     */
    public String convertSeverityToItop(String severity) {
        switch (severity.toLowerCase()) {
            case "high":
                return "2";
            case "medium":
                return "3";
            case "low":
                return "4";
            case "info":
                return "4";
            default:
                throw new IllegalArgumentException("Invalid severity: " + severity);
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

    public boolean isIssueClosed(Issue issue, ScanRequest request){
        if(issue.getState() == null){
            return true;
        }
        if(issue.getState().equals(TRANSITION_CLOSED) || issue.getState().equals(TRANSITION_RESOLVED)){
            return true;
        }else{
            return false;
        }
    }

    public boolean isIssueOpened(Issue issue, ScanRequest request){
        if(issue.getState() == null){
            return true;
        }
        return issue.getState().equals(TRANSITION_OPEN);
    }

}