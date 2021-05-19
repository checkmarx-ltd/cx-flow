package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.utils.ScanUtils;
import groovy.lang.Binding;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ExternalScriptService {
    private static final String SCAN_REQUEST_VARIABLE = "request";

    public String getScriptExecutionResult(ScanRequest request, String scriptFile, String entity) {
        String result = null;
        log.info("executing external script to determine the {} in Checkmarx to be used ({})", entity, scriptFile);
        try {
            String script = getStringFromFile(scriptFile);
            HashMap<String, Object> bindings = new HashMap<>();
            bindings.put(SCAN_REQUEST_VARIABLE, request);
            Object rawResult = runScript(script, bindings);
            if (rawResult instanceof String) {
                result = ((String) rawResult);
            }
            else {
                log.error("Script must return a result of type 'java.lang.String'");
            }
        } catch (IOException e) {
            log.error("Error reading script file for Checkmarx {} {}", entity, scriptFile, e);
        }
        return result;
    }

    public Object executeBranchScript(String scriptFile, ScanRequest request, List<String> branches) {
        Object result = null;
        log.info("executing external script to determine if branch should be scanned ({})", scriptFile);
        try {
            String script = getStringFromFile(scriptFile);
            HashMap<String, Object> bindings = new HashMap<>();
            bindings.put(SCAN_REQUEST_VARIABLE, request);
            bindings.put("branches", branches);
            result = runScript(script, bindings);
        } catch (IOException e) {
            log.error("Error reading script file {}", scriptFile, e);
        }
        return result;
    }

    private static String getStringFromFile(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(new File(path).getCanonicalPath())));
    }

    private Object runScript(String script, Map<String, Object> bindings){
        Binding binding = new Binding();
        if(bindings != null) {
            for (Map.Entry<String, Object> entry : bindings.entrySet()) {
                String param = entry.getKey();
                Object value = entry.getValue();
                if(!ScanUtils.empty(param) && value != null){
                    binding.setVariable(param, value);
                }
            }
        }
        try {
            GroovyShell shell = new GroovyShell(binding);
            return shell.evaluate(script);
        }catch (GroovyRuntimeException e){
            log.error("Error occurred while executing external script, returning null - {}", ExceptionUtils.getMessage(e));
            return null;
        }
    }
}

