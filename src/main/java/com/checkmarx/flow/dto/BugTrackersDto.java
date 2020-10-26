package com.checkmarx.flow.dto;

import com.checkmarx.flow.service.*;
import com.checkmarx.sdk.ShardManager.ShardSessionTracker;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Data
public class BugTrackersDto {
    private final EmailService emailService;
    private final BugTrackerEventTrigger bugTrackerEventTrigger;
    protected final GitHubService gitService;
    protected final GitLabService gitLabService;
    protected final BitBucketService bitBucketService;
    protected final ADOService adoService;
    protected final ShardSessionTracker sessionTracker;
}
