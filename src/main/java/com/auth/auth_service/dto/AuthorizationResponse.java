package com.auth.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationResponse {
    private boolean allowed;
    private String policyId;
    private List<String> denyReasons;
    private List<Map<String, Object>> obligations;
    private Integer riskScore;
}

