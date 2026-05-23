-- =============================================
-- V2: 管理端账密登录支持
-- 给 users 表添加 username / password 字段
-- =============================================

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS username VARCHAR(32) UNIQUE,
    ADD COLUMN IF NOT EXISTS password VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- 更新初始管理员账号（密码为 admin123，BCrypt 加密）
UPDATE users
SET username = 'admin',
    password = '$2a$10$oeCgZWfpqbSRrSdu4eYtwe.yAbovRejyryf3Yi.HCPZMXkuYNN7YO'
WHERE openid = 'test_admin_openid';
