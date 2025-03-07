package com.checkmarx.flow.config;

import com.checkmarx.sdk.config.CxGoProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Component
@ConfigurationProperties(prefix = "cx-flow")
@Validated
public class FlowProperties {
    private String contact;
    private String token;
    @NotNull @NotBlank
    private String bugTracker;
    private List<String> bugTrackerImpl;
    private List<String> branches;
    private List<String> filterSeverity;
    private List<String> filterCwe;
    private List<String> filterCategory;
    private List<String> filterStatus;
    private List<String> filterState;
    @Getter
    @Setter
    private List<String> excludeCategory;
    @Getter
    @Setter
    private List<String> excludeCwe;
    @Getter
    @Setter
    private List<String> excludeState;
    private String filterScript;
    private String commentScript;
    private List<String> enabledVulnerabilityScanners=new ArrayList<>();
    private boolean autoProfile = false;
    private boolean alwaysProfile = false;

    @Getter
    @Setter
    private boolean deleteForkedProject = false;


    @Getter
    @Setter
    private boolean disablePRFeedBack = false;

    @Getter
    @Setter
    private boolean enablecxFlowInteractive = false;








    private Integer profilingDepth = 1;
    private String profileConfig = "CxProfile.json";
    private boolean trackApplicationOnly = false;
    private boolean applicationRepoOnly = false;
    private String branchScript;
    private String mitreUrl;
    private String wikiUrl;
    private String codebashUrl;
    private String zipExclude;

    private String zipInclude;
    private boolean breakBuild = false;
    private Integer webHookQueue = 100;
    private Integer scanResultQueue = 4;
    private Integer httpConnectionTimeout = 30000;
    private Integer httpReadTimeout = 120000;
    private boolean listFalsePositives = false;
    private boolean scanResubmit = false;
    private String sshKeyPath;
    private Mail mail;
    @Getter @Setter
    private boolean preserveProjectName;
    private Map<FindingSeverity,Integer> thresholds;

    private boolean scanUnprotectedBranches= false;

    @Getter
    @Setter
    private boolean branchProtectionEnabled= false;

    @Getter
    @Setter
    private Boolean disableBreakbuild=false;

    @Getter
    @Setter
    private Integer maxPoolSize;

    @Getter
    @Setter
    private Integer corePoolSize;


    @Getter
    @Setter
    private Integer queuecapacityarg;
    @Getter
    @Setter
    private List<String> projectCustomField;
    @Getter
    @Setter
    private List<String> scanCustomField;

    public String getContact() {
        return this.contact;
    }

    public String getToken() {
        return this.token;
    }
    public String getZipInclude() {
        return zipInclude;
    }

    public void setZipInclude(String zipInclude) {
        this.zipInclude = zipInclude;
    }

    public @NotNull
    @NotBlank String getBugTracker() {
        return this.bugTracker;
    }

    public List<String> getBugTrackerImpl() {
        return bugTrackerImpl;
    }

    public void setBugTrackerImpl(List<String> bugTrackerImpl) {
        this.bugTrackerImpl = bugTrackerImpl;
    }

    public List<String> getBranches() {
        return this.branches;
    }

    public List<String> getFilterSeverity() {
        return this.filterSeverity;
    }

    public List<String> getFilterCwe() {
        return this.filterCwe;
    }

    public List<String> getFilterCategory() {
        return this.filterCategory;
    }

    public List<String> getFilterStatus() {
        return this.filterStatus;
    }

    public List<String> getFilterState() {
        return filterState;
    }

    public void setFilterState(List<String> filterState) {
        this.filterState = filterState;
    }

    public String getFilterScript() {
        return filterScript;
    }

    public void setFilterScript(String filterScript) {
        this.filterScript = filterScript;
    }

    public String getCommentScript() {
        return commentScript;
    }

    public void setCommentScript(String commentScript) {
        this.commentScript = commentScript;
    }

    public String getMitreUrl() {
        return this.mitreUrl;
    }

    public String getWikiUrl() {
        return this.wikiUrl;
    }

    public String getCodebashUrl() {
        return this.codebashUrl;
    }

    public Mail getMail() {
        return this.mail;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setBugTracker(@NotNull @NotBlank String bugTracker) {
        this.bugTracker = bugTracker;
    }

    public void setBranches(List<String> branches) {
        this.branches = branches;
    }

    public void setFilterSeverity(List<String> filterSeverity) {
        this.filterSeverity = filterSeverity;
    }

    public void setFilterCwe(List<String> filterCwe) {
        this.filterCwe = filterCwe;
    }

    public void setFilterCategory(List<String> filterCategory) {
        this.filterCategory = filterCategory;
    }

    public void setFilterStatus(List<String> filterStatus) {
        this.filterStatus = filterStatus;
    }

    public List<String> getEnabledVulnerabilityScanners() {
        return enabledVulnerabilityScanners;
    }

    public void setEnabledVulnerabilityScanners(List<String> enabledVulnerabilityScanners) {
        this.enabledVulnerabilityScanners = enabledVulnerabilityScanners;
    }

    /**
     * Defines how uniqueness is determined while correlating CxFlow issues with bug tracker issues.
     * @return
     * true: issues will be tracked according to the application name. The application name defaults to the repo name
     * but can be overridden in the WebHook flow.<br>
     * false: issues will be tracked by a combination of namespace/repo name/branch.  */
    public boolean isTrackApplicationOnly() {
        return trackApplicationOnly;
    }

    public void setTrackApplicationOnly(boolean trackApplicationOnly) {
        this.trackApplicationOnly = trackApplicationOnly;
    }

    public String getBranchScript() {
        return branchScript;
    }

    public void setBranchScript(String branchScript) {
        this.branchScript = branchScript;
    }

    public void setMitreUrl(String mitreUrl) {
        this.mitreUrl = mitreUrl;
    }

    public void setWikiUrl(String wikiUrl) {
        this.wikiUrl = wikiUrl;
    }

    public void setCodebashUrl(String codebashUrl) {
        this.codebashUrl = codebashUrl;
    }

    public void setMail(Mail mail) {
        this.mail = mail;
    }

    public Integer getWebHookQueue() {
        return webHookQueue;
    }

    public void setWebHookQueue(Integer webHookQueue) {
        this.webHookQueue = webHookQueue;
    }

    public Integer getScanResultQueue() {
        return scanResultQueue;
    }

    public void setScanResultQueue(Integer scanResultQueue) {
        this.scanResultQueue = scanResultQueue;
    }

    public boolean isBreakBuild() {
        return breakBuild;
    }

    public void setBreakBuild(boolean breakBuild) {
        this.breakBuild = breakBuild;
    }

    public String getZipExclude() {
        return zipExclude;
    }

    public void setZipExclude(String zipExclude) {
        this.zipExclude = zipExclude;
    }

    public Integer getHttpConnectionTimeout() {
        return httpConnectionTimeout;
    }

    public void setHttpConnectionTimeout(Integer httpConnectionTimeout) {
        this.httpConnectionTimeout = httpConnectionTimeout;
    }

    public Integer getHttpReadTimeout() {
        return httpReadTimeout;
    }

    public void setHttpReadTimeout(Integer httpReadTimeout) {
        this.httpReadTimeout = httpReadTimeout;
    }

    public boolean isApplicationRepoOnly() {
        return applicationRepoOnly;
    }

    public void setApplicationRepoOnly(boolean applicationRepoOnly) {
        this.applicationRepoOnly = applicationRepoOnly;
    }

    public boolean isAutoProfile() {
        return autoProfile;
    }

    public void setAutoProfile(boolean autoProfile) {
        this.autoProfile = autoProfile;
    }

    public Integer getProfilingDepth() {
        return profilingDepth;
    }

    public void setProfilingDepth(Integer profilingDepth) {
        this.profilingDepth = profilingDepth;
    }

    public String getProfileConfig() {
        return profileConfig;
    }

    public void setProfileConfig(String profileConfig) {
        this.profileConfig = profileConfig;
    }

    public boolean isAlwaysProfile() {
        return alwaysProfile;
    }

    public void setAlwaysProfile(boolean alwaysProfile) {
        this.alwaysProfile = alwaysProfile;
    }

    public boolean isListFalsePositives() {
        return listFalsePositives;
    }

    public void setListFalsePositives(boolean listFalsePositives) {
        this.listFalsePositives = listFalsePositives;
    }

    public boolean getScanResubmit() {return scanResubmit;}

    public void setScanResubmit(boolean scanResubmit) {this.scanResubmit = scanResubmit;}

    public Map<FindingSeverity, Integer> getThresholds() {
        return thresholds;
    }

    public void setThresholds(Map<FindingSeverity, Integer> thresholds) {
        this.thresholds = thresholds;
    }

    public void setSshkeypath(String sshKeyPath) {
        this.sshKeyPath = sshKeyPath;
    }

    public String getSshkeypath() {
        return sshKeyPath;
    }

    public static class Mail {
        private String host;
        private Integer port = 25;
        private String username;
        private String password;
        private List<String> cc;
        private boolean notificationEnabled = false;
        private EnabledNotifications enabledNotifications = new EnabledNotifications();
        private boolean allowEmptyMail = false;
        private String template;
        private SendGrid sendgrid;
        private MailTemplates templates;

        public String getHost() {
            return this.host;
        }

        public Integer getPort() {
            return this.port;
        }

        public String getUsername() {
            return this.username;
        }

        public String getPassword() {
            return this.password;
        }

        public boolean isNotificationEnabled() {
            return this.notificationEnabled;
        }

        public boolean isEmptyMailAllowed() { return this.allowEmptyMail; }

        public void setHost(String host) {
            this.host = host;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public void setNotification(boolean notification) {
            this.notificationEnabled = notification;
        }

        public EnabledNotifications getEnabledNotifications() {
            return this.enabledNotifications;
        }

        public void setEnabledNotifications(EnabledNotifications enabledNotifications) {
            this.enabledNotifications = enabledNotifications;
        }

        public List<String> getCc() { return this.cc; }

        public void setCc(List<String> cc) {
            this.cc = cc;
        }

        public void setAllowEmptyMail(boolean allowEmptyMail) {
            this.allowEmptyMail = allowEmptyMail;
        }

        public String getTemplate() { return template; }

        public void setTemplate(String template) { this.template = template; }

        public SendGrid getSendgrid() {
            return sendgrid;
        }

        public void setSendgrid(SendGrid sendgrid) {
            this.sendgrid = sendgrid;
        }

        public MailTemplates getTemplates() { return templates; }

        public void setTemplates(MailTemplates templates) { this.templates = templates; }
    }

    public static class EnabledNotifications {
        private boolean scanSubmitted = true;
        private boolean scanSummary = true;
        private boolean scanSummaryWithEmptyResults = true;

        public boolean getScanSubmitted() {
            return scanSubmitted;
        }

        public void setScanSubmitted(boolean scanSubmitted) {
            this.scanSubmitted = scanSubmitted;
        }

        public boolean getScanSummary() {
            return scanSummary;
        }

        public void setScanSummary(boolean scanSummary) {
            this.scanSummary = scanSummary;
        }

        public boolean getScanSummaryWithEmptyResults() {
            return scanSummaryWithEmptyResults;
        }

        public void setScanSummaryWithEmptyResults(boolean scanSummaryWithEmptyResults) {
            this.scanSummaryWithEmptyResults = scanSummaryWithEmptyResults;
        }
    }

    public static class MailTemplates {
        private String scanSubmitted;
        private String scanCompletedSuccessfully;

        public String getScanSubmitted() {
            return scanSubmitted;
        }

        public void setScanSubmitted(String scanSubmitted) {
            this.scanSubmitted = scanSubmitted;
        }

        public String getScanCompletedSuccessfully() {
            return scanCompletedSuccessfully;
        }

        public void setScanCompletedSuccessfully(String scanCompletedSuccessfully) {
            this.scanCompletedSuccessfully = scanCompletedSuccessfully;
        }
    }

    public static class SendGrid {
        private String apiToken;

        public String getApiToken() {
            return apiToken;
        }

        public void setApiToken(String apiToken) {
            this.apiToken = apiToken;
        }
    }

    public boolean isCxGoEnabled() {
        return anyScannerEnabled() && enabledVulnerabilityScanners.toString().toLowerCase().contains(CxGoProperties.CONFIG_PREFIX);
    }

    private boolean anyScannerEnabled() {
        return enabledVulnerabilityScanners != null && !enabledVulnerabilityScanners.isEmpty();
    }

    public boolean isScanUnprotectedBranches() {
        return scanUnprotectedBranches;
    }

    public void setScanUnprotectedBranches(boolean scanUnprotectedBranches) {
        this.scanUnprotectedBranches = scanUnprotectedBranches;
    }
}