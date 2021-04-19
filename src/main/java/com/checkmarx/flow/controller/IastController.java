package com.checkmarx.flow.controller;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.service.IastService;
import com.checkmarx.flow.service.JiraService;
import com.checkmarx.flow.utils.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

import static com.atlassian.sal.api.xsrf.XsrfHeaderValidator.TOKEN_HEADER;

@Slf4j
@RestController
@RequestMapping(value = "/iast")
@RequiredArgsConstructor
public class IastController {


    @Autowired
    private IastService iastService;
    @Autowired
    private TokenUtils tokenUtils;
    @Autowired
    private JiraService jiraService;

    @PostMapping(value = { "/generate-tag"})
    public ResponseEntity<EventResponse> generateTag(){
        return ResponseEntity.accepted().body(EventResponse.builder()
                .message(iastService.generateUniqTag())
                .success(true)
                .build());
    }

    @PostMapping(value = { "/stop-scan-and-create-jira-issue/{scanId}"})
    public ResponseEntity<EventResponse> stopScanAndCreateJiraIssue(
            @PathVariable(value = "scanId", required = false) String scanId,
            @RequestHeader(value = TOKEN_HEADER) String token){
//         Validate shared API token from header
        tokenUtils.validateToken(token);
        try {
            iastService.stopScanAndCreateJiraIssueFromIastSummary(scanId);
            return ResponseEntity.accepted().body(EventResponse.builder()
                    .message("OK")
                    .success(true)
                    .build());

        } catch (IOException e) {
            log.warn("Can't stop scan or create jira task", e);
        }

        return ResponseEntity.status(500).body(EventResponse.builder()
                .message("Internal Server Error")
                .success(false)
                .build());
    }

    @GetMapping(value = { "/jira/{label}"})
    public List<Issue> jiraIssue(@PathVariable(value = "label", required = false) String label){
        return jiraService.searchIssueByLabel(label);

    }
}
