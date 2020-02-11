package com.checkmarx.flow.cucumber.component.scan;

import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxProject;
import com.checkmarx.sdk.dto.cx.CxScanSummary;
import org.assertj.core.util.Lists;

import java.util.ArrayList;
import java.util.List;

public class ScanFixture {

    public static final int PROJECT_ID = 1;
    public static final String PROJECT_NAME = "CodeInjection1";
    public static final String TEAM_PATH = "\\CxServer";
    public static final String TEAM_ID = "1";
    public static final int SCAN_ID = 1;
    public static final String SCAN_LINK = "http://localhost/CxWebClient/ViewerMain.aspx?scanid=" + SCAN_ID + "&projectid=" + PROJECT_ID;
    public static final String FILES_SCANNED = "1";
    public static final String LOC_SCANNED = "8";
    public static final String SCAN_TYPE = "Full";
    public static final String SEVERITY_HIGH = "High";

    public static List<CxProject> getProjects() {
        List<CxProject> projects = new ArrayList<>(1);
        projects.add(getProject());
        return projects;
    }

    public static CxProject getProject() {
        return CxProject.builder().id(PROJECT_ID).name(PROJECT_NAME).isPublic(true).customFields(Lists.emptyList()).links(Lists.emptyList()).build();
    }

    public static List<Filter> getScanFilters() {
        List<Filter> filters = new ArrayList<>(1);
        filters.add(Filter.builder().type(Filter.Type.SEVERITY).value(SEVERITY_HIGH).build());
        return filters;
    }

    public static ScanResults getScanResults() {
        CxScanSummary scanSummary = new CxScanSummary();
        scanSummary.setHighSeverity(1);
        scanSummary.setMediumSeverity(1);
        scanSummary.setLowSeverity(1);
        scanSummary.setInfoSeverity(1);
        scanSummary.setStatisticsCalculationDate("");
        return ScanResults.builder().projectId(String.valueOf(PROJECT_ID)).team(TEAM_PATH).project(PROJECT_NAME).link(SCAN_LINK).files(FILES_SCANNED)
                .loc(LOC_SCANNED).scanType(SCAN_TYPE).additionalDetails(null).scanSummary(scanSummary).xIssues(Lists.emptyList()).build();
    }
}
