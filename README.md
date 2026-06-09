# 在线考试系统

Spring Boot 3 + Vue 3 在线考试系统。默认数据库为 SQL Server；H2 仅作为可选的本地开发/演示 profile。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2.5, Spring Data JPA, Hibernate |
| 数据库 | SQL Server 默认；H2 可选 |
| 认证 | JWT, BCrypt |
| 前端 | Vue 3, Vue Router 4, Vite 5, Axios |
| 构建 | Maven, npm |

## 后端配置

默认配置位于 `backend/src/main/resources/application.yml`，默认连接 SQL Server，并支持以下环境变量覆盖：

| 变量 | 说明 |
|------|------|
| `DB_URL` | SQL Server JDBC 地址 |
| `DB_USERNAME` | 数据库用户名 |
| `DB_PASSWORD` | 数据库密码 |
| `JWT_SECRET` | JWT 签名密钥 |
| `JWT_EXPIRATION` | JWT 过期时间，单位毫秒 |

PowerShell 示例：

```powershell
$env:DB_URL="jdbc:sqlserver://localhost:1433;databaseName=exam_system;encrypt=false;trustServerCertificate=true"
$env:DB_USERNAME="sa"
$env:DB_PASSWORD=""
$env:JWT_SECRET="change-this-to-a-long-random-secret"
$env:JWT_EXPIRATION="86400000"
```

Windows CMD 示例：

```bat
set DB_URL=jdbc:sqlserver://localhost:1433;databaseName=exam_system;encrypt=false;trustServerCertificate=true
set DB_USERNAME=sa
set DB_PASSWORD=
set JWT_SECRET=change-this-to-a-long-random-secret
set JWT_EXPIRATION=86400000
```

不要把真实数据库密码或生产 JWT 密钥提交到仓库。

## H2 开发 Profile

如需使用 H2 内存数据库进行本地演示，启动后端时显式启用 `h2` profile：

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

或在打包后运行：

```bash
java -jar target/exam-system-1.0.0.jar --spring.profiles.active=h2
```

H2 控制台路径为 `/h2-console`。默认启动不再连接 H2，避免与 SSMS 中的 SQL Server 数据不一致。

## 数据库初始化

默认使用 SQL Server。首次部署可在 SQL Server Management Studio 中打开并执行：

```text
database/init.sql
```

脚本会创建 `exam_system` 数据库，并以幂等方式创建或确认表、字段、索引、外键和约束。重复执行不应因对象已存在而失败。

核心表：

| 表 | 说明 |
|----|------|
| `users` | 用户、密码哈希、角色、账号状态 |
| `questions` | 题库 |
| `papers` | 试卷、开放时间、答案开放、发布状态、排行榜公开 |
| `paper_questions` | 试卷题目与分值 |
| `exam_records` | 考试记录 |
| `answer_details` | 答题明细与得分 |
| `class_rooms` | 教师创建的班级与邀请码 |
| `class_members` | 班级学生 |
| `paper_targets` | 试卷发布范围：`ALL` / `CLASS` / `STUDENT` |

## 当前功能

- 登录注册：登录必须同时校验用户名、密码、角色和账号状态；禁用账号不能登录。
- 管理员：查看系统概览、按角色分页查看用户、修改角色、重置密码、启用或禁用用户。当前登录管理员不能禁用自己的账号。
- 学生端：查看已发布且分配给自己的试卷、开始或继续作答、提交答案、查看记录、加入班级、查看公开排行榜。
- 教师端：题库管理、试卷管理、编辑组卷、设置答案开放、发布试卷、设置发布范围、班级管理、阅卷、统计平均分、查看排行榜、控制排行榜是否公开。
- 分值同步：教师修改试卷题目分值后，已提交记录会同步重算 `answer_details.score_got` 与 `exam_records.total_score`。

## 主要业务规则

- 试卷默认是草稿，学生端不可见。
- 发布试卷前必须满足：标题非空、至少一道题、每题分值大于等于 0、发布范围已设置或明确选择全部学生。
- 学生只能访问已发布且命中发布范围的试卷。
- 作答次数包含未提交记录；如果存在未提交记录，开始考试会恢复该记录并允许继续作答。
- 排行榜和统计只统计已提交记录；同一学生同一试卷多次提交时取最高分，分数相同按更早提交时间排名更靠前。
- 学生端排行榜只有在 `leaderboardPublic=true` 且学生有试卷访问权限时可见。
- 答案开放以 `releaseAnswerFlag` 为主，`answerReleaseTime` 控制公布时间；学生 DTO 中兼容返回 `teacherOpenAnswer`。

## 常用接口

### 用户

- `POST /api/user/register`
- `POST /api/user/login`，请求体包含 `{ username, password, role }`

### 管理员

- `GET /api/admin/overview`
- `GET /api/admin/users`
- `PUT /api/admin/users/{id}`
- `PUT /api/admin/users/{id}/reset-password`
- `PUT /api/admin/users/{id}/toggle-status`

### 教师

- `GET/POST/PUT/DELETE /api/teacher/questions`
- `GET/POST/PUT/DELETE /api/teacher/papers`
- `GET /api/teacher/papers/{paperId}/questions`
- `POST /api/teacher/papers/{paperId}/questions`
- `POST /api/teacher/papers/{paperId}/publish`
- `PUT /api/teacher/papers/{paperId}/leaderboard-visibility`
- `GET /api/teacher/classes`
- `POST /api/teacher/classes`
- `GET /api/teacher/classes/{classId}`
- `POST /api/teacher/classes/{classId}/students`
- `DELETE /api/teacher/classes/{classId}/students/{studentId}`
- `GET /api/teacher/students`
- `GET /api/teacher/papers/{paperId}/statistics`
- `GET /api/teacher/papers/{paperId}/leaderboard`

### 学生

- `GET /api/student/papers`
- `GET /api/student/papers/{paperId}`
- `POST /api/student/exam-records`
- `POST /api/student/exam-records/{recordId}/submit`
- `GET /api/student/exam-records`
- `GET /api/student/exam-records/{recordId}`
- `GET /api/student/classes`
- `POST /api/student/classes/join`
- `GET /api/student/papers/{paperId}/leaderboard`

## 构建

```bash
cd backend
mvn clean package -DskipTests

cd ../frontend
npm install
npm run build
```

后端接口统一返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```
