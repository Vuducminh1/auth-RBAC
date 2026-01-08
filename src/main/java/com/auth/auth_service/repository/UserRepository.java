package com.auth.auth_service.repository;

import com.auth.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUserId(String userId);
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    boolean existsByUserId(String userId);
    boolean existsByEmail(String email);
    
    List<User> findByDepartment(String department);
    List<User> findByBranch(String branch);
    List<User> findByRoleName(String roleName);
    
    @Query("SELECT u FROM User u WHERE u.department = :department AND u.branch = :branch")
    List<User> findByDepartmentAndBranch(@Param("department") String department, @Param("branch") String branch);
}

