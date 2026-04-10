package com.uidai.sandbox.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uidai.sandbox.common.dto.TokenRequest;
import com.uidai.sandbox.common.dto.TokenResponse;
import com.uidai.sandbox.gateway.service.GatewayService;
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

@WebMvcTest(controllers = GatewayController.class, excludeAutoConfiguration = {
        KafkaAutoConfiguration.class,
        RedisAutoConfiguration.class
})
public class GatewayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GatewayService gatewayService;

    @Test
    @WithMockUser
    public void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/v1/gateway/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("interoperability-gateway-service"))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @WithMockUser
    public void testProcessRequest() throws Exception {
        TokenRequest request = TokenRequest.builder()
                .token("test-token")
                .systemId("test-system")
                .build();

        TokenResponse response = TokenResponse.builder()
                .status("ACCEPTED")
                .message("Request received")
                .build();

        when(gatewayService.processIncomingRequest(any(TokenRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/gateway/process")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.message").value("Request received"));
    }
}
