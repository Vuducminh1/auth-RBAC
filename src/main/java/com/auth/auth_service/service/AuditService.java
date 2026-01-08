package com.auth.auth_service.service;

import com.auth.auth_service.entity.AuditLog;
import com.auth.auth_service.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {
    
    private final AuditLogRepository auditLogRepository;
    
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByUser(String userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByTimeRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return auditLogRepository.findByTimestampBetween(start, end, pageable);
    }
    
    @Transactional(readOnly = true)
    public List<AuditLog> getHighRiskActions(int threshold) {
        return auditLogRepository.findByRiskScoreGreaterThan(threshold);
    }
    
    @Transactional(readOnly = true)
    public List<AuditLog> getDeniedAccesses() {
        return auditLogRepository.findByAllowed(false);
    }
}

