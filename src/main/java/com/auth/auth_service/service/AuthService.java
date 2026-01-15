package com.auth.auth_service.service;

import com.auth.auth_service.dto.LoginRequest;
import com.auth.auth_service.dto.LoginResponse;
import com.auth.auth_service.dto.RegisterRequest;
import com.auth.auth_service.dto.UserDto;
import com.auth.auth_service.entity.PendingPermissionRequest;
import com.auth.auth_service.entity.Role;
import com.auth.auth_service.entity.User;
import com.auth.auth_service.repository.PendingPermissionRequestRepository;
import com.auth.auth_service.repository.PermissionRepository;
import com.auth.auth_service.repository.RoleRepository;
import com.auth.auth_service.repository.UserRepository;
import com.auth.auth_service.security.JwtTokenProvider;
import com.auth.auth_service.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AIPermissionService aiPermissionService;
    private final PermissionRepository permissionRepository;
    private final PendingPermissionRequestRepository pendingRepo;
    
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String jwt = tokenProvider.generateToken(authentication);
        
        // Get user for full permissions mapping
        User user = userRepository.findByUsername(userPrincipal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Map<String, String> permissionsMap = groupPermissionsByResource(user);
        
        log.info("User {} logged in successfully", userPrincipal.getUsername());
        
        return LoginResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getJwtExpiration())
                .userId(userPrincipal.getUserId())
                .username(userPrincipal.getUsername())
                .role(userPrincipal.getRole())
                .department(userPrincipal.getDepartment())
                .branch(userPrincipal.getBranch())
                .permissions(permissionsMap)
                .build();
    }
    
    @Transactional(readOnly = true)
    public UserDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        User user = userRepository.findByUsername(userPrincipal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return mapToUserDto(user);
    }
    
    @Transactional
    public UserDto register(RegisterRequest registerRequest) {
        // Validate username không tồn tại
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("Username already exists: " + registerRequest.getUsername());
        }
        
        // Validate email không tồn tại (nếu có)
        if (registerRequest.getEmail() != null && !registerRequest.getEmail().isEmpty() 
                && userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email already exists: " + registerRequest.getEmail());
        }
        
        // Tìm role
        Role role = roleRepository.findByName(registerRequest.getRole())
                .orElseThrow(() -> new RuntimeException("Role not found: " + registerRequest.getRole()));
        
        // Tạo userId unique
        String userId = generateUserId(registerRequest.getRole());
        
        // Tạo user mới
        String position = registerRequest.getPosition() != null ? registerRequest.getPosition() : "Staff";
        String seniority = registerRequest.getSeniority() != null ? registerRequest.getSeniority() : "Junior";
        String employmentType = registerRequest.getEmploymentType() != null ? registerRequest.getEmploymentType() : "FullTime";
        
        User user = User.builder()
                .userId(userId)
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .email(registerRequest.getEmail())
                .department(registerRequest.getDepartment())
                .branch(registerRequest.getBranch())
                .position(position)
                .hasLicense(registerRequest.isHasLicense())
                .seniority(seniority)
                .employmentType(employmentType)
                .role(role)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
        
        User savedUser = userRepository.save(user);
        
        // Gọi AI để gợi ý quyền bổ sung
        try {
            Map<String, Object> aiRecommendation = aiPermissionService.recommendNewUser(
                registerRequest.getRole(),
                registerRequest.getDepartment(),
                registerRequest.getBranch(),
                registerRequest.isHasLicense() ? "Yes" : "No",
                seniority,
                position,
                employmentType
            );
            
            if (aiRecommendation != null && !aiRecommendation.containsKey("error")) {
                int pendingCount = saveAIRecommendationsAsPending(savedUser, aiRecommendation);
                log.info("User registered with {} AI-recommended permissions pending approval", pendingCount);
            }
        } catch (Exception e) {
            log.warn("Failed to get AI recommendations for user {}: {}", savedUser.getUsername(), e.getMessage());
            // Không throw exception - user vẫn được tạo thành công
        }
        
        log.info("User registered successfully: {} with role {}", savedUser.getUsername(), role.getName());
        
        return mapToUserDto(savedUser);
    }
    
    /**
     * Lưu các gợi ý từ AI vào bảng pending_permission_requests
     */
    @SuppressWarnings("unchecked")
    private int saveAIRecommendationsAsPending(User user, Map<String, Object> aiRecommendation) {
        Object recommendationsObj = aiRecommendation.get("recommendations");
        if (recommendationsObj == null || !(recommendationsObj instanceof List)) {
            return 0;
        }
        
        List<Map<String, Object>> recommendations = (List<Map<String, Object>>) recommendationsObj;
        int count = 0;
        
        for (Map<String, Object> rec : recommendations) {
            String permissionLabel = (String) rec.get("permission"); // e.g., "MedicalRecord_read"
            Object confidenceObj = rec.get("confidence");
            double confidence = confidenceObj instanceof Number ? ((Number) confidenceObj).doubleValue() : 0.6;
            
            // Parse permission label: "ResourceType_action"
            String[] parts = permissionLabel.split("_");
            if (parts.length >= 2) {
                String resourceType = parts[0];
                String action = parts[1];
                
                // Tìm permission trong database
                permissionRepository.findFirstByResourceTypeAndAction(resourceType, action)
                    .ifPresent(permission -> {
                        // Kiểm tra user chưa có quyền này từ role
                        boolean alreadyHasFromRole = user.getRole().getPermissions().stream()
                            .anyMatch(p -> p.getId().equals(permission.getId()));
                        
                        // Nếu đã có quyền từ role thì không cần thêm
                        if (alreadyHasFromRole) {
                            return;
                        }
                        
                        // Kiểm tra chưa có pending request
                        boolean alreadyPending = pendingRepo.existsByUserIdAndPermissionIdAndStatus(
                            user.getId(), permission.getId(), "PENDING");
                        
                        if (!alreadyPending) {
                            PendingPermissionRequest pending = PendingPermissionRequest.builder()
                                .user(user)
                                .permission(permission)
                                .confidence(BigDecimal.valueOf(confidence))
                                .requestType("NEW_USER")
                                .changeType("ADD")
                                .status("PENDING")
                                .build();
                            
                            pendingRepo.save(pending);
                            log.debug("Created pending permission request: {} -> {} (confidence: {})",
                                user.getUsername(), permissionLabel, confidence);
                        }
                    });
                count++;
            }
        }
        
        return count;
    }
    
    private String generateUserId(String roleName) {
        String prefix = switch (roleName.toUpperCase()) {
            case "DOCTOR" -> "DOC";
            case "NURSE" -> "NUR";
            case "RECEPTIONIST" -> "REC";
            case "CASHIER" -> "CSH";
            case "HR" -> "HR";
            case "MANAGER" -> "MGR";
            case "ITADMIN" -> "IT";
            case "SECURITYADMIN" -> "SEC";
            default -> "USR";
        };
        
        // Generate unique ID
        String uniquePart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String userId = prefix + uniquePart;
        
        // Đảm bảo userId unique
        while (userRepository.existsByUserId(userId)) {
            uniquePart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            userId = prefix + uniquePart;
        }
        
        return userId;
    }
    
    private UserDto mapToUserDto(User user) {
        // Group permissions by resourceType -> actions (comma separated)
        Map<String, String> permissionsMap = groupPermissionsByResource(user);
        
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
                .permissions(permissionsMap)
                .build();
    }
    
    /**
     * Group permissions by resource type
     * Example output: {"AdmissionRecord": "read", "ClinicalNote": "create,read"}
     */
    private Map<String, String> groupPermissionsByResource(User user) {
        // Collect all permissions (from role + additional permissions)
        Set<com.auth.auth_service.entity.Permission> allPermissions = new HashSet<>(user.getRole().getPermissions());
        allPermissions.addAll(user.getAdditionalPermissions());
        
        // Group by resourceType -> Set of actions
        Map<String, Set<String>> resourceActionsMap = new LinkedHashMap<>();
        
        for (com.auth.auth_service.entity.Permission permission : allPermissions) {
            String resourceType = permission.getResourceType();
            String action = permission.getAction();
            
            resourceActionsMap.computeIfAbsent(resourceType, k -> new TreeSet<>()).add(action);
        }
        
        // Convert to Map<String, String> with comma-separated actions
        Map<String, String> result = new LinkedHashMap<>();
        resourceActionsMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> result.put(entry.getKey(), String.join(",", entry.getValue())));
        
        return result;
    }
}

