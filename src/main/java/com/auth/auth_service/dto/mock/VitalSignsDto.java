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
public class VitalSignsDto {
    private String vitalId;
    private String patientId;
    private String recordedBy;
    private Double temperature; // Celsius
    private Integer heartRate; // BPM
    private Integer bloodPressureSystolic;
    private Integer bloodPressureDiastolic;
    private Integer respiratoryRate;
    private Double oxygenSaturation; // %
    private Double weight; // kg
    private Double height; // cm
    private String notes;
    private String branch;
    private String department;
    private LocalDateTime recordedAt;
}

