package com.checkmarx.flow.utils;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaRuntimeException;

import java.util.Arrays;
import java.util.stream.Collectors;

public class MarkDownHelper {

    private static final String SAST_SCANNER = "SAST";
    private static final String AST_SAST_SCANNER = "AST-" + SAST_SCANNER;
    private static final String SCA_SCANNER = "SCA";

    public static final String SCAN_SUMMARY_DETAILS = "Scan Summary & Details";
    private static final String CX_PREFIX = "Cx-";
    public static final String AST_DETAILS_HEADER = CX_PREFIX + AST_SAST_SCANNER + " Details";

    static final String MORE_DETAILS_LINK_HEADER = "View more details on Checkmarx UI";
    static final String SAST_DETAILS_HEADER = CX_PREFIX + SAST_SCANNER + " Details";

    private static final String SUMMARY_SUFFIX = " Summary";
    public static final String SAST_SUMMARY_HEADER = CX_PREFIX + SAST_SCANNER + SUMMARY_SUFFIX;
    static final String AST_SUMMARY_HEADER = CX_PREFIX + AST_SAST_SCANNER + SUMMARY_SUFFIX;
    static final String SCA_SUMMARY_HEADER = CX_PREFIX + SCA_SCANNER + SUMMARY_SUFFIX;

    private static final String NBSP = "&nbsp;";
    private static final String LINE_BREAK = "<br />";
    private static final String IMAGE_TEMPLATE = "![%s](%s)";
    private static final String BOLD_TEMPLATE = "**%s**";
    private static final String LINK_TEMPLATE = "[%s](%s)";

    private static final String GITHUB_USER_PREFIX = "https://user-images.githubusercontent.com/23239410/";
    private static final String CHECKMARX_LOGO_URL = GITHUB_USER_PREFIX + "92153465-ff743900-ee2c-11ea-9c8d-8141e38feb41.png";
    private static final String HIGH_ICON = GITHUB_USER_PREFIX + "92157087-97285600-ee32-11ea-988f-0aca12c4c126.png";
    private static final String MEDIUM_ICON = GITHUB_USER_PREFIX + "92157093-98598300-ee32-11ea-83d7-af52251a011b.png";
    private static final String LOW_ICON = GITHUB_USER_PREFIX + "92157091-98598300-ee32-11ea-8498-19bd7d62019b.png";
    private static final String INFO_ICON = GITHUB_USER_PREFIX + "92157090-97c0ec80-ee32-11ea-9b2e-aa6b32b03d54.png";
    private static final String ICON_ICON = GITHUB_USER_PREFIX + "92355607-3d06e980-f0ed-11ea-8bb7-9029eb8716b9.png";

    private static final String CHECKMARX_PREFIX = "Checkmarx ";
    public static final String SAST_HEADER = CHECKMARX_PREFIX + SAST_SCANNER + " - " + SCAN_SUMMARY_DETAILS;
    public static final String SCA_HEADER = CHECKMARX_PREFIX + SCA_SCANNER + " - " + SCAN_SUMMARY_DETAILS;
    private static final String AST_SAST_HEADER = CHECKMARX_PREFIX + AST_SAST_SCANNER + " - " + SCAN_SUMMARY_DETAILS;


    private MarkDownHelper() {
    }

    /**
     *
     * @param headerTypeNumber Header type size. Must be between 3-6. In case of number out of range, default '3' will be returned
     * @param text             The header text
     * @return  A string reflect the new header type text
     */
    static String getMdHeaderType(int headerTypeNumber, String text) {
        StringBuilder builder = new StringBuilder("######");
        builder.setLength(headerTypeNumber);
        return builder.append(" ").append(text).toString();
    }

    static String getCheckmarxLogoFromLink(ScanRequest request) {
        return request.getRepoType() == ScanRequest.Repository.BITBUCKET
                ? getMdHeaderType(1, "Checkmarx")
                : getImageFromLink("Logo", CHECKMARX_LOGO_URL, request);
    }

    static String getNonBreakingSpace(ScanRequest scanRequest) {
        String nbsp;

        switch (scanRequest.getRepoType()) {
            case BITBUCKET:
                nbsp = " ";
                break;
            case GITHUB:
            case GITLAB:
            case ADO:
                nbsp = NBSP;
                break;
            default:
                nbsp = NBSP;
                break;
        }
        return nbsp;
    }

    static String getLineBreak(ScanRequest scanRequest) {
        String lineBreak;

        switch (scanRequest.getRepoType()) {
            case BITBUCKET:
                lineBreak = "  " + HTMLHelper.CRLF;
                break;
            case GITHUB:
            case GITLAB:
            case ADO:
                lineBreak = LINE_BREAK;
                break;
            default:
                lineBreak = HTMLHelper.CRLF + HTMLHelper.CRLF;
                break;

        }
        return lineBreak;
    }

    static String getHighIconFromLink(ScanRequest request) {
        return getImageFromLink("High", HIGH_ICON, request);
    }

    static String getMediumIconFromLink(ScanRequest request) {
        return getImageFromLink("Medium", MEDIUM_ICON, request);
    }

    static String getLowIconFromLink(ScanRequest request) {
        return getImageFromLink("Low", LOW_ICON, request);
    }

    static String getInfoIconFromLink(ScanRequest request) {
        return getImageFromLink("Info", INFO_ICON, request);
    }

    static String getIconFromLink(ScanRequest request) {
        return getImageFromLink("Icon", ICON_ICON, request);
    }

    static String getSastHeader() {
        return getBoldText(SAST_HEADER);
    }

    static String getAstBoldHeader() {
        return getBoldText(AST_SAST_HEADER);
    }

    static String getScaHeader() {
        return getBoldText(SCA_HEADER);
    }

    static String getBoldText(String text) {
        return String.format(BOLD_TEMPLATE, text);
    }

    static String getTextLink(String text, String link) {
        return String.format(LINK_TEMPLATE, text, link);
    }

    static String getSeverityIconFromLinkByText(String severity, ScanRequest request) {
        switch (severity) {
            case "High":
            case "HIGH":
                return getHighIconFromLink(request);
            case "Medium":
            case "MEDIUM":
                return getMediumIconFromLink(request);
            case "Low":
            case "LOW":    
                return getLowIconFromLink(request);
            case "Information":
            case "INFO":
                return getInfoIconFromLink(request);
            default:
                throw new MachinaRuntimeException(severity + " is not a valid severity");
        }
    }

    private static String getImageFromLink(String text, String url, ScanRequest request) {
        return request.getRepoType() == ScanRequest.Repository.BITBUCKET
                ? ""
                : String.format(IMAGE_TEMPLATE, text, url);
    }

    static void appendMDtableRow(StringBuilder sb, String... data) {
        sb.append(String.join("|", data)).append(HTMLHelper.CRLF);
    }

    static void appendMDtableHeaders(StringBuilder sb, String... hedears) {
        sb.append(Arrays.stream(hedears).collect(Collectors.joining("|","|","|"))).append(HTMLHelper.CRLF);
        sb.append(Arrays.stream(hedears).map(h -> "---").collect(Collectors.joining("|"))).append(HTMLHelper.CRLF);
    }
}