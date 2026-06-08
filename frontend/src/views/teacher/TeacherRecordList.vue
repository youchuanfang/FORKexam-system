<template>
  <div class="teacher-page">
    <header class="page-header">
      <div>
        <h1>考试记录</h1>
        <p>查看本试卷下所有学生的考试记录。</p>
      </div>
      <div class="header-actions">
        <router-link class="secondary-btn" to="/teacher/papers">返回试卷列表</router-link>
        <button class="secondary-btn" type="button" @click="loadRecords">刷新</button>
      </div>
    </header>

    <section class="panel">
      <p v-if="loading" class="state-text">加载中...</p>
      <p v-else-if="error" class="error-text">{{ error }}</p>
      <p v-else-if="records.length === 0" class="state-text">暂无考试记录。</p>

      <table v-else class="data-table">
        <thead>
          <tr>
            <th>记录ID</th>
            <th>学生姓名</th>
            <th>开始时间</th>
            <th>提交时间</th>
            <th>总分</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in records" :key="r.recordId">
            <td>{{ r.recordId }}</td>
            <td>{{ r.studentName }}</td>
            <td>{{ formatTime(r.startTime) }}</td>
            <td>{{ r.submitted ? formatTime(r.submitTime) : '-' }}</td>
            <td>{{ r.totalScore != null ? r.totalScore + ' 分' : '-' }}</td>
            <td>
              <span v-if="r.submitted" class="status-pill submitted">已提交</span>
              <span v-else class="status-pill pending">未提交</span>
              <span v-if="r.hasSubjective" class="status-pill subjective">待批改</span>
            </td>
            <td>
              <router-link class="text-btn" :to="`/teacher/records/${r.recordId}`">查看详情</router-link>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getExamRecords } from '../../api/teacher'

const route = useRoute()
const paperId = Number(route.params.paperId)
const records = ref([])
const loading = ref(false)
const error = ref('')

function formatTime(value) {
  return value ? value.replace('T', ' ').slice(0, 19) : ''
}

async function loadRecords() {
  loading.value = true
  error.value = ''
  try {
    const res = await getExamRecords({ paperId })
    if (res.code === 200) {
      records.value = res.data || []
    } else {
      error.value = res.message || '获取记录失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '获取记录失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadRecords)
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

h1, p { margin: 0; }

h1 { color: #1f2937; font-size: 28px; margin-bottom: 8px; }

.page-header p { color: #6b7280; }

.header-actions { display: flex; gap: 10px; }

.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 24px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table th {
  text-align: left;
  padding: 10px 12px;
  background: #f9fafb;
  color: #374151;
  font-weight: 600;
  border-bottom: 2px solid #e5e7eb;
}

.data-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f3f4f6;
  vertical-align: top;
}

.status-pill {
  display: inline-block;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 999px;
  margin-right: 4px;
}

.status-pill.submitted { background: #d1fae5; color: #065f46; }

.status-pill.pending { background: #fef3c7; color: #92400e; }

.status-pill.subjective { background: #dbeafe; color: #1e40af; }

.primary-btn, .secondary-btn, .text-btn {
  border: 0; cursor: pointer; text-decoration: none;
  font-size: 14px; border-radius: 6px; white-space: nowrap;
}

.secondary-btn {
  background: #fff; color: #374151;
  border: 1px solid #d1d5db; padding: 9px 14px;
  display: inline-block;
}

.text-btn {
  background: transparent; color: #2563eb; padding: 4px 0; font-size: 13px;
}

.error-text { color: #dc2626; }

.state-text { color: #6b7280; }

@media (max-width: 720px) {
  .teacher-page { padding: 20px; }
  .page-header { flex-direction: column; }
  .data-table { font-size: 12px; }
}
</style>
