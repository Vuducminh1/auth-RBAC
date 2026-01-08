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
public class TrainingRecordDto {
    private String trainingId;
    private String staffId;
    private String createdBy;
    private String trainingName;
    private String trainingType; // Mandatory, Optional, Certification
    private String provider;
    private LocalDate completedDate;
    private LocalDate expiryDate;
    private String status; // Scheduled, InProgress, Completed, Expired
    private String certificateNumber;
    private Double score;
    private String notes;
    private String branch;
    private String department;
    private LocalDateTime createdAt;
}

