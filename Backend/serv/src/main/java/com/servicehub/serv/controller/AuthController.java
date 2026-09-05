package com.servicehub.serv.controller;

import com.servicehub.serv.dto.LoginReqDto;
import com.servicehub.serv.dto.LoginResDto;
import com.servicehub.serv.dto.RegisterReqDto;
import com.servicehub.serv.service.UserLoginService;
import com.servicehub.serv.service.UserRegisterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserLoginService userLoginService;
    private final UserRegisterService userRegisterService;

    public AuthController(
            UserLoginService userLoginService,
            UserRegisterService userRegisterService
    ) {
        this.userLoginService = userLoginService;
        this.userRegisterService = userRegisterService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterReqDto request
    ) {

        userRegisterService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResDto> login(
            @Valid @RequestBody LoginReqDto request
    ) {

        LoginResDto response = userLoginService.login(request);

        return ResponseEntity.ok(response);
    }
}