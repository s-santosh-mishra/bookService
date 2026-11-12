package com.servicehub.serv.controller;

import com.servicehub.serv.config.SecurityConfig;
import com.servicehub.serv.security.JwtAuthenticationFilter;
import com.servicehub.serv.security.JwtService;
import com.servicehub.serv.service.WorkerStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WorkerStatusController.class)
@EnableWebSecurity
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class WorkerLocationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkerStatusService workerStatusService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void unauthenticatedUserCannotUpdateLocation() throws Exception {
        mockMvc.perform(post("/api/worker/status/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"latitude":20.2961,"longitude":85.8245}
                        """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(workerStatusService);
    }

    @Test
    void authenticatedNonWorkerCannotUpdateLocation() throws Exception {
        when(jwtService.extractUserId("customer-token"))
                .thenReturn(UUID.randomUUID().toString());
        when(jwtService.extractRole("customer-token"))
                .thenReturn("USER");

        mockMvc.perform(post("/api/worker/status/location")
                .header("Authorization", "Bearer customer-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"latitude":20.2961,"longitude":85.8245}
                        """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(workerStatusService);
    }
}
