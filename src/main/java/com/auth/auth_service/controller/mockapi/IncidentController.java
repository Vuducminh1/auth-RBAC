package com.auth.auth_service.controller.mockapi;

import com.auth.auth_service.dto.ApiResponse;
import com.auth.auth_service.dto.AuthorizationRequest;
import com.auth.auth_service.dto.AuthorizationResponse;
import com.auth.auth_service.dto.mock.IncidentCaseDto;
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
 * Mock API Controller for Incident Cases
 * Accessible by: SecurityAdmin (create, read, update)
 */
@RestController
@RequestMapping("/api/mock/incidents")
@RequiredArgsConstructor
@Slf4j
public class IncidentController {
    
    private final AuthorizationService authorizationService;
    
    private static final Map<String, IncidentCaseDto> mockIncidents = new HashMap<>();
    
    static {
        mockIncidents.put("INC001", IncidentCaseDto.builder()
                .caseId("INC001")
                .title("Unauthorized Access Attempt Detected")
                .description("Multiple failed login attempts detected from IP 192.168.1.100 for user 'admin'. " +
                        "Account temporarily locked after 5 failed attempts.")
                .severity("Medium")
                .category("Security")
                .status("Resolved")
                .reportedBy("SYSTEM")
                .assignedTo("SECADMIN001")
                .resolution("IP blocked, password reset required for affected account. " +
                        "User notified via email.")
                .affectedSystems(List.of("Authentication Service"))
                .affectedUsers(List.of("admin"))
                .incidentDate(LocalDateTime.now().minusDays(5))
                .reportedAt(LocalDateTime.now().minusDays(5))
                .resolvedAt(LocalDateTime.now().minusDays(4))
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now().minusDays(4))
                .build());
        
        mockIncidents.put("INC002", IncidentCaseDto.builder()
                .caseId("INC002")
                .title("Data Export Without Approval")
                .description("User DOC005 attempted to export medical records without proper approval. " +
                        "Export was blocked by policy POL004.")
                .severity("High")
                .category("Compliance")
                .status("InProgress")
                .reportedBy("SYSTEM")
                .assignedTo("SECADMIN001")
                .resolution(null)
                .affectedSystems(List.of("EMR System", "Export Service"))
                .affectedUsers(List.of("DOC005"))
                .incidentDate(LocalDateTime.now().minusDays(2))
                .reportedAt(LocalDateTime.now().minusDays(2))
                .resolvedAt(null)
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build());
        
        mockIncidents.put("INC003", IncidentCaseDto.builder()
                .caseId("INC003")
                .title("PHI Breach - Unintended Disclosure")
                .description("Patient data was accidentally sent to wrong email recipient. " +
                        "Sender immediately notified IT and Security.")
                .severity("Critical")
                .category("Privacy")
                .status("InProgress")
                .reportedBy("NUR003")
                .assignedTo("SECADMIN001")
                .resolution(null)
                .affectedSystems(List.of("Email System", "Patient Portal"))
                .affectedUsers(List.of("NUR003", "PAT025"))
                .incidentDate(LocalDateTime.now().minusHours(6))
                .reportedAt(LocalDateTime.now().minusHours(5))
                .resolvedAt(null)
                .createdAt(LocalDateTime.now().minusHours(5))
                .updatedAt(LocalDateTime.now().minusHours(1))
                .build());
        
        mockIncidents.put("INC004", IncidentCaseDto.builder()
                .caseId("INC004")
                .title("System Downtime - Database Connection Pool Exhausted")
                .description("Production database experienced connection pool exhaustion " +
                        "causing intermittent service failures for 30 minutes.")
                .severity("High")
                .category("Operational")
                .status("Resolved")
                .reportedBy("ITADMIN001")
                .assignedTo("ITADMIN001")
                .resolution("Connection pool size increased from 50 to 100. " +
                        "Added monitoring alerts for connection usage above 80%.")
                .affectedSystems(List.of("Database Server", "All Services"))
                .affectedUsers(List.of())
                .incidentDate(LocalDateTime.now().minusDays(7))
                .reportedAt(LocalDateTime.now().minusDays(7))
                .resolvedAt(LocalDateTime.now().minusDays(7).plusHours(2))
                .createdAt(LocalDateTime.now().minusDays(7))
                .updatedAt(LocalDateTime.now().minusDays(7).plusHours(2))
                .build());
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<IncidentCaseDto>>> getAllIncidents(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "IncidentCase", "read");
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<IncidentCaseDto> incidents = mockIncidents.values().stream()
                .filter(i -> severity == null || i.getSeverity().equals(severity))
                .filter(i -> category == null || i.getCategory().equals(category))
                .filter(i -> status == null || i.getStatus().equals(status))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Incidents retrieved", incidents));
    }
    
    @GetMapping("/{caseId}")
    public ResponseEntity<ApiResponse<IncidentCaseDto>> getIncident(
            @PathVariable String caseId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        IncidentCaseDto incident = mockIncidents.get(caseId);
        if (incident == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Incident not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "IncidentCase", "read");
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        return ResponseEntity.ok(ApiResponse.success(incident));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<IncidentCaseDto>> createIncident(
            @RequestBody IncidentCaseDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "IncidentCase", "create");
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        String caseId = "INC" + String.format("%03d", mockIncidents.size() + 1);
        request.setCaseId(caseId);
        request.setReportedBy(user.getUserId());
        request.setStatus("Open");
        request.setReportedAt(LocalDateTime.now());
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        
        mockIncidents.put(caseId, request);
        
        log.info("Incident created by {}: {} - {}", user.getUserId(), caseId, request.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Incident created", request));
    }
    
    @PutMapping("/{caseId}")
    public ResponseEntity<ApiResponse<IncidentCaseDto>> updateIncident(
            @PathVariable String caseId,
            @RequestBody IncidentCaseDto request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        IncidentCaseDto existing = mockIncidents.get(caseId);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Incident not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "IncidentCase", "update");
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        if (request.getTitle() != null) existing.setTitle(request.getTitle());
        if (request.getDescription() != null) existing.setDescription(request.getDescription());
        if (request.getSeverity() != null) existing.setSeverity(request.getSeverity());
        if (request.getCategory() != null) existing.setCategory(request.getCategory());
        if (request.getStatus() != null) existing.setStatus(request.getStatus());
        if (request.getAssignedTo() != null) existing.setAssignedTo(request.getAssignedTo());
        if (request.getResolution() != null) existing.setResolution(request.getResolution());
        if (request.getAffectedSystems() != null) existing.setAffectedSystems(request.getAffectedSystems());
        if (request.getAffectedUsers() != null) existing.setAffectedUsers(request.getAffectedUsers());
        
        // Auto-set resolved time if status changed to Resolved
        if ("Resolved".equals(request.getStatus()) && existing.getResolvedAt() == null) {
            existing.setResolvedAt(LocalDateTime.now());
        }
        
        existing.setUpdatedAt(LocalDateTime.now());
        
        log.info("Incident updated by {}: {}", user.getUserId(), caseId);
        return ResponseEntity.ok(ApiResponse.success("Incident updated", existing));
    }
    
    @PostMapping("/{caseId}/assign")
    public ResponseEntity<ApiResponse<IncidentCaseDto>> assignIncident(
            @PathVariable String caseId,
            @RequestParam String assigneeId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        IncidentCaseDto existing = mockIncidents.get(caseId);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Incident not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "IncidentCase", "update");
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        existing.setAssignedTo(assigneeId);
        if ("Open".equals(existing.getStatus())) {
            existing.setStatus("InProgress");
        }
        existing.setUpdatedAt(LocalDateTime.now());
        
        log.info("Incident {} assigned to {} by {}", caseId, assigneeId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Incident assigned", existing));
    }
    
    @PostMapping("/{caseId}/resolve")
    public ResponseEntity<ApiResponse<IncidentCaseDto>> resolveIncident(
            @PathVariable String caseId,
            @RequestParam String resolution,
            @AuthenticationPrincipal UserPrincipal user) {
        
        IncidentCaseDto existing = mockIncidents.get(caseId);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Incident not found"));
        }
        
        AuthorizationResponse authResponse = checkAuthorization(user, "IncidentCase", "update");
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        existing.setResolution(resolution);
        existing.setStatus("Resolved");
        existing.setResolvedAt(LocalDateTime.now());
        existing.setUpdatedAt(LocalDateTime.now());
        
        log.info("Incident {} resolved by {}", caseId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Incident resolved", existing));
    }
    
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getIncidentStats(
            @AuthenticationPrincipal UserPrincipal user) {
        
        AuthorizationResponse authResponse = checkAuthorization(user, "IncidentCase", "read");
        if (!authResponse.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + String.join(", ", authResponse.getDenyReasons())));
        }
        
        List<IncidentCaseDto> allIncidents = new ArrayList<>(mockIncidents.values());
        
        Map<String, Long> bySeverity = new HashMap<>();
        allIncidents.forEach(i -> bySeverity.merge(i.getSeverity(), 1L, Long::sum));
        
        Map<String, Long> byCategory = new HashMap<>();
        allIncidents.forEach(i -> byCategory.merge(i.getCategory(), 1L, Long::sum));
        
        Map<String, Long> byStatus = new HashMap<>();
        allIncidents.forEach(i -> byStatus.merge(i.getStatus(), 1L, Long::sum));
        
        long openIncidents = allIncidents.stream()
                .filter(i -> !"Resolved".equals(i.getStatus()) && !"Closed".equals(i.getStatus()))
                .count();
        
        long criticalOpen = allIncidents.stream()
                .filter(i -> "Critical".equals(i.getSeverity()))
                .filter(i -> !"Resolved".equals(i.getStatus()) && !"Closed".equals(i.getStatus()))
                .count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalIncidents", allIncidents.size());
        stats.put("openIncidents", openIncidents);
        stats.put("criticalOpenIncidents", criticalOpen);
        stats.put("bySeverity", bySeverity);
        stats.put("byCategory", byCategory);
        stats.put("byStatus", byStatus);
        
        return ResponseEntity.ok(ApiResponse.success("Incident statistics", stats));
    }
    
    private AuthorizationResponse checkAuthorization(UserPrincipal user, String resourceType, String action) {
        AuthorizationRequest request = AuthorizationRequest.builder()
                .resourceType(resourceType)
                .action(action)
                .build();
        
        return authorizationService.authorize(request);
    }
}

