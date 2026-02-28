-- MySQL 初始化脚本
-- Docker MySQL 首次启动时自动执行

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS blogdb
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- 创建用户并授权（如果不存在）
CREATE USER IF NOT EXISTS 'bloguser'@'%' IDENTIFIED BY 'MyBlog2026';
GRANT ALL PRIVILEGES ON blogdb.* TO 'bloguser'@'%';
FLUSH PRIVILEGES;
