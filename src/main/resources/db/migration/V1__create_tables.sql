-- =====================================================
-- V1: Create all tables for Auth RBAC/ABAC Service
-- =====================================================

-- Permissions table
CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    resource_type VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    scope VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    permission_key VARCHAR(255) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Roles table
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Role-Permission mapping (Many-to-Many)
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(50) UNIQUE NOT NULL,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    department VARCHAR(100) NOT NULL,
    branch VARCHAR(100) NOT NULL,
    position VARCHAR(100),
    has_license BOOLEAN DEFAULT FALSE,
    seniority VARCHAR(50) NOT NULL,
    employment_type VARCHAR(50) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    account_non_expired BOOLEAN DEFAULT TRUE,
    account_non_locked BOOLEAN DEFAULT TRUE,
    credentials_non_expired BOOLEAN DEFAULT TRUE,
    role_id BIGINT NOT NULL REFERENCES roles(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User assigned patients (for ABAC - Doctor/Nurse can only access assigned patients)
CREATE TABLE IF NOT EXISTS user_assigned_patients (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    patient_id VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, patient_id)
);

-- User additional permissions (beyond role permissions)
CREATE TABLE IF NOT EXISTS user_additional_permissions (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, permission_id)
);

-- Audit logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    allowed BOOLEAN NOT NULL,
    policy_id VARCHAR(255),
    deny_reasons VARCHAR(1000),
    risk_score INTEGER,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500)
);

-- Comments
COMMENT ON TABLE permissions IS 'Stores all permissions (resource:action:scope)';
COMMENT ON TABLE roles IS 'Stores all roles (Doctor, Nurse, HR, etc.)';
COMMENT ON TABLE role_permissions IS 'Maps roles to their permissions (RBAC)';
COMMENT ON TABLE users IS 'Stores user information with ABAC attributes';
COMMENT ON TABLE user_assigned_patients IS 'Stores patient assignments for ABAC';
COMMENT ON TABLE audit_logs IS 'Stores all authorization decisions for audit';

