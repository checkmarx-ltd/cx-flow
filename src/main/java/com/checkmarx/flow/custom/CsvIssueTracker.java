package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.beans.ConstructorProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service("Csv")
public class CsvIssueTracker implements IssueTracker {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(CsvIssueTracker.class);
    private final CsvProperties properties;
    private final FlowProperties flowProperties;

    @ConstructorProperties({"properties", "flowProperties"})
    public CsvIssueTracker(CsvProperties properties, FlowProperties flowProperties) {
        this.properties = properties;
        this.flowProperties = flowProperties;
    }

    @Override
    public void init(ScanRequest request, ScanResults results) throws MachinaException {
        String filename = properties.getFileNameFormat();
        filename = ScanUtils.getFilename(request, filename);
        String folder = properties.getDataFolder();
        if(!ScanUtils.empty(folder) && folder.endsWith("/")){
            filename = folder.concat(filename);
        }
        else if(!ScanUtils.empty(folder) && !folder.endsWith("/")){
            filename = folder.concat("/").concat(filename);
        }
        request.setFilename(filename);
        log.info("Creating file {}", filename);
        log.info("Deleting if already exists");
        try {
            Files.deleteIfExists(Paths.get(filename));
            Files.createFile(Paths.get(filename));
            if(properties.isIncludeHeader()) {
                log.debug("Writing headers for CSV");
                String headers = getHeaders(request, properties.getFields()).concat(ScanUtils.CRLF);
                Files.write(Paths.get(request.getFilename()), headers.getBytes());
            }
        } catch (IOException e){
            log.error("Issue deleting existing file or writing initial {}", filename, e);
        }
    }

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Finalizing CSV output");
    }

    @Override
    public String getFalsePositiveLabel() throws MachinaException {
        return null;
    }

    @Override
    public List<Issue> getIssues(ScanRequest request) throws MachinaException {
        return null;
    }

    @Override
    public Issue createIssue(ScanResults.XIssue issue, ScanRequest request) throws MachinaException {
        List<String> values = new ArrayList<>();
        for(CsvProperties.Field f: properties.getFields()) {
            String value;
            switch (f.getName()) {
                case "summary":
                    value = issue.getVulnerability().concat(" @ ").concat(issue.getFilename());
                    log.debug("summary: {}", value);
                    break;
                case "application":
                    log.debug("application: {}", request.getApplication());
                    value = request.getApplication();
                    break;
                case "static":
                    log.debug("static: {}", f.getDefaultValue());
                    value = f.getDefaultValue();
                    break;
                case "project":
                    log.debug("project: {}", request.getProject());
                    value = request.getProject();
                    break;
                case "namespace":
                    log.debug("namespace: {}", request.getNamespace());
                    value = request.getNamespace();
                    break;
                case "repo-name":
                    log.debug("repo-name: {}", request.getRepoName());
                    value = request.getRepoName();
                    break;
                case "repo-url":
                    log.debug("repo-url: {}", request.getRepoUrl());
                    value = request.getRepoUrl();
                    break;
                case "branch":
                    log.debug("branch: {}", request.getBranch());
                    value = request.getBranch();
                    break;
                case "severity":
                    log.debug("severity: {}", issue.getSeverity());
                    value = issue.getSeverity();
                    break;
                case "category":
                    log.debug("category: {}", issue.getVulnerability());
                    value = issue.getVulnerability();
                    break;
                case "cwe":
                    log.debug("cwe: {}", issue.getCwe());
                    value = issue.getCwe();
                    break;
                case "cve":
                    log.debug("cve: {}", issue.getCve());
                    value = issue.getCve();
                    break;
                case "recommendation":
                    value = String.format(flowProperties.getMitreUrl(), issue.getCwe());
                    break;
                case "loc":
                    value = "";
                    List<String> lines = new ArrayList<>();
                    if (issue.getDetails() != null && !issue.getDetails().isEmpty()) {
                        for (Map.Entry<Integer, ScanResults.IssueDetails> entry : issue.getDetails().entrySet()) {
                            if (entry.getKey() != null && entry.getValue() != null && !entry.getValue().isFalsePositive() && !ScanUtils.empty(entry.getValue().getCodeSnippet())) {
                                lines.add(entry.getKey().toString());
                            }
                        }
                        Collections.sort(lines);
                        value = StringUtils.join(lines, ",");
                        log.debug("loc: {}", value);
                    }
                    break;
                case "site":
                    log.debug("site: {}", request.getSite());
                    value = request.getSite();
                    break;
                case "issue-link":
                    log.debug("issue-link: {}", issue.getLink());
                    value = issue.getLink();
                    break;
                case "filename":
                    log.debug("filename: {}", issue.getFilename());
                    value = issue.getFilename();
                    break;
                case "language":
                    log.debug("language: {}", issue.getLanguage());
                    value = issue.getLanguage();
                    break;
                case "similarity-id":
                    log.debug("similarity-id: {}", issue.getSimilarityId());
                    value = issue.getSimilarityId();
                    break;
                case "description":
                    log.debug("description: {}", issue.getDescription());
                    value = issue.getDescription();
                    break;
                default:
                    if (request.getCxFields() != null) {
                        log.debug("Checking for Checkmarx custom field {}", f.getName());
                        value = request.getCxFields().get(f.getName());
                        if(ScanUtils.empty(value)){
                            log.warn("field value for {} not found", f.getName());
                        }
                        log.debug("Cx Field value: {}",value);
                    }
                    else {
                        log.warn("field value for {} not found", f.getName());
                        value = "";
                    }
            }
            if (ScanUtils.empty(value)) {
                log.debug("Value is empty, defaulting to configured default (if applicable)");
                if (!ScanUtils.empty(f.getDefaultValue())) {
                    value = f.getDefaultValue();
                    log.debug("Default value is {}", value);
                }
            }
            if(value == null){
                value = "";
            }
            if(!ScanUtils.empty(f.getPrefix())){
                value = subValues(request, f.getPrefix()).concat(value);
            }
            if(!ScanUtils.empty(f.getPostfix())){
                value = value.concat(subValues(request, f.getPostfix()));
            }
            value = escapeSpecialCharacters(value);
            if(value.contains(",")){
                value = "\"".concat(value).concat("\"");
            }
            values.add(value);
        }
        String csv = convertToCSV(values).concat(ScanUtils.CRLF);

        try {
            Files.write(Paths.get(request.getFilename()), csv.getBytes(), StandardOpenOption.APPEND);
        }catch (IOException e){
            log.error("Error writing to file {}, value {}", request.getFilename(), csv, e);
        }
        return null;
    }

    @Override
    public void closeIssue(Issue issue, ScanRequest request) throws MachinaException {

    }

    @Override
    public Issue updateIssue(Issue issue, ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        return null;
    }

    @Override
    public String getIssueKey(Issue issue, ScanRequest request) {
        return issue.getId();
    }

    @Override
    public String getXIssueKey(ScanResults.XIssue issue, ScanRequest request) {
        return issue.getFilename();
    }

    @Override
    public boolean isIssueClosed(Issue issue, ScanRequest request) {
        return false;
    }

    @Override
    public boolean isIssueOpened(Issue issue, ScanRequest request) {
        return false;
    }

    private String escapeSpecialCharacters(String data) {
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }

    private String convertToCSV(List<String> data) {
        String[] dataArr = data.toArray(new String[0]);

        return Stream.of(dataArr)
                .map(this::escapeSpecialCharacters)
                .collect(Collectors.joining(","));
    }

    private String getHeaders(ScanRequest request, List<CsvProperties.Field> fields){
        List<String> values = new ArrayList<>();
        for(CsvProperties.Field f: fields){
            String header = escapeSpecialCharacters(f.getHeader());
            values.add(header);
        }
        return convertToCSV(values);
    }

    private String subValues(ScanRequest request, String value){

        if(!ScanUtils.empty(request.getTeam())){
            String team = request.getTeam();
            team = team.replaceAll("\\\\","_");
            team = team.replaceAll("/","_");
            value = value.replace("[TEAM]", team);
        }
        if(!ScanUtils.empty(request.getApplication())) {
            value = value.replace("[APP]", request.getApplication());
            log.debug(request.getApplication());
            log.debug(value);
        }
        if(!ScanUtils.empty(request.getProject())) {
            value = value.replace("[PROJECT]", request.getProject());
            log.debug(request.getProject());
            log.debug(value);
        }
        if(!ScanUtils.empty(request.getNamespace())) {
            value = value.replace("[NAMESPACE]", request.getNamespace());
            log.debug(request.getNamespace());
            log.debug(value);
        }
        if(!ScanUtils.empty(request.getRepoName())) {
            value = value.replace("[REPO]", request.getRepoName());
            log.debug(request.getRepoName());
            log.debug(value);
        }
        if(!ScanUtils.empty(request.getBranch())) {
            value = value.replace("[BRANCH]", request.getBranch());
            log.debug(request.getBranch());
            log.debug(value);
        }
        return value;
    }

}