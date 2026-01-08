-- =====================================================
-- V3: Seed roles and permissions data (RBAC baseline)
-- Based on emr_authz.rego from PoweredAI-RBAC
-- =====================================================

-- Insert all permissions
-- Doctor permissions
INSERT INTO permissions (resource_type, action, scope, description, permission_key) VALUES
('PatientProfile', 'read', 'all', 'Read patient profile', 'PatientProfile:read:all'),
('MedicalRecord', 'read', 'all', 'Read medical record', 'MedicalRecord:read:all'),
('MedicalRecord', 'create', 'all', 'Create medical record', 'MedicalRecord:create:all'),
('MedicalRecord', 'update', 'all', 'Update medical record', 'MedicalRecord:update:all'),
('ClinicalNote', 'read', 'all', 'Read clinical note', 'ClinicalNote:read:all'),
('ClinicalNote', 'create', 'all', 'Create clinical note', 'ClinicalNote:create:all'),
('VitalSigns', 'read', 'all', 'Read vital signs', 'VitalSigns:read:all'),
('VitalSigns', 'create', 'all', 'Create vital signs', 'VitalSigns:create:all'),
('VitalSigns', 'update', 'all', 'Update vital signs', 'VitalSigns:update:all'),
('Prescription', 'read', 'all', 'Read prescription', 'Prescription:read:all'),
('Prescription', 'create', 'all', 'Create prescription', 'Prescription:create:all'),
('Prescription', 'update', 'all', 'Update prescription', 'Prescription:update:all'),
('Prescription', 'approve', 'all', 'Approve prescription', 'Prescription:approve:all'),
('LabOrder', 'create', 'all', 'Create lab order', 'LabOrder:create:all'),
('LabOrder', 'read', 'all', 'Read lab order', 'LabOrder:read:all'),
('LabResult', 'read', 'all', 'Read lab result', 'LabResult:read:all'),
('ImagingOrder', 'create', 'all', 'Create imaging order', 'ImagingOrder:create:all'),
('ImagingOrder', 'read', 'all', 'Read imaging order', 'ImagingOrder:read:all'),
('ImagingResult', 'read', 'all', 'Read imaging result', 'ImagingResult:read:all'),
('AdmissionRecord', 'read', 'all', 'Read admission record', 'AdmissionRecord:read:all'),
('AdmissionRecord', 'create', 'all', 'Create admission record', 'AdmissionRecord:create:all'),
('TransferRecord', 'read', 'all', 'Read transfer record', 'TransferRecord:read:all'),
('TransferRecord', 'create', 'all', 'Create transfer record', 'TransferRecord:create:all'),
('DischargeSummary', 'read', 'all', 'Read discharge summary', 'DischargeSummary:read:all'),
('DischargeSummary', 'create', 'all', 'Create discharge summary', 'DischargeSummary:create:all'),
('MedicalReport', 'read', 'all', 'Read medical report', 'MedicalReport:read:all'),
-- Receptionist permissions
('PatientProfile', 'create', 'all', 'Create patient profile', 'PatientProfile:create:all'),
('PatientProfile', 'update', 'all', 'Update patient profile', 'PatientProfile:update:all'),
('Appointment', 'create', 'all', 'Create appointment', 'Appointment:create:all'),
('Appointment', 'read', 'all', 'Read appointment', 'Appointment:read:all'),
('Appointment', 'update', 'all', 'Update appointment', 'Appointment:update:all'),
-- Cashier permissions
('BillingRecord', 'create', 'all', 'Create billing record', 'BillingRecord:create:all'),
('BillingRecord', 'read', 'all', 'Read billing record', 'BillingRecord:read:all'),
('BillingRecord', 'update', 'all', 'Update billing record', 'BillingRecord:update:all'),
('Invoice', 'create', 'all', 'Create invoice', 'Invoice:create:all'),
('Invoice', 'read', 'all', 'Read invoice', 'Invoice:read:all'),
('Invoice', 'update', 'all', 'Update invoice', 'Invoice:update:all'),
('InsuranceClaim', 'create', 'all', 'Create insurance claim', 'InsuranceClaim:create:all'),
('InsuranceClaim', 'read', 'all', 'Read insurance claim', 'InsuranceClaim:read:all'),
('InsuranceClaim', 'update', 'all', 'Update insurance claim', 'InsuranceClaim:update:all'),
('FinancialReport', 'read', 'all', 'Read financial report', 'FinancialReport:read:all'),
-- HR permissions
('StaffProfile', 'create', 'all', 'Create staff profile', 'StaffProfile:create:all'),
('StaffProfile', 'read', 'all', 'Read staff profile', 'StaffProfile:read:all'),
('StaffProfile', 'update', 'all', 'Update staff profile', 'StaffProfile:update:all'),
('WorkSchedule', 'create', 'all', 'Create work schedule', 'WorkSchedule:create:all'),
('WorkSchedule', 'read', 'all', 'Read work schedule', 'WorkSchedule:read:all'),
('WorkSchedule', 'update', 'all', 'Update work schedule', 'WorkSchedule:update:all'),
('TrainingRecord', 'create', 'all', 'Create training record', 'TrainingRecord:create:all'),
('TrainingRecord', 'read', 'all', 'Read training record', 'TrainingRecord:read:all'),
('TrainingRecord', 'update', 'all', 'Update training record', 'TrainingRecord:update:all'),
('OperationReport', 'read', 'all', 'Read operation report', 'OperationReport:read:all'),
-- ITAdmin permissions
('SystemConfig', 'read', 'all', 'Read system config', 'SystemConfig:read:all'),
('SystemConfig', 'update', 'all', 'Update system config', 'SystemConfig:update:all'),
('AccessPolicy', 'read', 'all', 'Read access policy', 'AccessPolicy:read:all'),
('AccessPolicy', 'update', 'all', 'Update access policy', 'AccessPolicy:update:all'),
('AuditLog', 'read', 'all', 'Read audit log', 'AuditLog:read:all'),
-- SecurityAdmin permissions
('IncidentCase', 'create', 'all', 'Create incident case', 'IncidentCase:create:all'),
('IncidentCase', 'read', 'all', 'Read incident case', 'IncidentCase:read:all'),
('IncidentCase', 'update', 'all', 'Update incident case', 'IncidentCase:update:all')
ON CONFLICT (permission_key) DO NOTHING;

-- Insert roles
INSERT INTO roles (name, description) VALUES
('Doctor', 'Medical doctor with clinical access'),
('Nurse', 'Nurse with limited clinical access'),
('Receptionist', 'Front desk staff for patient registration'),
('Cashier', 'Finance staff for billing'),
('HR', 'Human resources staff'),
('Manager', 'Department manager with reporting access'),
('ITAdmin', 'IT administrator for system management'),
('SecurityAdmin', 'Security administrator for audit and policy')
ON CONFLICT (name) DO NOTHING;

-- Assign permissions to Doctor role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'Doctor' AND p.permission_key IN (
    'PatientProfile:read:all',
    'MedicalRecord:read:all', 'MedicalRecord:create:all', 'MedicalRecord:update:all',
    'ClinicalNote:read:all', 'ClinicalNote:create:all',
    'VitalSigns:read:all',
    'Prescription:read:all', 'Prescription:create:all', 'Prescription:update:all', 'Prescription:approve:all',
    'LabOrder:create:all', 'LabOrder:read:all',
    'LabResult:read:all',
    'ImagingOrder:create:all', 'ImagingOrder:read:all',
    'ImagingResult:read:all',
    'AdmissionRecord:read:all',
    'TransferRecord:read:all',
    'DischargeSummary:create:all', 'DischargeSummary:read:all',
    'MedicalReport:read:all'
)
ON CONFLICT DO NOTHING;

-- Assign permissions to Nurse role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'Nurse' AND p.permission_key IN (
    'PatientProfile:read:all',
    'MedicalRecord:read:all',
    'ClinicalNote:read:all',
    'VitalSigns:read:all', 'VitalSigns:create:all', 'VitalSigns:update:all',
    'LabResult:read:all',
    'ImagingResult:read:all',
    'AdmissionRecord:read:all',
    'TransferRecord:read:all',
    'DischargeSummary:read:all'
)
ON CONFLICT DO NOTHING;

-- Assign permissions to Receptionist role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'Receptionist' AND p.permission_key IN (
    'PatientProfile:create:all', 'PatientProfile:read:all', 'PatientProfile:update:all',
    'Appointment:create:all', 'Appointment:read:all', 'Appointment:update:all',
    'AdmissionRecord:create:all', 'AdmissionRecord:read:all',
    'TransferRecord:create:all', 'TransferRecord:read:all'
)
ON CONFLICT DO NOTHING;

-- Assign permissions to Cashier role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'Cashier' AND p.permission_key IN (
    'BillingRecord:create:all', 'BillingRecord:read:all', 'BillingRecord:update:all',
    'Invoice:create:all', 'Invoice:read:all', 'Invoice:update:all',
    'InsuranceClaim:create:all', 'InsuranceClaim:read:all', 'InsuranceClaim:update:all',
    'FinancialReport:read:all'
)
ON CONFLICT DO NOTHING;

-- Assign permissions to HR role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'HR' AND p.permission_key IN (
    'StaffProfile:create:all', 'StaffProfile:read:all', 'StaffProfile:update:all',
    'WorkSchedule:create:all', 'WorkSchedule:read:all', 'WorkSchedule:update:all',
    'TrainingRecord:create:all', 'TrainingRecord:read:all', 'TrainingRecord:update:all',
    'OperationReport:read:all'
)
ON CONFLICT DO NOTHING;

-- Assign permissions to Manager role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'Manager' AND p.permission_key IN (
    'MedicalReport:read:all',
    'OperationReport:read:all',
    'FinancialReport:read:all',
    'WorkSchedule:read:all',
    'StaffProfile:read:all'
)
ON CONFLICT DO NOTHING;

-- Assign permissions to ITAdmin role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ITAdmin' AND p.permission_key IN (
    'SystemConfig:read:all', 'SystemConfig:update:all',
    'AccessPolicy:read:all',
    'AuditLog:read:all'
)
ON CONFLICT DO NOTHING;

-- Assign permissions to SecurityAdmin role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SecurityAdmin' AND p.permission_key IN (
    'AuditLog:read:all',
    'IncidentCase:create:all', 'IncidentCase:read:all', 'IncidentCase:update:all',
    'AccessPolicy:read:all', 'AccessPolicy:update:all',
    'SystemConfig:read:all'
)
ON CONFLICT DO NOTHING;

