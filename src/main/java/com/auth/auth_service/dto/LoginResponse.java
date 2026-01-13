package com.auth.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private String userId;
    private String username;
    private String role;
    private String department;
    private String branch;
    
    /**
     * Permissions grouped by resource type
     * Example: {"AdmissionRecord": "read", "ClinicalNote": "create,read"}
     */
    private Map<String, String> permissions;
}

