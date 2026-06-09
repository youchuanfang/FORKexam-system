<template>
  <div class="admin-page">
    <header class="page-header">
      <div>
        <h1>管理员控制台</h1>
        <p>系统概览与用户管理</p>
      </div>
      <button class="secondary-btn" @click="logout">退出登录</button>
    </header>

    <section class="panel">
      <h3>系统概览</h3>
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-num">{{ overview.totalUsers ?? '-' }}</div>
          <div class="stat-label">用户总数</div>
        </div>
        <div class="stat-card">
          <div class="stat-num">{{ overview.totalQuestions ?? '-' }}</div>
          <div class="stat-label">题目总数</div>
        </div>
        <div class="stat-card">
          <div class="stat-num">{{ overview.totalPapers ?? '-' }}</div>
          <div class="stat-label">试卷总数</div>
        </div>
        <div class="stat-card">
          <div class="stat-num">{{ overview.totalRecords ?? '-' }}</div>
          <div class="stat-label">考试记录数</div>
        </div>
      </div>
    </section>

    <section class="panel user-panel">
      <h3>用户管理</h3>
      <div class="filter-bar">
        <button
          v-for="r in ['', 'student', 'teacher', 'admin']"
          :key="r"
          :class="['filter-tag', { active: roleFilter === r }]"
          @click="roleFilter = r; currentPage = 0; loadUsers()"
        >{{ r || '全部' }}</button>
      </div>

      <p v-if="loading" class="state-text">加载中...</p>
      <p v-else-if="error" class="error-text">{{ error }}</p>
      <table v-else class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>角色</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>{{ user.id }}</td>
            <td>{{ user.username }}</td>
            <td>
              <span :class="['status-pill', roleClass(user.role)]">{{ user.role }}</span>
            </td>
            <td>
              <span :class="['status-pill', user.status === 'disabled' ? 'disabled' : 'active-status']">
                {{ user.status === 'disabled' ? '已禁用' : '正常' }}
              </span>
            </td>
            <td>
              <select
                :value="user.role"
                class="role-select"
                @change="changeRole(user, $event.target.value)"
              >
                <option value="student">student</option>
                <option value="teacher">teacher</option>
                <option value="admin">admin</option>
              </select>
              <button class="text-btn" type="button" @click="handleResetPassword(user)">重置密码</button>
              <button
                class="text-btn"
                type="button"
                :class="user.status === 'disabled' ? '' : 'danger'"
                @click="handleToggleStatus(user)"
              >
                {{ user.status === 'disabled' ? '启用' : '禁用' }}
              </button>
              <span v-if="roleMsg[user.id]" :class="roleOk[user.id] ? 'state-text' : 'error-text'" class="inline-msg">
                {{ roleMsg[user.id] }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
      <Pagination
        :currentPage="currentPage"
        :totalPages="totalPages"
        :totalElements="totalElements"
        @page-change="(p) => { currentPage = p; loadUsers() }"
      />
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../utils/request'
import { clearAuth } from '../../utils/auth'
import Pagination from '../../components/Pagination.vue'

const router = useRouter()
const overview = ref({})
const users = ref([])
const loading = ref(false)
const error = ref('')
const roleFilter = ref('')
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const roleMsg = reactive({})
const roleOk = reactive({})

function roleClass(role) {
  return { student: 'student', teacher: 'teacher', admin: 'admin' }[role] || ''
}

async function loadOverview() {
  try {
    const res = await request.get('/api/admin/overview')
    if (res.code === 200) overview.value = res.data || {}
  } catch {
    // 概览失败不影响用户管理。
  }
}

async function loadUsers() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: currentPage.value, size: 10 }
    if (roleFilter.value) params.role = roleFilter.value
    const res = await request.get('/api/admin/users', { params })
    if (res.code === 200) {
      const pageData = res.data
      users.value = pageData.content || []
      totalPages.value = pageData.totalPages || 0
      totalElements.value = pageData.totalElements || 0
    } else {
      error.value = res.message || '获取用户列表失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '获取用户列表失败'
  } finally {
    loading.value = false
  }
}

async function changeRole(user, newRole) {
  if (newRole === user.role) return
  try {
    const res = await request.put(`/api/admin/users/${user.id}`, { role: newRole })
    if (res.code === 200) {
      roleMsg[user.id] = '已更新'
      roleOk[user.id] = true
      user.role = newRole
    } else {
      roleMsg[user.id] = res.message || '更新失败'
      roleOk[user.id] = false
    }
  } catch (err) {
    roleMsg[user.id] = err.response?.data?.message || err.message || '更新失败'
    roleOk[user.id] = false
  }
}

async function handleResetPassword(user) {
  const newPwd = prompt(`请输入用户 "${user.username}" 的新密码（至少3位）：`)
  if (!newPwd || newPwd.length < 3) {
    if (newPwd !== null) alert('密码至少3位')
    return
  }
  try {
    const res = await request.put(`/api/admin/users/${user.id}/reset-password`, { password: newPwd })
    if (res.code === 200) {
      roleMsg[user.id] = '密码已重置'
      roleOk[user.id] = true
    } else {
      roleMsg[user.id] = res.message || '重置失败'
      roleOk[user.id] = false
    }
  } catch (err) {
    roleMsg[user.id] = err.response?.data?.message || err.message || '重置失败'
    roleOk[user.id] = false
  }
}

async function handleToggleStatus(user) {
  try {
    const res = await request.put(`/api/admin/users/${user.id}/toggle-status`)
    if (res.code === 200) {
      user.status = res.data.status
      roleMsg[user.id] = user.status === 'disabled' ? '已禁用' : '已启用'
      roleOk[user.id] = true
    } else {
      roleMsg[user.id] = res.message || '操作失败'
      roleOk[user.id] = false
    }
  } catch (err) {
    roleMsg[user.id] = err.response?.data?.message || err.message || '操作失败'
    roleOk[user.id] = false
  }
}

function logout() {
  clearAuth()
  router.push('/login')
}

onMounted(() => {
  loadOverview()
  loadUsers()
})
</script>

<style scoped>
.admin-page { min-height: 100vh; background: #f5f7fb; padding: 32px; }
.user-panel { margin-top: 20px; }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.stat-card {
  background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 8px;
  padding: 20px; text-align: center;
}
.stat-num { font-size: 28px; font-weight: 700; color: #2563eb; }
.stat-label { font-size: 13px; color: #6b7280; margin-top: 4px; }
.role-select {
  border: 1px solid #d1d5db; border-radius: 4px; padding: 4px 8px; font-size: 13px;
}
.inline-msg { margin-left: 8px; }
.status-pill.student { background: #dbeafe; color: #1e40af; }
.status-pill.teacher { background: #d1fae5; color: #065f46; }
.status-pill.admin { background: #fef3c7; color: #92400e; }
.status-pill.active-status { background: #d1fae5; color: #065f46; }
.status-pill.disabled { background: #fee2e2; color: #991b1b; }
@media (max-width: 720px) {
  .admin-page { padding: 20px; }
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
