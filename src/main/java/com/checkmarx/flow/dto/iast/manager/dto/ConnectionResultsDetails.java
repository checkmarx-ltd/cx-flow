package com.checkmarx.flow.dto.iast.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionResultsDetails {

    private String domain; // remote connection

    private Integer port;

    private String path;

    private int vectorId;

    private Instant firstDetectionTime;
}

