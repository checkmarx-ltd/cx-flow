package com.checkmarx.flow.utils;

import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.util.UriEncoder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringEscapeUtils {

    private static Pattern jsBlackListedPattern = Pattern.compile("[<>&()]");

    public static String escapeHtml4(String str){
        if (!(StringUtils.isEmpty(str)) && StringUtils.containsIgnoreCase(str, "<script>")) {
            throw new IllegalArgumentException("Illegal string: <script> in the input field: " + str);
        }
        return str;
    }

    /**
     * This method validates string for cross site scripting
     * Checks for characters <,>,&,(,)
     * @param str - input string
     * @return
     * @throws IllegalArgumentException
     */
    public static String escapeHtml4andJS(String str){
        if (!(StringUtils.isEmpty(str)) && StringUtils.containsIgnoreCase(str, "<script>")) {
            str = str.replace("<script>", UriEncoder.encode("<script>"));
        }
        if (str != null && !(StringUtils.isEmpty(str))) {
            Matcher matcher = jsBlackListedPattern.matcher(str);
            if (matcher.find()) {
                str = str.replaceAll("<",UriEncoder.encode("<"))
                        .replaceAll(">",UriEncoder.encode(">"))
                        .replaceAll("&",UriEncoder.encode("&"))
                        .replaceAll("()",UriEncoder.encode("()"));
            }
        }
        return str;
    }

}
