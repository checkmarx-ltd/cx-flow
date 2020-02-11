package com.checkmarx.flow.cucumber.common.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class JsonUtils {

    public static <T> T file2object(File file, Class<T> clazz) throws IOException {
        return newObjectMapper().readerFor(clazz).readValue(file);
    }

    public static <T> T json2object(String body, Class<T> clazz) throws IOException {
        return newObjectMapper().readerFor(clazz).readValue(body);
    }

    public static String object2json(Object obj) throws IOException {
        return newObjectMapper().writeValueAsString(obj);
    }

    private static ObjectMapper newObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    public static void reserialize(String sourceFile, String targetFile, Class<?> clazz) throws IOException {
        Object obj = file2object(new File(sourceFile), clazz);
        String json = object2json(obj);
        Files.write(Paths.get(targetFile), json.getBytes());
    }
}
