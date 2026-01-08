package com.auth.auth_service.dto.mock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DischargeSummaryDto {
    private String summaryId;
    private String admissionId;
    private String patientId;
    private String preparedBy;
    private String admittingDiagnosis;
    private String dischargeDiagnosis;
    private String proceduresPerformed;
    private String hospitalCourse;
    private String conditionAtDischarge;
    private List<String> dischargeInstructions;
    private List<String> medications;
    private String followUpAppointments;
    private String branch;
    private String department;
    private LocalDateTime createdAt;
}

