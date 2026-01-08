package com.auth.auth_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "permissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String resourceType;
    
    @Column(nullable = false)
    private String action;
    
    @Column(nullable = false)
    private String scope;
    
    private String description;
    
    @Column(unique = true)
    private String permissionKey;
    
    @PrePersist
    @PreUpdate
    public void generatePermissionKey() {
        this.permissionKey = resourceType + ":" + action + ":" + scope;
    }
}

