<template>
  <nav class="navbar">
    <div class="nav-brand">在线考试系统</div>
    <div class="nav-menu">
      <!-- 学生菜单 -->
      <template v-if="role === 'student'">
        <router-link to="/student">首页</router-link>
        <router-link to="/student/classes">我的班级</router-link>
        <router-link to="/student/records">考试记录</router-link>
      </template>

      <!-- 教师菜单 -->
      <template v-else-if="role === 'teacher'">
        <router-link to="/teacher">首页</router-link>
        <router-link to="/teacher/questions">题库管理</router-link>
        <router-link to="/teacher/classes">班级管理</router-link>
        <router-link to="/teacher/papers">试卷管理</router-link>
      </template>

      <!-- 管理员菜单 -->
      <template v-else-if="role === 'admin'">
        <router-link to="/admin">首页</router-link>
        <router-link to="/admin">用户管理</router-link>
        <router-link to="/admin">系统设置</router-link>
      </template>

      <span class="role-badge">{{ roleLabel }}</span>
      <a href="#" class="logout-btn" @click.prevent="handleLogout">退出登录</a>
    </div>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { authState, clearAuth, syncAuthFromStorage } from '../utils/auth'

const router = useRouter()

const role = computed(() => {
  syncAuthFromStorage()
  return authState.role
})

const roleLabel = computed(() => {
  const labels = { student: '学生', teacher: '教师', admin: '管理员' }
  return labels[role.value] || ''
})

function handleLogout() {
  clearAuth()
  router.push('/login')
}
</script>

<style scoped>
.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 30px;
  height: 60px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-brand {
  font-size: 20px;
  font-weight: bold;
  color: #667eea;
}

.nav-menu {
  display: flex;
  gap: 24px;
  align-items: center;
}

.nav-menu a {
  color: #555;
  text-decoration: none;
  font-size: 15px;
  transition: color 0.2s;
}

.nav-menu a:hover {
  color: #667eea;
}

.nav-menu a.router-link-active {
  color: #667eea;
  font-weight: 600;
}

.role-badge {
  background: #667eea;
  color: #fff;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
}

.logout-btn {
  color: #e74c3c !important;
}

.logout-btn:hover {
  color: #c0392b !important;
}
</style>
