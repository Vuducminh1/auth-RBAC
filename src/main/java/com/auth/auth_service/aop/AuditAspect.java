// language: java
package com.auth.auth_service.aop;

import com.auth.auth_service.entity.AuditLog;
import com.auth.auth_service.repository.AuditLogRepository;
import com.auth.auth_service.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    @Around("@annotation(Audit)")
    public Object aroundAudit(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Audit audit = method.getAnnotation(Audit.class);
        if (audit == null && pjp.getTarget() != null) {
            // try on class
            audit = pjp.getTarget().getClass().getAnnotation(Audit.class);
        }

        // Start with annotation-provided values (if any)
        String resourceType = null;
        String action = null;

        if (audit != null) {
            if (!audit.resourceType().isEmpty()) resourceType = audit.resourceType();
            if (!audit.action().isEmpty()) action = audit.action();
        }

        // Prepare request context
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest req = attrs != null ? attrs.getRequest() : null;
        HttpServletResponse resp = attrs != null ? attrs.getResponse() : null;

        String uri = null;
        if (req != null) {
            uri = req.getRequestURI();
        }
        // Final fallbacks
        if (resourceType == null) resourceType = method.getDeclaringClass().getSimpleName();
        if (action == null) action = method.getName();

        // prepare audit metadata
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = "anonymous";
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal user) {
            userId = user.getUserId();
        }

        String ip = null;
        String userAgent = null;
        if (req != null) {
            try {
                ip = req.getRemoteAddr();
                userAgent = req.getHeader("User-Agent");
            } catch (Exception ignored) {
            }
        }

        boolean allowed = true;
        String policyId = null;
        String denyReasons = null;
        Integer riskScore = null;
        int statusCode = 200;
        LocalDateTime now = LocalDateTime.now();
        Object resultObj = null;

        try {
            resultObj = pjp.proceed();

            // Prefer explicit ResponseEntity status
            if (resultObj instanceof ResponseEntity<?>) {
                statusCode = ((ResponseEntity<?>) resultObj).getStatusCode().value();
            } else if (resp != null) {
                // If controller wrote to the servlet response (or exception was translated), read it
                try {
                    statusCode = resp.getStatus();
                } catch (Exception ignored) {
                }
            } else {
                // Fallback to @ResponseStatus on method or class
                ResponseStatus rs = method.getAnnotation(ResponseStatus.class);
                if (rs == null && pjp.getTarget() != null) {
                    rs = pjp.getTarget().getClass().getAnnotation(ResponseStatus.class);
                }
                if (rs != null) {
                    try {
                        statusCode = rs.value().value();
                    } catch (Exception ignored) {
                    }
                }
            }
            denyReasons = "HTTP_" + statusCode;
            return resultObj;
        } catch (Throwable ex) {
            allowed = false;
            // Try to extract meaningful HTTP status for exceptions
            if (ex instanceof ResponseStatusException rse) {
                statusCode = rse.getStatusCode().value();
            } else {
                // check @ResponseStatus on the exception class
                ResponseStatus rs = ex.getClass().getAnnotation(ResponseStatus.class);
                if (rs != null) {
                    try {
                        statusCode = rs.value().value();
                    } catch (Exception ignored) {
                    }
                } else if (resp != null) {
                    try {
                        statusCode = resp.getStatus();
                    } catch (Exception ignored) {
                        statusCode = 500;
                    }
                } else {
                    statusCode = 500;
                }
            }
            denyReasons = ex.getMessage();
            throw ex;
        } finally {
            // build policy id using helper
            policyId = generatePolicyId(resourceType, action, allowed, statusCode);
            try {
                AuditLog logEntry = AuditLog.builder()
                    .userId(userId)
                    .resourceType(resourceType)
                    .resourceId(uri == null ? "N/A" : uri)
                    .action(action)
                    .allowed(allowed)
                    .policyId(policyId)
                    .denyReasons(denyReasons)
                    .riskScore(riskScore)
                    .timestamp(now)
                    .ipAddress(ip)
                    .userAgent(userAgent)
                    .build();

                auditLogRepository.save(logEntry);
            } catch (Exception e) {
                log.error("Failed to write audit log", e);
            }
        }
    }

    private String generatePolicyId(String resourceType, String method, boolean success, int statusCode) {
        if (success) {
            return "ALLOW_" + resourceType + "_" + method;
        } else if (statusCode == 401) {
            return "DENY_UNAUTHENTICATED";
        } else if (statusCode == 403) {
            return "DENY_UNAUTHORIZED";
        } else if (statusCode == 404) {
            return "DENY_NOT_FOUND";
        } else {
            return "DENY_HTTP_" + statusCode;
        }
    }
}
