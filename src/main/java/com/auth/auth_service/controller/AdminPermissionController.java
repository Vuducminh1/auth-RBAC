package com.auth.auth_service.controller;

import com.auth.auth_service.dto.ApiResponse;
import com.auth.auth_service.entity.PendingPermissionRequest;
import com.auth.auth_service.entity.User;
import com.auth.auth_service.repository.PendingPermissionRequestRepository;
import com.auth.auth_service.repository.UserRepository;
import com.auth.auth_service.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Controller cho Admin quản lý pending permission requests.
 * Cho phép HR, Manager, SecurityAdmin duyệt/từ chối các gợi ý từ AI.
 */
@RestController
@RequestMapping("/api/admin/permissions")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('SecurityAdmin', 'Manager', 'HR')")
public class AdminPermissionController {
    
    private final PendingPermissionRequestRepository pendingRepo;
    private final UserRepository userRepository;
    
    /**
     * Lấy tất cả pending requests
     * GET /api/admin/permissions/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPendingRequests() {
        List<PendingPermissionRequest> pendingList = pendingRepo.findAllPending();
        
        List<Map<String, Object>> result = pendingList.stream()
            .map(this::mapToDto)
            .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Pending permission requests", result));
    }
    
    /**
     * Lấy pending requests theo user
     * GET /api/admin/permissions/pending/user/{userId}
     */
    @GetMapping("/pending/user/{userId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPendingByUser(@PathVariable Long userId) {
        List<PendingPermissionRequest> pendingList = pendingRepo.findByUserIdAndStatus(userId, "PENDING");
        
        List<Map<String, Object>> result = pendingList.stream()
            .map(this::mapToDto)
            .toList();
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    /**
     * Lấy pending requests theo loại (NEW_USER, JOB_TRANSFER)
     * GET /api/admin/permissions/pending/type/{type}
     */
    @GetMapping("/pending/type/{type}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPendingByType(@PathVariable String type) {
        List<PendingPermissionRequest> pendingList = pendingRepo.findByStatusAndRequestType("PENDING", type);
        
        List<Map<String, Object>> result = pendingList.stream()
            .map(this::mapToDto)
            .toList();
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    /**
     * Thống kê pending requests
     * GET /api/admin/permissions/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        long pendingCount = pendingRepo.countByStatus("PENDING");
        long approvedCount = pendingRepo.countByStatus("APPROVED");
        long rejectedCount = pendingRepo.countByStatus("REJECTED");
        
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("pending", pendingCount);
        stats.put("approved", approvedCount);
        stats.put("rejected", rejectedCount);
        stats.put("total", pendingCount + approvedCount + rejectedCount);
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    /**
     * Phê duyệt yêu cầu quyền
     * - ADD: thêm vào user_additional_permissions
     * - REMOVE: xóa khỏi user_additional_permissions
     * 
     * POST /api/admin/permissions/approve/{requestId}
     */
    @PostMapping("/approve/{requestId}")
    @Transactional
    public ResponseEntity<ApiResponse<String>> approveRequest(
            @PathVariable Long requestId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal admin) {
        
        PendingPermissionRequest request = pendingRepo.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Request not found: " + requestId));
        
        if (!"PENDING".equals(request.getStatus())) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Request already processed: " + request.getStatus()));
        }
        
        // Lấy admin user
        User adminUser = userRepository.findByUsername(admin.getUsername())
            .orElseThrow(() -> new RuntimeException("Admin user not found"));
        
        User targetUser = request.getUser();
        String changeType = request.getChangeType();
        String action;
        
        if ("REMOVE".equals(changeType)) {
            // Thu hồi quyền - xóa khỏi additional permissions
            targetUser.getAdditionalPermissions().remove(request.getPermission());
            action = "revoked from";
        } else {
            // Thêm quyền - mặc định ADD
            targetUser.getAdditionalPermissions().add(request.getPermission());
            action = "added to";
        }
        userRepository.save(targetUser);
        
        // Cập nhật trạng thái request
        request.setStatus("APPROVED");
        request.setReviewedBy(adminUser);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewNotes(body != null ? body.get("notes") : null);
        pendingRepo.save(request);
        
        log.info("Permission {} {} user {} by admin {} (changeType: {})",
            request.getPermission().getPermissionKey(),
            action,
            targetUser.getUsername(),
            admin.getUsername(),
            changeType);
        
        return ResponseEntity.ok(ApiResponse.success(
            "Permission " + action + " user: " + request.getPermission().getPermissionKey()));
    }
    
    /**
     * Từ chối yêu cầu quyền
     * POST /api/admin/permissions/reject/{requestId}
     */
    @PostMapping("/reject/{requestId}")
    @Transactional
    public ResponseEntity<ApiResponse<String>> rejectRequest(
            @PathVariable Long requestId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal admin) {
        
        PendingPermissionRequest request = pendingRepo.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Request not found: " + requestId));
        
        if (!"PENDING".equals(request.getStatus())) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Request already processed: " + request.getStatus()));
        }
        
        // Lấy admin user
        User adminUser = userRepository.findByUsername(admin.getUsername())
            .orElseThrow(() -> new RuntimeException("Admin user not found"));
        
        // Cập nhật trạng thái request
        request.setStatus("REJECTED");
        request.setReviewedBy(adminUser);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewNotes(body != null ? body.get("notes") : null);
        pendingRepo.save(request);
        
        log.info("Permission {} rejected for user {} by admin {}",
            request.getPermission().getPermissionKey(),
            request.getUser().getUsername(),
            admin.getUsername());
        
        return ResponseEntity.ok(ApiResponse.success(
            "Permission rejected: " + request.getPermission().getPermissionKey()));
    }
    
    /**
     * Phê duyệt nhiều request cùng lúc
     * POST /api/admin/permissions/approve-bulk
     * Body: { "requestIds": [1, 2, 3], "notes": "Bulk approval" }
     */
    @PostMapping("/approve-bulk")
    @Transactional
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approveBulk(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal admin) {
        
        List<Number> requestIds = (List<Number>) body.get("requestIds");
        String notes = (String) body.get("notes");
        
        if (requestIds == null || requestIds.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("requestIds is required"));
        }
        
        User adminUser = userRepository.findByUsername(admin.getUsername())
            .orElseThrow(() -> new RuntimeException("Admin user not found"));
        
        int approved = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        
        for (Number id : requestIds) {
            try {
                PendingPermissionRequest request = pendingRepo.findById(id.longValue())
                    .orElseThrow(() -> new RuntimeException("Request not found: " + id));
                
                if (!"PENDING".equals(request.getStatus())) {
                    failed++;
                    errors.add("Request " + id + " already processed");
                    continue;
                }
                
                User targetUser = request.getUser();
                if ("REMOVE".equals(request.getChangeType())) {
                    targetUser.getAdditionalPermissions().remove(request.getPermission());
                } else {
                    targetUser.getAdditionalPermissions().add(request.getPermission());
                }
                userRepository.save(targetUser);
                
                request.setStatus("APPROVED");
                request.setReviewedBy(adminUser);
                request.setReviewedAt(LocalDateTime.now());
                request.setReviewNotes(notes);
                pendingRepo.save(request);
                
                approved++;
            } catch (Exception e) {
                failed++;
                errors.add("Request " + id + ": " + e.getMessage());
            }
        }
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("approved", approved);
        result.put("failed", failed);
        result.put("errors", errors);
        
        log.info("Bulk approval by {}: {} approved, {} failed", admin.getUsername(), approved, failed);
        
        return ResponseEntity.ok(ApiResponse.success("Bulk approval completed", result));
    }
    
    /**
     * Từ chối nhiều request cùng lúc
     * POST /api/admin/permissions/reject-bulk
     */
    @PostMapping("/reject-bulk")
    @Transactional
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rejectBulk(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal admin) {
        
        List<Number> requestIds = (List<Number>) body.get("requestIds");
        String notes = (String) body.get("notes");
        
        if (requestIds == null || requestIds.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("requestIds is required"));
        }
        
        User adminUser = userRepository.findByUsername(admin.getUsername())
            .orElseThrow(() -> new RuntimeException("Admin user not found"));
        
        int rejected = 0;
        int failed = 0;
        
        for (Number id : requestIds) {
            try {
                PendingPermissionRequest request = pendingRepo.findById(id.longValue())
                    .orElseThrow(() -> new RuntimeException("Request not found: " + id));
                
                if (!"PENDING".equals(request.getStatus())) {
                    failed++;
                    continue;
                }
                
                request.setStatus("REJECTED");
                request.setReviewedBy(adminUser);
                request.setReviewedAt(LocalDateTime.now());
                request.setReviewNotes(notes);
                pendingRepo.save(request);
                
                rejected++;
            } catch (Exception e) {
                failed++;
            }
        }
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rejected", rejected);
        result.put("failed", failed);
        
        log.info("Bulk rejection by {}: {} rejected, {} failed", admin.getUsername(), rejected, failed);
        
        return ResponseEntity.ok(ApiResponse.success("Bulk rejection completed", result));
    }
    
    /**
     * Phê duyệt tất cả pending của một user
     * POST /api/admin/permissions/approve-all-for-user/{userId}
     */
    @PostMapping("/approve-all-for-user/{userId}")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> approveAllForUser(
            @PathVariable Long userId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal admin) {
        
        User adminUser = userRepository.findByUsername(admin.getUsername())
            .orElseThrow(() -> new RuntimeException("Admin user not found"));
        
        List<PendingPermissionRequest> pendingList = pendingRepo.findByUserIdAndStatus(userId, "PENDING");
        
        if (pendingList.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No pending requests for user", Map.of("approved", 0)));
        }
        
        User targetUser = pendingList.get(0).getUser();
        String notes = body != null ? body.get("notes") : "Bulk approval for user";
        
        int approved = 0;
        for (PendingPermissionRequest request : pendingList) {
            if ("REMOVE".equals(request.getChangeType())) {
                targetUser.getAdditionalPermissions().remove(request.getPermission());
            } else {
                targetUser.getAdditionalPermissions().add(request.getPermission());
            }
            
            request.setStatus("APPROVED");
            request.setReviewedBy(adminUser);
            request.setReviewedAt(LocalDateTime.now());
            request.setReviewNotes(notes);
            pendingRepo.save(request);
            approved++;
        }
        
        userRepository.save(targetUser);
        
        log.info("All pending permissions approved for user {} by admin {}: {} permissions",
            targetUser.getUsername(), admin.getUsername(), approved);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("username", targetUser.getUsername());
        result.put("approved", approved);
        
        return ResponseEntity.ok(ApiResponse.success("All permissions approved for user", result));
    }
    
    /**
     * Map PendingPermissionRequest to DTO
     */
    private Map<String, Object> mapToDto(PendingPermissionRequest request) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", request.getId());
        dto.put("userId", request.getUser().getUserId());
        dto.put("userDbId", request.getUser().getId());
        dto.put("username", request.getUser().getUsername());
        dto.put("userRole", request.getUser().getRole().getName());
        dto.put("department", request.getUser().getDepartment());
        dto.put("branch", request.getUser().getBranch());
        dto.put("permissionId", request.getPermission().getId());
        dto.put("permissionKey", request.getPermission().getPermissionKey());
        dto.put("resourceType", request.getPermission().getResourceType());
        dto.put("action", request.getPermission().getAction());
        dto.put("confidence", request.getConfidence());
        dto.put("requestType", request.getRequestType());
        dto.put("changeType", request.getChangeType());
        dto.put("status", request.getStatus());
        dto.put("requestedAt", request.getRequestedAt());
        return dto;
    }
}
