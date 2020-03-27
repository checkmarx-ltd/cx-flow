package com.checkmarx.flow.cucumber.common.utils;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.CxFlowRunner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.checkmarx.flow.cucumber.common.Constants;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Stack;

public class TestUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static InputStream getResourceAsStream(String relativePath) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String fullResourcePath = toFullResourcePath(relativePath);
        return classLoader.getResourceAsStream(fullResourcePath);
    }

    public static File getFileFromResource(String relativePath) throws IOException {
        String fullResourcePath = toFullResourcePath(relativePath);
        return new ClassPathResource(fullResourcePath).getFile();
    }

    private static String toFullResourcePath(String relativePath) {
        return Paths.get(Constants.CUCUMBER_DATA_DIR, relativePath).toString();
    }

    public static Properties getPropertiesFromResource(String path) throws IOException {
        File file = ResourceUtils.getFile("classpath:" + path);
        Properties result = new Properties();
        result.load(Files.newInputStream(file.toPath()));
        return result;
    }

    public static JsonNode parseJsonFromResources(String pathRelativeToData) throws IOException {
        String resourcePath = Paths.get(Constants.CUCUMBER_DATA_DIR, pathRelativeToData).toString();
        File file = ResourceUtils.getFile("classpath:" + resourcePath);
        return objectMapper.readTree(file);
    }

    private interface Revertible {
        void revert();
    }

    private static final Stack<Revertible> propertiesChanges = new Stack<>();

    public static void changePropertiesBack() {
        while (!propertiesChanges.isEmpty()) {
            propertiesChanges.pop().revert();
        }
    }

    public static void runCxFlow(CxFlowRunner runner, String args) throws InvocationTargetException {
        runner.run(new DefaultApplicationArguments(args.split(" ")));
    }

    public static ConfigurableApplicationContext runCxFlowAsService() {
        return SpringApplication.run(CxFlowApplication.class, "--web");
    }
}
