package com.uidai.sandbox.token.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uidai.sandbox.common.dto.TokenRequest;
import com.uidai.sandbox.common.dto.TokenResponse;
import com.uidai.sandbox.common.dto.VerificationResult;
import com.uidai.sandbox.token.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TokenController.class, excludeAutoConfiguration = {
        KafkaAutoConfiguration.class,
        RedisAutoConfiguration.class
})
public class TokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TokenService tokenService;

    @Test
    @WithMockUser
    public void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/v1/token/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("token-verification-and-translation-service"))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @WithMockUser
    public void testVerifyToken() throws Exception {
        TokenRequest request = new TokenRequest("test-token", "test-system");

        VerificationResult result = new VerificationResult.Success("subject", "session-token", Map.of("systemId", "test-system"));

        when(tokenService.verifyAndTranslate(any(TokenRequest.class))).thenReturn(result);

        mockMvc.perform(post("/api/v1/token/verify")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.message").value("Token successfully verified and translated"));
    }
}
