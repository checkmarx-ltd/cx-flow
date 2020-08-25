package com.checkmarx.flow.utils;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.FileSystems;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
public class ZipUtils {
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    private ZipUtils() {
    }

    public static File zipToTempFile(String fileToZip, String excludePatterns) throws IOException {
        String targetFilename = "cx.".concat(UUID.randomUUID().toString()).concat(".zip");
        String targetPath = FileSystems.getDefault()
                .getPath(targetFilename)
                .toAbsolutePath()
                .toString();
        zipFile(fileToZip, targetPath, excludePatterns);
        File zippedFile = new File(targetPath);
        log.debug("Creating temp file {}", zippedFile.getPath());
        log.debug("free space {}", zippedFile.getFreeSpace());
        log.debug("total space {}", zippedFile.getTotalSpace());
        log.debug(zippedFile.getAbsolutePath());
        return zippedFile;
    }

    public static void zipFile(String fileToZip, String zipFile, String excludePatterns)
            throws IOException {
        List<String> excludeList = null;
        log.info("Creating zip file {} from contents of path {}", zipFile, fileToZip);
        if(excludePatterns != null) {
            log.info("Applying exclusions: {}", excludePatterns);
        }

        if(!Strings.isNullOrEmpty(excludePatterns)) {
            excludeList = Arrays.asList(excludePatterns.split(","));
        }

        zipFile = FileSystems.getDefault().getPath(zipFile).toAbsolutePath().toString();
        log.debug("Zip Absolute path: {}", zipFile);
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile))) {
            File srcFile = new File(fileToZip);
            if (srcFile.isDirectory()) {
                for (String fileName : Objects.requireNonNull(srcFile.list())) {
                    addToZip("", String.format("%s/%s", fileToZip, fileName), zipFile, zipOut, excludeList);
                }
            } else {
                addToZip("", fileToZip, zipFile, zipOut, excludeList);
            }
            zipOut.flush();
        }
        log.info("Successfully created {} ", zipFile);
    }

    private static void addToZip(String path, String srcFile, String zipFile, ZipOutputStream zipOut, List<String> excludePatterns)
            throws IOException {
        File file = new File(srcFile);
        String filePath = "".equals(path) ? file.getName() : String.format("%s/%s", path, file.getName());
        if (file.isDirectory()) {
            for (String fileName : Objects.requireNonNull(file.list())) {
                addToZip(filePath, srcFile + "/" + fileName, zipFile, zipOut, excludePatterns);
            }
        } else {
            String tmpPath = FileSystems.getDefault().getPath(srcFile).toAbsolutePath().toString();
            tmpPath = tmpPath.replace("/./","/"); //Linux FS
            tmpPath = tmpPath.replace("\\.\\","\\"); //Windows FS

            log.debug("@@@ {} | {} @@@", zipFile, tmpPath);
            if(tmpPath.equals(zipFile)){
                log.debug("#########Skipping the new zip file {}#########", zipFile);
                return;
            }
            if(excludePatterns == null || excludePatterns.isEmpty() || !anyMatches(excludePatterns, filePath)) {
                zipOut.putNextEntry(new ZipEntry(filePath));
                try (FileInputStream in = new FileInputStream(srcFile)) {
                    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        zipOut.write(buffer, 0, len);
                    }
                }
            }
        }
    }

    private static boolean anyMatches(List<String> patterns, String str){
        for(String pattern: patterns){
            pattern = pattern.trim();
            if(strMatches(pattern, str)) {
                log.debug("match: {}|{}", pattern, str);
                return true;
            }
        }
        return false;
    }

    private static boolean strMatches(String patternStr, String str){
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(str);
        if(matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            return start == 0 && end == str.length();
        }
        return false;
    }
}