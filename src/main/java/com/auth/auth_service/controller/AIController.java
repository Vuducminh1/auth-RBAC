package com.auth.auth_service.controller;

import com.auth.auth_service.dto.ApiResponse;
import com.auth.auth_service.service.AIPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller cho AI Permission Recommendation APIs Proxy các request tới PoweredAI-RBAC service
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AIController {

  private final AIPermissionService aiPermissionService;

  /**
   * Gợi ý quyền cho người dùng mới POST /api/ai/recommend/new-user
   * <p>
   * Request body: { "role": "Doctor", "department": "Khoa_Noi", "branch": "CN_HN", "license":
   * "Yes", "seniority": "Senior" }
   */
  @PostMapping("/recommend/new-user")
  @PreAuthorize("hasAnyRole('HR', 'SecurityAdmin', 'Manager')")
  public ResponseEntity<ApiResponse<Map<String, Object>>> recommendNewUser(
      @RequestBody Map<String, String> profile) {
    log.info("AI recommend new user request: {}", profile);

    String role = profile.getOrDefault("role", "");
    String department = profile.getOrDefault("department", "");
    String branch = profile.getOrDefault("branch", "");
    String license = profile.getOrDefault("license", "Yes");
    String seniority = profile.getOrDefault("seniority", "Senior");
    String position = profile.getOrDefault("position", "Doctor");
    String employmentType = profile.getOrDefault("employment_type", "Fulltime");

    Map<String, Object> result = aiPermissionService.recommendNewUser(
        role, department, branch, license, seniority, position, employmentType
    );

    if (result.containsKey("error")) {
      return ResponseEntity.ok(ApiResponse.error((String) result.get("error")));
    }

    return ResponseEntity.ok(ApiResponse.success(result));
  }

  /**
   * Gợi ý quyền khi chuyển vị trí công tác POST /api/ai/recommend/job-transfer
   * <p>
   * Request body: { "old_profile": { "role": "Doctor", "department": "Khoa_Noi", "branch": "CN_HN",
   * "license": "Yes", "seniority": "Senior" }, "new_profile": { "role": "HR", "department":
   * "Phong_NhanSu", "branch": "CN_HN", "license": "No", "seniority": "Senior" } }
   */
  @PostMapping("/recommend/job-transfer")
  @PreAuthorize("hasAnyRole('HR', 'SecurityAdmin', 'Manager')")
  @SuppressWarnings("unchecked")
  public ResponseEntity<ApiResponse<Map<String, Object>>> recommendJobTransfer(
      @RequestBody Map<String, Object> request) {
    log.info("AI recommend job transfer request");

    Map<String, String> oldProfile = (Map<String, String>) request.get("old_profile");
    Map<String, String> newProfile = (Map<String, String>) request.get("new_profile");

    if (oldProfile == null || newProfile == null) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("old_profile and new_profile are required"));
    }

    Map<String, Object> result = aiPermissionService.recommendJobTransfer(oldProfile, newProfile);

    if (result.containsKey("error")) {
      return ResponseEntity.ok(ApiResponse.error((String) result.get("error")));
    }

    return ResponseEntity.ok(ApiResponse.success(result));
  }

  /**
   * Rightsizing - phát hiện quyền không sử dụng POST /api/ai/recommend/rightsizing?lookbackDays=90
   */
  @PostMapping("/recommend/rightsizing")
  @PreAuthorize("hasAnyRole('SecurityAdmin', 'ITAdmin')")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getRightsizing(
      @RequestParam(defaultValue = "90") int lookbackDays) {
    log.info("AI rightsizing request with lookbackDays={}", lookbackDays);

    Map<String, Object> result = aiPermissionService.getRightsizing(lookbackDays);

    if (result.containsKey("error")) {
      return ResponseEntity.ok(ApiResponse.error((String) result.get("error")));
    }

    return ResponseEntity.ok(ApiResponse.success(result));
  }

  /**
   * Phát hiện bất thường POST /api/ai/recommend/anomaly?riskThreshold=3
   */
  @PostMapping("/recommend/anomaly")
  @PreAuthorize("hasRole('SecurityAdmin')")
  public ResponseEntity<ApiResponse<Map<String, Object>>> detectAnomaly(
      @RequestParam(defaultValue = "3") int riskThreshold) {
    log.info("AI anomaly detection request with riskThreshold={}", riskThreshold);

    Map<String, Object> result = aiPermissionService.detectAnomaly(riskThreshold);

    if (result.containsKey("error")) {
      return ResponseEntity.ok(ApiResponse.error((String) result.get("error")));
    }

    return ResponseEntity.ok(ApiResponse.success(result));
  }

  /**
   * Kiểm tra trạng thái kết nối AI service GET /api/ai/health
   */
  @GetMapping("/health")
  public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
    boolean isHealthy = aiPermissionService.healthCheck();

    Map<String, Object> status = Map.of(
        "ai_service", isHealthy ? "UP" : "DOWN",
        "message", isHealthy ? "AI service is running" : "AI service is not available"
    );

    if (isHealthy) {
      return ResponseEntity.ok(ApiResponse.success(status));
    }
    return ResponseEntity.ok(ApiResponse.success("AI service is not available", status));
  }
}

