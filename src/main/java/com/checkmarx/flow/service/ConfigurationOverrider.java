package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.FlowOverride;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.CxConfig;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurationOverrider {
    private static final Set<BugTracker.Type> bugTrackersForPullRequest = new HashSet<>(Arrays.asList(
            BugTracker.Type.ADOPULL,
            BugTracker.Type.BITBUCKETPULL,
            BugTracker.Type.BITBUCKETSERVERPULL,
            BugTracker.Type.GITHUBPULL,
            BugTracker.Type.GITLABMERGE));

    private final FlowProperties flowProperties;

    public ScanRequest overrideScanRequestProperties(CxConfig override, ScanRequest request) {
        Map<String, String> overridePropertiesMap = new HashMap<>();

        if (override == null || request == null || Boolean.FALSE.equals(override.getActive())) {
            return request;
        }
        Optional.ofNullable(override.getProject())
                .filter(StringUtils::isNotBlank)
                .ifPresent(p -> {
                    /*Replace ${repo} and ${branch}  with the actual reponame and branch - then strip out non-alphanumeric (-_ are allowed)*/
                    String project = p.replace("${repo}", request.getRepoName())
                            .replace("${branch}", request.getBranch())
                            .replaceAll("[^a-zA-Z0-9-_.]+", "-");
                    request.setProject(project);
                    overridePropertiesMap.put("project", project);
                });
        Optional.ofNullable(override.getTeam())
                .filter(StringUtils::isNotBlank)
                .ifPresent(t -> {
                    request.setTeam(t);
                    overridePropertiesMap.put("team", t);
                });
        Optional.ofNullable(override.getSast()).ifPresent(s -> {
            Optional.ofNullable(s.getIncremental()).ifPresent(si -> {
                request.setIncremental(si);
                overridePropertiesMap.put("incremental", si.toString());
            });

            Optional.ofNullable(s.getForceScan()).ifPresent(sf -> {
                request.setForceScan(sf);
                overridePropertiesMap.put("force scan", sf.toString());
            });

            Optional.ofNullable(s.getPreset()).ifPresent(sp -> {
                request.setScanPreset(sp);
                request.setScanPresetOverride(true);
                overridePropertiesMap.put("scan preset", sp);
            });
            Optional.ofNullable(s.getFolderExcludes()).ifPresent(sfe -> {
                request.setExcludeFolders(Arrays.asList(sfe.split(",")));
                overridePropertiesMap.put("exclude folders", sfe);
            });
            Optional.ofNullable(s.getFileExcludes()).ifPresent(sf -> {
                request.setExcludeFiles(Arrays.asList(sf.split(",")));
                overridePropertiesMap.put("exclude files", sf);
            });
        });

        try {
            Optional.ofNullable(override.getAdditionalProperties()).ifPresent(ap -> {
                Object flow = ap.get("cxFlow");
                ObjectMapper mapper = new ObjectMapper();
                FlowOverride flowOverride = mapper.convertValue(flow, FlowOverride.class);

                Optional.ofNullable(flowOverride).ifPresent(fo -> {
                    BugTracker bt = getBugTracker(fo, request, overridePropertiesMap);
                    /*Override only applicable to Simple JIRA bug*/
                    if (bt.getType().equals(BugTracker.Type.JIRA) && fo.getJira() != null) {
                        overrideJiraBugProperties(fo, bt);
                    }

                    request.setBugTracker(bt);

                    Optional.ofNullable(fo.getApplication())
                            .filter(StringUtils::isNotBlank)
                            .ifPresent(a -> {
                                request.setApplication(a);
                                overridePropertiesMap.put("application", a);
                            });

                    Optional.ofNullable(fo.getBranches())
                            .filter(CollectionUtils::isNotEmpty)
                            .ifPresent(br -> {
                                request.setActiveBranches(br);
                                overridePropertiesMap.put("active branches", Arrays.toString(br.toArray()));
                            });

                    Optional.ofNullable(fo.getEmails())
                            .ifPresent(e -> request.setEmail(e.isEmpty() ? null : e));

                    Optional.ofNullable(fo.getFilters()).ifPresent(f -> {
                        FilterFactory filterFactory = new FilterFactory();
                        ControllerRequest controllerRequest = new ControllerRequest(f.getSeverity(),
                                f.getCwe(),
                                f.getCategory(),
                                f.getStatus());
                        FilterConfiguration filter = filterFactory.getFilter(controllerRequest, null);
                        request.setFilter(filter);

                        String filterDescr;
                        if (CollectionUtils.isNotEmpty(filter.getSimpleFilters())) {
                            filterDescr = filter.getSimpleFilters().stream().map(Object::toString).collect(Collectors.joining(","));
                        } else {
                            filterDescr = "EMPTY";
                        }
                        overridePropertiesMap.put("filters", filterDescr);
                    });

                    Optional.ofNullable(flowOverride.getThresholds()).ifPresent(th -> {
                        if (!(
                                th.getHigh() == null &&
                                        th.getMedium() == null &&
                                        th.getLow() == null &&
                                        th.getInfo() == null
                        )) {
                            Map<FindingSeverity, Integer> thresholdsMap = getThresholdsMap(th);
                            if (!thresholdsMap.isEmpty()) {
                                flowProperties.setThresholds(thresholdsMap);
                            }

                            overridePropertiesMap.put("thresholds", convertMapToString(thresholdsMap));
                        }
                    });
                });
            });

            String overridePropertiesString = convertMapToString(overridePropertiesMap);

            log.info("The following properties were overridden by config-as-code file: {}", overridePropertiesString);

        } catch (IllegalArgumentException e) {
            log.warn("Issue parsing CxConfig cxFlow element", e);
        }
        return request;
    }

    /**
     * Override scan request details as per file/blob (MachinaOverride)
     */
    public ScanRequest overrideScanRequestProperties(FlowOverride override, ScanRequest request) {
        if (override == null) {
            return request;
        }

        BugTracker bt = request.getBugTracker();
        /*Override only applicable to Simple JIRA bug*/
        if (request.getBugTracker().getType().equals(BugTracker.Type.JIRA) && override.getJira() != null) {
            overrideJiraBugProperties(override, bt);
        }
        request.setBugTracker(bt);

        if (!ScanUtils.empty(override.getApplication())) {
            request.setApplication(override.getApplication());
        }

        if (!ScanUtils.empty(override.getBranches())) {
            request.setActiveBranches(override.getBranches());
        }

        List<String> emails = override.getEmails();
        if (emails != null) {
            if (emails.isEmpty()) {
                request.setEmail(null);
            } else {
                request.setEmail(emails);
            }
        }
        FlowOverride.Filters filtersObj = override.getFilters();

        if (filtersObj != null) {
            FilterFactory filterFactory = new FilterFactory();
            ControllerRequest controllerRequest = new ControllerRequest(filtersObj.getSeverity(),
                    filtersObj.getCwe(),
                    filtersObj.getCategory(),
                    filtersObj.getStatus());
            FilterConfiguration filter = filterFactory.getFilter(controllerRequest, null);
            request.setFilter(filter);
        }

        return request;
    }

    private BugTracker getBugTracker(FlowOverride override, ScanRequest request, Map<String, String> overridingReport) {
        BugTracker result;
        if (request.getBugTracker() == null) {
            result = BugTracker.builder()
                    .type(BugTracker.Type.NONE)
                    .build();
            log.debug("Bug tracker is not specified in scan request. Setting bug tracker type to '{}'.", result.getType());
        }
        else {
            result = request.getBugTracker();
        }

        if (canOverrideBugTracker(result, override)) {
            String bugTrackerNameOverride = override.getBugTracker();
            log.debug("Overriding '{}' bug tracker with '{}'.", result.getType(), bugTrackerNameOverride);
            BugTracker.Type bugTrackerTypeOverride = ScanUtils.getBugTypeEnum(bugTrackerNameOverride, flowProperties.getBugTrackerImpl());

            BugTracker.BugTrackerBuilder builder = BugTracker.builder()
                    .type(bugTrackerTypeOverride);

            if (bugTrackerTypeOverride.equals(BugTracker.Type.CUSTOM)) {
                builder.customBean(bugTrackerNameOverride);
            }
            result = builder.build();
            overridingReport.put("bug tracker", bugTrackerNameOverride);
        }
        return result;
    }

    private static boolean canOverrideBugTracker(BugTracker bugTrackerFromScanRequest, FlowOverride override) {
        String bugTrackerNameOverride = override.getBugTracker();
        BugTracker.Type currentBugTrackerType = bugTrackerFromScanRequest.getType();

        boolean comingFromPullRequest = bugTrackersForPullRequest.contains(currentBugTrackerType);
        boolean isOverridePresent = StringUtils.isNotEmpty(bugTrackerNameOverride);
        boolean overrideIsTheSame = bugTrackerNameOverride.equalsIgnoreCase(currentBugTrackerType.toString());

        String cannotOverrideReason = null;
        if (comingFromPullRequest) {
            // Don't override bug tracker type if the scan is initiated by a pull request.
            // Otherwise bug tracker events won't be triggered.
            cannotOverrideReason = "scan was initiated by pull request";
        } else if (!isOverridePresent) {
            cannotOverrideReason = "no bug tracker override is defined";
        } else if (overrideIsTheSame) {
            cannotOverrideReason = "bug tracker type in override is the same as in scan request";
        }

        if (cannotOverrideReason != null) {
            log.debug("Bug tracker override was not applied, because {}.", cannotOverrideReason);
        }

        return cannotOverrideReason == null;
    }

    private static String convertMapToString(Map<?, ?> map) {
        return map.keySet().stream()
                .map(key -> key + "=" + map.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
    }


    private static void overrideJiraBugProperties(FlowOverride override, BugTracker bt) {
        FlowOverride.Jira jira = override.getJira();
        if (!ScanUtils.empty(jira.getAssignee())) {
            bt.setAssignee(jira.getAssignee());
        }//if empty value override with null
        if (jira.getAssignee() != null && jira.getAssignee().isEmpty()) {
            bt.setAssignee(null);
        }
        if (!ScanUtils.empty(jira.getProject())) {
            bt.setProjectKey(jira.getProject());
        }
        if (!ScanUtils.empty(jira.getIssueType())) {
            bt.setIssueType(jira.getIssueType());
        }
        if (!ScanUtils.empty(jira.getOpenedStatus())) {
            bt.setOpenStatus(jira.getOpenedStatus());
        }
        if (!ScanUtils.empty(jira.getClosedStatus())) {
            bt.setClosedStatus(jira.getClosedStatus());
        }
        if (!ScanUtils.empty(jira.getOpenTransition())) {
            bt.setOpenTransition(jira.getOpenTransition());
        }
        if (!ScanUtils.empty(jira.getCloseTransition())) {
            bt.setCloseTransition(jira.getCloseTransition());
        }
        if (!ScanUtils.empty(jira.getCloseTransitionField())) {
            bt.setCloseTransitionField(jira.getCloseTransitionField());
        }
        if (!ScanUtils.empty(jira.getCloseTransitionValue())) {
            bt.setCloseTransitionValue(jira.getCloseTransitionValue());
        }
        if (jira.getFields() != null) { //if empty, assume no fields
            bt.setFields(jira.getFields());
        }
        if (jira.getPriorities() != null && !jira.getPriorities().isEmpty()) {
            bt.setPriorities(jira.getPriorities());
        }
    }

    private static Map<FindingSeverity, Integer> getThresholdsMap(FlowOverride.Thresholds thresholds) {
        Map<FindingSeverity, Integer> map = new EnumMap<>(FindingSeverity.class);
        if (thresholds.getHigh() != null) {
            map.put(FindingSeverity.HIGH, thresholds.getHigh());
        }
        if (thresholds.getMedium() != null) {
            map.put(FindingSeverity.MEDIUM, thresholds.getMedium());
        }
        if (thresholds.getLow() != null) {
            map.put(FindingSeverity.LOW, thresholds.getLow());
        }
        if (thresholds.getInfo() != null) {
            map.put(FindingSeverity.INFO, thresholds.getInfo());
        }

        return map;
    }
}
