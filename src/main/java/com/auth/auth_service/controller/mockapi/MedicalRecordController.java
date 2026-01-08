package com.auth.auth_service.controller.mockapi;

import com.auth.auth_service.dto.ApiResponse;
import com.auth.auth_service.dto.AuthorizationRequest;
import com.auth.auth_service.dto.AuthorizationResponse;
import com.auth.auth_service.dto.mock.MedicalRecordDto;
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
 * Mock API Controller for Medical Records
 * Demonstrates RBAC + ABAC authorization for clinical data
 * Accessible by: Doctor (full), Nurse (read only)
 */
@RestController
@RequestMapping("/api/mock/medical-records")
@RequiredArgsConstructor
@Slf4j
public class MedicalRecordController {
    
    private final AuthorizationService authorizationService;
    
    private static final Map<String, MedicalRecordDto> mockRecords = new HashMap<>();
    
    static {
        mockRecords.put("MR001", MedicalRecordDto.builder()
                .recordId("MR001")
                .patientId("PAT001")
                .doctorId("DOC001")
                .diagnosis("Hypertension Stage 1")
                .symptoms("Headache, dizziness, fatigue")
                .treatment("Lifestyle modification, Amlodipine 5mg daily")
                .notes("Patient advised to reduce sodium intake")
                .sensitivity("Normal")
                .branch("BRANCH_HCM")
                .department("Internal Medicine")
                .visitDate(LocalDateTime.now().minusDays(7))
                .createdAt(LocalDateTime.now().minusDays(7))
                .updatedAt(LocalDateTime.now())
                .build());
        
        mockRecords.put("MR002", MedicalRecordDto.builder()
                .recordId("MR002")
                .patientId("PAT001")
                .doctorId("DOC001")
                .diagnosis("Type 2 Diabetes Mellitus")
                .symptoms("Polyuria, polydipsia, weight loss")
                .treatment("Metformin 500mg BID, Diet control")
                .notes("HbA1c: 8.2% - Needs better glycemic control")
                .sensitivity("High")
                .branch("BRANCH_HCM")
                .department("Internal Medicine")
                .visitDate(LocalDateTime.now().minusDays(3))
                .createdAt(LocalDateTime.now().minusDays(3))
                .updatedAt(LocalDateTime.now())
                .build());
        
        mockRecords.put("MR003", MedicalRecordDto.builder()
                .recordId("MR003")
                .patientId("PAT002")
                .doctorId("DOC002")
                .diagnosis("Acute Myocardial Infarction - NSTEMI")
                .symptoms("Chest pain, shortness of breath, diaphoresis")
                .treatment("PCI performed, Dual antiplatelet therapy")
                .notes("Transferred to CCU for monitoring")
                .sensitivity("High")
                .branch("BRANCH_HCM")
                .department("Cardiology")
                .visitDate(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build());
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<MedicalRecordDto>>> getAllRecords(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String patientId) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "MedicalRecord", "read", null, null, null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<MedicalRecordDto> records = mockRecords.values().stream()
                .filter(r -> patientId == null || r.getPatientId().equals(patientId))
                .filter(r -> canAccessRecord(user, r))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Medical records retrieved", records));
    }
    
    @GetMapping("/{recordId}")
    public ResponseEntity<ApiResponse<MedicalRecordDto>> getRecord(
            @PathVariable String recordId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        MedicalRecordDto record = mockRecords.get(recordId);
        if (record == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Medical record not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "MedicalRecord", "read", 
                record.getBranch(), record.getDepartment(), record.getPatientId());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        return ResponseEntity.ok(ApiResponse.success(record));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<MedicalRecordDto>> createRecord(
            @RequestBody MedicalRecordDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "MedicalRecord", "create", 
                user.getBranch(), user.getDepartment(), request.getPatientId());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        String recordId = "MR" + String.format("%03d", mockRecords.size() + 1);
        request.setRecordId(recordId);
        request.setDoctorId(user.getUserId());
        request.setBranch(user.getBranch());
        request.setDepartment(user.getDepartment());
        request.setVisitDate(LocalDateTime.now());
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        
        mockRecords.put(recordId, request);
        
        log.info("Medical record created by {}: {}", user.getUserId(), recordId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Medical record created", request));
    }
    
    @PutMapping("/{recordId}")
    public ResponseEntity<ApiResponse<MedicalRecordDto>> updateRecord(
            @PathVariable String recordId,
            @RequestBody MedicalRecordDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        MedicalRecordDto existing = mockRecords.get(recordId);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Medical record not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "MedicalRecord", "update",
                existing.getBranch(), existing.getDepartment(), existing.getPatientId());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        existing.setDiagnosis(request.getDiagnosis() != null ? request.getDiagnosis() : existing.getDiagnosis());
        existing.setSymptoms(request.getSymptoms() != null ? request.getSymptoms() : existing.getSymptoms());
        existing.setTreatment(request.getTreatment() != null ? request.getTreatment() : existing.getTreatment());
        existing.setNotes(request.getNotes() != null ? request.getNotes() : existing.getNotes());
        existing.setUpdatedAt(LocalDateTime.now());
        
        log.info("Medical record updated by {}: {}", user.getUserId(), recordId);
        return ResponseEntity.ok(ApiResponse.success("Medical record updated", existing));
    }
    
    @PostMapping("/{recordId}/export")
    public ResponseEntity<ApiResponse<Map<String, Object>>> exportRecord(
            @PathVariable String recordId,
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false, defaultValue = "false") boolean emergencyMode,
            @RequestParam(required = false) String approvalTicketId) {
        
        MedicalRecordDto existing = mockRecords.get(recordId);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Medical record not found"));
        }
        
        // Build environment context for export action
        Map<String, Object> environment = new HashMap<>();
        environment.put("emergency_mode", emergencyMode);
        environment.put("export_approved", approvalTicketId != null && !approvalTicketId.isEmpty());
        environment.put("approval_ticket_id", approvalTicketId);
        
        AuthorizationRequest authRequest = AuthorizationRequest.builder()
                .resourceType("MedicalRecord")
                .action("export")
                .resourceId(recordId)
                .resourceBranch(existing.getBranch())
                .resourceDepartment(existing.getDepartment())
                .patientId(existing.getPatientId())
                .resourceSensitivity(existing.getSensitivity())
                .environment(environment)
                .build();
        
        AuthorizationResponse authResponse = authorizationService.authorize(authRequest);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Export denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        // Mock export response
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("recordId", existing.getRecordId());
        exportData.put("exportedAt", LocalDateTime.now());
        exportData.put("exportedBy", user.getUserId());
        exportData.put("format", "PDF");
        exportData.put("downloadUrl", "/api/mock/exports/" + recordId + ".pdf");
        
        log.info("Medical record exported by {}: {} (emergency={})", user.getUserId(), recordId, emergencyMode);
        return ResponseEntity.ok(ApiResponse.success("Export initiated", exportData));
    }
    
    private AuthorizationResponse checkAuthorization(UserPrincipal user, String resourceType, 
                                                     String action, String resourceBranch,
                                                     String resourceDepartment, String patientId) {
        AuthorizationRequest request = AuthorizationRequest.builder()
                .resourceType(resourceType)
                .action(action)
                .resourceBranch(resourceBranch)
                .resourceDepartment(resourceDepartment)
                .patientId(patientId)
                .build();
        
        return authorizationService.authorize(request);
    }
    
    private boolean canAccessRecord(UserPrincipal user, MedicalRecordDto record) {
        String role = user.getRole();
        
        // Doctors can access records of their assigned patients
        if ("Doctor".equals(role)) {
            return user.getAssignedPatients() == null || 
                   user.getAssignedPatients().contains(record.getPatientId());
        }
        
        // Nurses can access records in their department
        if ("Nurse".equals(role)) {
            return user.getDepartment().equals(record.getDepartment()) &&
                   user.getBranch().equals(record.getBranch());
        }
        
        return false;
    }
}

