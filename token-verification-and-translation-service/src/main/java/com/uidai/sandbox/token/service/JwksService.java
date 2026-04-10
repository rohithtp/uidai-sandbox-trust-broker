package com.uidai.sandbox.token.service;

import com.nimbusds.jose.jwk.JWKSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.text.ParseException;

@Slf4j
@Service
public class JwksService {

    @Value("${app.security.jwks-uri:https://example.com/.well-known/jwks.json}")
    private String jwksUri;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Fetches JWKS from the configured URI and caches the raw JSON string in Redis.
     * Caching the raw string ensures easy serialization/deserialization in Redis.
     */
    @Cacheable(value = "jwks", key = "'current_jwks'")
    public String getJwksAsJson() {
        log.info("Fetching JWKS from remote URI: {}", jwksUri);
        try {
            return restTemplate.getForObject(jwksUri, String.class);
        } catch (Exception e) {
            log.error("Failed to fetch JWKS from {}", jwksUri, e);
            throw new RuntimeException("Could not fetch JWKS", e);
        }
    }

    /**
     * Retrieves the JWKSet, using the cached JSON if available.
     */
    public JWKSet getJWKSet() {
        String json = getJwksAsJson();
        try {
            return JWKSet.parse(json);
        } catch (ParseException e) {
            log.error("Failed to parse JWKS JSON", e);
            throw new RuntimeException("Invalid JWKS format", e);
        }
    }
}
