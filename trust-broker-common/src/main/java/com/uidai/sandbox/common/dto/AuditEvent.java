package com.uidai.sandbox.common.dto;

import java.time.LocalDateTime;

public record AuditEvent(
    String eventId,
    String serviceName,
    String action,
    String status,
    String actor,
    LocalDateTime timestamp,
    String metadata
) {}
