package com.vibecode.auth.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vibecode.auth.dto.AuthResponse;
import com.vibecode.auth.dto.LoginRequest;
import com.vibecode.auth.dto.LoginResponse;
import com.vibecode.auth.dto.LogoutRequest;
import com.vibecode.auth.dto.RefreshTokenRequest;
import com.vibecode.auth.dto.RegisterRequest;
import com.vibecode.auth.services.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {

        authService.register(request);
        return ResponseEntity.ok(new AuthResponse("User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> postMethodName(@Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(@Valid  @RequestBody LogoutRequest  request) {
        authService.logout(request);        
        return ResponseEntity.ok(new AuthResponse("Logged out successfully"))   ;
    }
    
    

}
