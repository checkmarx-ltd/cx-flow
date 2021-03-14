package com.checkmarx.flow.controller;

import com.checkmarx.flow.dto.EventResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Random;

import static com.atlassian.sal.api.xsrf.XsrfHeaderValidator.TOKEN_HEADER;

@Slf4j
@RestController
@RequestMapping(value = "/iast")
@RequiredArgsConstructor
public class IastController {

    private Random random = new Random();

    @PostMapping(value = { "/iniqTag"})
    public ResponseEntity<EventResponse> generateUniqTag(){
        return ResponseEntity.accepted().body(EventResponse.builder()
                .message(Long.toString(Math.abs(random.nextLong())))
                .success(true)
                .build());
    }


    @PostMapping(value = { "/stopScanAndCreateJiraTask/{scanId}"})
    public ResponseEntity<EventResponse> stopScanAndCreateJiraTask(
            @PathVariable(value = "scanId", required = false) String scanId,
            @RequestHeader(value = TOKEN_HEADER) String token){
        // Validate shared API token from header
//        validateToken(token);   //FlowController


        return null;
    }


}
