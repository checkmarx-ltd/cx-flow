package com.checkmarx.flow.handlers.config;

import com.checkmarx.flow.config.BitBucketProperties;
import com.checkmarx.flow.service.BitBucketService;

public interface BitBucketConfigContextProvider extends ConfigContextProvider  {
    
    public BitBucketProperties getBitBucketProperties();
    public BitBucketService getBitbucketService();

}
