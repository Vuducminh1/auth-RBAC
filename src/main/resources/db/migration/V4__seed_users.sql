-- =====================================================
-- V4: Seed sample users data
-- Password: password123 (BCrypt encoded)
-- Admin password: admin123 (BCrypt encoded)
-- =====================================================

-- BCrypt hash for 'password123' = $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qUgTZrB7nJLBwj3qLy
-- BCrypt hash for 'admin123' = $2a$10$EqKcp1WFKrX3qSCj0VVzS.xM8YxvTc5f6NRqfD2B/kBJfFEqj0qmi

-- Insert sample users (Doctors)
INSERT INTO users (user_id, username, password, email, department, branch, position, has_license, seniority, employment_type, role_id)
SELECT 'U0000', 'u0000', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qUgTZrB7nJLBwj3qLy', 'u0000@hospital.com', 'Khoa_Noi', 'CN_HN', 'Doctor', true, 'Senior', 'FullTime', r.id
FROM roles r WHERE r.name = 'Doctor'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO users (user_id, username, password, email, department, branch, position, has_license, seniority, employment_type, role_id)
SELECT 'U0001', 'u0001', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qUgTZrB7nJLBwj3qLy', 'u0001@hospital.com', 'Khoa_Ngoai', 'CN_HN', 'Doctor', true, 'Mid', 'FullTime', r.id
FROM roles r WHERE r.name = 'Doctor'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO users (user_id, username, password, email, department, branch, position, has_license, seniority, employment_type, role_id)
SELECT 'U0002', 'u0002', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qUgTZrB7nJLBwj3qLy', 'u0002@hospital.com', 'Khoa_Noi', 'CN_HCM', 'Doctor', true, 'Junior', 'FullTime', r.id
FROM roles r WHERE r.name = 'Doctor'
ON CONFLICT (user_id) DO NOTHING;

-- Insert sample users (Nurses)
INSERT INTO users (user_id, username, password, email, department, branch, position, has_license, seniority, employment_type, role_id)
SELECT 'U0003', 'u0003', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qUgTZrB7nJLBwj3qLy', 'u0003@hospital.com', 'Khoa_Noi', 'CN_HN', 'Nurse', true, 'Senior', 'FullTime', r.id
FROM roles r WHERE r.name = 'Nurse'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO users (user_id, username, password, email, department, branch, position, has_license, seniority, employment_type, role_id)
SELECT 'U0004', 'u0004', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qUgTZrB7nJLBwj3qLy', 'u0004@hospital.com', 'Khoa_Ngoai', 'CN_HN', 'Nurse', true, 'Mid', 'FullTime', r.id
FROM roles r WHERE r.name = 'Nurse'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO users (user_id, username, password, email, department, branch, position, has_license, seniority, employment_type, role_id)
SELECT 'U0005', 'u0005', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qUgTZrB7nJLBwj3qLy', 'u0005@hospital.com', 'Khoa_Noi', 'CN_HCM', 'Nurse', true, 'Junior', 'FullTime', r.id
FROM roles r WHERE r.name = 'Nurse'
ON CONFLICT (user_id) DO NOTHING;

-- Insert sample users (Receptionists)
INSERT INTO users (user_id, username, password, email, department, branch, position, has_license, seniority, employment_type, role_id)
SELECT 'U0006', 'u0006', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qUgTZrB7nJLBwj3qLy', 'u0006@hospital.com', 'Phong_TiepDon', 'CN_HN', 'Receptionist', false, 'Senior', 'FullTime', r.id
FROM roles r WHERE r.name = 'Receptionist'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO users (user_id, username, password, email, department, branch, position, has_license, seniority, employment_type, role_id)
SELECT 'U0007', 'u0007', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qUgTZrB7nJLBwj3qLy', 'u0007@hospital.com', 'Phong_TiepDon', 'CN_HCM', 'Receptionist', false, 'Mid', 'FullTime', r.id
FROM roles r WHERE r.name = 'Receptionist'
ON CONFLICT (user_id) DO NOTHING;

-- Insert sample users (Cashiers)
INSERT INTO users (user_id, username, password, email, department, branch, position, has_license, seniority, employment_type, role_id)
SELECT 'U0008', 'u0008', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qUgTZrB7nJLBwj3qLy', 'u0008@hospital.com', 'Phong_TaiChinh', 'CN_HN', 'Cashier', false, 'Senior', 'FullTime', r.id
FROM roles r WHERE r.name = 'Cashier'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO users (user_id, username, password, email, department, branch, position, has_license, seniority, employment_type, role_id)
SELECT 'U0009', 'u0009', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qUgTZrB7nJLBwj3qLy', 'u0009@hospital.com', 'Phong_TaiChinh', 'CN_HCM', 'Cashier', false, 'Mid', 'FullTime', r.id
FROM roles r WHERE r.name = 'Cashier'
ON CONFLICT (user_id) DO NOTHING;

-- Insert sample users (HR)
INSERT INTO users (user_id, username, password, email, department, branch, position, has_license, seniority, employment_type, role_id)
SELECT 'U0010', 'u0010', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qUgTZrB7nJLBwj3qLy', 'u0010@hospital.com', 'Phong_NhanSu', 'CN_HN', 'HR', false, 'Senior', 'FullTime', r.id
FROM roles r WHERE r.name = 'HR'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO users (user_id, username, password, email, department, branch, position, has_license, seniority, employment_type, role_id)
SELECT 'U0011', 'u0011', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qUgTZrB7nJLBwj3qLy', 'u0011@hospital.com', 'Phong_NhanSu', 'CN_HCM', 'HR', false, 'Mid', 'FullTime', r.id
FROM roles r WHERE r.name = 'HR'
ON CONFLICT (user_id) DO NOTHING;

-- Insert sample users (Managers)
INSERT INTO users (user_id, username, password, email, department, branch, position, has_license, seniority, employment_type, role_id)
SELECT 'U0012', 'u0012', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qUgTZrB7nJLBwj3qLy', 'u0012@hospital.com', 'Khoa_Noi', 'CN_HN', 'Manager', false, 'Senior', 'FullTime', r.id
FROM roles r WHERE r.name = 'Manager'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO users (user_id, username, password, email, department, branch, position, has_license, seniority, employment_type, role_id)
SELECT 'U0013', 'u0013', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qUgTZrB7nJLBwj3qLy', 'u0013@hospital.com', 'Khoa_Ngoai', 'CN_HCM', 'Manager', false, 'Senior', 'FullTime', r.id
FROM roles r WHERE r.name = 'Manager'
ON CONFLICT (user_id) DO NOTHING;

-- Insert sample users (ITAdmin)
INSERT INTO users (user_id, username, password, email, department, branch, position, has_license, seniority, employment_type, role_id)
SELECT 'U0014', 'u0014', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qUgTZrB7nJLBwj3qLy', 'u0014@hospital.com', 'IT', 'CN_HN', 'ITAdmin', false, 'Senior', 'FullTime', r.id
FROM roles r WHERE r.name = 'ITAdmin'
ON CONFLICT (user_id) DO NOTHING;

-- Insert sample users (SecurityAdmin)
INSERT INTO users (user_id, username, password, email, department, branch, position, has_license, seniority, employment_type, role_id)
SELECT 'U0015', 'u0015', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.qUgTZrB7nJLBwj3qLy', 'u0015@hospital.com', 'Security', 'CN_HN', 'SecurityAdmin', false, 'Senior', 'FullTime', r.id
FROM roles r WHERE r.name = 'SecurityAdmin'
ON CONFLICT (user_id) DO NOTHING;

-- Insert admin user
INSERT INTO users (user_id, username, password, email, department, branch, position, has_license, seniority, employment_type, role_id)
SELECT 'ADMIN001', 'admin', '$2a$10$EqKcp1WFKrX3qSCj0VVzS.xM8YxvTc5f6NRqfD2B/kBJfFEqj0qmi', 'admin@hospital.com', 'Security', 'CN_HN', 'SecurityAdmin', false, 'Senior', 'FullTime', r.id
FROM roles r WHERE r.name = 'SecurityAdmin'
ON CONFLICT (user_id) DO NOTHING;

-- Assign patients to Doctors and Nurses (for ABAC testing)
-- Doctor U0000 (CN_HN, Khoa_Noi)
INSERT INTO user_assigned_patients (user_id, patient_id)
SELECT u.id, p.patient_id FROM users u, 
(SELECT unnest(ARRAY['P00001', 'P00002', 'P00003', 'P00004', 'P00005', 'P00010', 'P00011']) as patient_id) p
WHERE u.user_id = 'U0000'
ON CONFLICT DO NOTHING;

-- Doctor U0001 (CN_HN, Khoa_Ngoai)
INSERT INTO user_assigned_patients (user_id, patient_id)
SELECT u.id, p.patient_id FROM users u, 
(SELECT unnest(ARRAY['P00006', 'P00007', 'P00008', 'P00009', 'P00012', 'P00013']) as patient_id) p
WHERE u.user_id = 'U0001'
ON CONFLICT DO NOTHING;

-- Doctor U0002 (CN_HCM, Khoa_Noi)
INSERT INTO user_assigned_patients (user_id, patient_id)
SELECT u.id, p.patient_id FROM users u, 
(SELECT unnest(ARRAY['P00014', 'P00015', 'P00016', 'P00017', 'P00018']) as patient_id) p
WHERE u.user_id = 'U0002'
ON CONFLICT DO NOTHING;

-- Nurse U0003 (CN_HN, Khoa_Noi) - same patients as Doctor U0000
INSERT INTO user_assigned_patients (user_id, patient_id)
SELECT u.id, p.patient_id FROM users u, 
(SELECT unnest(ARRAY['P00001', 'P00002', 'P00003', 'P00004', 'P00005']) as patient_id) p
WHERE u.user_id = 'U0003'
ON CONFLICT DO NOTHING;

-- Nurse U0004 (CN_HN, Khoa_Ngoai) - same patients as Doctor U0001
INSERT INTO user_assigned_patients (user_id, patient_id)
SELECT u.id, p.patient_id FROM users u, 
(SELECT unnest(ARRAY['P00006', 'P00007', 'P00008', 'P00009']) as patient_id) p
WHERE u.user_id = 'U0004'
ON CONFLICT DO NOTHING;

-- Nurse U0005 (CN_HCM, Khoa_Noi) - same patients as Doctor U0002
INSERT INTO user_assigned_patients (user_id, patient_id)
SELECT u.id, p.patient_id FROM users u, 
(SELECT unnest(ARRAY['P00014', 'P00015', 'P00016', 'P00017']) as patient_id) p
WHERE u.user_id = 'U0005'
ON CONFLICT DO NOTHING;

