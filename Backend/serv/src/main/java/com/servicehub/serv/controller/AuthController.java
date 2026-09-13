package com.servicehub.serv.controller;

import com.servicehub.serv.dto.LoginReqDto;
import com.servicehub.serv.dto.LoginResDto;
import com.servicehub.serv.service.UserLoginService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserLoginService userLoginService;

    public AuthController(
            UserLoginService userLoginService
    ) {
        this.userLoginService = userLoginService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResDto> login(
            @Valid @RequestBody LoginReqDto request
    ) {

        LoginResDto response = userLoginService.login(request);

        return ResponseEntity.ok(response);
    }
}