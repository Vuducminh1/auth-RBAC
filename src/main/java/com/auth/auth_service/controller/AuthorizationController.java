package com.auth.auth_service.controller;

import com.auth.auth_service.aop.Audit;
import com.auth.auth_service.dto.ApiResponse;
import com.auth.auth_service.dto.AuthorizationRequest;
import com.auth.auth_service.dto.AuthorizationResponse;
import com.auth.auth_service.service.AuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/authz")
@RequiredArgsConstructor
@Slf4j
public class AuthorizationController {
    
    private final AuthorizationService authorizationService;
    
    /**
     * Check if current user is authorized to perform an action on a resource
     */
    @Audit(resourceType = "AuthorizationRequest", action = "check", useFirstArgAsResourceId = false)
    @PostMapping("/check")
    public ResponseEntity<ApiResponse<AuthorizationResponse>> checkAuthorization(
            @Valid @RequestBody AuthorizationRequest request) {
        log.debug("Authorization check: resource={}, action={}", request.getResourceType(), request.getAction());
        AuthorizationResponse response = authorizationService.authorize(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Quick permission check (RBAC only, no ABAC)
     */
    @GetMapping("/permission")
    public ResponseEntity<ApiResponse<Boolean>> hasPermission(
            @RequestParam String resourceType,
            @RequestParam String action) {
        boolean hasPermission = authorizationService.hasPermission(resourceType, action);
        return ResponseEntity.ok(ApiResponse.success(hasPermission));
    }
    
    /**
     * Batch authorization check
     */
    @PostMapping("/check-batch")
    public ResponseEntity<ApiResponse<java.util.List<AuthorizationResponse>>> checkAuthorizationBatch(
            @Valid @RequestBody java.util.List<AuthorizationRequest> requests) {
        java.util.List<AuthorizationResponse> responses = requests.stream()
                .map(authorizationService::authorize)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
