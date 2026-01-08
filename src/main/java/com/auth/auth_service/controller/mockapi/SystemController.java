package com.auth.auth_service.controller.mockapi;

import com.auth.auth_service.dto.ApiResponse;
import com.auth.auth_service.dto.AuthorizationRequest;
import com.auth.auth_service.dto.AuthorizationResponse;
import com.auth.auth_service.dto.mock.AccessPolicyDto;
import com.auth.auth_service.dto.mock.SystemConfigDto;
import com.auth.auth_service.entity.AuditLog;
import com.auth.auth_service.repository.AuditLogRepository;
import com.auth.auth_service.security.UserPrincipal;
import com.auth.auth_service.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Mock API Controller for System Configuration and Access Policies
 * SystemConfig: ITAdmin (read, update), SecurityAdmin (read)
 * AccessPolicy: ITAdmin (read), SecurityAdmin (read, update)
 * AuditLog: ITAdmin (read), SecurityAdmin (read)
 */
@RestController
@RequestMapping("/api/mock/system")
@RequiredArgsConstructor
@Slf4j
public class SystemController {
    
    private final AuthorizationService authorizationService;
    private final AuditLogRepository auditLogRepository;
    
    private static final Map<String, SystemConfigDto> mockConfigs = new HashMap<>();
    private static final Map<String, AccessPolicyDto> mockPolicies = new HashMap<>();
    
    static {
        // System Configurations
        mockConfigs.put("CFG001", SystemConfigDto.builder()
                .configId("CFG001")
                .configKey("session.timeout")
                .configValue("30")
                .dataType("Integer")
                .category("Security")
                .description("Session timeout in minutes")
                .encrypted(false)
                .modifiedBy("ITADMIN001")
                .createdAt(LocalDateTime.now().minusMonths(6))
                .updatedAt(LocalDateTime.now().minusDays(30))
                .build());
        
        mockConfigs.put("CFG002", SystemConfigDto.builder()
                .configId("CFG002")
                .configKey("password.min_length")
                .configValue("12")
                .dataType("Integer")
                .category("Security")
                .description("Minimum password length")
                .encrypted(false)
                .modifiedBy("ITADMIN001")
                .createdAt(LocalDateTime.now().minusMonths(6))
                .updatedAt(LocalDateTime.now().minusMonths(3))
                .build());
        
        mockConfigs.put("CFG003", SystemConfigDto.builder()
                .configId("CFG003")
                .configKey("audit.retention_days")
                .configValue("365")
                .dataType("Integer")
                .category("System")
                .description("Audit log retention period in days")
                .encrypted(false)
                .modifiedBy("SECADMIN001")
                .createdAt(LocalDateTime.now().minusMonths(6))
                .updatedAt(LocalDateTime.now().minusMonths(1))
                .build());
        
        mockConfigs.put("CFG004", SystemConfigDto.builder()
                .configId("CFG004")
                .configKey("notification.email_server")
                .configValue("smtp.hospital.local")
                .dataType("String")
                .category("Notification")
                .description("SMTP server for email notifications")
                .encrypted(false)
                .modifiedBy("ITADMIN001")
                .createdAt(LocalDateTime.now().minusMonths(6))
                .updatedAt(LocalDateTime.now().minusMonths(2))
                .build());
        
        mockConfigs.put("CFG005", SystemConfigDto.builder()
                .configId("CFG005")
                .configKey("integration.lab_system_key")
                .configValue("***ENCRYPTED***")
                .dataType("String")
                .category("Integration")
                .description("API key for laboratory system integration")
                .encrypted(true)
                .modifiedBy("ITADMIN001")
                .createdAt(LocalDateTime.now().minusMonths(3))
                .updatedAt(LocalDateTime.now().minusMonths(1))
                .build());
        
        // Access Policies
        mockPolicies.put("POL001", AccessPolicyDto.builder()
                .policyId("POL001")
                .policyName("Doctor Clinical Access")
                .description("Allows doctors to access clinical data for assigned patients")
                .policyType("RBAC")
                .enabled(true)
                .priority(100)
                .targetRole("Doctor")
                .targetResource("MedicalRecord")
                .allowedActions(List.of("read", "create", "update"))
                .conditions(Map.of(
                        "patient_assigned", true,
                        "same_branch", true
                ))
                .modifiedBy("SECADMIN001")
                .createdAt(LocalDateTime.now().minusMonths(6))
                .updatedAt(LocalDateTime.now().minusMonths(1))
                .build());
        
        mockPolicies.put("POL002", AccessPolicyDto.builder()
                .policyId("POL002")
                .policyName("Off-Hours MFA Requirement")
                .description("Requires MFA for access outside business hours")
                .policyType("ABAC")
                .enabled(true)
                .priority(50)
                .targetRole(null)
                .targetResource("*")
                .allowedActions(List.of("*"))
                .conditions(Map.of(
                        "time_before", "08:00",
                        "time_after", "18:00",
                        "requires_mfa", true
                ))
                .modifiedBy("SECADMIN001")
                .createdAt(LocalDateTime.now().minusMonths(6))
                .updatedAt(LocalDateTime.now().minusDays(15))
                .build());
        
        mockPolicies.put("POL003", AccessPolicyDto.builder()
                .policyId("POL003")
                .policyName("No Delete Patient Data")
                .description("Prevents deletion of patient-related data")
                .policyType("RBAC")
                .enabled(true)
                .priority(200)
                .targetRole("*")
                .targetResource("PatientProfile,MedicalRecord,ClinicalNote")
                .allowedActions(List.of())
                .conditions(Map.of(
                        "action", "delete",
                        "effect", "deny"
                ))
                .modifiedBy("SECADMIN001")
                .createdAt(LocalDateTime.now().minusMonths(6))
                .updatedAt(LocalDateTime.now().minusMonths(6))
                .build());
        
        mockPolicies.put("POL004", AccessPolicyDto.builder()
                .policyId("POL004")
                .policyName("Export Approval Required")
                .description("Requires approval or emergency mode for medical record export")
                .policyType("ABAC")
                .enabled(true)
                .priority(150)
                .targetRole(null)
                .targetResource("MedicalRecord")
                .allowedActions(List.of("export"))
                .conditions(Map.of(
                        "requires_approval", true,
                        "emergency_mode_allows", true,
                        "emergency_mode_role", "SecurityAdmin"
                ))
                .modifiedBy("SECADMIN001")
                .createdAt(LocalDateTime.now().minusMonths(3))
                .updatedAt(LocalDateTime.now().minusMonths(1))
                .build());
    }
    
    // ==================== System Configuration ====================
    
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<List<SystemConfigDto>>> getAllConfigs(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String category) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "SystemConfig", "read");
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<SystemConfigDto> configs = mockConfigs.values().stream()
                .filter(c -> category == null || c.getCategory().equals(category))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("System configurations retrieved", configs));
    }
    
    @GetMapping("/config/{configId}")
    public ResponseEntity<ApiResponse<SystemConfigDto>> getConfig(
            @PathVariable String configId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        SystemConfigDto config = mockConfigs.get(configId);
        if (config == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Configuration not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "SystemConfig", "read");
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        return ResponseEntity.ok(ApiResponse.success(config));
    }
    
    @PutMapping("/config/{configId}")
    public ResponseEntity<ApiResponse<SystemConfigDto>> updateConfig(
            @PathVariable String configId,
            @RequestBody SystemConfigDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        SystemConfigDto existing = mockConfigs.get(configId);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Configuration not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "SystemConfig", "update");
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        if (request.getConfigValue() != null) existing.setConfigValue(request.getConfigValue());
        if (request.getDescription() != null) existing.setDescription(request.getDescription());
        existing.setModifiedBy(user.getUserId());
        existing.setUpdatedAt(LocalDateTime.now());
        
        log.info("System config updated by {}: {}", user.getUserId(), configId);
        return ResponseEntity.ok(ApiResponse.success("Configuration updated", existing));
    }
    
    // ==================== Access Policies ====================
    
    @GetMapping("/policies")
    public ResponseEntity<ApiResponse<List<AccessPolicyDto>>> getAllPolicies(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String policyType,
            @RequestParam(required = false) Boolean enabled) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "AccessPolicy", "read");
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<AccessPolicyDto> policies = mockPolicies.values().stream()
                .filter(p -> policyType == null || p.getPolicyType().equals(policyType))
                .filter(p -> enabled == null || p.isEnabled() == enabled)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Access policies retrieved", policies));
    }
    
    @GetMapping("/policies/{policyId}")
    public ResponseEntity<ApiResponse<AccessPolicyDto>> getPolicy(
            @PathVariable String policyId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        AccessPolicyDto policy = mockPolicies.get(policyId);
        if (policy == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Policy not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "AccessPolicy", "read");
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        return ResponseEntity.ok(ApiResponse.success(policy));
    }
    
    @PutMapping("/policies/{policyId}")
    public ResponseEntity<ApiResponse<AccessPolicyDto>> updatePolicy(
            @PathVariable String policyId,
            @RequestBody AccessPolicyDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        AccessPolicyDto existing = mockPolicies.get(policyId);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Policy not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "AccessPolicy", "update");
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        if (request.getDescription() != null) existing.setDescription(request.getDescription());
        if (request.getConditions() != null) existing.setConditions(request.getConditions());
        existing.setEnabled(request.isEnabled());
        if (request.getPriority() != null) existing.setPriority(request.getPriority());
        existing.setModifiedBy(user.getUserId());
        existing.setUpdatedAt(LocalDateTime.now());
        
        log.info("Access policy updated by {}: {}", user.getUserId(), policyId);
        return ResponseEntity.ok(ApiResponse.success("Policy updated", existing));
    }
    
    // ==================== Audit Logs ====================
    
    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getAuditLogs(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Boolean allowed,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "AuditLog", "read");
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        // Fetch from actual audit log repository
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        List<AuditLog> logs = auditLogRepository.findAll(pageRequest).getContent();
        
        // Apply filters
        if (userId != null) {
            logs = logs.stream().filter(l -> userId.equals(l.getUserId())).toList();
        }
        if (resourceType != null) {
            logs = logs.stream().filter(l -> resourceType.equals(l.getResourceType())).toList();
        }
        if (allowed != null) {
            logs = logs.stream().filter(l -> allowed.equals(l.isAllowed())).toList();
        }
        
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved", logs));
    }
    
    @GetMapping("/audit-logs/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAuditStats(
            @AuthenticationPrincipal UserPrincipal user) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "AuditLog", "read");
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        // Calculate stats from audit logs
        List<AuditLog> allLogs = auditLogRepository.findAll();
        
        long totalEntries = allLogs.size();
        long allowedCount = allLogs.stream().filter(AuditLog::isAllowed).count();
        long deniedCount = totalEntries - allowedCount;
        double denyRate = totalEntries > 0 ? (double) deniedCount / totalEntries * 100 : 0;
        
        Map<String, Long> byResourceType = new HashMap<>();
        allLogs.forEach(l -> byResourceType.merge(l.getResourceType(), 1L, Long::sum));
        
        Map<String, Long> byAction = new HashMap<>();
        allLogs.forEach(l -> byAction.merge(l.getAction(), 1L, Long::sum));
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEntries", totalEntries);
        stats.put("allowedCount", allowedCount);
        stats.put("deniedCount", deniedCount);
        stats.put("denyRate", String.format("%.2f%%", denyRate));
        stats.put("byResourceType", byResourceType);
        stats.put("byAction", byAction);
        
        return ResponseEntity.ok(ApiResponse.success("Audit statistics", stats));
    }
    
    private AuthorizationResponse checkAuthorization(UserPrincipal user, String resourceType, String action) {
        AuthorizationRequest request = AuthorizationRequest.builder()
                .resourceType(resourceType)
                .action(action)
                .build();
        
        return authorizationService.authorize(request);
    }
}

