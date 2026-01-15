package com.auth.auth_service.controller;

import com.auth.auth_service.aop.Audit;
import com.auth.auth_service.dto.ApiResponse;
import com.auth.auth_service.dto.JobTransferRequest;
import com.auth.auth_service.dto.UserDto;
import com.auth.auth_service.entity.User;
import com.auth.auth_service.repository.PendingPermissionRequestRepository;
import com.auth.auth_service.repository.UserRepository;
import com.auth.auth_service.service.JobTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserRepository userRepository;
    private final JobTransferService jobTransferService;
    private final PendingPermissionRequestRepository pendingRepo;
    
    @Audit(resourceType = "User", action = "list", useFirstArgAsResourceId = false)
    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'Manager', 'SecurityAdmin')")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        List<UserDto> users = userRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(users));
    }
    
    /**
     * API chi tiết cho Admin - Lấy tất cả users với thông tin đầy đủ
     * Bao gồm: pending permissions count, additional permissions count, statistics
     * 
     * GET /api/users/admin/all
     */
    @Audit(resourceType = "User", action = "adminList", useFirstArgAsResourceId = false)
    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyRole('HR', 'Manager', 'SecurityAdmin', 'ITAdmin')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllUsersForAdmin(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) Boolean enabled) {
        
        List<User> users = userRepository.findAll();
        
        // Apply filters
        if (role != null && !role.isEmpty()) {
            users = users.stream()
                .filter(u -> u.getRole().getName().equalsIgnoreCase(role))
                .collect(Collectors.toList());
        }
        if (department != null && !department.isEmpty()) {
            users = users.stream()
                .filter(u -> u.getDepartment().equalsIgnoreCase(department))
                .collect(Collectors.toList());
        }
        if (branch != null && !branch.isEmpty()) {
            users = users.stream()
                .filter(u -> u.getBranch().equalsIgnoreCase(branch))
                .collect(Collectors.toList());
        }
        if (enabled != null) {
            users = users.stream()
                .filter(u -> u.isEnabled() == enabled)
                .collect(Collectors.toList());
        }
        
        // Map to detailed DTO
        List<Map<String, Object>> userList = users.stream()
            .map(this::mapToAdminDto)
            .collect(Collectors.toList());
        
        // Statistics
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", userList.size());
        stats.put("activeUsers", users.stream().filter(User::isEnabled).count());
        stats.put("inactiveUsers", users.stream().filter(u -> !u.isEnabled()).count());
        stats.put("totalPendingPermissions", pendingRepo.countByStatus("PENDING"));
        
        // Group by role
        Map<String, Long> byRole = users.stream()
            .collect(Collectors.groupingBy(u -> u.getRole().getName(), Collectors.counting()));
        stats.put("byRole", byRole);
        
        // Group by department
        Map<String, Long> byDepartment = users.stream()
            .collect(Collectors.groupingBy(User::getDepartment, Collectors.counting()));
        stats.put("byDepartment", byDepartment);
        
        // Group by branch
        Map<String, Long> byBranch = users.stream()
            .collect(Collectors.groupingBy(User::getBranch, Collectors.counting()));
        stats.put("byBranch", byBranch);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("users", userList);
        result.put("statistics", stats);
        
        return ResponseEntity.ok(ApiResponse.success("All users for admin", result));
    }
    
    /**
     * Lấy thông tin chi tiết user cho admin
     * GET /api/users/admin/{userId}
     */
    @Audit(resourceType = "User", action = "adminGetDetail")
    @GetMapping("/admin/{userId}")
    @PreAuthorize("hasAnyRole('HR', 'Manager', 'SecurityAdmin', 'ITAdmin')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserDetailForAdmin(@PathVariable String userId) {
        User user = userRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        Map<String, Object> detail = mapToAdminDto(user);
        
        // Thêm pending permissions chi tiết
        List<Map<String, Object>> pendingList = pendingRepo.findByUserIdAndStatus(user.getId(), "PENDING")
            .stream()
            .map(p -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", p.getId());
                item.put("permissionKey", p.getPermission().getPermissionKey());
                item.put("resourceType", p.getPermission().getResourceType());
                item.put("action", p.getPermission().getAction());
                item.put("confidence", p.getConfidence());
                item.put("changeType", p.getChangeType());
                item.put("requestType", p.getRequestType());
                item.put("requestedAt", p.getRequestedAt());
                return item;
            })
            .collect(Collectors.toList());
        
        detail.put("pendingPermissions", pendingList);
        
        return ResponseEntity.ok(ApiResponse.success(detail));
    }
    
    /**
     * Map User to Admin DTO với thông tin đầy đủ
     */
    private Map<String, Object> mapToAdminDto(User user) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", user.getId());
        dto.put("userId", user.getUserId());
        dto.put("username", user.getUsername());
        dto.put("email", user.getEmail());
        dto.put("role", user.getRole().getName());
        dto.put("department", user.getDepartment());
        dto.put("branch", user.getBranch());
        dto.put("position", user.getPosition());
        dto.put("hasLicense", user.isHasLicense());
        dto.put("seniority", user.getSeniority());
        dto.put("employmentType", user.getEmploymentType());
        dto.put("enabled", user.isEnabled());
        dto.put("accountNonLocked", user.isAccountNonLocked());
        dto.put("assignedPatients", user.getAssignedPatients());
        dto.put("assignedPatientsCount", user.getAssignedPatients() != null ? user.getAssignedPatients().size() : 0);
        
        // Role permissions count
        dto.put("rolePermissionsCount", user.getRole().getPermissions().size());
        
        // Additional permissions count
        dto.put("additionalPermissionsCount", user.getAdditionalPermissions().size());
        
        // Pending permissions count
        long pendingCount = pendingRepo.findByUserIdAndStatus(user.getId(), "PENDING").size();
        dto.put("pendingPermissionsCount", pendingCount);
        
        // Total effective permissions
        dto.put("totalEffectivePermissions", 
            user.getRole().getPermissions().size() + user.getAdditionalPermissions().size());
        
        // Permissions map
        dto.put("permissions", groupPermissionsByResource(user));
        
        return dto;
    }
    
    @Audit(resourceType = "User", action = "get")
    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('HR', 'Manager', 'SecurityAdmin')")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return ResponseEntity.ok(ApiResponse.success(mapToDto(user)));
    }
    
    @Audit(resourceType = "User", action = "listByDepartment")
    @GetMapping("/department/{department}")
    @PreAuthorize("hasAnyRole('HR', 'Manager')")
    public ResponseEntity<ApiResponse<List<UserDto>>> getUsersByDepartment(@PathVariable String department) {
        List<UserDto> users = userRepository.findByDepartment(department).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(users));
    }
    
    @Audit(resourceType = "User", action = "listByBranch")
    @GetMapping("/branch/{branch}")
    @PreAuthorize("hasAnyRole('HR', 'Manager')")
    public ResponseEntity<ApiResponse<List<UserDto>>> getUsersByBranch(@PathVariable String branch) {
        List<UserDto> users = userRepository.findByBranch(branch).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(users));
    }
    
    @Audit(resourceType = "User", action = "listByRole")
    @GetMapping("/role/{roleName}")
    @PreAuthorize("hasAnyRole('HR', 'Manager', 'SecurityAdmin')")
    public ResponseEntity<ApiResponse<List<UserDto>>> getUsersByRole(@PathVariable String roleName) {
        List<UserDto> users = userRepository.findByRoleName(roleName).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(users));
    }
    
    /**
     * Chuyển phòng ban/vị trí cho user (Job Transfer)
     * AI sẽ gợi ý quyền cần thêm/thu hồi dựa trên vị trí mới
     * 
     * PUT /api/users/{userId}/transfer
     * Body: {
     *   "newDepartment": "Human Resources",
     *   "newBranch": "BRANCH_HCM",
     *   "newRole": "HR",
     *   "newPosition": "HR Staff",
     *   "reason": "Chuyển công tác theo yêu cầu"
     * }
     */
    @Audit(resourceType = "User", action = "transfer")
    @PutMapping("/{userId}/transfer")
    @PreAuthorize("hasAnyRole('HR', 'Manager', 'SecurityAdmin')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> transferUser(
            @PathVariable String userId,
            @Valid @RequestBody JobTransferRequest request) {
        
        log.info("Job transfer requested for user {} to department {}", 
            userId, request.getNewDepartment());
        
        try {
            Map<String, Object> result = jobTransferService.initiateJobTransfer(userId, request);
            return ResponseEntity.ok(ApiResponse.success("Job transfer initiated", result));
        } catch (RuntimeException e) {
            log.error("Job transfer failed for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Lấy danh sách pending permission changes cho một user
     * GET /api/users/{userId}/pending-permissions
     */
    @Audit(resourceType = "User", action = "getPendingChanges")
    @GetMapping("/{userId}/pending-permissions")
    @PreAuthorize("hasAnyRole('HR', 'Manager', 'SecurityAdmin')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPendingPermissions(
            @PathVariable String userId) {
        
        try {
            Map<String, Object> result = jobTransferService.getPendingChanges(userId);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .department(user.getDepartment())
                .branch(user.getBranch())
                .position(user.getPosition())
                .hasLicense(user.isHasLicense())
                .seniority(user.getSeniority())
                .employmentType(user.getEmploymentType())
                .enabled(user.isEnabled())
                .assignedPatients(user.getAssignedPatients())
                .permissions(groupPermissionsByResource(user))
                .build();
    }
    
    /**
     * Group permissions by resource type
     * Example output: {"AdmissionRecord": "read", "ClinicalNote": "create,read"}
     */
    private Map<String, String> groupPermissionsByResource(User user) {
        Set<com.auth.auth_service.entity.Permission> allPermissions = new HashSet<>(user.getRole().getPermissions());
        allPermissions.addAll(user.getAdditionalPermissions());
        
        Map<String, Set<String>> resourceActionsMap = new LinkedHashMap<>();
        
        for (com.auth.auth_service.entity.Permission permission : allPermissions) {
            String resourceType = permission.getResourceType();
            String action = permission.getAction();
            resourceActionsMap.computeIfAbsent(resourceType, k -> new TreeSet<>()).add(action);
        }
        
        Map<String, String> result = new LinkedHashMap<>();
        resourceActionsMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> result.put(entry.getKey(), String.join(",", entry.getValue())));
        
        return result;
    }
}
