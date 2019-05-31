package com.checkmarx.flow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Component
@ConfigurationProperties(prefix = "checkmarx")
@Validated
public class CxProperties {

    @NotNull @NotBlank
    private String username;

    @NotNull @NotBlank
    private String password;

    @NotNull @NotBlank
    private String clientSecret;

    @NotNull @NotBlank
    private String baseUrl;

    @NotNull @NotBlank
    private String url;

    private boolean multiTenant = false;
    private String scanPreset = "Checkmarx Default";
    private String configuration = "Default Configuration";
    private Boolean incremental = false;
    private Integer incrementalThreshold = 7;
    private Integer incrementalNumScans = 5;
    private String team;
    private Boolean offline = false;
    private Boolean preserveXml = false;
    private Integer scanTimeout = 120;
    private String jiraProjectField = "jira-project";
    private String jiraIssuetypeField = "jira-issuetype";
    private String jiraCustomField = "jira-fields";
    private String jiraAssigneeField = "jira-assignee";

    @NotNull @NotBlank
    private String portalUrl;

    @NotNull @NotBlank
    private String sdkUrl;

    @NotNull @NotBlank
    private String portalWsdl;

    @NotNull @NotBlank
    private String sdkWsdl;

    @NotNull @NotBlank
    private String portalPackage = "checkmarx.wsdl.portal";

    private String htmlStrip = "<style>.cxtaghighlight{color: rgb(101, 170, 235);font-weight:bold;}</style>";

    public @NotNull
    @NotBlank String getUsername() {
        return this.username;
    }

    public @NotNull
    @NotBlank String getPassword() {
        return this.password;
    }

    public @NotNull
    @NotBlank String getClientSecret() {
        return this.clientSecret;
    }

    public @NotNull
    @NotBlank String getBaseUrl() {
        return this.baseUrl;
    }

    public @NotNull
    @NotBlank String getUrl() {
        return this.url;
    }

    public boolean isMultiTenant() {
        return this.multiTenant;
    }

    public @NotNull Boolean getIncremental() {
        return this.incremental;
    }

    public @NotNull Integer getIncrementalThreshold() {
        return this.incrementalThreshold;
    }

    public @NotNull Integer getIncrementalNumScans(){
        return this.incrementalNumScans;
    }

    public String getScanPreset() {
        return this.scanPreset;
    }

    public String getConfiguration() {
        return this.configuration;
    }

    public String getTeam() {
        return this.team;
    }

    public Boolean getOffline() {
        return this.offline;
    }

    public Boolean getPreserveXml() {
        return preserveXml;
    }

    public void setPreserveXml(Boolean preserveXml) {
        this.preserveXml = preserveXml;
    }

    public Integer getScanTimeout() {
        return this.scanTimeout;
    }

    public String getJiraProjectField() {
        return this.jiraProjectField;
    }

    public String getJiraCustomField() {
        return this.jiraCustomField;
    }

    public String getJiraIssuetypeField() {
        return this.jiraIssuetypeField;
    }

    public @NotNull
    @NotBlank String getPortalUrl() {
        return this.portalUrl;
    }

    public @NotNull
    @NotBlank String getSdkUrl() {
        return this.sdkUrl;
    }

    public @NotNull
    @NotBlank String getPortalWsdl() {
        return this.portalWsdl;
    }

    public @NotNull
    @NotBlank String getSdkWsdl() {
        return this.sdkWsdl;
    }

    public @NotNull
    @NotBlank String getPortalPackage() {
        return this.portalPackage;
    }

    public String getHtmlStrip() {
        return this.htmlStrip;
    }

    public void setUsername(@NotNull @NotBlank String username) {
        this.username = username;
    }

    public void setPassword(@NotNull @NotBlank String password) {
        this.password = password;
    }

    public void setClientSecret(@NotNull @NotBlank String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setBaseUrl(@NotNull @NotBlank String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setUrl(@NotNull @NotBlank String url) {
        this.url = url;
    }

    public void setMultiTenant(boolean multiTenant) {
        this.multiTenant = multiTenant;
    }

    public void setIncremental(@NotNull Boolean incremental) {
        this.incremental = incremental;
    }

    public void setIncrementalThreshold(@NotNull Integer incrementalThreshold){
        this.incrementalThreshold = incrementalThreshold;
    }

    public void setIncrementalNumScans(@NotNull Integer incrementalNumScans){
        this.incrementalNumScans = incrementalNumScans;
    }

    public void setScanPreset(String scanPreset) {
        this.scanPreset = scanPreset;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public void setOffline(Boolean offline) {
        this.offline = offline;
    }

    public void setScanTimeout(Integer scanTimeout) {
        this.scanTimeout = scanTimeout;
    }

    public void setJiraProjectField(String jiraProjectField) {
        this.jiraProjectField = jiraProjectField;
    }

    public void setJiraIssuetypeField(String jiraIssuetypeField) {
        this.jiraIssuetypeField = jiraIssuetypeField;
    }

    public void setJiraCustomField(String jiraCustomField) {
        this.jiraCustomField = jiraCustomField;
    }

    public void setPortalUrl(@NotNull @NotBlank String portalUrl) {
        this.portalUrl = portalUrl;
    }

    public void setSdkUrl(@NotNull @NotBlank String sdkUrl) {
        this.sdkUrl = sdkUrl;
    }

    public void setPortalWsdl(@NotNull @NotBlank String portalWsdl) {
        this.portalWsdl = portalWsdl;
    }

    public void setSdkWsdl(@NotNull @NotBlank String sdkWsdl) {
        this.sdkWsdl = sdkWsdl;
    }

    public void setPortalPackage(@NotNull @NotBlank String portalPackage) {
        this.portalPackage = portalPackage;
    }

    public void setHtmlStrip(String htmlStrip) {
        this.htmlStrip = htmlStrip;
    }

    public String getJiraAssigneeField() {
        return this.jiraAssigneeField;
    }

    public void setJiraAssigneeField(String jiraAssigneeField) {
        this.jiraAssigneeField = jiraAssigneeField;
    }
}
