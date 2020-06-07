package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.utils.ScanUtils;
import org.springframework.stereotype.Service;

@Service
public class SanitizingFilenameFormatter implements FilenameFormatter {
    @Override
    public String format(ScanRequest request, String filenameFormat, String dataFolder) {
        String result = filenameFormat;
        result = ScanUtils.getFilename(request, result);
        if (!ScanUtils.empty(dataFolder)) {
            if (dataFolder.endsWith("/")) {
                result = dataFolder.concat(result);
            } else {
                result = dataFolder.concat("/").concat(result);
            }
        }
        return result;
    }
}
