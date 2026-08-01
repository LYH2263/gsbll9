-- 创建数据库
CREATE DATABASE IF NOT EXISTS ctf_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ctf_db;

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    student_id VARCHAR(50) NOT NULL UNIQUE COMMENT '学号',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    full_name VARCHAR(100) NOT NULL COMMENT '真实姓名',
    role VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色: user/admin',
    is_active BOOLEAN DEFAULT TRUE COMMENT '账号是否激活',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 创建题目分类表
CREATE TABLE IF NOT EXISTS categories (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '分类名称',
    description VARCHAR(255) COMMENT '分类描述',
    order_num INT DEFAULT 0 COMMENT '排序号',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目分类表';

-- 创建题目表
CREATE TABLE IF NOT EXISTS questions (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '题目ID',
    category_id INT NOT NULL COMMENT '分类ID',
    title VARCHAR(255) NOT NULL COMMENT '题目标题',
    description TEXT COMMENT '题目描述',
    file_url VARCHAR(500) COMMENT '附件URL',
    flag VARCHAR(255) NOT NULL COMMENT '正确答案',
    points INT DEFAULT 1 COMMENT '分值',
    difficulty VARCHAR(20) DEFAULT 'medium' COMMENT '难度: easy/medium/hard',
    order_num INT DEFAULT 0 COMMENT '排序号',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
    INDEX idx_question_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目表';

-- 创建用户比赛数据表
CREATE TABLE IF NOT EXISTS contest_users (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '比赛用户ID',
    user_id INT NOT NULL COMMENT '用户ID',
    selected_questions TEXT COMMENT '已选题目ID列表(JSON)',
    current_question_id INT COMMENT '当前题目ID',
    total_score INT DEFAULT 0 COMMENT '总分',
    submitted BOOLEAN DEFAULT FALSE COMMENT '是否已提交',
    start_time TIMESTAMP NULL COMMENT '开始时间',
    submit_time TIMESTAMP NULL COMMENT '提交时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY unique_user (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户比赛数据表';

-- 创建提交记录表
CREATE TABLE IF NOT EXISTS submissions (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '提交记录ID',
    contest_user_id INT NOT NULL COMMENT '用户比赛ID',
    question_id INT NOT NULL COMMENT '题目ID',
    user_answer VARCHAR(255) NOT NULL COMMENT '用户输入的答案',
    is_correct BOOLEAN DEFAULT FALSE COMMENT '是否正确',
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    FOREIGN KEY (contest_user_id) REFERENCES contest_users(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    UNIQUE KEY unique_submission (contest_user_id, question_id),
    INDEX idx_submissions_user (contest_user_id),
    INDEX idx_submissions_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提交记录表';

-- 创建比赛配置表
CREATE TABLE IF NOT EXISTS contest_config (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    config_key VARCHAR(50) NOT NULL UNIQUE COMMENT '配置键',
    config_value VARCHAR(255) NOT NULL COMMENT '配置值',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛配置表';

-- 创建题目提示表
CREATE TABLE IF NOT EXISTS hints (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '提示ID',
    question_id INT NOT NULL COMMENT '题目ID',
    hint_number INT NOT NULL COMMENT '提示序号: 1, 2, 3',
    content TEXT NOT NULL COMMENT '提示内容',
    penalty DECIMAL(5, 2) DEFAULT 0 COMMENT '扣分值',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    UNIQUE KEY unique_hint (question_id, hint_number),
    INDEX idx_hints_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目提示表';

-- 创建提示解锁记录表
CREATE TABLE IF NOT EXISTS hint_unlocks (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '解锁记录ID',
    contest_user_id INT NOT NULL COMMENT '比赛用户ID',
    hint_id INT NOT NULL COMMENT '提示ID',
    unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '解锁时间',
    FOREIGN KEY (contest_user_id) REFERENCES contest_users(id) ON DELETE CASCADE,
    FOREIGN KEY (hint_id) REFERENCES hints(id) ON DELETE CASCADE,
    UNIQUE KEY unique_unlock (contest_user_id, hint_id),
    INDEX idx_hint_unlocks_user (contest_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提示解锁记录表';

-- 创建公告表
CREATE TABLE IF NOT EXISTS announcements (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '公告ID',
    content TEXT NOT NULL COMMENT '公告内容',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    is_active BOOLEAN DEFAULT FALSE COMMENT '是否激活显示',
    INDEX idx_announcements_active (is_active),
    INDEX idx_announcements_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';

