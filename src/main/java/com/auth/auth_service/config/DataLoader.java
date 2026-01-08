package com.auth.auth_service.config;

import com.auth.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * DataLoader is now disabled as data is managed by Flyway migrations.
 * Migration files are in: src/main/resources/db/migration/
 * 
 * - V1__create_tables.sql: Creates all database tables
 * - V2__create_indexes.sql: Creates indexes for performance
 * - V3__seed_roles_permissions.sql: Seeds roles and permissions (RBAC)
 * - V4__seed_users.sql: Seeds sample users with ABAC attributes
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {
    
    private final UserRepository userRepository;
    
    @Override
    public void run(String... args) {
        long userCount = userRepository.count();
        log.info("Application started. Total users in database: {}", userCount);
        
        if (userCount == 0) {
            log.warn("No users found in database. Please check Flyway migrations.");
        } else {
            log.info("Database initialized successfully with Flyway migrations.");
        }
    }
}
