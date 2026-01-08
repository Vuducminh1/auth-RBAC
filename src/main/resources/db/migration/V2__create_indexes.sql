-- =====================================================
-- V2: Create indexes for better performance
-- =====================================================

-- Permissions indexes
CREATE INDEX IF NOT EXISTS idx_permissions_resource_type ON permissions(resource_type);
CREATE INDEX IF NOT EXISTS idx_permissions_action ON permissions(action);
CREATE INDEX IF NOT EXISTS idx_permissions_key ON permissions(permission_key);

-- Roles indexes
CREATE INDEX IF NOT EXISTS idx_roles_name ON roles(name);

-- Users indexes
CREATE INDEX IF NOT EXISTS idx_users_user_id ON users(user_id);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_department ON users(department);
CREATE INDEX IF NOT EXISTS idx_users_branch ON users(branch);
CREATE INDEX IF NOT EXISTS idx_users_role_id ON users(role_id);

-- User assigned patients indexes
CREATE INDEX IF NOT EXISTS idx_user_assigned_patients_user_id ON user_assigned_patients(user_id);
CREATE INDEX IF NOT EXISTS idx_user_assigned_patients_patient_id ON user_assigned_patients(patient_id);

-- Audit logs indexes
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_resource_type ON audit_logs(resource_type);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_allowed ON audit_logs(allowed);
CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_logs_risk_score ON audit_logs(risk_score);

