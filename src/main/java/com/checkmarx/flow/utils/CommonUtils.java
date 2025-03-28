package com.checkmarx.flow.utils;

public class CommonUtils {
    public static String parseCommand(String comment) {
        comment = comment.trim().toLowerCase();
        // todo : inline space can be trimmed to single space
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
