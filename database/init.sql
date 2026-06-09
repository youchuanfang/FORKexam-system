-- ========================================
-- 在线考试系统 - 数据库初始化脚本
-- 数据库：SQL Server
-- 数据库名：exam_system
-- ========================================

-- 创建数据库（如果不存在则创建）
IF NOT EXISTS (SELECT 1 FROM sys.databases WHERE name = N'exam_system')
BEGIN
    CREATE DATABASE exam_system;
END
GO

USE exam_system;
GO

-- ========================================
-- 1. 用户表
-- ========================================
IF OBJECT_ID('dbo.users', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.users (
        id INT IDENTITY(1,1) PRIMARY KEY,
        username NVARCHAR(50) NOT NULL,
        password NVARCHAR(255) NOT NULL,
        role NVARCHAR(20) NOT NULL,
        status NVARCHAR(20) NOT NULL CONSTRAINT DF_users_status DEFAULT 'active',
        CONSTRAINT UQ_users_username UNIQUE(username),
        CONSTRAINT CK_users_role CHECK (role IN ('student','teacher','admin')),
        CONSTRAINT CK_users_status CHECK (status IN ('active','disabled'))
    );
END
GO

IF COL_LENGTH('dbo.users', 'status') IS NULL
BEGIN
    ALTER TABLE dbo.users
        ADD status NVARCHAR(20) NOT NULL CONSTRAINT DF_users_status DEFAULT 'active' WITH VALUES;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_users_status' AND parent_object_id = OBJECT_ID('dbo.users'))
BEGIN
    ALTER TABLE dbo.users ADD CONSTRAINT CK_users_status CHECK (status IN ('active','disabled'));
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'idx_users_role'
      AND object_id = OBJECT_ID('dbo.users')
)
BEGIN
    CREATE NONCLUSTERED INDEX idx_users_role ON dbo.users(role);
END
GO

-- ========================================
-- 2. 题目表
-- ========================================
IF OBJECT_ID('dbo.questions', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.questions (
        id INT IDENTITY(1,1) PRIMARY KEY,
        type NVARCHAR(20) NOT NULL,
        content NVARCHAR(MAX),
        options NVARCHAR(MAX),
        answer NVARCHAR(MAX),
        reference_answer NVARCHAR(MAX),
        course_id INT,
        created_by INT NULL,
        CONSTRAINT CK_questions_type CHECK (type IN ('single_choice','multi_choice','true_false','fill_blank','short_answer')),
        CONSTRAINT FK_questions_created_by FOREIGN KEY (created_by) REFERENCES dbo.users(id)
    );
END
GO

IF COL_LENGTH('dbo.questions', 'reference_answer') IS NULL
BEGIN
    ALTER TABLE dbo.questions ADD reference_answer NVARCHAR(MAX) NULL;
END
GO

IF COL_LENGTH('dbo.questions', 'created_by') IS NULL
BEGIN
    ALTER TABLE dbo.questions ADD created_by INT NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_questions_created_by' AND parent_object_id = OBJECT_ID('dbo.questions'))
BEGIN
    ALTER TABLE dbo.questions ADD CONSTRAINT FK_questions_created_by FOREIGN KEY (created_by) REFERENCES dbo.users(id);
END
GO

-- ========================================
-- 3. 试卷表
-- ========================================
IF OBJECT_ID('dbo.papers', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.papers (
        id INT IDENTITY(1,1) PRIMARY KEY,
        title NVARCHAR(100),
        duration INT,
        created_by INT,
        teacher_open_answer BIT NOT NULL CONSTRAINT DF_papers_teacher_open_answer DEFAULT 0,
        open_start_time DATETIME2 NULL,
        open_end_time DATETIME2 NULL,
        max_attempts INT NOT NULL CONSTRAINT DF_papers_max_attempts DEFAULT 1,
        release_answer_flag BIT NOT NULL CONSTRAINT DF_papers_release_answer_flag DEFAULT 0,
        answer_release_time DATETIME2 NULL,
        published BIT NOT NULL CONSTRAINT DF_papers_published DEFAULT 0,
        published_at DATETIME2 NULL,
        leaderboard_public BIT NOT NULL CONSTRAINT DF_papers_leaderboard_public DEFAULT 0,
        CONSTRAINT FK_papers_created_by FOREIGN KEY (created_by) REFERENCES dbo.users(id)
    );
END
GO

IF COL_LENGTH('dbo.papers', 'teacher_open_answer') IS NULL
BEGIN
    ALTER TABLE dbo.papers ADD teacher_open_answer BIT NOT NULL CONSTRAINT DF_papers_teacher_open_answer DEFAULT 0 WITH VALUES;
END
GO

IF COL_LENGTH('dbo.papers', 'open_start_time') IS NULL
BEGIN
    ALTER TABLE dbo.papers ADD open_start_time DATETIME2 NULL;
END
GO

IF COL_LENGTH('dbo.papers', 'open_end_time') IS NULL
BEGIN
    ALTER TABLE dbo.papers ADD open_end_time DATETIME2 NULL;
END
GO

IF COL_LENGTH('dbo.papers', 'max_attempts') IS NULL
BEGIN
    ALTER TABLE dbo.papers ADD max_attempts INT NOT NULL CONSTRAINT DF_papers_max_attempts DEFAULT 1 WITH VALUES;
END
GO

IF COL_LENGTH('dbo.papers', 'release_answer_flag') IS NULL
BEGIN
    ALTER TABLE dbo.papers ADD release_answer_flag BIT NOT NULL CONSTRAINT DF_papers_release_answer_flag DEFAULT 0 WITH VALUES;
END
GO

IF COL_LENGTH('dbo.papers', 'answer_release_time') IS NULL
BEGIN
    ALTER TABLE dbo.papers ADD answer_release_time DATETIME2 NULL;
END
GO

IF COL_LENGTH('dbo.papers', 'published') IS NULL
BEGIN
    ALTER TABLE dbo.papers ADD published BIT NOT NULL CONSTRAINT DF_papers_published DEFAULT 0 WITH VALUES;
END
GO

IF COL_LENGTH('dbo.papers', 'published_at') IS NULL
BEGIN
    ALTER TABLE dbo.papers ADD published_at DATETIME2 NULL;
END
GO

IF COL_LENGTH('dbo.papers', 'leaderboard_public') IS NULL
BEGIN
    ALTER TABLE dbo.papers ADD leaderboard_public BIT NOT NULL CONSTRAINT DF_papers_leaderboard_public DEFAULT 0 WITH VALUES;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_papers_created_by' AND parent_object_id = OBJECT_ID('dbo.papers'))
BEGIN
    ALTER TABLE dbo.papers ADD CONSTRAINT FK_papers_created_by FOREIGN KEY (created_by) REFERENCES dbo.users(id);
END
GO

-- ========================================
-- 4. 试卷-题目关联表
-- ========================================
IF OBJECT_ID('dbo.paper_questions', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.paper_questions (
        id INT IDENTITY(1,1) PRIMARY KEY,
        paper_id INT,
        question_id INT,
        score DECIMAL(5,1),
        CONSTRAINT FK_pq_paper FOREIGN KEY (paper_id) REFERENCES dbo.papers(id),
        CONSTRAINT FK_pq_question FOREIGN KEY (question_id) REFERENCES dbo.questions(id)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_pq_paper' AND parent_object_id = OBJECT_ID('dbo.paper_questions'))
BEGIN
    ALTER TABLE dbo.paper_questions ADD CONSTRAINT FK_pq_paper FOREIGN KEY (paper_id) REFERENCES dbo.papers(id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_pq_question' AND parent_object_id = OBJECT_ID('dbo.paper_questions'))
BEGIN
    ALTER TABLE dbo.paper_questions ADD CONSTRAINT FK_pq_question FOREIGN KEY (question_id) REFERENCES dbo.questions(id);
END
GO

-- ========================================
-- 5. 考试记录表
-- ========================================
IF OBJECT_ID('dbo.exam_records', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.exam_records (
        id INT IDENTITY(1,1) PRIMARY KEY,
        student_id INT,
        paper_id INT,
        start_time DATETIME2,
        submit_time DATETIME2,
        total_score DECIMAL(5,1),
        CONSTRAINT FK_er_student FOREIGN KEY (student_id) REFERENCES dbo.users(id),
        CONSTRAINT FK_er_paper FOREIGN KEY (paper_id) REFERENCES dbo.papers(id)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_er_student' AND parent_object_id = OBJECT_ID('dbo.exam_records'))
BEGIN
    ALTER TABLE dbo.exam_records ADD CONSTRAINT FK_er_student FOREIGN KEY (student_id) REFERENCES dbo.users(id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_er_paper' AND parent_object_id = OBJECT_ID('dbo.exam_records'))
BEGIN
    ALTER TABLE dbo.exam_records ADD CONSTRAINT FK_er_paper FOREIGN KEY (paper_id) REFERENCES dbo.papers(id);
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'idx_exam_records_student'
      AND object_id = OBJECT_ID('dbo.exam_records')
)
BEGIN
    CREATE NONCLUSTERED INDEX idx_exam_records_student ON dbo.exam_records(student_id);
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'idx_exam_records_paper'
      AND object_id = OBJECT_ID('dbo.exam_records')
)
BEGIN
    CREATE NONCLUSTERED INDEX idx_exam_records_paper ON dbo.exam_records(paper_id);
END
GO

-- ========================================
-- 6. 答题明细表
-- ========================================
IF OBJECT_ID('dbo.answer_details', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.answer_details (
        id INT IDENTITY(1,1) PRIMARY KEY,
        record_id INT,
        question_id INT,
        student_answer NVARCHAR(MAX),
        is_correct BIT,
        score_got DECIMAL(5,1),
        CONSTRAINT FK_ad_record FOREIGN KEY (record_id) REFERENCES dbo.exam_records(id),
        CONSTRAINT FK_ad_question FOREIGN KEY (question_id) REFERENCES dbo.questions(id)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_ad_record' AND parent_object_id = OBJECT_ID('dbo.answer_details'))
BEGIN
    ALTER TABLE dbo.answer_details ADD CONSTRAINT FK_ad_record FOREIGN KEY (record_id) REFERENCES dbo.exam_records(id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_ad_question' AND parent_object_id = OBJECT_ID('dbo.answer_details'))
BEGIN
    ALTER TABLE dbo.answer_details ADD CONSTRAINT FK_ad_question FOREIGN KEY (question_id) REFERENCES dbo.questions(id);
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'idx_answer_details_record'
      AND object_id = OBJECT_ID('dbo.answer_details')
)
BEGIN
    CREATE NONCLUSTERED INDEX idx_answer_details_record ON dbo.answer_details(record_id);
END
GO

-- ========================================
-- 7. 班级表
-- ========================================
IF OBJECT_ID('dbo.class_rooms', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.class_rooms (
        id INT IDENTITY(1,1) PRIMARY KEY,
        name NVARCHAR(100) NOT NULL,
        teacher_id INT NOT NULL,
        join_code NVARCHAR(32) NOT NULL,
        created_at DATETIME2 NULL,
        CONSTRAINT UQ_class_rooms_join_code UNIQUE(join_code),
        CONSTRAINT FK_class_rooms_teacher FOREIGN KEY (teacher_id) REFERENCES dbo.users(id)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = 'UQ_class_rooms_join_code' AND parent_object_id = OBJECT_ID('dbo.class_rooms'))
BEGIN
    ALTER TABLE dbo.class_rooms ADD CONSTRAINT UQ_class_rooms_join_code UNIQUE(join_code);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_class_rooms_teacher' AND parent_object_id = OBJECT_ID('dbo.class_rooms'))
BEGIN
    ALTER TABLE dbo.class_rooms ADD CONSTRAINT FK_class_rooms_teacher FOREIGN KEY (teacher_id) REFERENCES dbo.users(id);
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'idx_class_rooms_teacher'
      AND object_id = OBJECT_ID('dbo.class_rooms')
)
BEGIN
    CREATE NONCLUSTERED INDEX idx_class_rooms_teacher ON dbo.class_rooms(teacher_id);
END
GO

-- ========================================
-- 8. 班级成员表
-- ========================================
IF OBJECT_ID('dbo.class_members', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.class_members (
        id INT IDENTITY(1,1) PRIMARY KEY,
        class_id INT NOT NULL,
        student_id INT NOT NULL,
        joined_at DATETIME2 NULL,
        CONSTRAINT UQ_class_members_class_student UNIQUE(class_id, student_id),
        CONSTRAINT FK_class_members_class FOREIGN KEY (class_id) REFERENCES dbo.class_rooms(id),
        CONSTRAINT FK_class_members_student FOREIGN KEY (student_id) REFERENCES dbo.users(id)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = 'UQ_class_members_class_student' AND parent_object_id = OBJECT_ID('dbo.class_members'))
BEGIN
    ALTER TABLE dbo.class_members ADD CONSTRAINT UQ_class_members_class_student UNIQUE(class_id, student_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_class_members_class' AND parent_object_id = OBJECT_ID('dbo.class_members'))
BEGIN
    ALTER TABLE dbo.class_members ADD CONSTRAINT FK_class_members_class FOREIGN KEY (class_id) REFERENCES dbo.class_rooms(id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_class_members_student' AND parent_object_id = OBJECT_ID('dbo.class_members'))
BEGIN
    ALTER TABLE dbo.class_members ADD CONSTRAINT FK_class_members_student FOREIGN KEY (student_id) REFERENCES dbo.users(id);
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'idx_class_members_class'
      AND object_id = OBJECT_ID('dbo.class_members')
)
BEGIN
    CREATE NONCLUSTERED INDEX idx_class_members_class ON dbo.class_members(class_id);
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'idx_class_members_student'
      AND object_id = OBJECT_ID('dbo.class_members')
)
BEGIN
    CREATE NONCLUSTERED INDEX idx_class_members_student ON dbo.class_members(student_id);
END
GO

-- ========================================
-- 9. 试卷发布范围表
-- ========================================
IF OBJECT_ID('dbo.paper_targets', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.paper_targets (
        id INT IDENTITY(1,1) PRIMARY KEY,
        paper_id INT NOT NULL,
        target_type NVARCHAR(20) NOT NULL,
        target_id INT NULL,
        CONSTRAINT FK_paper_targets_paper FOREIGN KEY (paper_id) REFERENCES dbo.papers(id),
        CONSTRAINT CK_paper_targets_type CHECK (target_type IN ('ALL','CLASS','STUDENT'))
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_paper_targets_type' AND parent_object_id = OBJECT_ID('dbo.paper_targets'))
BEGIN
    ALTER TABLE dbo.paper_targets ADD CONSTRAINT CK_paper_targets_type CHECK (target_type IN ('ALL','CLASS','STUDENT'));
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_paper_targets_paper' AND parent_object_id = OBJECT_ID('dbo.paper_targets'))
BEGIN
    ALTER TABLE dbo.paper_targets ADD CONSTRAINT FK_paper_targets_paper FOREIGN KEY (paper_id) REFERENCES dbo.papers(id);
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'idx_paper_targets_paper'
      AND object_id = OBJECT_ID('dbo.paper_targets')
)
BEGIN
    CREATE NONCLUSTERED INDEX idx_paper_targets_paper ON dbo.paper_targets(paper_id);
END
GO

PRINT '数据库初始化完成！所有表已创建或已确认存在。';
GO
