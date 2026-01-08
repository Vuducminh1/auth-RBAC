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
public class IncidentCaseDto {
    private String caseId;
    private String title;
    private String description;
    private String severity; // Low, Medium, High, Critical
    private String category; // Security, Privacy, Compliance, Operational
    private String status; // Open, InProgress, Resolved, Closed
    private String reportedBy;
    private String assignedTo;
    private String resolution;
    private List<String> affectedSystems;
    private List<String> affectedUsers;
    private LocalDateTime incidentDate;
    private LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

