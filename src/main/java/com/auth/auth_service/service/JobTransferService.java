package com.auth.auth_service.service;

import com.auth.auth_service.dto.JobTransferRequest;
import com.auth.auth_service.entity.PendingPermissionRequest;
import com.auth.auth_service.entity.Role;
import com.auth.auth_service.entity.User;
import com.auth.auth_service.repository.PendingPermissionRequestRepository;
import com.auth.auth_service.repository.PermissionRepository;
import com.auth.auth_service.repository.RoleRepository;
import com.auth.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service xử lý Job Transfer - chuyển phòng ban/vị trí cho user.
 * Tích hợp với AI service để gợi ý thay đổi quyền.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobTransferService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AIPermissionService aiPermissionService;
    private final PendingPermissionRequestRepository pendingRepo;
    
    /**
     * Khởi tạo quy trình chuyển phòng ban cho user.
     * 1. Lưu old profile
     * 2. Gọi AI để lấy gợi ý thay đổi quyền
     * 3. Cập nhật thông tin user
     * 4. Lưu pending permission requests
     */
    @Transactional
    public Map<String, Object> initiateJobTransfer(String userId, JobTransferRequest request) {
        User user = userRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        // 1. Lưu old profile
        Map<String, String> oldProfile = buildProfile(user);
        String oldDepartment = user.getDepartment();
        String oldBranch = user.getBranch();
        String oldRole = user.getRole().getName();
        
        // 2. Chuẩn bị new profile
        String newRole = request.getNewRole() != null ? request.getNewRole() : user.getRole().getName();
        String newDepartment = request.getNewDepartment();
        String newBranch = request.getNewBranch() != null ? request.getNewBranch() : user.getBranch();
        Boolean hasLicense = request.getHasLicense() != null ? request.getHasLicense() : user.isHasLicense();
        String seniority = request.getSeniority() != null ? request.getSeniority() : user.getSeniority();
        String position = request.getNewPosition() != null ? request.getNewPosition() : user.getPosition();
        
        Map<String, String> newProfile = Map.of(
            "role", newRole,
            "department", newDepartment,
            "branch", newBranch,
            "license", hasLicense ? "Yes" : "No",
            "seniority", seniority,
            "position", position,
            "employment_type", user.getEmploymentType()
        );
        
        // 3. Gọi AI để lấy gợi ý
        Map<String, Object> aiResult = aiPermissionService.recommendJobTransfer(oldProfile, newProfile);
        
        if (aiResult.containsKey("error")) {
            log.warn("AI service error during job transfer: {}", aiResult.get("error"));
            // Vẫn tiếp tục cập nhật user, chỉ không có AI recommendation
        }
        
        // 4. Cập nhật thông tin user
        user.setDepartment(newDepartment);
        if (request.getNewBranch() != null) {
            user.setBranch(request.getNewBranch());
        }
        if (request.getNewPosition() != null) {
            user.setPosition(request.getNewPosition());
        }
        if (request.getHasLicense() != null) {
            user.setHasLicense(request.getHasLicense());
        }
        if (request.getSeniority() != null) {
            user.setSeniority(request.getSeniority());
        }
        
        // Nếu đổi role
        if (request.getNewRole() != null && !request.getNewRole().equals(user.getRole().getName())) {
            Role newRoleEntity = roleRepository.findByName(request.getNewRole())
                .orElseThrow(() -> new RuntimeException("Role not found: " + request.getNewRole()));
            user.setRole(newRoleEntity);
        }
        
        userRepository.save(user);
        
        // 5. Lưu pending permission requests
        int addedCount = 0;
        int removedCount = 0;
        
        if (!aiResult.containsKey("error")) {
            addedCount = savePermissionChanges(user, aiResult, "added_permissions", "ADD");
            removedCount = savePermissionChanges(user, aiResult, "removed_permissions", "REMOVE");
        }
        
        log.info("Job transfer completed for user {}: {} -> {}, {} permissions to add, {} to remove",
            userId, oldDepartment, newDepartment, addedCount, removedCount);
        
        // 6. Trả về kết quả
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "JOB_TRANSFER_INITIATED");
        result.put("userId", userId);
        result.put("username", user.getUsername());
        result.put("changes", Map.of(
            "department", Map.of("from", oldDepartment, "to", newDepartment),
            "branch", Map.of("from", oldBranch, "to", user.getBranch()),
            "role", Map.of("from", oldRole, "to", user.getRole().getName())
        ));
        result.put("pendingPermissions", Map.of(
            "toAdd", addedCount,
            "toRemove", removedCount
        ));
        
        if (!aiResult.containsKey("error")) {
            result.put("aiRecommendation", aiResult);
        }
        
        result.put("message", "Job transfer processed. Permission changes pending admin approval.");
        
        return result;
    }
    
    /**
     * Build user profile cho AI request
     */
    private Map<String, String> buildProfile(User user) {
        return Map.of(
            "role", user.getRole().getName(),
            "department", user.getDepartment(),
            "branch", user.getBranch(),
            "license", user.isHasLicense() ? "Yes" : "No",
            "seniority", user.getSeniority() != null ? user.getSeniority() : "Junior",
            "position", user.getPosition() != null ? user.getPosition() : user.getRole().getName(),
            "employment_type", user.getEmploymentType() != null ? user.getEmploymentType() : "FullTime"
        );
    }
    
    /**
     * Lưu các thay đổi permission vào bảng pending
     */
    @SuppressWarnings("unchecked")
    private int savePermissionChanges(User user, Map<String, Object> aiResult, 
                                       String key, String changeType) {
        Object permissionsObj = aiResult.get(key);
        if (permissionsObj == null) {
            return 0;
        }
        
        List<Map<String, Object>> permissions;
        if (permissionsObj instanceof List) {
            permissions = (List<Map<String, Object>>) permissionsObj;
        } else {
            return 0;
        }
        
        if (permissions.isEmpty()) {
            return 0;
        }
        
        int count = 0;
        for (Map<String, Object> perm : permissions) {
            String permissionLabel = (String) perm.get("permission"); // e.g., "ClinicalNote_create"
            Object confidenceObj = perm.get("confidence");
            double confidence = confidenceObj instanceof Number ? ((Number) confidenceObj).doubleValue() : 0.6;
            
            // Parse permission label: "ResourceType_action"
            String[] parts = permissionLabel.split("_");
            if (parts.length >= 2) {
                String resourceType = parts[0];
                String action = parts[1];
                
                // Tìm permission trong database
                permissionRepository.findFirstByResourceTypeAndAction(resourceType, action)
                    .ifPresent(permission -> {
                        // Kiểm tra chưa có pending request
                        boolean alreadyPending = pendingRepo
                            .existsByUserIdAndPermissionIdAndStatusAndChangeType(
                                user.getId(), permission.getId(), "PENDING", changeType);
                        
                        if (!alreadyPending) {
                            PendingPermissionRequest pending = PendingPermissionRequest.builder()
                                .user(user)
                                .permission(permission)
                                .confidence(BigDecimal.valueOf(confidence))
                                .requestType("JOB_TRANSFER")
                                .changeType(changeType)
                                .status("PENDING")
                                .build();
                            
                            pendingRepo.save(pending);
                            log.debug("Created pending {} request: {} -> {} (confidence: {})",
                                changeType, user.getUsername(), permissionLabel, confidence);
                        }
                    });
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Lấy danh sách pending changes cho một user
     */
    public Map<String, Object> getPendingChanges(String userId) {
        User user = userRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        List<PendingPermissionRequest> pending = pendingRepo.findByUserIdAndStatus(user.getId(), "PENDING");
        
        List<Map<String, Object>> toAdd = new ArrayList<>();
        List<Map<String, Object>> toRemove = new ArrayList<>();
        
        for (PendingPermissionRequest req : pending) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", req.getId());
            item.put("permissionKey", req.getPermission().getPermissionKey());
            item.put("resourceType", req.getPermission().getResourceType());
            item.put("action", req.getPermission().getAction());
            item.put("confidence", req.getConfidence());
            item.put("requestType", req.getRequestType());
            item.put("requestedAt", req.getRequestedAt());
            
            if ("ADD".equals(req.getChangeType())) {
                toAdd.add(item);
            } else if ("REMOVE".equals(req.getChangeType())) {
                toRemove.add(item);
            }
        }
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("username", user.getUsername());
        result.put("toAdd", toAdd);
        result.put("toRemove", toRemove);
        result.put("totalPending", pending.size());
        
        return result;
    }
}
