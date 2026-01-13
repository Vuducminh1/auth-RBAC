package com.auth.auth_service.controller;

import com.auth.auth_service.dto.ApiResponse;
import com.auth.auth_service.dto.LoginRequest;
import com.auth.auth_service.dto.LoginResponse;
import com.auth.auth_service.dto.RegisterRequest;
import com.auth.auth_service.dto.UserDto;
import com.auth.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    
    /**
     * Đăng ký người dùng mới
     * POST /api/auth/register
     * 
     * Request body:
     * {
     *   "username": "doctor1",
     *   "password": "password123",
     *   "email": "doctor1@hospital.com",
     *   "department": "Internal Medicine",
     *   "branch": "BRANCH_HCM",
     *   "position": "Doctor",
     *   "role": "Doctor",
     *   "hasLicense": true,
     *   "seniority": "Senior",
     *   "employmentType": "FullTime"
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDto>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Registration attempt for user: {}", registerRequest.getUsername());
        try {
            UserDto user = authService.register(registerRequest);
            log.info("User registered successfully: {}", user.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Registration successful", user));
        } catch (RuntimeException e) {
            log.error("Registration failed for user: {} - {}", registerRequest.getUsername(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
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
    
    /**
     * Lấy danh sách các role có sẵn để đăng ký
     * GET /api/auth/roles
     */
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<java.util.List<String>>> getAvailableRoles() {
        java.util.List<String> roles = java.util.List.of(
            "Doctor", "Nurse", "Receptionist", "Cashier", 
            "HR", "Manager", "ITAdmin", "SecurityAdmin"
        );
        return ResponseEntity.ok(ApiResponse.success("Available roles", roles));
    }
}

