package com.auth.auth_service.service;

import com.auth.auth_service.dto.AuthorizationRequest;
import com.auth.auth_service.dto.AuthorizationResponse;
import com.auth.auth_service.entity.AuditLog;
import com.auth.auth_service.repository.AuditLogRepository;
import com.auth.auth_service.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static java.util.Map.entry;

/**
 * Authorization Service implementing RBAC + ABAC logic
 * Based on emr_authz.rego policy from PoweredAI-RBAC
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {
    
    private final AuditLogRepository auditLogRepository;
    
    // Role-based permissions mapping (from emr_authz.rego)
    private static final Map<String, Map<String, Set<String>>> ROLE_PERMISSIONS = new HashMap<>();
    
    // High-risk actions and resources
    private static final Set<String> HIGH_RISK_ACTIONS = Set.of("export", "delete");
    private static final Set<String> HIGH_RISK_RESOURCES = Set.of("MedicalRecord", "AuditLog", "AccessPolicy", "SystemConfig");
    
    // Clinical resources
    private static final Set<String> CLINICAL_RESOURCES = Set.of(
            "MedicalRecord", "ClinicalNote", "VitalSigns", "Prescription", 
            "LabResult", "ImagingResult", "DischargeSummary"
    );
    
    static {
        // Doctor permissions (13 entries - use Map.ofEntries)
        ROLE_PERMISSIONS.put("Doctor", Map.ofEntries(
                entry("PatientProfile", Set.of("read")),
                entry("MedicalRecord", Set.of("read", "create", "update")),
                entry("ClinicalNote", Set.of("read", "create")),
                entry("VitalSigns", Set.of("read")),
                entry("Prescription", Set.of("read", "create", "update", "approve")),
                entry("LabOrder", Set.of("create", "read")),
                entry("LabResult", Set.of("read")),
                entry("ImagingOrder", Set.of("create", "read")),
                entry("ImagingResult", Set.of("read")),
                entry("AdmissionRecord", Set.of("read")),
                entry("TransferRecord", Set.of("read")),
                entry("DischargeSummary", Set.of("create", "read")),
                entry("MedicalReport", Set.of("read"))
        ));
        
        // Nurse permissions (9 entries - can use Map.of)
        ROLE_PERMISSIONS.put("Nurse", Map.of(
                "PatientProfile", Set.of("read"),
                "MedicalRecord", Set.of("read"),
                "ClinicalNote", Set.of("read"),
                "VitalSigns", Set.of("read", "create", "update"),
                "LabResult", Set.of("read"),
                "ImagingResult", Set.of("read"),
                "AdmissionRecord", Set.of("read"),
                "TransferRecord", Set.of("read"),
                "DischargeSummary", Set.of("read")
        ));
        
        // Receptionist permissions
        ROLE_PERMISSIONS.put("Receptionist", Map.of(
                "PatientProfile", Set.of("create", "read", "update"),
                "Appointment", Set.of("create", "read", "update"),
                "AdmissionRecord", Set.of("create", "read"),
                "TransferRecord", Set.of("create", "read")
        ));
        
        // Cashier permissions
        ROLE_PERMISSIONS.put("Cashier", Map.of(
                "BillingRecord", Set.of("create", "read", "update"),
                "Invoice", Set.of("create", "read", "update"),
                "InsuranceClaim", Set.of("create", "read", "update"),
                "FinancialReport", Set.of("read")
        ));
        
        // HR permissions
        ROLE_PERMISSIONS.put("HR", Map.of(
                "StaffProfile", Set.of("create", "read", "update"),
                "WorkSchedule", Set.of("create", "read", "update"),
                "TrainingRecord", Set.of("create", "read", "update"),
                "OperationReport", Set.of("read")
        ));
        
        // Manager permissions
        ROLE_PERMISSIONS.put("Manager", Map.of(
                "MedicalReport", Set.of("read"),
                "OperationReport", Set.of("read"),
                "FinancialReport", Set.of("read"),
                "WorkSchedule", Set.of("read"),
                "StaffProfile", Set.of("read")
        ));
        
        // ITAdmin permissions
        ROLE_PERMISSIONS.put("ITAdmin", Map.of(
                "SystemConfig", Set.of("read", "update"),
                "AccessPolicy", Set.of("read"),
                "AuditLog", Set.of("read")
        ));
        
        // SecurityAdmin permissions
        ROLE_PERMISSIONS.put("SecurityAdmin", Map.of(
                "AuditLog", Set.of("read"),
                "IncidentCase", Set.of("create", "read", "update"),
                "AccessPolicy", Set.of("read", "update"),
                "SystemConfig", Set.of("read")
        ));
    }
    
    @Transactional
    public AuthorizationResponse authorize(AuthorizationRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        
        List<String> denyReasons = new ArrayList<>();
        List<Map<String, Object>> obligations = new ArrayList<>();
        
        // Check explicit deny rules first
        checkDenyRules(user, request, denyReasons);
        
        boolean rbacAllows = checkRbacPermission(user.getRole(), request.getResourceType(), request.getAction());
        boolean abacOk = checkAbacConditions(user, request);
        
        boolean allowed = rbacAllows && abacOk && denyReasons.isEmpty();
        
        // Calculate risk score
        int riskScore = calculateRiskScore(request);
        
        // Add obligations if allowed
        if (allowed) {
            addObligations(user, request, obligations, riskScore);
        }
        
        // Generate policy ID
        String policyId = allowed 
                ? "ALLOW_" + user.getRole() + "_" + request.getResourceType() + "_" + request.getAction()
                : "DENY_" + (denyReasons.isEmpty() ? "UNAUTHORIZED" : String.join("_", denyReasons));
        
        // Log the authorization decision
//        logAuthorizationDecision(user, request, allowed, policyId, denyReasons, riskScore);
        
        return AuthorizationResponse.builder()
                .allowed(allowed)
                .policyId(policyId)
                .denyReasons(denyReasons)
                .obligations(obligations)
                .riskScore(riskScore)
                .build();
    }
    
    private boolean checkRbacPermission(String role, String resourceType, String action) {
        Map<String, Set<String>> rolePerms = ROLE_PERMISSIONS.get(role);
        if (rolePerms == null) {
            return false;
        }
        Set<String> allowedActions = rolePerms.get(resourceType);
        return allowedActions != null && allowedActions.contains(action);
    }
    
    private void checkDenyRules(UserPrincipal user, AuthorizationRequest request, List<String> denyReasons) {
        String role = user.getRole();
        String resourceType = request.getResourceType();
        String action = request.getAction();
        
        // Receptionist cannot access clinical data
        if ("Receptionist".equals(role) && Set.of("MedicalRecord", "ClinicalNote", "VitalSigns", 
                "Prescription", "LabResult", "ImagingResult", "DischargeSummary").contains(resourceType)) {
            denyReasons.add("RECEPTIONIST_NO_CLINICAL_ACCESS");
        }
        
        // Cashier cannot access clinical data
        if ("Cashier".equals(role) && Set.of("MedicalRecord", "ClinicalNote", "VitalSigns", 
                "Prescription", "LabResult", "ImagingResult").contains(resourceType)) {
            denyReasons.add("CASHIER_NO_CLINICAL_ACCESS");
        }
        
        // HR cannot access patient or finance data
        if ("HR".equals(role) && Set.of("MedicalRecord", "ClinicalNote", "VitalSigns", 
                "Prescription", "LabResult", "ImagingResult", "BillingRecord", 
                "Invoice", "InsuranceClaim").contains(resourceType)) {
            denyReasons.add("HR_NO_PATIENT_OR_FINANCE_ACCESS");
        }
        
        // ITAdmin cannot access patient data
        if ("ITAdmin".equals(role) && Set.of("MedicalRecord", "ClinicalNote", "VitalSigns", 
                "Prescription", "LabResult", "ImagingResult", "PatientProfile").contains(resourceType)) {
            denyReasons.add("ITADMIN_NO_PATIENT_DATA");
        }
        
        // No delete for patient data
        if ("delete".equals(action) && Set.of("MedicalRecord", "ClinicalNote", "VitalSigns", 
                "Prescription", "LabResult", "ImagingResult", "PatientProfile").contains(resourceType)) {
            denyReasons.add("NO_DELETE_PATIENT_DATA");
        }
        
        // Export controls
        if ("export".equals(action) && "MedicalRecord".equals(resourceType)) {
            Map<String, Object> env = request.getEnvironment();
            boolean emergencyMode = env != null && Boolean.TRUE.equals(env.get("emergency_mode"));
            boolean exportApproved = env != null && Boolean.TRUE.equals(env.get("export_approved"));
            
            if (!emergencyMode && !exportApproved) {
                denyReasons.add("EXPORT_REQUIRES_APPROVAL_OR_EMERGENCY");
            } else if (emergencyMode && !"SecurityAdmin".equals(role)) {
                denyReasons.add("ONLY_SECURITYADMIN_CAN_EXPORT_IN_EMERGENCY");
            }
        }
        
        // Branch mismatch for non-cross-branch roles
        if (Set.of("Doctor", "Nurse", "Receptionist", "Cashier", "HR").contains(role)) {
            if (request.getResourceBranch() != null && !request.getResourceBranch().equals(user.getBranch())) {
                denyReasons.add("BRANCH_MISMATCH");
            }
        }
        
        // SoD: Creator cannot approve
        if ("approve".equals(action) && Set.of("Invoice", "InsuranceClaim", "Prescription").contains(resourceType)) {
            if (user.getUserId().equals(request.getCreatedBy())) {
                denyReasons.add("SOD_CREATOR_CANNOT_APPROVE");
            }
        }
    }
    
    private boolean checkAbacConditions(UserPrincipal user, AuthorizationRequest request) {
        String role = user.getRole();
        String resourceType = request.getResourceType();
        
        // Clinical resources ABAC
        if (CLINICAL_RESOURCES.contains(resourceType)) {
            return checkClinicalAbac(user, request);
        }
        
        // PatientProfile ABAC
        if ("PatientProfile".equals(resourceType)) {
            return checkSameBranch(user, request);
        }
        
        // Appointment ABAC
        if ("Appointment".equals(resourceType)) {
            return checkSameBranch(user, request);
        }
        
        // Admission/Transfer ABAC
        if (Set.of("AdmissionRecord", "TransferRecord").contains(resourceType)) {
            return checkSameBranch(user, request);
        }
        
        // Billing ABAC
        if (Set.of("BillingRecord", "Invoice", "InsuranceClaim").contains(resourceType)) {
            return checkSameBranch(user, request);
        }
        
        // Staff ABAC
        if (Set.of("StaffProfile", "WorkSchedule", "TrainingRecord").contains(resourceType)) {
            return checkStaffAbac(user, request);
        }
        
        // Reports ABAC
        if (Set.of("MedicalReport", "OperationReport", "FinancialReport").contains(resourceType)) {
            return checkReportAbac(user, request);
        }
        
        // System/Security ABAC
        if (Set.of("SystemConfig", "AccessPolicy", "AuditLog", "IncidentCase").contains(resourceType)) {
            return Set.of("ITAdmin", "SecurityAdmin").contains(role);
        }
        
        return true;
    }
    
    private boolean checkClinicalAbac(UserPrincipal user, AuthorizationRequest request) {
        String role = user.getRole();
        
        if ("Doctor".equals(role)) {
            return checkPatientAssigned(user, request);
        }
        
        if ("Nurse".equals(role)) {
            return checkPatientAssigned(user, request) && checkSameDepartment(user, request);
        }
        
        return false;
    }
    
    private boolean checkPatientAssigned(UserPrincipal user, AuthorizationRequest request) {
        if (request.getPatientId() == null) {
            return true; // No specific patient required
        }
        return user.getAssignedPatients() != null && 
               user.getAssignedPatients().contains(request.getPatientId());
    }
    
    private boolean checkSameBranch(UserPrincipal user, AuthorizationRequest request) {
        if (request.getResourceBranch() == null) {
            return true;
        }
        return user.getBranch().equals(request.getResourceBranch());
    }
    
    private boolean checkSameDepartment(UserPrincipal user, AuthorizationRequest request) {
        if (request.getResourceDepartment() == null) {
            return true;
        }
        return user.getDepartment().equals(request.getResourceDepartment());
    }
    
    private boolean checkStaffAbac(UserPrincipal user, AuthorizationRequest request) {
        String role = user.getRole();
        
        if ("HR".equals(role)) {
            return checkSameBranch(user, request);
        }
        
        if ("Manager".equals(role)) {
            return checkSameBranch(user, request) && checkSameDepartment(user, request);
        }
        
        return false;
    }
    
    private boolean checkReportAbac(UserPrincipal user, AuthorizationRequest request) {
        String role = user.getRole();
        String resourceType = request.getResourceType();
        
        if (checkSameBranch(user, request)) {
            return true;
        }
        
        if ("MedicalReport".equals(resourceType) && Set.of("Manager", "Doctor").contains(role)) {
            if ("Manager".equals(role)) {
                return checkSameDepartment(user, request);
            }
            return true;
        }
        
        return false;
    }
    
    private int calculateRiskScore(AuthorizationRequest request) {
        int score = 0;
        
        // Off-hours check (before 8 AM or after 6 PM)
        int currentHour = LocalTime.now().getHour();
        if (currentHour < 8 || currentHour > 18) {
            score += 2;
        }
        
        // Export action
        if ("export".equals(request.getAction())) {
            score += 3;
        }
        
        // High sensitivity
        if ("High".equals(request.getResourceSensitivity())) {
            score += 2;
        }
        
        // High-risk resources
        if (HIGH_RISK_RESOURCES.contains(request.getResourceType())) {
            score += 3;
        }
        
        // High-risk actions
        if (HIGH_RISK_ACTIONS.contains(request.getAction())) {
            score += 2;
        }
        
        return score;
    }
    
    private void addObligations(UserPrincipal user, AuthorizationRequest request, 
                                List<Map<String, Object>> obligations, int riskScore) {
        // Off-hours MFA requirement
        int currentHour = LocalTime.now().getHour();
        if (currentHour < 8 || currentHour > 18) {
            Map<String, Object> obligation = new HashMap<>();
            obligation.put("type", "step_up_mfa");
            obligation.put("reason", "off_hours");
            obligations.add(obligation);
        }
        
        // Mask fields for PatientProfile (non-Receptionist)
        if ("PatientProfile".equals(request.getResourceType()) && !"Receptionist".equals(user.getRole())) {
            Map<String, Object> obligation = new HashMap<>();
            obligation.put("type", "mask_fields");
            obligation.put("fields", List.of("national_id", "address"));
            obligation.put("reason", "privacy_minimization");
            obligations.add(obligation);
        }
        
        // Log high-risk actions
        if (HIGH_RISK_ACTIONS.contains(request.getAction())) {
            Map<String, Object> obligation = new HashMap<>();
            obligation.put("type", "log_high_risk");
            obligation.put("reason", "high_risk_action");
            obligations.add(obligation);
        }
        
        // Export approval reference
        if ("export".equals(request.getAction())) {
            Map<String, Object> obligation = new HashMap<>();
            obligation.put("type", "require_approval_ref");
            obligation.put("field", "environment.approval_ticket_id");
            obligations.add(obligation);
        }
        
        // Rate limit for bulk access
        Map<String, Object> env = request.getEnvironment();
        if (env != null && Boolean.TRUE.equals(env.get("is_bulk"))) {
            Map<String, Object> obligation = new HashMap<>();
            obligation.put("type", "rate_limit");
            obligation.put("limit_per_minute", 60);
            obligation.put("reason", "bulk_access_control");
            obligations.add(obligation);
        }
    }
    
    private void logAuthorizationDecision(UserPrincipal user, AuthorizationRequest request,
                                          boolean allowed, String policyId, 
                                          List<String> denyReasons, int riskScore) {
        AuditLog auditLog = AuditLog.builder()
                .userId(user.getUserId())
                .resourceType(request.getResourceType())
                .resourceId(request.getResourceId() != null ? request.getResourceId() : "N/A")
                .action(request.getAction())
                .allowed(allowed)
                .policyId(policyId)
                .denyReasons(denyReasons.isEmpty() ? null : String.join(", ", denyReasons))
                .riskScore(riskScore)
                .timestamp(LocalDateTime.now())
                .build();
        
        auditLogRepository.save(auditLog);
        
        log.info("Authorization decision: user={}, resource={}, action={}, allowed={}, policy={}",
                user.getUserId(), request.getResourceType(), request.getAction(), allowed, policyId);
    }
    
    /**
     * Quick permission check without logging (for UI/filtering purposes)
     */
    public boolean hasPermission(String resourceType, String action) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            return false;
        }
        
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return checkRbacPermission(user.getRole(), resourceType, action);
    }
}
