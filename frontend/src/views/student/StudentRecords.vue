<template>
  <div class="student-page">
    <header class="page-header">
      <div>
        <h1>我的考试记录</h1>
        <p>查看已开始或已提交的考试。</p>
      </div>
      <router-link class="secondary-btn" to="/student">返回首页</router-link>
    </header>

    <section class="panel">
      <p v-if="loading" class="state-text">加载中...</p>
      <p v-else-if="error" class="error-text">{{ error }}</p>
      <p v-else-if="records.length === 0" class="state-text">暂无考试记录。</p>

      <div v-else class="record-list">
        <article v-for="record in records" :key="record.recordId" class="record-card">
          <div>
            <h3>{{ record.paperTitle || '未知试卷' }}</h3>
            <p>开始时间：{{ formatTime(record.startTime) }}</p>
            <p>提交时间：{{ record.submitTime ? formatTime(record.submitTime) : '未提交' }}</p>
          </div>
          <div class="record-side">
            <strong>{{ record.submitTime ? `${record.totalScore ?? 0} 分` : '未提交' }}</strong>
            <router-link v-if="record.submitTime" class="primary-btn" :to="`/student/records/${record.recordId}`">查看详情</router-link>
            <router-link v-else class="primary-btn" :to="`/student/exam/${record.recordId}`">继续作答</router-link>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getExamRecords } from '../../api/student'

const records = ref([])
const loading = ref(false)
const error = ref('')

function formatTime(value) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

async function loadRecords() {
  loading.value = true
  error.value = ''
  try {
    const res = await getExamRecords()
    if (res.code === 200) {
      records.value = res.data || []
    } else {
      error.value = res.message || '获取考试记录失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '获取考试记录失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadRecords)
</script>

<style scoped>
.student-page { min-height: 100vh; background: #f5f7fb; padding: 32px; }
.page-header, .panel { max-width: 980px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; gap: 20px; align-items: flex-start; margin-bottom: 24px; }
h1, h3, p { margin: 0; }
h1 { color: #111827; font-size: 28px; margin-bottom: 8px; }
.page-header p, .record-card p, .state-text { color: #6b7280; }
.panel { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 24px; }
.record-list { display: grid; gap: 12px; }
.record-card { display: flex; justify-content: space-between; gap: 16px; border: 1px solid #e5e7eb; border-radius: 8px; padding: 18px; }
.record-card h3 { color: #111827; font-size: 18px; margin-bottom: 8px; }
.record-side { align-items: flex-end; display: flex; flex-direction: column; gap: 12px; }
.record-side strong { color: #111827; font-size: 20px; }
.primary-btn, .secondary-btn { border-radius: 6px; cursor: pointer; font-size: 14px; text-decoration: none; white-space: nowrap; }
.primary-btn { background: #2563eb; color: #fff; padding: 10px 16px; }
.secondary-btn { background: #fff; border: 1px solid #d1d5db; color: #374151; padding: 9px 14px; }
.error-text { color: #dc2626; }
@media (max-width: 720px) {
  .student-page { padding: 20px; }
  .page-header, .record-card, .record-side { align-items: stretch; flex-direction: column; }
}
</style>
