package com.checkmarx.flow.dto.azure;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {
    private String consumerActionId;
    private String consumerId;
    private ConsumerInputs consumerInputs;
    private String eventType;
    private String publisherId;
    private PublisherInputs publisherInputs;
    private String resourceVersion;
    private Integer scope;
}