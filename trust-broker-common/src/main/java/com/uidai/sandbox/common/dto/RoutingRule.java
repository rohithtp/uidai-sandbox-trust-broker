package com.uidai.sandbox.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingRule implements Serializable {
    private String ruleId;
    private String systemId;
    private String targetEndpoint;
    private String protocol; // e.g., REST, KAFKA, GRPC
    private int priority;
}
