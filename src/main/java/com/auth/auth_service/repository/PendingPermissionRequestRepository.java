package com.auth.auth_service.repository;

import com.auth.auth_service.entity.PendingPermissionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PendingPermissionRequestRepository extends JpaRepository<PendingPermissionRequest, Long> {
    
    /**
     * Tìm theo status
     */
    List<PendingPermissionRequest> findByStatus(String status);
    
    /**
     * Tìm theo user và status
     */
    List<PendingPermissionRequest> findByUserIdAndStatus(Long userId, String status);
    
    /**
     * Tìm theo user
     */
    List<PendingPermissionRequest> findByUserId(Long userId);
    
    /**
     * Tìm tất cả pending, sắp xếp theo thời gian
     */
    @Query("SELECT p FROM PendingPermissionRequest p WHERE p.status = 'PENDING' ORDER BY p.requestedAt DESC")
    List<PendingPermissionRequest> findAllPending();
    
    /**
     * Tìm pending theo request type
     */
    List<PendingPermissionRequest> findByStatusAndRequestType(String status, String requestType);
    
    /**
     * Kiểm tra đã tồn tại pending request chưa
     */
    boolean existsByUserIdAndPermissionIdAndStatus(Long userId, Long permissionId, String status);
    
    /**
     * Kiểm tra đã tồn tại pending request với change type
     */
    boolean existsByUserIdAndPermissionIdAndStatusAndChangeType(Long userId, Long permissionId, String status, String changeType);
    
    /**
     * Đếm số pending requests
     */
    long countByStatus(String status);
    
    /**
     * Tìm theo request type
     */
    List<PendingPermissionRequest> findByRequestType(String requestType);
    
    /**
     * Xóa tất cả pending của một user (khi user bị xóa hoặc reset)
     */
    void deleteByUserIdAndStatus(Long userId, String status);
}
