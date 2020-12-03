package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.sdk.config.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GitAuthUrlGenerator {

    public String addCredToUrl(ScanRequest.Repository repoType, String gitUrl, String scmAccessToken) {
        String gitAuthUrl;

        switch (repoType) {
            case GITHUB:
            case ADO:
            case BITBUCKETSERVER:
                gitAuthUrl = gitUrl.replace(Constants.HTTPS, Constants.HTTPS.concat(scmAccessToken).concat("@"));
                return gitAuthUrl.replace(Constants.HTTP, Constants.HTTP.concat(scmAccessToken).concat("@"));
            case BITBUCKET:
                return gitUrl.replace(Constants.HTTPS, Constants.HTTPS.concat(scmAccessToken).concat("@"));
            case GITLAB:
                gitAuthUrl = gitUrl.replace(Constants.HTTPS, Constants.HTTPS_OAUTH2.concat(scmAccessToken).concat("@"));
                return gitAuthUrl.replace(Constants.HTTP, Constants.HTTP_OAUTH2.concat(scmAccessToken).concat("@"));
            default:
                throw new MachinaRuntimeException("SCM type: " + repoType + " is not supported");
        }
    }
}