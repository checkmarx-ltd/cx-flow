package com.checkmarx.flow.cucumber.common.utils;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.CxFlowRunner;
import com.checkmarx.flow.cucumber.common.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * relativePath's in the methods of this class are relative to Cucumber data dir in resources.
 */
public class TestUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private TestUtils() {
        // Instances should not be created.
    }

    public static InputStream getResourceAsStream(String relativePath) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String fullResourcePath = toFullResourcePath(relativePath);
        return classLoader.getResourceAsStream(fullResourcePath);
    }

    public static File getFileFromRelativeResourcePath(String relativePath) throws IOException {
        return new ClassPathResource(relativePath).getFile();
    }

    public static File getFileFromResource(String relativePath) throws IOException {
        String fullResourcePath = toFullResourcePath(relativePath);
        return new ClassPathResource(fullResourcePath).getFile();
    }

    public static String getResourceAsString(String relativePath) throws IOException {
        InputStream resourceStream = getResourceAsStream(relativePath);
        return IOUtils.toString(resourceStream, StandardCharsets.UTF_8);
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

    public static JsonNode parseJsonFromResources(String relativePath) throws IOException {
        String resourcePath = Paths.get(Constants.CUCUMBER_DATA_DIR, relativePath).toString();
        File file = ResourceUtils.getFile("classpath:" + resourcePath);
        return objectMapper.readTree(file);
    }

    public static void runCxFlow(CxFlowRunner runner, String args) throws InvocationTargetException {
        runner.run(new DefaultApplicationArguments(args.split(" ")));
    }

    public static ConfigurableApplicationContext runCxFlowAsService() {
        return SpringApplication.run(CxFlowApplication.class, "--web");
    }

    public static ConfigurableApplicationContext runCxFlowAsServiceWithAdditionalProfiles(String... additionalProfiles) {
        SpringApplication springApplication=new SpringApplication(CxFlowApplication.class);
        springApplication.setAdditionalProfiles(additionalProfiles); 
        return springApplication.run("--web"); 
    }

    public static void exitCxFlowService(ConfigurableApplicationContext appContext) {
        SpringApplication.exit(appContext);
    }
}
