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
public class LabResultDto {
    private String resultId;
    private String orderId;
    private String patientId;
    private String performedBy;
    private String verifiedBy;
    private String status; // Pending, Verified, Released
    private List<TestResult> results;
    private String interpretation;
    private String branch;
    private String department;
    private LocalDateTime performedAt;
    private LocalDateTime releasedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestResult {
        private String testName;
        private String value;
        private String unit;
        private String referenceRange;
        private String flag; // Normal, High, Low, Critical
    }
}

