package com.checkmarx.flow.dto;

import com.checkmarx.sdk.dto.ScanResults;

import java.util.concurrent.CompletableFuture;

public class ScanDetails {

    private CompletableFuture<ScanResults> results = null;
    private Integer projectId = null;
    private Integer scanId = null;
    private String osaScanId = null;
    private boolean processResults = true;

    public ScanDetails(){
        
    }
    
    public ScanDetails(Integer projectId, Integer scanId, String osaScanId) {
        this.projectId = projectId;
        this.scanId = scanId;
        this.osaScanId = osaScanId;
    }

    public ScanDetails(Integer projectId, Integer scanId, CompletableFuture<ScanResults> results, boolean processResults) {
        this.projectId = projectId;
        this.scanId = scanId;
        this.results = results;
        this.processResults = processResults;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public Integer getScanId() {
        return scanId;
    }

    public String getOsaScanId() {
        return osaScanId;
    }

    public boolean processResults(){
        return processResults;
    }

    public boolean isOsaScan(){
        return osaScanId == null;
    }

    public CompletableFuture<ScanResults> getResults() {
        return results;
    }
}
