package com.uidai.sandbox.common.dto;

import java.util.Map;

public record TokenResponse(
    String status,
    String message,
    String translatedToken,
    Map<String, Object> details
) {}
