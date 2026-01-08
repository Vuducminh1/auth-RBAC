package com.auth.auth_service.controller;

import com.auth.auth_service.dto.ApiResponse;
import com.auth.auth_service.entity.AuditLog;
import com.auth.auth_service.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {
    
    private final AuditService auditService;
    
    @GetMapping
    @PreAuthorize("hasAnyRole('SecurityAdmin', 'ITAdmin')")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> logs = auditService.getAuditLogs(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp")));
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
    
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('SecurityAdmin', 'ITAdmin')")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogsByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> logs = auditService.getAuditLogsByUser(userId, 
                PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
    
    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('SecurityAdmin', 'ITAdmin')")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> logs = auditService.getAuditLogsByTimeRange(start, end, 
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp")));
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
    
    @GetMapping("/high-risk")
    @PreAuthorize("hasAnyRole('SecurityAdmin', 'ITAdmin')")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getHighRiskActions(
            @RequestParam(defaultValue = "5") int threshold) {
        List<AuditLog> logs = auditService.getHighRiskActions(threshold);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
    
    @GetMapping("/denied")
    @PreAuthorize("hasAnyRole('SecurityAdmin', 'ITAdmin')")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getDeniedAccesses() {
        List<AuditLog> logs = auditService.getDeniedAccesses();
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}

