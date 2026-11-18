package me.xjanua.aitranswatch.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import me.xjanua.aitranswatch.dto.RestResponse;
import me.xjanua.aitranswatch.dto.auth.LoginRequest;
import me.xjanua.aitranswatch.dto.auth.LoginResponse;
import me.xjanua.aitranswatch.dto.auth.UserMeResponse;
import me.xjanua.aitranswatch.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public RestResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return RestResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    public RestResponse<UserMeResponse> me(Authentication authentication) {
        return RestResponse.success(authService.me(authentication));
    }
}
