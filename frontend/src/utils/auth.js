import { reactive } from 'vue'

export const authState = reactive({
  token: localStorage.getItem('token') || '',
  role: localStorage.getItem('role') || ''
})

export function setAuth(token, role) {
  authState.token = token || ''
  authState.role = role || ''
  if (authState.token) {
    localStorage.setItem('token', authState.token)
  } else {
    localStorage.removeItem('token')
  }
  if (authState.role) {
    localStorage.setItem('role', authState.role)
  } else {
    localStorage.removeItem('role')
  }
}

export function clearAuth() {
  setAuth('', '')
}

export function syncAuthFromStorage() {
  authState.token = localStorage.getItem('token') || ''
  authState.role = localStorage.getItem('role') || ''
}
