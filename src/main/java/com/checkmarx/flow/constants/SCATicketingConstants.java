package com.checkmarx.flow.constants;

public class SCATicketingConstants {

    private SCATicketingConstants() {
    }

    public static final String SCA_HTML_ISSUE_BODY = "<div><b>%s Vulnerable Package</b> issue exists @ <b>%s</b> in branch <b>%s</b>";

    public static final String SCA_SUMMARY_CUSTOM_ISSUE_KEY = "%s %.1f: %s in %s and %s @ %s.%s";
    public static final String SCA_CUSTOM_ISSUE_BODY = "**%s Vulnerable Package** issue exists @ **%s** in branch **%s**";

    public static final String SCA_JIRA_ISSUE_KEY = "%s%s %.1f: %s in %s and %s @ %s.%s%s";
    public static final String SCA_JIRA_ISSUE_KEY_WITHOUT_BRANCH = "%s%s %.1f: %s in %s and %s @ %s%s";
    public static final String SCA_JIRA_ISSUE_BODY = "*%s Vulnerable Package* issue exists @ *%s* in branch *%s*";
    public static final String SCA_JIRA_ISSUE_BODY_WITHOUT_BRANCH = "*%s Vulnerable Package* issue exists @ *%s*";
}