package com.auth.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String userId;
    private String username;
    private String email;
    private String role;
    private String department;
    private String branch;
    private String position;
    private boolean hasLicense;
    private String seniority;
    private String employmentType;
    private boolean enabled;
    private Set<String> assignedPatients;
    private Set<String> permissions;
}

