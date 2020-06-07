package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.ScanRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SanitizingFilenameFormatterTest {
    public static final String BASE_DIR = "/tmp/report/details";
    public static final String EXPECTED_RESULT = "/tmp/report/details/output.csv";

    @Test
    void format_noTrailingSlash_basicFunctionality() {
        verifySanity(false);
    }

    @Test
    void format_trailingSlash_basicFunctionality() {
        verifySanity(true);
    }

    @Test
    void format_parentDirs_sanitizingCorrectly() {
        SanitizingFilenameFormatter formatter = new SanitizingFilenameFormatter();
        ScanRequest request = ScanRequest.builder().build();
        String formatted = formatter.formatPath(request, "../../../output.csv", BASE_DIR);
        assertEquals(EXPECTED_RESULT, formatted, "Sanitizing doesn't work as expected.");
    }

    @Test
    void format_placeholders_replacementWorks() {
        SanitizingFilenameFormatter formatter = new SanitizingFilenameFormatter();
        ScanRequest request = ScanRequest.builder()
                .team("ninjas")
                .application("CX")
                .project("ePayment")
                .namespace("myOrg")
                .repoName("myRepo")
                .branch("fixed-last-bug")
                .build();

        final String filenameTemplate = "[TEAM]-[APP]-[PROJECT]-[NAMESPACE]-[REPO]-[BRANCH].csv";
        final String expected = String.format("%s/ninjas-CX-ePayment-myOrg-myRepo-fixed-last-bug.csv", BASE_DIR);
        String actual = formatter.formatPath(request, filenameTemplate, BASE_DIR);

        assertEquals(expected, actual, "Unexpected path after formatting by template.");
    }

    private void verifySanity(boolean useTrailingSlash) {
        SanitizingFilenameFormatter formatter = new SanitizingFilenameFormatter();
        ScanRequest request = ScanRequest.builder().build();
        String dir = BASE_DIR + (useTrailingSlash ? "/" : "");
        String formatted = formatter.formatPath(request, "output.csv", dir);
        assertEquals(EXPECTED_RESULT, formatted, "Unexpected path format.");
    }
}