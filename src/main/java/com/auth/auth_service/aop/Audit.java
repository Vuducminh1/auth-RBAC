package com.auth.auth_service.aop;

import java.lang.annotation.*;

/**
 * Annotation to mark methods for audit logging.
 * resourceType: optional human-readable resource type (e.g., "MedicalRecord").
 * action: optional action name (e.g., "read", "update").
 * useFirstArgAsResourceId: if true, the aspect will use the first method argument's toString() as resourceId.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audit {
    String resourceType() default "";
    String action() default "";
    boolean useFirstArgAsResourceId() default true;
}

