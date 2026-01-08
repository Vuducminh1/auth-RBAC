package com.auth.auth_service.dto.mock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingRecordDto {
    private String billingId;
    private String patientId;
    private String admissionId;
    private String createdBy;
    private List<BillingItem> items;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal total;
    private String status; // Draft, Finalized, Paid, Partial, Cancelled
    private String branch;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillingItem {
        private String itemCode;
        private String description;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal amount;
        private String category; // Room, Procedure, Medication, Lab, Imaging
    }
}

