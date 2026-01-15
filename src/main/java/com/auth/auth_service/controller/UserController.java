package com.auth.auth_service.controller;

import com.auth.auth_service.aop.Audit;
import com.auth.auth_service.dto.ApiResponse;
import com.auth.auth_service.dto.UserDto;
import com.auth.auth_service.entity.User;
import com.auth.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserRepository userRepository;
    
    @Audit(resourceType = "User", action = "list", useFirstArgAsResourceId = false)
    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'Manager', 'SecurityAdmin')")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        List<UserDto> users = userRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(users));
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
