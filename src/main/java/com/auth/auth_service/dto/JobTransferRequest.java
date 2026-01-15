package com.auth.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho yêu cầu chuyển phòng ban/vị trí của user.
 * Admin sử dụng để đổi department, branch, role cho user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobTransferRequest {
    
    /**
     * Phòng ban mới (bắt buộc)
     */
    @NotBlank(message = "New department is required")
    private String newDepartment;
    
    /**
     * Chi nhánh mới (tùy chọn - giữ nguyên nếu không đổi)
     */
    private String newBranch;
    
    /**
     * Role mới (tùy chọn - giữ nguyên nếu không đổi)
     */
    private String newRole;
    
    /**
     * Vị trí mới (tùy chọn)
     */
    private String newPosition;
    
    /**
     * Cập nhật giấy phép hành nghề (tùy chọn)
     */
    private Boolean hasLicense;
    
    /**
     * Cập nhật thâm niên (tùy chọn)
     */
    private String seniority;
    
    /**
     * Lý do chuyển phòng ban
     */
    private String reason;
}
