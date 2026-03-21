-- 预设功能点库 + 风险系数「标准周期」等（可重复执行）
CREATE TABLE IF NOT EXISTS biz_quote_preset_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(512) NOT NULL,
    complexity VARCHAR(32) NOT NULL DEFAULT 'standard',
    category VARCHAR(128) NULL COMMENT '分组/模块建议',
    sort_order INT NOT NULL DEFAULT 0,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_enabled_sort (enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 标准交付周期（负向风险，与加急互斥建议由前端提示）
INSERT IGNORE INTO biz_quote_risk_config (risk_key, label, default_pct, enabled) VALUES
('standard_cycle', '标准交付周期（非加急）', -10, 1);

-- 预设功能点（按名称幂等，避免重复插入）
INSERT INTO biz_quote_preset_item (name, complexity, category, sort_order, enabled)
SELECT t.name, t.complexity, t.category, t.sort_order, t.enabled FROM (
    SELECT '手机号验证码登录' AS name, 'standard' AS complexity, '用户与认证' AS category, 10 AS sort_order, 1 AS enabled
) t WHERE NOT EXISTS (SELECT 1 FROM biz_quote_preset_item p WHERE p.name = t.name LIMIT 1);
INSERT INTO biz_quote_preset_item (name, complexity, category, sort_order, enabled)
SELECT t.name, t.complexity, t.category, t.sort_order, t.enabled FROM (
    SELECT '微信 OAuth / 一键登录' AS name, 'medium' AS complexity, '用户与认证' AS category, 20 AS sort_order, 1 AS enabled
) t WHERE NOT EXISTS (SELECT 1 FROM biz_quote_preset_item p WHERE p.name = t.name LIMIT 1);
INSERT INTO biz_quote_preset_item (name, complexity, category, sort_order, enabled)
SELECT t.name, t.complexity, t.category, t.sort_order, t.enabled FROM (
    SELECT '用户注册与个人中心' AS name, 'standard' AS complexity, '用户与认证' AS category, 30 AS sort_order, 1 AS enabled
) t WHERE NOT EXISTS (SELECT 1 FROM biz_quote_preset_item p WHERE p.name = t.name LIMIT 1);
INSERT INTO biz_quote_preset_item (name, complexity, category, sort_order, enabled)
SELECT t.name, t.complexity, t.category, t.sort_order, t.enabled FROM (
    SELECT '商品列表与详情（含 SKU）' AS name, 'standard' AS complexity, '电商' AS category, 40 AS sort_order, 1 AS enabled
) t WHERE NOT EXISTS (SELECT 1 FROM biz_quote_preset_item p WHERE p.name = t.name LIMIT 1);
INSERT INTO biz_quote_preset_item (name, complexity, category, sort_order, enabled)
SELECT t.name, t.complexity, t.category, t.sort_order, t.enabled FROM (
    SELECT '购物车与下单' AS name, 'medium' AS complexity, '电商' AS category, 50 AS sort_order, 1 AS enabled
) t WHERE NOT EXISTS (SELECT 1 FROM biz_quote_preset_item p WHERE p.name = t.name LIMIT 1);
INSERT INTO biz_quote_preset_item (name, complexity, category, sort_order, enabled)
SELECT t.name, t.complexity, t.category, t.sort_order, t.enabled FROM (
    SELECT '订单管理与支付对接' AS name, 'complex' AS complexity, '电商' AS category, 60 AS sort_order, 1 AS enabled
) t WHERE NOT EXISTS (SELECT 1 FROM biz_quote_preset_item p WHERE p.name = t.name LIMIT 1);
INSERT INTO biz_quote_preset_item (name, complexity, category, sort_order, enabled)
SELECT t.name, t.complexity, t.category, t.sort_order, t.enabled FROM (
    SELECT '后台权限与角色（RBAC）' AS name, 'medium' AS complexity, '管理后台' AS category, 70 AS sort_order, 1 AS enabled
) t WHERE NOT EXISTS (SELECT 1 FROM biz_quote_preset_item p WHERE p.name = t.name LIMIT 1);
INSERT INTO biz_quote_preset_item (name, complexity, category, sort_order, enabled)
SELECT t.name, t.complexity, t.category, t.sort_order, t.enabled FROM (
    SELECT '数据报表与导出' AS name, 'standard' AS complexity, '管理后台' AS category, 80 AS sort_order, 1 AS enabled
) t WHERE NOT EXISTS (SELECT 1 FROM biz_quote_preset_item p WHERE p.name = t.name LIMIT 1);
INSERT INTO biz_quote_preset_item (name, complexity, category, sort_order, enabled)
SELECT t.name, t.complexity, t.category, t.sort_order, t.enabled FROM (
    SELECT '第三方接口对接（通用）' AS name, 'complex' AS complexity, '集成' AS category, 90 AS sort_order, 1 AS enabled
) t WHERE NOT EXISTS (SELECT 1 FROM biz_quote_preset_item p WHERE p.name = t.name LIMIT 1);
