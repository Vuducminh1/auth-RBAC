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
public class TransferRecordDto {
    private String transferId;
    private String patientId;
    private String admissionId;
    private String initiatedBy;
    private String approvedBy;
    private String fromDepartment;
    private String toDepartment;
    private String fromRoom;
    private String toRoom;
    private String reason;
    private String status; // Pending, Approved, Completed, Cancelled
    private String branch;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
}

