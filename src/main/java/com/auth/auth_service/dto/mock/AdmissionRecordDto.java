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
public class AdmissionRecordDto {
    private String admissionId;
    private String patientId;
    private String admittedBy;
    private String attendingDoctorId;
    private String admissionType; // Emergency, Elective, Transfer
    private String status; // Active, Discharged, Transferred
    private String chiefComplaint;
    private String admissionDiagnosis;
    private String roomNumber;
    private String bedNumber;
    private String branch;
    private String department;
    private LocalDateTime admittedAt;
    private LocalDateTime dischargedAt;
}

