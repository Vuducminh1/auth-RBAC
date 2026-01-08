package com.auth.auth_service.dto.mock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessPolicyDto {
    private String policyId;
    private String policyName;
    private String description;
    private String policyType; // RBAC, ABAC, TimeBasedmodifiedBy
    private boolean enabled;
    private Integer priority;
    private String targetRole;
    private String targetResource;
    private List<String> allowedActions;
    private Map<String, Object> conditions;
    private String modifiedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

