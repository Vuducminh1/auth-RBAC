package com.auth.auth_service.dto.mock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkScheduleDto {
    private String scheduleId;
    private String staffId;
    private String createdBy;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String shiftType; // Morning, Afternoon, Night, OnCall
    private String status; // Scheduled, Completed, Cancelled, NoShow
    private String notes;
    private String branch;
    private String department;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

