package com.auth.auth_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private String resourceType;
    
    @Column(nullable = false)
    private String resourceId;
    
    @Column(nullable = false)
    private String action;
    
    @Column(nullable = false)
    private boolean allowed;
    
    private String policyId;
    
    @Column(length = 1000)
    private String denyReasons;
    
    private Integer riskScore;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    private String ipAddress;
    
    private String userAgent;
    
    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}

