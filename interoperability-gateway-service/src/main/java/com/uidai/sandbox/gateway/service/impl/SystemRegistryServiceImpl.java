package com.uidai.sandbox.gateway.service.impl;

import com.uidai.sandbox.common.dto.ExternalSystem;
import com.uidai.sandbox.common.dto.RoutingRule;
import com.uidai.sandbox.common.dto.TrustLevel;
import com.uidai.sandbox.gateway.service.SystemRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SystemRegistryServiceImpl implements SystemRegistryService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String SYSTEM_KEY_PREFIX = "system:";
    private static final String ROUTING_RULE_KEY_PREFIX = "rule:";
    private static final String SYSTEM_RULES_SET_PREFIX = "system_rules:";

    @Override
    public ExternalSystem registerSystem(ExternalSystem system) {
        log.info("Registering system: {}", system.getSystemId());
        redisTemplate.opsForValue().set(SYSTEM_KEY_PREFIX + system.getSystemId(), system);
        return system;
    }

    @Override
    public Optional<ExternalSystem> getSystem(String systemId) {
        ExternalSystem system = (ExternalSystem) redisTemplate.opsForValue().get(SYSTEM_KEY_PREFIX + systemId);
        return Optional.ofNullable(system);
    }

    @Override
    public List<ExternalSystem> getAllSystems() {
        Set<String> keys = redisTemplate.keys(SYSTEM_KEY_PREFIX + "*");
        if (keys == null) return List.of();
        return keys.stream()
                .map(key -> (ExternalSystem) redisTemplate.opsForValue().get(key))
                .collect(Collectors.toList());
    }

    @Override
    public void updateTrustLevel(String systemId, TrustLevel trustLevel) {
        getSystem(systemId).ifPresent(system -> {
            system.setTrustLevel(trustLevel);
            registerSystem(system);
        });
    }

    @Override
    public RoutingRule addRoutingRule(RoutingRule rule) {
        log.info("Adding routing rule: {} for system: {}", rule.getRuleId(), rule.getSystemId());
        redisTemplate.opsForValue().set(ROUTING_RULE_KEY_PREFIX + rule.getRuleId(), rule);
        redisTemplate.opsForSet().add(SYSTEM_RULES_SET_PREFIX + rule.getSystemId(), rule.getRuleId());
        return rule;
    }

    @Override
    public List<RoutingRule> getRoutingRules(String systemId) {
        Set<Object> ruleIds = redisTemplate.opsForSet().members(SYSTEM_RULES_SET_PREFIX + systemId);
        if (ruleIds == null) return List.of();
        return ruleIds.stream()
                .map(id -> (RoutingRule) redisTemplate.opsForValue().get(ROUTING_RULE_KEY_PREFIX + id))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteRoutingRule(String ruleId) {
        RoutingRule rule = (RoutingRule) redisTemplate.opsForValue().get(ROUTING_RULE_KEY_PREFIX + ruleId);
        if (rule != null) {
            redisTemplate.delete(ROUTING_RULE_KEY_PREFIX + ruleId);
            redisTemplate.opsForSet().remove(SYSTEM_RULES_SET_PREFIX + rule.getSystemId(), ruleId);
        }
    }
}
