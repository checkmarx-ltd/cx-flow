package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.utils.ScanUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class SanitizingFilenameFormatter implements FilenameFormatter {
    @Override
    public String formatPath(ScanRequest request, String filenameTemplate, String baseDir) {
        String result = formatFilenameTemplate(request, filenameTemplate);
        if (!ScanUtils.empty(baseDir)) {
            if (baseDir.endsWith("/")) {
                result = baseDir.concat(result);
            } else {
                result = baseDir.concat("/").concat(result);
            }
        }
        return result;
    }

    @Override
    public String formatFilenameTemplate(ScanRequest request, String filenameTemplate) {
        String result = filenameTemplate;

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss");
        String timestamp = now.format(formatter);
        result = result.replace("[TIME]", timestamp);
        log.debug("Timestamp: {}", timestamp);
        log.debug(result);

        result = fillPlaceholder(result, "[TEAM]", request.getTeam());
        result = fillPlaceholder(result, "[APP]", request.getApplication());
        result = fillPlaceholder(result, "[PROJECT]", request.getProject());
        result = fillPlaceholder(result, "[NAMESPACE]", request.getNamespace());
        result = fillPlaceholder(result, "[REPO]", request.getRepoName());
        result = fillPlaceholder(result, "[BRANCH]", request.getBranch());

        return sanitizeAgainstPathTraversal(result);
    }

    private static String fillPlaceholder(String filename, String placeholder, String actualValue){
        if(StringUtils.isNotEmpty(actualValue)) {
            actualValue = actualValue.replaceAll("[^a-zA-Z0-9-_]+","_");
            filename = filename.replace(placeholder, actualValue);
            log.debug(actualValue);
            log.debug(filename);
        }
        return filename;
    }

    private String sanitizeAgainstPathTraversal(String filename) {
        return Paths.get(filename)
                .getFileName()
                .toString();
    }
}
