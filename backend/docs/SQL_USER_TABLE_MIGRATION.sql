-- ============================================
-- 用户表结构迁移 SQL（PostgreSQL）
-- ============================================

-- 1. 添加密码哈希字段
ALTER TABLE app_user.users 
ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

-- 2. 添加状态字段
ALTER TABLE app_user.users 
ADD COLUMN IF NOT EXISTS status INTEGER DEFAULT 1;

-- 3. 添加注释
COMMENT ON COLUMN app_user.users.password_hash IS '密码哈希（BCrypt）';
COMMENT ON COLUMN app_user.users.status IS '用户状态：1-正常，0-禁用';

-- 4. 创建索引
CREATE INDEX IF NOT EXISTS idx_users_status ON app_user.users(status);

-- ============================================
-- 测试数据（可选）
-- ============================================

-- 插入测试用户（密码为 "123456"）
INSERT INTO app_user.users (external_id, password_hash, nickname, avatar_url, status, created_at, updated_at)
VALUES (
    'test_user',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- BCrypt hash of "123456"
    '测试用户',
    'https://via.placeholder.com/150',
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (external_id) DO NOTHING;

-- 验证数据
SELECT id, external_id, nickname, status, created_at 
FROM app_user.users 
WHERE external_id = 'test_user';
