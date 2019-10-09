package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.Sources;
import com.checkmarx.sdk.dto.CxConfig;
import com.checkmarx.sdk.utils.ScanUtils;

public abstract class RepoService {

    public abstract Sources getRepoContent();

    public CxConfig getCxConfigOverride(){
        return null;
    }

    public String profileSource(Sources sources){
        //TODO iterate through fingerprint/profiles (TBD) and find the first match, and return appropriate "profile" aka Scanning preset
        return null;
    }
}
