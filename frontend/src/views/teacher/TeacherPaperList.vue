<template>
  <div class="teacher-page">
    <header class="page-header">
      <div>
        <h1>试卷管理</h1>
        <p>创建和管理试卷，设置考试参数。</p>
      </div>
      <div class="header-actions">
        <router-link class="primary-btn" to="/teacher/papers/new">新建试卷</router-link>
        <button class="secondary-btn" type="button" @click="loadPapers">刷新</button>
      </div>
    </header>

    <section class="panel">
      <p v-if="loading" class="state-text">加载中...</p>
      <p v-else-if="error" class="error-text">{{ error }}</p>
      <p v-else-if="papers.length === 0" class="state-text">暂无试卷，请点击"新建试卷"创建。</p>

      <div v-else class="paper-list">
        <article v-for="paper in papers" :key="paper.id" class="paper-card">
          <div class="paper-main">
            <h3>{{ paper.title || '未命名试卷' }}</h3>
            <div class="paper-meta">
              <span>时长：{{ paper.duration || 0 }} 分钟</span>
              <span>最大次数：{{ paper.maxAttempts ?? 1 }}</span>
              <span>题目数：{{ paper.questionCount ?? 0 }}</span>
              <span>考试记录：{{ paper.recordCount ?? 0 }}</span>
              <span>{{ paper.published ? '已发布' : '草稿' }}</span>
              <span>范围：{{ paper.targetSummary || '未设置' }}</span>
              <span>平均分：{{ paper.averageScore == null ? '-' : paper.averageScore }}</span>
              <span>答案已{{ paper.releaseAnswerFlag ? '公开' : '关闭' }}</span>
            </div>
            <div class="paper-time">
              <span v-if="paper.openStartTime || paper.openEndTime">
                开放时间：{{ formatTime(paper.openStartTime) || '不限' }} ~ {{ formatTime(paper.openEndTime) || '不限' }}
              </span>
              <span v-else>长期开放</span>
              <span v-if="paper.releaseAnswerFlag && paper.answerReleaseTime">
                 | 答案公布：{{ formatTime(paper.answerReleaseTime) }}
              </span>
            </div>
          </div>
          <div class="paper-actions">
            <router-link class="secondary-btn" :to="`/teacher/papers/${paper.id}/edit`">编辑组卷</router-link>
            <router-link class="secondary-btn" :to="`/teacher/papers/${paper.id}/records`">查看记录</router-link>
            <button v-if="!paper.published" class="primary-btn" type="button" @click="handlePublish(paper)">确认发布</button>
            <button v-if="paper.published" class="secondary-btn" type="button" @click="handleUnpublish(paper)">取消发布</button>
            <button v-if="(paper.recordCount ?? 0) > 0" class="secondary-btn" type="button" @click="handleExport(paper)">导出成绩</button>
            <button class="secondary-btn" type="button" @click="toggleLeaderboard(paper)">排行榜</button>
            <label class="switch-row">
              <input :checked="paper.leaderboardPublic" type="checkbox" @change="handleLeaderboardPublic(paper, $event.target.checked)" />
              公开排行榜
            </label>
            <button
              class="text-btn danger"
              type="button"
              :disabled="(paper.recordCount ?? 0) > 0"
              :title="(paper.recordCount ?? 0) > 0 ? '该试卷已有考试记录，不能删除' : ''"
              @click="handleDelete(paper)"
            >删除</button>
          </div>
          <div v-if="expandedPaperId === paper.id" class="extra-panel">
            <p v-if="leaderboardLoading" class="state-text">加载排行榜中...</p>
            <template v-else>
              <p class="state-text">提交人数：{{ statistics?.submittedCount ?? 0 }}，总平均分：{{ statistics?.overallAverage ?? '-' }}，最高分：{{ statistics?.maxScore ?? '-' }}，最低分：{{ statistics?.minScore ?? '-' }}</p>
              <div class="stats-block">
                <strong>班级平均分</strong>
                <span v-if="!statistics?.classAverages?.length" class="state-text">暂无班级提交数据</span>
                <span v-for="item in statistics?.classAverages || []" :key="item.classId">
                  {{ item.className }}：{{ item.averageScore ?? '-' }}（{{ item.submittedCount || 0 }} 人）
                </span>
              </div>
              <div class="stats-block">
                <strong>指定学生平均分</strong>
                <div class="student-checks">
                  <label v-for="student in students" :key="student.id">
                    <input v-model="selectedStudentIds" type="checkbox" :value="student.id" @change="refreshStatistics(paper)" />
                    {{ student.username }}
                  </label>
                </div>
                <span>当前选择：{{ statistics?.selectedStudentsAverage ?? '-' }}</span>
              </div>
              <div class="leaderboard-list">
                <div v-for="item in leaderboard" :key="`${item.rank}-${item.username}`" class="leaderboard-row">
                  <span>#{{ item.rank }}</span>
                  <span>{{ item.username }}</span>
                  <strong>{{ item.score ?? 0 }}</strong>
                  <small>{{ formatTime(item.submitTime) }}</small>
                </div>
              </div>
              <!-- 分数分布 -->
              <div v-if="statistics?.scoreDistribution" class="stats-block" style="margin-top:12px">
                <strong>分数分布</strong>
                <div class="dist-bars">
                  <div v-for="(count, range) in statistics.scoreDistribution" :key="range" class="dist-bar-row">
                    <span class="dist-label">{{ range }}</span>
                    <div class="dist-bar-track">
                      <div class="dist-bar-fill" :style="{ width: distPercent(count) + '%' }"></div>
                    </div>
                    <span class="dist-count">{{ count }} 人</span>
                  </div>
                </div>
              </div>
              <p v-if="leaderboard.length === 0" class="state-text">暂无排行榜数据。</p>
            </template>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getPapers, deletePaper, publishPaper, unpublishPaper, exportPaperRecords, updateLeaderboardVisibility, getLeaderboard, getPaperStatistics, getStudents } from '../../api/teacher'

const papers = ref([])
const loading = ref(false)
const error = ref('')
const expandedPaperId = ref(null)
const leaderboard = ref([])
const statistics = ref(null)
const leaderboardLoading = ref(false)
const students = ref([])
const selectedStudentIds = ref([])

function formatTime(value) {
  return value ? value.replace('T', ' ').slice(0, 19) : ''
}

async function loadPapers() {
  loading.value = true
  error.value = ''
  try {
    const res = await getPapers()
    if (res.code === 200) {
      papers.value = res.data || []
    } else {
      error.value = res.message || '获取试卷失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '获取试卷失败'
  } finally {
    loading.value = false
  }
}

async function handleDelete(paper) {
  if ((paper.recordCount ?? 0) > 0) {
    error.value = '该试卷已有考试记录，不能删除，可后续改为归档/禁用'
    return
  }
  if (!confirm(`确定删除试卷 "${paper.title}" 吗？此操作不可撤销。`)) return
  try {
    const res = await deletePaper(paper.id)
    if (res.code === 200) {
      await loadPapers()
    } else {
      error.value = res.message || '删除失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '删除失败'
  }
}

async function handlePublish(paper) {
  if (!paper.targets?.length) {
    error.value = '请先在编辑组卷页设置发布范围'
    return
  }
  if (!confirm(`确认发布试卷 "${paper.title}" 吗？`)) return
  try {
    const res = await publishPaper(paper.id, paper.targets)
    if (res.code === 200) await loadPapers()
    else error.value = res.message || '发布失败'
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '发布失败'
  }
}

async function handleUnpublish(paper) {
  if (!confirm(`确认取消发布试卷 "${paper.title}" 吗？学生将无法再看到该试卷。`)) return
  try {
    const res = await unpublishPaper(paper.id)
    if (res.code === 200) await loadPapers()
    else error.value = res.message || '取消发布失败'
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '取消发布失败'
  }
}

async function handleExport(paper) {
  try {
    const res = await exportPaperRecords(paper.id)
    const url = window.URL.createObjectURL(new Blob([res]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `paper_${paper.id}_records.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch (err) {
    error.value = '导出失败'
  }
}

function distPercent(count) {
  if (!statistics.value?.scoreDistribution) return 0
  const max = Math.max(1, ...Object.values(statistics.value.scoreDistribution))
  return Math.round((count / max) * 100)
}

async function handleLeaderboardPublic(paper, visible) {
  try {
    const res = await updateLeaderboardVisibility(paper.id, visible)
    if (res.code === 200) {
      paper.leaderboardPublic = visible
    } else {
      error.value = res.message || '更新排行榜公开状态失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '更新排行榜公开状态失败'
  }
}

async function toggleLeaderboard(paper) {
  if (expandedPaperId.value === paper.id) {
    expandedPaperId.value = null
    return
  }
  expandedPaperId.value = paper.id
  leaderboardLoading.value = true
  try {
    selectedStudentIds.value = []
    const [rankRes, statRes, studentRes] = await Promise.all([getLeaderboard(paper.id), getPaperStatistics(paper.id), getStudents({})])
    leaderboard.value = rankRes.code === 200 ? (rankRes.data || []) : []
    statistics.value = statRes.code === 200 ? statRes.data : null
    students.value = studentRes.code === 200 ? (studentRes.data || []) : []
  } finally {
    leaderboardLoading.value = false
  }
}

async function refreshStatistics(paper) {
  const res = await getPaperStatistics(paper.id, { studentIds: selectedStudentIds.value.join(',') })
  if (res.code === 200) {
    statistics.value = res.data
  }
}

onMounted(loadPapers)
</script>

<style scoped>
.teacher-page {
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

h1, h3, p { margin: 0; }

h1 { color: #1f2937; font-size: 28px; margin-bottom: 8px; }

.page-header p { color: #6b7280; }

.header-actions { display: flex; gap: 10px; }

.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 24px;
}

.paper-list { display: grid; gap: 12px; }

.paper-card {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 18px;
}

.paper-main { display: grid; gap: 8px; }

.paper-card h3 { color: #111827; font-size: 18px; }

.paper-meta {
  display: flex; flex-wrap: wrap; gap: 12px;
  font-size: 13px; color: #6b7280;
}

.paper-time { font-size: 12px; color: #9ca3af; }

.paper-actions {
  display: flex; flex-direction: column; gap: 6px;
  align-items: flex-end;
}

.extra-panel {
  border-top: 1px solid #e5e7eb;
  margin-top: 14px;
  padding-top: 14px;
  width: 100%;
}

.leaderboard-list { display: grid; gap: 6px; margin-top: 10px; }

.stats-block {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
  padding: 10px;
}

.stats-block strong { color: #374151; width: 100%; }

.student-checks {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  width: 100%;
}

.student-checks label {
  align-items: center;
  color: #374151;
  display: flex;
  gap: 5px;
  font-size: 13px;
}

.leaderboard-row {
  align-items: center;
  background: #f9fafb;
  border-radius: 6px;
  display: grid;
  gap: 10px;
  grid-template-columns: 54px 1fr 80px 170px;
  padding: 8px 10px;
}
.dist-bars { display: grid; gap: 6px; margin-top: 8px; }
.dist-bar-row { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.dist-label { width: 50px; color: #6b7280; text-align: right; flex-shrink: 0; }
.dist-bar-track { flex: 1; height: 16px; background: #f3f4f6; border-radius: 8px; overflow: hidden; }
.dist-bar-fill { height: 100%; background: #2563eb; border-radius: 8px; transition: width 0.3s; min-width: 2px; }
.dist-count { width: 36px; color: #374151; font-weight: 500; flex-shrink: 0; }

.switch-row {
  align-items: center;
  color: #374151;
  display: flex;
  font-size: 13px;
  gap: 6px;
}

.primary-btn, .secondary-btn, .text-btn {
  border: 0; cursor: pointer; text-decoration: none;
  font-size: 14px; border-radius: 6px; white-space: nowrap;
}

.primary-btn {
  background: #2563eb; color: #fff; padding: 10px 16px;
  display: inline-block;
}

.secondary-btn {
  background: #fff; color: #374151;
  border: 1px solid #d1d5db; padding: 9px 14px;
  display: inline-block;
}

.text-btn {
  background: transparent; color: #2563eb; padding: 4px 0; font-size: 13px;
}

.text-btn.danger { color: #dc2626; }

.text-btn:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.error-text { color: #dc2626; }

.state-text { color: #6b7280; }

@media (max-width: 720px) {
  .teacher-page { padding: 20px; }
  .page-header, .paper-card { flex-direction: column; }
  .paper-actions { align-items: stretch; flex-direction: row; flex-wrap: wrap; }
  .leaderboard-row { grid-template-columns: 44px 1fr; }
}
</style>
