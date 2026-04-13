package com.footballstats.backend.controller;

import com.footballstats.backend.dto.auth.AuthResponse;
import com.footballstats.backend.dto.auth.GuestLoginRequest;
import com.footballstats.backend.dto.auth.LoginRequest;
import com.footballstats.backend.dto.auth.RegisterRequest;
import com.footballstats.backend.dto.auth.UserResponse;
import com.footballstats.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/guest")
    public ResponseEntity<AuthResponse> guestLogin(@RequestBody(required = false) GuestLoginRequest request) {
        return ResponseEntity.ok(authService.guestLogin(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(authService.getCurrentUser(authHeader));
    }
}
