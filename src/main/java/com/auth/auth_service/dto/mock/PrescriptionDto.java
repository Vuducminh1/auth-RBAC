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
public class PrescriptionDto {
    private String prescriptionId;
    private String patientId;
    private String doctorId;
    private String approvedBy;
    private String status; // Draft, Pending, Approved, Dispensed
    private List<MedicationItem> medications;
    private String instructions;
    private String notes;
    private String branch;
    private String department;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicationItem {
        private String medicationName;
        private String dosage;
        private String frequency;
        private Integer duration; // days
        private String route; // Oral, IV, Topical
        private String instructions;
    }
}

