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
public class LabOrderDto {
    private String orderId;
    private String patientId;
    private String orderedBy;
    private String status; // Ordered, InProgress, Completed, Cancelled
    private List<String> tests; // List of test names
    private String priority; // Routine, Urgent, STAT
    private String clinicalNotes;
    private String branch;
    private String department;
    private LocalDateTime orderedAt;
    private LocalDateTime completedAt;
}

