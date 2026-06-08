import request from '../utils/request'

// ==================== 题库管理 ====================

// 获取题目列表
export function getQuestions(params) {
  return request.get('/api/teacher/questions', { params })
}

// 新增题目
export function createQuestion(data) {
  return request.post('/api/teacher/questions', data)
}

// 修改题目
export function updateQuestion(id, data) {
  return request.put(`/api/teacher/questions/${id}`, data)
}

// 删除题目
export function deleteQuestion(id) {
  return request.delete(`/api/teacher/questions/${id}`)
}

// ==================== 试卷管理 ====================

// 获取试卷列表
export function getPapers() {
  return request.get('/api/teacher/papers')
}

// 获取试卷详情
export function getPaper(paperId) {
  return request.get(`/api/teacher/papers/${paperId}`)
}

// 新增试卷
export function createPaper(data) {
  return request.post('/api/teacher/papers', data)
}

// 修改试卷
export function updatePaper(id, data) {
  return request.put(`/api/teacher/papers/${id}`, data)
}

// 删除试卷
export function deletePaper(id) {
  return request.delete(`/api/teacher/papers/${id}`)
}

// 给试卷组题
export function assignQuestions(paperId, questions) {
  return request.post(`/api/teacher/papers/${paperId}/questions`, questions)
}

// ==================== 阅卷评分 ====================

// 查看考试记录
export function getExamRecords(params) {
  return request.get('/api/teacher/exam-records', { params })
}

// 查看考试记录详情
export function getExamRecordDetail(recordId) {
  return request.get(`/api/teacher/exam-records/${recordId}`)
}

// 批改简答题
export function gradeQuestion(recordId, data) {
  return request.post(`/api/teacher/exam-records/${recordId}/grade`, data)
}
