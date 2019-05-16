package com.checkmarx.flow.controller;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.service.FlowService;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.*;
import java.beans.ConstructorProperties;


@RestController
@RequestMapping(value = "/")
public class FlowController {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(FlowController.class);

    private final FlowProperties properties;
    private final FlowService scanService;

    @ConstructorProperties({"properties", "scanService"})
    public FlowController(FlowProperties properties, FlowService scanService) {
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
