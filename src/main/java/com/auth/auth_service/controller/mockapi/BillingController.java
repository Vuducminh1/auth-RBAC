package com.auth.auth_service.controller.mockapi;

import com.auth.auth_service.dto.ApiResponse;
import com.auth.auth_service.dto.AuthorizationRequest;
import com.auth.auth_service.dto.AuthorizationResponse;
import com.auth.auth_service.dto.mock.BillingRecordDto;
import com.auth.auth_service.dto.mock.InsuranceClaimDto;
import com.auth.auth_service.dto.mock.InvoiceDto;
import com.auth.auth_service.dto.mock.ReportDto;
import com.auth.auth_service.security.UserPrincipal;
import com.auth.auth_service.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Mock API Controller for Billing, Invoices, Insurance Claims, and Financial Reports
 * Accessible by: Cashier (create, read, update)
 * FinancialReport: Cashier (read), Manager (read)
 */
@RestController
@RequestMapping("/api/mock/billing")
@RequiredArgsConstructor
@Slf4j
public class BillingController {
    
    private final AuthorizationService authorizationService;
    
    private static final Map<String, BillingRecordDto> mockBillings = new HashMap<>();
    private static final Map<String, InvoiceDto> mockInvoices = new HashMap<>();
    private static final Map<String, InsuranceClaimDto> mockClaims = new HashMap<>();
    private static final Map<String, ReportDto> mockFinancialReports = new HashMap<>();
    
    static {
        // Billing Records
        mockBillings.put("BL001", BillingRecordDto.builder()
                .billingId("BL001")
                .patientId("PAT001")
                .admissionId("ADM001")
                .createdBy("CSH001")
                .items(List.of(
                        BillingRecordDto.BillingItem.builder()
                                .itemCode("RM001")
                                .description("Room charge - Ward (5 days)")
                                .quantity(5)
                                .unitPrice(BigDecimal.valueOf(500000))
                                .amount(BigDecimal.valueOf(2500000))
                                .category("Room")
                                .build(),
                        BillingRecordDto.BillingItem.builder()
                                .itemCode("LAB001")
                                .description("Laboratory tests")
                                .quantity(1)
                                .unitPrice(BigDecimal.valueOf(1200000))
                                .amount(BigDecimal.valueOf(1200000))
                                .category("Lab")
                                .build(),
                        BillingRecordDto.BillingItem.builder()
                                .itemCode("MED001")
                                .description("Medications")
                                .quantity(1)
                                .unitPrice(BigDecimal.valueOf(800000))
                                .amount(BigDecimal.valueOf(800000))
                                .category("Medication")
                                .build()
                ))
                .subtotal(BigDecimal.valueOf(4500000))
                .discount(BigDecimal.valueOf(450000))
                .tax(BigDecimal.valueOf(405000))
                .total(BigDecimal.valueOf(4455000))
                .status("Finalized")
                .branch("BRANCH_HCM")
                .createdAt(LocalDateTime.now().minusDays(3))
                .updatedAt(LocalDateTime.now().minusDays(3))
                .build());
        
        // Invoices
        mockInvoices.put("INV001", InvoiceDto.builder()
                .invoiceId("INV001")
                .billingId("BL001")
                .patientId("PAT001")
                .createdBy("CSH001")
                .approvedBy("CSH002")
                .amount(BigDecimal.valueOf(4455000))
                .paidAmount(BigDecimal.valueOf(4455000))
                .balance(BigDecimal.ZERO)
                .status("Paid")
                .paymentMethod("BankTransfer")
                .dueDate(LocalDate.now().plusDays(30))
                .branch("BRANCH_HCM")
                .createdAt(LocalDateTime.now().minusDays(3))
                .approvedAt(LocalDateTime.now().minusDays(3))
                .paidAt(LocalDateTime.now().minusDays(2))
                .build());
        
        // Insurance Claims
        mockClaims.put("IC001", InsuranceClaimDto.builder()
                .claimId("IC001")
                .invoiceId("INV001")
                .patientId("PAT001")
                .insuranceProvider("Bảo hiểm Bảo Việt")
                .policyNumber("BV-2024-123456")
                .createdBy("CSH001")
                .approvedBy(null)
                .claimAmount(BigDecimal.valueOf(3500000))
                .approvedAmount(null)
                .status("Submitted")
                .rejectionReason(null)
                .branch("BRANCH_HCM")
                .createdAt(LocalDateTime.now().minusDays(2))
                .submittedAt(LocalDateTime.now().minusDays(2))
                .approvedAt(null)
                .build());
        
        // Financial Reports
        mockFinancialReports.put("FR001", ReportDto.builder()
                .reportId("FR001")
                .reportType("FinancialReport")
                .reportName("Monthly Revenue Report - December 2025")
                .generatedBy("CSH001")
                .periodStart("2025-12-01")
                .periodEnd("2025-12-31")
                .status("Final")
                .data(Map.of(
                        "totalRevenue", 1250000000,
                        "totalExpenses", 850000000,
                        "netProfit", 400000000,
                        "outpatientRevenue", 450000000,
                        "inpatientRevenue", 800000000,
                        "insuranceCollected", 650000000,
                        "cashCollected", 600000000
                ))
                .summary("Strong month with 12% revenue growth compared to previous month")
                .branch("BRANCH_HCM")
                .department(null)
                .generatedAt(LocalDateTime.now().minusDays(1))
                .build());
    }
    
    // ==================== Billing Records ====================
    
    @GetMapping("/records")
    public ResponseEntity<ApiResponse<List<BillingRecordDto>>> getAllBillingRecords(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String status) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "BillingRecord", "read", null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<BillingRecordDto> records = mockBillings.values().stream()
                .filter(b -> patientId == null || b.getPatientId().equals(patientId))
                .filter(b -> status == null || b.getStatus().equals(status))
                .filter(b -> user.getBranch().equals(b.getBranch()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Billing records retrieved", records));
    }
    
    @GetMapping("/records/{billingId}")
    public ResponseEntity<ApiResponse<BillingRecordDto>> getBillingRecord(
            @PathVariable String billingId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        BillingRecordDto billing = mockBillings.get(billingId);
        if (billing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Billing record not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "BillingRecord", "read", billing.getBranch());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        return ResponseEntity.ok(ApiResponse.success(billing));
    }
    
    @PostMapping("/records")
    public ResponseEntity<ApiResponse<BillingRecordDto>> createBillingRecord(
            @RequestBody(required = false) BillingRecordDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        // Auto-create request with defaults if null
        if (request == null) {
            request = new BillingRecordDto();
        }
        
        // Set default patientId if not provided
        if (request.getPatientId() == null || request.getPatientId().isEmpty()) {
            request.setPatientId("PAT001");
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "BillingRecord", "create", user.getBranch());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        String billingId = "BL" + String.format("%03d", mockBillings.size() + 1);
        request.setBillingId(billingId);
        request.setCreatedBy(user.getUserId());
        request.setStatus("Draft");
        request.setBranch(user.getBranch());
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        
        // Set default items if not provided
        if (request.getItems() == null || request.getItems().isEmpty()) {
            request.setItems(List.of(
                BillingRecordDto.BillingItem.builder()
                    .itemCode("CONS001")
                    .description("Consultation fee")
                    .quantity(1)
                    .unitPrice(BigDecimal.valueOf(200000))
                    .amount(BigDecimal.valueOf(200000))
                    .category("Consultation")
                    .build()
            ));
        }
        
        // Calculate totals if not provided
        if (request.getSubtotal() == null) {
            BigDecimal subtotal = request.getItems().stream()
                .map(BillingRecordDto.BillingItem::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            request.setSubtotal(subtotal);
        }
        if (request.getDiscount() == null) request.setDiscount(BigDecimal.ZERO);
        if (request.getTax() == null) request.setTax(request.getSubtotal().multiply(BigDecimal.valueOf(0.1)));
        if (request.getTotal() == null) {
            request.setTotal(request.getSubtotal().subtract(request.getDiscount()).add(request.getTax()));
        }
        
        mockBillings.put(billingId, request);
        
        log.info("Billing record created by {}: {}", user.getUserId(), billingId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Billing record created", request));
    }
    
    @PutMapping("/records/{billingId}")
    public ResponseEntity<ApiResponse<BillingRecordDto>> updateBillingRecord(
            @PathVariable String billingId,
            @RequestBody BillingRecordDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        BillingRecordDto existing = mockBillings.get(billingId);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Billing record not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "BillingRecord", "update", existing.getBranch());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        if (request.getItems() != null) existing.setItems(request.getItems());
        if (request.getStatus() != null) existing.setStatus(request.getStatus());
        if (request.getDiscount() != null) existing.setDiscount(request.getDiscount());
        existing.setUpdatedAt(LocalDateTime.now());
        
        log.info("Billing record updated by {}: {}", user.getUserId(), billingId);
        return ResponseEntity.ok(ApiResponse.success("Billing record updated", existing));
    }
    
    // ==================== Invoices ====================
    
    @GetMapping("/invoices")
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getAllInvoices(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String status) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "Invoice", "read", null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<InvoiceDto> invoices = mockInvoices.values().stream()
                .filter(i -> patientId == null || i.getPatientId().equals(patientId))
                .filter(i -> status == null || i.getStatus().equals(status))
                .filter(i -> user.getBranch().equals(i.getBranch()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Invoices retrieved", invoices));
    }
    
    @PostMapping("/invoices")
    public ResponseEntity<ApiResponse<InvoiceDto>> createInvoice(
            @RequestBody(required = false) InvoiceDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        // Auto-create request with defaults if null
        if (request == null) {
            request = new InvoiceDto();
        }
        
        // Set default patientId if not provided
        if (request.getPatientId() == null || request.getPatientId().isEmpty()) {
            request.setPatientId("PAT001");
        }
        
        // Set default billingId if not provided
        if (request.getBillingId() == null || request.getBillingId().isEmpty()) {
            request.setBillingId("BL001");
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "Invoice", "create", user.getBranch());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        String invoiceId = "INV" + String.format("%03d", mockInvoices.size() + 1);
        request.setInvoiceId(invoiceId);
        request.setCreatedBy(user.getUserId());
        request.setStatus("Pending");
        request.setBranch(user.getBranch());
        request.setCreatedAt(LocalDateTime.now());
        
        // Set defaults for optional fields
        if (request.getAmount() == null) request.setAmount(BigDecimal.valueOf(200000));
        if (request.getPaidAmount() == null) request.setPaidAmount(BigDecimal.ZERO);
        if (request.getBalance() == null) request.setBalance(request.getAmount().subtract(request.getPaidAmount()));
        if (request.getDueDate() == null) request.setDueDate(LocalDate.now().plusDays(30));
        
        mockInvoices.put(invoiceId, request);
        
        log.info("Invoice created by {}: {}", user.getUserId(), invoiceId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Invoice created", request));
    }
    
    @PostMapping("/invoices/{invoiceId}/approve")
    public ResponseEntity<ApiResponse<InvoiceDto>> approveInvoice(
            @PathVariable String invoiceId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        InvoiceDto existing = mockInvoices.get(invoiceId);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Invoice not found"));
        }
        
        // SoD check
        AuthorizationRequest authRequest = AuthorizationRequest.builder()
                .resourceType("Invoice")
                .action("approve")
                .resourceId(invoiceId)
                .resourceBranch(existing.getBranch())
                .createdBy(existing.getCreatedBy())
                .build();
        
        AuthorizationResponse authResponse = authorizationService.authorize(authRequest);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        existing.setStatus("Approved");
        existing.setApprovedBy(user.getUserId());
        existing.setApprovedAt(LocalDateTime.now());
        
        log.info("Invoice approved by {}: {}", user.getUserId(), invoiceId);
        return ResponseEntity.ok(ApiResponse.success("Invoice approved", existing));
    }
    
    // ==================== Insurance Claims ====================
    
    @GetMapping("/claims")
    public ResponseEntity<ApiResponse<List<InsuranceClaimDto>>> getAllClaims(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String status) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "InsuranceClaim", "read", null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<InsuranceClaimDto> claims = mockClaims.values().stream()
                .filter(c -> patientId == null || c.getPatientId().equals(patientId))
                .filter(c -> status == null || c.getStatus().equals(status))
                .filter(c -> user.getBranch().equals(c.getBranch()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Claims retrieved", claims));
    }
    
    @PostMapping("/claims")
    public ResponseEntity<ApiResponse<InsuranceClaimDto>> createClaim(
            @RequestBody(required = false) InsuranceClaimDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        // Auto-create request with defaults if null
        if (request == null) {
            request = new InsuranceClaimDto();
        }
        
        // Set default patientId if not provided
        if (request.getPatientId() == null || request.getPatientId().isEmpty()) {
            request.setPatientId("PAT001");
        }
        
        // Set default invoiceId if not provided
        if (request.getInvoiceId() == null || request.getInvoiceId().isEmpty()) {
            request.setInvoiceId("INV001");
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "InsuranceClaim", "create", user.getBranch());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        String claimId = "IC" + String.format("%03d", mockClaims.size() + 1);
        request.setClaimId(claimId);
        request.setCreatedBy(user.getUserId());
        request.setStatus("Draft");
        request.setBranch(user.getBranch());
        request.setCreatedAt(LocalDateTime.now());
        
        // Set defaults for optional fields
        if (request.getInsuranceProvider() == null) request.setInsuranceProvider("Sample Insurance Co.");
        if (request.getPolicyNumber() == null) request.setPolicyNumber("POL-" + System.currentTimeMillis());
        if (request.getClaimAmount() == null) request.setClaimAmount(BigDecimal.valueOf(100000));
        
        mockClaims.put(claimId, request);
        
        log.info("Insurance claim created by {}: {}", user.getUserId(), claimId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Insurance claim created", request));
    }
    
    // ==================== Financial Reports ====================
    
    @GetMapping("/reports/financial")
    public ResponseEntity<ApiResponse<List<ReportDto>>> getAllFinancialReports(
            @AuthenticationPrincipal UserPrincipal user) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "FinancialReport", "read", null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<ReportDto> reports = mockFinancialReports.values().stream()
                .filter(r -> user.getBranch().equals(r.getBranch()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Financial reports retrieved", reports));
    }
    
    @GetMapping("/reports/financial/{reportId}")
    public ResponseEntity<ApiResponse<ReportDto>> getFinancialReport(
            @PathVariable String reportId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        ReportDto report = mockFinancialReports.get(reportId);
        if (report == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Report not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "FinancialReport", "read", report.getBranch());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        return ResponseEntity.ok(ApiResponse.success(report));
    }
    
    private AuthorizationResponse checkAuthorization(UserPrincipal user, String resourceType, 
                                                     String action, String resourceBranch) {
        AuthorizationRequest request = AuthorizationRequest.builder()
                .resourceType(resourceType)
                .action(action)
                .resourceBranch(resourceBranch)
                .build();
        
        return authorizationService.authorize(request);
    }
}

