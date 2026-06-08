# 在线考试系统

Spring Boot 3 + Vue 3 在线考试系统，后端使用 SQL Server，前端使用 Vue Router 和 Axios。系统支持学生参加考试、继续未提交考试、查看考试记录；教师管理题库、试卷、组卷和阅卷；管理员模块保留基础入口。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2.5, Spring Data JPA, Hibernate |
| 数据库 | SQL Server 默认；H2 仅作为本地可选测试方案 |
| 认证 | JWT, BCrypt |
| 前端 | Vue 3, Vue Router 4, Vite 5, Axios |
| 构建 | Maven, npm |

## 项目结构

```text
exam-system/
  backend/
    pom.xml
    src/main/java/com/exam/
      common/        # Result, UserContext, 全局异常处理
      config/        # CORS, JWT 拦截器, Web 配置
      controller/    # user/student/teacher/admin 接口
      dto/           # 前后端传输对象
      entity/        # User, Question, Paper, ExamRecord 等实体
      repository/    # Spring Data JPA Repository
      service/       # 用户、学生端、教师端业务逻辑
      utils/         # JWT 工具
    src/main/resources/application.yml
  frontend/
    src/
      api/           # Axios API 封装
      components/    # 通用组件
      router/        # 路由与权限守卫
      utils/         # request/auth 等工具
      views/         # 登录、学生端、教师端、管理员页面
  database/
    init.sql         # SQL Server 初始化脚本
```

## 主要功能

- 用户注册、登录、JWT 鉴权和角色路由保护。
- 学生端：查看可参加考试、查看试卷详情、开始/继续未提交考试、提交答案、查看考试记录和答案解析。
- 教师端：题库管理、试卷管理、编辑组卷、设置开放时间和最大作答次数、设置答案公布、查看考试记录、批改简答题。
- 管理员端：基础入口和后续扩展位置。

## 配置

默认配置位于 `backend/src/main/resources/application.yml`，通过环境变量覆盖：

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

不要把真实数据库密码或生产 JWT 密钥提交到仓库。可以使用本地未跟踪文件或终端环境变量保存私密配置。

## 启动

### 后端

```bash
cd backend
mvn spring-boot:run
```

服务默认运行在 `http://localhost:8080`。

### 前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，开发代理会把 `/api` 请求转发到后端。

## 构建验证

```bash
cd backend
mvn clean package -DskipTests

cd ../frontend
npm install
npm run build
```

## 数据库说明

默认使用 SQL Server。首次使用可执行 `database/init.sql`，也可依赖 JPA `ddl-auto: update` 在开发环境自动维护表结构。

H2 只建议用于临时本地测试。如需使用 H2，需要额外添加 H2 runtime 依赖，并将 datasource、driver 和 dialect 改为 H2 对应配置。

## 统一响应格式

后端接口统一返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

常见状态码：

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 业务异常 |
| 401 | 未登录或 token 无效 |
| 500 | 服务端异常 |
