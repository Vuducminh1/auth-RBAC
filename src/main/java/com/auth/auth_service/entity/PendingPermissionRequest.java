package com.auth.auth_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity lưu các gợi ý permission từ AI chờ admin duyệt.
 * Sử dụng cho:
 * - NEW_USER: Gợi ý quyền khi tạo user mới
 * - JOB_TRANSFER: Gợi ý quyền khi chuyển phòng ban
 * - RIGHTSIZING: Gợi ý thu hồi quyền không sử dụng
 */
@Entity
@Table(name = "pending_permission_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingPermissionRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * User được gợi ý permission
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * Permission được gợi ý
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;
    
    /**
     * Độ tin cậy từ AI model (0.60 - 1.00)
     */
    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal confidence;
    
    /**
     * Loại yêu cầu: NEW_USER, JOB_TRANSFER, RIGHTSIZING
     */
    @Column(name = "request_type", nullable = false)
    @Builder.Default
    private String requestType = "NEW_USER";
    
    /**
     * Loại thay đổi: ADD (thêm quyền), REMOVE (thu hồi quyền)
     */
    @Column(name = "change_type", nullable = false)
    @Builder.Default
    private String changeType = "ADD";
    
    /**
     * Trạng thái: PENDING, APPROVED, REJECTED
     */
    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";
    
    /**
     * Thời gian tạo request
     */
    @Column(name = "requested_at", nullable = false)
    @Builder.Default
    private LocalDateTime requestedAt = LocalDateTime.now();
    
    /**
     * Admin đã review
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;
    
    /**
     * Thời gian review
     */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    /**
     * Ghi chú khi review
     */
    @Column(name = "review_notes", length = 500)
    private String reviewNotes;
}
