# 🏥 EMR Auth Service - RBAC/ABAC Authorization

Hệ thống xác thực và phân quyền cho **Electronic Medical Record (EMR)** sử dụng Spring Boot với JWT, PostgreSQL, Flyway Migration, RBAC (Role-Based Access Control) và ABAC (Attribute-Based Access Control).

## 📋 Mục lục

- [Tính năng](#-tính-năng)
- [Kiến trúc](#-kiến-trúc)
- [Cài đặt](#-cài-đặt)
- [API Endpoints](#-api-endpoints)
- [Mock APIs](#-mock-apis)
- [RBAC - Roles & Permissions](#-rbac---roles--permissions)
- [ABAC Rules](#-abac-rules)
- [Test API](#-test-api)
- [Tài khoản mẫu](#-tài-khoản-mẫu)

---

## ✨ Tính năng

| Tính năng | Mô tả |
|-----------|-------|
| 🔐 **JWT Authentication** | Đăng nhập và xác thực bằng JWT token |
| 👥 **RBAC** | Phân quyền dựa trên 8 vai trò (Doctor, Nurse, Receptionist, Cashier, HR, Manager, ITAdmin, SecurityAdmin) |
| 🎯 **ABAC** | Phân quyền dựa trên thuộc tính (branch, department, assigned patients) |
| 🗄️ **PostgreSQL + Flyway** | Cơ sở dữ liệu với quản lý migration tự động |
| 📝 **Audit Logging** | Ghi log tất cả các quyết định phân quyền |
| ⚠️ **Risk Scoring** | Đánh giá rủi ro cho mỗi request |
| 📋 **Obligations** | Các yêu cầu bổ sung (MFA, masking, rate limiting) |
| 🔒 **Separation of Duties** | Creator không thể approve chính mình |
| 🏥 **Mock EMR APIs** | 13+ mock controllers cho hệ thống EMR hoàn chỉnh |

---

## 🏗️ Kiến trúc

```
src/main/java/com/auth/auth_service/
├── config/
│   ├── SecurityConfig.java          # Cấu hình Spring Security
│   └── DataLoader.java              # Kiểm tra data sau migration
├── controller/
│   ├── AuthController.java          # API xác thực
│   ├── AuthorizationController.java # API phân quyền
│   ├── AuditController.java         # API audit log
│   ├── UserController.java          # API quản lý user
│   └── mockapi/                     # 📁 Mock EMR APIs
│       ├── PatientController.java
│       ├── MedicalRecordController.java
│       ├── ClinicalController.java
│       ├── PrescriptionController.java
│       ├── LabController.java
│       ├── ImagingController.java
│       ├── AdmissionController.java
│       ├── AppointmentController.java
│       ├── BillingController.java
│       ├── StaffController.java
│       ├── ReportController.java
│       ├── SystemController.java
│       └── IncidentController.java
├── dto/
│   ├── ApiResponse.java
│   ├── AuthorizationRequest.java
│   ├── AuthorizationResponse.java
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── UserDto.java
│   └── mock/                        # 📁 Mock DTOs (18 files)
├── entity/                          # JPA Entities
├── exception/                       # Exception Handlers
├── repository/                      # JPA Repositories
├── security/                        # JWT & Security
└── service/                         # Business Logic
```

---

## 🚀 Cài đặt

### Yêu cầu
- Java 21+
- PostgreSQL 13+
- Maven 3.6+

### 1. Cài đặt PostgreSQL

```bash
# Sử dụng Docker (khuyến nghị)
docker run -d \
  --name postgres-auth \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=auth_rbac_db \
  -p 5432:5432 \
  postgres:15

# Hoặc tạo database thủ công
psql -U postgres -c "CREATE DATABASE auth_rbac_db;"
```

### 2. Cấu hình (tùy chọn)

Cập nhật `src/main/resources/application.properties` nếu cần:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/auth_rbac_db
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### 3. Build và chạy

```bash
# Build project
./mvnw clean package -DskipTests

# Chạy ứng dụng (Flyway tự động chạy migrations)
./mvnw spring-boot:run
```

Ứng dụng chạy tại: **http://localhost:8080**

### 4. Docker Compose (Full stack)

```bash
docker-compose up -d
```

---

## 🔌 API Endpoints

### 🔐 Authentication (`/api/auth`)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/auth/login` | Đăng nhập, nhận JWT token |
| GET | `/api/auth/me` | Lấy thông tin user hiện tại |
| POST | `/api/auth/logout` | Đăng xuất |
| POST | `/api/auth/refresh` | Refresh token |

### 🛡️ Authorization (`/api/authz`)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/authz/check` | Kiểm tra quyền truy cập (RBAC + ABAC) |
| GET | `/api/authz/permission` | Kiểm tra nhanh quyền (RBAC only) |
| POST | `/api/authz/check-batch` | Kiểm tra nhiều quyền cùng lúc |

### 📊 Audit (`/api/audit`)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/audit` | Lấy danh sách audit log |
| GET | `/api/audit/user/{userId}` | Lấy log theo user |
| GET | `/api/audit/high-risk` | Lấy các action có rủi ro cao |
| GET | `/api/audit/denied` | Lấy các request bị từ chối |

### 👤 Users (`/api/users`)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/users` | Lấy danh sách users |
| GET | `/api/users/{userId}` | Lấy thông tin user |
| GET | `/api/users/department/{dept}` | Lấy users theo phòng ban |
| GET | `/api/users/branch/{branch}` | Lấy users theo chi nhánh |

---

## 🏥 Mock APIs

### Tổng quan Mock EMR APIs

Các Mock API mô phỏng hệ thống EMR hoàn chỉnh với phân quyền RBAC/ABAC.

| Controller | Base Path | Resource Types | Roles được phép |
|------------|-----------|----------------|-----------------|
| **PatientController** | `/api/mock/patients` | PatientProfile | Doctor, Nurse (read), Receptionist (CRUD) |
| **MedicalRecordController** | `/api/mock/medical-records` | MedicalRecord | Doctor (CRUD), Nurse (read) |
| **ClinicalController** | `/api/mock/clinical` | ClinicalNote, VitalSigns | Doctor, Nurse |
| **PrescriptionController** | `/api/mock/prescriptions` | Prescription | Doctor (CRUD + approve) |
| **LabController** | `/api/mock/lab` | LabOrder, LabResult | Doctor (order), Doctor/Nurse (read results) |
| **ImagingController** | `/api/mock/imaging` | ImagingOrder, ImagingResult | Doctor (order), Doctor/Nurse (read results) |
| **AdmissionController** | `/api/mock/admissions` | Admission, Transfer, Discharge | Receptionist, Doctor, Nurse |
| **AppointmentController** | `/api/mock/appointments` | Appointment | Receptionist (CRUD) |
| **BillingController** | `/api/mock/billing` | Billing, Invoice, InsuranceClaim | Cashier (CRUD), Manager (read reports) |
| **StaffController** | `/api/mock/staff` | StaffProfile, WorkSchedule, Training | HR (CRUD), Manager (read) |
| **ReportController** | `/api/mock/reports` | MedicalReport | Doctor, Manager (read) |
| **SystemController** | `/api/mock/system` | SystemConfig, AccessPolicy, AuditLog | ITAdmin, SecurityAdmin |
| **IncidentController** | `/api/mock/incidents` | IncidentCase | SecurityAdmin (CRUD) |

### Chi tiết Mock API Endpoints

#### 👤 Patient Management
```
GET    /api/mock/patients                 # Lấy danh sách bệnh nhân
GET    /api/mock/patients/{id}            # Lấy thông tin bệnh nhân
POST   /api/mock/patients                 # Tạo bệnh nhân mới
PUT    /api/mock/patients/{id}            # Cập nhật bệnh nhân
DELETE /api/mock/patients/{id}            # Xóa bệnh nhân (bị chặn bởi policy)
```

#### 📋 Medical Records
```
GET    /api/mock/medical-records          # Lấy danh sách hồ sơ bệnh án
GET    /api/mock/medical-records/{id}     # Lấy chi tiết hồ sơ
POST   /api/mock/medical-records          # Tạo hồ sơ mới
PUT    /api/mock/medical-records/{id}     # Cập nhật hồ sơ
POST   /api/mock/medical-records/{id}/export  # Export hồ sơ (cần approval)
```

#### 🩺 Clinical Notes & Vital Signs
```
GET    /api/mock/clinical/notes           # Danh sách ghi chú lâm sàng
POST   /api/mock/clinical/notes           # Tạo ghi chú mới
GET    /api/mock/clinical/vitals          # Danh sách sinh hiệu
POST   /api/mock/clinical/vitals          # Ghi nhận sinh hiệu
PUT    /api/mock/clinical/vitals/{id}     # Cập nhật sinh hiệu
```

#### 💊 Prescriptions
```
GET    /api/mock/prescriptions            # Danh sách đơn thuốc
POST   /api/mock/prescriptions            # Tạo đơn thuốc
PUT    /api/mock/prescriptions/{id}       # Cập nhật đơn thuốc
POST   /api/mock/prescriptions/{id}/approve  # Phê duyệt đơn thuốc (SoD applied)
```

#### 🧪 Lab Orders & Results
```
GET    /api/mock/lab/orders               # Danh sách yêu cầu xét nghiệm
POST   /api/mock/lab/orders               # Tạo yêu cầu xét nghiệm
GET    /api/mock/lab/results              # Danh sách kết quả
GET    /api/mock/lab/results/{id}         # Chi tiết kết quả
```

#### 🏥 Imaging Orders & Results
```
GET    /api/mock/imaging/orders           # Danh sách yêu cầu CĐHA
POST   /api/mock/imaging/orders           # Tạo yêu cầu CĐHA
GET    /api/mock/imaging/results          # Danh sách kết quả CĐHA
GET    /api/mock/imaging/results/{id}     # Chi tiết kết quả
```

#### 🛏️ Admissions, Transfers & Discharges
```
GET    /api/mock/admissions               # Danh sách nhập viện
POST   /api/mock/admissions               # Tạo bản ghi nhập viện
GET    /api/mock/admissions/transfers     # Danh sách chuyển khoa
POST   /api/mock/admissions/transfers     # Tạo yêu cầu chuyển khoa
GET    /api/mock/admissions/discharge-summaries  # Danh sách xuất viện
POST   /api/mock/admissions/discharge-summaries  # Tạo tóm tắt xuất viện
```

#### 📅 Appointments
```
GET    /api/mock/appointments             # Danh sách lịch hẹn
POST   /api/mock/appointments             # Tạo lịch hẹn
PUT    /api/mock/appointments/{id}        # Cập nhật lịch hẹn
POST   /api/mock/appointments/{id}/check-in   # Check-in bệnh nhân
POST   /api/mock/appointments/{id}/cancel     # Hủy lịch hẹn
```

#### 💰 Billing, Invoices & Insurance
```
GET    /api/mock/billing/records          # Danh sách hóa đơn
POST   /api/mock/billing/records          # Tạo hóa đơn
GET    /api/mock/billing/invoices         # Danh sách invoice
POST   /api/mock/billing/invoices/{id}/approve  # Phê duyệt invoice (SoD)
GET    /api/mock/billing/claims           # Danh sách claims bảo hiểm
GET    /api/mock/billing/reports/financial     # Báo cáo tài chính
```

#### 👨‍💼 Staff Management (HR)
```
GET    /api/mock/staff/profiles           # Danh sách nhân viên
POST   /api/mock/staff/profiles           # Tạo hồ sơ nhân viên
GET    /api/mock/staff/schedules          # Lịch làm việc
POST   /api/mock/staff/schedules          # Tạo lịch làm việc
GET    /api/mock/staff/training           # Hồ sơ đào tạo
GET    /api/mock/staff/reports/operation  # Báo cáo vận hành
```

#### 📊 Reports
```
GET    /api/mock/reports/medical          # Báo cáo y tế
GET    /api/mock/reports/medical/{id}     # Chi tiết báo cáo
GET    /api/mock/reports/summary          # Tóm tắt quyền truy cập báo cáo
```

#### ⚙️ System Configuration (IT/Security Admin)
```
GET    /api/mock/system/config            # Danh sách cấu hình
PUT    /api/mock/system/config/{id}       # Cập nhật cấu hình
GET    /api/mock/system/policies          # Danh sách access policies
PUT    /api/mock/system/policies/{id}     # Cập nhật policy
GET    /api/mock/system/audit-logs        # Xem audit logs
GET    /api/mock/system/audit-logs/stats  # Thống kê audit
```

#### 🚨 Incident Management (Security Admin)
```
GET    /api/mock/incidents                # Danh sách sự cố
POST   /api/mock/incidents                # Báo cáo sự cố mới
PUT    /api/mock/incidents/{id}           # Cập nhật sự cố
POST   /api/mock/incidents/{id}/assign    # Phân công xử lý
POST   /api/mock/incidents/{id}/resolve   # Đóng sự cố
GET    /api/mock/incidents/stats          # Thống kê sự cố
```

---

## 👥 RBAC - Roles & Permissions

### Ma trận phân quyền

| Resource | Doctor | Nurse | Receptionist | Cashier | HR | Manager | ITAdmin | SecurityAdmin |
|----------|:------:|:-----:|:------------:|:-------:|:--:|:-------:|:-------:|:-------------:|
| PatientProfile | R | R | CRU | ❌ | ❌ | ❌ | ❌ | ❌ |
| MedicalRecord | CRU | R | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| ClinicalNote | CR | R | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| VitalSigns | R | CRU | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Prescription | CRUA | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| LabOrder | CR | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| LabResult | R | R | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Appointment | ❌ | ❌ | CRU | ❌ | ❌ | ❌ | ❌ | ❌ |
| BillingRecord | ❌ | ❌ | ❌ | CRU | ❌ | ❌ | ❌ | ❌ |
| Invoice | ❌ | ❌ | ❌ | CRU | ❌ | ❌ | ❌ | ❌ |
| StaffProfile | ❌ | ❌ | ❌ | ❌ | CRU | R | ❌ | ❌ |
| WorkSchedule | ❌ | ❌ | ❌ | ❌ | CRU | R | ❌ | ❌ |
| MedicalReport | R | ❌ | ❌ | ❌ | ❌ | R | ❌ | ❌ |
| FinancialReport | ❌ | ❌ | ❌ | R | ❌ | R | ❌ | ❌ |
| OperationReport | ❌ | ❌ | ❌ | ❌ | R | R | ❌ | ❌ |
| SystemConfig | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | RU | R |
| AccessPolicy | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | R | RU |
| AuditLog | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | R | R |
| IncidentCase | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | CRU |

> **Legend**: C=Create, R=Read, U=Update, A=Approve, ❌=No Access

---

## 🎯 ABAC Rules

### Điều kiện kiểm tra bổ sung

| Rule | Mô tả | Áp dụng cho |
|------|-------|-------------|
| **Branch Match** | User chỉ truy cập tài nguyên trong cùng chi nhánh | Doctor, Nurse, Receptionist, Cashier, HR |
| **Patient Assignment** | Chỉ truy cập hồ sơ bệnh nhân được giao | Doctor, Nurse |
| **Department Scope** | Manager chỉ xem nhân viên trong phòng ban mình | Manager |
| **No Delete Patient Data** | Không ai được xóa dữ liệu bệnh nhân | Tất cả |
| **Export Approval** | Export MedicalRecord cần approval hoặc emergency mode | Tất cả |
| **SoD - Creator Cannot Approve** | Người tạo không thể approve | Invoice, Prescription, InsuranceClaim |

### Deny Rules

| Code | Mô tả |
|------|-------|
| `RECEPTIONIST_NO_CLINICAL_ACCESS` | Receptionist không được truy cập clinical data |
| `CASHIER_NO_CLINICAL_ACCESS` | Cashier không được truy cập clinical data |
| `HR_NO_PATIENT_OR_FINANCE_ACCESS` | HR không được truy cập patient/finance data |
| `ITADMIN_NO_PATIENT_DATA` | ITAdmin không được truy cập patient data |
| `NO_DELETE_PATIENT_DATA` | Không ai được xóa patient data |
| `BRANCH_MISMATCH` | User không thể truy cập tài nguyên khác chi nhánh |
| `SOD_CREATOR_CANNOT_APPROVE` | Creator không thể approve chính record mình tạo |
| `EXPORT_REQUIRES_APPROVAL_OR_EMERGENCY` | Export cần được phê duyệt |

---

## 🧪 Test API

### 1. Đăng nhập

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "u0000", "password": "password123"}'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "userId": "U0000",
    "username": "u0000",
    "role": "Doctor",
    "department": "Khoa_Noi",
    "branch": "CN_HN"
  }
}
```

### 2. Kiểm tra quyền (RBAC + ABAC)

```bash
curl -X POST http://localhost:8080/api/authz/check \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your_token>" \
  -d '{
    "resourceType": "MedicalRecord",
    "action": "read",
    "resourceBranch": "CN_HN",
    "patientId": "P00001"
  }'
```

**Response (Allowed):**
```json
{
  "success": true,
  "data": {
    "allowed": true,
    "policyId": "ALLOW_Doctor_MedicalRecord_read",
    "denyReasons": [],
    "obligations": [],
    "riskScore": 3
  }
}
```

**Response (Denied):**
```json
{
  "success": true,
  "data": {
    "allowed": false,
    "policyId": "DENY_BRANCH_MISMATCH",
    "denyReasons": ["BRANCH_MISMATCH"],
    "obligations": [],
    "riskScore": 3
  }
}
```

### 3. Gọi Mock API (với phân quyền)

```bash
# Lấy danh sách bệnh nhân (Doctor role)
curl -X GET http://localhost:8080/api/mock/patients \
  -H "Authorization: Bearer <doctor_token>"

# Tạo lịch hẹn (Receptionist role)
curl -X POST http://localhost:8080/api/mock/appointments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <receptionist_token>" \
  -d '{
    "patientId": "PAT001",
    "doctorId": "DOC001",
    "appointmentType": "Consultation",
    "reason": "Khám định kỳ",
    "scheduledAt": "2026-01-15T09:00:00",
    "durationMinutes": 30,
    "department": "Internal Medicine"
  }'

# Xem audit logs (ITAdmin/SecurityAdmin role)
curl -X GET http://localhost:8080/api/mock/system/audit-logs \
  -H "Authorization: Bearer <admin_token>"
```

---

## 👤 Tài khoản mẫu

### Thông tin đăng nhập

| Username | Password | Role | Branch | Department |
|----------|----------|------|--------|------------|
| `admin` | `admin123` | SecurityAdmin | CN_HN | Security |
| `u0000` | `password123` | Doctor | CN_HN | Khoa_Noi |
| `u0001` | `password123` | Doctor | CN_HN | Khoa_Ngoai |
| `u0002` | `password123` | Doctor | CN_HCM | Khoa_Noi |
| `u0003` | `password123` | Nurse | CN_HN | Khoa_Noi |
| `u0004` | `password123` | Nurse | CN_HN | Khoa_Ngoai |
| `u0005` | `password123` | Nurse | CN_HCM | Khoa_Noi |
| `u0006` | `password123` | Receptionist | CN_HN | Phong_TiepDon |
| `u0007` | `password123` | Receptionist | CN_HCM | Phong_TiepDon |
| `u0008` | `password123` | Cashier | CN_HN | Phong_TaiChinh |
| `u0009` | `password123` | Cashier | CN_HCM | Phong_TaiChinh |
| `u0010` | `password123` | HR | CN_HN | Phong_NhanSu |
| `u0011` | `password123` | HR | CN_HCM | Phong_NhanSu |
| `u0012` | `password123` | Manager | CN_HN | Khoa_Noi |
| `u0013` | `password123` | Manager | CN_HCM | Khoa_Noi |
| `u0014` | `password123` | ITAdmin | CN_HN | IT |

### Patient Assignments (ABAC)

| User | Role | Assigned Patients |
|------|------|-------------------|
| U0000 | Doctor | P00001, P00002, P00003, P00004, P00005, P00010, P00011 |
| U0001 | Doctor | P00006, P00007, P00008, P00009, P00012, P00013 |
| U0003 | Nurse | P00001, P00002, P00003, P00004, P00005 |
| U0004 | Nurse | P00006, P00007, P00008, P00009 |

---

## ⚠️ Risk Scoring

| Factor | Score |
|--------|:-----:|
| Off-hours (before 8 AM or after 6 PM) | +2 |
| Export action | +3 |
| High sensitivity resource | +2 |
| High-risk resources (MedicalRecord, AuditLog, SystemConfig) | +3 |
| High-risk actions (export, delete) | +2 |

---

## 📋 Obligations

Khi request được allow, hệ thống có thể yêu cầu thêm các obligations:

| Obligation | Condition | Mô tả |
|------------|-----------|-------|
| `step_up_mfa` | Off-hours access | Yêu cầu MFA bổ sung |
| `mask_fields` | Non-Receptionist đọc PatientProfile | Mask national_id, address |
| `log_high_risk` | High-risk actions | Ghi log chi tiết |
| `require_approval_ref` | Export action | Yêu cầu approval ticket |
| `rate_limit` | Bulk access | Giới hạn 60 requests/minute |

---

## 📄 Flyway Migrations

| Version | File | Mô tả |
|---------|------|-------|
| V1 | `create_tables.sql` | Tạo các bảng: users, roles, permissions, audit_logs |
| V2 | `create_indexes.sql` | Tạo indexes cho performance |
| V3 | `seed_roles_permissions.sql` | Seed 8 roles và 60+ permissions |
| V4 | `seed_users.sql` | Seed 16+ users mẫu với patient assignments |

---

## 📝 License

MIT License - Xem file [LICENSE](LICENSE) để biết thêm chi tiết.
