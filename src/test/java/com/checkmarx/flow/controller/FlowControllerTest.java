package com.checkmarx.flow.controller;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.cucumber.component.scan.ScanFixture;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.*;
import com.checkmarx.flow.utils.ApiFlowControllerComponentTestProperties;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.CxClient;
import com.checkmarx.utils.TestsParseUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@WebMvcTest({FlowController.class, ApiFlowControllerComponentTestProperties.class, FlowProperties.class})
public class FlowControllerTest {

    @MockBean
    CxClient cxClient;

    @MockBean
    FlowService flowService;

    @Autowired
    FlowProperties flowProperties;

    @MockBean
    CxProperties cxProperties;

    @MockBean
    HelperService helperService;

    @MockBean
    JiraProperties jiraProperties;

    @Autowired
    FlowController flowController;

    @Autowired
    ApiFlowControllerComponentTestProperties testProps;

    @Autowired
    ResultsService resultsService;

    @Autowired
    SastScanner sastScanner;

    @BeforeEach
    public void initMocks() throws CheckmarxException {
        when(cxClient.getTeamId(anyString())).thenReturn(ScanFixture.TEAM_ID);
        when(cxClient.getProjectId(ScanFixture.TEAM_ID, ScanFixture.PROJECT_NAME)).thenReturn(ScanFixture.PROJECT_ID);
        when(cxClient.getProject(ScanFixture.PROJECT_ID)).thenReturn(ScanFixture.getProject());
        when(cxClient.getLastScanId(ScanFixture.PROJECT_ID)).thenReturn(ScanFixture.SCAN_ID);

    }

    @ParameterizedTest
    @MethodSource("generateDataForSuccessfulScanResults")
    public void testSSuccessfulScanResult(String severity, String cwe, String category, String status, String assignee, String override, String bug) {
        ScanResults results = new ScanResults();
        CompletableFuture<ScanResults> cf = CompletableFuture.completedFuture(results);
        when(sastScanner.cxGetResults(any(ScanRequest.class), isNull())).thenReturn(cf);

        ArgumentCaptor<ScanRequest> captor = ArgumentCaptor.forClass(ScanRequest.class);
        List<String> severityFilters = TestsParseUtils.parseCsvToList(severity);
        List<String> cweFilters = TestsParseUtils.parseCsvToList(cwe);
        List<String> categoryFilters = TestsParseUtils.parseCsvToList(category);
        List<String> statusFilters = TestsParseUtils.parseCsvToList(status);
        ScanResults scanResults = flowController.latestScanResults(testProps.getProject(), flowProperties.getToken(), ScanFixture.TEAM_ID, testProps.getApplication(), severityFilters,
                cweFilters, categoryFilters, statusFilters, assignee, override, bug);
        verify(sastScanner, times(1)).cxGetResults(captor.capture(), isNull());
        ScanRequest actual = captor.getValue();
        assertScanResultsRequest(actual, testProps.getApplication(), ScanFixture.TEAM_ID, severityFilters, cweFilters, categoryFilters, statusFilters);
    }

    @Test
    public void testScanApiWithDefaultParamters() {
        FlowController.CxScanRequest request = getScanRequestWithDefaults();
        FlowController.CxScanRequest referenceRequest = getScanRequestWithDefaults();
        referenceRequest.setFilters(getDefaultFilters());
        ArgumentCaptor<ScanRequest> captor = ArgumentCaptor.forClass(ScanRequest.class);
        ResponseEntity<EventResponse>  response = flowController.initiateScan(request, flowProperties.getToken());
        verify(flowService).initiateAutomation(captor.capture());
        ScanRequest actual = captor.getValue();
        assertScanRequest(actual, referenceRequest);
        assertOKResponse(response);
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    public void testScanIncrementalParameter(boolean incremental) {
        FlowController.CxScanRequest request = getScanRequestWithDefaults();
        request.setIncremental(incremental);
        ResponseEntity<EventResponse> response = flowController.initiateScan(request, flowProperties.getToken());
        assertOKResponse(response);
        ArgumentCaptor<ScanRequest> captor = ArgumentCaptor.forClass(ScanRequest.class);
        verify(flowService, times(1)).initiateAutomation(captor.capture());
        ScanRequest actual = captor.getValue();
        assertScanRequest(actual, request);
    }

    @ParameterizedTest
    @MethodSource("getExcludeFiles")
    public void testScanApiWithExcludeFilesAndFolders(List<String> excludeFiles, List<String> excludeFolders) {
        FlowController.CxScanRequest request = getScanRequestWithDefaults();
        request.setExcludeFiles(excludeFiles);
        request.setExcludeFolders(excludeFolders);
        ResponseEntity<EventResponse> response = flowController.initiateScan(request, flowProperties.getToken());
        ArgumentCaptor<ScanRequest> captor = ArgumentCaptor.forClass(ScanRequest.class);
        verify(flowService, times(1)).initiateAutomation(captor.capture());
        assertOKResponse(response);
        assertScanRequest(captor.getValue(), request);
    }


    @ParameterizedTest
    @MethodSource("generateDataForScanApi")
    public void testScanApiWithFilters(String filterSeverity, String filterCwe, String filterOwasp, String filterType, String filterStatus, String id) {
        FlowController.CxScanRequest request = getScanRequestWithDefaults();
        List<Filter> filters = prepareScanApiFilters(filterSeverity, filterCwe, filterOwasp, filterType, filterStatus);
        request.setFilters(filters);
        ResponseEntity<EventResponse> response = flowController.initiateScan(request, flowProperties.getToken());
        ArgumentCaptor<ScanRequest> captor = ArgumentCaptor.forClass(ScanRequest.class);
        verify(flowService).initiateAutomation(captor.capture());
        ScanRequest actual = captor.getValue();
        assertScanApiFilters(actual.getFilter().getSimpleFilters(), filters);
        assertOKResponse(response);
    }

    @ParameterizedTest
    @CsvSource({",PRoject1,Team1","master,,pr1", "master, pr1, "})
        public void testScanApiWithInvalidParams(String branch, String project, String gitUrl){
        FlowController.CxScanRequest request = getScanRequestWithDefaults();
        request.setBranch(branch);
        request.setProject(project);
        request.setGitUrl(gitUrl);
        ResponseEntity<EventResponse> response = flowController.initiateScan(request, flowProperties.getToken());
        verify(flowService, times(0)).initiateAutomation(any(ScanRequest.class));
        assert400Response(response);
    }

    private void assertOKResponse(ResponseEntity response) {
        assertHttpResponse(response, HttpStatus.OK);
    }

    private void assert400Response(ResponseEntity response) {
        assertHttpResponse(response, HttpStatus.BAD_REQUEST);
    }

    private void assertHttpResponse(ResponseEntity response, HttpStatus desiredStatus) {
        Assert.assertEquals("Received wrong HTTP status",desiredStatus, response.getStatusCode());
    }

    private List<Filter> prepareScanApiFilters(String filterSeverity, String filterCwe, String filterOwasp, String filterType, String filterStatus) {
        List<Filter> severityFilters = createFiltersFromString(filterSeverity, Filter.Type.SEVERITY);
        List<Filter> cweFilters = createFiltersFromString(filterCwe, Filter.Type.CWE);
        List<Filter> statusFilters = createFiltersFromString(filterStatus, Filter.Type.STATUS);
        List<Filter> owaspFilters = createFiltersFromString(filterOwasp, Filter.Type.OWASP);
        List<Filter> typeFilters = createFiltersFromString(filterType, Filter.Type.TYPE);
        List<Filter> filters = new ArrayList<>();
        filters.addAll(severityFilters);
        filters.addAll(cweFilters);
        filters.addAll(owaspFilters);
        filters.addAll(statusFilters);
        filters.addAll(typeFilters);
        return filters;
    }
    private void assertScanRequest(ScanRequest actual, FlowController.CxScanRequest wanted) {
        Assert.assertEquals("Branch does not match", actual.getBranch(), wanted.getBranch());
        Assert.assertEquals("Application does not match", actual.getApplication(), wanted.getApplication());
        Assert.assertEquals("Project does not match",actual.getProject(), wanted.getProject());
        Assert.assertEquals("Branch does not match", actual.getProduct().getProduct(), wanted.getProduct());
        Assert.assertEquals("Namespace does not match", actual.getNamespace(), wanted.getNamespace());
        Assert.assertEquals("RepoName does not match", actual.getRepoName(), wanted.getRepoName());
        Assert.assertEquals("Team does not match", actual.getTeam(), wanted.getTeam());
        Assert.assertEquals("Incremental value does not match", actual.isIncremental(), wanted.isIncremental());
        Assert.assertEquals("Scan preset value does not match", actual.getScanPreset(), wanted.getPreset());

    }

    private void assertScanResultsRequest(ScanRequest actual, String application, String teamId, List<String> severity, List<String> cwe, List<String> category, List<String> status) {
        Assert.assertEquals("Application does not match", application, actual.getApplication());
        Assert.assertEquals( "Team does not match", teamId, actual.getTeam());
        assertScanResultsFilters(severity, actual, Filter.Type.SEVERITY);
        assertScanResultsFilters(cwe, actual, Filter.Type.CWE);
        assertScanResultsFilters(category, actual, Filter.Type.TYPE);
        assertScanResultsFilters(status, actual, Filter.Type.STATUS);
    }


    private void assertScanResultsFilters(List<String> wanted, ScanRequest scanRequest, Filter.Type filterType) {
        List<Filter> filtersForType = scanRequest.getFilter()
                .getSimpleFilters()
                .stream()
                .filter(f -> f.getType().equals(filterType))
                .collect(Collectors.toList());

        for (String wantedStr : wanted) {
            boolean found = !(filtersForType.stream().filter(f -> f.getValue().equals(wantedStr)).count() == 0);
            Assert.assertTrue(String.format("Filter from type: %s and value %s was not found in call to FlowService", filterType, wanted), found);
        }
    }

    private void assertScanApiFilters(List<Filter> actual, List<Filter> wanted) {
        if (wanted == null) {
            Assert.assertNull(actual);
        } else {
            Assert.assertNotNull("Filters does not match", actual);
        }
        for(Filter wantedFilter: wanted) {
            Assert.assertNotEquals(String.format("Filter Type %s with value %s not found in actual call", wantedFilter.getType(), wantedFilter.getValue()), 0, actual.stream().filter(f ->
                    f.getType().equals(wantedFilter.getType()) &&
                            f.getValue().equals(wantedFilter.getValue())).count());
        }
    }

    private List<Filter> createFiltersFromString(String filterValue, Filter.Type type) {
        if (StringUtils.isEmpty(filterValue)) {
            return Collections.emptyList();
        }
        String[] filterValArr = filterValue.split(",");
        return Arrays.stream(filterValArr).map(filterVal -> new Filter(type, filterVal)).collect(Collectors.toList());
    }

    private List<Filter> getDefaultFilters() {
        List<Filter> result = new ArrayList<>();
        if (flowProperties.getFilterCategory() != null) {
            result.addAll(createFiltersFromString(String.join(",", flowProperties.getFilterCategory()), Filter.Type.TYPE));
        }
        if (flowProperties.getFilterCwe() != null) {
            result.addAll(createFiltersFromString(String.join(",", flowProperties.getFilterCwe()), Filter.Type.CWE));
        }
        if (flowProperties.getFilterSeverity() != null ) {
            result.addAll(createFiltersFromString(String.join(",", flowProperties.getFilterSeverity()), Filter.Type.SEVERITY));
        }
        if (flowProperties.getFilterStatus() != null) {
            result.addAll(createFiltersFromString(String.join(",", flowProperties.getFilterStatus()), Filter.Type.STATUS));
        }
        return result;
    }

    private FlowController.CxScanRequest getScanRequestWithDefaults() {
        FlowController.CxScanRequest request = new FlowController.CxScanRequest();
        request.setGitUrl(testProps.getGitUrl());
        request.setProject(testProps.getProject());
        request.setPreset(testProps.getPreset());
        request.setProduct(testProps.getProduct());
        request.setRepoName(testProps.getRepoName());
        request.setNamespace(testProps.getNamespace());
        request.setResultUrl(testProps.getResultUrl());
        request.setApplication(testProps.getApplication());
        request.setBranch(testProps.getBranch());
        request.setTeam(testProps.getTeam());
        return request;
    }

    static Stream<Arguments> generateDataForSuccessfulScanResults() {
        return Stream.of(
                Arguments.of("High,Medium", "89,79", "Stored_XSS,SQL_Injection", "Confirmed,Urgent", "user1", "", ""),
                Arguments.of("Medium","89", "Stored_XSS", "Urgent", "user2", "", "")
        );
    }

    static Stream<Arguments> generateDataForScanApi() {
        return Stream.of(
                Arguments.of("High", "89", "10",  "Stored_XSS", "Confirmed", "1"),
                Arguments.of("Medium", "90", "10",  "Stored_XSS", "Confirmed", "1"),
                Arguments.of("Low", "89", "10",  "Stored_XSS", "", "1")
        );
    }

    static Stream<Arguments> getExcludeFiles() {
        return Stream.of(
                Arguments.of(Arrays.asList("C:/asdfasd.java" , "C:/blabla.xml"), Arrays.asList("C:/asdfasd" , "C:/blabla"))
        );
    }






}