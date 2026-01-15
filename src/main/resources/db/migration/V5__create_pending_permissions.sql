-- =====================================================
-- V5: Create pending_permission_requests table
-- Lưu các gợi ý permission từ AI để admin duyệt
-- =====================================================

CREATE TABLE IF NOT EXISTS pending_permission_requests (
    id BIGSERIAL PRIMARY KEY,
    
    -- User được gợi ý permission
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Permission được gợi ý
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    
    -- Độ tin cậy từ AI (0.00 - 1.00)
    confidence DECIMAL(3,2) NOT NULL,
    
    -- Loại yêu cầu: NEW_USER, JOB_TRANSFER, RIGHTSIZING
    request_type VARCHAR(50) NOT NULL DEFAULT 'NEW_USER',
    
    -- Loại thay đổi: ADD (thêm quyền), REMOVE (thu hồi quyền)
    change_type VARCHAR(20) NOT NULL DEFAULT 'ADD',
    
    -- Trạng thái: PENDING, APPROVED, REJECTED
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    -- Thời gian tạo request
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Admin đã review
    reviewed_by BIGINT REFERENCES users(id),
    reviewed_at TIMESTAMP,
    review_notes VARCHAR(500),
    
    -- Unique constraint: không có duplicate pending request
    UNIQUE(user_id, permission_id, status, change_type)
);

-- Indexes
CREATE INDEX idx_pending_status ON pending_permission_requests(status);
CREATE INDEX idx_pending_user ON pending_permission_requests(user_id);
CREATE INDEX idx_pending_type ON pending_permission_requests(request_type);
CREATE INDEX idx_pending_change_type ON pending_permission_requests(change_type);

-- Comments
COMMENT ON TABLE pending_permission_requests IS 'Lưu các gợi ý permission từ AI chờ admin duyệt';
COMMENT ON COLUMN pending_permission_requests.confidence IS 'Độ tin cậy từ AI model (0.60 - 1.00)';
COMMENT ON COLUMN pending_permission_requests.request_type IS 'NEW_USER: user mới, JOB_TRANSFER: chuyển phòng';
COMMENT ON COLUMN pending_permission_requests.change_type IS 'ADD: thêm quyền, REMOVE: thu hồi quyền';
