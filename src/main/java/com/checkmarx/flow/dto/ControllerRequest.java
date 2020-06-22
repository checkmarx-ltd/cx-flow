package com.checkmarx.flow.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ControllerRequest {
    private String application;
    private List<String> branch;
}
