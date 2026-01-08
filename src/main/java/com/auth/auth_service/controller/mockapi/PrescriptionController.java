package com.auth.auth_service.controller.mockapi;

import com.auth.auth_service.dto.ApiResponse;
import com.auth.auth_service.dto.AuthorizationRequest;
import com.auth.auth_service.dto.AuthorizationResponse;
import com.auth.auth_service.dto.mock.PrescriptionDto;
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
 * Mock API Controller for Prescriptions
 * Accessible by: Doctor (read, create, update, approve)
 * SoD rule: Creator cannot approve their own prescription
 */
@RestController
@RequestMapping("/api/mock/prescriptions")
@RequiredArgsConstructor
@Slf4j
public class PrescriptionController {
    
    private final AuthorizationService authorizationService;
    
    private static final Map<String, PrescriptionDto> mockPrescriptions = new HashMap<>();
    
    static {
        mockPrescriptions.put("RX001", PrescriptionDto.builder()
                .prescriptionId("RX001")
                .patientId("PAT001")
                .doctorId("DOC001")
                .approvedBy(null)
                .status("Pending")
                .medications(List.of(
                        PrescriptionDto.MedicationItem.builder()
                                .medicationName("Amlodipine")
                                .dosage("5mg")
                                .frequency("Once daily")
                                .duration(30)
                                .route("Oral")
                                .instructions("Take in the morning")
                                .build(),
                        PrescriptionDto.MedicationItem.builder()
                                .medicationName("Metformin")
                                .dosage("500mg")
                                .frequency("Twice daily")
                                .duration(30)
                                .route("Oral")
                                .instructions("Take with meals")
                                .build()
                ))
                .instructions("Monitor blood pressure daily")
                .notes("Review in 2 weeks")
                .branch("BRANCH_HCM")
                .department("Internal Medicine")
                .createdAt(LocalDateTime.now().minusDays(2))
                .approvedAt(null)
                .build());
        
        mockPrescriptions.put("RX002", PrescriptionDto.builder()
                .prescriptionId("RX002")
                .patientId("PAT002")
                .doctorId("DOC002")
                .approvedBy("DOC003")
                .status("Approved")
                .medications(List.of(
                        PrescriptionDto.MedicationItem.builder()
                                .medicationName("Aspirin")
                                .dosage("100mg")
                                .frequency("Once daily")
                                .duration(90)
                                .route("Oral")
                                .instructions("Take with food")
                                .build(),
                        PrescriptionDto.MedicationItem.builder()
                                .medicationName("Clopidogrel")
                                .dosage("75mg")
                                .frequency("Once daily")
                                .duration(365)
                                .route("Oral")
                                .instructions("Do not stop without medical advice")
                                .build()
                ))
                .instructions("Dual antiplatelet therapy post-PCI")
                .notes("Annual review required")
                .branch("BRANCH_HCM")
                .department("Cardiology")
                .createdAt(LocalDateTime.now().minusDays(1))
                .approvedAt(LocalDateTime.now().minusHours(12))
                .build());
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<PrescriptionDto>>> getAllPrescriptions(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String status) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "Prescription", "read", null, null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<PrescriptionDto> prescriptions = mockPrescriptions.values().stream()
                .filter(p -> patientId == null || p.getPatientId().equals(patientId))
                .filter(p -> status == null || p.getStatus().equals(status))
                .filter(p -> user.getBranch().equals(p.getBranch()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Prescriptions retrieved", prescriptions));
    }
    
    @GetMapping("/{prescriptionId}")
    public ResponseEntity<ApiResponse<PrescriptionDto>> getPrescription(
            @PathVariable String prescriptionId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        PrescriptionDto prescription = mockPrescriptions.get(prescriptionId);
        if (prescription == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Prescription not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "Prescription", "read", 
                prescription.getBranch(), prescription.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        return ResponseEntity.ok(ApiResponse.success(prescription));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<PrescriptionDto>> createPrescription(
            @RequestBody PrescriptionDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "Prescription", "create", 
                user.getBranch(), user.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        String prescriptionId = "RX" + String.format("%03d", mockPrescriptions.size() + 1);
        request.setPrescriptionId(prescriptionId);
        request.setDoctorId(user.getUserId());
        request.setStatus("Pending");
        request.setBranch(user.getBranch());
        request.setDepartment(user.getDepartment());
        request.setCreatedAt(LocalDateTime.now());
        
        mockPrescriptions.put(prescriptionId, request);
        
        log.info("Prescription created by {}: {}", user.getUserId(), prescriptionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Prescription created", request));
    }
    
    @PutMapping("/{prescriptionId}")
    public ResponseEntity<ApiResponse<PrescriptionDto>> updatePrescription(
            @PathVariable String prescriptionId,
            @RequestBody PrescriptionDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        PrescriptionDto existing = mockPrescriptions.get(prescriptionId);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Prescription not found"));
        }
        
        if ("Approved".equals(existing.getStatus()) || "Dispensed".equals(existing.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Cannot update approved/dispensed prescription"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "Prescription", "update",
                existing.getBranch(), existing.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        if (request.getMedications() != null) existing.setMedications(request.getMedications());
        if (request.getInstructions() != null) existing.setInstructions(request.getInstructions());
        if (request.getNotes() != null) existing.setNotes(request.getNotes());
        
        log.info("Prescription updated by {}: {}", user.getUserId(), prescriptionId);
        return ResponseEntity.ok(ApiResponse.success("Prescription updated", existing));
    }
    
    @PostMapping("/{prescriptionId}/approve")
    public ResponseEntity<ApiResponse<PrescriptionDto>> approvePrescription(
            @PathVariable String prescriptionId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        PrescriptionDto existing = mockPrescriptions.get(prescriptionId);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Prescription not found"));
        }
        
        if (!"Pending".equals(existing.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Only pending prescriptions can be approved"));
        }
        
        // Check authorization with SoD (creator cannot approve)
        AuthorizationRequest authRequest = AuthorizationRequest.builder()
                .resourceType("Prescription")
                .action("approve")
                .resourceId(prescriptionId)
                .resourceBranch(existing.getBranch())
                .resourceDepartment(existing.getDepartment())
                .createdBy(existing.getDoctorId()) // For SoD check
                .build();
        
        AuthorizationResponse authResponse = authorizationService.authorize(authRequest);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        existing.setStatus("Approved");
        existing.setApprovedBy(user.getUserId());
        existing.setApprovedAt(LocalDateTime.now());
        
        log.info("Prescription approved by {}: {}", user.getUserId(), prescriptionId);
        return ResponseEntity.ok(ApiResponse.success("Prescription approved", existing));
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

