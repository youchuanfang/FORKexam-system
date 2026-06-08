<template>
  <div class="student-page">
    <header class="page-header">
      <div>
        <h1>我的班级</h1>
        <p>输入教师提供的邀请码加入班级。</p>
      </div>
      <button class="secondary-btn" type="button" @click="loadClasses">刷新</button>
    </header>

    <section class="panel">
      <div class="form-row">
        <input v-model="joinCode" placeholder="输入班级邀请码" />
        <button class="primary-btn" type="button" @click="handleJoin">加入班级</button>
      </div>
      <p v-if="error" class="error-text">{{ error }}</p>
      <p v-if="message" class="state-text">{{ message }}</p>
    </section>

    <section class="panel">
      <p v-if="loading" class="state-text">加载中...</p>
      <p v-else-if="classes.length === 0" class="state-text">暂未加入班级。</p>
      <div v-else class="class-list">
        <article v-for="item in classes" :key="item.id" class="class-card">
          <h3>{{ item.name }}</h3>
          <p>邀请码：{{ item.joinCode }} · 学生 {{ item.memberCount || 0 }} 人</p>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getMyClasses, joinClass } from '../../api/student'

const classes = ref([])
const joinCode = ref('')
const loading = ref(false)
const error = ref('')
const message = ref('')

async function loadClasses() {
  loading.value = true
  error.value = ''
  try {
    const res = await getMyClasses()
    if (res.code === 200) {
      classes.value = res.data || []
    } else {
      error.value = res.message || '获取班级失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '获取班级失败'
  } finally {
    loading.value = false
  }
}

async function handleJoin() {
  if (!joinCode.value.trim()) {
    error.value = '邀请码不能为空'
    return
  }
  try {
    const res = await joinClass(joinCode.value.trim())
    if (res.code === 200) {
      message.value = `已加入班级：${res.data.name}`
      joinCode.value = ''
      await loadClasses()
    } else {
      error.value = res.message || '加入班级失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '加入班级失败'
  }
}

onMounted(loadClasses)
</script>

<style scoped>
.student-page { min-height: 100vh; background: #f5f7fb; padding: 32px; }
.page-header, .panel { max-width: 980px; margin: 0 auto 18px; }
.page-header { display: flex; justify-content: space-between; gap: 18px; align-items: flex-start; }
h1, h3, p { margin: 0; }
h1 { color: #1f2937; font-size: 28px; margin-bottom: 8px; }
.page-header p, .state-text, .class-card p { color: #6b7280; }
.panel { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 24px; }
.form-row { display: flex; gap: 10px; }
input { border: 1px solid #d1d5db; border-radius: 6px; flex: 1; padding: 10px 12px; }
.class-list { display: grid; gap: 12px; }
.class-card { border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; }
.primary-btn, .secondary-btn { border-radius: 6px; cursor: pointer; font-size: 14px; text-decoration: none; white-space: nowrap; }
.primary-btn { background: #2563eb; border: 0; color: #fff; padding: 10px 16px; }
.secondary-btn { background: #fff; border: 1px solid #d1d5db; color: #374151; padding: 9px 14px; }
.error-text { color: #dc2626; }
@media (max-width: 720px) {
  .student-page { padding: 20px; }
  .page-header, .form-row { flex-direction: column; align-items: stretch; }
}
</style>
