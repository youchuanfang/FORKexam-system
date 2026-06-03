import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/student',
    name: 'StudentHome',
    component: () => import('../views/student/StudentHome.vue'),
    meta: { requiresAuth: true, role: 'student' }
  },
  {
    path: '/student/papers/:paperId',
    name: 'StudentPaperDetail',
    component: () => import('../views/student/StudentPaperDetail.vue'),
    meta: { requiresAuth: true, role: 'student' }
  },
  {
    path: '/student/exam/:recordId',
    name: 'StudentExam',
    component: () => import('../views/student/StudentExam.vue'),
    meta: { requiresAuth: true, role: 'student' }
  },
  {
    path: '/student/records',
    name: 'StudentRecords',
    component: () => import('../views/student/StudentRecords.vue'),
    meta: { requiresAuth: true, role: 'student' }
  },
  {
    path: '/student/records/:recordId',
    name: 'StudentRecordDetail',
    component: () => import('../views/student/StudentRecordDetail.vue'),
    meta: { requiresAuth: true, role: 'student' }
  },
  {
    path: '/teacher',
    name: 'TeacherHome',
    component: () => import('../views/teacher/TeacherHome.vue'),
    meta: { requiresAuth: true, role: 'teacher' }
  },
  {
    path: '/admin',
    name: 'AdminHome',
    component: () => import('../views/admin/AdminHome.vue'),
    meta: { requiresAuth: true, role: 'admin' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')

  // 访问登录页：已登录则跳转到对应首页
  if (to.path === '/login') {
    if (token && role) {
      next('/' + role)
    } else {
      next()
    }
    return
  }

  // 未登录 → 跳转登录页
  if (!token) {
    next('/login')
    return
  }

  // 已登录但角色不匹配 → 跳转到自己的首页
  if (to.meta.role && to.meta.role !== role) {
    next('/' + role)
    return
  }

  next()
})

export default router
