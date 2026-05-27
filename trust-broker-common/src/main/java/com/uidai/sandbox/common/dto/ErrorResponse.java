package com.uidai.sandbox.common.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
    String message,
    String errorCode,
    LocalDateTime timestamp,
    String path
) {}
