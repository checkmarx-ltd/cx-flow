package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.utils.ScanUtils;
import groovy.lang.Binding;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExternalScriptService {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ExternalScriptService.class);
    private static final String REQUEST = "request";

    public String getScriptExecutionResult(ScanRequest request, String scriptFile, String entity) {
        String result = null;
        log.info("executing external script to determine the {} in Checkmarx to be used ({})", entity, scriptFile);
        try {
            String script = getStringFromFile(scriptFile);
            HashMap<String, Object> bindings = new HashMap<>();
            bindings.put(REQUEST, request);
            Object rawResult = runScript(script, bindings);
            if (rawResult instanceof String) {
                result = ((String) rawResult);
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
            bindings.put(REQUEST, request);
            bindings.put("branches", branches);
            result = runScript(script, bindings);
        } catch (IOException e) {
            log.error("Error reading script file {}", scriptFile, e);
        }
        return result;
    }

    private String getStringFromFile(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path.intern())));
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
            log.error("Error occurred while executing external script, returning null - {}", ExceptionUtils.getMessage(e), e);
            return null;
        }
    }
}

