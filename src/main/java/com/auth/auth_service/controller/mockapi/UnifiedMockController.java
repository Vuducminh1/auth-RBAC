package com.auth.auth_service.controller.mockapi;

import com.auth.auth_service.aop.Audit;
import com.auth.auth_service.dto.ApiResponse;
import com.auth.auth_service.dto.AuthorizationRequest;
import com.auth.auth_service.dto.AuthorizationResponse;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unified Mock API Controller
 * Một endpoint duy nhất cho tất cả resource types
 * 
 * Supported resourceTypes:
 * - patient, medical-record, prescription, clinical-note, vital-signs
 * - lab-order, lab-result, imaging-order, imaging-result
 * - admission, transfer, discharge-summary
 * - appointment, billing, invoice, insurance-claim
 * - staff, schedule, training
 * - incident, system-config, access-policy
 * - medical-report, financial-report, operation-report
 * 
 * Usage:
 * GET    /api/v2/mock/{resourceType}              - Lấy danh sách
 * GET    /api/v2/mock/{resourceType}/{id}         - Lấy chi tiết
 * POST   /api/v2/mock/{resourceType}              - Tạo mới (không cần body)
 * PUT    /api/v2/mock/{resourceType}/{id}         - Cập nhật
 * DELETE /api/v2/mock/{resourceType}/{id}         - Xóa
 * POST   /api/v2/mock/{resourceType}/{id}/{action} - Hành động đặc biệt
 */
@RestController
@RequestMapping("/api/v2/mock")
@RequiredArgsConstructor
@Slf4j
public class UnifiedMockController {
    
    private final AuthorizationService authorizationService;
    
    // Generic storage for all resource types
    private static final Map<String, Map<String, Map<String, Object>>> dataStore = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> idCounters = new ConcurrentHashMap<>();
    
    // Resource type to authorization resource mapping
    private static final Map<String, String> resourceTypeMapping = Map.ofEntries(
        Map.entry("patient", "PatientProfile"),
        Map.entry("medical-record", "MedicalRecord"),
        Map.entry("prescription", "Prescription"),
        Map.entry("clinical-note", "ClinicalNote"),
        Map.entry("vital-signs", "VitalSigns"),
        Map.entry("lab-order", "LabOrder"),
        Map.entry("lab-result", "LabResult"),
        Map.entry("imaging-order", "ImagingOrder"),
        Map.entry("imaging-result", "ImagingResult"),
        Map.entry("admission", "AdmissionRecord"),
        Map.entry("transfer", "TransferRecord"),
        Map.entry("discharge-summary", "DischargeSummary"),
        Map.entry("appointment", "Appointment"),
        Map.entry("billing", "BillingRecord"),
        Map.entry("invoice", "Invoice"),
        Map.entry("insurance-claim", "InsuranceClaim"),
        Map.entry("staff", "StaffProfile"),
        Map.entry("schedule", "WorkSchedule"),
        Map.entry("training", "TrainingRecord"),
        Map.entry("incident", "IncidentCase"),
        Map.entry("system-config", "SystemConfig"),
        Map.entry("access-policy", "AccessPolicy"),
        Map.entry("medical-report", "MedicalReport"),
        Map.entry("financial-report", "FinancialReport"),
        Map.entry("operation-report", "OperationReport")
    );
    
    // ID prefixes for each resource type
    private static final Map<String, String> idPrefixes = Map.ofEntries(
        Map.entry("patient", "PAT"),
        Map.entry("medical-record", "MR"),
        Map.entry("prescription", "RX"),
        Map.entry("clinical-note", "CN"),
        Map.entry("vital-signs", "VS"),
        Map.entry("lab-order", "LO"),
        Map.entry("lab-result", "LR"),
        Map.entry("imaging-order", "IO"),
        Map.entry("imaging-result", "IR"),
        Map.entry("admission", "ADM"),
        Map.entry("transfer", "TR"),
        Map.entry("discharge-summary", "DS"),
        Map.entry("appointment", "APT"),
        Map.entry("billing", "BL"),
        Map.entry("invoice", "INV"),
        Map.entry("insurance-claim", "IC"),
        Map.entry("staff", "STF"),
        Map.entry("schedule", "WS"),
        Map.entry("training", "TRN"),
        Map.entry("incident", "INC"),
        Map.entry("system-config", "CFG"),
        Map.entry("access-policy", "POL"),
        Map.entry("medical-report", "MED-RPT"),
        Map.entry("financial-report", "FIN-RPT"),
        Map.entry("operation-report", "OPS-RPT")
    );
    
    static {
        // Initialize with sample data
        initializeSampleData();
    }
    
    // ==================== GET ALL SUPPORTED RESOURCE TYPES ====================
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSupportedTypes() {
        Map<String, Object> info = new HashMap<>();
        info.put("resourceTypes", resourceTypeMapping);
        info.put("totalTypes", resourceTypeMapping.size());
        info.put("usage", Map.of(
            "getAll", "GET /api/v2/mock/{resourceType}",
            "getById", "GET /api/v2/mock/{resourceType}/{id}",
            "create", "POST /api/v2/mock/{resourceType}",
            "update", "PUT /api/v2/mock/{resourceType}/{id}",
            "delete", "DELETE /api/v2/mock/{resourceType}/{id}",
            "action", "POST /api/v2/mock/{resourceType}/{id}/{action}"
        ));
        return ResponseEntity.ok(ApiResponse.success("Supported resource types", info));
    }
    
    // ==================== GET ALL ====================
    @Audit
    @GetMapping("/{resourceType}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAll(
            @PathVariable String resourceType,
            @AuthenticationPrincipal UserPrincipal user) {
        
        String authResourceType = getAuthResourceType(resourceType);
        if (authResourceType == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unknown resource type: " + resourceType + 
                            ". Use GET /api/v2/mock/types to see supported types."));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, authResourceType, "read", null, null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        Map<String, Map<String, Object>> store = dataStore.getOrDefault(resourceType, new HashMap<>());
        
        // Filter by user's branch
        List<Map<String, Object>> results = store.values().stream()
                .filter(item -> {
                    String itemBranch = (String) item.get("branch");
                    return itemBranch == null || itemBranch.equals(user.getBranch()) 
                            || Set.of("Manager", "ITAdmin", "SecurityAdmin").contains(user.getRole());
                })
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(resourceType + " retrieved (" + results.size() + " items)", results));
    }
    
    // ==================== GET BY ID ====================
    @Audit
    @GetMapping("/{resourceType}/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getById(
            @PathVariable String resourceType,
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal user) {
        
        String authResourceType = getAuthResourceType(resourceType);
        if (authResourceType == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unknown resource type: " + resourceType));
        }
        
        Map<String, Map<String, Object>> store = dataStore.getOrDefault(resourceType, new HashMap<>());
        Map<String, Object> item = store.get(id);
        
        if (item == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(resourceType + " not found: " + id));
        }
        
        String itemBranch = (String) item.get("branch");
        AuthorizationResponse authResponse = checkAuthorization(user, authResourceType, "read", itemBranch, null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        return ResponseEntity.ok(ApiResponse.success(item));
    }
    
    // ==================== CREATE (NO BODY REQUIRED) ====================
    @Audit
    @PostMapping("/{resourceType}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(
            @PathVariable String resourceType,
            @RequestBody(required = false) Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        String authResourceType = getAuthResourceType(resourceType);
        if (authResourceType == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unknown resource type: " + resourceType));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, authResourceType, "create", user.getBranch(), user.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        // Initialize request if null
        if (request == null) {
            request = new HashMap<>();
        }
        
        // Generate ID
        String prefix = idPrefixes.getOrDefault(resourceType, "ID");
        int counter = idCounters.computeIfAbsent(resourceType, k -> new AtomicInteger(100)).incrementAndGet();
        String id = prefix + String.format("%03d", counter);
        
        // Create the item with defaults
        Map<String, Object> item = new HashMap<>(request);
        item.put("id", id);
        item.put("branch", user.getBranch());
        item.put("department", user.getDepartment());
        item.put("createdBy", user.getUserId());
        item.put("createdAt", LocalDateTime.now().toString());
        item.put("updatedAt", LocalDateTime.now().toString());
        
        // Set resource-specific defaults
        setDefaults(resourceType, item, user);
        
        // Store the item
        dataStore.computeIfAbsent(resourceType, k -> new ConcurrentHashMap<>()).put(id, item);
        
        log.info("{} created by {}: {}", authResourceType, user.getUserId(), id);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(resourceType + " created", item));
    }
    
    // ==================== UPDATE ====================
    @PutMapping("/{resourceType}/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(
            @PathVariable String resourceType,
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        String authResourceType = getAuthResourceType(resourceType);
        if (authResourceType == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unknown resource type: " + resourceType));
        }
        
        Map<String, Map<String, Object>> store = dataStore.getOrDefault(resourceType, new HashMap<>());
        Map<String, Object> existing = store.get(id);
        
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(resourceType + " not found: " + id));
        }
        
        String itemBranch = (String) existing.get("branch");
        AuthorizationResponse authResponse = checkAuthorization(user, authResourceType, "update", itemBranch, null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        // Merge updates
        if (request != null) {
            request.forEach((key, value) -> {
                if (!"id".equals(key) && !"createdAt".equals(key) && !"createdBy".equals(key)) {
                    existing.put(key, value);
                }
            });
        }
        existing.put("updatedAt", LocalDateTime.now().toString());
        existing.put("updatedBy", user.getUserId());
        
        log.info("{} updated by {}: {}", authResourceType, user.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(resourceType + " updated", existing));
    }
    
    // ==================== DELETE ====================
    @DeleteMapping("/{resourceType}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String resourceType,
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal user) {
        
        String authResourceType = getAuthResourceType(resourceType);
        if (authResourceType == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unknown resource type: " + resourceType));
        }
        
        Map<String, Map<String, Object>> store = dataStore.getOrDefault(resourceType, new HashMap<>());
        Map<String, Object> existing = store.get(id);
        
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(resourceType + " not found: " + id));
        }
        
        String itemBranch = (String) existing.get("branch");
        AuthorizationResponse authResponse = checkAuthorization(user, authResourceType, "delete", itemBranch, null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        store.remove(id);
        
        log.info("{} deleted by {}: {}", authResourceType, user.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(resourceType + " deleted", null));
    }
    
    // ==================== SPECIAL ACTIONS ====================
    @PostMapping("/{resourceType}/{id}/{action}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> performAction(
            @PathVariable String resourceType,
            @PathVariable String id,
            @PathVariable String action,
            @RequestBody(required = false) Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        String authResourceType = getAuthResourceType(resourceType);
        if (authResourceType == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unknown resource type: " + resourceType));
        }
        
        Map<String, Map<String, Object>> store = dataStore.getOrDefault(resourceType, new HashMap<>());
        Map<String, Object> existing = store.get(id);
        
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(resourceType + " not found: " + id));
        }
        
        // Check authorization for the specific action
        String itemBranch = (String) existing.get("branch");
        String itemDepartment = (String) existing.get("department");
        String createdBy = (String) existing.get("createdBy");
        
        AuthorizationRequest authRequest = AuthorizationRequest.builder()
                .resourceType(authResourceType)
                .action(action)
                .resourceId(id)
                .resourceBranch(itemBranch)
                .resourceDepartment(itemDepartment)
                .createdBy(createdBy) // For SoD check
                .build();
        
        AuthorizationResponse authResponse = authorizationService.authorize(authRequest);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        // Perform the action
        handleAction(action, existing, user, request);
        
        log.info("{} action '{}' by {}: {}", authResourceType, action, user.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Action '" + action + "' completed", existing));
    }
    
    // ==================== HELPER METHODS ====================
    
    private String getAuthResourceType(String resourceType) {
        return resourceTypeMapping.get(resourceType.toLowerCase());
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
    
    private void setDefaults(String resourceType, Map<String, Object> item, UserPrincipal user) {
        // Common defaults
        item.putIfAbsent("status", "Active");
        
        switch (resourceType) {
            case "patient" -> {
                item.putIfAbsent("patientId", item.get("id"));
                item.putIfAbsent("fullName", "New Patient");
                item.putIfAbsent("dateOfBirth", "1990-01-01");
                item.putIfAbsent("gender", "Not specified");
                item.putIfAbsent("phone", "0900000000");
                item.putIfAbsent("nationalId", "000000000000");
                item.putIfAbsent("email", "patient@example.com");
                item.putIfAbsent("address", "Address pending");
                item.putIfAbsent("bloodType", "Unknown");
                item.putIfAbsent("allergies", "None known");
            }
            case "medical-record" -> {
                item.putIfAbsent("recordId", item.get("id"));
                item.putIfAbsent("patientId", "PAT001");
                item.putIfAbsent("doctorId", user.getUserId());
                item.putIfAbsent("diagnosis", "Pending diagnosis");
                item.putIfAbsent("symptoms", "To be assessed");
                item.putIfAbsent("treatment", "Treatment plan pending");
                item.putIfAbsent("sensitivity", "Normal");
                item.putIfAbsent("visitDate", LocalDateTime.now().toString());
            }
            case "prescription" -> {
                item.putIfAbsent("prescriptionId", item.get("id"));
                item.putIfAbsent("patientId", "PAT001");
                item.putIfAbsent("doctorId", user.getUserId());
                item.putIfAbsent("status", "Pending");
                item.putIfAbsent("medications", List.of(Map.of(
                    "medicationName", "Sample Medication",
                    "dosage", "10mg",
                    "frequency", "Once daily",
                    "duration", 7,
                    "route", "Oral",
                    "instructions", "Take with food"
                )));
                item.putIfAbsent("instructions", "Follow medication schedule");
            }
            case "clinical-note" -> {
                item.putIfAbsent("noteId", item.get("id"));
                item.putIfAbsent("patientId", "PAT001");
                item.putIfAbsent("authorId", user.getUserId());
                item.putIfAbsent("authorRole", user.getRole());
                item.putIfAbsent("noteType", "Progress");
                item.putIfAbsent("content", "Clinical note recorded");
            }
            case "vital-signs" -> {
                item.putIfAbsent("vitalId", item.get("id"));
                item.putIfAbsent("patientId", "PAT001");
                item.putIfAbsent("recordedBy", user.getUserId());
                item.putIfAbsent("temperature", 36.5);
                item.putIfAbsent("heartRate", 72);
                item.putIfAbsent("bloodPressureSystolic", 120);
                item.putIfAbsent("bloodPressureDiastolic", 80);
                item.putIfAbsent("respiratoryRate", 16);
                item.putIfAbsent("oxygenSaturation", 98.0);
                item.putIfAbsent("weight", 70.0);
                item.putIfAbsent("height", 170.0);
                item.putIfAbsent("notes", "Patient stable");
                item.putIfAbsent("recordedAt", LocalDateTime.now().toString());
            }
            case "lab-order" -> {
                item.putIfAbsent("orderId", item.get("id"));
                item.putIfAbsent("patientId", "PAT001");
                item.putIfAbsent("orderedBy", user.getUserId());
                item.putIfAbsent("status", "Ordered");
                item.putIfAbsent("tests", List.of("Complete Blood Count", "Basic Metabolic Panel"));
                item.putIfAbsent("priority", "Routine");
                item.putIfAbsent("clinicalNotes", "Routine laboratory tests");
                item.putIfAbsent("orderedAt", LocalDateTime.now().toString());
            }
            case "lab-result" -> {
                item.putIfAbsent("resultId", item.get("id"));
                item.putIfAbsent("orderId", "LO001");
                item.putIfAbsent("patientId", "PAT001");
                item.putIfAbsent("status", "Pending");
                item.putIfAbsent("performedBy", user.getUserId());
                item.putIfAbsent("results", List.of(Map.of(
                    "testName", "Sample Test",
                    "value", "Normal",
                    "unit", "",
                    "referenceRange", "Normal",
                    "flag", "Normal"
                )));
                item.putIfAbsent("performedAt", LocalDateTime.now().toString());
            }
            case "imaging-order" -> {
                item.putIfAbsent("orderId", item.get("id"));
                item.putIfAbsent("patientId", "PAT001");
                item.putIfAbsent("orderedBy", user.getUserId());
                item.putIfAbsent("status", "Ordered");
                item.putIfAbsent("modality", "X-Ray");
                item.putIfAbsent("bodyPart", "Chest");
                item.putIfAbsent("priority", "Routine");
                item.putIfAbsent("clinicalIndication", "Routine imaging study");
                item.putIfAbsent("orderedAt", LocalDateTime.now().toString());
            }
            case "imaging-result" -> {
                item.putIfAbsent("resultId", item.get("id"));
                item.putIfAbsent("orderId", "IO001");
                item.putIfAbsent("patientId", "PAT001");
                item.putIfAbsent("radiologistId", user.getUserId());
                item.putIfAbsent("status", "Pending");
                item.putIfAbsent("modality", "X-Ray");
                item.putIfAbsent("bodyPart", "Chest");
                item.putIfAbsent("findings", "Pending interpretation");
                item.putIfAbsent("impression", "Pending");
                item.putIfAbsent("performedAt", LocalDateTime.now().toString());
            }
            case "admission" -> {
                item.putIfAbsent("admissionId", item.get("id"));
                item.putIfAbsent("patientId", "PAT001");
                item.putIfAbsent("admittedBy", user.getUserId());
                item.putIfAbsent("status", "Active");
                item.putIfAbsent("admissionType", "Elective");
                item.putIfAbsent("chiefComplaint", "General admission");
                item.putIfAbsent("admissionDiagnosis", "Pending diagnosis");
                item.putIfAbsent("roomNumber", "TBD");
                item.putIfAbsent("bedNumber", "TBD");
                item.putIfAbsent("admittedAt", LocalDateTime.now().toString());
            }
            case "transfer" -> {
                item.putIfAbsent("transferId", item.get("id"));
                item.putIfAbsent("patientId", "PAT001");
                item.putIfAbsent("admissionId", "ADM001");
                item.putIfAbsent("initiatedBy", user.getUserId());
                item.putIfAbsent("status", "Pending");
                item.putIfAbsent("fromDepartment", user.getDepartment());
                item.putIfAbsent("toDepartment", "Internal Medicine");
                item.putIfAbsent("fromRoom", "TBD");
                item.putIfAbsent("toRoom", "TBD");
                item.putIfAbsent("reason", "Transfer requested");
                item.putIfAbsent("requestedAt", LocalDateTime.now().toString());
            }
            case "discharge-summary" -> {
                item.putIfAbsent("summaryId", item.get("id"));
                item.putIfAbsent("admissionId", "ADM001");
                item.putIfAbsent("patientId", "PAT001");
                item.putIfAbsent("preparedBy", user.getUserId());
                item.putIfAbsent("admittingDiagnosis", "General admission");
                item.putIfAbsent("dischargeDiagnosis", "Condition improved");
                item.putIfAbsent("hospitalCourse", "Uncomplicated hospital course");
                item.putIfAbsent("conditionAtDischarge", "Stable");
                item.putIfAbsent("dischargeInstructions", List.of("Follow up in 2 weeks"));
                item.putIfAbsent("medications", List.of());
            }
            case "appointment" -> {
                item.putIfAbsent("appointmentId", item.get("id"));
                item.putIfAbsent("patientId", "PAT001");
                item.putIfAbsent("doctorId", "DOC001");
                item.putIfAbsent("status", "Scheduled");
                item.putIfAbsent("appointmentType", "Consultation");
                item.putIfAbsent("reason", "General consultation");
                item.putIfAbsent("durationMinutes", 30);
                item.putIfAbsent("scheduledAt", LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).toString());
            }
            case "billing" -> {
                item.putIfAbsent("billingId", item.get("id"));
                item.putIfAbsent("patientId", "PAT001");
                item.putIfAbsent("status", "Draft");
                item.putIfAbsent("items", List.of(Map.of(
                    "itemCode", "CONS001",
                    "description", "Consultation fee",
                    "quantity", 1,
                    "unitPrice", 200000,
                    "amount", 200000,
                    "category", "Consultation"
                )));
                item.putIfAbsent("subtotal", 200000);
                item.putIfAbsent("discount", 0);
                item.putIfAbsent("tax", 20000);
                item.putIfAbsent("total", 220000);
            }
            case "invoice" -> {
                item.putIfAbsent("invoiceId", item.get("id"));
                item.putIfAbsent("patientId", "PAT001");
                item.putIfAbsent("billingId", "BL001");
                item.putIfAbsent("status", "Pending");
                item.putIfAbsent("amount", 200000);
                item.putIfAbsent("paidAmount", 0);
                item.putIfAbsent("balance", 200000);
                item.putIfAbsent("dueDate", LocalDate.now().plusDays(30).toString());
            }
            case "insurance-claim" -> {
                item.putIfAbsent("claimId", item.get("id"));
                item.putIfAbsent("patientId", "PAT001");
                item.putIfAbsent("invoiceId", "INV001");
                item.putIfAbsent("status", "Draft");
                item.putIfAbsent("insuranceProvider", "Sample Insurance Co.");
                item.putIfAbsent("policyNumber", "POL-" + System.currentTimeMillis());
                item.putIfAbsent("claimAmount", 100000);
            }
            case "staff" -> {
                item.putIfAbsent("staffId", item.get("id"));
                item.putIfAbsent("fullName", "New Staff Member");
                item.putIfAbsent("employeeNumber", "EMP-" + System.currentTimeMillis());
                item.putIfAbsent("email", "staff@hospital.com");
                item.putIfAbsent("phone", "0900000000");
                item.putIfAbsent("position", "Staff");
                item.putIfAbsent("role", "General");
                item.putIfAbsent("employmentType", "FullTime");
                item.putIfAbsent("seniority", "Junior");
                item.putIfAbsent("hasLicense", false);
                item.putIfAbsent("hireDate", LocalDate.now().toString());
            }
            case "schedule" -> {
                item.putIfAbsent("scheduleId", item.get("id"));
                item.putIfAbsent("staffId", "STF001");
                item.putIfAbsent("status", "Scheduled");
                item.putIfAbsent("date", LocalDate.now().plusDays(1).toString());
                item.putIfAbsent("startTime", "08:00");
                item.putIfAbsent("endTime", "17:00");
                item.putIfAbsent("shiftType", "Morning");
            }
            case "training" -> {
                item.putIfAbsent("trainingId", item.get("id"));
                item.putIfAbsent("staffId", "STF001");
                item.putIfAbsent("status", "Scheduled");
                item.putIfAbsent("trainingName", "New Training Course");
                item.putIfAbsent("trainingType", "General");
                item.putIfAbsent("provider", "Internal");
            }
            case "incident" -> {
                item.putIfAbsent("caseId", item.get("id"));
                item.putIfAbsent("status", "Open");
                item.putIfAbsent("title", "New Security Incident");
                item.putIfAbsent("description", "Incident reported by " + user.getUserId());
                item.putIfAbsent("severity", "Medium");
                item.putIfAbsent("category", "Security");
                item.putIfAbsent("reportedBy", user.getUserId());
                item.putIfAbsent("reportedAt", LocalDateTime.now().toString());
                item.putIfAbsent("incidentDate", LocalDateTime.now().toString());
            }
            case "system-config" -> {
                item.putIfAbsent("configId", item.get("id"));
                item.putIfAbsent("configKey", "config.key." + System.currentTimeMillis());
                item.putIfAbsent("configValue", "default_value");
                item.putIfAbsent("dataType", "String");
                item.putIfAbsent("category", "General");
                item.putIfAbsent("description", "Configuration setting");
                item.putIfAbsent("encrypted", false);
                item.putIfAbsent("modifiedBy", user.getUserId());
            }
            case "access-policy" -> {
                item.putIfAbsent("policyId", item.get("id"));
                item.putIfAbsent("policyName", "New Policy");
                item.putIfAbsent("description", "Policy description");
                item.putIfAbsent("policyType", "RBAC");
                item.putIfAbsent("enabled", true);
                item.putIfAbsent("priority", 100);
                item.putIfAbsent("targetResource", "*");
                item.putIfAbsent("allowedActions", List.of("read"));
                item.putIfAbsent("modifiedBy", user.getUserId());
            }
            case "medical-report", "financial-report", "operation-report" -> {
                item.putIfAbsent("reportId", item.get("id"));
                item.putIfAbsent("reportType", resourceType.replace("-", " ").toUpperCase());
                item.putIfAbsent("reportName", "New Report - " + LocalDate.now());
                item.putIfAbsent("generatedBy", user.getUserId());
                item.putIfAbsent("periodStart", LocalDate.now().minusMonths(1).toString());
                item.putIfAbsent("periodEnd", LocalDate.now().toString());
                item.putIfAbsent("status", "Draft");
                item.putIfAbsent("data", Map.of("placeholder", "data"));
                item.putIfAbsent("summary", "Report summary pending");
                item.putIfAbsent("generatedAt", LocalDateTime.now().toString());
            }
        }
    }
    
    private void handleAction(String action, Map<String, Object> item, UserPrincipal user, Map<String, Object> request) {
        switch (action.toLowerCase()) {
            case "approve" -> {
                item.put("status", "Approved");
                item.put("approvedBy", user.getUserId());
                item.put("approvedAt", LocalDateTime.now().toString());
            }
            case "reject" -> {
                item.put("status", "Rejected");
                item.put("rejectedBy", user.getUserId());
                item.put("rejectedAt", LocalDateTime.now().toString());
                if (request != null && request.containsKey("reason")) {
                    item.put("rejectionReason", request.get("reason"));
                }
            }
            case "cancel" -> {
                item.put("status", "Cancelled");
                item.put("cancelledBy", user.getUserId());
                item.put("cancelledAt", LocalDateTime.now().toString());
            }
            case "complete" -> {
                item.put("status", "Completed");
                item.put("completedBy", user.getUserId());
                item.put("completedAt", LocalDateTime.now().toString());
            }
            case "check-in" -> {
                item.put("status", "CheckedIn");
                item.put("checkedInBy", user.getUserId());
                item.put("checkedInAt", LocalDateTime.now().toString());
            }
            case "resolve" -> {
                item.put("status", "Resolved");
                item.put("resolvedBy", user.getUserId());
                item.put("resolvedAt", LocalDateTime.now().toString());
                if (request != null && request.containsKey("resolution")) {
                    item.put("resolution", request.get("resolution"));
                }
            }
            case "export" -> {
                item.put("lastExportedBy", user.getUserId());
                item.put("lastExportedAt", LocalDateTime.now().toString());
                item.put("exportCount", ((Number) item.getOrDefault("exportCount", 0)).intValue() + 1);
            }
            case "assign" -> {
                if (request != null && request.containsKey("assigneeId")) {
                    item.put("assignedTo", request.get("assigneeId"));
                    item.put("assignedBy", user.getUserId());
                    item.put("assignedAt", LocalDateTime.now().toString());
                    if ("Open".equals(item.get("status"))) {
                        item.put("status", "InProgress");
                    }
                }
            }
            case "pay" -> {
                if (request != null) {
                    Number payAmount = (Number) request.getOrDefault("amount", 0);
                    Number currentPaid = (Number) item.getOrDefault("paidAmount", 0);
                    Number total = (Number) item.getOrDefault("amount", 0);
                    
                    double newPaid = currentPaid.doubleValue() + payAmount.doubleValue();
                    item.put("paidAmount", newPaid);
                    item.put("balance", total.doubleValue() - newPaid);
                    item.put("paymentMethod", request.getOrDefault("paymentMethod", "Cash"));
                    item.put("paidAt", LocalDateTime.now().toString());
                    
                    if (newPaid >= total.doubleValue()) {
                        item.put("status", "Paid");
                    } else {
                        item.put("status", "PartiallyPaid");
                    }
                }
            }
            case "submit" -> {
                item.put("status", "Submitted");
                item.put("submittedBy", user.getUserId());
                item.put("submittedAt", LocalDateTime.now().toString());
            }
            case "release" -> {
                item.put("status", "Released");
                item.put("releasedBy", user.getUserId());
                item.put("releasedAt", LocalDateTime.now().toString());
            }
            case "verify" -> {
                item.put("status", "Verified");
                item.put("verifiedBy", user.getUserId());
                item.put("verifiedAt", LocalDateTime.now().toString());
            }
            default -> {
                item.put("lastAction", action);
                item.put("lastActionBy", user.getUserId());
                item.put("lastActionAt", LocalDateTime.now().toString());
            }
        }
        item.put("updatedAt", LocalDateTime.now().toString());
        item.put("updatedBy", user.getUserId());
    }
    
    private static void initializeSampleData() {
        // Initialize sample patients
        Map<String, Object> patient1 = new HashMap<>();
        patient1.put("id", "PAT001");
        patient1.put("patientId", "PAT001");
        patient1.put("fullName", "Nguyễn Văn A");
        patient1.put("dateOfBirth", "1990-05-15");
        patient1.put("gender", "Nam");
        patient1.put("nationalId", "001234567890");
        patient1.put("phone", "0901234567");
        patient1.put("email", "nguyenvana@email.com");
        patient1.put("address", "123 Đường ABC, Quận 1, TP.HCM");
        patient1.put("branch", "BRANCH_HCM");
        patient1.put("department", "Internal Medicine");
        patient1.put("bloodType", "O+");
        patient1.put("allergies", "Penicillin");
        patient1.put("status", "Active");
        patient1.put("createdAt", LocalDateTime.now().minusDays(30).toString());
        
        Map<String, Object> patient2 = new HashMap<>();
        patient2.put("id", "PAT002");
        patient2.put("patientId", "PAT002");
        patient2.put("fullName", "Trần Thị B");
        patient2.put("dateOfBirth", "1985-08-20");
        patient2.put("gender", "Nữ");
        patient2.put("nationalId", "001234567891");
        patient2.put("phone", "0901234568");
        patient2.put("email", "tranthib@email.com");
        patient2.put("address", "456 Đường XYZ, Quận 3, TP.HCM");
        patient2.put("branch", "BRANCH_HN");
        patient2.put("department", "Cardiology");
        patient2.put("bloodType", "A+");
        patient2.put("allergies", "None");
        patient2.put("status", "Active");
        patient2.put("createdAt", LocalDateTime.now().minusDays(20).toString());
        
        dataStore.computeIfAbsent("patient", k -> new ConcurrentHashMap<>()).put("PAT001", patient1);
        dataStore.computeIfAbsent("patient", k -> new ConcurrentHashMap<>()).put("PAT002", patient2);
        
        // Initialize sample medical records
        Map<String, Object> record1 = new HashMap<>();
        record1.put("id", "MR001");
        record1.put("recordId", "MR001");
        record1.put("patientId", "PAT001");
        record1.put("doctorId", "DOC001");
        record1.put("diagnosis", "Hypertension Stage 2, Type 2 Diabetes");
        record1.put("symptoms", "Headache, fatigue, frequent urination");
        record1.put("treatment", "Amlodipine 10mg daily, Metformin 500mg BID");
        record1.put("branch", "BRANCH_HCM");
        record1.put("department", "Internal Medicine");
        record1.put("sensitivity", "Normal");
        record1.put("status", "Active");
        record1.put("visitDate", LocalDateTime.now().minusDays(7).toString());
        record1.put("createdAt", LocalDateTime.now().minusDays(7).toString());
        
        dataStore.computeIfAbsent("medical-record", k -> new ConcurrentHashMap<>()).put("MR001", record1);
        
        // Initialize sample prescription
        Map<String, Object> prescription1 = new HashMap<>();
        prescription1.put("id", "RX001");
        prescription1.put("prescriptionId", "RX001");
        prescription1.put("patientId", "PAT001");
        prescription1.put("doctorId", "DOC001");
        prescription1.put("status", "Pending");
        prescription1.put("medications", List.of(
            Map.of("medicationName", "Amlodipine", "dosage", "10mg", "frequency", "Once daily", "duration", 30),
            Map.of("medicationName", "Metformin", "dosage", "500mg", "frequency", "Twice daily", "duration", 30)
        ));
        prescription1.put("instructions", "Take medications as prescribed. Monitor blood pressure daily.");
        prescription1.put("branch", "BRANCH_HCM");
        prescription1.put("department", "Internal Medicine");
        prescription1.put("createdBy", "DOC001");
        prescription1.put("createdAt", LocalDateTime.now().minusDays(5).toString());
        
        dataStore.computeIfAbsent("prescription", k -> new ConcurrentHashMap<>()).put("RX001", prescription1);
        
        // Initialize sample admission
        Map<String, Object> admission1 = new HashMap<>();
        admission1.put("id", "ADM001");
        admission1.put("admissionId", "ADM001");
        admission1.put("patientId", "PAT001");
        admission1.put("admittedBy", "REC001");
        admission1.put("attendingDoctorId", "DOC001");
        admission1.put("admissionType", "Elective");
        admission1.put("status", "Discharged");
        admission1.put("chiefComplaint", "Uncontrolled hypertension and diabetes");
        admission1.put("admissionDiagnosis", "Hypertension Stage 2, Type 2 DM");
        admission1.put("roomNumber", "301");
        admission1.put("bedNumber", "A");
        admission1.put("branch", "BRANCH_HCM");
        admission1.put("department", "Internal Medicine");
        admission1.put("admittedAt", LocalDateTime.now().minusDays(7).toString());
        admission1.put("dischargedAt", LocalDateTime.now().minusDays(3).toString());
        
        dataStore.computeIfAbsent("admission", k -> new ConcurrentHashMap<>()).put("ADM001", admission1);
        
        // Initialize sample staff
        Map<String, Object> staff1 = new HashMap<>();
        staff1.put("id", "STF001");
        staff1.put("staffId", "STF001");
        staff1.put("employeeNumber", "EMP-2020-001");
        staff1.put("fullName", "Dr. Nguyễn Minh Tuấn");
        staff1.put("email", "tuannm@hospital.com");
        staff1.put("phone", "0901234570");
        staff1.put("position", "Senior Doctor");
        staff1.put("department", "Internal Medicine");
        staff1.put("branch", "BRANCH_HCM");
        staff1.put("role", "Doctor");
        staff1.put("employmentType", "FullTime");
        staff1.put("seniority", "Senior");
        staff1.put("hasLicense", true);
        staff1.put("licenseNumber", "MED-HCM-12345");
        staff1.put("status", "Active");
        staff1.put("hireDate", "2020-01-15");
        
        dataStore.computeIfAbsent("staff", k -> new ConcurrentHashMap<>()).put("STF001", staff1);
        
        // Initialize sample incident
        Map<String, Object> incident1 = new HashMap<>();
        incident1.put("id", "INC001");
        incident1.put("caseId", "INC001");
        incident1.put("title", "Unauthorized Access Attempt Detected");
        incident1.put("description", "Multiple failed login attempts detected from IP 192.168.1.100");
        incident1.put("severity", "Medium");
        incident1.put("category", "Security");
        incident1.put("status", "Resolved");
        incident1.put("reportedBy", "SYSTEM");
        incident1.put("assignedTo", "SECADMIN001");
        incident1.put("resolution", "IP blocked, password reset required");
        incident1.put("incidentDate", LocalDateTime.now().minusDays(5).toString());
        incident1.put("reportedAt", LocalDateTime.now().minusDays(5).toString());
        incident1.put("resolvedAt", LocalDateTime.now().minusDays(4).toString());
        
        dataStore.computeIfAbsent("incident", k -> new ConcurrentHashMap<>()).put("INC001", incident1);
        
        log.info("Sample mock data initialized for UnifiedMockController");
    }
}

