# Auth Service - RBAC/ABAC Authorization

Hệ thống xác thực và phân quyền sử dụng Spring Boot với JWT, PostgreSQL, Flyway Migration, RBAC (Role-Based Access Control) và ABAC (Attribute-Based Access Control).

## Tính năng

- **Xác thực JWT**: Đăng nhập và xác thực bằng JWT token
- **RBAC**: Phân quyền dựa trên vai trò (Doctor, Nurse, Receptionist, Cashier, HR, Manager, ITAdmin, SecurityAdmin)
- **ABAC**: Phân quyền dựa trên thuộc tính (branch, department, assigned patients)
- **PostgreSQL**: Cơ sở dữ liệu PostgreSQL
- **Flyway Migration**: Quản lý schema và data migration
- **Audit Logging**: Ghi log tất cả các quyết định phân quyền
- **Risk Scoring**: Đánh giá rủi ro cho mỗi request
- **Obligations**: Các yêu cầu bổ sung (MFA, masking, rate limiting)

## Cấu trúc project

```
src/main/
├── java/com/auth/auth_service/
│   ├── config/
│   │   ├── SecurityConfig.java      # Cấu hình Spring Security
│   │   └── DataLoader.java          # Kiểm tra data sau migration
│   ├── controller/
│   │   ├── AuthController.java      # API xác thực
│   │   ├── AuthorizationController.java  # API phân quyền
│   │   ├── AuditController.java     # API audit log
│   │   └── UserController.java      # API quản lý user
│   ├── dto/                         # Data Transfer Objects
│   ├── entity/                      # JPA Entities
│   ├── exception/                   # Exception Handlers
│   ├── repository/                  # JPA Repositories
│   ├── security/                    # JWT & Security components
│   └── service/                     # Business Logic
└── resources/
    ├── application.properties       # Cấu hình ứng dụng
    └── db/migration/                # Flyway migrations
        ├── V1__create_tables.sql
        ├── V2__create_indexes.sql
        ├── V3__seed_roles_permissions.sql
        └── V4__seed_users.sql
```

## Yêu cầu

- Java 11+
- PostgreSQL 13+
- Maven 3.6+

## Cài đặt PostgreSQL

### Windows
```bash
# Download và cài đặt từ https://www.postgresql.org/download/windows/
# Hoặc dùng Docker:
docker run -d --name postgres-auth -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=auth_rbac_db -p 5432:5432 postgres:15
```

### Tạo database
```sql
CREATE DATABASE auth_rbac_db;
```

## Cấu hình

Cập nhật `application.properties` nếu cần:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/auth_rbac_db
spring.datasource.username=postgres
spring.datasource.password=postgres
```

## Chạy ứng dụng

```bash
# Build project
./mvnw clean package -DskipTests

# Chạy ứng dụng (Flyway sẽ tự động chạy migrations)
./mvnw spring-boot:run
```

Ứng dụng sẽ chạy tại `http://localhost:8080`

## Flyway Migrations

Các file migration được chạy tự động khi khởi động ứng dụng:

| Version | File | Mô tả |
|---------|------|-------|
| V1 | create_tables.sql | Tạo các bảng: users, roles, permissions, audit_logs |
| V2 | create_indexes.sql | Tạo indexes cho performance |
| V3 | seed_roles_permissions.sql | Seed 8 roles và 60+ permissions |
| V4 | seed_users.sql | Seed 16+ users mẫu với patient assignments |

## API Endpoints

### Authentication
- `POST /api/auth/login` - Đăng nhập
- `GET /api/auth/me` - Lấy thông tin user hiện tại
- `POST /api/auth/logout` - Đăng xuất

### Authorization (RBAC + ABAC)
- `POST /api/authz/check` - Kiểm tra quyền truy cập (full RBAC + ABAC)
- `GET /api/authz/permission?resourceType=X&action=Y` - Kiểm tra nhanh quyền
- `POST /api/authz/check-batch` - Kiểm tra nhiều quyền cùng lúc

### Audit
- `GET /api/audit` - Lấy danh sách audit log
- `GET /api/audit/user/{userId}` - Lấy log theo user
- `GET /api/audit/high-risk` - Lấy các action có rủi ro cao
- `GET /api/audit/denied` - Lấy các request bị từ chối

### Users
- `GET /api/users` - Lấy danh sách users
- `GET /api/users/{userId}` - Lấy thông tin user
- `GET /api/users/department/{department}` - Lấy users theo phòng ban
- `GET /api/users/branch/{branch}` - Lấy users theo chi nhánh

## Test API

### 1. Đăng nhập
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "u0000", "password": "password123"}'
```

Response:
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

Response (Allowed):
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

Response (Denied - Branch mismatch):
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

## RBAC - Vai trò và quyền

| Role | Resources | Actions |
|------|-----------|---------|
| Doctor | MedicalRecord, Prescription, LabOrder, LabResult, ImagingOrder, ImagingResult, DischargeSummary... | read, create, update, approve |
| Nurse | VitalSigns, MedicalRecord, ClinicalNote, LabResult... | read, create, update |
| Receptionist | PatientProfile, Appointment, AdmissionRecord, TransferRecord | create, read, update |
| Cashier | BillingRecord, Invoice, InsuranceClaim, FinancialReport | create, read, update |
| HR | StaffProfile, WorkSchedule, TrainingRecord, OperationReport | create, read, update |
| Manager | Reports, StaffProfile, WorkSchedule | read |
| ITAdmin | SystemConfig, AccessPolicy, AuditLog | read, update |
| SecurityAdmin | AuditLog, IncidentCase, AccessPolicy | read, create, update |

## ABAC Rules

1. **Branch mismatch**: Users chỉ có thể truy cập tài nguyên trong cùng chi nhánh (CN_HN, CN_HCM)
2. **Patient assignment**: Doctors/Nurses chỉ có thể truy cập hồ sơ bệnh nhân được giao
3. **Department scope**: Một số tài nguyên chỉ giới hạn trong phòng ban
4. **No delete patient data**: Không ai được xóa dữ liệu bệnh nhân
5. **Export controls**: Export MedicalRecord cần approval hoặc emergency mode
6. **Segregation of Duties**: Creator không thể approve (Invoice, Prescription...)

## Deny Rules

| Rule | Description |
|------|-------------|
| RECEPTIONIST_NO_CLINICAL_ACCESS | Receptionist không được truy cập clinical data |
| CASHIER_NO_CLINICAL_ACCESS | Cashier không được truy cập clinical data |
| HR_NO_PATIENT_OR_FINANCE_ACCESS | HR không được truy cập patient/finance data |
| ITADMIN_NO_PATIENT_DATA | ITAdmin không được truy cập patient data |
| NO_DELETE_PATIENT_DATA | Không ai được xóa patient data |
| BRANCH_MISMATCH | User không thể truy cập tài nguyên khác chi nhánh |
| SOD_CREATOR_CANNOT_APPROVE | Creator không thể approve chính record mình tạo |

## Tài khoản mẫu

| Username | Password | Role | Branch | Department |
|----------|----------|------|--------|------------|
| `admin` | `admin123` | SecurityAdmin | CN_HN | Security |
| `u0000` | `password123` | Doctor | CN_HN | Khoa_Noi |
| `u0001` | `password123` | Doctor | CN_HN | Khoa_Ngoai |
| `u0002` | `password123` | Doctor | CN_HCM | Khoa_Noi |
| `u0003` | `password123` | Nurse | CN_HN | Khoa_Noi |
| `u0006` | `password123` | Receptionist | CN_HN | Phong_TiepDon |
| `u0008` | `password123` | Cashier | CN_HN | Phong_TaiChinh |
| `u0010` | `password123` | HR | CN_HN | Phong_NhanSu |
| `u0012` | `password123` | Manager | CN_HN | Khoa_Noi |
| `u0014` | `password123` | ITAdmin | CN_HN | IT |

## Patient Assignments (ABAC)

| User | Role | Assigned Patients |
|------|------|-------------------|
| U0000 (Doctor) | Doctor | P00001, P00002, P00003, P00004, P00005, P00010, P00011 |
| U0001 (Doctor) | Doctor | P00006, P00007, P00008, P00009, P00012, P00013 |
| U0003 (Nurse) | Nurse | P00001, P00002, P00003, P00004, P00005 |

## Risk Score Calculation

| Factor | Score |
|--------|-------|
| Off-hours (before 8 AM or after 6 PM) | +2 |
| Export action | +3 |
| High sensitivity resource | +2 |
| High-risk resources (MedicalRecord, AuditLog...) | +3 |
| High-risk actions (export, delete) | +2 |

## Obligations

Khi request được allow, hệ thống có thể yêu cầu thêm các obligations:

| Obligation | Condition | Description |
|------------|-----------|-------------|
| step_up_mfa | Off-hours access | Yêu cầu MFA bổ sung |
| mask_fields | Non-Receptionist reading PatientProfile | Mask national_id, address |
| log_high_risk | High-risk actions | Ghi log chi tiết |
| require_approval_ref | Export action | Yêu cầu approval ticket |
| rate_limit | Bulk access | Giới hạn 60 requests/minute |
