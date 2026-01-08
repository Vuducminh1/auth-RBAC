package com.auth.auth_service.controller;

import com.auth.auth_service.dto.ApiResponse;
import com.auth.auth_service.dto.LoginRequest;
import com.auth.auth_service.dto.LoginResponse;
import com.auth.auth_service.dto.UserDto;
import com.auth.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for user: {}", loginRequest.getUsername());
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
    
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser() {
        UserDto user = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(user));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // JWT is stateless, client should discard token
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refreshToken() {
        // In a production system, implement refresh token logic
        return ResponseEntity.ok(ApiResponse.success("Token refresh not implemented yet", null));
    }
}

