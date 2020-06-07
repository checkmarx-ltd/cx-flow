package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.ScanRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SanitizingFilenameFormatterTest {

    @Test
    void format_noTrailingSlash_sanity() {
        verifySanity(false);
    }
    @Test
    void format_trailingSlash_sanity() {
        verifySanity(true);
    }

    private void verifySanity(boolean useTrailingSlash) {
        SanitizingFilenameFormatter formatter = new SanitizingFilenameFormatter();
        ScanRequest request = ScanRequest.builder().build();
        String dir = "/tmp/report/details" + (useTrailingSlash ? "/" : "");
        String formatted = formatter.format(request, "output.csv", dir);
        Assertions.assertEquals("/tmp/report/details/output.csv", formatted, "Unexpected path format.");
    }
}