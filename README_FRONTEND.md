# 📖 Frontend Integration Guide - Auth RBAC System

## Mục Lục
1. [Authentication](#1-authentication)
2. [Đăng Ký User Mới (với AI Recommendation)](#2-đăng-ký-user-mới-với-ai-recommendation)
3. [Chuyển Phòng Ban (Job Transfer)](#3-chuyển-phòng-ban-job-transfer)
4. [Admin - Quản Lý Users](#4-admin---quản-lý-users)
5. [Admin - Quản Lý Pending Permissions](#5-admin---quản-lý-pending-permissions)
6. [Error Handling](#6-error-handling)

---

## 🔑 Base Configuration

```javascript
const API_BASE_URL = 'http://localhost:8080/api';

// Headers mặc định
const headers = {
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${accessToken}` // Lấy từ login response
};
```

---

## 1. Authentication

### 1.1 Đăng Nhập

```
POST /api/auth/login
```

**Request:**
```json
{
  "username": "doctor1",
  "password": "password123"
}
```

**Response Success (200):**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "userId": "DOC001",
    "username": "doctor1",
    "role": "Doctor",
    "department": "Internal Medicine",
    "branch": "BRANCH_HCM",
    "permissions": {
      "MedicalRecord": "create,read,update",
      "Prescription": "approve,create,read,update",
      "PatientProfile": "read",
      "LabOrder": "create,read",
      "LabResult": "read"
    }
  }
}
```

**Response Error (401):**
```json
{
  "success": false,
  "message": "Bad credentials",
  "data": null
}
```

### 1.2 Lấy Thông Tin User Hiện Tại

```
GET /api/auth/me
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "userId": "DOC001",
    "username": "doctor1",
    "email": "doctor1@hospital.com",
    "role": "Doctor",
    "department": "Internal Medicine",
    "branch": "BRANCH_HCM",
    "position": "Senior Doctor",
    "hasLicense": true,
    "seniority": "Senior",
    "employmentType": "FullTime",
    "enabled": true,
    "assignedPatients": ["PAT001", "PAT002"],
    "permissions": {
      "MedicalRecord": "create,read,update",
      "Prescription": "approve,create,read,update"
    }
  }
}
```

### 1.3 Lấy Danh Sách Roles (cho dropdown đăng ký)

```
GET /api/auth/roles
```

**Response:**
```json
{
  "success": true,
  "message": "Available roles",
  "data": [
    "Doctor",
    "Nurse", 
    "Receptionist",
    "Cashier",
    "HR",
    "Manager",
    "ITAdmin",
    "SecurityAdmin"
  ]
}
```

---

## 2. Đăng Ký User Mới (với AI Recommendation)

### 🔄 Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         ĐĂNG KÝ USER MỚI                                │
└─────────────────────────────────────────────────────────────────────────┘

    ┌──────────────┐
    │  Form Đăng Ký │
    │              │
    │ - username   │
    │ - password   │
    │ - email      │
    │ - role       │ ──────► Dropdown từ GET /api/auth/roles
    │ - department │
    │ - branch     │
    │ - position   │
    │ - hasLicense │
    │ - seniority  │
    └──────┬───────┘
           │
           ▼
    POST /api/auth/register
           │
           ▼
    ┌──────────────────────────────────────────────────────────────────┐
    │ Backend:                                                          │
    │ 1. Validate & Create User                                        │
    │ 2. Gọi AI Service để gợi ý quyền bổ sung                         │
    │ 3. Lưu AI recommendations vào pending_permission_requests        │
    │ 4. Return user info                                              │
    └──────────────────────────────────────────────────────────────────┘
           │
           ▼
    ┌──────────────────────────────────────────────────────────────────┐
    │ Response:                                                         │
    │ - User được tạo thành công                                       │
    │ - Có N pending permissions chờ admin duyệt                       │
    └──────────────────────────────────────────────────────────────────┘
           │
           ▼
    ┌──────────────────────────────────────────────────────────────────┐
    │ Admin (HR/Manager/SecurityAdmin):                                 │
    │ - Vào màn GET /api/admin/permissions/pending                     │
    │ - Xem các gợi ý quyền từ AI                                      │
    │ - Approve hoặc Reject từng permission                            │
    └──────────────────────────────────────────────────────────────────┘
```

### 2.1 API Đăng Ký

```
POST /api/auth/register
Content-Type: application/json
```

**Request:**
```json
{
  "username": "newdoctor",
  "password": "SecurePass123!",
  "email": "newdoctor@hospital.com",
  "role": "Doctor",
  "department": "Internal Medicine",
  "branch": "BRANCH_HCM",
  "position": "Doctor",
  "hasLicense": true,
  "seniority": "Junior",
  "employmentType": "FullTime"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| username | string | ✅ | Tên đăng nhập (unique) |
| password | string | ✅ | Mật khẩu |
| email | string | ❌ | Email (unique nếu có) |
| role | string | ✅ | Vai trò: Doctor, Nurse, HR, etc. |
| department | string | ✅ | Phòng ban |
| branch | string | ✅ | Chi nhánh: BRANCH_HCM, BRANCH_HN |
| position | string | ❌ | Vị trí công việc (default: "Staff") |
| hasLicense | boolean | ❌ | Có giấy phép hành nghề (default: false) |
| seniority | string | ❌ | Thâm niên: Junior/Senior (default: "Junior") |
| employmentType | string | ❌ | Loại hợp đồng: FullTime/PartTime/Contract (default: "FullTime") |

**Response Success (201):**
```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "userId": "DOC12AB34CD",
    "username": "newdoctor",
    "email": "newdoctor@hospital.com",
    "role": "Doctor",
    "department": "Internal Medicine",
    "branch": "BRANCH_HCM",
    "position": "Doctor",
    "hasLicense": true,
    "seniority": "Junior",
    "employmentType": "FullTime",
    "enabled": true,
    "assignedPatients": [],
    "permissions": {
      "MedicalRecord": "create,read,update",
      "Prescription": "approve,create,read,update"
    }
  }
}
```

**Response Error (400):**
```json
{
  "success": false,
  "message": "Username already exists: newdoctor",
  "data": null
}
```

### 2.2 Departments & Branches (Gợi ý cho dropdown)

**Departments:**
```javascript
const departments = [
  "Internal Medicine",
  "Surgery", 
  "Pediatrics",
  "Cardiology",
  "Neurology",
  "Emergency",
  "Radiology",
  "Laboratory",
  "Pharmacy",
  "Human Resources",
  "Finance",
  "IT"
];
```

**Branches:**
```javascript
const branches = [
  "BRANCH_HCM",
  "BRANCH_HN"
];
```

**Seniority:**
```javascript
const seniorityOptions = ["Junior", "Senior"];
```

**Employment Type:**
```javascript
const employmentTypes = ["FullTime", "PartTime", "Contract"];
```

---

## 3. Chuyển Phòng Ban (Job Transfer)

### 🔄 Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      CHUYỂN PHÒNG BAN (JOB TRANSFER)                    │
└─────────────────────────────────────────────────────────────────────────┘

    ┌──────────────────────────────────────────────────────────────────┐
    │ Admin (HR/Manager/SecurityAdmin) chọn user cần chuyển            │
    │                                                                  │
    │ GET /api/users/admin/{userId}   ← Xem thông tin user hiện tại   │
    └──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
    ┌──────────────────────────────────────────────────────────────────┐
    │ Form Chuyển Phòng Ban:                                           │
    │                                                                  │
    │ Thông tin hiện tại:          Thông tin mới:                     │
    │ ├─ Role: Doctor              ├─ Role: HR (dropdown)             │
    │ ├─ Dept: Internal Medicine   ├─ Dept: Human Resources           │
    │ ├─ Branch: BRANCH_HCM        ├─ Branch: BRANCH_HCM              │
    │ └─ Position: Doctor          └─ Position: HR Staff              │
    │                                                                  │
    │ Lý do: [___________________________]                            │
    └──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                   PUT /api/users/{userId}/transfer
                              │
                              ▼
    ┌──────────────────────────────────────────────────────────────────┐
    │ Backend:                                                          │
    │ 1. Lưu old_profile của user                                      │
    │ 2. Gọi AI: POST /recommend/job-transfer                          │
    │    - So sánh old_profile vs new_profile                          │
    │    - AI trả về: added_permissions, removed_permissions           │
    │ 3. Cập nhật thông tin user (dept, role, branch...)               │
    │ 4. Lưu AI recommendations vào pending_permission_requests        │
    │    - added_permissions → change_type = "ADD"                     │
    │    - removed_permissions → change_type = "REMOVE"                │
    └──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
    ┌──────────────────────────────────────────────────────────────────┐
    │ Response:                                                         │
    │ - User info đã được cập nhật                                     │
    │ - Số quyền cần thêm: N                                           │
    │ - Số quyền cần thu hồi: M                                        │
    │ - AI recommendation chi tiết                                     │
    └──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
    ┌──────────────────────────────────────────────────────────────────┐
    │ Admin review pending permissions:                                 │
    │                                                                  │
    │ GET /api/admin/permissions/pending/user/{userId}                 │
    │                                                                  │
    │ Hiển thị:                                                        │
    │ ┌────────────────────────────────────────────────────────────┐   │
    │ │ Quyền cần THÊM (ADD):                                      │   │
    │ │ ☐ StaffProfile:read (confidence: 95%)     [Approve][Reject]│   │
    │ │ ☐ StaffProfile:update (confidence: 88%)   [Approve][Reject]│   │
    │ │ ☐ WorkSchedule:read (confidence: 82%)     [Approve][Reject]│   │
    │ ├────────────────────────────────────────────────────────────┤   │
    │ │ Quyền cần THU HỒI (REMOVE):                                │   │
    │ │ ☐ MedicalRecord:read (confidence: 92%)    [Approve][Reject]│   │
    │ │ ☐ Prescription:create (confidence: 85%)   [Approve][Reject]│   │
    │ └────────────────────────────────────────────────────────────┘   │
    │                                                                  │
    │ [Approve All] [Reject All]                                       │
    └──────────────────────────────────────────────────────────────────┘
```

### 3.1 API Chuyển Phòng Ban

```
PUT /api/users/{userId}/transfer
Authorization: Bearer {token}
Content-Type: application/json
```

**Roles được phép:** HR, Manager, SecurityAdmin

**Request:**
```json
{
  "newDepartment": "Human Resources",
  "newBranch": "BRANCH_HCM",
  "newRole": "HR",
  "newPosition": "HR Staff",
  "hasLicense": false,
  "seniority": "Senior",
  "reason": "Chuyển công tác theo yêu cầu cá nhân"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| newDepartment | string | ✅ | Phòng ban mới |
| newBranch | string | ❌ | Chi nhánh mới (giữ nguyên nếu không truyền) |
| newRole | string | ❌ | Role mới (giữ nguyên nếu không truyền) |
| newPosition | string | ❌ | Vị trí mới |
| hasLicense | boolean | ❌ | Cập nhật giấy phép |
| seniority | string | ❌ | Cập nhật thâm niên |
| reason | string | ❌ | Lý do chuyển |

**Response Success (200):**
```json
{
  "success": true,
  "message": "Job transfer initiated",
  "data": {
    "status": "JOB_TRANSFER_INITIATED",
    "userId": "DOC001",
    "username": "doctor1",
    "changes": {
      "department": {
        "from": "Internal Medicine",
        "to": "Human Resources"
      },
      "branch": {
        "from": "BRANCH_HCM",
        "to": "BRANCH_HCM"
      },
      "role": {
        "from": "Doctor",
        "to": "HR"
      }
    },
    "pendingPermissions": {
      "toAdd": 5,
      "toRemove": 12
    },
    "aiRecommendation": {
      "type": "JOB_TRANSFER",
      "added_permissions": [
        {"permission_id": 45, "permission": "StaffProfile_read", "confidence": 0.95},
        {"permission_id": 46, "permission": "StaffProfile_create", "confidence": 0.88},
        {"permission_id": 47, "permission": "StaffProfile_update", "confidence": 0.85},
        {"permission_id": 48, "permission": "WorkSchedule_read", "confidence": 0.82},
        {"permission_id": 49, "permission": "TrainingRecord_read", "confidence": 0.78}
      ],
      "removed_permissions": [
        {"permission_id": 1, "permission": "MedicalRecord_read", "confidence": 0.92},
        {"permission_id": 2, "permission": "MedicalRecord_create", "confidence": 0.90},
        {"permission_id": 3, "permission": "Prescription_create", "confidence": 0.88}
      ],
      "retained_permissions": [],
      "strategy": {
        "added": "secondary assignment with expiry",
        "removed": "revoke or downgrade",
        "retained": "scoped or read-only"
      }
    },
    "message": "Job transfer processed. Permission changes pending admin approval."
  }
}
```

### 3.2 Xem Pending Permissions của User

```
GET /api/users/{userId}/pending-permissions
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "userId": "DOC001",
    "username": "doctor1",
    "toAdd": [
      {
        "id": 1,
        "permissionKey": "StaffProfile:read:all",
        "resourceType": "StaffProfile",
        "action": "read",
        "confidence": 0.95,
        "requestType": "JOB_TRANSFER",
        "requestedAt": "2026-01-15T10:30:00"
      }
    ],
    "toRemove": [
      {
        "id": 2,
        "permissionKey": "MedicalRecord:read:all",
        "resourceType": "MedicalRecord",
        "action": "read",
        "confidence": 0.92,
        "requestType": "JOB_TRANSFER",
        "requestedAt": "2026-01-15T10:30:00"
      }
    ],
    "totalPending": 17
  }
}
```

---

## 4. Admin - Quản Lý Users

### 4.1 Lấy Tất Cả Users (với Statistics)

```
GET /api/users/admin/all
Authorization: Bearer {token}
```

**Query Parameters (Optional):**
| Param | Type | Description |
|-------|------|-------------|
| role | string | Filter theo role |
| department | string | Filter theo department |
| branch | string | Filter theo branch |
| enabled | boolean | Filter theo trạng thái |

**Examples:**
```
GET /api/users/admin/all
GET /api/users/admin/all?role=Doctor
GET /api/users/admin/all?department=Internal Medicine
GET /api/users/admin/all?branch=BRANCH_HCM&enabled=true
```

**Response:**
```json
{
  "success": true,
  "message": "All users for admin",
  "data": {
    "users": [
      {
        "id": 1,
        "userId": "DOC001",
        "username": "doctor1",
        "email": "doctor1@hospital.com",
        "role": "Doctor",
        "department": "Internal Medicine",
        "branch": "BRANCH_HCM",
        "position": "Senior Doctor",
        "hasLicense": true,
        "seniority": "Senior",
        "employmentType": "FullTime",
        "enabled": true,
        "accountNonLocked": true,
        "assignedPatients": ["PAT001", "PAT002"],
        "assignedPatientsCount": 2,
        "rolePermissionsCount": 18,
        "additionalPermissionsCount": 2,
        "pendingPermissionsCount": 3,
        "totalEffectivePermissions": 20,
        "permissions": {
          "MedicalRecord": "create,read,update",
          "Prescription": "approve,create,read,update"
        }
      },
      {
        "id": 2,
        "userId": "NUR001",
        "username": "nurse1",
        "email": "nurse1@hospital.com",
        "role": "Nurse",
        "department": "Internal Medicine",
        "branch": "BRANCH_HCM",
        "position": "Nurse",
        "hasLicense": true,
        "seniority": "Junior",
        "employmentType": "FullTime",
        "enabled": true,
        "accountNonLocked": true,
        "assignedPatients": ["PAT001"],
        "assignedPatientsCount": 1,
        "rolePermissionsCount": 9,
        "additionalPermissionsCount": 0,
        "pendingPermissionsCount": 0,
        "totalEffectivePermissions": 9,
        "permissions": {
          "VitalSigns": "create,read,update",
          "MedicalRecord": "read"
        }
      }
    ],
    "statistics": {
      "totalUsers": 10,
      "activeUsers": 9,
      "inactiveUsers": 1,
      "totalPendingPermissions": 15,
      "byRole": {
        "Doctor": 3,
        "Nurse": 4,
        "HR": 2,
        "SecurityAdmin": 1
      },
      "byDepartment": {
        "Internal Medicine": 4,
        "Surgery": 3,
        "Human Resources": 2,
        "IT": 1
      },
      "byBranch": {
        "BRANCH_HCM": 6,
        "BRANCH_HN": 4
      }
    }
  }
}
```

### 4.2 Chi Tiết User (cho Admin)

```
GET /api/users/admin/{userId}
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": "DOC001",
    "username": "doctor1",
    "email": "doctor1@hospital.com",
    "role": "Doctor",
    "department": "Internal Medicine",
    "branch": "BRANCH_HCM",
    "position": "Senior Doctor",
    "hasLicense": true,
    "seniority": "Senior",
    "employmentType": "FullTime",
    "enabled": true,
    "accountNonLocked": true,
    "assignedPatients": ["PAT001", "PAT002"],
    "assignedPatientsCount": 2,
    "rolePermissionsCount": 18,
    "additionalPermissionsCount": 2,
    "pendingPermissionsCount": 3,
    "totalEffectivePermissions": 20,
    "permissions": {
      "MedicalRecord": "create,read,update",
      "Prescription": "approve,create,read,update"
    },
    "pendingPermissions": [
      {
        "id": 1,
        "permissionKey": "StaffProfile:read:all",
        "resourceType": "StaffProfile",
        "action": "read",
        "confidence": 0.95,
        "changeType": "ADD",
        "requestType": "JOB_TRANSFER",
        "requestedAt": "2026-01-15T10:30:00"
      },
      {
        "id": 2,
        "permissionKey": "MedicalRecord:read:all",
        "resourceType": "MedicalRecord",
        "action": "read",
        "confidence": 0.92,
        "changeType": "REMOVE",
        "requestType": "JOB_TRANSFER",
        "requestedAt": "2026-01-15T10:30:00"
      }
    ]
  }
}
```

### 4.3 Lấy Users theo filter đơn giản

```
GET /api/users                           # Tất cả users
GET /api/users/{userId}                  # Chi tiết user
GET /api/users/department/{department}   # Theo department
GET /api/users/branch/{branch}           # Theo branch
GET /api/users/role/{roleName}           # Theo role
```

---

## 5. Admin - Quản Lý Pending Permissions

### 🔄 Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    ADMIN QUẢN LÝ PENDING PERMISSIONS                    │
└─────────────────────────────────────────────────────────────────────────┘

    ┌──────────────────────────────────────────────────────────────────┐
    │ Dashboard Admin:                                                  │
    │                                                                  │
    │ GET /api/admin/permissions/stats                                 │
    │ ┌────────────────────────────────────────────────────────────┐   │
    │ │  📊 Thống kê:                                              │   │
    │ │  • Pending: 15                                             │   │
    │ │  • Approved: 120                                           │   │
    │ │  • Rejected: 8                                             │   │
    │ └────────────────────────────────────────────────────────────┘   │
    └──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
    ┌──────────────────────────────────────────────────────────────────┐
    │ Danh sách Pending:                                               │
    │                                                                  │
    │ GET /api/admin/permissions/pending                               │
    │ GET /api/admin/permissions/pending/type/NEW_USER     ← Filter   │
    │ GET /api/admin/permissions/pending/type/JOB_TRANSFER ← Filter   │
    │                                                                  │
    │ ┌────────────────────────────────────────────────────────────┐   │
    │ │ User       │ Permission        │ Type        │ Change │Act│   │
    │ ├────────────┼───────────────────┼─────────────┼────────┼───┤   │
    │ │ doctor1    │ StaffProfile:read │ JOB_TRANSFER│ ADD    │ ✓✗│   │
    │ │ doctor1    │ MedicalRecord:read│ JOB_TRANSFER│ REMOVE │ ✓✗│   │
    │ │ newuser1   │ ExtraPermission   │ NEW_USER    │ ADD    │ ✓✗│   │
    │ └────────────────────────────────────────────────────────────┘   │
    │                                                                  │
    │ [Approve Selected] [Reject Selected] [Approve All for User]     │
    └──────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
    ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐
    │   Approve   │  │   Reject    │  │ Approve All     │
    │   Single    │  │   Single    │  │ for User        │
    │             │  │             │  │                 │
    │ POST        │  │ POST        │  │ POST            │
    │ /approve/1  │  │ /reject/1   │  │ /approve-all-   │
    │             │  │             │  │  for-user/{id}  │
    └─────────────┘  └─────────────┘  └─────────────────┘
              │               │               │
              └───────────────┼───────────────┘
                              ▼
    ┌──────────────────────────────────────────────────────────────────┐
    │ Kết quả:                                                          │
    │ • ADD + Approve → Thêm permission vào user                       │
    │ • REMOVE + Approve → Xóa permission khỏi user                    │
    │ • Reject → Không thay đổi permission                             │
    └──────────────────────────────────────────────────────────────────┘
```

### 5.1 Thống Kê Pending

```
GET /api/admin/permissions/stats
Authorization: Bearer {token}
```

**Roles được phép:** SecurityAdmin, Manager, HR

**Response:**
```json
{
  "success": true,
  "data": {
    "pending": 15,
    "approved": 120,
    "rejected": 8,
    "total": 143
  }
}
```

### 5.2 Lấy Tất Cả Pending Requests

```
GET /api/admin/permissions/pending
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "message": "Pending permission requests",
  "data": [
    {
      "id": 1,
      "userId": "DOC001",
      "userDbId": 1,
      "username": "doctor1",
      "userRole": "HR",
      "department": "Human Resources",
      "branch": "BRANCH_HCM",
      "permissionId": 45,
      "permissionKey": "StaffProfile:read:all",
      "resourceType": "StaffProfile",
      "action": "read",
      "confidence": 0.95,
      "requestType": "JOB_TRANSFER",
      "changeType": "ADD",
      "status": "PENDING",
      "requestedAt": "2026-01-15T10:30:00"
    },
    {
      "id": 2,
      "userId": "DOC001",
      "userDbId": 1,
      "username": "doctor1",
      "userRole": "HR",
      "department": "Human Resources",
      "branch": "BRANCH_HCM",
      "permissionId": 1,
      "permissionKey": "MedicalRecord:read:all",
      "resourceType": "MedicalRecord",
      "action": "read",
      "confidence": 0.92,
      "requestType": "JOB_TRANSFER",
      "changeType": "REMOVE",
      "status": "PENDING",
      "requestedAt": "2026-01-15T10:30:00"
    }
  ]
}
```

### 5.3 Filter Pending theo User

```
GET /api/admin/permissions/pending/user/{userDbId}
Authorization: Bearer {token}
```

**Note:** Sử dụng `userDbId` (database ID), không phải `userId` (DOC001)

### 5.4 Filter Pending theo Type

```
GET /api/admin/permissions/pending/type/{type}
Authorization: Bearer {token}
```

**Types:**
- `NEW_USER` - Quyền gợi ý khi tạo user mới
- `JOB_TRANSFER` - Quyền gợi ý khi chuyển phòng ban

### 5.5 Approve Single Request

```
POST /api/admin/permissions/approve/{requestId}
Authorization: Bearer {token}
Content-Type: application/json
```

**Request (Optional):**
```json
{
  "notes": "Approved by admin - necessary for new role"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Permission added to user: StaffProfile:read:all"
}
```

**Note về changeType:**
- `ADD` + Approve → **Thêm** permission vào `user_additional_permissions`
- `REMOVE` + Approve → **Xóa** permission khỏi `user_additional_permissions`

### 5.6 Reject Single Request

```
POST /api/admin/permissions/reject/{requestId}
Authorization: Bearer {token}
Content-Type: application/json
```

**Request (Optional):**
```json
{
  "notes": "Rejected - user doesn't need this permission"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Permission rejected: StaffProfile:read:all"
}
```

### 5.7 Bulk Approve

```
POST /api/admin/permissions/approve-bulk
Authorization: Bearer {token}
Content-Type: application/json
```

**Request:**
```json
{
  "requestIds": [1, 2, 3, 4, 5],
  "notes": "Bulk approval for job transfer"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Bulk approval completed",
  "data": {
    "approved": 5,
    "failed": 0,
    "errors": []
  }
}
```

### 5.8 Bulk Reject

```
POST /api/admin/permissions/reject-bulk
Authorization: Bearer {token}
Content-Type: application/json
```

**Request:**
```json
{
  "requestIds": [6, 7, 8],
  "notes": "Bulk rejection - not needed"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Bulk rejection completed",
  "data": {
    "rejected": 3,
    "failed": 0
  }
}
```

### 5.9 Approve All for User

```
POST /api/admin/permissions/approve-all-for-user/{userDbId}
Authorization: Bearer {token}
Content-Type: application/json
```

**Request (Optional):**
```json
{
  "notes": "Approved all permissions for new employee"
}
```

**Response:**
```json
{
  "success": true,
  "message": "All permissions approved for user",
  "data": {
    "userId": 1,
    "username": "doctor1",
    "approved": 5
  }
}
```

---

## 6. Error Handling

### Response Format

**Success:**
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... }
}
```

**Error:**
```json
{
  "success": false,
  "message": "Error description",
  "data": null
}
```

### Common HTTP Status Codes

| Code | Meaning | When |
|------|---------|------|
| 200 | OK | Request thành công |
| 201 | Created | Tạo resource thành công |
| 400 | Bad Request | Validation error, duplicate data |
| 401 | Unauthorized | Token không hợp lệ hoặc hết hạn |
| 403 | Forbidden | Không có quyền truy cập |
| 404 | Not Found | Resource không tồn tại |
| 500 | Internal Server Error | Lỗi server |

### Common Error Messages

```json
// Username đã tồn tại
{
  "success": false,
  "message": "Username already exists: doctor1"
}

// Email đã tồn tại
{
  "success": false,
  "message": "Email already exists: doctor1@hospital.com"
}

// Role không tồn tại
{
  "success": false,
  "message": "Role not found: InvalidRole"
}

// User không tồn tại
{
  "success": false,
  "message": "User not found: DOC999"
}

// Request đã được xử lý
{
  "success": false,
  "message": "Request already processed: APPROVED"
}

// Không có quyền
{
  "success": false,
  "message": "Access denied: BRANCH_MISMATCH"
}
```

---

## 📋 API Summary Table

| Method | Endpoint | Description | Roles |
|--------|----------|-------------|-------|
| POST | `/api/auth/login` | Đăng nhập | Public |
| POST | `/api/auth/register` | Đăng ký + AI recommend | Public |
| GET | `/api/auth/me` | User hiện tại | Authenticated |
| GET | `/api/auth/roles` | Danh sách roles | Public |
| GET | `/api/users` | Danh sách users | HR, Manager, SecurityAdmin |
| GET | `/api/users/admin/all` | Users + Statistics | HR, Manager, SecurityAdmin, ITAdmin |
| GET | `/api/users/admin/{userId}` | Chi tiết user (admin) | HR, Manager, SecurityAdmin, ITAdmin |
| GET | `/api/users/{userId}` | Chi tiết user | HR, Manager, SecurityAdmin |
| PUT | `/api/users/{userId}/transfer` | Chuyển phòng ban | HR, Manager, SecurityAdmin |
| GET | `/api/users/{userId}/pending-permissions` | Pending của user | HR, Manager, SecurityAdmin |
| GET | `/api/admin/permissions/stats` | Thống kê pending | SecurityAdmin, Manager, HR |
| GET | `/api/admin/permissions/pending` | Tất cả pending | SecurityAdmin, Manager, HR |
| GET | `/api/admin/permissions/pending/user/{id}` | Pending theo user | SecurityAdmin, Manager, HR |
| GET | `/api/admin/permissions/pending/type/{type}` | Pending theo type | SecurityAdmin, Manager, HR |
| POST | `/api/admin/permissions/approve/{id}` | Approve 1 request | SecurityAdmin, Manager, HR |
| POST | `/api/admin/permissions/reject/{id}` | Reject 1 request | SecurityAdmin, Manager, HR |
| POST | `/api/admin/permissions/approve-bulk` | Approve nhiều | SecurityAdmin, Manager, HR |
| POST | `/api/admin/permissions/reject-bulk` | Reject nhiều | SecurityAdmin, Manager, HR |
| POST | `/api/admin/permissions/approve-all-for-user/{id}` | Approve tất cả của user | SecurityAdmin, Manager, HR |

---

## 🎨 UI Components Gợi Ý

### 1. Form Đăng Ký
```
┌─────────────────────────────────────────┐
│           ĐĂNG KÝ TÀI KHOẢN             │
├─────────────────────────────────────────┤
│ Username:    [___________________]      │
│ Password:    [___________________]      │
│ Email:       [___________________]      │
│ Role:        [Doctor         ▼]         │
│ Department:  [Internal Medicine ▼]      │
│ Branch:      [BRANCH_HCM     ▼]         │
│ Position:    [___________________]      │
│ Seniority:   [Junior ▼]                 │
│ Employment:  [FullTime ▼]               │
│ Has License: [✓]                        │
│                                         │
│         [ĐĂNG KÝ]                        │
└─────────────────────────────────────────┘
```

### 2. Form Chuyển Phòng Ban
```
┌─────────────────────────────────────────┐
│         CHUYỂN PHÒNG BAN                │
│         User: doctor1 (DOC001)          │
├─────────────────────────────────────────┤
│ Thông tin hiện tại:                     │
│ • Role: Doctor                          │
│ • Dept: Internal Medicine               │
│ • Branch: BRANCH_HCM                    │
├─────────────────────────────────────────┤
│ Thông tin mới:                          │
│ New Role:       [HR              ▼]     │
│ New Department: [Human Resources ▼]     │
│ New Branch:     [BRANCH_HCM      ▼]     │
│ New Position:   [HR Staff___________]   │
│ Reason:         [__________________]    │
│                 [__________________]    │
│                                         │
│         [CHUYỂN PHÒNG BAN]              │
└─────────────────────────────────────────┘
```

### 3. Bảng Pending Permissions
```
┌────────────────────────────────────────────────────────────────────────┐
│ PENDING PERMISSION REQUESTS                          Total: 15        │
├────────────────────────────────────────────────────────────────────────┤
│ Filter: [All Types ▼] [All Users ▼]        [🔍 Search...]             │
├────────────────────────────────────────────────────────────────────────┤
│ ☐ │ User     │ Permission         │ Type        │ Change │ Conf │ Act │
├───┼──────────┼────────────────────┼─────────────┼────────┼──────┼─────┤
│ ☐ │ doctor1  │ StaffProfile:read  │ JOB_TRANSFER│ ADD    │ 95%  │ ✓ ✗│
│ ☐ │ doctor1  │ MedicalRecord:read │ JOB_TRANSFER│ REMOVE │ 92%  │ ✓ ✗│
│ ☐ │ newuser1 │ LabResult:read     │ NEW_USER    │ ADD    │ 88%  │ ✓ ✗│
├────────────────────────────────────────────────────────────────────────┤
│ [✓ Approve Selected] [✗ Reject Selected] [✓✓ Approve All for User]   │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 📞 Contact

Nếu có thắc mắc về API, vui lòng liên hệ Backend team.

