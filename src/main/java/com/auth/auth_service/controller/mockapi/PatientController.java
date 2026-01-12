package com.auth.auth_service.controller.mockapi;

import com.auth.auth_service.dto.ApiResponse;
import com.auth.auth_service.dto.AuthorizationRequest;
import com.auth.auth_service.dto.AuthorizationResponse;
import com.auth.auth_service.dto.mock.PatientProfileDto;
import com.auth.auth_service.security.UserPrincipal;
import com.auth.auth_service.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Mock API Controller for Patient Profile resources
 * Demonstrates RBAC authorization for patient data access
 */
@RestController
@RequestMapping("/api/mock/patients")
@RequiredArgsConstructor
@Slf4j
public class PatientController {
    
    private final AuthorizationService authorizationService;
    
    // Mock data storage
    private static final Map<String, PatientProfileDto> mockPatients = new HashMap<>();
    
    static {
        mockPatients.put("PAT001", PatientProfileDto.builder()
                .patientId("PAT001")
                .fullName("Nguyễn Văn A")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .gender("Nam")
                .nationalId("001234567890")
                .phone("0901234567")
                .email("nguyenvana@email.com")
                .address("123 Đường ABC, Quận 1, TP.HCM")
                .branch("BRANCH_HCM")
                .department("Internal Medicine")
                .bloodType("O+")
                .allergies("Penicillin")
                .emergencyContact("0909876543")
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now())
                .build());
        
        mockPatients.put("PAT002", PatientProfileDto.builder()
                .patientId("PAT002")
                .fullName("Trần Thị B")
                .dateOfBirth(LocalDate.of(1985, 8, 20))
                .gender("Nữ")
                .nationalId("001234567891")
                .phone("0901234568")
                .email("tranthib@email.com")
                .address("456 Đường XYZ, Quận 3, TP.HCM")
                .branch("BRANCH_HCM")
                .department("Cardiology")
                .bloodType("A+")
                .allergies("None")
                .emergencyContact("0909876544")
                .createdAt(LocalDateTime.now().minusDays(20))
                .updatedAt(LocalDateTime.now())
                .build());
        
        mockPatients.put("PAT003", PatientProfileDto.builder()
                .patientId("PAT003")
                .fullName("Lê Văn C")
                .dateOfBirth(LocalDate.of(1975, 12, 10))
                .gender("Nam")
                .nationalId("001234567892")
                .phone("0901234569")
                .email("levanc@email.com")
                .address("789 Đường DEF, Quận 7, TP.HCM")
                .branch("BRANCH_HN")
                .department("Orthopedics")
                .bloodType("B+")
                .allergies("Aspirin")
                .emergencyContact("0909876545")
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now())
                .build());
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<PatientProfileDto>>> getAllPatients(
            @AuthenticationPrincipal UserPrincipal user) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "PatientProfile", "read", null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        // Filter by branch for non-admin roles
        List<PatientProfileDto> patients = mockPatients.values().stream()
                .filter(p -> user.getBranch() == null || p.getBranch().equals(user.getBranch()) 
                        || Set.of("Manager", "ITAdmin", "SecurityAdmin").contains(user.getRole()))
                .map(p -> applyMasking(p, authResponse))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Patients retrieved successfully", patients));
    }
    
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<PatientProfileDto>> getPatient(
            @PathVariable String patientId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        PatientProfileDto patient = mockPatients.get(patientId);
        if (patient == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Patient not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "PatientProfile", "read", patient.getBranch());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        return ResponseEntity.ok(ApiResponse.success(applyMasking(patient, authResponse)));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<PatientProfileDto>> createPatient(
            @RequestBody(required = false) PatientProfileDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        // Auto-create request with defaults if null
        if (request == null) {
            request = new PatientProfileDto();
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "PatientProfile", "create", user.getBranch());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        String patientId = "PAT" + String.format("%03d", mockPatients.size() + 1);
        request.setPatientId(patientId);
        request.setBranch(user.getBranch());
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        
        // Set defaults for required fields
        if (request.getFullName() == null || request.getFullName().isEmpty()) {
            request.setFullName("New Patient");
        }
        if (request.getDateOfBirth() == null) {
            request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        }
        if (request.getGender() == null) request.setGender("Not specified");
        if (request.getPhone() == null) request.setPhone("0900000000");
        if (request.getDepartment() == null) request.setDepartment(user.getDepartment() != null ? user.getDepartment() : "General");
        
        mockPatients.put(patientId, request);
        
        log.info("Patient created by {}: {}", user.getUserId(), patientId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Patient created successfully", request));
    }
    
    @PutMapping("/{patientId}")
    public ResponseEntity<ApiResponse<PatientProfileDto>> updatePatient(
            @PathVariable String patientId,
            @RequestBody PatientProfileDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        PatientProfileDto existing = mockPatients.get(patientId);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Patient not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "PatientProfile", "update", existing.getBranch());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        // Update fields
        existing.setFullName(request.getFullName() != null ? request.getFullName() : existing.getFullName());
        existing.setPhone(request.getPhone() != null ? request.getPhone() : existing.getPhone());
        existing.setEmail(request.getEmail() != null ? request.getEmail() : existing.getEmail());
        existing.setAddress(request.getAddress() != null ? request.getAddress() : existing.getAddress());
        existing.setEmergencyContact(request.getEmergencyContact() != null ? request.getEmergencyContact() : existing.getEmergencyContact());
        existing.setUpdatedAt(LocalDateTime.now());
        
        log.info("Patient updated by {}: {}", user.getUserId(), patientId);
        return ResponseEntity.ok(ApiResponse.success("Patient updated successfully", existing));
    }
    
    @DeleteMapping("/{patientId}")
    public ResponseEntity<ApiResponse<Void>> deletePatient(
            @PathVariable String patientId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        PatientProfileDto existing = mockPatients.get(patientId);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Patient not found"));
        }
        
        // Delete patient data is always denied per policy
        AuthorizationResponse authResponse = checkAuthorization(user, "PatientProfile", "delete", existing.getBranch());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        mockPatients.remove(patientId);
        return ResponseEntity.ok(ApiResponse.success("Patient deleted successfully", null));
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
    
    private PatientProfileDto applyMasking(PatientProfileDto patient, AuthorizationResponse authResponse) {
        // Check if masking obligation exists
        if (authResponse.getObligations() != null) {
            boolean shouldMask = authResponse.getObligations().stream()
                    .anyMatch(o -> "mask_fields".equals(o.get("type")));
            
            if (shouldMask) {
                return PatientProfileDto.builder()
                        .patientId(patient.getPatientId())
                        .fullName(patient.getFullName())
                        .dateOfBirth(patient.getDateOfBirth())
                        .gender(patient.getGender())
                        .nationalId("***MASKED***") // Masked
                        .phone(patient.getPhone())
                        .email(patient.getEmail())
                        .address("***MASKED***") // Masked
                        .branch(patient.getBranch())
                        .department(patient.getDepartment())
                        .bloodType(patient.getBloodType())
                        .allergies(patient.getAllergies())
                        .emergencyContact(patient.getEmergencyContact())
                        .createdAt(patient.getCreatedAt())
                        .updatedAt(patient.getUpdatedAt())
                        .build();
            }
        }
        return patient;
    }
}

