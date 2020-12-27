package com.checkmarx.flow.service;

import com.checkmarx.configprovider.ConfigProvider;
import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.CxIntegrationsProperties;
import com.checkmarx.flow.config.external.ASTConfig;
import com.checkmarx.flow.config.external.CxGoConfigFromWebService;
import com.checkmarx.flow.constants.FlowConstants;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.FlowOverride;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.*;
import com.checkmarx.sdk.dto.CxConfig;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.dto.Sca;
import com.checkmarx.sdk.dto.filtering.EngineFilterConfiguration;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
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

    private static final String TEAM_REPORT_KEY = "team";

    private final FlowProperties flowProperties;
    private final CxIntegrationsProperties cxIntegrationsProperties;
    private final SCAScanner scaScanner;
    private final SastScanner sastScanner;
    private final CxGoScanner cxgoScanner;
    private final ScaConfigurationOverrider scaConfigOverrider;
    private final ReposManagerService reposManagerService;
    private final GitAuthUrlGenerator gitAuthUrlGenerator;
    
    public ScanRequest overrideScanRequestProperties(CxConfig override, ScanRequest request) {
        if (request == null) {
            log.warn("Unable to override scan request properties. Scan request is null.");
            return null;
        }

        Map<String, String> overrideReport = new HashMap<>();
        applyCxGoDynamicConfig(overrideReport, request);
        scaConfigOverrider.initScaConfig(request);

        ConfigProvider configProvider = ConfigProvider.getInstance();
        boolean noOverridesArePresent = !configProviderResultsAreAvailable(configProvider) && !configAsCodeIsAvailable(override);
        if (noOverridesArePresent) {
            log.debug("No scan request property overrides were detected.");
            return request;
        }

        overrideMainProperties(Optional.ofNullable(override), request, overrideReport);

        try {
            overrideAdditionalProperties(override, request, overrideReport);

            String overriddenProperties = ScanUtils.convertMapToString(overrideReport);
            log.info("The following properties were overridden by config-as-code file: {}", overriddenProperties);

        } catch (IllegalArgumentException e) {
            log.warn("Error parsing cxFlow object from CxConfig.", e);
        }
        return request;
    }

    private void overrideAdditionalProperties(CxConfig override, ScanRequest request, Map<String, String> overrideReport) {
        Optional.ofNullable(override)
            .map(CxConfig::getAdditionalProperties).ifPresent(ap -> {
            Object flow = ap.get("cxFlow");
            ObjectMapper mapper = new ObjectMapper();
            Optional.ofNullable(mapper.convertValue(flow, FlowOverride.class)).ifPresent(flowOverride ->
                applyFlowOverride(flowOverride, request, overrideReport)
            );
        });
    }

    private boolean configProviderResultsAreAvailable(ConfigProvider configProvider) {
        return configProvider.hasAnyConfiguration(MDC.get(FlowConstants.MAIN_MDC_ENTRY));
    }

    private boolean configAsCodeIsAvailable(CxConfig override) {
        // Config-as-code should be enabled if the 'active' property is either
        // true or null => checking against Boolean.FALSE.
        return override != null && !Boolean.FALSE.equals(override.getActive());
    }

    private void applyFlowOverride(FlowOverride override, ScanRequest request, Map<String, String> overrideReport) {
        BugTracker bt = getBugTracker(override, request, overrideReport);
        /*Override only applicable to Simple JIRA bug*/
        if (BugTracker.Type.JIRA.equals(bt.getType()) && override.getJira() != null) {
            overrideJiraBugProperties(override, bt);
        }

        request.setBugTracker(bt);

        Optional.ofNullable(override.getApplication())
                .filter(StringUtils::isNotBlank)
                .ifPresent(a -> {
                    request.setApplication(a);
                    overrideReport.put("application", a);
                });

        Optional.ofNullable(override.getBranches())
                .filter(CollectionUtils::isNotEmpty)
                .ifPresent(br -> {
                    request.setActiveBranches(br);
                    overrideReport.put("active branches", Arrays.toString(br.toArray()));
                });

        Optional.ofNullable(override.getEmails())
                .ifPresent(e -> request.setEmail(e.isEmpty() ? null : e));

        overrideFilters(override, request, overrideReport);

        overrideThresholds(override, overrideReport, request);

        overrideEnabledVulnerabilityScanners(override, request, overrideReport);
    }

    private void overrideEnabledVulnerabilityScanners(FlowOverride override,
                                                      ScanRequest request,
                                                      Map<String, String> overrideReport) {
        Optional.ofNullable(override.getVulnerabilityScanners()).ifPresent(vulnerabilityScanners -> {
            List<VulnerabilityScanner> scannersForRequest = new ArrayList<>();

            vulnerabilityScanners.forEach(scannerName -> {
                if (scannerName.equalsIgnoreCase(ScaProperties.CONFIG_PREFIX)) {
                    scannersForRequest.add(scaScanner);
                }
                if (scannerName.equalsIgnoreCase(CxProperties.CONFIG_PREFIX)) {
                    scannersForRequest.add(sastScanner);
                }
                if (scannerName.equalsIgnoreCase(CxGoProperties.CONFIG_PREFIX)) {
                    scannersForRequest.add(cxgoScanner);
                }
            });
            request.setVulnerabilityScanners(scannersForRequest);
            overrideReport.put("vulnerabilityScanners", vulnerabilityScanners.toString());
        });
    }

    private void overrideThresholds(FlowOverride flowOverride, Map<String, String> overrideReport, ScanRequest request) {
        Optional.ofNullable(flowOverride.getThresholds()).ifPresent(thresholds -> {
            Map<FindingSeverity, Integer> thresholdsMap = getThresholdsMap(thresholds);
            if (!thresholdsMap.isEmpty()) {
                request.setThresholds(thresholdsMap);
                overrideReport.put("thresholds", ScanUtils.convertMapToString(thresholdsMap));
            }
        });
    }

    private void overrideFilters(FlowOverride flowOverride, ScanRequest request, Map<String, String> overrideReport) {
        Optional.ofNullable(flowOverride.getFilters()).ifPresent(override -> {
            FilterFactory filterFactory = new FilterFactory();
            ControllerRequest controllerRequest = new ControllerRequest(override.getSeverity(),
                    override.getCwe(),
                    override.getCategory(),
                    override.getStatus());
            FilterConfiguration filterConfig = filterFactory.getFilter(controllerRequest, null);
            request.setFilter(filterConfig);

            String filterDescr;
            List<Filter> simpleFilters = Optional.ofNullable(filterConfig)
                    .map(FilterConfiguration::getSastFilters)
                    .map(EngineFilterConfiguration::getSimpleFilters)
                    .orElse(null);

            if (CollectionUtils.isNotEmpty(simpleFilters)) {
                filterDescr = simpleFilters.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(","));
            } else {
                filterDescr = "EMPTY";
            }
            overrideReport.put("filters", filterDescr);
        });
    }

    private void overrideMainProperties(Optional<CxConfig> override, ScanRequest request, Map<String, String> overrideReport) {
        override.map(CxConfig::getProject)
                .filter(StringUtils::isNotBlank)
                .ifPresent(p -> {
                    /*Replace ${repo} and ${branch}  with the actual reponame and branch - then strip out non-alphanumeric (-_ are allowed)*/
                    String project = p.replace("${repo}", request.getRepoName())
                            .replace("${branch}", request.getBranch())
                            .replaceAll("[^a-zA-Z0-9-_.]+", "-");
                    request.setProject(project);
                    overrideReport.put("project", project);
                });
        override.map(CxConfig::getTeam)
                .filter(StringUtils::isNotBlank)
                .ifPresent(t -> {
                    request.setTeam(t);
                    overrideReport.put(TEAM_REPORT_KEY, t);
                });
        override.map(CxConfig::getSast).ifPresent(s -> {
            Optional.ofNullable(s.getIncremental()).ifPresent(si -> {
                request.setIncremental(si);
                overrideReport.put("incremental", si.toString());
            });

            Optional.ofNullable(s.getForceScan()).ifPresent(sf -> {
                request.setForceScan(sf);
                overrideReport.put("force scan", sf.toString());
            });

            Optional.ofNullable(s.getPreset()).ifPresent(sp -> {
                request.setScanPreset(sp);
                request.setScanPresetOverride(true);
                overrideReport.put("scan preset", sp);
            });
            Optional.ofNullable(s.getFolderExcludes()).ifPresent(sfe -> {
                request.setExcludeFolders(Arrays.asList(sfe.split(",")));
                overrideReport.put("exclude folders", sfe);
            });
            Optional.ofNullable(s.getFileExcludes()).ifPresent(sf -> {
                request.setExcludeFiles(Arrays.asList(sf.split(",")));
                overrideReport.put("exclude files", sf);
            });
            Optional.ofNullable(s.getEngineConfiguration()).ifPresent(scanConfiguration -> {
                request.setScanConfiguration(scanConfiguration);
                overrideReport.put("scan configuration", scanConfiguration);
            });
        });
        overrideUsingConfigProvider(override, overrideReport, request);
    }

    private void overrideUsingConfigProvider(Optional<CxConfig> fallback, Map<String, String> overrideReport, ScanRequest request) {
        ConfigProvider configProvider = ConfigProvider.getInstance();
        String uid = MDC.get(FlowConstants.MAIN_MDC_ENTRY);

        ScaConfig scaConfiguration = configProvider.getConfiguration(uid, ScaProperties.CONFIG_PREFIX, ScaConfig.class);
        if (scaConfiguration != null) {
            log.info("Overriding SCA properties from config provider configuration");
            scaConfigOverrider.overrideScanRequestProperties(scaConfiguration, request, overrideReport);
        } else {
            Sca scaPropertiesFromConfigAsCode = fallback.map(CxConfig::getSca).orElse(null);
            scaConfigOverrider.overrideScanRequestProperties(scaPropertiesFromConfigAsCode, request, overrideReport);
        }

        ASTConfig astConfiguration = configProvider.getConfiguration(uid, AstProperties.CONFIG_PREFIX, ASTConfig.class);
        if (astConfiguration != null) {
            log.info("Overriding AST properties from config provider configuration");
            overridePropertiesAst(astConfiguration, overrideReport, request);
        }
    }

    private void applyCxGoDynamicConfig(Map<String, String> overrideReport, ScanRequest request) {
        if (cxIntegrationsProperties.isReadMultiTenantConfiguration()) {
            String scmType = request.getRepoType().getRepository().toLowerCase();

            /*
                When ADO is the SCM event trigger, the Repos-Manager expects to get 'azure' in the URL path
                rather than 'ado'. Was decided to make the change here rather than the other services
             */
            if (scmType.equalsIgnoreCase(ScanRequest.Repository.ADO.getRepository())) {
                scmType = "azure";
            }

            CxGoConfigFromWebService cxgoConfig = reposManagerService.getCxGoDynamicConfig(
                    scmType,
                    request.getOrganizationId());

            if (cxgoConfig == null) {
                log.error("Multi Tenant mode: missing CxGo configuration in Repos Manager Service. Working with Multi Tenant = false ");
                return;
            }

            String className = CxGoConfigFromWebService.class.getSimpleName();
            log.info("Applying {} configuration.", className);
            Optional.ofNullable(cxgoConfig.getTeam())
                    .filter(StringUtils::isNotEmpty)
                    .ifPresent(team -> {
                        request.setTeam(team);
                        log.info("Using team from {}", className);
                        overrideReport.put(TEAM_REPORT_KEY, team);
                    });
            Optional.ofNullable(cxgoConfig.getCxgoSecret())
                    .filter(StringUtils::isNotEmpty)
                    .ifPresent(secret -> {
                        request.setScannerApiSecret(secret);
                        log.info("Using scanner API secret from {}", className);
                        overrideReport.put("scannerApiSecret", "<actually it's a secret>");
                    });
            Optional.ofNullable(cxgoConfig.getScmAccessToken())
                    .filter(StringUtils::isNotEmpty)
                    .ifPresent(token -> {
                        String authUrl = gitAuthUrlGenerator.addCredToUrl
                                (request.getRepoType(), request.getGitUrl(), cxgoConfig.getScmAccessToken());
                        request.setRepoUrlWithAuth(authUrl);
                        log.info("Using SCM token from {}", className);
                        overrideReport.put("SCM token", "********");
                    });
        }
    }

    private void overridePropertiesAst(ASTConfig astConfiguration, Map<String, String> overrideReport, ScanRequest request) {
        writeAstPropertiesToReport(astConfiguration, overrideReport);
        request.setAstConfig(astConfiguration);
    }

    private void writeAstPropertiesToReport(ASTConfig astConfiguration, Map<String, String> overrideReport) {
        overrideReport.put("AST apiUrl", astConfiguration.getApiUrl());
        overrideReport.put("AST preset", astConfiguration.getPreset());
        overrideReport.put("AST incremental", String.valueOf(astConfiguration.isIncremental()));
    }

    /**
     * Override scan request details as per file/blob (MachinaOverride)
     */
    public ScanRequest overrideScanRequestProperties(FlowOverride override, ScanRequest request) {
        scaConfigOverrider.initScaConfig(request);

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
        } else {
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

        String cannotOverrideReason = null;

        if (StringUtils.isEmpty(bugTrackerNameOverride)) {
            cannotOverrideReason = "no bug tracker override is defined";
        } else if (comingFromPullRequest && !bugTrackerNameOverride.equalsIgnoreCase(BugTracker.Type.NONE.toString())) {
            // if Bug-tracker override is 'NONE' - always override
            // If not, don't override bug tracker type if the scan is initiated by a pull request.
            // Otherwise bug tracker events won't be triggered.
            cannotOverrideReason = "scan was initiated by pull request and the overriding value is not NONE";
        } else if (bugTrackerNameOverride.equalsIgnoreCase(currentBugTrackerType.toString())) {
            cannotOverrideReason = "bug tracker type in override is the same as in scan request";
        }

        if (cannotOverrideReason != null) {
            log.debug("Bug tracker override was not applied, because {}.", cannotOverrideReason);
        }

        return cannotOverrideReason == null;
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
