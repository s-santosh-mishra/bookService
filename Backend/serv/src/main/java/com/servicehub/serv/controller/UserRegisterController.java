package com.servicehub.serv.controller;

import com.servicehub.serv.dto.RegisterReqDto;
import com.servicehub.serv.service.UserRegisterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserRegisterController {

    private final UserRegisterService userRegisterService;

    public UserRegisterController(
            UserRegisterService userRegisterService
    ) {
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
}