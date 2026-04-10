package com.uidai.sandbox.token.service;

import com.uidai.sandbox.common.dto.TokenRequest;
import com.uidai.sandbox.common.dto.TokenResponse;

/**
 * Service interface for token verification and translation.
 */
public interface TokenService {
    
    /**
     * Verifies the provided token and translates it into a standard UIDAI format.
     * 
     * @param request the token request containing the raw token and system identifier
     * @return a normalized token response
     */
    TokenResponse verifyAndTranslate(TokenRequest request);
}
