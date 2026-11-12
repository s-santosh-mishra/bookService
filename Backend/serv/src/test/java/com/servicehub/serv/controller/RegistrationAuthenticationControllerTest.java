package com.servicehub.serv.controller;

import com.servicehub.serv.config.SecurityConfig;
import com.servicehub.serv.dto.LoginResDto;
import com.servicehub.serv.enums.UserRole;
import com.servicehub.serv.security.JwtAuthenticationFilter;
import com.servicehub.serv.security.JwtService;
import com.servicehub.serv.service.UserLoginService;
import com.servicehub.serv.service.UserRegisterService;
import com.servicehub.serv.service.WorkerRegisterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        UserRegisterController.class,
        WorkerRegisterController.class,
        AuthController.class
})
@EnableWebSecurity
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class RegistrationAuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRegisterService userRegisterService;

    @MockitoBean
    private WorkerRegisterService workerRegisterService;

    @MockitoBean
    private UserLoginService userLoginService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void customerRegistrationEndpointIsPublicAndReturnsCreated() throws Exception {
        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "fullName":"Test Customer",
                          "age":30,
                          "gender":"Other",
                          "email":"customer@example.com",
                          "phone":"9876543210",
                          "addressLine1":"1 Main Street",
                          "city":"Bhubaneswar",
                          "state":"Odisha",
                          "pinCode":"751001",
                          "password":"customer-password",
                          "confirmPassword":"customer-password",
                          "termsAccepted":true
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(content().string("User registered successfully"));

        verify(userRegisterService).register(any());
    }

    @Test
    void workerRegistrationEndpointIsPublicAndReturnsCreated() throws Exception {
        mockMvc.perform(post("/api/worker/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "fullName":"Test Worker",
                          "age":32,
                          "gender":"Other",
                          "email":"worker@example.com",
                          "phone":"9876543210",
                          "addressLine1":"2 Main Street",
                          "city":"Bhubaneswar",
                          "state":"Odisha",
                          "pinCode":"751001",
                          "bankAccountHolderName":"Account Holder",
                          "bankAccountNumber":"1234567890",
                          "bankIfscCode":"ABCD0123456",
                          "bankName":"Test Bank",
                          "password":"worker-password",
                          "confirmPassword":"worker-password",
                          "experienceYears":5,
                          "qualification":"Trade Certificate",
                          "serviceIds":["10000000-0000-0000-0000-000000000001"],
                          "termsAccepted":true
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(content().string("Worker registration submitted successfully"));

        verify(workerRegisterService).register(any());
    }

    @Test
    void loginEndpointIsPublicAndReturnsLoginResponse() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userLoginService.login(any())).thenReturn(
                new LoginResDto(
                        userId,
                        "Test Customer",
                        "customer@example.com",
                        UserRole.USER,
                        "customer-token"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email":"customer@example.com",
                          "password":"customer-password"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.token").value("customer-token"));

        verify(userLoginService).login(any());
    }

    @Test
    void invalidCustomerRegistrationIsRejectedBeforeServiceCall() throws Exception {
        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userRegisterService);
    }

    @Test
    void invalidWorkerRegistrationIsRejectedBeforeServiceCall() throws Exception {
        mockMvc.perform(post("/api/worker/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(workerRegisterService);
    }

    @Test
    void invalidLoginRequestIsRejectedBeforeServiceCall() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userLoginService);
    }
}
