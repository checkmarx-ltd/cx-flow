package com.checkmarx.flow.controller;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.utils.ScanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.*;

import java.beans.ConstructorProperties;
import java.util.List;


/**
 * REST endpoint for Cx-Flow. Currently supports fetching the latest scan results given a Checkmarx Project Name
 * and a fully qualified Checkmarx Team Path (ex. CxServer\EMEA\Marketing)
 */
@RestController
@RequestMapping(value = "/")
public class FlowController {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(FlowController.class);

    // Simple (shared) API token
    private static final String TOKEN_HEADER = "x-cx-token";

    private final FlowProperties properties;
    private final FlowService scanService;
    private final JiraProperties jiraProperties;

    @ConstructorProperties({"properties", "scanService", "jiraProperties"})
    public FlowController(FlowProperties properties, FlowService scanService, JiraProperties jiraProperties) {
        this.properties = properties;
        this.scanService = scanService;
        this.jiraProperties = jiraProperties;
    }

    @RequestMapping(value = "/scanresults", method = RequestMethod.GET, produces = "application/json")
    public ScanResults latestScanResults(
            // Mandatory parameters
            @RequestParam(value = "team", required = true) String team,
            @RequestParam(value = "project", required = true) String project,
            @RequestHeader(value = TOKEN_HEADER) String token,
            // Optional parameters
            @RequestParam(value = "application", required = false) String application,
            @RequestParam(value = "severity", required = false) List<String> severity,
            @RequestParam(value = "cwe", required = false) List<String> cwe,
            @RequestParam(value = "category", required = false) List<String> category,
            @RequestParam(value = "status", required = false) List<String> status,
            @RequestParam(value = "assignee", required = false) String assignee,
            @RequestParam(value = "override", required = false) String override,
            @RequestParam(value = "tracker", required = false) String tracker) {

        // Validate shared API token from header
        validateToken(token);

        // Create bug tracker
        BugTracker bugTracker = getBugTracker(assignee, tracker);

        // Create filters if available
        List<Filter> filters = getFilters(severity, cwe, category, status);

        // Create the scan request
        ScanRequest scanRequest = ScanRequest.builder()
                // By default, use project as application, unless overridden
                .application(StringUtils.isBlank(application) ? project : application)
                .project(project)
                .team(team)
                .bugTracker(bugTracker)
                .filters(filters)
                .build();

        // If an override blob/file is provided, substitute these values
        if (StringUtils.isNotBlank(override)) {
            MachinaOverride ovr = ScanUtils.getMachinaOverride(override);
            scanRequest = ScanUtils.overrideMap(scanRequest, ovr);
        }

        // Fetch the Checkmarx Scan Results based on given ScanRequest.
        // The cxProject parameter is null because the required project metadata
        // is already contained in the scanRequest parameter.
        ScanResults scanResults = scanService.cxGetResults(scanRequest, null).join();

        // Debug log
        if (log.isDebugEnabled()) {
            log.debug("ScanResults " + scanResults.toString());
        }

        return scanResults;
    }

    /**
     * Validates given token against the token value defined in the cx-flow section of the application yml.
     *
     * @param token token to validate
     */
    private void validateToken(String token) {
        log.info("Validating REST API token");
        if (!properties.getToken().equals(token)) {
            log.error("REST API token validation failed");
            throw new InvalidTokenException();
        }
        log.info("Validation successful");
    }

    /**
     * Creates a list of {@link Filter}s based on any given values. If no values are provided,
     * the filter values specified in the cx-flow section of the yml file will be used.
     *
     * @param severity list of severity values to use
     * @param cwe      list of CWE values to use
     * @param category list of vulnerability categories to use
     * @param status   list of status values to use
     * @return list of {@link Filter} objects
     */
    protected List<Filter> getFilters(List<String> severity, List<String> cwe, List<String> category, List<String> status) {
        List<Filter> filters;
        // If values are provided, use them
        if (!ScanUtils.empty(severity) || !ScanUtils.empty(cwe) || !ScanUtils.empty(category) || !ScanUtils.empty(status)) {
            filters = ScanUtils.getFilters(severity, cwe, category, status);
        }
        // otherwise, default to filters specified in the cx-flow section of the yml
        else {
            filters = ScanUtils.getFilters(properties.getFilterSeverity(), properties.getFilterCwe(),
                    properties.getFilterCategory(), properties.getFilterStatus());
        }
        return filters;
    }

    /**
     * Creates a {@link BugTracker} from given values. If values are not provided,
     * a default tracker of type {@link BugTracker.Type#NONE} will be returned.
     *
     * @param assignee assignee for bug tracking
     * @param tracker  bug tracker type to use
     * @return a {@link BugTracker}
     */
    protected BugTracker getBugTracker(String assignee, String tracker) {
        // Default bug tracker type : NONE
        BugTracker bugTracker = BugTracker.builder().type(BugTracker.Type.NONE).build();

        // If a bug tracker is explicitly provided, override the default
        // TODO: Ask Ken if assignee is mandatory for a tracker
        if (StringUtils.isNotBlank(tracker) && StringUtils.isNotBlank(assignee)) {
            BugTracker.Type bugTypeEnum = ScanUtils.getBugTypeEnum(tracker, properties.getBugTrackerImpl());
            bugTracker = ScanUtils.getBugTracker(assignee, bugTypeEnum, jiraProperties, tracker);
        }
        return bugTracker;
    }

}
