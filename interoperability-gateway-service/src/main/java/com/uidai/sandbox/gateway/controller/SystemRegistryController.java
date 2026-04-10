package com.uidai.sandbox.gateway.controller;

import com.uidai.sandbox.common.dto.ExternalSystem;
import com.uidai.sandbox.common.dto.RoutingRule;
import com.uidai.sandbox.common.dto.TrustLevel;
import com.uidai.sandbox.gateway.service.SystemRegistryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/registry")
@RequiredArgsConstructor
@Tag(name = "System Registry", description = "Endpoints for managing external system trust levels and routing rules.")
public class SystemRegistryController {

    private final SystemRegistryService registryService;

    @Operation(summary = "Register External System", description = "Adds a new external system to the registry with a specified trust level.")
    @PostMapping("/systems")
    public ResponseEntity<ExternalSystem> registerSystem(@RequestBody ExternalSystem system) {
        return ResponseEntity.ok(registryService.registerSystem(system));
    }

    @Operation(summary = "Get System Details", description = "Retrieves metadata for a specific external system.")
    @GetMapping("/systems/{systemId}")
    public ResponseEntity<ExternalSystem> getSystem(@PathVariable String systemId) {
        return registryService.getSystem(systemId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "List All Systems", description = "Retrieves a list of all registered external systems.")
    @GetMapping("/systems")
    public ResponseEntity<List<ExternalSystem>> getAllSystems() {
        return ResponseEntity.ok(registryService.getAllSystems());
    }

    @Operation(summary = "Update Trust Level", description = "Updates the trust level for an existing system.")
    @PatchMapping("/systems/{systemId}/trust")
    public ResponseEntity<Void> updateTrustLevel(@PathVariable String systemId, @RequestParam TrustLevel trustLevel) {
        registryService.updateTrustLevel(systemId, trustLevel);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add Routing Rule", description = "Defines a new routing rule for an external system.")
    @PostMapping("/rules")
    public ResponseEntity<RoutingRule> addRoutingRule(@RequestBody RoutingRule rule) {
        return ResponseEntity.ok(registryService.addRoutingRule(rule));
    }

    @Operation(summary = "Get Routing Rules", description = "Retrieves all routing rules associated with a system.")
    @GetMapping("/systems/{systemId}/rules")
    public ResponseEntity<List<RoutingRule>> getRoutingRules(@PathVariable String systemId) {
        return ResponseEntity.ok(registryService.getRoutingRules(systemId));
    }

    @Operation(summary = "Delete Routing Rule", description = "Removes a specific routing rule.")
    @DeleteMapping("/rules/{ruleId}")
    public ResponseEntity<Void> deleteRoutingRule(@PathVariable String ruleId) {
        registryService.deleteRoutingRule(ruleId);
        return ResponseEntity.noContent().build();
    }
}
