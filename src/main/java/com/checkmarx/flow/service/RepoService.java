package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.RepoComment;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.Sources;
import com.checkmarx.sdk.dto.sast.CxConfig;

import java.io.IOException;
import java.util.List;

public abstract class RepoService {
    public abstract Sources getRepoContent(ScanRequest request);

    public CxConfig getCxConfigOverride(ScanRequest request) {
        return null;
    }

    public abstract void deleteComment(String url, ScanRequest scanRequest);

    public abstract List<RepoComment> getComments(ScanRequest scanRequest) throws IOException;
}
