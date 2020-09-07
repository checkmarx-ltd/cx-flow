package com.checkmarx.flow.utils;

import com.checkmarx.flow.exception.MachinaRuntimeException;

public class MarkDownHelper {

    public static final String MORE_DETAILS_LINK_HEADER = "View more details on Checkmarx UI";
    public static final String SCAN_SUMMARY_DETAILS = "Scan Summary & Details";

    static final String MD_H3 = "### ";
    static final String MD_H4 = "#### ";
    static final String SAST_DETAILS_HEADER = "SAST Details";
    static final String AST_DETAILS_HEADER = "AST-SAST Details";
    static final String SCA_DETAILS_HEADER = "SCA-SAST Details";

    static final String SAST_SUMMARY_HEADER = "SAST Summary";
    static final String AST_SUMMARY_HEADER = "AST Summary";
    static final String SCA_SUMMARY_HEADER = "SCA Summary";

    static final String LINE_BREAK = "<br>";
    static final String NBSP = "&nbsp;";

    private static final String IMAGE_TEMPLATE = "![%s](%s)";
    private static final String BOLD_TEMPLATE = "**%s**";
    private static final String LINK_TEMPLATE = "[%s](%s)";

    private static final String CHECKMARX_LOGO_URL = "https://user-images.githubusercontent.com/23239410/92153465-ff743900-ee2c-11ea-9c8d-8141e38feb41.png";
    private static final String HIGH_ICON = "https://user-images.githubusercontent.com/23239410/92157087-97285600-ee32-11ea-988f-0aca12c4c126.png";
    private static final String MEDIUM_ICON = "https://user-images.githubusercontent.com/23239410/92157093-98598300-ee32-11ea-83d7-af52251a011b.png";
    private static final String LOW_ICON = "https://user-images.githubusercontent.com/23239410/92157091-98598300-ee32-11ea-8498-19bd7d62019b.png";
    private static final String INFO_ICON = "https://user-images.githubusercontent.com/23239410/92157090-97c0ec80-ee32-11ea-9b2e-aa6b32b03d54.png";
    private static final String ICON_ICON = "https://user-images.githubusercontent.com/23239410/92355607-3d06e980-f0ed-11ea-8bb7-9029eb8716b9.png";
    private static final String SAST_BOLD_HEADER = "Checkmarx SAST - " + SCAN_SUMMARY_DETAILS;


    private MarkDownHelper() {
    }

    static String getCheckmarxLogoFromLink() {
        return getImageFromLink("Logo", CHECKMARX_LOGO_URL);
    }

    static String getHighIconFromLink() {
        return getImageFromLink("High", HIGH_ICON);
    }

    static String getMediumIconFromLink() {
        return getImageFromLink("Medium", MEDIUM_ICON);
    }

    static String getLowIconFromLink() {
        return getImageFromLink("Low", LOW_ICON);
    }

    static String getInfoIconFromLink() {
        return getImageFromLink("Info", INFO_ICON);
    }

    static String getIconFromLink() {
        return getImageFromLink("Icon", ICON_ICON);
    }

    static String getSastBoldHeader() {
        return getBoldText(SAST_BOLD_HEADER);
    }

    static String getBoldText(String text) {
        return String.format(BOLD_TEMPLATE, text);
    }

    static String getTextLink(String text, String link) {
        return String.format(LINK_TEMPLATE, text, link);
    }

    static String getSeverityIconFromLinkByText(String severity) {
        switch (severity) {
            case "High":
                return getHighIconFromLink();
            case "Medium":
                return getMediumIconFromLink();
            case "Low":
                return getLowIconFromLink();
            case "Info":
                return getInfoIconFromLink();
            default:
                throw new MachinaRuntimeException(severity + " is not a valid severity");
        }
    }

    private static String getImageFromLink(String text, String url) {
        return String.format(IMAGE_TEMPLATE, text, url);
    }
}