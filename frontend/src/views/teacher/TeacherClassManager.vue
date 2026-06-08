<template>
  <div class="teacher-page">
    <header class="page-header">
      <div>
        <h1>班级管理</h1>
        <p>创建班级、查看邀请码并管理班级学生。</p>
      </div>
      <button class="secondary-btn" type="button" @click="loadClasses">刷新</button>
    </header>

    <section class="panel">
      <div class="form-row">
        <input v-model="className" placeholder="输入班级名称" />
        <button class="primary-btn" type="button" @click="handleCreate">创建班级</button>
      </div>
      <p v-if="error" class="error-text">{{ error }}</p>
      <p v-if="message" class="state-text">{{ message }}</p>
    </section>

    <section class="panel">
      <p v-if="loading" class="state-text">加载中...</p>
      <p v-else-if="classes.length === 0" class="state-text">暂无班级。</p>
      <div v-else class="class-list">
        <article v-for="item in classes" :key="item.id" class="class-card">
          <div class="class-head">
            <div>
              <h3>{{ item.name }}</h3>
              <p>邀请码：<strong>{{ item.joinCode }}</strong> · 学生 {{ item.memberCount || 0 }} 人</p>
            </div>
            <button class="secondary-btn" type="button" @click="toggleDetail(item)">
              {{ expandedId === item.id ? '收起' : '查看学生' }}
            </button>
          </div>
          <div v-if="expandedId === item.id" class="student-list">
            <div class="add-student-row">
              <input v-model="studentKeyword" placeholder="搜索学生用户名" @input="loadStudentOptions" />
              <button class="secondary-btn" type="button" @click="loadStudentOptions">搜索</button>
            </div>
            <div v-if="studentOptions.length" class="student-options">
              <button
                v-for="student in studentOptions"
                :key="student.id"
                class="student-option"
                type="button"
                @click="handleAdd(item.id, student.id)"
              >
                加入 {{ student.username }}
              </button>
            </div>
            <p v-if="detailLoading" class="state-text">加载学生中...</p>
            <p v-else-if="students.length === 0" class="state-text">暂无学生。</p>
            <div v-else v-for="student in students" :key="student.id" class="student-row">
              <span>{{ student.username }}</span>
              <button class="text-btn danger" type="button" @click="handleRemove(item.id, student.id)">移除</button>
            </div>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { addClassStudent, createClass, getClassDetail, getClasses, getStudents, removeClassStudent } from '../../api/teacher'

const classes = ref([])
const students = ref([])
const className = ref('')
const expandedId = ref(null)
const loading = ref(false)
const detailLoading = ref(false)
const error = ref('')
const message = ref('')
const studentKeyword = ref('')
const studentOptions = ref([])

async function loadClasses() {
  loading.value = true
  error.value = ''
  try {
    const res = await getClasses()
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

async function handleCreate() {
  if (!className.value.trim()) {
    error.value = '班级名称不能为空'
    return
  }
  try {
    const res = await createClass({ name: className.value.trim() })
    if (res.code === 200) {
      className.value = ''
      message.value = `创建成功，邀请码：${res.data.joinCode}`
      await loadClasses()
    } else {
      error.value = res.message || '创建班级失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '创建班级失败'
  }
}

async function toggleDetail(item) {
  if (expandedId.value === item.id) {
    expandedId.value = null
    students.value = []
    return
  }
  expandedId.value = item.id
  studentKeyword.value = ''
  studentOptions.value = []
  detailLoading.value = true
  try {
    const res = await getClassDetail(item.id)
    students.value = res.code === 200 ? (res.data?.students || []) : []
  } finally {
    detailLoading.value = false
  }
}

async function loadStudentOptions() {
  try {
    const res = await getStudents(studentKeyword.value ? { keyword: studentKeyword.value } : {})
    studentOptions.value = res.code === 200 ? (res.data || []) : []
  } catch {
    studentOptions.value = []
  }
}

async function refreshCurrentClass(classId) {
  const current = classes.value.find(item => item.id === classId)
  if (!current) return
  expandedId.value = null
  await toggleDetail(current)
  await loadClasses()
}

async function handleAdd(classId, studentId) {
  try {
    const res = await addClassStudent(classId, studentId)
    if (res.code === 200) {
      message.value = '学生已加入班级'
      await refreshCurrentClass(classId)
    } else {
      error.value = res.message || '添加学生失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '添加学生失败'
  }
}

async function handleRemove(classId, studentId) {
  if (!window.confirm('确认移除该学生？')) return
  await removeClassStudent(classId, studentId)
  await refreshCurrentClass(classId)
}

onMounted(loadClasses)
</script>

<style scoped>
.teacher-page { min-height: 100vh; background: #f5f7fb; padding: 32px; }
.page-header, .panel { max-width: 1080px; margin: 0 auto 18px; }
.page-header { display: flex; justify-content: space-between; gap: 18px; align-items: flex-start; }
h1, h3, p { margin: 0; }
h1 { color: #1f2937; font-size: 28px; margin-bottom: 8px; }
.page-header p, .state-text, .class-card p { color: #6b7280; }
.panel { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 24px; }
.form-row { display: flex; gap: 10px; }
input { border: 1px solid #d1d5db; border-radius: 6px; flex: 1; padding: 10px 12px; }
.class-list { display: grid; gap: 12px; }
.class-card { border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; }
.class-head, .student-row { display: flex; justify-content: space-between; gap: 12px; align-items: center; }
.student-list { border-top: 1px solid #e5e7eb; display: grid; gap: 8px; margin-top: 12px; padding-top: 12px; }
.add-student-row { display: flex; gap: 8px; }
.student-options { display: flex; flex-wrap: wrap; gap: 8px; }
.student-option { background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 6px; color: #1d4ed8; cursor: pointer; padding: 6px 10px; }
.primary-btn, .secondary-btn, .text-btn { border-radius: 6px; cursor: pointer; font-size: 14px; text-decoration: none; white-space: nowrap; }
.primary-btn { background: #2563eb; border: 0; color: #fff; padding: 10px 16px; }
.secondary-btn { background: #fff; border: 1px solid #d1d5db; color: #374151; padding: 9px 14px; }
.text-btn { background: transparent; border: 0; color: #2563eb; }
.text-btn.danger, .error-text { color: #dc2626; }
@media (max-width: 720px) {
  .teacher-page { padding: 20px; }
  .page-header, .form-row, .class-head, .student-row, .add-student-row { flex-direction: column; align-items: stretch; }
}
</style>
