package com.auth.auth_service.controller.mockapi;

import com.auth.auth_service.dto.ApiResponse;
import com.auth.auth_service.dto.AuthorizationRequest;
import com.auth.auth_service.dto.AuthorizationResponse;
import com.auth.auth_service.dto.mock.ReportDto;
import com.auth.auth_service.dto.mock.StaffProfileDto;
import com.auth.auth_service.dto.mock.TrainingRecordDto;
import com.auth.auth_service.dto.mock.WorkScheduleDto;
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
import java.time.LocalTime;
import java.util.*;

/**
 * Mock API Controller for Staff resources (HR domain)
 * StaffProfile, WorkSchedule, TrainingRecord: HR (create, read, update)
 * Manager can read staff profiles and schedules in their department
 * OperationReport: HR (read), Manager (read)
 */
@RestController
@RequestMapping("/api/mock/staff")
@RequiredArgsConstructor
@Slf4j
public class StaffController {
    
    private final AuthorizationService authorizationService;
    
    private static final Map<String, StaffProfileDto> mockStaff = new HashMap<>();
    private static final Map<String, WorkScheduleDto> mockSchedules = new HashMap<>();
    private static final Map<String, TrainingRecordDto> mockTraining = new HashMap<>();
    private static final Map<String, ReportDto> mockOperationReports = new HashMap<>();
    
    static {
        // Staff Profiles
        mockStaff.put("STF001", StaffProfileDto.builder()
                .staffId("STF001")
                .employeeNumber("EMP-2020-001")
                .fullName("Dr. Nguyễn Minh Tuấn")
                .email("tuannm@hospital.com")
                .phone("0901234570")
                .position("Senior Doctor")
                .department("Internal Medicine")
                .branch("BRANCH_HCM")
                .role("Doctor")
                .employmentType("FullTime")
                .seniority("Senior")
                .hasLicense(true)
                .licenseNumber("MED-HCM-12345")
                .licenseExpiry(LocalDate.now().plusYears(2))
                .hireDate(LocalDate.of(2020, 1, 15))
                .status("Active")
                .createdAt(LocalDateTime.now().minusYears(4))
                .updatedAt(LocalDateTime.now())
                .build());
        
        mockStaff.put("STF002", StaffProfileDto.builder()
                .staffId("STF002")
                .employeeNumber("EMP-2021-045")
                .fullName("Trần Thị Hồng")
                .email("hongtt@hospital.com")
                .phone("0901234571")
                .position("Nurse")
                .department("Internal Medicine")
                .branch("BRANCH_HCM")
                .role("Nurse")
                .employmentType("FullTime")
                .seniority("Mid")
                .hasLicense(true)
                .licenseNumber("NUR-HCM-67890")
                .licenseExpiry(LocalDate.now().plusYears(1))
                .hireDate(LocalDate.of(2021, 6, 1))
                .status("Active")
                .createdAt(LocalDateTime.now().minusYears(3))
                .updatedAt(LocalDateTime.now())
                .build());
        
        mockStaff.put("STF003", StaffProfileDto.builder()
                .staffId("STF003")
                .employeeNumber("EMP-2022-089")
                .fullName("Lê Văn Nam")
                .email("namlv@hospital.com")
                .phone("0901234572")
                .position("Receptionist")
                .department("Front Desk")
                .branch("BRANCH_HCM")
                .role("Receptionist")
                .employmentType("FullTime")
                .seniority("Junior")
                .hasLicense(false)
                .licenseNumber(null)
                .licenseExpiry(null)
                .hireDate(LocalDate.of(2022, 3, 15))
                .status("Active")
                .createdAt(LocalDateTime.now().minusYears(2))
                .updatedAt(LocalDateTime.now())
                .build());
        
        // Work Schedules
        mockSchedules.put("WS001", WorkScheduleDto.builder()
                .scheduleId("WS001")
                .staffId("STF001")
                .createdBy("HR001")
                .date(LocalDate.now())
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(17, 0))
                .shiftType("Morning")
                .status("Scheduled")
                .notes("OPD duty")
                .branch("BRANCH_HCM")
                .department("Internal Medicine")
                .createdAt(LocalDateTime.now().minusDays(7))
                .updatedAt(LocalDateTime.now())
                .build());
        
        mockSchedules.put("WS002", WorkScheduleDto.builder()
                .scheduleId("WS002")
                .staffId("STF002")
                .createdBy("HR001")
                .date(LocalDate.now())
                .startTime(LocalTime.of(7, 0))
                .endTime(LocalTime.of(15, 0))
                .shiftType("Morning")
                .status("Scheduled")
                .notes("Ward duty - Floor 3")
                .branch("BRANCH_HCM")
                .department("Internal Medicine")
                .createdAt(LocalDateTime.now().minusDays(7))
                .updatedAt(LocalDateTime.now())
                .build());
        
        // Training Records
        mockTraining.put("TRN001", TrainingRecordDto.builder()
                .trainingId("TRN001")
                .staffId("STF001")
                .createdBy("HR001")
                .trainingName("Advanced Cardiac Life Support (ACLS)")
                .trainingType("Certification")
                .provider("American Heart Association")
                .completedDate(LocalDate.now().minusMonths(6))
                .expiryDate(LocalDate.now().plusYears(2))
                .status("Completed")
                .certificateNumber("ACLS-2024-12345")
                .score(95.0)
                .notes("Excellent performance")
                .branch("BRANCH_HCM")
                .department("Internal Medicine")
                .createdAt(LocalDateTime.now().minusMonths(6))
                .build());
        
        mockTraining.put("TRN002", TrainingRecordDto.builder()
                .trainingId("TRN002")
                .staffId("STF002")
                .createdBy("HR001")
                .trainingName("Basic Life Support (BLS)")
                .trainingType("Mandatory")
                .provider("Red Cross Vietnam")
                .completedDate(LocalDate.now().minusMonths(3))
                .expiryDate(LocalDate.now().plusYears(1))
                .status("Completed")
                .certificateNumber("BLS-2024-67890")
                .score(88.5)
                .notes(null)
                .branch("BRANCH_HCM")
                .department("Internal Medicine")
                .createdAt(LocalDateTime.now().minusMonths(3))
                .build());
        
        // Operation Reports
        mockOperationReports.put("OR001", ReportDto.builder()
                .reportId("OR001")
                .reportType("OperationReport")
                .reportName("Monthly Operation Report - December 2025")
                .generatedBy("HR001")
                .periodStart("2025-12-01")
                .periodEnd("2025-12-31")
                .status("Final")
                .data(Map.of(
                        "totalStaff", 250,
                        "activeStaff", 245,
                        "onLeave", 5,
                        "averageAttendance", 96.5,
                        "totalShifts", 7500,
                        "overtimeHours", 450,
                        "trainingCompleted", 35
                ))
                .summary("Staff performance metrics are within acceptable range")
                .branch("BRANCH_HCM")
                .department(null)
                .generatedAt(LocalDateTime.now().minusDays(1))
                .build());
    }
    
    // ==================== Staff Profiles ====================
    
    @GetMapping("/profiles")
    public ResponseEntity<ApiResponse<List<StaffProfileDto>>> getAllStaffProfiles(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "StaffProfile", "read", null, null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<StaffProfileDto> staff = mockStaff.values().stream()
                .filter(s -> department == null || s.getDepartment().equals(department))
                .filter(s -> role == null || s.getRole().equals(role))
                .filter(s -> status == null || s.getStatus().equals(status))
                .filter(s -> user.getBranch().equals(s.getBranch()))
                // Manager can only see staff in their department
                .filter(s -> !"Manager".equals(user.getRole()) || s.getDepartment().equals(user.getDepartment()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Staff profiles retrieved", staff));
    }
    
    @GetMapping("/profiles/{staffId}")
    public ResponseEntity<ApiResponse<StaffProfileDto>> getStaffProfile(
            @PathVariable String staffId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        StaffProfileDto staff = mockStaff.get(staffId);
        if (staff == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Staff profile not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "StaffProfile", "read", 
                staff.getBranch(), staff.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        return ResponseEntity.ok(ApiResponse.success(staff));
    }
    
    @PostMapping("/profiles")
    public ResponseEntity<ApiResponse<StaffProfileDto>> createStaffProfile(
            @RequestBody StaffProfileDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "StaffProfile", "create", 
                user.getBranch(), request.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        String staffId = "STF" + String.format("%03d", mockStaff.size() + 1);
        request.setStaffId(staffId);
        request.setStatus("Active");
        request.setBranch(user.getBranch());
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        
        mockStaff.put(staffId, request);
        
        log.info("Staff profile created by {}: {}", user.getUserId(), staffId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Staff profile created", request));
    }
    
    @PutMapping("/profiles/{staffId}")
    public ResponseEntity<ApiResponse<StaffProfileDto>> updateStaffProfile(
            @PathVariable String staffId,
            @RequestBody StaffProfileDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        StaffProfileDto existing = mockStaff.get(staffId);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Staff profile not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "StaffProfile", "update",
                existing.getBranch(), existing.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        if (request.getPhone() != null) existing.setPhone(request.getPhone());
        if (request.getEmail() != null) existing.setEmail(request.getEmail());
        if (request.getPosition() != null) existing.setPosition(request.getPosition());
        if (request.getDepartment() != null) existing.setDepartment(request.getDepartment());
        if (request.getStatus() != null) existing.setStatus(request.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        
        log.info("Staff profile updated by {}: {}", user.getUserId(), staffId);
        return ResponseEntity.ok(ApiResponse.success("Staff profile updated", existing));
    }
    
    // ==================== Work Schedules ====================
    
    @GetMapping("/schedules")
    public ResponseEntity<ApiResponse<List<WorkScheduleDto>>> getAllSchedules(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String staffId,
            @RequestParam(required = false) String date) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "WorkSchedule", "read", null, null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<WorkScheduleDto> schedules = mockSchedules.values().stream()
                .filter(s -> staffId == null || s.getStaffId().equals(staffId))
                .filter(s -> user.getBranch().equals(s.getBranch()))
                .filter(s -> !"Manager".equals(user.getRole()) || s.getDepartment().equals(user.getDepartment()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Work schedules retrieved", schedules));
    }
    
    @PostMapping("/schedules")
    public ResponseEntity<ApiResponse<WorkScheduleDto>> createSchedule(
            @RequestBody WorkScheduleDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "WorkSchedule", "create", 
                user.getBranch(), request.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        String scheduleId = "WS" + String.format("%03d", mockSchedules.size() + 1);
        request.setScheduleId(scheduleId);
        request.setCreatedBy(user.getUserId());
        request.setStatus("Scheduled");
        request.setBranch(user.getBranch());
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        
        mockSchedules.put(scheduleId, request);
        
        log.info("Work schedule created by {}: {}", user.getUserId(), scheduleId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Work schedule created", request));
    }
    
    @PutMapping("/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<WorkScheduleDto>> updateSchedule(
            @PathVariable String scheduleId,
            @RequestBody WorkScheduleDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        WorkScheduleDto existing = mockSchedules.get(scheduleId);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Work schedule not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "WorkSchedule", "update",
                existing.getBranch(), existing.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        if (request.getStartTime() != null) existing.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) existing.setEndTime(request.getEndTime());
        if (request.getShiftType() != null) existing.setShiftType(request.getShiftType());
        if (request.getStatus() != null) existing.setStatus(request.getStatus());
        if (request.getNotes() != null) existing.setNotes(request.getNotes());
        existing.setUpdatedAt(LocalDateTime.now());
        
        log.info("Work schedule updated by {}: {}", user.getUserId(), scheduleId);
        return ResponseEntity.ok(ApiResponse.success("Work schedule updated", existing));
    }
    
    // ==================== Training Records ====================
    
    @GetMapping("/training")
    public ResponseEntity<ApiResponse<List<TrainingRecordDto>>> getAllTrainingRecords(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String staffId,
            @RequestParam(required = false) String status) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "TrainingRecord", "read", null, null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<TrainingRecordDto> training = mockTraining.values().stream()
                .filter(t -> staffId == null || t.getStaffId().equals(staffId))
                .filter(t -> status == null || t.getStatus().equals(status))
                .filter(t -> user.getBranch().equals(t.getBranch()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Training records retrieved", training));
    }
    
    @PostMapping("/training")
    public ResponseEntity<ApiResponse<TrainingRecordDto>> createTrainingRecord(
            @RequestBody TrainingRecordDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "TrainingRecord", "create", 
                user.getBranch(), request.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        String trainingId = "TRN" + String.format("%03d", mockTraining.size() + 1);
        request.setTrainingId(trainingId);
        request.setCreatedBy(user.getUserId());
        request.setStatus("Scheduled");
        request.setBranch(user.getBranch());
        request.setCreatedAt(LocalDateTime.now());
        
        mockTraining.put(trainingId, request);
        
        log.info("Training record created by {}: {}", user.getUserId(), trainingId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Training record created", request));
    }
    
    // ==================== Operation Reports ====================
    
    @GetMapping("/reports/operation")
    public ResponseEntity<ApiResponse<List<ReportDto>>> getAllOperationReports(
            @AuthenticationPrincipal UserPrincipal user) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "OperationReport", "read", null, null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<ReportDto> reports = mockOperationReports.values().stream()
                .filter(r -> user.getBranch().equals(r.getBranch()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Operation reports retrieved", reports));
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

