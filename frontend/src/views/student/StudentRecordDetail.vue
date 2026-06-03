<template>
  <div class="student-page">
    <header class="page-header">
      <router-link class="secondary-btn" to="/student/records">返回记录</router-link>
      <router-link class="secondary-btn" to="/student">返回首页</router-link>
    </header>

    <section class="panel">
      <p v-if="loading" class="state-text">加载中...</p>
      <p v-else-if="error" class="error-text">{{ error }}</p>

      <template v-else-if="record">
        <div class="record-summary">
          <div>
            <h1>{{ record.paperTitle || '考试详情' }}</h1>
            <p>开始时间：{{ formatTime(record.startTime) }}</p>
            <p>提交时间：{{ record.submitTime ? formatTime(record.submitTime) : '未提交' }}</p>
          </div>
          <strong>{{ record.totalScore ?? 0 }} 分</strong>
        </div>

        <div v-if="record.answers?.length" class="answer-list">
          <article v-for="(answer, index) in record.answers" :key="answer.questionId" class="answer-card">
            <div class="answer-head">
              <strong>第 {{ index + 1 }} 题</strong>
              <span>{{ typeLabel(answer.type) }} · 得分 {{ answer.scoreGot ?? 0 }} / {{ answer.score ?? 0 }}</span>
            </div>
            <p class="question-content">{{ answer.content }}</p>
            <pre v-if="answer.options" class="options-text">{{ answer.options }}</pre>
            <div class="student-answer">
              <span>我的答案</span>
              <p>{{ answer.studentAnswer || '未作答' }}</p>
            </div>
            <p class="result-text">{{ resultText(answer) }}</p>
          </article>
        </div>
        <p v-else class="state-text">暂无答题明细。</p>
      </template>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getExamRecordDetail } from '../../api/student'

const route = useRoute()
const record = ref(null)
const loading = ref(false)
const error = ref('')

const labels = {
  single_choice: '单选题',
  multi_choice: '多选题',
  true_false: '判断题',
  fill_blank: '填空题',
  short_answer: '简答题'
}

function typeLabel(type) {
  return labels[type] || type || '题目'
}

function formatTime(value) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

function resultText(answer) {
  if (answer.type === 'short_answer') {
    return '主观题待教师批改'
  }
  if (answer.isCorrect === true) {
    return '自动判分：正确'
  }
  if (answer.isCorrect === false) {
    return '自动判分：错误'
  }
  return '未判分'
}

async function loadRecord() {
  loading.value = true
  error.value = ''
  try {
    const res = await getExamRecordDetail(route.params.recordId)
    if (res.code === 200) {
      record.value = res.data
    } else {
      error.value = res.message || '获取考试记录详情失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '获取考试记录详情失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadRecord)
</script>

<style scoped>
.student-page {
  min-height: 100vh;
  background: #f5f7fb;
  padding: 32px;
}

.page-header,
.panel {
  max-width: 980px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
}

.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 24px;
}

.record-summary {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 22px;
}

.record-summary strong {
  color: #111827;
  font-size: 28px;
}

h1,
p {
  margin: 0;
}

h1 {
  color: #111827;
  font-size: 26px;
  margin-bottom: 8px;
}

.record-summary p,
.state-text {
  color: #6b7280;
}

.answer-list {
  display: grid;
  gap: 12px;
}

.answer-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
}

.answer-head {
  display: flex;
  justify-content: space-between;
  color: #374151;
  margin-bottom: 10px;
}

.question-content {
  color: #111827;
  line-height: 1.6;
}

.options-text {
  background: #f9fafb;
  border-radius: 6px;
  color: #374151;
  font-family: inherit;
  margin: 12px 0;
  padding: 12px;
  white-space: pre-wrap;
}

.student-answer {
  background: #f9fafb;
  border-radius: 6px;
  margin-top: 12px;
  padding: 12px;
}

.student-answer span {
  color: #6b7280;
  display: block;
  font-size: 13px;
  margin-bottom: 6px;
}

.result-text {
  color: #2563eb;
  margin-top: 10px;
}

.secondary-btn {
  background: #fff;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  color: #374151;
  cursor: pointer;
  font-size: 14px;
  padding: 9px 14px;
  text-decoration: none;
}

.error-text {
  color: #dc2626;
}

@media (max-width: 720px) {
  .student-page {
    padding: 20px;
  }

  .record-summary,
  .answer-head {
    flex-direction: column;
  }
}
</style>
