package com.checkmarx.flow.dto.report;

import com.checkmarx.flow.constants.JiraConstants;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.sdk.dto.ScanResults;
import com.google.common.collect.ImmutableMap;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class JiraTicketsReport extends AnalyticsReport{

    public static final String OPERATION = "Jira Tickets Creation";
    private ImmutableMap<String, List<String>> jiraTickets;

    public JiraTicketsReport(Integer sastScanId, ScanRequest request, ScanResults results) {
        super(sastScanId,request);
        setEncodedRepoUrl(request.getRepoUrl());
    }

    public JiraTicketsReport(String osaScanId, ScanRequest request, ScanResults results) {
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
