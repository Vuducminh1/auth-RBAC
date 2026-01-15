//package com.auth.auth_service.config;
//
//import com.auth.auth_service.entity.AuditLog;
//import com.auth.auth_service.repository.AuditLogRepository;
//import com.auth.auth_service.security.UserPrincipal;
//import jakarta.servlet.*;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.core.annotation.Order;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.time.LocalDateTime;
//import java.util.Set;
//
///**
// * Filter to log all API requests to audit_logs table
// * This ensures every API access is tracked, not just authorization checks
// */
//@Component
//@Order(1)
//@RequiredArgsConstructor
//@Slf4j
//public class AuditLoggingFilter implements Filter {
//
//    private final AuditLogRepository auditLogRepository;
//
//    // Endpoints that already have their own audit logging via AuthorizationService
//    private static final Set<String> SKIP_AUDIT_PATHS = Set.of(
//            "/api/mock/"  // Mock APIs already log via authorizationService.authorize()
//    );
//
//    // Endpoints to completely ignore (no logging needed)
//    private static final Set<String> IGNORE_PATHS = Set.of(
//            "/actuator",
//            "/swagger-ui",
//            "/v3/api-docs",
//            "/favicon.ico",
//            "/error"
//    );
//
//    @Override
//    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
//            throws IOException, ServletException {
//
//        HttpServletRequest httpRequest = (HttpServletRequest) request;
//        HttpServletResponse httpResponse = (HttpServletResponse) response;
//
//        String uri = httpRequest.getRequestURI();
//
//        // Skip static resources and ignored paths
//        if (shouldIgnore(uri)) {
//            chain.doFilter(request, response);
//            return;
//        }
//
//        // Skip paths that already have audit logging
//        if (shouldSkipAudit(uri)) {
//            chain.doFilter(request, response);
//            return;
//        }
//
//        long startTime = System.currentTimeMillis();
//
//        try {
//            chain.doFilter(request, response);
//        } finally {
//            // Log after request completes
//            logRequest(httpRequest, httpResponse, System.currentTimeMillis() - startTime);
//        }
//    }
//
//    private boolean shouldIgnore(String uri) {
//        return IGNORE_PATHS.stream().anyMatch(uri::startsWith) || uri.contains(".");
//    }
//
//    private boolean shouldSkipAudit(String uri) {
//        return SKIP_AUDIT_PATHS.stream().anyMatch(uri::startsWith);
//    }
//
//    private void logRequest(HttpServletRequest request, HttpServletResponse response, long duration) {
//        try {
//            String userId = getCurrentUserId();
//
//            // Don't log anonymous requests (except login attempts)
//            String uri = request.getRequestURI();
//            if ((userId == null || "anonymous".equals(userId)) && !uri.contains("/auth/login")) {
//                return;
//            }
//
//            boolean success = response.getStatus() < 400;
//            int statusCode = response.getStatus();
//
//            // For login endpoint, use special handling
//            if (uri.contains("/auth/login")) {
//                userId = userId != null ? userId : "LOGIN_ATTEMPT";
//            }
//
//            AuditLog auditLog = AuditLog.builder()
//                    .userId(userId != null ? userId : "anonymous")
//                    .resourceType(extractResourceType(uri))
//                    .resourceId(uri)
//                    .action(request.getMethod())
//                    .allowed(success)
//                    .policyId(generatePolicyId(uri, request.getMethod(), success, statusCode))
//                    .denyReasons(success ? null : "HTTP_" + statusCode)
//                    .riskScore(calculateRiskScore(request))
//                    .timestamp(LocalDateTime.now())
//                    .ipAddress(getClientIpAddress(request))
//                    .userAgent(request.getHeader("User-Agent"))
//                    .build();
//
//            auditLogRepository.save(auditLog);
//
//            log.debug("Audit logged: user={}, uri={}, method={}, status={}, duration={}ms",
//                    userId, uri, request.getMethod(), statusCode, duration);
//
//        } catch (Exception e) {
//            log.warn("Failed to log audit: {}", e.getMessage());
//        }
//    }
//
//    private String getCurrentUserId() {
//        try {
//            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//            if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
//                return ((UserPrincipal) auth.getPrincipal()).getUserId();
//            }
//            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
//                return auth.getName();
//            }
//        } catch (Exception e) {
//            log.debug("Could not get current user: {}", e.getMessage());
//        }
//        return "anonymous";
//    }
//
//    private String extractResourceType(String uri) {
//        if (uri.contains("/auth/login")) return "Authentication";
//        if (uri.contains("/auth/logout")) return "Authentication";
//        if (uri.contains("/auth/refresh")) return "Authentication";
//        if (uri.contains("/auth/me")) return "UserProfile";
//        if (uri.contains("/ai/recommend/new-user")) return "AIRecommendation";
//        if (uri.contains("/ai/recommend/job-transfer")) return "AIRecommendation";
//        if (uri.contains("/ai/recommend/rightsizing")) return "AIRightsizing";
//        if (uri.contains("/ai/recommend/anomaly")) return "AIAnomalyDetection";
//        if (uri.contains("/ai/health")) return "AIHealthCheck";
//        if (uri.contains("/users")) return "UserManagement";
//        if (uri.contains("/audit")) return "AuditLog";
//        if (uri.contains("/authz")) return "Authorization";
//        return "API";
//    }
//
//    private String generatePolicyId(String uri, String method, boolean success, int statusCode) {
//        String resourceType = extractResourceType(uri);
//        if (success) {
//            return "ALLOW_" + resourceType + "_" + method;
//        } else if (statusCode == 401) {
//            return "DENY_UNAUTHENTICATED";
//        } else if (statusCode == 403) {
//            return "DENY_UNAUTHORIZED";
//        } else if (statusCode == 404) {
//            return "DENY_NOT_FOUND";
//        } else {
//            return "DENY_HTTP_" + statusCode;
//        }
//    }
//
//    private int calculateRiskScore(HttpServletRequest request) {
//        int score = 0;
//
//        // Off-hours access (before 8 AM or after 6 PM)
//        int hour = LocalDateTime.now().getHour();
//        if (hour < 8 || hour > 18) {
//            score += 2;
//        }
//
//        // High-risk HTTP methods
//        String method = request.getMethod();
//        if ("DELETE".equals(method)) {
//            score += 3;
//        } else if ("PUT".equals(method) || "PATCH".equals(method)) {
//            score += 1;
//        }
//
//        // High-risk endpoints
//        String uri = request.getRequestURI();
//        if (uri.contains("export")) {
//            score += 3;
//        }
//        if (uri.contains("/system/") || uri.contains("/policies/")) {
//            score += 2;
//        }
//        if (uri.contains("/ai/recommend/anomaly")) {
//            score += 1; // Sensitive security operation
//        }
//
//        return score;
//    }
//
//    private String getClientIpAddress(HttpServletRequest request) {
//        // Check for proxy headers first
//        String[] headerNames = {
//                "X-Forwarded-For",
//                "X-Real-IP",
//                "Proxy-Client-IP",
//                "WL-Proxy-Client-IP"
//        };
//
//        for (String header : headerNames) {
//            String ip = request.getHeader(header);
//            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
//                // X-Forwarded-For can contain multiple IPs, take the first one
//                return ip.split(",")[0].trim();
//            }
//        }
//
//        return request.getRemoteAddr();
//    }
//}
//
