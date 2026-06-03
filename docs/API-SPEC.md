# API 接口规范

## 通用说明

- 统一返回格式：`{ code: 200, message: "success", data: ... }`
- 所有接口（除登录/注册）需带 `Authorization: Bearer <token>` header
- 基础路径：`http://localhost:8080`

## 认证接口

### POST /api/user/login
- 请求体：`{ username, password }`
- 返回：`{ code, message, data: { token, role } }`

### POST /api/user/register
- 请求体：`{ username, password, role }`
- role 可选值：`student`、`teacher`、`admin`

---

## 教师模块 `/api/teacher`

### POST /api/teacher/questions
创建题目
- 请求体：`{ type, content, options, answer, courseId? }`
- type：`single_choice | multi_choice | true_false | fill_blank | short_answer`

### GET /api/teacher/questions
分页查题库
- 参数：`?page=1&size=20&type=&courseId=`
- 返回：`{ code, message, data: { list: [...], total, page, size } }`

### PUT /api/teacher/questions/{id}
修改题目
- 请求体：`{ type?, content?, options?, answer?, courseId? }`

### DELETE /api/teacher/questions/{id}
删除题目

### POST /api/teacher/papers
创建试卷
- 请求体：`{ title, duration, questionIds?: [...], teacherOpenAnswer?: boolean, openStartTime?: "2026-06-02T08:00:00", openEndTime?: "2026-06-02T09:00:00" }`
- 字段约定：
  - `openStartTime`：允许学生开始作答的时间；为空表示不限制开始时间。
  - `openEndTime`：考试截止时间；为空表示不限制截止时间。
  - `teacherOpenAnswer`：是否允许学生在截止时间后查看标准答案/参考答案。

### GET /api/teacher/papers
试卷列表
- 返回：`{ code, message, data: [{ id, title, duration, teacherOpenAnswer, openStartTime, openEndTime, ... }] }`

### PUT /api/teacher/papers/{id}
编辑试卷
- 请求体：`{ title?, duration?, teacherOpenAnswer?, openStartTime?, openEndTime? }`
- 注意：教师端尚未实现页面时，可以先直接维护 `papers.teacher_open_answer`、`papers.open_start_time`、`papers.open_end_time` 字段。

### DELETE /api/teacher/papers/{id}
删除试卷

### POST /api/teacher/papers/{paperId}/questions
向试卷添加题目
- 请求体：`{ questionId, score }` 或 `{ questions: [{ questionId, score }] }`

### GET /api/teacher/exam-records
查看考试记录
- 参数：`?paperId=`

### POST /api/teacher/exam-records/{recordId}/grade`
批改主观题
- 请求体：`{ details: [{ questionId, scoreGot }] }`

---

## 学生模块 `/api/student`

### GET /api/student/papers
可参加的考试列表
- 返回：`{ code, message, data: [{ paperId, title, duration, openStartTime, openEndTime, status, statusText, attemptCount, remainingAttempts, bestScore }] }`
- `status`：
  - `NOT_OPEN`：当前时间早于 `openStartTime`
  - `OPEN`：当前时间在开放窗口内，且仍有作答次数
  - `CLOSED`：当前时间已到达或超过 `openEndTime`
  - `NO_ATTEMPTS`：开放窗口内但作答次数已用完

### GET /api/student/papers/{paperId}
获取试卷详情（含题目，不含答案）

### POST /api/student/exam-records
开始考试
- 请求体：`{ paperId }`
- 仅当试卷状态为 `OPEN` 时允许开始考试。

### POST /api/student/exam-records/{recordId}/submit`
提交答案
- 请求体：`{ answers: [{ questionId, answer }] }`
- 后端会校验当前时间仍在试卷开放窗口内；若已到达 `openEndTime`，拒绝提交。

### GET /api/student/exam-records
我的考试记录列表

### GET /api/student/exam-records/{recordId}
考试记录详情（含得分）
- 返回新增字段：`teacherOpenAnswer`、`openStartTime`、`openEndTime`、`referenceAnswerMap`。
- 答案展示约定：
  - 只有当 `teacherOpenAnswer === true` 且当前时间已到达或超过 `openEndTime` 后，接口才返回客观题 `correctAnswer` 和主观题 `referenceAnswerMap`。
  - 若 `openEndTime` 为空，则兼容旧逻辑：提交后或 `startTime + duration` 后可展示答案。
