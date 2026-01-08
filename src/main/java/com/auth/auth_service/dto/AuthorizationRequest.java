package com.auth.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationRequest {
    
    @NotBlank(message = "Resource type is required")
    private String resourceType;
    
    @NotBlank(message = "Action is required")
    private String action;
    
    private String resourceId;
    
    private String resourceBranch;
    
    private String resourceDepartment;
    
    private String patientId;
    
    private String resourceSensitivity;
    
    private String createdBy;
    
    private Map<String, Object> environment;
}

