package com.checkmarx.flow.dto.github;

public class CxFlowCommandParser {

    public static String parseCommand(String comment) {
        comment = comment.trim().toLowerCase();
        if (comment.contains("@cxflow status")) {
            return "status";
        } else if (comment.contains("@cxflow rescan")) {
            return "rescan";
        }else if (comment.contains("@cxflow hi")) {
            return "hi";
        }else if (comment.contains("@cxflow cancel")) {
            return "cancel";
        } else {
            return "unsupported";
        }
    }
}

