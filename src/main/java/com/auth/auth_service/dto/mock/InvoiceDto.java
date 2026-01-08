package com.auth.auth_service.dto.mock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDto {
    private String invoiceId;
    private String billingId;
    private String patientId;
    private String createdBy;
    private String approvedBy;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private BigDecimal balance;
    private String status; // Draft, Pending, Approved, Sent, Paid, Overdue, Cancelled
    private String paymentMethod; // Cash, Card, BankTransfer, Insurance
    private LocalDate dueDate;
    private String branch;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime paidAt;
}

