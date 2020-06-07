package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.ScanRequest;

public interface FilenameFormatter {
    String format(ScanRequest request, String filenameFormat, String dataFolder);
}
