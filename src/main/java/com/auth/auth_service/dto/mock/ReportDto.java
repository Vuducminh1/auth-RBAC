package com.auth.auth_service.dto.mock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDto {
    private String reportId;
    private String reportType; // MedicalReport, OperationReport, FinancialReport
    private String reportName;
    private String generatedBy;
    private String periodStart;
    private String periodEnd;
    private String status; // Draft, Final
    private Map<String, Object> data;
    private String summary;
    private String branch;
    private String department;
    private LocalDateTime generatedAt;
}

