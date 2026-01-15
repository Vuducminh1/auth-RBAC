package com.auth.auth_service.controller.mockapi;

import com.auth.auth_service.aop.Audit;
import com.auth.auth_service.dto.ApiResponse;
import com.auth.auth_service.dto.AuthorizationRequest;
import com.auth.auth_service.dto.AuthorizationResponse;
import com.auth.auth_service.dto.mock.ImagingOrderDto;
import com.auth.auth_service.dto.mock.ImagingResultDto;
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
 * Mock API Controller for Imaging Orders and Results
 * ImagingOrder: Doctor (create, read)
 * ImagingResult: Doctor (read), Nurse (read)
 */
@RestController
@RequestMapping("/api/mock/imaging")
@RequiredArgsConstructor
@Slf4j
public class ImagingController {
    
    private final AuthorizationService authorizationService;
    
    private static final Map<String, ImagingOrderDto> mockImagingOrders = new HashMap<>();
    private static final Map<String, ImagingResultDto> mockImagingResults = new HashMap<>();
    
    static {
        // Imaging Orders
        mockImagingOrders.put("IO001", ImagingOrderDto.builder()
                .orderId("IO001")
                .patientId("PAT001")
                .orderedBy("DOC001")
                .modality("X-Ray")
                .bodyPart("Chest")
                .status("Completed")
                .priority("Routine")
                .clinicalIndication("Rule out pneumonia")
                .branch("BRANCH_HCM")
                .department("Internal Medicine")
                .orderedAt(LocalDateTime.now().minusDays(5))
                .scheduledAt(LocalDateTime.now().minusDays(4))
                .build());
        
        mockImagingOrders.put("IO002", ImagingOrderDto.builder()
                .orderId("IO002")
                .patientId("PAT002")
                .orderedBy("DOC002")
                .modality("CT")
                .bodyPart("Coronary")
                .status("Completed")
                .priority("Urgent")
                .clinicalIndication("Post-PCI assessment")
                .branch("BRANCH_HCM")
                .department("Cardiology")
                .orderedAt(LocalDateTime.now().minusDays(1))
                .scheduledAt(LocalDateTime.now().minusHours(12))
                .build());
        
        mockImagingOrders.put("IO003", ImagingOrderDto.builder()
                .orderId("IO003")
                .patientId("PAT003")
                .orderedBy("DOC003")
                .modality("MRI")
                .bodyPart("Lumbar Spine")
                .status("Scheduled")
                .priority("Routine")
                .clinicalIndication("Chronic low back pain")
                .branch("BRANCH_HN")
                .department("Orthopedics")
                .orderedAt(LocalDateTime.now())
                .scheduledAt(LocalDateTime.now().plusDays(2))
                .build());
        
        // Imaging Results
        mockImagingResults.put("IR001", ImagingResultDto.builder()
                .resultId("IR001")
                .orderId("IO001")
                .patientId("PAT001")
                .radiologistId("RAD001")
                .modality("X-Ray")
                .bodyPart("Chest")
                .status("Final")
                .findings("Clear lung fields. No infiltrates or consolidation. " +
                        "Cardiac silhouette within normal limits. No pleural effusion.")
                .impression("Normal chest X-ray")
                .recommendations("No follow-up imaging required")
                .imageUrls(List.of("/images/IO001/PA.jpg", "/images/IO001/Lateral.jpg"))
                .branch("BRANCH_HCM")
                .department("Internal Medicine")
                .performedAt(LocalDateTime.now().minusDays(4))
                .reportedAt(LocalDateTime.now().minusDays(4))
                .build());
        
        mockImagingResults.put("IR002", ImagingResultDto.builder()
                .resultId("IR002")
                .orderId("IO002")
                .patientId("PAT002")
                .radiologistId("RAD002")
                .modality("CT")
                .bodyPart("Coronary")
                .status("Final")
                .findings("Patent stents in LAD and LCx. No in-stent restenosis. " +
                        "Mild non-obstructive plaque in RCA.")
                .impression("Satisfactory post-PCI appearance. Patent stents.")
                .recommendations("Follow-up in 6 months")
                .imageUrls(List.of("/images/IO002/series1.dcm", "/images/IO002/series2.dcm"))
                .branch("BRANCH_HCM")
                .department("Cardiology")
                .performedAt(LocalDateTime.now().minusHours(10))
                .reportedAt(LocalDateTime.now().minusHours(8))
                .build());
    }
    
    // ==================== Imaging Orders ====================
    
    @Audit(resourceType = "ImagingOrder", action = "read")
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<ImagingOrderDto>>> getAllImagingOrders(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String modality) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "ImagingOrder", "read", null, null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<ImagingOrderDto> orders = mockImagingOrders.values().stream()
                .filter(o -> patientId == null || o.getPatientId().equals(patientId))
                .filter(o -> status == null || o.getStatus().equals(status))
                .filter(o -> modality == null || o.getModality().equals(modality))
                .filter(o -> user.getBranch().equals(o.getBranch()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Imaging orders retrieved", orders));
    }
    
    @Audit(resourceType = "ImagingOrder", action = "read")
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<ImagingOrderDto>> getImagingOrder(
            @PathVariable String orderId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        ImagingOrderDto order = mockImagingOrders.get(orderId);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Imaging order not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "ImagingOrder", "read", 
                order.getBranch(), order.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        return ResponseEntity.ok(ApiResponse.success(order));
    }
    
    @Audit(resourceType = "ImagingOrder", action = "create")
    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<ImagingOrderDto>> createImagingOrder(
            @RequestBody(required = false) ImagingOrderDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        // Auto-create request with defaults if null
        if (request == null) {
            request = new ImagingOrderDto();
        }
        
        // Set default patientId if not provided
        if (request.getPatientId() == null || request.getPatientId().isEmpty()) {
            request.setPatientId("PAT001");
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "ImagingOrder", "create", 
                user.getBranch(), user.getDepartment());
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        String orderId = "IO" + String.format("%03d", mockImagingOrders.size() + 1);
        request.setOrderId(orderId);
        request.setOrderedBy(user.getUserId());
        request.setStatus("Ordered");
        request.setBranch(user.getBranch());
        request.setDepartment(user.getDepartment());
        request.setOrderedAt(LocalDateTime.now());
        
        // Set defaults for optional fields
        if (request.getModality() == null) request.setModality("X-Ray");
        if (request.getBodyPart() == null) request.setBodyPart("Chest");
        if (request.getPriority() == null) request.setPriority("Routine");
        if (request.getClinicalIndication() == null) request.setClinicalIndication("Routine imaging study");
        
        mockImagingOrders.put(orderId, request);
        
        log.info("Imaging order created by {}: {}", user.getUserId(), orderId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Imaging order created", request));
    }
    
    // ==================== Imaging Results ====================
    
    @Audit(resourceType = "ImagingResult", action = "read")
    @GetMapping("/results")
    public ResponseEntity<ApiResponse<List<ImagingResultDto>>> getAllImagingResults(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String orderId) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "ImagingResult", "read", null, null);
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<ImagingResultDto> results = mockImagingResults.values().stream()
                .filter(r -> patientId == null || r.getPatientId().equals(patientId))
                .filter(r -> orderId == null || r.getOrderId().equals(orderId))
                .filter(r -> user.getBranch().equals(r.getBranch()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Imaging results retrieved", results));
    }
    
    @Audit(resourceType = "ImagingResult", action = "read")
    @GetMapping("/results/{resultId}")
    public ResponseEntity<ApiResponse<ImagingResultDto>> getImagingResult(
            @PathVariable String resultId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        ImagingResultDto result = mockImagingResults.get(resultId);
        if (result == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Imaging result not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "ImagingResult", "read", 
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

