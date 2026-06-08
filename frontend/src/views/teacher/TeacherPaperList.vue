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
            <button class="text-btn danger" type="button" @click="handleDelete(paper)">删除</button>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getPapers, deletePaper } from '../../api/teacher'

const papers = ref([])
const loading = ref(false)
const error = ref('')

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

.error-text { color: #dc2626; }

.state-text { color: #6b7280; }

@media (max-width: 720px) {
  .teacher-page { padding: 20px; }
  .page-header, .paper-card { flex-direction: column; }
  .paper-actions { align-items: stretch; flex-direction: row; flex-wrap: wrap; }
}
</style>
