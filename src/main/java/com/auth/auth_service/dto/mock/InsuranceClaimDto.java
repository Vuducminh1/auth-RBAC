package com.auth.auth_service.dto.mock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceClaimDto {
    private String claimId;
    private String invoiceId;
    private String patientId;
    private String insuranceProvider;
    private String policyNumber;
    private String createdBy;
    private String approvedBy;
    private BigDecimal claimAmount;
    private BigDecimal approvedAmount;
    private String status; // Draft, Submitted, UnderReview, Approved, Rejected, Paid
    private String rejectionReason;
    private String branch;
    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
}

