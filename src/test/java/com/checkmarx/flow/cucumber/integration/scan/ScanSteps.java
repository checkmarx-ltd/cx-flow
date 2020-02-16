package com.checkmarx.flow.cucumber.integration.scan;

import com.atlassian.httpclient.api.Common;
import com.checkmarx.flow.CxFlowApplication;

import com.checkmarx.flow.cucumber.integration.scan.AbstractScanSteps;

import com.checkmarx.flow.exception.ExitThrowable;

import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxProject;
import com.checkmarx.sdk.exception.CheckmarxException;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;


@SpringBootTest(classes = { CxFlowApplication.class })
@ActiveProfiles({ "scan", "secrets" })
public class ScanSteps extends AbstractScanSteps {

//    @When("nothing {string}")
//    public void whenDoNothing(String str){}
    
    
    @And("The request sent to SAST will contain exclude-folder {string} and exclude files {string}")
    public void setExcludeFolders(String excludeFolders, String excludeFiles) {
        if(!excludeFiles.equals("")){
            cxProperties.setExcludeFiles(excludeFiles);
            excludeFilesList = Arrays.asList(cxProperties.getExcludeFiles().split(","));
        }
        if(!excludeFolders.equals("")){
            cxProperties.setExcludeFolders(excludeFolders);
            excludeFoldersList = Arrays.asList(cxProperties.getExcludeFolders().split(","));
        }
    }

    @And ("scanned lines of code will be {string}")
    public void checkLinesOfCode(String loc){
        assertEquals(results.getLoc(),loc);
    }
    
    @And("filter-severity is {string} and filter-category is {string} and filter-cwe {string} and filter-status {string}")
    public void setFilters(String severity, String category, String cwe, String status){
        if(severity.equals("") && category.equals("") && cwe.equals("") && status.equals("") ){
            return;
        }
        LinkedList filterList;
        if(!severity.equals("")) {
            filterList = prepareFilterList(severity, Filter.Type.SEVERITY);
            flowProperties.setFilterStatus(filterList);
        }
        if(!category.equals("")) {
            filterList = prepareFilterList(category, Filter.Type.TYPE);
            flowProperties.setFilterCategory(filterList);
        }
        if(!cwe.equals("")) {
            filterList = prepareFilterList(cwe, Filter.Type.CWE);
            flowProperties.setFilterCwe(filterList);
          
        }
        if(!status.equals("")) {
            filterList = prepareFilterList(status, Filter.Type.STATUS);
            flowProperties.setFilterStatus(filterList);
        }
    }

    private LinkedList prepareFilterList(String filterStr, Filter.Type type) {
        LinkedList filterList = new LinkedList<String>();
        String[] filterSplitArr = new String[1];
        if(filterStr.contains(",")) {
            filterSplitArr = filterStr.split(",");
        }else{
            filterSplitArr[0] = filterStr;
        }
        for (String currfilter: filterSplitArr ) {
            Filter filter = new Filter(type, currfilter);
            filters.add(new Filter(type, currfilter));
            filterList.add(filter);
        }
        return filterList;
    }

    @Then ("output file will contain vulnerabilities {int}")
    public void verifyOutput(int number){
        
        ScanDTO scanDTO = callSast();
        clearAfterTest(scanDTO);
        
        List<ScanResults.XIssue> issues = results.getXIssues();

        int countResults = 0;
        
        for (ScanResults.XIssue issue: issues){
            countResults+=issue.getDetails().size();
        }
        
        assertEquals( countResults,number );
    }
    
    @Given("there is a SAST environment configured and running")
    public void runningSASTEnv(){}

    @When("running a scan for repository {string}")
    public void setGithubRepo(String repoUrl){
        this.repoUrl = repoUrl;
        this.gitHubProperties.setUrl(repoUrl);
        this.branch = "master";
        String [] repoUrlSegments = repoUrl.split("/");
        this.application = repoUrlSegments[repoUrlSegments.length-1].replaceAll(".git","");
        setGithubAuthURL();
    }

    @Then ("SAST output will contain high severity number {int} and medium severity number {int} and low severity number {int}")
    public void verifyOutputSeverityI(Integer high, Integer medium, Integer low){
        ScanDTO scanDTO = callSast();
        clearAfterTest(scanDTO);
        
        assertEquals(results.getScanSummary().getHighSeverity(), high);
        assertEquals(results.getScanSummary().getMediumSeverity(), medium);
        assertEquals(results.getScanSummary().getLowSeverity(), low);
        
        
    }

    @Given("github repository which contains project CodeInjection")
    public void setGitHubRepository(){
        setGithubAuthURL();
    }

    @And("team in application.yml is \\CxServer\\SP")
    public void nothingToDoAnd(){}

    @When("project is: {string} and branch={string}")
    public void setProjectAndBranch(String projectName, String branchName){
        this.projectName  = projectName;
        this.branch = branchName;
    }

    @And ("multi-tenant=true")
    public void setMultiTenantTrue(){
        cxProperties.setMultiTenant(Boolean.TRUE);
    }


    @And ("multi-tenant=false")
    public void setMultiTenantFalse(){
        cxProperties.setMultiTenant(Boolean.FALSE);
    }

    @And("namespace is: {string} and application is {string}")
    public void setNameSpaceApplication(String namespace, String application ){
        this.namespace = namespace;
        this.application = application;
    }


    @And ("repo-name is {string} and --repo-url is supplied and --github flag is supplied")
    public void setRepoName(String repoName){
        this.repoName = repoName;
    }


    @Then ("The request sent to SAST reporitory will contain scan result with project name={string} and team {string}")
    public void runVerifyProjectName(String OutProjectName,String outputTeamName ) throws ExitThrowable {
        ScanDTO scanDTO = callSast();

        assertTrue(results.getProject().equalsIgnoreCase(OutProjectName));

        try {
            assertTrue(scanDTO.getTeamId().equals(cxClient.getTeamId(outputTeamName)));
        } catch (CheckmarxException e) {
            fail(e.getMessage());
        }
        finally {
            clearAfterTest(scanDTO);
        }

    }


   
    public void clearAfterTest(ScanDTO scanDTO) {
        try {
                cxClient.deleteScan(scanDTO.getScanId());
                cxClient.deleteProject(scanDTO.getProjectId());
                if(!results.getTeam().equals("SP") &&  !results.getTeam().equals("\\CxServer") && ! results.getTeam().equals("\\CxServer\\SP")) {
                    cxClient.deleteTeam(scanDTO.getTeamId());
                }
        } catch (CheckmarxException e) {
            fail("Failed to clean after test: " + e.getMessage());
        }
    }

}
