package com.auth.auth_service.security;

import com.auth.auth_service.entity.Permission;
import com.auth.auth_service.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
public class UserPrincipal implements UserDetails {
    
    private Long id;
    private String userId;
    private String username;
    private String password;
    private String role;
    private String department;
    private String branch;
    private String position;
    private boolean hasLicense;
    private String seniority;
    private String employmentType;
    private boolean enabled;
    private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private Set<String> assignedPatients;
    private Collection<? extends GrantedAuthority> authorities;
    
    public static UserPrincipal create(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        
        // Add role authority
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()));
        
        // Add permissions from role
        if (user.getRole().getPermissions() != null) {
            user.getRole().getPermissions().forEach(permission -> 
                authorities.add(new SimpleGrantedAuthority(permission.getPermissionKey()))
            );
        }
        
        // Add additional user permissions
        if (user.getAdditionalPermissions() != null) {
            user.getAdditionalPermissions().forEach(permission -> 
                authorities.add(new SimpleGrantedAuthority(permission.getPermissionKey()))
            );
        }
        
        return UserPrincipal.builder()
                .id(user.getId())
                .userId(user.getUserId())
                .username(user.getUsername())
                .password(user.getPassword())
                .role(user.getRole().getName())
                .department(user.getDepartment())
                .branch(user.getBranch())
                .position(user.getPosition())
                .hasLicense(user.isHasLicense())
                .seniority(user.getSeniority())
                .employmentType(user.getEmploymentType())
                .enabled(user.isEnabled())
                .accountNonExpired(user.isAccountNonExpired())
                .accountNonLocked(user.isAccountNonLocked())
                .credentialsNonExpired(user.isCredentialsNonExpired())
                .assignedPatients(user.getAssignedPatients())
                .authorities(authorities)
                .build();
    }
    
    public Set<String> getPermissionKeys() {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> !auth.startsWith("ROLE_"))
                .collect(Collectors.toSet());
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
}

