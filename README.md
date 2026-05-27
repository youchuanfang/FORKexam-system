# 在线考试系统

Spring Boot 3 + Vue 3 全栈在线考试平台，支持学生参加考试、教师管理题库试卷、管理员系统管理。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.5 |
| 数据库 | SQL Server（开发可用 H2） |
| ORM | Spring Data JPA + Hibernate |
| 认证 | JWT（jjwt 0.12.x） |
| 密码加密 | BCrypt（Spring Security Crypto） |
| 前端框架 | Vue 3 + Vue Router 4 |
| 构建工具 | Vite 5 + Maven |
| HTTP 客户端 | Axios |

## 项目结构

```
exam-system/
├── backend/                        # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/exam/
│       ├── ExamApplication.java    # 启动类
│       ├── common/
│       │   ├── Result.java         # 统一响应体 { code, message, data }
│       │   ├── UserContext.java    # ThreadLocal 用户上下文
│       │   └── GlobalExceptionHandler.java
│       ├── config/
│       │   ├── CorsConfig.java     # 跨域配置
│       │   ├── JwtInterceptor.java # JWT 鉴权拦截器
│       │   ├── SecurityConfig.java # BCrypt Bean
│       │   └── WebConfig.java      # 拦截器注册
│       ├── controller/
│       │   └── UserController.java # /api/user/login, /register
│       ├── entity/
│       │   └── User.java           # 用户实体
│       ├── repository/
│       │   └── UserRepository.java
│       ├── service/
│       │   ├── UserService.java    # 接口
│       │   └── impl/
│       │       └── UserServiceImpl.java
│       └── utils/
│           └── JwtUtil.java        # JWT 生成/解析/校验
├── frontend/                       # Vue 3 前端
│   ├── vite.config.js              # API 代理配置
│   └── src/
│       ├── main.js                 # 入口
│       ├── App.vue
│       ├── style.css
│       ├── router/index.js         # 路由 + 守卫
│       ├── utils/request.js        # Axios 封装 + 拦截器
│       ├── api/                    # API 调用封装
│       └── views/
│           ├── Login.vue           # 登录 + 注册
│           ├── StudentHome.vue
│           ├── TeacherHome.vue
│           └── AdminHome.vue
└── database/
    └── init.sql                    # SQL Server 建库建表脚本
```

## 数据库表

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| users | 用户 | username(唯一), password(BCrypt), role(student/teacher/admin) |
| questions | 题目 | type(5种题型), content, options, answer, course_id |
| papers | 试卷 | title, duration, created_by(FK→users) |
| paper_questions | 试卷-题目关联 | paper_id, question_id, score |
| exam_records | 考试记录 | student_id, paper_id, start_time, submit_time, total_score |
| answer_details | 答题明细 | record_id, question_id, student_answer, is_correct, score_got |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.9+
- SQL Server（或使用 H2 内存数据库测试）
- Node.js 18+

### 1. 初始化数据库

在 SQL Server 中执行 `database/init.sql`，或启动应用后由 JPA `ddl-auto: update` 自动建表。

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

应用运行在 `http://localhost:8080`。

**使用 H2 内存数据库（无需安装 SQL Server）：**

修改 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:exam_system;DB_CLOSE_DELAY=-1;MODE=MSSQLServer
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
```

并在 `pom.xml` 中添加 H2 依赖：

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端运行在 `http://localhost:5173`，API 请求自动代理到后端 8080 端口。

## API 接口

### 公开接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/user/register` | 用户注册 `{ username, password, role }` |
| POST | `/api/user/login` | 用户登录 `{ username, password }` → `{ token, role }` |

### 认证方式

登录后获得 JWT token，后续请求在 Header 中携带：

```
Authorization: Bearer <token>
```

### 响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 业务异常 |
| 401 | 未登录/token 无效 |
| 500 | 服务器内部错误 |

## 角色权限

| 角色 | 权限 |
|------|------|
| student | 查看试卷、参加考试、查看成绩 |
| teacher | 管理题库、管理试卷、查看学生成绩、批改试卷 |
| admin | 用户管理、系统设置 |

## 开发状态

- [x] 项目框架搭建
- [x] 用户认证（注册/登录/JWT）
- [ ] 学生模块（考试、成绩）
- [ ] 教师模块（题库、试卷、批改）
- [ ] 管理员模块（用户管理）
