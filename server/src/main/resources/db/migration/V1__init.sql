-- =============================================
-- 陈哥厨房 数据库初始化脚本
-- =============================================

-- 自动更新 update_time 的触发器函数（全局复用）
CREATE OR REPLACE FUNCTION set_update_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- 用户表
-- =============================================
CREATE TABLE users (
    id          BIGSERIAL       PRIMARY KEY,
    openid      VARCHAR(64)     NOT NULL UNIQUE,
    nickname    VARCHAR(50),
    avatar_url  VARCHAR(500),
    role        VARCHAR(10)     NOT NULL DEFAULT 'user',
    status      VARCHAR(10)     NOT NULL DEFAULT 'active',
    create_time TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP       NOT NULL DEFAULT NOW(),
    create_by   VARCHAR(32),
    update_by   VARCHAR(32),
    deleted     BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_users_openid  ON users(openid);
CREATE INDEX idx_users_deleted ON users(deleted);

CREATE TRIGGER trg_users_update_time
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_update_time();

-- =============================================
-- 菜品分类表
-- =============================================
CREATE TABLE categories (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(20)     NOT NULL,
    sort_order  INT             NOT NULL DEFAULT 0,
    status      VARCHAR(10)     NOT NULL DEFAULT 'active',
    create_time TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP       NOT NULL DEFAULT NOW(),
    create_by   VARCHAR(32),
    update_by   VARCHAR(32),
    deleted     BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_categories_deleted ON categories(deleted);

CREATE TRIGGER trg_categories_update_time
    BEFORE UPDATE ON categories
    FOR EACH ROW EXECUTE FUNCTION set_update_time();

-- =============================================
-- 菜品表
-- =============================================
CREATE TABLE dishes (
    id            BIGSERIAL       PRIMARY KEY,
    name          VARCHAR(50)     NOT NULL,
    description   VARCHAR(500),
    image_url     VARCHAR(500),
    category_id   BIGINT          NOT NULL REFERENCES categories(id),
    cooking_time  INT,
    status        VARCHAR(15)     NOT NULL DEFAULT 'normal',
    is_listed     BOOLEAN         NOT NULL DEFAULT TRUE,
    create_time   TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time   TIMESTAMP       NOT NULL DEFAULT NOW(),
    create_by     VARCHAR(32),
    update_by     VARCHAR(32),
    deleted       BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_dishes_category ON dishes(category_id);
CREATE INDEX idx_dishes_listed   ON dishes(is_listed, deleted);
CREATE INDEX idx_dishes_deleted  ON dishes(deleted);

CREATE TRIGGER trg_dishes_update_time
    BEFORE UPDATE ON dishes
    FOR EACH ROW EXECUTE FUNCTION set_update_time();

-- =============================================
-- 订单表
-- =============================================
CREATE TABLE orders (
    id                BIGSERIAL       PRIMARY KEY,
    user_id           BIGINT          NOT NULL REFERENCES users(id),
    meal_type         VARCHAR(10)     NOT NULL,
    meal_date         DATE            NOT NULL,
    status            VARCHAR(15)     NOT NULL DEFAULT 'pending',
    taste_tags        JSONB,
    dietary_notes     VARCHAR(500),
    special_requests  VARCHAR(500),
    create_time       TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time       TIMESTAMP       NOT NULL DEFAULT NOW(),
    create_by         VARCHAR(32),
    update_by         VARCHAR(32),
    deleted           BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_orders_user      ON orders(user_id);
CREATE INDEX idx_orders_status    ON orders(status);
CREATE INDEX idx_orders_meal_date ON orders(meal_date);
CREATE INDEX idx_orders_deleted   ON orders(deleted);

CREATE TRIGGER trg_orders_update_time
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION set_update_time();

-- =============================================
-- 订单菜品明细表
-- =============================================
CREATE TABLE order_items (
    id          BIGSERIAL       PRIMARY KEY,
    order_id    BIGINT          NOT NULL REFERENCES orders(id),
    dish_id     BIGINT          NOT NULL REFERENCES dishes(id),
    dish_name   VARCHAR(50)     NOT NULL,
    quantity    INT             NOT NULL DEFAULT 1,
    is_extra    BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP       NOT NULL DEFAULT NOW(),
    create_by   VARCHAR(32),
    update_by   VARCHAR(32),
    deleted     BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_order_items_order   ON order_items(order_id);
CREATE INDEX idx_order_items_deleted ON order_items(deleted);

CREATE TRIGGER trg_order_items_update_time
    BEFORE UPDATE ON order_items
    FOR EACH ROW EXECUTE FUNCTION set_update_time();

-- =============================================
-- 收藏表
-- =============================================
CREATE TABLE favorites (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(id),
    dish_id     BIGINT          NOT NULL REFERENCES dishes(id),
    create_time TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP       NOT NULL DEFAULT NOW(),
    create_by   VARCHAR(32),
    update_by   VARCHAR(32),
    deleted     BOOLEAN         NOT NULL DEFAULT FALSE,
    UNIQUE(user_id, dish_id)
);

CREATE INDEX idx_favorites_user    ON favorites(user_id);
CREATE INDEX idx_favorites_deleted ON favorites(deleted);

CREATE TRIGGER trg_favorites_update_time
    BEFORE UPDATE ON favorites
    FOR EACH ROW EXECUTE FUNCTION set_update_time();

-- =============================================
-- 初始数据：插入管理员和默认分类
-- =============================================
INSERT INTO users (openid, nickname, role, create_by) VALUES
('test_admin_openid', '陈哥', 'admin', 'system'),
('test_wife_openid', '淇姐', 'user', 'system');

INSERT INTO categories (name, sort_order, create_by) VALUES
('家常菜', 1, 'system'),
('汤羹', 2, 'system'),
('主食', 3, 'system'),
('小吃', 4, 'system'),
('专属私房菜', 5, 'system');
