package com.auth.auth_service.dto.mock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigDto {
    private String configId;
    private String configKey;
    private String configValue;
    private String dataType; // String, Integer, Boolean, JSON
    private String category; // Security, System, Notification, Integration
    private String description;
    private boolean encrypted;
    private String modifiedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

