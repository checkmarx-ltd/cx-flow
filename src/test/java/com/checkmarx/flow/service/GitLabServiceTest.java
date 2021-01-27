package com.checkmarx.flow.service;

import com.checkmarx.flow.config.GitLabProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.Sources;
import com.checkmarx.sdk.dto.sast.CxConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.junit4.SpringRunner;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@Import(GitLabProperties.class)
@SpringBootTest
public class GitLabServiceTest {

    @Autowired
    private GitLabService service;

    @Test
    public void getProjectDetails() {
    }

    @Test
    public void getIssues() {
    }

    @Test
    public void processResults() {
    }

    @Test
    public void getProjectDetails1() {
    }

    @Test
    public void getProjectDetails2() {
    }

    @Test
    public void process() {
    }

    @Test
    public void processMerge() {
    }

    @Test
    public void sendMergeComment() {
    }

    @Test
    public void processCommit() {
    }

    @Test
    public void sendCommitComment() {
    }

    @IfProfileValue(name ="testprofile", value ="integration")
    @Test
    public void getSources() {
        ScanRequest request = ScanRequest.builder()
                .namespace("custodela-test")
                .repoName("WebGoat")
                .branch("develop")
                .build();
        request.setRepoProjectId(11842418);
        Sources sources = service.getRepoContent(request);
        assertNotNull(sources);
    }

    @IfProfileValue(name ="testprofile", value ="integration")
    @Test
    public void getCxConfig() {
        ScanRequest request = ScanRequest.builder()
                .namespace("custodela-test")
                .repoName("WebGoat")
                .branch("develop")
                .build();
        request.setRepoProjectId(11842418);
            CxConfig cxConfig = service.getCxConfigOverride(request);
            assertNotNull(cxConfig);
    }
}