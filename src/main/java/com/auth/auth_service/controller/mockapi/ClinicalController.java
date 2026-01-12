package com.auth.auth_service.controller.mockapi;

import com.auth.auth_service.dto.ApiResponse;
import com.auth.auth_service.dto.AuthorizationRequest;
import com.auth.auth_service.dto.AuthorizationResponse;
import com.auth.auth_service.dto.mock.ClinicalNoteDto;
import com.auth.auth_service.dto.mock.VitalSignsDto;
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
 * Mock API Controller for Clinical Notes and Vital Signs
 * ClinicalNote: Doctor (read, create), Nurse (read)
 * VitalSigns: Doctor (read), Nurse (read, create, update)
 */
@RestController
@RequestMapping("/api/mock/clinical")
@RequiredArgsConstructor
@Slf4j
public class    ClinicalController {
    
    private final AuthorizationService authorizationService;
    
    private static final Map<String, ClinicalNoteDto> mockNotes = new HashMap<>();
    private static final Map<String, VitalSignsDto> mockVitals = new HashMap<>();
    
    static {
        // Clinical Notes
        mockNotes.put("CN001", ClinicalNoteDto.builder()
                .noteId("CN001")
                .patientId("PAT001")
                .authorId("DOC001")
                .authorRole("Doctor")
                .noteType("Progress")
                .content("Patient showing improvement. Blood pressure controlled with medication.")
                .branch("BRANCH_HCM")
                .department("Internal Medicine")
                .createdAt(LocalDateTime.now().minusDays(2))
                .build());
        
        mockNotes.put("CN002", ClinicalNoteDto.builder()
                .noteId("CN002")
                .patientId("PAT001")
                .authorId("NUR001")
                .authorRole("Nurse")
                .noteType("Assessment")
                .content("Patient ambulatory, no complaints of pain or discomfort.")
                .branch("BRANCH_HCM")
                .department("Internal Medicine")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build());
        
        // Vital Signs
        mockVitals.put("VS001", VitalSignsDto.builder()
                .vitalId("VS001")
                .patientId("PAT001")
                .recordedBy("NUR001")
                .temperature(36.8)
                .heartRate(78)
                .bloodPressureSystolic(135)
                .bloodPressureDiastolic(85)
                .respiratoryRate(16)
                .oxygenSaturation(98.0)
                .weight(72.5)
                .height(175.0)
                .notes("Patient stable")
                .branch("BRANCH_HCM")
                .department("Internal Medicine")
                .recordedAt(LocalDateTime.now().minusHours(6))
                .build());
        
        mockVitals.put("VS002", VitalSignsDto.builder()
                .vitalId("VS002")
                .patientId("PAT002")
                .recordedBy("NUR002")
                .temperature(37.2)
                .heartRate(92)
                .bloodPressureSystolic(110)
                .bloodPressureDiastolic(70)
                .respiratoryRate(18)
                .oxygenSaturation(96.0)
                .weight(65.0)
                .height(160.0)
                .notes("Post-procedure monitoring")
                .branch("BRANCH_HCM")
                .department("Cardiology")
                .recordedAt(LocalDateTime.now().minusHours(2))
                .build());
    }
    
    // ==================== Clinical Notes ====================
    
    @GetMapping("/notes")
    public ResponseEntity<ApiResponse<List<ClinicalNoteDto>>> getAllNotes(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String patientId) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "ClinicalNote", "read", null, null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<ClinicalNoteDto> notes = mockNotes.values().stream()
                .filter(n -> patientId == null || n.getPatientId().equals(patientId))
                .filter(n -> user.getBranch().equals(n.getBranch()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Clinical notes retrieved", notes));
    }
    
    @GetMapping("/notes/{noteId}")
    public ResponseEntity<ApiResponse<ClinicalNoteDto>> getNote(
            @PathVariable String noteId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        ClinicalNoteDto note = mockNotes.get(noteId);
        if (note == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Clinical note not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "ClinicalNote", "read", 
                note.getBranch(), note.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        return ResponseEntity.ok(ApiResponse.success(note));
    }
    
    @PostMapping("/notes")
    public ResponseEntity<ApiResponse<ClinicalNoteDto>> createNote(
            @RequestBody(required = false) ClinicalNoteDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        // Auto-create request with defaults if null
        if (request == null) {
            request = new ClinicalNoteDto();
        }
        
        // Set default patientId if not provided
        if (request.getPatientId() == null || request.getPatientId().isEmpty()) {
            request.setPatientId("PAT001");
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "ClinicalNote", "create", 
                user.getBranch(), user.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        String noteId = "CN" + String.format("%03d", mockNotes.size() + 1);
        request.setNoteId(noteId);
        request.setAuthorId(user.getUserId());
        request.setAuthorRole(user.getRole());
        request.setBranch(user.getBranch());
        request.setDepartment(user.getDepartment());
        request.setCreatedAt(LocalDateTime.now());
        
        // Set defaults for optional fields
        if (request.getNoteType() == null) request.setNoteType("Progress");
        if (request.getContent() == null) request.setContent("Clinical note recorded");
        
        mockNotes.put(noteId, request);
        
        log.info("Clinical note created by {}: {}", user.getUserId(), noteId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Clinical note created", request));
    }
    
    // ==================== Vital Signs ====================
    
    @GetMapping("/vitals")
    public ResponseEntity<ApiResponse<List<VitalSignsDto>>> getAllVitals(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String patientId) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "VitalSigns", "read", null, null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<VitalSignsDto> vitals = mockVitals.values().stream()
                .filter(v -> patientId == null || v.getPatientId().equals(patientId))
                .filter(v -> user.getBranch().equals(v.getBranch()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Vital signs retrieved", vitals));
    }
    
    @GetMapping("/vitals/{vitalId}")
    public ResponseEntity<ApiResponse<VitalSignsDto>> getVital(
            @PathVariable String vitalId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        VitalSignsDto vital = mockVitals.get(vitalId);
        if (vital == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Vital signs record not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "VitalSigns", "read", 
                vital.getBranch(), vital.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        return ResponseEntity.ok(ApiResponse.success(vital));
    }
    
    @PostMapping("/vitals")
    public ResponseEntity<ApiResponse<VitalSignsDto>> createVital(
            @RequestBody(required = false) VitalSignsDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        // Auto-create request with defaults if null
        if (request == null) {
            request = new VitalSignsDto();
        }
        
        // Set default patientId if not provided
        if (request.getPatientId() == null || request.getPatientId().isEmpty()) {
            request.setPatientId("PAT001");
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "VitalSigns", "create", 
                user.getBranch(), user.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        String vitalId = "VS" + String.format("%03d", mockVitals.size() + 1);
        request.setVitalId(vitalId);
        request.setRecordedBy(user.getUserId());
        request.setBranch(user.getBranch());
        request.setDepartment(user.getDepartment());
        request.setRecordedAt(LocalDateTime.now());
        
        // Set defaults for vital signs if not provided
        if (request.getTemperature() == null) request.setTemperature(36.5);
        if (request.getHeartRate() == null) request.setHeartRate(72);
        if (request.getBloodPressureSystolic() == null) request.setBloodPressureSystolic(120);
        if (request.getBloodPressureDiastolic() == null) request.setBloodPressureDiastolic(80);
        if (request.getRespiratoryRate() == null) request.setRespiratoryRate(16);
        if (request.getOxygenSaturation() == null) request.setOxygenSaturation(98.0);
        
        mockVitals.put(vitalId, request);
        
        log.info("Vital signs recorded by {}: {}", user.getUserId(), vitalId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vital signs recorded", request));
    }
    
    @PutMapping("/vitals/{vitalId}")
    public ResponseEntity<ApiResponse<VitalSignsDto>> updateVital(
            @PathVariable String vitalId,
            @RequestBody VitalSignsDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        VitalSignsDto existing = mockVitals.get(vitalId);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Vital signs record not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "VitalSigns", "update",
                existing.getBranch(), existing.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        // Update fields
        if (request.getTemperature() != null) existing.setTemperature(request.getTemperature());
        if (request.getHeartRate() != null) existing.setHeartRate(request.getHeartRate());
        if (request.getBloodPressureSystolic() != null) existing.setBloodPressureSystolic(request.getBloodPressureSystolic());
        if (request.getBloodPressureDiastolic() != null) existing.setBloodPressureDiastolic(request.getBloodPressureDiastolic());
        if (request.getRespiratoryRate() != null) existing.setRespiratoryRate(request.getRespiratoryRate());
        if (request.getOxygenSaturation() != null) existing.setOxygenSaturation(request.getOxygenSaturation());
        if (request.getNotes() != null) existing.setNotes(request.getNotes());
        
        log.info("Vital signs updated by {}: {}", user.getUserId(), vitalId);
        return ResponseEntity.ok(ApiResponse.success("Vital signs updated", existing));
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

