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
public class AppointmentDto {
    private String appointmentId;
    private String patientId;
    private String doctorId;
    private String createdBy;
    private String appointmentType; // Consultation, FollowUp, Procedure
    private String status; // Scheduled, CheckedIn, InProgress, Completed, Cancelled, NoShow
    private String reason;
    private String notes;
    private Integer durationMinutes;
    private String roomNumber;
    private String branch;
    private String department;
    private LocalDateTime scheduledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

