package com.uidai.sandbox.gateway.service;

import com.uidai.sandbox.common.dto.ExternalSystem;
import com.uidai.sandbox.common.dto.RoutingRule;

import java.util.List;
import java.util.Optional;

public interface SystemRegistryService {
    ExternalSystem registerSystem(ExternalSystem system);
    Optional<ExternalSystem> getSystem(String systemId);
    List<ExternalSystem> getAllSystems();
    void updateTrustLevel(String systemId, com.uidai.sandbox.common.dto.TrustLevel trustLevel);
    
    RoutingRule addRoutingRule(RoutingRule rule);
    List<RoutingRule> getRoutingRules(String systemId);
    void deleteRoutingRule(String ruleId);
}
