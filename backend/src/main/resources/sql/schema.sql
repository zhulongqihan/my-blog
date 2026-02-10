-- =============================================
-- 后台管理系统数据库设计
-- 设计标准：阿里/字节 P7 级别
-- 特点：完整索引、详细注释、性能优化
-- =============================================

-- =============================================
-- 1. 用户权限体系（RBAC）
-- =============================================

-- 1.1 用户表（扩展）
ALTER TABLE `users` 
ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-正常' AFTER `role`,
ADD COLUMN `last_login_time` DATETIME COMMENT '最后登录时间' AFTER `status`,
ADD COLUMN `last_login_ip` VARCHAR(50) COMMENT '最后登录IP' AFTER `last_login_time`,
ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除' AFTER `updated_at`,
ADD INDEX `idx_role` (`role`),
ADD INDEX `idx_status` (`status`);

-- 1.2 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `role_key` VARCHAR(50) NOT NULL COMMENT '角色标识（如：admin、editor）',
  `description` VARCHAR(200) COMMENT '角色描述',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 初始化角色数据
INSERT INTO `sys_role` (`role_name`, `role_key`, `description`, `sort_order`) VALUES
('超级管理员', 'admin', '拥有所有权限', 1),
('编辑', 'editor', '可以管理文章和评论', 2),
('普通用户', 'user', '只能发表评论', 3);

-- 1.3 菜单权限表
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父菜单ID（0表示顶级菜单）',
  `menu_name` VARCHAR(50) NOT NULL COMMENT '菜单名称',
  `menu_type` CHAR(1) NOT NULL COMMENT '菜单类型：M-目录 C-菜单 F-按钮',
  `permission` VARCHAR(100) COMMENT '权限标识（如：article:create）',
  `path` VARCHAR(200) COMMENT '路由路径',
  `component` VARCHAR(200) COMMENT '组件路径',
  `icon` VARCHAR(100) COMMENT '菜单图标',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `visible` TINYINT NOT NULL DEFAULT 1 COMMENT '是否可见：0-隐藏 1-显示',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单权限表';

-- 索引设计理由：idx_parent_id 用于查询子菜单（树形结构查询）

-- 1.4 用户角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 索引设计理由：
-- uk_user_role: 防止重复分配角色
-- idx_user_id: 查询用户的所有角色
-- idx_role_id: 查询角色下的所有用户

-- 1.5 角色菜单关联表
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

-- =============================================
-- 2. 业务表优化
-- =============================================

-- 2.1 文章表（扩展）
ALTER TABLE `articles`
ADD COLUMN `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数' AFTER `like_count`,
ADD COLUMN `top` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：0-否 1-是' AFTER `featured`,
ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除' AFTER `updated_at`,
ADD INDEX `idx_author_id` (`author_id`),
ADD INDEX `idx_category_id` (`category_id`),
ADD INDEX `idx_published_time` (`published`, `published_at`),
ADD INDEX `idx_view_count` (`view_count`),
ADD INDEX `idx_created_at` (`created_at`);

-- 索引设计理由：
-- idx_author_id: 查询某作者的所有文章
-- idx_category_id: 按分类查询文章（高频操作）
-- idx_published_time: 查询已发布文章并按时间排序（组合索引，覆盖最常见查询）
-- idx_view_count: 热门文章排行榜
-- idx_created_at: 后台管理按创建时间排序

-- 2.2 分类表（扩展）
ALTER TABLE `categories`
ADD COLUMN `article_count` INT NOT NULL DEFAULT 0 COMMENT '文章数量（冗余字段，提升查询性能）' AFTER `sort_order`,
ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `article_count`,
ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`,
ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 AFTER `updated_at`;

-- 设计亮点：article_count 冗余字段避免每次查询都COUNT，提升性能
-- 更新策略：文章发布/删除时通过应用层更新

-- 2.3 标签表（扩展）
ALTER TABLE `tags`
ADD COLUMN `article_count` INT NOT NULL DEFAULT 0 COMMENT '文章数量' AFTER `color`,
ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `article_count`,
ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`,
ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 AFTER `updated_at`;


-- 2.4 评论表（扩展）
ALTER TABLE `comments`
ADD COLUMN `root_id` BIGINT DEFAULT 0 COMMENT '根评论ID（方便查询整个评论树）' AFTER `parent_id`,
ADD COLUMN `reply_to_user_id` BIGINT COMMENT '回复的用户ID' AFTER `root_id`,
ADD COLUMN `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数' AFTER `approved`,
ADD COLUMN `ip_address` VARCHAR(50) COMMENT '评论者IP' AFTER `like_count`,
ADD COLUMN `user_agent` VARCHAR(255) COMMENT '浏览器信息' AFTER `ip_address`,
ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`,
ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 AFTER `updated_at`,
ADD INDEX `idx_article_id` (`article_id`),
ADD INDEX `idx_user_id` (`user_id`),
ADD INDEX `idx_parent_id` (`parent_id`),
ADD INDEX `idx_root_id` (`root_id`),
ADD INDEX `idx_created_at` (`created_at`);

-- 索引设计理由：
-- idx_article_id: 查询文章的所有评论（高频）
-- idx_parent_id: 查询子评论（树形结构）
-- idx_root_id: 快速定位整个评论树
-- idx_created_at: 按时间排序

-- =============================================
-- 3. 运维表
-- =============================================

-- 3.1 操作日志表
CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` BIGINT COMMENT '操作用户ID',
  `username` VARCHAR(50) COMMENT '操作用户名',
  `operation_type` VARCHAR(20) NOT NULL COMMENT '操作类型：CREATE/UPDATE/DELETE/QUERY',
  `module` VARCHAR(50) NOT NULL COMMENT '操作模块：ARTICLE/COMMENT/USER等',
  `description` VARCHAR(200) COMMENT '操作描述',
  `method` VARCHAR(200) COMMENT '请求方法（类名.方法名）',
  `request_url` VARCHAR(255) COMMENT '请求URL',
  `request_method` VARCHAR(10) COMMENT 'HTTP方法：GET/POST/PUT/DELETE',
  `request_params` TEXT COMMENT '请求参数（JSON）',
  `response_result` TEXT COMMENT '响应结果（JSON，可选）',
  `ip_address` VARCHAR(50) COMMENT '操作IP',
  `location` VARCHAR(100) COMMENT 'IP归属地',
  `browser` VARCHAR(100) COMMENT '浏览器',
  `os` VARCHAR(100) COMMENT '操作系统',
  `execution_time` INT COMMENT '执行耗时（毫秒）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-失败 1-成功',
  `error_msg` TEXT COMMENT '错误信息',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_module` (`module`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- 索引设计理由：
-- idx_user_id: 查询某用户的操作记录
-- idx_module: 按模块查询日志
-- idx_operation_type: 按操作类型查询
-- idx_created_at: 按时间范围查询（日志分析常用）

-- 性能优化建议：
-- 1. 日志表数据量大，建议按月分表（operation_log_202602）
-- 2. 定期归档历史数据到冷存储
-- 3. 异步写入日志，避免阻塞主业务

-- =============================================
-- 4. 初始化菜单数据
-- =============================================

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_type`, `permission`, `path`, `component`, `icon`, `sort_order`) VALUES
-- 一级菜单
(0, '仪表盘', 'C', 'dashboard:view', '/dashboard', 'Dashboard', 'dashboard', 1),
(0, '内容管理', 'M', NULL, '/content', NULL, 'file-text', 2),
(0, '系统管理', 'M', NULL, '/system', NULL, 'setting', 3),

-- 内容管理子菜单
(2, '文章管理', 'C', 'article:list', '/content/articles', 'ArticleList', 'article', 1),
(2, '分类管理', 'C', 'category:list', '/content/categories', 'CategoryList', 'folder', 2),
(2, '标签管理', 'C', 'tag:list', '/content/tags', 'TagList', 'tag', 3),
(2, '评论管理', 'C', 'comment:list', '/content/comments', 'CommentList', 'message', 4),

-- 文章管理按钮权限
(4, '新增文章', 'F', 'article:create', NULL, NULL, NULL, 1),
(4, '编辑文章', 'F', 'article:update', NULL, NULL, NULL, 2),
(4, '删除文章', 'F', 'article:delete', NULL, NULL, NULL, 3),
(4, '发布文章', 'F', 'article:publish', NULL, NULL, NULL, 4),

-- 分类管理按钮权限
(5, '新增分类', 'F', 'category:create', NULL, NULL, NULL, 1),
(5, '编辑分类', 'F', 'category:update', NULL, NULL, NULL, 2),
(5, '删除分类', 'F', 'category:delete', NULL, NULL, NULL, 3),

-- 标签管理按钮权限
(6, '新增标签', 'F', 'tag:create', NULL, NULL, NULL, 1),
(6, '编辑标签', 'F', 'tag:update', NULL, NULL, NULL, 2),
(6, '删除标签', 'F', 'tag:delete', NULL, NULL, NULL, 3),

-- 评论管理按钮权限
(7, '审核评论', 'F', 'comment:audit', NULL, NULL, NULL, 1),
(7, '删除评论', 'F', 'comment:delete', NULL, NULL, NULL, 2),
(7, '回复评论', 'F', 'comment:reply', NULL, NULL, NULL, 3),

-- 系统管理子菜单
(3, '用户管理', 'C', 'user:list', '/system/users', 'UserList', 'user', 1),
(3, '角色管理', 'C', 'role:list', '/system/roles', 'RoleList', 'team', 2),
(3, '操作日志', 'C', 'log:list', '/system/logs', 'LogList', 'file', 3);

-- =============================================
-- 5. 初始化角色权限关联
-- =============================================

-- 超级管理员拥有所有权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, id FROM `sys_menu` WHERE `is_deleted` = 0;

-- 编辑拥有内容管理权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 2, id FROM `sys_menu` WHERE `is_deleted` = 0 AND (
  `parent_id` = 2 OR `id` = 2 OR `parent_id` IN (4, 5, 6, 7)
);

-- =============================================
-- 6. 数据完整性约束（可选）
-- =============================================

-- 外键约束（生产环境建议不使用外键，改用应用层控制）
-- ALTER TABLE `sys_user_role` ADD CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;
-- ALTER TABLE `sys_user_role` ADD CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE;
-- ALTER TABLE `sys_role_menu` ADD CONSTRAINT `fk_role_menu_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE;
-- ALTER TABLE `sys_role_menu` ADD CONSTRAINT `fk_role_menu_menu` FOREIGN KEY (`menu_id`) REFERENCES `sys_menu` (`id`) ON DELETE CASCADE;

-- =============================================
-- 7. 性能优化建议
-- =============================================

-- 7.1 定期清理软删除数据
-- DELETE FROM `articles` WHERE `is_deleted` = 1 AND `updated_at` < DATE_SUB(NOW(), INTERVAL 90 DAY);

-- 7.2 定期归档操作日志
-- CREATE TABLE `operation_log_archive_202601` LIKE `operation_log`;
-- INSERT INTO `operation_log_archive_202601` SELECT * FROM `operation_log` WHERE `created_at` < '2026-02-01';
-- DELETE FROM `operation_log` WHERE `created_at` < '2026-02-01';

-- 7.3 定期优化表
-- OPTIMIZE TABLE `articles`;
-- OPTIMIZE TABLE `comments`;
-- OPTIMIZE TABLE `operation_log`;

-- =============================================
-- 8. 监控查询（用于仪表盘）
-- =============================================

-- 8.1 统计文章总数
-- SELECT COUNT(*) FROM `articles` WHERE `is_deleted` = 0;

-- 8.2 统计评论总数
-- SELECT COUNT(*) FROM `comments` WHERE `is_deleted` = 0;

-- 8.3 最近7天访问趋势（需要访问日志表，暂时用文章创建趋势代替）
-- SELECT DATE(created_at) as date, COUNT(*) as count 
-- FROM `articles` 
-- WHERE `created_at` >= DATE_SUB(NOW(), INTERVAL 7 DAY) AND `is_deleted` = 0
-- GROUP BY DATE(created_at)
-- ORDER BY date;

-- 8.4 热门文章Top5
-- SELECT id, title, view_count 
-- FROM `articles` 
-- WHERE `published` = 1 AND `is_deleted` = 0
-- ORDER BY `view_count` DESC 
-- LIMIT 5;

-- 8.5 最新评论列表
-- SELECT c.id, c.content, c.created_at, u.username, a.title as article_title
-- FROM `comments` c
-- LEFT JOIN `users` u ON c.user_id = u.id
-- LEFT JOIN `articles` a ON c.article_id = a.id
-- WHERE c.is_deleted = 0
-- ORDER BY c.created_at DESC
-- LIMIT 10;
