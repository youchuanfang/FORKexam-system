<template>
  <div class="student-page">
    <header class="page-header">
      <button class="secondary-btn" type="button" @click="router.push('/student')">返回首页</button>
      <router-link class="secondary-btn" to="/student/records">考试记录</router-link>
    </header>

    <section class="panel">
      <p v-if="loading" class="state-text">加载中...</p>
      <p v-else-if="error" class="error-text">{{ error }}</p>

      <template v-else-if="paper">
        <div class="paper-summary">
          <div>
            <div class="title-row">
              <h1>{{ paper.title || '未命名试卷' }}</h1>
              <span :class="['status-pill', paper.status]">{{ paper.statusText || '考试开放中' }}</span>
            </div>
            <p>考试时长：{{ paper.duration || 0 }} 分钟</p>
            <p>开放时间：{{ formatOpenRange(paper) }}</p>
            <p>作答次数：{{ paper.attemptCount || 0 }} / {{ paper.maxAttempts || 1 }}</p>
            <p>剩余机会：{{ paper.remainingAttempts ?? 0 }}</p>
            <p>最高分：{{ paper.bestScore == null ? '未作答' : `${paper.bestScore} 分` }}</p>
          </div>
          <button class="primary-btn" type="button" :disabled="!canStart(paper) || starting" @click="handleStartExam">
            {{ actionText(paper) }}
          </button>
        </div>

        <p class="state-text">{{ paper.inProgressRecordId ? '已有未提交记录，点击继续作答会恢复该记录。' : '点击开始考试后进入答题页并加载题目。' }}</p>

        <div v-if="paper.leaderboardPublic" class="leaderboard-panel">
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
import { useRoute, useRouter } from 'vue-router'
import { getPaperDetail, startExam, getLeaderboard } from '../../api/student'

const route = useRoute()
const router = useRouter()
const paper = ref(null)
const loading = ref(false)
const starting = ref(false)
const error = ref('')
const leaderboard = ref([])
const leaderboardLoading = ref(false)

function normalizePaper(rawPaper) {
  const item = { ...rawPaper }
  const maxAttempts = Number(item.maxAttempts ?? 1)
  const attemptCount = Number(item.attemptCount ?? 0)
  const remainingAttempts = Number(item.remainingAttempts ?? Math.max(0, maxAttempts - attemptCount))

  item.maxAttempts = maxAttempts
  item.attemptCount = attemptCount
  item.remainingAttempts = remainingAttempts
  if (!item.status) {
    item.status = item.inProgressRecordId || remainingAttempts > 0 ? 'OPEN' : 'NO_ATTEMPTS'
  }
  if (!item.statusText) {
    item.statusText = item.inProgressRecordId ? '可继续作答' : (item.status === 'NO_ATTEMPTS' ? '作答次数已用完' : '考试开放中')
  }
  return item
}

function canStart(item) {
  return item.status === 'OPEN' && (item.inProgressRecordId || (item.remainingAttempts ?? 0) > 0)
}

function actionText(item) {
  if (starting.value) return item.inProgressRecordId ? '正在恢复...' : '正在开始...'
  if (item.status === 'NOT_OPEN') return '未开放'
  if (item.status === 'CLOSED') return '已关闭，不能作答'
  if (item.inProgressRecordId) return '继续作答'
  if ((item.remainingAttempts ?? 0) <= 0) return '作答次数已用完'
  return (item.attemptCount || 0) > 0 ? '再次作答' : '开始考试'
}

function formatTime(value) {
  return value ? value.replace('T', ' ').slice(0, 19) : ''
}

function formatOpenRange(item) {
  const start = formatTime(item.openStartTime)
  const end = formatTime(item.openEndTime)
  if (!start && !end) return '长期开放'
  if (start && end) return `${start} 至 ${end}`
  if (start) return `${start} 起`
  return `${end} 前`
}

function saveExamContext(data) {
  sessionStorage.setItem(`student_exam_${data.recordId}`, JSON.stringify(data))
}

async function loadPaper() {
  loading.value = true
  error.value = ''
  try {
    const res = await getPaperDetail(route.params.paperId)
    if (res.code === 200) {
      paper.value = normalizePaper(res.data)
      if (paper.value.leaderboardPublic) {
        await loadLeaderboard()
      }
    } else {
      error.value = res.message || '获取试卷详情失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '获取试卷详情失败'
  } finally {
    loading.value = false
  }
}

async function loadLeaderboard() {
  leaderboardLoading.value = true
  try {
    const res = await getLeaderboard(route.params.paperId)
    leaderboard.value = res.code === 200 ? (res.data || []) : []
  } finally {
    leaderboardLoading.value = false
  }
}

async function handleStartExam() {
  if (!paper.value || !canStart(paper.value)) return
  starting.value = true
  error.value = ''
  try {
    const res = await startExam(paper.value.paperId)
    if (res.code === 200) {
      saveExamContext(res.data)
      router.push(`/student/exam/${res.data.recordId}`)
    } else {
      error.value = res.message || '开始考试失败'
      await loadPaper()
    }
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '开始考试失败'
  } finally {
    starting.value = false
  }
}

onMounted(loadPaper)
</script>

<style scoped>
.student-page { min-height: 100vh; background: #f5f7fb; padding: 32px; }
.page-header, .panel { max-width: 980px; margin: 0 auto; }
.page-header { display: flex; gap: 10px; margin-bottom: 18px; }
.panel { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 24px; }
.paper-summary { display: flex; justify-content: space-between; gap: 18px; align-items: flex-start; margin-bottom: 22px; }
.title-row { align-items: center; display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 8px; }
h1, p { margin: 0; }
h1 { color: #111827; font-size: 26px; }
.paper-summary p, .state-text { color: #6b7280; line-height: 1.8; }
.status-pill { background: #eef2ff; border-radius: 999px; color: #3730a3; font-size: 12px; padding: 4px 9px; }
.status-pill.NOT_OPEN { background: #fff7ed; color: #c2410c; }
.status-pill.CLOSED, .status-pill.NO_ATTEMPTS { background: #f3f4f6; color: #4b5563; }
.primary-btn, .secondary-btn { border-radius: 6px; cursor: pointer; font-size: 14px; text-decoration: none; white-space: nowrap; }
.primary-btn { background: #2563eb; border: 0; color: #fff; padding: 10px 16px; }
.primary-btn:disabled { cursor: not-allowed; opacity: 0.65; }
.secondary-btn { background: #fff; border: 1px solid #d1d5db; color: #374151; padding: 9px 14px; }
.error-text { color: #dc2626; }
.leaderboard-panel { border-top: 1px solid #e5e7eb; margin-top: 18px; padding-top: 18px; }
.leaderboard-panel h3 { color: #111827; margin: 0 0 10px; }
.leaderboard-list { display: grid; gap: 6px; }
.leaderboard-row { align-items: center; background: #f9fafb; border-radius: 6px; display: grid; gap: 10px; grid-template-columns: 54px 1fr 90px 170px; padding: 8px 10px; }
@media (max-width: 720px) {
  .student-page { padding: 20px; }
  .paper-summary { flex-direction: column; }
  .leaderboard-row { grid-template-columns: 44px 1fr; }
}
</style>
