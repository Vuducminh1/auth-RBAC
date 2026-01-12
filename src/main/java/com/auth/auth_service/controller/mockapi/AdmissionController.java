package com.auth.auth_service.controller.mockapi;

import com.auth.auth_service.dto.ApiResponse;
import com.auth.auth_service.dto.AuthorizationRequest;
import com.auth.auth_service.dto.AuthorizationResponse;
import com.auth.auth_service.dto.mock.AdmissionRecordDto;
import com.auth.auth_service.dto.mock.DischargeSummaryDto;
import com.auth.auth_service.dto.mock.TransferRecordDto;
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
 * Mock API Controller for Admission, Transfer, and Discharge records
 * AdmissionRecord: Receptionist (create, read), Doctor (read), Nurse (read)
 * TransferRecord: Receptionist (create, read), Doctor (read), Nurse (read)
 * DischargeSummary: Doctor (create, read), Nurse (read)
 */
@RestController
@RequestMapping("/api/mock/admissions")
@RequiredArgsConstructor
@Slf4j
public class AdmissionController {
    
    private final AuthorizationService authorizationService;
    
    private static final Map<String, AdmissionRecordDto> mockAdmissions = new HashMap<>();
    private static final Map<String, TransferRecordDto> mockTransfers = new HashMap<>();
    private static final Map<String, DischargeSummaryDto> mockDischargeSummaries = new HashMap<>();
    
    static {
        // Admission Records
        mockAdmissions.put("ADM001", AdmissionRecordDto.builder()
                .admissionId("ADM001")
                .patientId("PAT001")
                .admittedBy("REC001")
                .attendingDoctorId("DOC001")
                .admissionType("Elective")
                .status("Discharged")
                .chiefComplaint("Uncontrolled hypertension and diabetes")
                .admissionDiagnosis("Hypertension Stage 2, Type 2 DM")
                .roomNumber("301")
                .bedNumber("A")
                .branch("BRANCH_HCM")
                .department("Internal Medicine")
                .admittedAt(LocalDateTime.now().minusDays(7))
                .dischargedAt(LocalDateTime.now().minusDays(3))
                .build());
        
        mockAdmissions.put("ADM002", AdmissionRecordDto.builder()
                .admissionId("ADM002")
                .patientId("PAT002")
                .admittedBy("REC001")
                .attendingDoctorId("DOC002")
                .admissionType("Emergency")
                .status("Active")
                .chiefComplaint("Acute chest pain radiating to left arm")
                .admissionDiagnosis("NSTEMI")
                .roomNumber("CCU-01")
                .bedNumber("1")
                .branch("BRANCH_HCM")
                .department("Cardiology")
                .admittedAt(LocalDateTime.now().minusDays(1))
                .dischargedAt(null)
                .build());
        
        // Transfer Records
        mockTransfers.put("TR001", TransferRecordDto.builder()
                .transferId("TR001")
                .patientId("PAT002")
                .admissionId("ADM002")
                .initiatedBy("DOC002")
                .approvedBy("DOC003")
                .fromDepartment("Emergency")
                .toDepartment("Cardiology")
                .fromRoom("ER-05")
                .toRoom("CCU-01")
                .reason("Requires ICU level monitoring post-PCI")
                .status("Completed")
                .branch("BRANCH_HCM")
                .requestedAt(LocalDateTime.now().minusDays(1))
                .completedAt(LocalDateTime.now().minusDays(1).plusHours(2))
                .build());
        
        // Discharge Summaries
        mockDischargeSummaries.put("DS001", DischargeSummaryDto.builder()
                .summaryId("DS001")
                .admissionId("ADM001")
                .patientId("PAT001")
                .preparedBy("DOC001")
                .admittingDiagnosis("Hypertension Stage 2, Type 2 DM")
                .dischargeDiagnosis("Controlled Hypertension, Type 2 DM - improved")
                .proceduresPerformed("None")
                .hospitalCourse("Patient admitted for blood pressure and glucose optimization. " +
                        "Medications adjusted. Lifestyle counseling provided.")
                .conditionAtDischarge("Stable, improved")
                .dischargeInstructions(List.of(
                        "Continue Amlodipine 10mg daily",
                        "Continue Metformin 500mg BID",
                        "Low sodium, diabetic diet",
                        "Monitor BP daily at home",
                        "Return if BP > 160/100 or any symptoms"
                ))
                .medications(List.of("Amlodipine 10mg", "Metformin 500mg", "Atorvastatin 20mg"))
                .followUpAppointments("Internal Medicine clinic in 2 weeks")
                .branch("BRANCH_HCM")
                .department("Internal Medicine")
                .createdAt(LocalDateTime.now().minusDays(3))
                .build());
    }
    
    // ==================== Admission Records ====================
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdmissionRecordDto>>> getAllAdmissions(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String status) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "AdmissionRecord", "read", null, null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<AdmissionRecordDto> admissions = mockAdmissions.values().stream()
                .filter(a -> patientId == null || a.getPatientId().equals(patientId))
                .filter(a -> status == null || a.getStatus().equals(status))
                .filter(a -> user.getBranch().equals(a.getBranch()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Admissions retrieved", admissions));
    }
    
    @GetMapping("/{admissionId}")
    public ResponseEntity<ApiResponse<AdmissionRecordDto>> getAdmission(
            @PathVariable String admissionId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        AdmissionRecordDto admission = mockAdmissions.get(admissionId);
        if (admission == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Admission record not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "AdmissionRecord", "read", 
                admission.getBranch(), admission.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        return ResponseEntity.ok(ApiResponse.success(admission));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<AdmissionRecordDto>> createAdmission(
            @RequestBody(required = false) AdmissionRecordDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        // Auto-create request with defaults if null
        if (request == null) {
            request = new AdmissionRecordDto();
        }
        
        // Set default patientId if not provided
        if (request.getPatientId() == null || request.getPatientId().isEmpty()) {
            request.setPatientId("PAT001");
        }
        
        // Set default department if not provided
        if (request.getDepartment() == null || request.getDepartment().isEmpty()) {
            request.setDepartment(user.getDepartment() != null ? user.getDepartment() : "General");
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "AdmissionRecord", "create", 
                user.getBranch(), request.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        String admissionId = "ADM" + String.format("%03d", mockAdmissions.size() + 1);
        request.setAdmissionId(admissionId);
        request.setAdmittedBy(user.getUserId());
        request.setStatus("Active");
        request.setBranch(user.getBranch());
        request.setAdmittedAt(LocalDateTime.now());
        
        // Set defaults for optional fields
        if (request.getAdmissionType() == null) request.setAdmissionType("Elective");
        if (request.getChiefComplaint() == null) request.setChiefComplaint("General admission");
        if (request.getAdmissionDiagnosis() == null) request.setAdmissionDiagnosis("Pending diagnosis");
        if (request.getRoomNumber() == null) request.setRoomNumber("TBD");
        if (request.getBedNumber() == null) request.setBedNumber("TBD");
        
        mockAdmissions.put(admissionId, request);
        
        log.info("Admission created by {}: {}", user.getUserId(), admissionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Admission created", request));
    }
    
    // ==================== Transfer Records ====================
    
    @GetMapping("/transfers")
    public ResponseEntity<ApiResponse<List<TransferRecordDto>>> getAllTransfers(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String admissionId) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "TransferRecord", "read", null, null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<TransferRecordDto> transfers = mockTransfers.values().stream()
                .filter(t -> admissionId == null || t.getAdmissionId().equals(admissionId))
                .filter(t -> user.getBranch().equals(t.getBranch()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Transfers retrieved", transfers));
    }
    
    @PostMapping("/transfers")
    public ResponseEntity<ApiResponse<TransferRecordDto>> createTransfer(
            @RequestBody(required = false) TransferRecordDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        // Auto-create request with defaults if null
        if (request == null) {
            request = new TransferRecordDto();
        }
        
        // Set default patientId if not provided
        if (request.getPatientId() == null || request.getPatientId().isEmpty()) {
            request.setPatientId("PAT001");
        }
        
        // Set default toDepartment if not provided
        if (request.getToDepartment() == null || request.getToDepartment().isEmpty()) {
            request.setToDepartment("Internal Medicine");
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "TransferRecord", "create", 
                user.getBranch(), request.getToDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        String transferId = "TR" + String.format("%03d", mockTransfers.size() + 1);
        request.setTransferId(transferId);
        request.setInitiatedBy(user.getUserId());
        request.setStatus("Pending");
        request.setBranch(user.getBranch());
        request.setRequestedAt(LocalDateTime.now());
        
        // Set defaults for optional fields
        if (request.getFromDepartment() == null) request.setFromDepartment(user.getDepartment() != null ? user.getDepartment() : "Current");
        if (request.getFromRoom() == null) request.setFromRoom("TBD");
        if (request.getToRoom() == null) request.setToRoom("TBD");
        if (request.getReason() == null) request.setReason("Transfer requested");
        
        mockTransfers.put(transferId, request);
        
        log.info("Transfer request created by {}: {}", user.getUserId(), transferId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transfer request created", request));
    }
    
    // ==================== Discharge Summaries ====================
    
    @GetMapping("/discharge-summaries")
    public ResponseEntity<ApiResponse<List<DischargeSummaryDto>>> getAllDischargeSummaries(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String admissionId) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "DischargeSummary", "read", null, null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<DischargeSummaryDto> summaries = mockDischargeSummaries.values().stream()
                .filter(s -> patientId == null || s.getPatientId().equals(patientId))
                .filter(s -> admissionId == null || s.getAdmissionId().equals(admissionId))
                .filter(s -> user.getBranch().equals(s.getBranch()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Discharge summaries retrieved", summaries));
    }
    
    @GetMapping("/discharge-summaries/{summaryId}")
    public ResponseEntity<ApiResponse<DischargeSummaryDto>> getDischargeSummary(
            @PathVariable String summaryId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        DischargeSummaryDto summary = mockDischargeSummaries.get(summaryId);
        if (summary == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Discharge summary not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "DischargeSummary", "read", 
                summary.getBranch(), summary.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
    
    @PostMapping("/discharge-summaries")
    public ResponseEntity<ApiResponse<DischargeSummaryDto>> createDischargeSummary(
            @RequestBody(required = false) DischargeSummaryDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        // Auto-create request with defaults if null
        if (request == null) {
            request = new DischargeSummaryDto();
        }
        
        // Set default patientId if not provided
        if (request.getPatientId() == null || request.getPatientId().isEmpty()) {
            request.setPatientId("PAT001");
        }
        
        // Set default admissionId if not provided
        if (request.getAdmissionId() == null || request.getAdmissionId().isEmpty()) {
            request.setAdmissionId("ADM001");
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "DischargeSummary", "create", 
                user.getBranch(), user.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        String summaryId = "DS" + String.format("%03d", mockDischargeSummaries.size() + 1);
        request.setSummaryId(summaryId);
        request.setPreparedBy(user.getUserId());
        request.setBranch(user.getBranch());
        request.setDepartment(user.getDepartment());
        request.setCreatedAt(LocalDateTime.now());
        
        // Set defaults for optional fields
        if (request.getAdmittingDiagnosis() == null) request.setAdmittingDiagnosis("General admission");
        if (request.getDischargeDiagnosis() == null) request.setDischargeDiagnosis("Condition improved");
        if (request.getHospitalCourse() == null) request.setHospitalCourse("Uncomplicated hospital course");
        if (request.getConditionAtDischarge() == null) request.setConditionAtDischarge("Stable");
        if (request.getDischargeInstructions() == null) request.setDischargeInstructions(List.of("Follow up in 2 weeks"));
        if (request.getMedications() == null) request.setMedications(List.of());
        
        // Update admission status
        AdmissionRecordDto admission = mockAdmissions.get(request.getAdmissionId());
        if (admission != null) {
            admission.setStatus("Discharged");
            admission.setDischargedAt(LocalDateTime.now());
        }
        
        mockDischargeSummaries.put(summaryId, request);
        
        log.info("Discharge summary created by {}: {}", user.getUserId(), summaryId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Discharge summary created", request));
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

