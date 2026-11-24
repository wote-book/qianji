-- 理财管理系统 数据库结构 v1.0
-- 依据：方案总结 v3.0 第 7 章、开发方案 v1.0 第 4 章
-- 说明：固定六张业务表，允许字段级补充（account.init_balance 用于期初余额对账）

CREATE DATABASE IF NOT EXISTS finance_db
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE finance_db;

DROP TABLE IF EXISTS `usage_event`;
DROP TABLE IF EXISTS `login_log`;
DROP TABLE IF EXISTS `record`;
DROP TABLE IF EXISTS `budget`;
DROP TABLE IF EXISTS `goal`;
DROP TABLE IF EXISTS `category`;
DROP TABLE IF EXISTS `account`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  username VARCHAR(50) NOT NULL COMMENT '登录名',
  password VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希',
  nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT 'USER / ADMIN',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1 正常 / 0 禁用',
  security_question VARCHAR(100) DEFAULT NULL COMMENT '找回密码问题',
  security_answer VARCHAR(100) DEFAULT NULL COMMENT '安全问题答案（BCrypt）',
  last_login_time DATETIME DEFAULT NULL COMMENT '最近登录时间',
  last_login_ip VARCHAR(45) DEFAULT NULL COMMENT '最近登录 IP',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE `account` (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  acc_name VARCHAR(50) NOT NULL,
  type VARCHAR(20) NOT NULL DEFAULT 'CASH' COMMENT 'CASH / BANK / ALIPAY / WECHAT / OTHER',
  init_balance DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '期初余额，仅建账户时写入',
  balance DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '当前余额，由流水驱动',
  allow_overdraft TINYINT NOT NULL DEFAULT 0 COMMENT '1 允许透支',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_acc (user_id, acc_name),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户表';

CREATE TABLE `category` (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(30) NOT NULL,
  type VARCHAR(10) NOT NULL COMMENT 'INCOME / EXPENSE',
  icon VARCHAR(50) DEFAULT NULL,
  is_preset TINYINT NOT NULL DEFAULT 0 COMMENT '1 预设分类，不可删除',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 停用',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_type_name (user_id, type, name),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类表';

CREATE TABLE `record` (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  amount DECIMAL(12,2) NOT NULL COMMENT '恒为正，方向由 type 决定',
  type VARCHAR(10) NOT NULL COMMENT 'INCOME / EXPENSE',
  record_date DATE NOT NULL,
  remark VARCHAR(200) DEFAULT NULL,
  source VARCHAR(10) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL / CSV / RESTORE',
  dedup_key VARCHAR(64) DEFAULT NULL COMMENT '导入去重指纹',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_dedup (user_id, dedup_key),
  KEY idx_user_date (user_id, record_date),
  KEY idx_user_account (user_id, account_id),
  KEY idx_user_category (user_id, category_id),
  CONSTRAINT chk_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收支流水表';

CREATE TABLE `budget` (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  month CHAR(7) NOT NULL COMMENT 'yyyy-MM',
  category_id BIGINT NOT NULL DEFAULT 0 COMMENT '0 表示月度总预算',
  limit_amt DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  warn_threshold TINYINT NOT NULL DEFAULT 80 COMMENT '黄灯阈值百分比',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_month_cat (user_id, month, category_id),
  CONSTRAINT chk_limit CHECK (limit_amt >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预算表';

CREATE TABLE `goal` (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(50) NOT NULL,
  target_amt DECIMAL(12,2) NOT NULL,
  saved_amt DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '手工累加，不联动余额',
  start_date DATE NOT NULL,
  status VARCHAR(10) NOT NULL DEFAULT 'DOING' COMMENT 'DOING / DONE',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user (user_id),
  CONSTRAINT chk_target CHECK (target_amt > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='理财目标表';

CREATE TABLE `usage_event` (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  module VARCHAR(32) NOT NULL COMMENT 'RECORD/ACCOUNT/CATEGORY/BUDGET/STATS/GOAL/BRIEF/REPORT/BACKUP/IMPORT',
  action VARCHAR(32) NOT NULL COMMENT 'VIEW/CREATE/UPDATE/DELETE/GENERATE/EXPORT',
  ref_id BIGINT DEFAULT NULL,
  ip VARCHAR(45) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_time (user_id, create_time),
  KEY idx_module_time (module, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='使用事件';

CREATE TABLE `login_log` (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT DEFAULT NULL,
  username VARCHAR(50) NOT NULL,
  ip VARCHAR(45) DEFAULT NULL,
  user_agent VARCHAR(255) DEFAULT NULL,
  success TINYINT NOT NULL DEFAULT 1 COMMENT '1 成功 / 0 失败',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_time (create_time),
  KEY idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志';
