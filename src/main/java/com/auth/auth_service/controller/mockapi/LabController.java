package com.auth.auth_service.controller.mockapi;

import com.auth.auth_service.dto.ApiResponse;
import com.auth.auth_service.dto.AuthorizationRequest;
import com.auth.auth_service.dto.AuthorizationResponse;
import com.auth.auth_service.dto.mock.LabOrderDto;
import com.auth.auth_service.dto.mock.LabResultDto;
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
 * Mock API Controller for Lab Orders and Lab Results
 * LabOrder: Doctor (create, read)
 * LabResult: Doctor (read), Nurse (read)
 */
@RestController
@RequestMapping("/api/mock/lab")
@RequiredArgsConstructor
@Slf4j
public class LabController {
    
    private final AuthorizationService authorizationService;
    
    private static final Map<String, LabOrderDto> mockLabOrders = new HashMap<>();
    private static final Map<String, LabResultDto> mockLabResults = new HashMap<>();
    
    static {
        // Lab Orders
        mockLabOrders.put("LO001", LabOrderDto.builder()
                .orderId("LO001")
                .patientId("PAT001")
                .orderedBy("DOC001")
                .status("Completed")
                .tests(List.of("Complete Blood Count", "HbA1c", "Lipid Panel"))
                .priority("Routine")
                .clinicalNotes("Baseline tests for diabetes management")
                .branch("BRANCH_HCM")
                .department("Internal Medicine")
                .orderedAt(LocalDateTime.now().minusDays(3))
                .completedAt(LocalDateTime.now().minusDays(2))
                .build());
        
        mockLabOrders.put("LO002", LabOrderDto.builder()
                .orderId("LO002")
                .patientId("PAT002")
                .orderedBy("DOC002")
                .status("InProgress")
                .tests(List.of("Troponin I", "BNP", "Creatinine"))
                .priority("STAT")
                .clinicalNotes("Cardiac markers post-PCI")
                .branch("BRANCH_HCM")
                .department("Cardiology")
                .orderedAt(LocalDateTime.now().minusHours(6))
                .completedAt(null)
                .build());
        
        // Lab Results
        mockLabResults.put("LR001", LabResultDto.builder()
                .resultId("LR001")
                .orderId("LO001")
                .patientId("PAT001")
                .performedBy("LAB001")
                .verifiedBy("LAB002")
                .status("Released")
                .results(List.of(
                        LabResultDto.TestResult.builder()
                                .testName("HbA1c")
                                .value("8.2")
                                .unit("%")
                                .referenceRange("4.0-5.6")
                                .flag("High")
                                .build(),
                        LabResultDto.TestResult.builder()
                                .testName("Fasting Glucose")
                                .value("145")
                                .unit("mg/dL")
                                .referenceRange("70-100")
                                .flag("High")
                                .build(),
                        LabResultDto.TestResult.builder()
                                .testName("Total Cholesterol")
                                .value("210")
                                .unit("mg/dL")
                                .referenceRange("<200")
                                .flag("High")
                                .build()
                ))
                .interpretation("Suboptimal glycemic control. Elevated cholesterol.")
                .branch("BRANCH_HCM")
                .department("Internal Medicine")
                .performedAt(LocalDateTime.now().minusDays(2))
                .releasedAt(LocalDateTime.now().minusDays(2))
                .build());
    }
    
    // ==================== Lab Orders ====================
    
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<LabOrderDto>>> getAllLabOrders(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String status) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "LabOrder", "read", null, null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<LabOrderDto> orders = mockLabOrders.values().stream()
                .filter(o -> patientId == null || o.getPatientId().equals(patientId))
                .filter(o -> status == null || o.getStatus().equals(status))
                .filter(o -> user.getBranch().equals(o.getBranch()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Lab orders retrieved", orders));
    }
    
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<LabOrderDto>> getLabOrder(
            @PathVariable String orderId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        LabOrderDto order = mockLabOrders.get(orderId);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Lab order not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "LabOrder", "read", 
                order.getBranch(), order.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        return ResponseEntity.ok(ApiResponse.success(order));
    }
    
    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<LabOrderDto>> createLabOrder(
            @RequestBody(required = false) LabOrderDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        // Auto-create request with defaults if null
        if (request == null) {
            request = new LabOrderDto();
        }
        
        // Set default patientId if not provided
        if (request.getPatientId() == null || request.getPatientId().isEmpty()) {
            request.setPatientId("PAT001");
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "LabOrder", "create", 
                user.getBranch(), user.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        String orderId = "LO" + String.format("%03d", mockLabOrders.size() + 1);
        request.setOrderId(orderId);
        request.setOrderedBy(user.getUserId());
        request.setStatus("Ordered");
        request.setBranch(user.getBranch());
        request.setDepartment(user.getDepartment());
        request.setOrderedAt(LocalDateTime.now());
        
        // Set defaults for optional fields
        if (request.getTests() == null || request.getTests().isEmpty()) {
            request.setTests(List.of("Complete Blood Count", "Basic Metabolic Panel"));
        }
        if (request.getPriority() == null) request.setPriority("Routine");
        if (request.getClinicalNotes() == null) request.setClinicalNotes("Routine laboratory tests");
        
        mockLabOrders.put(orderId, request);
        
        log.info("Lab order created by {}: {}", user.getUserId(), orderId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lab order created", request));
    }
    
    // ==================== Lab Results ====================
    
    @GetMapping("/results")
    public ResponseEntity<ApiResponse<List<LabResultDto>>> getAllLabResults(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String orderId) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "LabResult", "read", null, null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<LabResultDto> results = mockLabResults.values().stream()
                .filter(r -> patientId == null || r.getPatientId().equals(patientId))
                .filter(r -> orderId == null || r.getOrderId().equals(orderId))
                .filter(r -> user.getBranch().equals(r.getBranch()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Lab results retrieved", results));
    }
    
    @GetMapping("/results/{resultId}")
    public ResponseEntity<ApiResponse<LabResultDto>> getLabResult(
            @PathVariable String resultId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        LabResultDto result = mockLabResults.get(resultId);
        if (result == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Lab result not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "LabResult", "read", 
                result.getBranch(), result.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        return ResponseEntity.ok(ApiResponse.success(result));
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

