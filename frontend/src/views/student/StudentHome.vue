<template>
  <div class="student-page">
    <header class="page-header">
      <div>
        <h1>学生考试中心</h1>
        <p>查看考试开放状态、作答次数和成绩记录。</p>
      </div>
      <div class="header-actions">
        <router-link class="secondary-btn" to="/student/records">考试记录</router-link>
        <button class="secondary-btn" type="button" @click="logout">退出登录</button>
      </div>
    </header>

    <section class="panel">
      <div class="panel-title">
        <h2>我的考试</h2>
        <button class="text-btn" type="button" @click="loadPapers">刷新</button>
      </div>

      <div class="filter-tabs">
        <button
          v-for="item in filters"
          :key="item.key"
          :class="['filter-tab', { active: activeFilter === item.key }]"
          type="button"
          @click="activeFilter = item.key"
        >
          <span>{{ item.label }}</span>
          <strong>{{ countByFilter(item.key) }}</strong>
        </button>
      </div>

      <p v-if="loading" class="state-text">加载中...</p>
      <p v-else-if="error" class="error-text">{{ error }}</p>
      <p v-else-if="filteredPapers.length === 0" class="state-text">当前分类暂无考试。</p>

      <div v-else class="paper-list">
        <article v-for="paper in filteredPapers" :key="paper.paperId" class="paper-card">
          <div class="paper-main">
            <div class="paper-title-row">
              <h3>{{ paper.title || '未命名试卷' }}</h3>
              <span :class="['status-pill', paper.status]">{{ paper.statusText || '已开放' }}</span>
            </div>
            <p>考试时长：{{ paper.duration || 0 }} 分钟</p>
            <p>开放时间：{{ formatOpenRange(paper) }}</p>
            <p>作答次数：{{ paper.attemptCount || 0 }} / {{ paper.maxAttempts || 1 }}</p>
            <p>剩余机会：{{ paper.remainingAttempts ?? 0 }}</p>
            <p>最高分：{{ paper.bestScore == null ? '未作答' : `${paper.bestScore} 分` }}</p>
          </div>
          <div class="paper-actions">
            <router-link class="secondary-btn" :to="`/student/papers/${paper.paperId}`">查看详情</router-link>
            <button class="primary-btn" type="button" :disabled="!canStart(paper) || startingId === paper.paperId" @click="handleStartExam(paper)">
              {{ actionText(paper) }}
            </button>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getPapers, startExam } from '../../api/student'

const router = useRouter()
const papers = ref([])
const loading = ref(false)
const error = ref('')
const activeFilter = ref('OPEN')
const startingId = ref(null)

const filters = [
  { key: 'NOT_OPEN', label: '未开放' },
  { key: 'OPEN', label: '已开放' },
  { key: 'CLOSED', label: '已关闭' }
]

const filteredPapers = computed(() => papers.value.filter(paper => filterKey(paper) === activeFilter.value))

function normalizePaper(rawPaper) {
  const paper = { ...rawPaper }
  const maxAttempts = Number(paper.maxAttempts ?? 1)
  const attemptCount = Number(paper.attemptCount ?? 0)
  const remainingAttempts = Number(paper.remainingAttempts ?? Math.max(0, maxAttempts - attemptCount))

  paper.maxAttempts = maxAttempts
  paper.attemptCount = attemptCount
  paper.remainingAttempts = remainingAttempts
  if (!paper.status) {
    paper.status = remainingAttempts > 0 ? 'OPEN' : 'NO_ATTEMPTS'
  }
  if (!paper.statusText) {
    paper.statusText = paper.status === 'NO_ATTEMPTS' ? '作答次数已用完' : '已开放'
  }
  return paper
}

function filterKey(paper) {
  if (paper.status === 'NOT_OPEN') return 'NOT_OPEN'
  if (paper.status === 'CLOSED') return 'CLOSED'
  return 'OPEN'
}

function countByFilter(key) {
  return papers.value.filter(paper => filterKey(paper) === key).length
}

function canStart(paper) {
  return paper.status === 'OPEN' && (paper.remainingAttempts ?? 0) > 0
}

function actionText(paper) {
  if (startingId.value === paper.paperId) return '正在开始...'
  if (paper.status === 'NOT_OPEN') return '未开放'
  if (paper.status === 'CLOSED') return '已关闭，不可作答'
  if ((paper.remainingAttempts ?? 0) <= 0) return '作答次数已用完'
  return (paper.attemptCount || 0) > 0 ? '再次作答' : '开始考试'
}

function formatTime(value) {
  return value ? value.replace('T', ' ').slice(0, 19) : ''
}

function formatOpenRange(paper) {
  const start = formatTime(paper.openStartTime)
  const end = formatTime(paper.openEndTime)
  if (!start && !end) return '长期开放'
  if (start && end) return `${start} 至 ${end}`
  if (start) return `${start} 起`
  return `${end} 前`
}

function saveExamContext(data) {
  sessionStorage.setItem(`student_exam_${data.recordId}`, JSON.stringify(data))
}

async function loadPapers() {
  loading.value = true
  error.value = ''
  try {
    const res = await getPapers()
    if (res.code === 200) {
      papers.value = (res.data || []).map(normalizePaper)
    } else {
      error.value = res.message || '获取考试列表失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '获取考试列表失败'
  } finally {
    loading.value = false
  }
}

async function handleStartExam(paper) {
  if (!canStart(paper)) return
  startingId.value = paper.paperId
  error.value = ''
  try {
    const res = await startExam(paper.paperId)
    if (res.code === 200) {
      saveExamContext(res.data)
      router.push(`/student/exam/${res.data.recordId}`)
    } else {
      error.value = res.message || '开始考试失败'
      await loadPapers()
    }
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '开始考试失败'
  } finally {
    startingId.value = null
  }
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('role')
  router.push('/login')
}

onMounted(loadPapers)
</script>

<style scoped>
.student-page {
  min-height: 100vh;
  background: #f5f7fb;
  padding: 32px;
}

.page-header,
.panel {
  max-width: 1080px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
  margin-bottom: 24px;
}

h1,
h2,
h3,
p {
  margin: 0;
}

h1 {
  color: #1f2937;
  font-size: 28px;
  margin-bottom: 8px;
}

.page-header p,
.paper-card p,
.state-text {
  color: #6b7280;
}

.header-actions,
.paper-actions {
  display: flex;
  gap: 10px;
}

.paper-actions {
  align-items: flex-end;
  flex-direction: column;
}

.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 24px;
}

.panel-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.filter-tabs {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-bottom: 18px;
}

.filter-tab {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  color: #374151;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  padding: 14px;
}

.filter-tab.active {
  background: #eff6ff;
  border-color: #2563eb;
  color: #1d4ed8;
}

.paper-list {
  display: grid;
  gap: 12px;
}

.paper-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 18px;
}

.paper-main {
  display: grid;
  gap: 6px;
}

.paper-title-row {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.paper-card h3 {
  color: #111827;
  font-size: 18px;
}

.status-pill {
  background: #eef2ff;
  border-radius: 999px;
  color: #3730a3;
  font-size: 12px;
  padding: 4px 9px;
}

.status-pill.NOT_OPEN {
  background: #fff7ed;
  color: #c2410c;
}

.status-pill.CLOSED,
.status-pill.NO_ATTEMPTS {
  background: #f3f4f6;
  color: #4b5563;
}

.primary-btn,
.secondary-btn,
.text-btn {
  border: 0;
  cursor: pointer;
  text-decoration: none;
  font-size: 14px;
  border-radius: 6px;
  white-space: nowrap;
}

.primary-btn {
  background: #2563eb;
  color: #fff;
  padding: 10px 16px;
}

.primary-btn:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.secondary-btn {
  background: #fff;
  color: #374151;
  border: 1px solid #d1d5db;
  padding: 9px 14px;
}

.text-btn {
  background: transparent;
  color: #2563eb;
  padding: 6px 0;
}

.error-text {
  color: #dc2626;
}

@media (max-width: 720px) {
  .student-page {
    padding: 20px;
  }

  .page-header,
  .paper-card,
  .paper-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-tabs {
    grid-template-columns: 1fr;
  }

  .header-actions {
    flex-wrap: wrap;
  }
}
</style>
