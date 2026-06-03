-- ========================================
-- 在线考试系统 - 数据库初始化脚本
-- 数据库：SQL Server
-- 数据库名：exam_system
-- ========================================

-- 创建数据库（如果不存在则创建）
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'exam_system')
BEGIN
    CREATE DATABASE exam_system;
END
GO

USE exam_system;
GO

-- ========================================
-- 1. 用户表
-- ========================================
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='users' AND xtype='U')
BEGIN
    CREATE TABLE users (
        id INT IDENTITY(1,1) PRIMARY KEY,
        username NVARCHAR(50) NOT NULL,
        password NVARCHAR(255) NOT NULL,
        role NVARCHAR(20) NOT NULL,
        CONSTRAINT UQ_users_username UNIQUE(username),
        CONSTRAINT CK_users_role CHECK (role IN ('student','teacher','admin'))
    );
END
GO

-- role 字段索引（按角色查询用户）
CREATE NONCLUSTERED INDEX idx_users_role ON users(role);
GO

-- ========================================
-- 2. 题目表
-- ========================================
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='questions' AND xtype='U')
BEGIN
    CREATE TABLE questions (
        id INT IDENTITY PRIMARY KEY,
        type NVARCHAR(20) NOT NULL,
        content NVARCHAR(MAX),
        options NVARCHAR(MAX),
        answer NVARCHAR(MAX),
        reference_answer NVARCHAR(MAX),
        course_id INT,
        CONSTRAINT CK_questions_type CHECK (type IN ('single_choice','multi_choice','true_false','fill_blank','short_answer'))
    );
END
GO

IF COL_LENGTH('questions', 'reference_answer') IS NULL
BEGIN
    ALTER TABLE questions ADD reference_answer NVARCHAR(MAX) NULL;
END
GO

-- ========================================
-- 3. 试卷表
-- ========================================
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='papers' AND xtype='U')
BEGIN
    CREATE TABLE papers (
        id INT IDENTITY PRIMARY KEY,
        title NVARCHAR(100),
        duration INT,
        created_by INT,
        teacher_open_answer BIT NOT NULL DEFAULT 0,
        open_start_time DATETIME2 NULL,
        open_end_time DATETIME2 NULL,
        CONSTRAINT FK_papers_created_by FOREIGN KEY (created_by) REFERENCES users(id)
    );
END
GO

IF COL_LENGTH('papers', 'teacher_open_answer') IS NULL
BEGIN
    ALTER TABLE papers ADD teacher_open_answer BIT NOT NULL DEFAULT 0;
END
GO

IF COL_LENGTH('papers', 'open_start_time') IS NULL
BEGIN
    ALTER TABLE papers ADD open_start_time DATETIME2 NULL;
END
GO

IF COL_LENGTH('papers', 'open_end_time') IS NULL
BEGIN
    ALTER TABLE papers ADD open_end_time DATETIME2 NULL;
END
GO

-- ========================================
-- 4. 试卷-题目关联表
-- ========================================
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='paper_questions' AND xtype='U')
BEGIN
    CREATE TABLE paper_questions (
        id INT IDENTITY PRIMARY KEY,
        paper_id INT,
        question_id INT,
        score DECIMAL(5,1),
        CONSTRAINT FK_pq_paper FOREIGN KEY (paper_id) REFERENCES papers(id),
        CONSTRAINT FK_pq_question FOREIGN KEY (question_id) REFERENCES questions(id)
    );
END
GO

-- ========================================
-- 5. 考试记录表
-- ========================================
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='exam_records' AND xtype='U')
BEGIN
    CREATE TABLE exam_records (
        id INT IDENTITY PRIMARY KEY,
        student_id INT,
        paper_id INT,
        start_time DATETIME2,
        submit_time DATETIME2,
        total_score DECIMAL(5,1),
        CONSTRAINT FK_er_student FOREIGN KEY (student_id) REFERENCES users(id),
        CONSTRAINT FK_er_paper FOREIGN KEY (paper_id) REFERENCES papers(id)
    );
END
GO

CREATE NONCLUSTERED INDEX idx_exam_records_student ON exam_records(student_id);
CREATE NONCLUSTERED INDEX idx_exam_records_paper ON exam_records(paper_id);
GO

-- ========================================
-- 6. 答题明细表
-- ========================================
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='answer_details' AND xtype='U')
BEGIN
    CREATE TABLE answer_details (
        id INT IDENTITY PRIMARY KEY,
        record_id INT,
        question_id INT,
        student_answer NVARCHAR(MAX),
        is_correct BIT,
        score_got DECIMAL(5,1),
        CONSTRAINT FK_ad_record FOREIGN KEY (record_id) REFERENCES exam_records(id),
        CONSTRAINT FK_ad_question FOREIGN KEY (question_id) REFERENCES questions(id)
    );
END
GO

CREATE NONCLUSTERED INDEX idx_answer_details_record ON answer_details(record_id);
GO

PRINT '数据库初始化完成！所有表已创建。';
GO
