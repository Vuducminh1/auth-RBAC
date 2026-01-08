package com.auth.auth_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String userId;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    private String email;
    
    @Column(nullable = false)
    private String department;
    
    @Column(nullable = false)
    private String branch;
    
    private String position;
    
    @Column(nullable = false)
    private boolean hasLicense;
    
    @Column(nullable = false)
    private String seniority;
    
    @Column(nullable = false)
    private String employmentType;
    
    @Column(nullable = false)
    private boolean enabled = true;
    
    @Column(nullable = false)
    private boolean accountNonExpired = true;
    
    @Column(nullable = false)
    private boolean accountNonLocked = true;
    
    @Column(nullable = false)
    private boolean credentialsNonExpired = true;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_assigned_patients", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "patient_id")
    @Builder.Default
    private Set<String> assignedPatients = new HashSet<>();
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_additional_permissions",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    private Set<Permission> additionalPermissions = new HashSet<>();
}

