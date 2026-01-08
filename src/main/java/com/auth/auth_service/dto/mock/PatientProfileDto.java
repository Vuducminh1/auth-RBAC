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
public class PatientProfileDto {
    private String patientId;
    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private String nationalId;
    private String phone;
    private String email;
    private String address;
    private String branch;
    private String department;
    private String bloodType;
    private String allergies;
    private String emergencyContact;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

