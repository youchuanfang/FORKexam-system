# 在线考试系统

Spring Boot 3 + Vue 3 在线考试系统，默认数据库为 SQL Server。系统支持用户登录注册、学生考试与记录、教师题库/试卷/组卷/阅卷、答案开放、试卷发布、班级发布范围、成绩统计和排行榜公开。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2.5, Spring Data JPA, Hibernate |
| 数据库 | SQL Server 默认；H2 仅作为可选本地测试方案 |
| 认证 | JWT, BCrypt |
| 前端 | Vue 3, Vue Router 4, Vite 5, Axios |
| 构建 | Maven, npm |

## 配置

后端配置位于 `backend/src/main/resources/application.yml`，支持环境变量覆盖：

| 变量 | 说明 |
|------|------|
| `DB_URL` | SQL Server JDBC 地址 |
| `DB_USERNAME` | 数据库用户名 |
| `DB_PASSWORD` | 数据库密码 |
| `JWT_SECRET` | JWT 签名密钥 |
| `JWT_EXPIRATION` | JWT 过期时间，单位毫秒 |

示例：

```powershell
$env:DB_URL="jdbc:sqlserver://localhost:1433;databaseName=exam_system;encrypt=false;trustServerCertificate=true"
$env:DB_USERNAME="sa"
$env:DB_PASSWORD=""
$env:JWT_SECRET="change-this-to-a-long-random-secret"
$env:JWT_EXPIRATION="86400000"
```

不要把真实数据库密码或生产 JWT 密钥提交到仓库。

## 项目结构

```text
exam-system/
  backend/
    src/main/java/com/exam/
      common/        # Result, UserContext, 全局异常处理
      config/        # CORS, JWT 拦截器, Web 配置
      controller/    # user/student/teacher 接口
      dto/           # 前后端传输对象
      entity/        # User, Question, Paper, ClassRoom 等实体
      repository/    # Spring Data JPA Repository
      service/       # 用户、学生端、教师端业务逻辑
      utils/         # JWT 工具
  frontend/
    src/
      api/           # Axios API 封装
      components/    # NavBar 等通用组件
      router/        # 路由与权限守卫
      utils/         # request/auth
      views/         # 登录、学生端、教师端、管理员页面
  database/
    init.sql         # SQL Server 初始化脚本
```

## 当前功能

- 登录注册：登录必须同时校验用户名、密码和前端选择角色；角色不匹配不会返回 token。
- 学生端：查看已发布且分配给自己的试卷、开始/继续作答、提交答案、查看记录、加入班级、查看公开排行榜。
- 教师端：题库管理、试卷管理、编辑组卷、设置答案开放、发布试卷、按全部学生/班级/学生设置发布范围、班级管理、阅卷、统计平均分、查看排行榜、控制排行榜是否公开。
- 分值同步：教师修改试卷题目分值后，已提交记录会同步重算 `answer_details.score_got` 与 `exam_records.total_score`。

## 主要业务规则

- 试卷默认是草稿，学生端完全不可见。
- 发布试卷前必须满足：标题非空、至少一道题、每题分值大于等于 0、发布范围已设置或明确选择全部学生。
- 学生只能访问已发布且命中发布范围的试卷。直接访问无权限试卷会返回“试卷未发布或无权限查看”。
- 作答次数包含未提交记录；如果存在未提交记录，开始考试会恢复该记录并允许继续作答。
- 排行榜和统计只统计已提交记录；同一学生同一试卷多次提交时取最高分，分数相同按更早提交时间排名更靠前。
- 学生端排行榜只有在 `leaderboardPublic=true` 且学生有该试卷访问权限时可见。
- 答案开放以 `releaseAnswerFlag` 为主，`answerReleaseTime` 控制公布时间；学生 DTO 中兼容返回 `teacherOpenAnswer`。

## 数据库

默认使用 SQL Server。首次部署可执行 `database/init.sql`；开发环境也可依赖 JPA `ddl-auto: update` 自动补列。

核心表：

| 表 | 说明 |
|----|------|
| `users` | 用户、密码哈希、角色 |
| `questions` | 题库 |
| `papers` | 试卷、开放时间、答案开放、发布状态、排行榜公开 |
| `paper_questions` | 试卷题目与分值 |
| `exam_records` | 考试记录 |
| `answer_details` | 答题明细与得分 |
| `class_rooms` | 教师创建的班级与邀请码 |
| `class_members` | 班级学生 |
| `paper_targets` | 试卷发布范围：`ALL` / `CLASS` / `STUDENT` |

## 常用接口

### 用户

- `POST /api/user/register`
- `POST /api/user/login`，请求体包含 `{ username, password, role }`

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
