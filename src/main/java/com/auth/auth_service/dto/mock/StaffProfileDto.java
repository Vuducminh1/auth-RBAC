package com.auth.auth_service.dto.mock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffProfileDto {
    private String staffId;
    private String employeeNumber;
    private String fullName;
    private String email;
    private String phone;
    private String position;
    private String department;
    private String branch;
    private String role;
    private String employmentType; // FullTime, PartTime, Contract
    private String seniority; // Junior, Mid, Senior
    private boolean hasLicense;
    private String licenseNumber;
    private LocalDate licenseExpiry;
    private LocalDate hireDate;
    private String status; // Active, OnLeave, Suspended, Terminated
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

