package com.checkmarx.jira;

import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.service.SastScanner;

import java.io.File;
import java.io.IOException;

public interface IPublishUtils {
    File getFileFromResourcePath(String path) throws IOException;
    BugTracker createJiraBugTracker();
    void publishRequest(ScanRequest request, File file, BugTracker.Type bugTrackerType, SastScanner scanner) throws ExitThrowable;
    ScanRequest getScanRequestWithDefaults();
}
