package com.custodela.machina.controller;

import com.custodela.machina.config.MachinaProperties;
import com.custodela.machina.service.MachinaService;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.*;
import java.beans.ConstructorProperties;


@RestController
@RequestMapping(value = "/")
public class MachinaController {

    private static final String EVENT = "token";
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(MachinaController.class);

    private final MachinaProperties properties;
    private final MachinaService scanService;

    @ConstructorProperties({"properties", "scanService"})
    public MachinaController(MachinaProperties properties, MachinaService scanService) {
        this.properties = properties;
        this.scanService = scanService;
    }

    @PostMapping(value = "/tmp")
    public String tmp(@RequestHeader(value="token", required = false) String token, @RequestBody String body){
        log.info("Processing request");
        log.info(body);
        log.info(token);
        return "xyz";
    }

}
