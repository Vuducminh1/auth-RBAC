package com.auth.auth_service.controller.mockapi;

import com.auth.auth_service.aop.Audit;
import com.auth.auth_service.dto.ApiResponse;
import com.auth.auth_service.dto.AuthorizationRequest;
import com.auth.auth_service.dto.AuthorizationResponse;
import com.auth.auth_service.dto.mock.AppointmentDto;
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
 * Mock API Controller for Appointments
 * Accessible by: Receptionist (create, read, update)
 */
@RestController
@RequestMapping("/api/mock/appointments")
@RequiredArgsConstructor
@Slf4j
public class AppointmentController {
    
    private final AuthorizationService authorizationService;
    
    private static final Map<String, AppointmentDto> mockAppointments = new HashMap<>();
    
    static {
        mockAppointments.put("APT001", AppointmentDto.builder()
                .appointmentId("APT001")
                .patientId("PAT001")
                .doctorId("DOC001")
                .createdBy("REC001")
                .appointmentType("FollowUp")
                .status("Scheduled")
                .reason("Blood pressure follow-up")
                .notes("Patient needs BP monitoring")
                .durationMinutes(30)
                .roomNumber("201")
                .branch("BRANCH_HCM")
                .department("Internal Medicine")
                .scheduledAt(LocalDateTime.now().plusDays(7).withHour(9).withMinute(0))
                .createdAt(LocalDateTime.now().minusDays(3))
                .updatedAt(LocalDateTime.now())
                .build());
        
        mockAppointments.put("APT002", AppointmentDto.builder()
                .appointmentId("APT002")
                .patientId("PAT002")
                .doctorId("DOC002")
                .createdBy("REC001")
                .appointmentType("Consultation")
                .status("Completed")
                .reason("Cardiac consultation")
                .notes("Post-MI follow-up")
                .durationMinutes(45)
                .roomNumber("305")
                .branch("BRANCH_HCM")
                .department("Cardiology")
                .scheduledAt(LocalDateTime.now().minusDays(1).withHour(10).withMinute(30))
                .createdAt(LocalDateTime.now().minusDays(7))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build());
        
        mockAppointments.put("APT003", AppointmentDto.builder()
                .appointmentId("APT003")
                .patientId("PAT003")
                .doctorId("DOC003")
                .createdBy("REC002")
                .appointmentType("Procedure")
                .status("Scheduled")
                .reason("MRI examination")
                .notes("Lumbar spine MRI")
                .durationMinutes(60)
                .roomNumber("MRI-01")
                .branch("BRANCH_HN")
                .department("Orthopedics")
                .scheduledAt(LocalDateTime.now().plusDays(2).withHour(14).withMinute(0))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }
    
    @Audit(resourceType = "Appointment", action = "read")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> getAllAppointments(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String doctorId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String date) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "Appointment", "read", null, null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<AppointmentDto> appointments = mockAppointments.values().stream()
                .filter(a -> patientId == null || a.getPatientId().equals(patientId))
                .filter(a -> doctorId == null || a.getDoctorId().equals(doctorId))
                .filter(a -> status == null || a.getStatus().equals(status))
                .filter(a -> user.getBranch().equals(a.getBranch()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Appointments retrieved", appointments));
    }
    
    @Audit(resourceType = "Appointment", action = "read")
    @GetMapping("/{appointmentId}")
    public ResponseEntity<ApiResponse<AppointmentDto>> getAppointment(
            @PathVariable String appointmentId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        AppointmentDto appointment = mockAppointments.get(appointmentId);
        if (appointment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Appointment not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "Appointment", "read", 
                appointment.getBranch(), appointment.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        return ResponseEntity.ok(ApiResponse.success(appointment));
    }
    
    @Audit(resourceType = "Appointment", action = "create")
    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentDto>> createAppointment(
            @RequestBody(required = false) AppointmentDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        // Auto-create request with defaults if null
        if (request == null) {
            request = new AppointmentDto();
        }
        
        // Set default patientId if not provided
        if (request.getPatientId() == null || request.getPatientId().isEmpty()) {
            request.setPatientId("PAT001");
        }
        
        // Set default doctorId if not provided
        if (request.getDoctorId() == null || request.getDoctorId().isEmpty()) {
            request.setDoctorId("DOC001");
        }
        
        // Set default department if not provided
        if (request.getDepartment() == null || request.getDepartment().isEmpty()) {
            request.setDepartment(user.getDepartment() != null ? user.getDepartment() : "General");
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "Appointment", "create", 
                user.getBranch(), request.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        String appointmentId = "APT" + String.format("%03d", mockAppointments.size() + 1);
        request.setAppointmentId(appointmentId);
        request.setCreatedBy(user.getUserId());
        request.setStatus("Scheduled");
        request.setBranch(user.getBranch());
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        
        // Set defaults for optional fields
        if (request.getAppointmentType() == null) request.setAppointmentType("Consultation");
        if (request.getReason() == null) request.setReason("General consultation");
        if (request.getDurationMinutes() == null) request.setDurationMinutes(30);
        if (request.getScheduledAt() == null) request.setScheduledAt(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0));
        
        mockAppointments.put(appointmentId, request);
        
        log.info("Appointment created by {}: {}", user.getUserId(), appointmentId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Appointment created", request));
    }
    
    @Audit(resourceType = "Appointment", action = "update")
    @PutMapping("/{appointmentId}")
    public ResponseEntity<ApiResponse<AppointmentDto>> updateAppointment(
            @PathVariable String appointmentId,
            @RequestBody AppointmentDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        AppointmentDto existing = mockAppointments.get(appointmentId);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Appointment not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "Appointment", "update",
                existing.getBranch(), existing.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        if (request.getScheduledAt() != null) existing.setScheduledAt(request.getScheduledAt());
        if (request.getStatus() != null) existing.setStatus(request.getStatus());
        if (request.getReason() != null) existing.setReason(request.getReason());
        if (request.getNotes() != null) existing.setNotes(request.getNotes());
        if (request.getRoomNumber() != null) existing.setRoomNumber(request.getRoomNumber());
        if (request.getDurationMinutes() != null) existing.setDurationMinutes(request.getDurationMinutes());
        existing.setUpdatedAt(LocalDateTime.now());
        
        log.info("Appointment updated by {}: {}", user.getUserId(), appointmentId);
        return ResponseEntity.ok(ApiResponse.success("Appointment updated", existing));
    }
    
    @Audit(resourceType = "Appointment", action = "update")
    @PostMapping("/{appointmentId}/check-in")
    public ResponseEntity<ApiResponse<AppointmentDto>> checkInAppointment(
            @PathVariable String appointmentId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        AppointmentDto existing = mockAppointments.get(appointmentId);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Appointment not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "Appointment", "update",
                existing.getBranch(), existing.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        existing.setStatus("CheckedIn");
        existing.setUpdatedAt(LocalDateTime.now());
        
        log.info("Patient checked in by {}: {}", user.getUserId(), appointmentId);
        return ResponseEntity.ok(ApiResponse.success("Patient checked in", existing));
    }
    
    @Audit(resourceType = "Appointment", action = "update")
    @PostMapping("/{appointmentId}/cancel")
    public ResponseEntity<ApiResponse<AppointmentDto>> cancelAppointment(
            @PathVariable String appointmentId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        AppointmentDto existing = mockAppointments.get(appointmentId);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Appointment not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "Appointment", "update",
                existing.getBranch(), existing.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        existing.setStatus("Cancelled");
        existing.setUpdatedAt(LocalDateTime.now());
        
        log.info("Appointment cancelled by {}: {}", user.getUserId(), appointmentId);
        return ResponseEntity.ok(ApiResponse.success("Appointment cancelled", existing));
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

