package com.checkmarx.flow.dto.azure;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerInputs {
    private String httpHeaders;
    private String url;
}