package com.checkmarx.flow.service;

import com.checkmarx.flow.config.GitLabProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@Import(GitLabProperties.class)
@SpringBootTest
public class GitLabServiceTest {

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
}