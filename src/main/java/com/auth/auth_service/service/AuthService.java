package com.auth.auth_service.service;

import com.auth.auth_service.dto.LoginRequest;
import com.auth.auth_service.dto.LoginResponse;
import com.auth.auth_service.dto.UserDto;
import com.auth.auth_service.entity.User;
import com.auth.auth_service.repository.UserRepository;
import com.auth.auth_service.security.JwtTokenProvider;
import com.auth.auth_service.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    
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
                .permissions(userPrincipal.getPermissionKeys())
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
    
    private UserDto mapToUserDto(User user) {
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
                .permissions(user.getRole().getPermissions().stream()
                        .map(p -> p.getPermissionKey())
                        .collect(Collectors.toSet()))
                .build();
    }
}

