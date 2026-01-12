package com.auth.auth_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Service để gọi AI Permission Recommendation API
 * Kết nối với PoweredAI-RBAC service
 */
@Service
@Slf4j
public class AIPermissionService {
    
    private final WebClient webClient;
    private final int timeout;
    
    public AIPermissionService(
            @Value("${ai.service.url:http://localhost:8000}") String aiServiceUrl,
            @Value("${ai.service.timeout:5000}") int timeout) {
        this.webClient = WebClient.builder()
                .baseUrl(aiServiceUrl)
                .build();
        this.timeout = timeout;
        log.info("AIPermissionService initialized with URL: {}", aiServiceUrl);
    }
    
    /**
     * Gợi ý quyền cho người dùng mới
     * @param role Vai trò (Doctor, Nurse, Receptionist, Cashier, HR)
     * @param department Phòng ban (Khoa_Noi, Khoa_Ngoai, Phong_TiepDon, etc.)
     * @param branch Chi nhánh (CN_HN, CN_HCM)
     * @param license Có giấy phép (Yes/No)
     * @param seniority Thâm niên (Junior/Senior)
     * @return Map chứa danh sách quyền được gợi ý và độ tin cậy
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> recommendNewUser(String role, String department, String branch, 
                                                 String license, String seniority) {
        log.info("Requesting AI recommendation for new user: role={}, department={}, branch={}", 
                role, department, branch);
        
        Map<String, String> profile = Map.of(
            "role", role,
            "department", department,
            "branch", branch,
            "license", license,
            "seniority", seniority
        );
        
        try {
            Map<String, Object> result = webClient.post()
                    .uri("/recommend/new-user")
                    .bodyValue(profile)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();
            
            log.info("AI recommendation received successfully");
            return result;
        } catch (WebClientResponseException e) {
            log.error("AI Service HTTP error: {} - {}", e.getStatusCode(), e.getMessage());
            return Map.of(
                "error", "AI service error",
                "status", e.getStatusCode().value(),
                "message", e.getMessage()
            );
        } catch (Exception e) {
            log.error("AI Service connection error: {}", e.getMessage());
            return Map.of(
                "error", "AI service unavailable",
                "message", e.getMessage()
            );
        }
    }
    
    /**
     * Gợi ý quyền khi chuyển vị trí công tác
     * @param oldProfile Profile cũ của người dùng
     * @param newProfile Profile mới của người dùng
     * @return Map chứa quyền được thêm, gỡ và giữ lại
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> recommendJobTransfer(Map<String, String> oldProfile, 
                                                     Map<String, String> newProfile) {
        log.info("Requesting AI recommendation for job transfer: {} -> {}", 
                oldProfile.get("role"), newProfile.get("role"));
        
        Map<String, Object> request = Map.of(
            "old_profile", oldProfile,
            "new_profile", newProfile
        );
        
        try {
            Map<String, Object> result = webClient.post()
                    .uri("/recommend/job-transfer")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();
            
            log.info("AI job transfer recommendation received successfully");
            return result;
        } catch (WebClientResponseException e) {
            log.error("AI Service HTTP error: {} - {}", e.getStatusCode(), e.getMessage());
            return Map.of(
                "error", "AI service error",
                "status", e.getStatusCode().value(),
                "message", e.getMessage()
            );
        } catch (Exception e) {
            log.error("AI Service connection error: {}", e.getMessage());
            return Map.of(
                "error", "AI service unavailable",
                "message", e.getMessage()
            );
        }
    }
    
    /**
     * Rightsizing - phát hiện quyền không sử dụng
     * @param lookbackDays Số ngày quay lại để kiểm tra (mặc định 90)
     * @return Map chứa danh sách quyền không sử dụng
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getRightsizing(int lookbackDays) {
        log.info("Requesting AI rightsizing analysis with lookback_days={}", lookbackDays);
        
        try {
            Map<String, Object> result = webClient.post()
                    .uri("/recommend/rightsizing")
                    .bodyValue(Map.of("lookback_days", lookbackDays))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();
            
            log.info("AI rightsizing analysis received successfully");
            return result;
        } catch (WebClientResponseException e) {
            log.error("AI Service HTTP error: {} - {}", e.getStatusCode(), e.getMessage());
            return Map.of(
                "error", "AI service error",
                "status", e.getStatusCode().value(),
                "message", e.getMessage()
            );
        } catch (Exception e) {
            log.error("AI Service connection error: {}", e.getMessage());
            return Map.of(
                "error", "AI service unavailable",
                "message", e.getMessage()
            );
        }
    }
    
    /**
     * Phát hiện bất thường trong audit logs
     * @param riskThreshold Ngưỡng rủi ro (mặc định 3)
     * @return Map chứa danh sách các sự kiện bất thường
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> detectAnomaly(int riskThreshold) {
        log.info("Requesting AI anomaly detection with risk_threshold={}", riskThreshold);
        
        try {
            Map<String, Object> result = webClient.post()
                    .uri("/recommend/anomaly")
                    .bodyValue(Map.of("risk_threshold", riskThreshold))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();
            
            log.info("AI anomaly detection received successfully");
            return result;
        } catch (WebClientResponseException e) {
            log.error("AI Service HTTP error: {} - {}", e.getStatusCode(), e.getMessage());
            return Map.of(
                "error", "AI service error",
                "status", e.getStatusCode().value(),
                "message", e.getMessage()
            );
        } catch (Exception e) {
            log.error("AI Service connection error: {}", e.getMessage());
            return Map.of(
                "error", "AI service unavailable",
                "message", e.getMessage()
            );
        }
    }
    
    /**
     * Kiểm tra kết nối tới AI service
     * @return true nếu kết nối thành công
     */
    public boolean healthCheck() {
        try {
            webClient.get()
                    .uri("/docs")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();
            return true;
        } catch (Exception e) {
            log.warn("AI Service health check failed: {}", e.getMessage());
            return false;
        }
    }
}


