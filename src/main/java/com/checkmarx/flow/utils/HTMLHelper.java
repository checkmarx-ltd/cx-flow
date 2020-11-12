package com.checkmarx.flow.utils;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.RepoProperties;
import com.checkmarx.flow.constants.SCATicketingConstants;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.dto.Filter.Severity;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.ast.SCAResults;
import com.checkmarx.sdk.dto.cx.CxScanSummary;
import com.cx.restclient.ast.dto.sca.report.Finding;
import com.cx.restclient.ast.dto.sca.report.Package;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.checkmarx.flow.service.PullRequestCommentsHelper.COMMENT_TYPE_SCA_FINDINGS;
import static java.util.Map.Entry.comparingByKey;


@Slf4j
public class HTMLHelper {

    public static final String VERSION = "Version: ";
    public static final String DESCRIPTION = "Description: ";
    public static final String RECOMMENDATION = "Recommendation: ";
    public static final String RECOMMENDED_FIX = "recommendedFix";
    public static final String URL = "URL: ";
    public static final String DETAILS = "Details - ";
    public static final String SEVERITY = "Severity";
    public static final String DIV_CLOSING_TAG = "</div>";
    public static final String CRLF = "\r\n";
    public static final String WEB_HOOK_PAYLOAD = "web-hook-payload";
    private static final String ITALIC_OPENING_DIV = "<div><i>";
    private static final String ITALIC_CLOSING_DIV = "</i></div>";
    private static final String NVD_URL_PREFIX = "https://nvd.nist.gov/vuln/detail/";

    public static final String ISSUE_BODY = "**%s** issue exists @ **%s** in branch **%s**";
    public static final String ISSUE_BODY_TEXT = "%s issue exists @ %s in branch %s";

    private static final String DIV_A_HREF = "<div><a href='";
    public static final String NO_POLICY_VIOLATION_MESSAGE = "No policy violation found";
    private static final String NO_PACKAGE_VIOLATION_MESSAGE = "No package violation found";

    private HTMLHelper() {
    }

    public static String getMergeCommentMD(ScanRequest request, ScanResults results, RepoProperties properties) {
        StringBuilder body = new StringBuilder();

        if (results.isSastResults() || results.isAstResults()) {
            log.debug("Building merge comment MD for SAST scanner");

            if (results.isAstResults()) {
                ScanUtils.setASTXIssuesInScanResults(results);
            }

            addScanSummarySection(request, results, properties, body);
            addFlowSummarySection(results, properties, body, request);
            addDetailsSection(request, results, properties, body);
        }

        addScaBody(results, body, request);
        return body.toString();
    }

    /**
     * = Generates an HTML message describing the discovered issue.
     *
     * @param issue The issue to add the comment too
     * @return string with the HTML message
     */
    public static String getHTMLBody(ScanResults.XIssue issue, ScanRequest request, FlowProperties flowProperties) {
        String branch = request.getBranch();
        StringBuilder body = new StringBuilder();
        body.append("<div>");

        if (Optional.ofNullable(issue.getScaDetails()).isPresent()) {
            setSCAHtmlBody(issue, request, body);

        } else {
            setSASTHtmlBody(issue, flowProperties, branch, body);
        }
        body.append(DIV_CLOSING_TAG);
        return body.toString();
    }

    private static void addFlowSummarySection(ScanResults results, RepoProperties properties, StringBuilder body, ScanRequest request) {
        if (properties.isFlowSummary() ) {
            if (!ScanUtils.empty(properties.getFlowSummaryHeader())) {
                appendAll(body, MarkDownHelper.getMdHeaderType(3, properties.getFlowSummaryHeader()), CRLF);
            }
            Map<String, Integer> flowSummaryToMap = (Map<String, Integer>) results.getAdditionalDetails().get(Constants.SUMMARY_KEY);

            if (flowSummaryToMap.isEmpty()) {
                appendAll(body, MarkDownHelper.getMdHeaderType(4, NO_POLICY_VIOLATION_MESSAGE), CRLF);
            } else {
                flowSummaryToMap.forEach((severityKey, value) ->
                        appendAll(body,
                                MarkDownHelper.getSeverityIconFromLinkByText(severityKey, request),
                                MarkDownHelper.getNonBreakingSpace(request), MarkDownHelper.getBoldText(value.toString()),
                                " ",
                                MarkDownHelper.getBoldText(severityKey), MarkDownHelper.getLineBreak(request))
                );
                body.append(CRLF);
                appendAll(body, MarkDownHelper.getTextLink(MarkDownHelper.MORE_DETAILS_LINK_HEADER, results.getLink()), CRLF);
            }
        }
    }
    private static void addScaBody(ScanResults results, StringBuilder body, ScanRequest request) {
        Optional.ofNullable(results.getScaResults()).ifPresent(r -> {
            log.debug("Building merge comment MD for SCA scanner");
            if (body.length() > 0) {
                appendAll(body, "***", CRLF);
            }

            appendAll(body, MarkDownHelper.getCheckmarxLogoFromLink(request), CRLF, MarkDownHelper.getScaHeader(), CRLF);
            scaSummaryBuilder(body, r, request);
            appendAll(body, MarkDownHelper.getMdHeaderType(3, COMMENT_TYPE_SCA_FINDINGS), CRLF);

            if (r.getFindings().isEmpty()) {
                appendAll(body, MarkDownHelper.getMdHeaderType(4, NO_PACKAGE_VIOLATION_MESSAGE), CRLF);
            } else {
                scaVulnerabilitiesTableBuilder(body, r);
            }
        });
    }

    public static String getMDBody(ScanResults.XIssue issue, String branch, String fileUrl,
            FlowProperties flowProperties) {
        StringBuilder body = new StringBuilder();

        List<ScanResults.ScaDetails> scaDetails = issue.getScaDetails();
        if (!ScanUtils.empty(scaDetails)) {
            setSCAMDBody(branch, body, scaDetails);

        } else {
            setSASTMDBody(issue, branch, fileUrl, flowProperties, body);
        }

        return body.toString();
    }

    private static void scaSummaryBuilder(StringBuilder body, SCAResults r, ScanRequest request) {
        appendAll(body, MarkDownHelper.getMdHeaderType(3, MarkDownHelper.SCA_SUMMARY_HEADER), CRLF);
        appendAll(body, MarkDownHelper.getBoldText("Total Packages Identified"), ": ", MarkDownHelper.getBoldText(String.valueOf(r.getSummary().getTotalPackages())), CRLF);
        appendAll(body, MarkDownHelper.getBoldText("Scan Risk Score"), ": ", MarkDownHelper.getBoldText(String.format("%.2f", r.getSummary().getRiskScore())), CRLF, CRLF);

        Arrays.asList("High", "Medium", "Low").forEach(v ->
                appendAll(body, MarkDownHelper.getSeverityIconFromLinkByText(v, request), MarkDownHelper.getNonBreakingSpace(request), MarkDownHelper.getBoldText(String.valueOf(r.getSummary().getFindingCounts().get(Severity.valueOf(v.toUpperCase())))),
                        " ", MarkDownHelper.getBoldText(v), " " ,MarkDownHelper.getBoldText("severity vulnerabilities"), CRLF));

        appendAll(body, MarkDownHelper.getTextLink(MarkDownHelper.MORE_DETAILS_LINK_HEADER, r.getWebReportLink()), CRLF);
    }

    private static void scaVulnerabilitiesTableBuilder(StringBuilder body, SCAResults r) {
        MarkDownHelper.appendMDtableHeaders(body, "Vulnerability ID", "Package", SEVERITY,
                // "CWE / Category",
                "CVSS score", "Publish date", "Current version", "Recommended version", "Link in CxSCA",
                "Reference – NVD link");

        r.getFindings().stream().sorted(Comparator.comparingDouble(o -> -o.getScore()))
                .sorted(Comparator.comparingInt(o -> -o.getSeverity().ordinal())).forEach(
                f -> MarkDownHelper.appendMDtableRow(body, '`' + f.getId() + '`', extractPackageNameFromFindings(r, f),
                        f.getSeverity().name(),
                        // "N\\A",
                        String.valueOf(f.getScore()), f.getPublishDate(),
                        extractPackageVersionFromFindings(r, f),
                        Optional.ofNullable(f.getRecommendations()).orElse(""),
                        " [Vulnerability Link]("
                                + ScanUtils.constructVulnerabilityUrl(r.getWebReportLink(), f) + ")",
                        (StringUtils.isEmpty(f.getCveName())) ? "N\\A"
                                : appendAll(new StringBuilder(), '[', f.getCveName(),
                                "](https://nvd.nist.gov/vuln/detail/", f.getCveName(), ")")
                                .toString()));
    }

    private static String extractPackageNameFromFindings(SCAResults r, Finding f) {
        return r.getPackages().stream().filter(p -> p.getId().equals(f.getPackageId())).map(Package::getName)
                .findFirst().orElse("");
    }

    private static String extractPackageVersionFromFindings(SCAResults r, Finding f) {
        try {
            return r.getPackages().stream().filter(p -> p.getId().equals(f.getPackageId())).map(Package::getVersion)
                    .findFirst().orElse("");
        }
        catch (Exception e) {
            log.warn("failed to extract package version from SCA finding - {}", f.toString());
            return "EMPTY";
        }
    }

    private static void setSCAMDBody(String branch, StringBuilder body, List<ScanResults.ScaDetails> scaDetails) {
        log.debug("Building MD body for SCA scanner");
        scaDetails.stream().findAny().ifPresent(any -> {
            appendAll(body, "**Description**", CRLF, CRLF);
            appendAll(body, any.getFinding().getDescription(), CRLF, CRLF);
            appendAll(body, String.format(SCATicketingConstants.SCA_CUSTOM_ISSUE_BODY, any.getFinding().getSeverity(),
                    any.getVulnerabilityPackage().getName(), branch), CRLF, CRLF);

            Map<String, String> scaDetailsMap = new LinkedHashMap<>();
            scaDetailsMap.put("**Vulnerability ID", any.getFinding().getId());
            scaDetailsMap.put("**Package Name", any.getVulnerabilityPackage().getName());
            scaDetailsMap.put("**Severity", any.getFinding().getSeverity().name());
            scaDetailsMap.put("**CVSS Score", String.valueOf(any.getFinding().getScore()));
            scaDetailsMap.put("**Publish Date", any.getFinding().getPublishDate());
            scaDetailsMap.put("**Current Package Version", any.getVulnerabilityPackage().getVersion());
            Optional.ofNullable(any.getFinding().getFixResolutionText())
                    .ifPresent(f -> scaDetailsMap.put("**Remediation Upgrade Recommendation", f)

                    );

            scaDetailsMap.forEach((key, value) -> appendAll(body, key, ":** ", value, CRLF, CRLF));

            String findingLink = ScanUtils.constructVulnerabilityUrl(any.getVulnerabilityLink(), any.getFinding());
            appendAll(body, "[Link To SCA](", findingLink, ")", CRLF, CRLF);

            String cveName = any.getFinding().getCveName();
            if (!ScanUtils.empty(cveName)) {
                appendAll(body, "[Reference – NVD link](", NVD_URL_PREFIX, cveName, ")", CRLF, CRLF);
            }
        });
    }

    private static void setSASTHtmlBody(ScanResults.XIssue issue, FlowProperties flowProperties, String branch,
            StringBuilder body) {
        appendAll(body, String.format(ISSUE_BODY, issue.getVulnerability(), issue.getFilename(), branch), CRLF);

        if (!ScanUtils.empty(issue.getDescription())) {
            appendAll(body, ITALIC_OPENING_DIV, issue.getDescription().trim(), ITALIC_CLOSING_DIV);
        }
        body.append(CRLF);
        if (!ScanUtils.empty(issue.getSeverity())) {
            appendAll(body, "<div><b>Severity:</b> ", issue.getSeverity(), DIV_CLOSING_TAG);
        }
        if (!ScanUtils.empty(issue.getCwe())) {
            appendAll(body, "<div><b>CWE:</b>", issue.getCwe(), DIV_CLOSING_TAG);
            if (!ScanUtils.empty(flowProperties.getMitreUrl())) {
                appendAll(body, DIV_A_HREF, String.format(flowProperties.getMitreUrl(), issue.getCwe()),
                        "'>Vulnerability details and guidance</a></div>");
            }
        }
        if (!ScanUtils.empty(flowProperties.getWikiUrl())) {
            appendAll(body, DIV_A_HREF, flowProperties.getWikiUrl(), "'>Internal Guidance</a></div>");
        }
        if (!ScanUtils.empty(issue.getLink())) {
            appendAll(body, DIV_A_HREF, issue.getLink(), "'>Checkmarx</a></div>");
        }
        Map<String, Object> additionalDetails = issue.getAdditionalDetails();
        if (MapUtils.isNotEmpty(additionalDetails) && additionalDetails.containsKey(ScanUtils.RECOMMENDED_FIX)) {
            appendAll(body, DIV_A_HREF, additionalDetails.get(ScanUtils.RECOMMENDED_FIX),
                    "'>Recommended Fix</a></div>");
        }

        appendsSastAstDetails(issue, flowProperties, body);
        appendOsaDetailsHTML(issue, body);
    }

    private static void appendsSastAstDetails(ScanResults.XIssue issue, FlowProperties flowProperties,
            StringBuilder body) {
        if (issue.getDetails() != null && !issue.getDetails().isEmpty()) {
            Map<Integer, ScanResults.IssueDetails> trueIssues = issue.getDetails().entrySet().stream()
                    .filter(x -> x.getKey() != null && x.getValue() != null && !x.getValue().isFalsePositive())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            Map<Integer, ScanResults.IssueDetails> fpIssues = issue.getDetails().entrySet().stream()
                    .filter(x -> x.getKey() != null && x.getValue() != null && x.getValue().isFalsePositive())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            appendLinesHTML(body, trueIssues);
            appendNotExploitableHTML(flowProperties, body, fpIssues);
            appendCodeSnippetHTML(body, trueIssues);
            body.append("<hr/>");
        }
    }

    private static void appendOsaDetailsHTML(ScanResults.XIssue issue, StringBuilder body) {
        if (issue.getOsaDetails() != null) {
            for (ScanResults.OsaDetails o : issue.getOsaDetails()) {
                body.append(CRLF);
                if (!ScanUtils.empty(o.getCve())) {
                    body.append("<b>").append(o.getCve()).append("</b>").append(CRLF);
                }
                body.append("<pre><code><div>");
                appendOsaDetails(body, o);
                body.append("</div></code></pre><div>");
                body.append(CRLF);
            }
        }
    }

    private static void appendCodeSnippetHTML(StringBuilder body, Map<Integer, ScanResults.IssueDetails> trueIssues) {
        for (Map.Entry<Integer, ScanResults.IssueDetails> entry : trueIssues.entrySet()) {
            if (!ScanUtils.empty(entry.getValue().getCodeSnippet())) {
                body.append("<hr/>");
                body.append("<b>Line #").append(entry.getKey()).append("</b>");
                body.append("<pre><code><div>");
                String codeSnippet = entry.getValue().getCodeSnippet();
                body.append(StringEscapeUtils.escapeHtml4(codeSnippet));
                body.append("</div></code></pre><div>");
            }
        }
    }

    private static void appendLinesHTML(StringBuilder body, Map<Integer, ScanResults.IssueDetails> trueIssues) {
        if (!trueIssues.isEmpty()) {
            body.append("<div><b>Lines: </b>");
            for (Map.Entry<Integer, ScanResults.IssueDetails> entry : trueIssues.entrySet()) {
                body.append(entry.getKey()).append(" ");
            }
            body.append(DIV_CLOSING_TAG);
        }
    }

    private static void appendNotExploitableHTML(FlowProperties flowProperties, StringBuilder body,
            Map<Integer, ScanResults.IssueDetails> fpIssues) {
        if (flowProperties.isListFalsePositives() && !fpIssues.isEmpty()) {// List the false positives / not exploitable
            body.append("<div><b>Lines Marked Not Exploitable: </b>");
            for (Map.Entry<Integer, ScanResults.IssueDetails> entry : fpIssues.entrySet()) {
                body.append(entry.getKey()).append(" ");
            }
            body.append(DIV_CLOSING_TAG);
        }
    }

    private static void setSCAHtmlBody(ScanResults.XIssue issue, ScanRequest request, StringBuilder body) {
        log.debug("Building HTML body for SCA scanner");
        issue.getScaDetails().stream().findAny().ifPresent(any -> {
            body.append(ITALIC_OPENING_DIV).append(any.getFinding().getDescription()).append(ITALIC_CLOSING_DIV)
                    .append(MarkDownHelper.getLineBreak(request));
            body.append(String.format(SCATicketingConstants.SCA_HTML_ISSUE_BODY, any.getFinding().getSeverity(),
                    any.getVulnerabilityPackage().getName(), request.getBranch())).append(DIV_CLOSING_TAG)
                    .append(MarkDownHelper.getLineBreak(request));
        });

        Map<String, String> scaDetailsMap = new LinkedHashMap<>();
        issue.getScaDetails().stream().findAny().ifPresent(any -> {
            scaDetailsMap.put("<b>Vulnerability ID", any.getFinding().getId());
            scaDetailsMap.put("<b>Package Name", any.getVulnerabilityPackage().getName());
            scaDetailsMap.put("<b>Severity", any.getFinding().getSeverity().name());
            scaDetailsMap.put("<b>CVSS Score", String.valueOf(any.getFinding().getScore()));
            scaDetailsMap.put("<b>Publish Date", any.getFinding().getPublishDate());
            scaDetailsMap.put("<b>Current Package Version", any.getVulnerabilityPackage().getVersion());
            Optional.ofNullable(any.getFinding().getFixResolutionText())
                    .ifPresent(f -> scaDetailsMap.put("<b>Remediation Upgrade Recommendation", f)

                    );

            scaDetailsMap.forEach((key, value) -> body.append(key).append(":</b> ").append(value).append(MarkDownHelper.getLineBreak(request)));

            String findingLink = ScanUtils.constructVulnerabilityUrl(any.getVulnerabilityLink(), any.getFinding());
            body.append(DIV_A_HREF).append(findingLink).append("'>Link To SCA</a></div>");

            String cveName = any.getFinding().getCveName();
            if (!ScanUtils.empty(cveName)) {
                body.append(DIV_A_HREF).append(NVD_URL_PREFIX).append(cveName)
                        .append("'>Reference – NVD link</a></div>");
            }
        });
    }

    private static void setSASTMDBody(ScanResults.XIssue issue, String branch, String fileUrl,
            FlowProperties flowProperties, StringBuilder body) {
        log.debug("Building MD body for SAST scanner");
        body.append(String.format(ISSUE_BODY, issue.getVulnerability(), issue.getFilename(), branch)).append(CRLF)
                .append(CRLF);
        if (!ScanUtils.empty(issue.getDescription())) {
            body.append("*").append(issue.getDescription().trim()).append("*").append(CRLF).append(CRLF);
        }
        if (!ScanUtils.empty(issue.getSeverity())) {
            body.append(SEVERITY).append(": ").append(issue.getSeverity()).append(CRLF).append(CRLF);
        }
        if (!ScanUtils.empty(issue.getCwe())) {
            body.append("CWE:").append(issue.getCwe()).append(CRLF).append(CRLF);
            if (!ScanUtils.empty(flowProperties.getMitreUrl())) {
                body.append("[Vulnerability details and guidance](")
                        .append(String.format(flowProperties.getMitreUrl(), issue.getCwe())).append(")").append(CRLF)
                        .append(CRLF);
            }
        }
        if (!ScanUtils.empty(flowProperties.getWikiUrl())) {
            body.append("[Internal Guidance](").append(flowProperties.getWikiUrl()).append(")").append(CRLF)
                    .append(CRLF);
        }
        if (!ScanUtils.empty(issue.getLink())) {
            body.append("[Checkmarx](").append(issue.getLink()).append(")").append(CRLF).append(CRLF);
        }

        if (!ScanUtils.empty(flowProperties.getCodebashUrl())) {
            appendAll(body, "[Training](", flowProperties.getCodebashUrl(), ")", HTMLHelper.CRLF);
        }

        Map<String, Object> additionalDetails = issue.getAdditionalDetails();
        if (MapUtils.isNotEmpty(additionalDetails) && additionalDetails.containsKey(RECOMMENDED_FIX)) {
            body.append("[Recommended Fix](").append(additionalDetails.get(ScanUtils.RECOMMENDED_FIX)).append(")")
                    .append(CRLF).append(CRLF);
        }

        appendSastAstDetails(issue, fileUrl, flowProperties, body);
        appendOsaDetails(issue, body);
    }

    private static void appendSastAstDetails(ScanResults.XIssue issue, String fileUrl, FlowProperties flowProperties,
            StringBuilder body) {
        if (issue.getDetails() != null && !issue.getDetails().isEmpty()) {
            Map<Integer, ScanResults.IssueDetails> trueIssues = issue.getDetails().entrySet().stream()
                    .filter(x -> x.getKey() != null && x.getValue() != null && !x.getValue().isFalsePositive())
                    .sorted(comparingByKey()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            Map<Integer, ScanResults.IssueDetails> fpIssues = issue.getDetails().entrySet().stream()
                    .filter(x -> x.getKey() != null && x.getValue() != null && x.getValue().isFalsePositive())
                    .sorted(comparingByKey()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            appendLines(fileUrl, body, trueIssues);
            appendNotExploitable(fileUrl, flowProperties, body, fpIssues);
            appendCodeSnippet(fileUrl, body, trueIssues);
            appendAll(body, "---", CRLF);
        }
    }

    private static void appendLines(String fileUrl, StringBuilder body,
            Map<Integer, ScanResults.IssueDetails> trueIssues) {
        if (!trueIssues.isEmpty()) {
            body.append("Lines: ");
            for (Map.Entry<Integer, ScanResults.IssueDetails> entry : trueIssues.entrySet()) {
                if (fileUrl != null) { // [<line>](<url>)
                    appendAll(body, "[", entry.getKey(), "](", fileUrl, "#L", entry.getKey(), ") ");
                } else { // if the fileUrl is not provided, simply putting the line number (no link) -
                         // ADO for example
                    appendAll(body, entry.getKey(), " ");
                }
            }
            appendAll(body, CRLF, CRLF);
        }
    }

    private static void appendCodeSnippet(String fileUrl, StringBuilder body,
            Map<Integer, ScanResults.IssueDetails> trueIssues) {
        for (Map.Entry<Integer, ScanResults.IssueDetails> entry : trueIssues.entrySet()) {
            if (entry.getValue() != null && entry.getValue().getCodeSnippet() != null) {
                appendAll(body, "---", CRLF);
                appendAll(body, "[Code (Line #", entry.getKey(), "):](", fileUrl, "#L", entry.getKey(), ")", CRLF);
                appendAll(body, "```", CRLF);
                appendAll(body, entry.getValue().getCodeSnippet(), CRLF);
                appendAll(body, "```", CRLF);
            }
        }
    }

    private static void appendNotExploitable(String fileUrl, FlowProperties flowProperties, StringBuilder body,
            Map<Integer, ScanResults.IssueDetails> fpIssues) {
        if (flowProperties.isListFalsePositives() && !fpIssues.isEmpty()) {// List the false positives / not exploitable
            body.append(CRLF);
            body.append("Lines Marked Not Exploitable: ");
            for (Map.Entry<Integer, ScanResults.IssueDetails> entry : fpIssues.entrySet()) {
                if (fileUrl != null) { // [<line>](<url>)
                    appendAll(body, "[", entry.getKey(), "](", fileUrl, "#L", entry.getKey(), ") ");
                } else { // if the fileUrl is not provided, simply putting the line number (no link) -
                         // ADO for example
                    appendAll(body, entry.getKey(), " ");
                }
            }
            body.append(CRLF).append(CRLF);
        }
    }

    private static void appendOsaDetails(ScanResults.XIssue issue, StringBuilder body) {
        if (issue.getOsaDetails() != null) {
            for (ScanResults.OsaDetails o : issue.getOsaDetails()) {
                body.append(CRLF);
                if (!ScanUtils.empty(o.getCve())) {
                    appendAll(body, "*", o.getCve(), "*", CRLF);
                }
                body.append("```");
                appendOsaDetails(body, o);
                body.append("```");
                body.append(CRLF);
            }
        }
    }

    /**
     * = Generates an Text message describing the discovered issue.
     *
     * @param issue The issue to add the comment too
     * @return string with the HTML message
     */
    public static String getTextBody(ScanResults.XIssue issue, ScanRequest request, FlowProperties flowProperties) {
        String branch = request.getBranch();
        StringBuilder body = new StringBuilder();
        appendAll(body, String.format(ISSUE_BODY_TEXT, issue.getVulnerability(), issue.getFilename(), branch), CRLF);
        if (!ScanUtils.empty(issue.getDescription())) {
            body.append(issue.getDescription().trim());
        }
        body.append(CRLF);
        if (!ScanUtils.empty(issue.getSeverity())) {
            appendAll(body, SEVERITY,  ": ", issue.getSeverity(), CRLF);
        }
        appendCWE(issue, flowProperties, body);

        if (!ScanUtils.empty(flowProperties.getWikiUrl())) {
            appendAll(body, DETAILS, flowProperties.getWikiUrl(), " - Internal Guidance ", CRLF);
        }
        if (!ScanUtils.empty(issue.getLink())) {
            appendAll(body, DETAILS, issue.getLink(), " - Checkmarx", CRLF);
        }
        Map<String, Object> additionalDetails = issue.getAdditionalDetails();
        if (MapUtils.isNotEmpty(additionalDetails) && additionalDetails.containsKey(ScanUtils.RECOMMENDED_FIX)) {
            appendAll(body, DETAILS, additionalDetails.get(ScanUtils.RECOMMENDED_FIX), " - Recommended Fix", CRLF);
        }

        appendSastAstDetials(issue, flowProperties, body);

        if (issue.getOsaDetails() != null) {
            for (ScanResults.OsaDetails o : issue.getOsaDetails()) {
                body.append(CRLF);
                if (!ScanUtils.empty(o.getCve())) {
                    body.append(o.getCve()).append(CRLF);
                }
                appendOsaDetails(body, o);
                body.append(CRLF);
            }
        }
        return body.toString();
    }

    private static void appendCWE(ScanResults.XIssue issue, FlowProperties flowProperties, StringBuilder body) {
        if (!ScanUtils.empty(issue.getCwe())) {
            body.append("CWE: ").append(issue.getCwe()).append(CRLF);
            if (!ScanUtils.empty(flowProperties.getMitreUrl())) {
                body.append(DETAILS).append(String.format(flowProperties.getMitreUrl(), issue.getCwe()))
                        .append(" - Vulnerability details and guidance").append(CRLF);
            }
        }
    }

    private static void appendSastAstDetials(ScanResults.XIssue issue, FlowProperties flowProperties,
            StringBuilder body) {
        if (issue.getDetails() != null && !issue.getDetails().isEmpty()) {
            Map<Integer, ScanResults.IssueDetails> trueIssues = issue.getDetails().entrySet().stream()
                    .filter(x -> x.getKey() != null && x.getValue() != null && !x.getValue().isFalsePositive())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            Map<Integer, ScanResults.IssueDetails> fpIssues = issue.getDetails().entrySet().stream()
                    .filter(x -> x.getKey() != null && x.getValue() != null && x.getValue().isFalsePositive())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            if (!trueIssues.isEmpty()) {
                body.append("Lines: ");
                trueIssues.keySet().forEach(key -> body.append(key).append(" "));
            }
            if (flowProperties.isListFalsePositives() && !fpIssues.isEmpty()) {// List the false positives / not
                                                                               // exploitable
                body.append("Lines Marked Not Exploitable: ");
                fpIssues.keySet().forEach(key -> body.append(key).append(" "));
            }
            for (Map.Entry<Integer, ScanResults.IssueDetails> entry : trueIssues.entrySet()) {
                if (!ScanUtils.empty(entry.getValue().getCodeSnippet())) {
                    appendAll(body, "Line # ", entry.getKey());
                    String codeSnippet = entry.getValue().getCodeSnippet();
                    appendAll(body, StringEscapeUtils.escapeHtml4(codeSnippet), CRLF);
                }
            }
        }
    }

    private static void appendOsaDetails(StringBuilder body, ScanResults.OsaDetails o) {
        BiConsumer<String, Object> addIfPresent = (name, field) -> {
            if (!ScanUtils.empty(o.getSeverity())) {
                appendAll(body, name, field, CRLF);
            }
        };

        addIfPresent.accept("Severity: ", o.getSeverity());
        addIfPresent.accept(VERSION, o.getVersion());
        addIfPresent.accept(DESCRIPTION, o.getDescription());
        addIfPresent.accept(RECOMMENDATION, o.getRecommendation());
        addIfPresent.accept(URL, o.getUrl());

    }
    
    private static void addSastAstDetailsBody(ScanRequest request, StringBuilder body, Map<String, ScanResults.XIssue> xMap, Comparator<ScanResults.XIssue> issueComparator) {
        xMap.entrySet().stream()
                .filter(x -> x.getValue() != null && x.getValue().getDetails() != null)
                .sorted(Map.Entry.comparingByValue(issueComparator.reversed()))
                .forEach(xIssue -> {
                    ScanResults.XIssue currentIssue = xIssue.getValue();
                    String fileUrl = ScanUtils.getFileUrl(request, currentIssue.getFilename());
                    currentIssue.getDetails().entrySet().stream()
                            .filter(x -> x.getKey() != null && x.getValue() != null && !x.getValue().isFalsePositive())
                            .sorted(Map.Entry.comparingByKey())
                            .forEach(entry -> {
                                //[<line>](<url>)
                                //Azure DevOps direct repo line url is unknown at this time.
                                if (request.getRepoType().equals(ScanRequest.Repository.ADO)) {
                                    appendAll(body, entry.getKey(), " ");
                                } else {
                                    appendAll(body, "[", entry.getKey(), "](", fileUrl);
                                    if (request.getRepoType().equals(ScanRequest.Repository.BITBUCKET)) {
                                        appendAll(body, "#lines-", entry.getKey(), ") ");
                                    } else if (request.getRepoType().equals(ScanRequest.Repository.BITBUCKETSERVER)) {
                                        appendAll(body, "#", entry.getKey(), ") ");
                                    } else {
                                        appendAll(body, "#L", entry.getKey(), ") ");
                                    }
                                }
                            });
                    if (currentIssue.getDetails().entrySet().stream().anyMatch(x -> x.getKey() != null && x.getValue() != null && !x.getValue().isFalsePositive())) {
                        body.append("|");
                        body.append(currentIssue.getSeverity()).append("|");
                        body.append(currentIssue.getVulnerability()).append("|");
                        body.append(getEffectiveFileName(currentIssue.getFilename())).append("|");
                        body.append("[Checkmarx](").append(currentIssue.getLink()).append(")");
                        body.append(CRLF);
                    }
                });
    }

    private static String getEffectiveFileName(String fileName) {
        return fileName.startsWith("/")
                ? fileName.substring(1)
                : fileName;
    }

    private static void addOsaDetailesBody(ScanResults results, StringBuilder body, Map<String, ScanResults.XIssue> xMap, Comparator<ScanResults.XIssue> issueComparator) {
        if (results.getOsa() != null && results.getOsa()) {
            log.debug("Building merge comment MD for OSA scanner");
            body.append(CRLF);
            MarkDownHelper.appendMDtableHeaders(body, "Library", SEVERITY, "CVE");

            //OSA
            xMap.entrySet().stream()
                    .filter(x -> x.getValue() != null && x.getValue().getOsaDetails() != null)
                    .sorted(Map.Entry.comparingByValue(issueComparator))
                    .forEach(xIssue -> {
                        ScanResults.XIssue currentIssue = xIssue.getValue();
                        body.append("|");
                        body.append(currentIssue.getFilename()).append("|");
                        body.append(currentIssue.getSeverity()).append("|");
                        for (ScanResults.OsaDetails o : currentIssue.getOsaDetails()) {
                            body.append("[").append(o.getCve()).append("](")
                                    .append("https://cve.mitre.org/cgi-bin/cvename.cgi?name=").append(o.getCve()).append(") ");
                        }
                        body.append("|");
                        body.append(CRLF);
                        //body.append("```").append(currentIssue.getDescription()).append("```").append(CRLF); Description is too long
                    });
        }
    }


    private static void addScanSummarySection(ScanRequest request, ScanResults results, RepoProperties properties, StringBuilder body) {
        setScannerLogoHeader(request, results, body);
        setScannerSummaryHeader(results, body);

        CxScanSummary summary = results.getScanSummary();
        setScannerTotalVulnerabilities(body, summary, request);

        if (properties.isCxSummary() && !request.getProduct().equals(ScanRequest.Product.CXOSA)) {
            if (!ScanUtils.empty(properties.getCxSummaryHeader())) {
                appendAll(body, MarkDownHelper.getMdHeaderType(4, properties.getCxSummaryHeader()), CRLF);
            }
            MarkDownHelper.appendMDtableHeaders(body, SEVERITY, "Count");
            MarkDownHelper.appendMDtableRow(body, "High", summary.getHighSeverity().toString());
            MarkDownHelper.appendMDtableRow(body, "Medium", summary.getMediumSeverity().toString());
            MarkDownHelper.appendMDtableRow(body, "Low", summary.getLowSeverity().toString());
            MarkDownHelper.appendMDtableRow(body, "Informational", summary.getInfoSeverity().toString());
            body.append(CRLF);
        }
    }

    private static void setScannerTotalVulnerabilities(StringBuilder body, CxScanSummary summary, ScanRequest request) {
        appendAll(body, "Total of " + countSastTotalVulnerabilities(summary) + " vulnerabilities", MarkDownHelper.getLineBreak(request));
        appendAll(body, MarkDownHelper.getHighIconFromLink(request), MarkDownHelper.getNonBreakingSpace(request), MarkDownHelper.getBoldText(summary.getHighSeverity() + " High"), MarkDownHelper.getLineBreak(request));
        appendAll(body, MarkDownHelper.getMediumIconFromLink(request), MarkDownHelper.getNonBreakingSpace(request), MarkDownHelper.getBoldText(summary.getMediumSeverity() + " Medium"), MarkDownHelper.getLineBreak(request));
        appendAll(body, MarkDownHelper.getLowIconFromLink(request), MarkDownHelper.getNonBreakingSpace(request), MarkDownHelper.getBoldText(summary.getLowSeverity() + " Low"), MarkDownHelper.getLineBreak(request));
        appendAll(body, MarkDownHelper.getInfoIconFromLink(request), MarkDownHelper.getNonBreakingSpace(request), MarkDownHelper.getBoldText(summary.getInfoSeverity() + " Info"), MarkDownHelper.getLineBreak(request), CRLF);
    }

    private static String countSastTotalVulnerabilities(CxScanSummary summary) {
        int totalVulnerabilities = 0;

        totalVulnerabilities += Optional.ofNullable(summary.getHighSeverity()).orElse(0);
        totalVulnerabilities += Optional.ofNullable(summary.getMediumSeverity()).orElse(0);
        totalVulnerabilities += Optional.ofNullable(summary.getLowSeverity()).orElse(0);
        totalVulnerabilities += Optional.ofNullable(summary.getInfoSeverity()).orElse(0);

        return String.valueOf(totalVulnerabilities);
    }

    private static void addDetailsSection(ScanRequest request, ScanResults results, RepoProperties properties, StringBuilder body) {
        if (properties.isDetailed()) {
            Map<String, ScanResults.XIssue> xMap;
            xMap = ScanUtils.getXIssueMap(results.getXIssues(), request);

            if (xMap.size() > 0) {
                setScannerDetailsHeader(results, body);
                MarkDownHelper.appendMDtableHeaders(body,"Lines",SEVERITY,"Category","File","Link");
                log.info("Creating Merge/Pull Request Markdown comment");

                Comparator<ScanResults.XIssue> issueComparator = Comparator
                        .comparing(ScanResults.XIssue::getSeverity)
                        .thenComparing(ScanResults.XIssue::getVulnerability);

                addSastAstDetailsBody(request, body, xMap, issueComparator);

                addOsaDetailesBody(results, body, xMap, issueComparator);
            }
        }
    }

    private static void setScannerLogoHeader(ScanRequest request, ScanResults results, StringBuilder body) {
        if (Optional.ofNullable(results.getAstResults()).isPresent()) {
            appendAll(body, MarkDownHelper.getCheckmarxLogoFromLink(request), CRLF, MarkDownHelper.getAstBoldHeader(), CRLF);
        } else {
            appendAll(body, MarkDownHelper.getCheckmarxLogoFromLink(request), MarkDownHelper.getLineBreak(request), MarkDownHelper.getSastHeader(), CRLF);
        }
    }

    private static void setScannerSummaryHeader(ScanResults results, StringBuilder body) {
        if (Optional.ofNullable(results.getAstResults()).isPresent()) {
            appendAll(body, MarkDownHelper.getMdHeaderType(3, MarkDownHelper.AST_SUMMARY_HEADER), CRLF);
        } else {
            appendAll(body, MarkDownHelper.getMdHeaderType(3, MarkDownHelper.SAST_SUMMARY_HEADER), CRLF);
        }
    }

    private static void setScannerDetailsHeader(ScanResults results, StringBuilder body) {
        if (Optional.ofNullable(results.getAstResults()).isPresent()) {
            appendAll(body, MarkDownHelper.getMdHeaderType(3, MarkDownHelper.AST_DETAILS_HEADER), CRLF);
        } else {
            appendAll(body, MarkDownHelper.getMdHeaderType(3, MarkDownHelper.SAST_DETAILS_HEADER), CRLF);
        }
    }

    private static StringBuilder appendAll(StringBuilder sb, Object... parts) {
        Stream.of(parts).forEach(sb::append);
        return sb;
    }

}
