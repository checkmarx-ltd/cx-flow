package com.checkmarx.flow.service;

import com.checkmarx.flow.utils.ScanUtils;
import groovy.lang.Binding;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class ExternalScriptService {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ExternalScriptService.class);

    public Object runScript(String script, Map<String, Object> bindings){
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

