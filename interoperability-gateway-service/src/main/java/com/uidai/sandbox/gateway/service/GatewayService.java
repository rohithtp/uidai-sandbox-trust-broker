package com.uidai.sandbox.gateway.service;

import com.uidai.sandbox.common.dto.TokenRequest;
import com.uidai.sandbox.common.dto.TokenResponse;

/**
 * Service interface for the Interoperability Gateway.
 * Handles request normalization and routing logic.
 */
public interface GatewayService {
    
    /**
     * Processes an incoming request, identifies the target system, 
     * and performs initial normalization.
     * 
     * @param request the incoming token request
     * @return a response indicating the result of the gateway processing
     */
    TokenResponse processIncomingRequest(TokenRequest request);
}
