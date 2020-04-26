package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanDetails;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxProject;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.CxClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.checkmarx.flow.exception.ExitThrowable.exit;
import static com.checkmarx.sdk.config.Constants.UNKNOWN_INT;

@Service
@RequiredArgsConstructor
public class FlowService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(FlowService.class);

    private static final String ERROR_BREAK_MSG = "Exiting with Error code 10 due to issues present";

    private final CxClient cxService;
    private final EmailService emailService;
    private final CxProperties cxProperties;
    private final FlowProperties flowProperties;
    private final ResultsService resultsService;
    private final HelperService helperService;
    private final SastScannerService sastScannerService;

    private ScanDetails scanDetails = null;

    @Async("webHook")
    public void initiateAutomation(ScanRequest request) {
        Map<String, Object>  emailCtx = new HashMap<>();
        try {
            if (request.getProduct().equals(ScanRequest.Product.CX)) {
                if(!ScanUtils.anyEmpty(request.getNamespace(), request.getRepoName(), request.getRepoUrl())) {
                    emailCtx.put("message", "Checkmarx Scan has been submitted for "
                            .concat(request.getNamespace()).concat("/").concat(request.getRepoName()).concat(" - ")
                            .concat(request.getRepoUrl()));
                    emailCtx.put("heading", "Scan Request Submitted");
                    emailService.sendmail(request.getEmail(), "Checkmarx Scan Submitted for ".concat(request.getNamespace()).concat("/").concat(request.getRepoName()), emailCtx, "message.html");
                }
                CompletableFuture<ScanResults> results = sastScannerService.executeCxScanFlow(request, null);
                if(results.isCompletedExceptionally()){
                    log.error("An error occurred while executing process");
                }
            } else {
                log.warn("Unknown Product type of {}, exiting", request.getProduct());
            }
        } catch (MachinaException e){
            log.error("Machina Exception has occurred.", e);
            emailCtx.put("message", "Error occurred during scan/bug tracking process for "
                    .concat(request.getNamespace()).concat("/").concat(request.getRepoName()).concat(" - ")
                    .concat(request.getRepoUrl()).concat("  Error: ").concat(e.getMessage()));
            emailCtx.put("heading","Error occurred during scan");
            emailService.sendmail(request.getEmail(), "Error occurred for ".concat(request.getNamespace()).concat("/").concat(request.getRepoName()), emailCtx, "message-error.html");
        }
    }

    public void cxParseResults(ScanRequest request, File file) throws ExitThrowable {
        try {
            ScanResults results = cxService.getReportContent(file, request.getFilters());
            resultsService.processResults(request, results, scanDetails);
            if(flowProperties.isBreakBuild() && results !=null && results.getXIssues()!=null && !results.getXIssues().isEmpty()){
                log.error(ERROR_BREAK_MSG);
                exit(10);
            }
        } catch (MachinaException | CheckmarxException e) {
            log.error("Error occurred while processing results file", e);
            exit(3);
        }
    }

    public void cxOsaParseResults(ScanRequest request, File file, File libs) throws ExitThrowable {
        try {
            ScanResults results = cxService.getOsaReportContent(file, libs, request.getFilters());
            resultsService.processResults(request, results, scanDetails);
            if(flowProperties.isBreakBuild() && results !=null && results.getXIssues()!=null && !results.getXIssues().isEmpty()){
                log.error(ERROR_BREAK_MSG);
                exit(10);
            }
        } catch (MachinaException | CheckmarxException e) {
            log.error("Error occurred while processing results file(s)", e);
            exit(3);
        }
    }

    public CompletableFuture<ScanResults> cxGetResults(ScanRequest request, CxProject cxProject){
        try {
            CxProject project;

            if(cxProject == null) {
                String team = request.getTeam();
                if(ScanUtils.empty(team)){
                    //if the team is not provided, use the default
                    team = cxProperties.getTeam();
                    request.setTeam(team);
                }
                if (!team.startsWith(cxProperties.getTeamPathSeparator())) {
                    team = cxProperties.getTeamPathSeparator().concat(team);
                }
                String teamId = cxService.getTeamId(team);
                Integer projectId = cxService.getProjectId(teamId, request.getProject());
                if(projectId.equals(UNKNOWN_INT)){
                    log.warn("No project found for {}", request.getProject());
                    CompletableFuture<ScanResults> x = new CompletableFuture<>();
                    x.complete(null);
                    return x;
                }
                project = cxService.getProject(projectId);

            }
            else {
                project = cxProject;
            }
            Integer scanId = cxService.getLastScanId(project.getId());
            if(scanId.equals(UNKNOWN_INT)){
                log.warn("No Scan Results to process for project {}", project.getName());
                CompletableFuture<ScanResults> x = new CompletableFuture<>();
                x.complete(null);
                return x;
            }
            else {
                getCxFields(project, request);
                //null is passed for osaScanId as it is not applicable here and will be ignored
                return resultsService.processScanResultsAsync(request, project.getId(), scanId, null, request.getFilters());
            }

        } catch (MachinaException | CheckmarxException e) {
            log.error("Error occurred while processing results for {}{}", request.getTeam(), request.getProject(), e);
            CompletableFuture<ScanResults> x = new CompletableFuture<>();
            x.completeExceptionally(e);
            return x;
        }
    }

    private void getCxFields(CxProject project, ScanRequest request) {
        if(project == null) { return; }

        Map<String, String> fields = new HashMap<>();
        for(CxProject.CustomField field : project.getCustomFields()){
            String name = field.getName();
            String value = field.getValue();
            if(!ScanUtils.empty(name) && !ScanUtils.empty(value)) {
                fields.put(name, value);
            }
        }
        if(!ScanUtils.empty(cxProperties.getJiraProjectField())){
            String jiraProject = fields.get(cxProperties.getJiraProjectField());
            if(!ScanUtils.empty(jiraProject)) {
                request.getBugTracker().setProjectKey(jiraProject);
            }
        }
        if(!ScanUtils.empty(cxProperties.getJiraIssuetypeField())) {
            String jiraIssuetype = fields.get(cxProperties.getJiraIssuetypeField());
            if (!ScanUtils.empty(jiraIssuetype)) {
                request.getBugTracker().setIssueType(jiraIssuetype);
            }
        }
        if(!ScanUtils.empty(cxProperties.getJiraCustomField()) &&
                (fields.get(cxProperties.getJiraCustomField()) != null) && !fields.get(cxProperties.getJiraCustomField()).isEmpty()){
            request.getBugTracker().setFields(ScanUtils.getCustomFieldsFromCx(fields.get(cxProperties.getJiraCustomField())));
        }

        if(!ScanUtils.empty(cxProperties.getJiraAssigneeField())){
            String assignee = fields.get(cxProperties.getJiraAssigneeField());
            if(!ScanUtils.empty(assignee)) {
                request.getBugTracker().setAssignee(assignee);
            }
        }

        request.setCxFields(fields);
    }

    /**
     * Process Projects in batch mode - JIRA ONLY
     */
    public void cxBatch(ScanRequest originalRequest) throws ExitThrowable {
        try {
            List<CxProject> projects;
            List<CompletableFuture<ScanResults>> processes = new ArrayList<>();
            //Get all projects
            if(ScanUtils.empty(originalRequest.getTeam())){
                projects = cxService.getProjects();
            }
            else{ //Get projects for the provided team
                String team = originalRequest.getTeam();
                if(!team.startsWith(cxProperties.getTeamPathSeparator())){
                    team = cxProperties.getTeamPathSeparator().concat(team);
                }
                String teamId = cxService.getTeamId(team);
                projects = cxService.getProjects(teamId);
            }
            for(CxProject project: projects){
                ScanRequest request = new ScanRequest(originalRequest);
                String name = project.getName().replaceAll("[^a-zA-Z0-9-_]+","_");
                //TODO set team when entire instance batch mode
                helperService.getShortUid(request); //update new request object with a unique id for thread log monitoring
                request.setProject(name);
                request.setApplication(name);
                processes.add(cxGetResults(request, project));
            }
            log.info("Waiting for processing to complete");
            processes.forEach(CompletableFuture::join);

        } catch ( CheckmarxException e) {
            log.error("Error occurred while processing projects in batch mode", e);
            exit(3);
        }
    }
}
