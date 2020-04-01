package com.checkmarx.flow.dto.report;

import com.checkmarx.flow.dto.ScanRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;

/**
 * Logged after CxFlow has modified (created/updated/closed) Jira tickets.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JiraTicketsReport extends AnalyticsReport{

    public static final String OPERATION = "Jira Tickets Creation";
    private HashMap<String, List<String>> jiraTickets = new HashMap<>();

    public JiraTicketsReport(ScanRequest request) {
        this.scanId = NOT_APPLICABLE;
        scanInitiator = NOT_APPLICABLE;
        projectName = request.getProject();
        setEncryptedRepoUrl(request.getRepoUrl());
    }
    
    public JiraTicketsReport(Integer sastScanId, ScanRequest request) {
        super(sastScanId,request);
        setEncryptedRepoUrl(request.getRepoUrl());
    }

    public JiraTicketsReport(String osaScanId, ScanRequest request) {
        super(osaScanId,request);
        setEncryptedRepoUrl(request.getRepoUrl());
    }
    

    @Override
    protected String _getOperation() {
        return OPERATION;
    }

    public JiraTicketsReport build(ImmutableMap<String, List<String>> ticketsMap) {
        this.jiraTickets.putAll(ticketsMap);
        return this;
    }

}
