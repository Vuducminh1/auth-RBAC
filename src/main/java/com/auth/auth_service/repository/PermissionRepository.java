package com.auth.auth_service.repository;

import com.auth.auth_service.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByResourceTypeAndActionAndScope(String resourceType, String action, String scope);
    Optional<Permission> findByPermissionKey(String permissionKey);
    List<Permission> findByResourceType(String resourceType);
    List<Permission> findByAction(String action);
    boolean existsByPermissionKey(String permissionKey);
    
    /**
     * Tìm permission theo resourceType và action (không quan tâm scope)
     */
    Optional<Permission> findFirstByResourceTypeAndAction(String resourceType, String action);
}

