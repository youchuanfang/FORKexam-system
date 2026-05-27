<template>
  <div class="login-container">
    <div class="login-card">
      <h1>在线考试系统</h1>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label>用户名</label>
          <input v-model="username" type="text" placeholder="请输入用户名" required />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input v-model="password" type="password" placeholder="请输入密码" required />
        </div>
        <div class="form-group">
          <label>角色</label>
          <select v-model="role" required>
            <option value="student">学生</option>
            <option value="teacher">教师</option>
            <option value="admin">管理员</option>
          </select>
        </div>
        <button type="submit" class="btn-login">登录</button>
      </form>
      <p class="register-link">
        还没有账号？<a href="#" @click.prevent="showRegister = true">立即注册</a>
      </p>
    </div>

    <!-- 注册弹窗 -->
    <div v-if="showRegister" class="modal-overlay" @click.self="showRegister = false">
      <div class="modal-card">
        <h2>用户注册</h2>
        <form @submit.prevent="handleRegister">
          <div class="form-group">
            <label>用户名</label>
            <input v-model="regUsername" type="text" placeholder="请输入用户名" required />
          </div>
          <div class="form-group">
            <label>密码</label>
            <input v-model="regPassword" type="password" placeholder="请输入密码" required />
          </div>
          <div class="form-group">
            <label>角色</label>
            <select v-model="regRole" required>
              <option value="student">学生</option>
              <option value="teacher">教师</option>
              <option value="admin">管理员</option>
            </select>
          </div>
          <p v-if="regMessage" :class="regSuccess ? 'success-msg' : 'error-msg'">{{ regMessage }}</p>
          <div class="modal-buttons">
            <button type="submit" class="btn-primary">注册</button>
            <button type="button" class="btn-cancel" @click="showRegister = false">取消</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'

const router = useRouter()

// 登录表单
const username = ref('')
const password = ref('')
const role = ref('student')

// 注册表单
const showRegister = ref(false)
const regUsername = ref('')
const regPassword = ref('')
const regRole = ref('student')
const regMessage = ref('')
const regSuccess = ref(false)

const roleMap = { student: '/student', teacher: '/teacher', admin: '/admin' }

async function handleLogin() {
  try {
    const res = await request.post('/api/user/login', {
      username: username.value,
      password: password.value
    })
    if (res.code === 200) {
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('role', res.data.role)
      router.push(roleMap[res.data.role] || '/')
    } else {
      alert(res.message || '登录失败')
    }
  } catch (err) {
    alert('登录失败：' + (err.response?.data?.message || err.message))
  }
}

async function handleRegister() {
  try {
    regMessage.value = ''
    const res = await request.post('/api/user/register', {
      username: regUsername.value,
      password: regPassword.value,
      role: regRole.value
    })
    if (res.code === 200) {
      regSuccess.value = true
      regMessage.value = '注册成功！请登录'
      // 2秒后自动关闭弹窗
      setTimeout(() => {
        showRegister.value = false
        regMessage.value = ''
      }, 1500)
    } else {
      regSuccess.value = false
      regMessage.value = res.message || '注册失败'
    }
  } catch (err) {
    regSuccess.value = false
    regMessage.value = err.response?.data?.message || err.message || '注册失败'
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  background: #fff;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  width: 400px;
  max-width: 90vw;
}

h1 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
  font-size: 24px;
}

h2 {
  margin-bottom: 20px;
  color: #333;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  color: #555;
  font-size: 14px;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.3s;
  background: #fff;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #667eea;
}

.btn-login {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  cursor: pointer;
  margin-top: 10px;
  transition: opacity 0.3s;
}

.btn-login:hover {
  opacity: 0.9;
}

.register-link {
  text-align: center;
  margin-top: 20px;
  color: #888;
  font-size: 14px;
}

.register-link a {
  color: #667eea;
  text-decoration: none;
}

.register-link a:hover {
  text-decoration: underline;
}

/* 注册弹窗 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-card {
  background: #fff;
  padding: 30px;
  border-radius: 12px;
  width: 420px;
  max-width: 90vw;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.modal-buttons {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.btn-primary {
  flex: 1;
  padding: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 15px;
  cursor: pointer;
}

.btn-primary:hover {
  opacity: 0.9;
}

.btn-cancel {
  flex: 1;
  padding: 12px;
  background: #eee;
  color: #333;
  border: none;
  border-radius: 6px;
  font-size: 15px;
  cursor: pointer;
}

.btn-cancel:hover {
  background: #ddd;
}

.error-msg {
  color: #e74c3c;
  font-size: 14px;
  margin-top: 6px;
}

.success-msg {
  color: #27ae60;
  font-size: 14px;
  margin-top: 6px;
}
</style>
