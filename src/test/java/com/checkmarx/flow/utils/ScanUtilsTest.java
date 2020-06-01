package com.checkmarx.flow.utils;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.dto.CxConfig;
import com.checkmarx.sdk.utils.ScanUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static org.junit.Assert.*;


public class ScanUtilsTest {

    private FlowProperties flowProperties;
    private JiraProperties jiraProperties;

    @Before
    public void setup(){
        flowProperties = new FlowProperties();
        flowProperties.setBugTrackerImpl(Arrays.asList("JIRA","GitHub","GitLab"));

        jiraProperties = new JiraProperties();

    }
    @Test
    public void testCxConfigOverride(){
        ScanRequest request = ScanRequest.builder()
                .application("abc")
                .product(ScanRequest.Product.CX)
                .project("test")
                .team("\\CxServer\\SP\\Checkmarx")
                .namespace("Custodela")
                .repoName("Riches")
                .repoUrl("https://github.com/Custodela/Riches.git")
                .repoType(ScanRequest.Repository.GITHUB)
                .branch("master")
                .refs(Constants.CX_BRANCH_PREFIX.concat("master"))
                .email(null)
                .incremental(true)
                .scanPreset(Constants.CX_DEFAULT_PRESET)
                //.bugTracker(bt)
                //.filters(filters)
                .build();
        File file = new File(
                getClass().getClassLoader().getResource("CxConfig.json").getFile()
        );
        CxConfig cxConfig = ScanUtils.getConfigAsCode(file);
        assertNotNull(cxConfig);
        com.checkmarx.flow.utils.ScanUtils.overrideCxConfig(request, cxConfig, flowProperties);
        assertEquals("/a/b/c", request.getTeam());
        assertEquals("XYZ-Riches-master", request.getProject());
        assertFalse(request.isIncremental());
        assertEquals("All", request.getScanPreset());
    }

    @Test
    public void testCxConfigFlowOverride(){
        ScanRequest request = ScanRequest.builder()
                .application("abc")
                .product(ScanRequest.Product.CX)
                .project("test")
                .team("\\CxServer\\SP\\Checkmarx")
                .namespace("Custodela")
                .repoName("Riches")
                .repoUrl("https://github.com/Custodela/Riches.git")
                .repoType(ScanRequest.Repository.GITHUB)
                .branch("master")
                .refs(Constants.CX_BRANCH_PREFIX.concat("master"))
                .email(null)
                .incremental(true)
                .scanPreset(Constants.CX_DEFAULT_PRESET)
                //.bugTracker(bt)
                //.filters(filters)
                .build();
        File file = new File(
                getClass().getClassLoader().getResource("CxConfig-flow.json").getFile()
        );
        CxConfig cxConfig = ScanUtils.getConfigAsCode(file);
        assertNotNull(cxConfig);
        com.checkmarx.flow.utils.ScanUtils.overrideCxConfig(request, cxConfig, flowProperties);
        assertEquals("/a/b/c", request.getTeam());
        assertEquals("XYZ-Riches-master", request.getProject());
        assertEquals("test app", request.getApplication());
        assertEquals(2, request.getActiveBranches().size());
        assertNotNull(request.getFilter());
        assertNotNull(request.getFilter().getSimpleFilters());
        assertFalse(request.getFilter().getSimpleFilters().isEmpty());
    }

    @Test
    public void testCxConfigBugOverride(){
        BugTracker bt = BugTracker.builder()
                .type(BugTracker.Type.CUSTOM)
                .customBean("GitHub")
                .build();
        ScanRequest request = ScanRequest.builder()
                .application("abc")
                .product(ScanRequest.Product.CX)
                .project("test")
                .team("\\CxServer\\SP\\Checkmarx")
                .namespace("Custodela")
                .repoName("Riches")
                .repoUrl("https://github.com/Custodela/Riches.git")
                .repoType(ScanRequest.Repository.GITHUB)
                .branch("master")
                .refs(Constants.CX_BRANCH_PREFIX.concat("master"))
                .email(null)
                .incremental(true)
                .scanPreset(Constants.CX_DEFAULT_PRESET)
                .bugTracker(bt)
                .build();
        File file = new File(
                getClass().getClassLoader().getResource("CxConfig-bug.json").getFile()
        );
        CxConfig cxConfig = ScanUtils.getConfigAsCode(file);
        assertNotNull(cxConfig);
        com.checkmarx.flow.utils.ScanUtils.overrideCxConfig(request, cxConfig, flowProperties);
        assertEquals("JIRA", request.getBugTracker().getType().toString());
        assertEquals("APPSEC", request.getBugTracker().getProjectKey());
    }
}