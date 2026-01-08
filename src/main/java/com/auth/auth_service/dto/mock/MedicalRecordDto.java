package com.auth.auth_service.dto.mock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecordDto {
    private String recordId;
    private String patientId;
    private String doctorId;
    private String diagnosis;
    private String symptoms;
    private String treatment;
    private String notes;
    private String sensitivity; // Normal, High
    private String branch;
    private String department;
    private LocalDateTime visitDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

