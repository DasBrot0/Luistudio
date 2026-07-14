package com.luistudio.reservas.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthIsPublicEvenWithoutAValidJwt() throws Exception {
        mockMvc.perform(get("/actuator/health")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-uptime-token"))
            .andExpect(result -> {
                int responseStatus = result.getResponse().getStatus();
                assertNotEquals(401, responseStatus);
                assertNotEquals(403, responseStatus);
            });
    }

    @Test
    void applicationEndpointsRejectAnInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/rooms")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-application-token"))
            .andExpect(status().isUnauthorized());
    }
}
