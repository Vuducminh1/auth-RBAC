package com.auth.auth_service.repository;

import com.auth.auth_service.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUserId(String userId);
    List<AuditLog> findByResourceType(String resourceType);
    List<AuditLog> findByAllowed(boolean allowed);
    
    Page<AuditLog> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);
    Page<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    List<AuditLog> findByUserIdAndTimestampBetween(String userId, LocalDateTime start, LocalDateTime end);
    List<AuditLog> findByRiskScoreGreaterThan(Integer riskScore);
}

