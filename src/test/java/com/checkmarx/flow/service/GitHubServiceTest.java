package com.checkmarx.flow.service;

import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.Sources;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@Import(GitHubProperties.class)
@SpringBootTest
public class GitHubServiceTest {

    @Autowired
    private GitHubService service;

    @Test
    public void getIssues() {
    }

    @Test
    public void processResults() {
    }

    @Test
    public void process() {
    }

    @Test
    public void processPull() {
    }

    @Test
    public void sendMergeComment() {
    }

    @Test
    public void getRepoContent() {
        ScanRequest request = ScanRequest.builder()
                .namespace("Custodela")
                .repoName("Riches")
                .branch("master")
                .build();
        Sources sources = service.getRepoContent(request);
        assertNotNull(sources);
    }
}