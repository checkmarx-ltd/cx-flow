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
    public String format(ScanRequest request, String filenameFormat, String dataFolder) {
        String result = formatFilenameTemplate(request, filenameFormat);
        result = sanitizeAgainstPathTraversal(result);
        if (!ScanUtils.empty(dataFolder)) {
            if (dataFolder.endsWith("/")) {
                result = dataFolder.concat(result);
            } else {
                result = dataFolder.concat("/").concat(result);
            }
        }
        return result;
    }

    @Override
    public String formatFilenameTemplate(ScanRequest request, String filenameTemplate) {
        String filename;
        filename = filenameTemplate;

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss");
        String dt = now.format(formatter);
        filename = filename.replace("[TIME]", dt);
        log.debug(dt);
        log.debug(filename);

        filename = fillPlaceholder(filename, "[TEAM]", request.getTeam());
        filename = fillPlaceholder(filename, "[APP]", request.getApplication());
        filename = fillPlaceholder(filename, "[PROJECT]", request.getProject());
        filename = fillPlaceholder(filename, "[NAMESPACE]", request.getNamespace());
        filename = fillPlaceholder(filename, "[REPO]", request.getRepoName());
        filename = fillPlaceholder(filename, "[BRANCH]", request.getBranch());

        return filename;
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
