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
          <div class="record-side">
            <strong>{{ record.totalScore ?? 0 }} 分</strong>
            <button
              v-if="record.submitTime"
              class="secondary-btn export-btn"
              type="button"
              :disabled="exporting"
              @click="handleExportWrong"
            >{{ exporting ? '导出中...' : '导出错题 Excel' }}</button>
          </div>
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
            <div v-if="shouldShowCorrectAnswer(answer)" class="correct-answer">
              <span>{{ answer.type === 'short_answer' ? '参考答案' : '正确答案' }}</span>
              <p>{{ getCorrectAnswer(answer) }}</p>
            </div>
          </article>
        </div>
        <p v-else class="state-text">暂无答题明细。</p>

        <div v-if="record.leaderboardPublic" class="leaderboard-panel">
          <h3>排行榜</h3>
          <p v-if="leaderboardLoading" class="state-text">加载排行榜中...</p>
          <div v-else class="leaderboard-list">
            <div v-for="item in leaderboard" :key="`${item.rank}-${item.username}`" class="leaderboard-row">
              <span>#{{ item.rank }}</span>
              <span>{{ item.username }}</span>
              <strong>{{ item.score ?? 0 }} 分</strong>
              <small>{{ formatTime(item.submitTime) }}</small>
            </div>
            <p v-if="leaderboard.length === 0" class="state-text">暂无排行榜数据。</p>
          </div>
        </div>
      </template>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getExamRecordDetail, getLeaderboard, exportWrongQuestions } from '../../api/student'

const route = useRoute()
const record = ref(null)
const loading = ref(false)
const error = ref('')
const leaderboard = ref([])
const leaderboardLoading = ref(false)
const exporting = ref(false)

const labels = {
  single_choice: '单选题',
  multi_choice: '多选题',
  true_false: '判断题',
  fill_blank: '填空题',
  short_answer: '简答题'
}

const objectiveTypes = ['single_choice', 'multi_choice', 'true_false', 'fill_blank']

function typeLabel(type) {
  return labels[type] || type || '题目'
}

function formatTime(value) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

function getCorrectAnswer(answer) {
  if (!answer) return ''
  if (answer.type === 'short_answer') {
    return record.value?.referenceAnswerMap?.[answer.questionId] || ''
  }
  return answer.correctAnswer || ''
}

function shouldShowCorrectAnswer(answer) {
  if (record.value?.teacherOpenAnswer !== true) {
    return false
  }
  if (answer.type === 'short_answer') {
    return Boolean(getCorrectAnswer(answer))
  }
  return objectiveTypes.includes(answer.type) && Boolean(getCorrectAnswer(answer))
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
      if (record.value?.leaderboardPublic) {
        await loadLeaderboard()
      }
    } else {
      error.value = res.message || '获取考试记录详情失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '获取考试记录详情失败'
  } finally {
    loading.value = false
  }
}

async function handleExportWrong() {
  exporting.value = true
  try {
    const res = await exportWrongQuestions(route.params.recordId)
    const url = window.URL.createObjectURL(new Blob([res]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `wrong_questions_${route.params.recordId}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch (err) {
    error.value = '导出失败'
  } finally {
    exporting.value = false
  }
}

async function loadLeaderboard() {
  leaderboardLoading.value = true
  try {
    const res = await getLeaderboard(record.value.paperId)
    leaderboard.value = res.code === 200 ? (res.data || []) : []
  } finally {
    leaderboardLoading.value = false
  }
}

onMounted(loadRecord)
</script>

<style scoped>
.student-page { min-height: 100vh; background: #f5f7fb; padding: 32px; }
.page-header, .panel { max-width: 980px; margin: 0 auto; }
.page-header { display: flex; gap: 10px; margin-bottom: 18px; }
.panel { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 24px; }
.record-summary { display: flex; justify-content: space-between; gap: 18px; margin-bottom: 22px; }
.record-summary strong { color: #111827; font-size: 28px; }
.record-side { display: flex; flex-direction: column; align-items: flex-end; gap: 10px; }
.export-btn { font-size: 13px !important; }
h1, p { margin: 0; }
h1 { color: #111827; font-size: 26px; margin-bottom: 8px; }
.record-summary p, .state-text { color: #6b7280; }
.answer-list { display: grid; gap: 12px; }
.answer-card { border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; }
.answer-head { display: flex; justify-content: space-between; color: #374151; margin-bottom: 10px; }
.question-content { color: #111827; line-height: 1.6; }
.options-text { background: #f9fafb; border-radius: 6px; color: #374151; font-family: inherit; margin: 12px 0; padding: 12px; white-space: pre-wrap; }
.student-answer { background: #f9fafb; border-radius: 6px; margin-top: 12px; padding: 12px; }
.student-answer span { color: #6b7280; display: block; font-size: 13px; margin-bottom: 6px; }
.result-text { color: #2563eb; margin-top: 10px; }
.correct-answer { background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 6px; margin-top: 12px; padding: 12px; }
.correct-answer span { color: #15803d; display: block; font-size: 13px; margin-bottom: 6px; }
.correct-answer p { color: #166534; white-space: pre-wrap; }
.secondary-btn { background: #fff; border: 1px solid #d1d5db; border-radius: 6px; color: #374151; cursor: pointer; font-size: 14px; padding: 9px 14px; text-decoration: none; }
.error-text { color: #dc2626; }
.leaderboard-panel { border-top: 1px solid #e5e7eb; margin-top: 18px; padding-top: 18px; }
.leaderboard-panel h3 { color: #111827; margin: 0 0 10px; }
.leaderboard-list { display: grid; gap: 6px; }
.leaderboard-row { align-items: center; background: #f9fafb; border-radius: 6px; display: grid; gap: 10px; grid-template-columns: 54px 1fr 90px 170px; padding: 8px 10px; }
@media (max-width: 720px) {
  .student-page { padding: 20px; }
  .record-summary, .answer-head { flex-direction: column; }
  .leaderboard-row { grid-template-columns: 44px 1fr; }
}
</style>
