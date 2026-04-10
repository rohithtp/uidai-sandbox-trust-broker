package com.uidai.sandbox.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {
    private String eventId;
    private String serviceName;
    private String action;
    private String status;
    private String actor;
    private LocalDateTime timestamp;
    private String metadata;
}
