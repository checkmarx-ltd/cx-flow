package com.checkmarx.flow.config;

public class RepoProperties {
    private boolean enabled;
    private String webhookToken;
    private String token;
    private String url;
    private String apiUrl;
    private boolean autoProfile = false;
    private Integer profilingDepth = 1;
    private String falsePositiveLabel = "false-positive";
    private String configAsCode = "cx.config";
    private String openTransition = "open";
    private String closeTransition = "closed";
    private boolean blockMerge = false;
    private boolean errorMerge = false;
    private boolean detailed = true;
    private String detailHeader = "Details";
    private boolean flowSummary = true;
    private String flowSummaryHeader = "Violation Summary";
    private boolean cxSummary = false;
    private String cxSummaryHeader = "Checkmarx Scan Summary";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getWebhookToken() {
        return webhookToken;
    }

    public void setWebhookToken(String webhookToken) {
        this.webhookToken = webhookToken;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getConfigAsCode() {
        return configAsCode;
    }

    public void setConfigAsCode(String configAsCode) {
        this.configAsCode = configAsCode;
    }

    public String getFalsePositiveLabel() {
        return falsePositiveLabel;
    }

    public void setFalsePositiveLabel(String falsePositiveLabel) {
        this.falsePositiveLabel = falsePositiveLabel;
    }

    public String getOpenTransition() {
        return openTransition;
    }

    public void setOpenTransition(String openTransition) {
        this.openTransition = openTransition;
    }

    public String getCloseTransition() {
        return closeTransition;
    }

    public void setCloseTransition(String closeTransition) {
        this.closeTransition = closeTransition;
    }

    public boolean isBlockMerge() {
        return blockMerge;
    }

    public void setBlockMerge(boolean blockMerge) {
        this.blockMerge = blockMerge;
    }

    public boolean isErrorMerge() {
        return errorMerge;
    }

    public void setErrorMerge(boolean errorMerge) {
        this.errorMerge = errorMerge;
    }

    public boolean isDetailed() {
        return detailed;
    }

    public void setDetailed(boolean detailed) {
        this.detailed = detailed;
    }

    public boolean isCxSummary() {
        return cxSummary;
    }

    public void setCxSummary(boolean cxSummary) {
        this.cxSummary = cxSummary;
    }

    public boolean isFlowSummary() {
        return flowSummary;
    }

    public void setFlowSummary(boolean flowSummary) {
        this.flowSummary = flowSummary;
    }

    public String getDetailHeader() {
        return detailHeader;
    }

    public void setDetailHeader(String detailHeader) {
        this.detailHeader = detailHeader;
    }

    public String getFlowSummaryHeader() {
        return flowSummaryHeader;
    }

    public void setFlowSummaryHeader(String flowSummaryHeader) {
        this.flowSummaryHeader = flowSummaryHeader;
    }

    public String getCxSummaryHeader() {
        return cxSummaryHeader;
    }

    public void setCxSummaryHeader(String cxSummaryHeader) {
        this.cxSummaryHeader = cxSummaryHeader;
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
}
