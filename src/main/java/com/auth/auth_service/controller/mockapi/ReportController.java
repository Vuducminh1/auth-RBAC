package com.auth.auth_service.controller.mockapi;

import com.auth.auth_service.dto.ApiResponse;
import com.auth.auth_service.dto.AuthorizationRequest;
import com.auth.auth_service.dto.AuthorizationResponse;
import com.auth.auth_service.dto.mock.ReportDto;
import com.auth.auth_service.security.UserPrincipal;
import com.auth.auth_service.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Mock API Controller for Medical Reports
 * MedicalReport: Doctor (read), Manager (read)
 */
@RestController
@RequestMapping("/api/mock/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {
    
    private final AuthorizationService authorizationService;
    
    private static final Map<String, ReportDto> mockMedicalReports = new HashMap<>();
    
    static {
        mockMedicalReports.put("MED-RPT001", ReportDto.builder()
                .reportId("MED-RPT001")
                .reportType("MedicalReport")
                .reportName("Monthly Patient Statistics - December 2025")
                .generatedBy("DOC001")
                .periodStart("2025-12-01")
                .periodEnd("2025-12-31")
                .status("Final")
                .data(Map.of(
                        "totalPatients", 1250,
                        "newPatients", 180,
                        "outpatientVisits", 3500,
                        "inpatientAdmissions", 150,
                        "averageLengthOfStay", 4.5,
                        "readmissionRate", 3.2,
                        "topDiagnoses", List.of(
                                "Hypertension", "Type 2 Diabetes", "URTI", "Gastritis", "Back Pain"
                        )
                ))
                .summary("Patient volume increased by 8% compared to previous month")
                .branch("BRANCH_HCM")
                .department("Internal Medicine")
                .generatedAt(LocalDateTime.now().minusDays(1))
                .build());
        
        mockMedicalReports.put("MED-RPT002", ReportDto.builder()
                .reportId("MED-RPT002")
                .reportType("MedicalReport")
                .reportName("Cardiology Department Performance - Q4 2025")
                .generatedBy("DOC002")
                .periodStart("2025-10-01")
                .periodEnd("2025-12-31")
                .status("Final")
                .data(Map.of(
                        "totalProcedures", 245,
                        "angioplastyCount", 85,
                        "echocardiograms", 450,
                        "stressTests", 320,
                        "mortalityRate", 1.2,
                        "complicationRate", 2.8,
                        "averageWaitTime", 3.5
                ))
                .summary("Cardiology department exceeding performance targets")
                .branch("BRANCH_HCM")
                .department("Cardiology")
                .generatedAt(LocalDateTime.now().minusDays(2))
                .build());
        
        mockMedicalReports.put("MED-RPT003", ReportDto.builder()
                .reportId("MED-RPT003")
                .reportType("MedicalReport")
                .reportName("Emergency Department Metrics - December 2025")
                .generatedBy("DOC003")
                .periodStart("2025-12-01")
                .periodEnd("2025-12-31")
                .status("Draft")
                .data(Map.of(
                        "totalVisits", 2800,
                        "averageTriageTime", 8,
                        "averageWaitTime", 45,
                        "leftWithoutSeen", 35,
                        "admissionFromED", 380,
                        "criticalCases", 120,
                        "resuscitations", 15
                ))
                .summary("ED volumes remain high, staffing adjustments recommended")
                .branch("BRANCH_HCM")
                .department("Emergency")
                .generatedAt(LocalDateTime.now())
                .build());
    }
    
    @GetMapping("/medical")
    public ResponseEntity<ApiResponse<List<ReportDto>>> getAllMedicalReports(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "MedicalReport", "read", null, null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<ReportDto> reports = mockMedicalReports.values().stream()
                .filter(r -> department == null || r.getDepartment().equals(department))
                .filter(r -> status == null || r.getStatus().equals(status))
                .filter(r -> user.getBranch().equals(r.getBranch()))
                // Manager can only see reports from their department
                .filter(r -> !"Manager".equals(user.getRole()) || 
                        r.getDepartment() == null || r.getDepartment().equals(user.getDepartment()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Medical reports retrieved", reports));
    }
    
    @GetMapping("/medical/{reportId}")
    public ResponseEntity<ApiResponse<ReportDto>> getMedicalReport(
            @PathVariable String reportId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        ReportDto report = mockMedicalReports.get(reportId);
        if (report == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Report not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "MedicalReport", "read", 
                report.getBranch(), report.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        return ResponseEntity.ok(ApiResponse.success(report));
    }
    
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReportSummary(
            @AuthenticationPrincipal UserPrincipal user) {
        
        // Check if user can read at least one type of report
        boolean canReadMedical = authorizationService.hasPermission("MedicalReport", "read");
        boolean canReadFinancial = authorizationService.hasPermission("FinancialReport", "read");
        boolean canReadOperation = authorizationService.hasPermission("OperationReport", "read");
        
        if (!canReadMedical && !canReadFinancial && !canReadOperation) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: No report access"));
        }
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("canAccessMedicalReports", canReadMedical);
        summary.put("canAccessFinancialReports", canReadFinancial);
        summary.put("canAccessOperationReports", canReadOperation);
        summary.put("userRole", user.getRole());
        summary.put("userBranch", user.getBranch());
        summary.put("userDepartment", user.getDepartment());
        
        if (canReadMedical) {
            summary.put("medicalReportCount", mockMedicalReports.size());
        }
        
        return ResponseEntity.ok(ApiResponse.success("Report summary", summary));
    }
    
    private AuthorizationResponse checkAuthorization(UserPrincipal user, String resourceType, 
                                                     String action, String resourceBranch,
                                                     String resourceDepartment) {
        AuthorizationRequest request = AuthorizationRequest.builder()
                .resourceType(resourceType)
                .action(action)
                .resourceBranch(resourceBranch)
                .resourceDepartment(resourceDepartment)
                .build();
        
        return authorizationService.authorize(request);
    }
}

