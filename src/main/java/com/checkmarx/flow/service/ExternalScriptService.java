package com.checkmarx.flow.service;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class ExternalScriptService {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(CxService.class);

    public void test(){
        Binding binding = new Binding();
        binding.setVariable("x", "x");
        GroovyShell shell = new GroovyShell(binding);
        Object result = shell.evaluate("@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7' )\n" +
                "import groovyx.net.http.HTTPBuilder\n" +
                "def http = new HTTPBuilder('https://google.com')\n" +
                "def html = http.get(path : '/search', query : [q:x])\n" +
                "return html");
        log.info(result.toString());
    }
}
