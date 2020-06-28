package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.ScanRequest;

public interface FilenameFormatter {
    /**
     * Formats filenameTemplate by replacing placeholders in the template with values from request.
     * Additionally, current timestamp may be specified as a placeholder.
     * @param request used as a source of placeholder values
     * @param filenameTemplate filename that may optionally contain placeholders
     * @param baseDir path that is prepended to the formatted filename
     * @return the formatted path
     */
    String formatPath(ScanRequest request, String filenameTemplate, String baseDir);

    /**
     * Formats filenameTemplate by replacing placeholders in the template with values from request.
     * Additionally, current timestamp may be specified as a placeholder.
     * @param request used as a source of placeholder values
     * @param filenameTemplate filename that may optionally contain placeholders
     * @return the formatted filename
     */
    String formatFilenameTemplate(ScanRequest request, String filenameTemplate);
}
