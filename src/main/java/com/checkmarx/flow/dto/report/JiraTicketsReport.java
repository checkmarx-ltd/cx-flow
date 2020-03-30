package com.checkmarx.flow.dto.report;

import com.checkmarx.flow.dto.ScanRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JiraTicketsReport extends AnalyticsReport{

    public static final String OPERATION = "Jira Tickets Creation";
    private ImmutableMap<String, List<String>> jiraTickets;

    public JiraTicketsReport(ScanRequest request) {
        this.scanId = NOT_APPLICABLE;
        scanInitiator = NOT_APPLICABLE;
        projectName = request.getProject();
        setEncodedRepoUrl(request.getRepoUrl());
    }
    
    public JiraTicketsReport(Integer sastScanId, ScanRequest request) {
        super(sastScanId,request);
        setEncodedRepoUrl(request.getRepoUrl());
    }

    public JiraTicketsReport(String osaScanId, ScanRequest request) {
        super(osaScanId,request);
        setEncodedRepoUrl(request.getRepoUrl());
    }
    

    //adding underscore to prevent getOperation() to be called during logging of this object in log()
    //since we don't want the OPERATION to be a part of the logged object
    @Override
    public String _getOperation() {
        return OPERATION;
    }

    public JiraTicketsReport build(ImmutableMap<String, List<String>> ticketsMap) {
        this.jiraTickets = ticketsMap;
        return this;
    }

}
