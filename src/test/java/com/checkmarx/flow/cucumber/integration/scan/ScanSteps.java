package com.checkmarx.flow.cucumber.integration.scan;

import com.checkmarx.flow.CxFlowApplication;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.Status;
import com.checkmarx.flow.exception.ExitThrowable;

import com.checkmarx.flow.utils.AesEncodingUtils;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.ResourceUtils;

import java.io.*;
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
        this.gitHubProperties.setApiUrl(repoUrl + "repos/");
        this.branch = "master";
        String [] repoUrlSegments = repoUrl.split("/");
        this.application = repoUrlSegments[repoUrlSegments.length-1].replaceAll(".git","");
        setGithubAuthURL();
    }

    
    @Then ("SAST output will contain high severity number {int} and medium severity number {int} and low severity number {int} and  SAST team name will be {string}")
    public void verifyOutputSeverityAndTeam(Integer high, Integer medium, Integer low, String outputTeamName){
        ScanDTO scanDTO = callSast();
        if(errorExpected){
           return;
        }
        try {
            assertTrue(scanDTO.getTeamId().equals(cxClient.getTeamId(outputTeamName)));
            assertEquals(high, results.getScanSummary().getHighSeverity());
            assertEquals(medium, results.getScanSummary().getMediumSeverity());
            assertEquals(low, results.getScanSummary().getLowSeverity());
            
        } catch (CheckmarxException e) {
            fail(e.getMessage());
        }
        finally {
            clearAfterTest(scanDTO);
        }
    }
            
    @Then ("SAST output will contain high severity number {int} and medium severity number {int} and low severity number {int}")
    public void verifyOutputSeverityI(Integer high, Integer medium, Integer low){
        ScanDTO scanDTO = callSast();
        clearAfterTest(scanDTO);

        assertEquals(high, results.getScanSummary().getHighSeverity());
        assertEquals(medium, results.getScanSummary().getMediumSeverity());
        assertEquals(low, results.getScanSummary().getLowSeverity());
    }
    
    @And ("parameter path is populated in application.xml and scanType is {string} and branch {string}")
    public void setScanTypeAndBranch(String scanType, String branch){
        
        if(scanType.equals("Inc")){
            cxProperties.setIncremental(true);
        }

        this.branch = branch;
        
        setDeafultTeam();
        
    }


    @And ("parameter path is populated in application.xml and scanType is {string} and team is {string}")
    public void setScanTypeAndTeam(String scanType, String team){

        if(scanType.equals("Inc")){
            cxProperties.setIncremental(true);
        }

        if(team.equals("invalidTeam")){
            super.errorExpected = true;
        }
        cxProperties.setTeam(team);
        this.teamName = team;

    }

    
    @And ("output json logger will have Scan request {string}")
    public void verifyJsonLogger(String repoUrl) {
        verifyJsonLoggerAndScanStatus(repoUrl, Status.SUCCESS.getMessage());
    }
    
    @And ("output json logger will have Scan request {string} and scan status will be {string}")
    public void verifyJsonLoggerAndScanStatus(String repoUrl, String scanStatus){
        JsonNode node = null;
        ObjectMapper objectMapper = new ObjectMapper();
        String logAbsolutePath = System.getProperty("LOG_PATH") + File.separator +"CxFlowReport.json";
        try (
                FileInputStream inputStream = new FileInputStream(logAbsolutePath);
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))
            ) {
            boolean moreLines = true;
            String scanRequest = streamReader.readLine();
            String nextScanRequest = null;
            while(moreLines) {
                nextScanRequest = streamReader.readLine();
                if(nextScanRequest!=null){
                    scanRequest = nextScanRequest;
                }else{
                    moreLines = false;
                }
            }
            node = objectMapper.readTree(scanRequest).get("Scan Request");

            if(this.repoType.equals(ScanRequest.Repository.GITHUB)) {
                assertEquals( (ScanRequest.Repository.GITHUB.toString()),node.get("repoType").textValue());
                assertEquals(this.branch, node.get("branch").textValue());
                assertEquals(repoUrl, AesEncodingUtils.decode(node.get("repoUrl").textValue().trim()));
            }else{
                assertEquals("NA",node.get("repoType").textValue());
                if(!errorExpected){
                    assertEquals(fileRepo.getPath(), AesEncodingUtils.decode(node.get("repoUrl").textValue().trim()));
                }
            }
            
            assertTrue(node.get("scanStatus").textValue().startsWith(scanStatus));
            assertEquals(cxProperties.getIncremental() ? "Inc" : "Full", node.get("scanType").textValue());
            assertNotEquals("null", node.get("scanId").textValue() );

            
        }catch (IOException | CheckmarxException e) {
                fail(e.getMessage());
                
        }
        finally{
            objectMapper = null;
            node=null;
            errorExpected = false;
        }
        
    }

    @And ("running a scan for a specified folder") 
    public void setFileReporitory() {
        try {

            super.fileRepo = ResourceUtils.getFile("classpath:\\cucumber\\data\\input-files-toscan\\VB_3845\\encode.zip");
            this.application = "VB_3845";
            repoType = ScanRequest.Repository.NA;
            
        } catch (FileNotFoundException e) {
           fail(e.getMessage());
        }
    }
    
    @Given("github repository which contains project CodeInjection")
    public void setGitHubRepository(){
        setGithubAuthURL();
    }

    @And("team in application.yml is \\CxServer\\SP")
    public void setTeam()
    {
        setDeafultTeam();
    }

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
        setGithubRepo("https://github.com/cxflowtestuser/Code_Injection");
    }


    @Then ("The request sent to SAST reporitory will contain scan result with project name={string} and team {string}")
    public void runVerifyProjectName(String OutProjectName,String outputTeamName ) throws ExitThrowable {
        ScanDTO scanDTO = callSast();

        assertTrue(results.getProject().equalsIgnoreCase(OutProjectName));

        try {
            assertEquals(cxClient.getTeamId(outputTeamName), scanDTO.getTeamId());
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
