import api from './index'

export const authApi = {
  sendVerifyCode: (email) => api.post('/auth/send-verify-code', { email }),

  register: (data) => api.post('/auth/register', data),

  login: (data) => api.post('/auth/login', data),

  getCurrentUser: () => api.get('/auth/me')
}

export const TOKEN_KEY = 'auth_token'
export const USER_KEY = 'auth_user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function getUser() {
  const userStr = localStorage.getItem(USER_KEY)
  return userStr ? JSON.parse(userStr) : null
}

export function setUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function removeUser() {
  localStorage.removeItem(USER_KEY)
}
