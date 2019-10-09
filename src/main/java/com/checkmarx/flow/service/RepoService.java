package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.Sources;
import com.checkmarx.sdk.dto.CxConfig;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.utils.ScanUtils;

public abstract class RepoService {

    public abstract Sources getRepoContent();

    public CxConfig getCxConfigOverride(ScanRequest request) throws CheckmarxException {
        return null;
    }

    public String profileSource(Sources sources) throws CheckmarxException {
        //TODO iterate through fingerprint/profiles (TBD) and find the first match, and return appropriate "profile" aka Scanning preset
        return null;
    }
}
