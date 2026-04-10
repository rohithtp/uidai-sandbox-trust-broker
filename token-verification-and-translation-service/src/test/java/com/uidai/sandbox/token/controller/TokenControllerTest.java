package com.uidai.sandbox.token.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uidai.sandbox.common.dto.TokenRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

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
        TokenRequest request = TokenRequest.builder()
                .token("test-token")
                .systemId("test-system")
                .build();

        mockMvc.perform(post("/api/v1/token/verify")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_IMPLEMENTED"));
    }
}
