package com.checkmarx.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestsParseUtils {
    /**
     * Splits input by comma and trims the parts.
     *
     * @param input comma-separated values
     * @return stream of input parts
     */
    public static Stream<String> parseCSV(String input) {
        if (StringUtils.isEmpty(input)) {
            return  Stream.empty();
        }
        final String SEPARATOR = ",";
        return Arrays.stream(input.split(SEPARATOR))
                .map(String::trim);
    }

    public static List<String> parseCsvToList(String input) {
        if (StringUtils.isEmpty(input)) {
            return Collections.emptyList();
        }
        return parseCSV(input).collect(Collectors.toList());
    }

}
