package com.uidai.sandbox.common.dto;

import java.util.Map;

public sealed interface VerificationResult {
    record Success(String subject, String sessionToken, Map<String, Object> details) 
        implements VerificationResult {}
    record Failure(String reason, String systemId) 
        implements VerificationResult {}
}
