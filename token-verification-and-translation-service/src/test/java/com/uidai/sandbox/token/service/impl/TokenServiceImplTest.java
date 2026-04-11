package com.uidai.sandbox.token.service.impl;

import com.uidai.sandbox.common.dto.TokenRequest;
import com.uidai.sandbox.common.dto.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.jwt.*;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TokenServiceImplTest {

    private TokenServiceImpl tokenService;

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private JwtEncoder jwtEncoder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tokenService = new TokenServiceImpl(jwtDecoder, jwtEncoder);
    }

    @Test
    void testVerifyAndTranslate_ProofOfClaimBinding() {
        // 1. Setup incoming JWT with a name field
        Jwt incomingJwt = Jwt.withTokenValue("incoming-jwt")
                .header("alg", "RS256")
                .subject("UID-789")
                .claim("name", "Jane Doe")
                .build();
        
        when(jwtDecoder.decode("incoming-jwt")).thenReturn(incomingJwt);

        // 2. Setup mock for outgoing JWT
        Jwt outgoingJwt = Jwt.withTokenValue("signed-session-token")
                .header("alg", "RS256")
                .subject("UID-789")
                .build();
        
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(outgoingJwt);

        // 3. Execute
        TokenRequest request = TokenRequest.builder()
                .token("incoming-jwt")
                .systemId("TEST-SYS")
                .build();
        
        TokenResponse response = tokenService.verifyAndTranslate(request);

        // 4. Verify explicit signing path
        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());
        
        JwtClaimsSet capturedClaims = captor.getValue().getClaims();

        // CHECK: The "additional field" (normalizedName) is bound to the signed token!
        assertEquals("JANE DOE", capturedClaims.getClaim("normalizedName"), 
                "The normalizedName claim must be present in the newly signed token");
        assertEquals("SANDBOX_SESSION_TOKEN", capturedClaims.getClaim("tokenType"),
                "The tokenType claim must be present in the newly signed token");
        assertEquals("UID-789", capturedClaims.getSubject());
        
        // CHECK: The response contains the token and metadata
        assertEquals("VERIFIED", response.getStatus());
        assertEquals("signed-session-token", response.getTranslatedToken());
        assertEquals("JANE DOE", response.getDetails().get("normalizedName"));
        
        System.out.println("Verified: Additional field 'normalizedName' is bound to the NEWLY SIGNED token.");
    }

    @Test
    void testVerifyAndTranslate_Failure() {
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("Invalid token"));

        TokenRequest request = TokenRequest.builder()
                .token("invalid")
                .systemId("TEST-SYS")
                .build();
        
        TokenResponse response = tokenService.verifyAndTranslate(request);

        assertEquals("FAILED", response.getStatus());
        assertNull(response.getTranslatedToken());
        assertTrue(response.getMessage().contains("Invalid token"));
    }
}
