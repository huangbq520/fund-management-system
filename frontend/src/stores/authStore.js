import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, setToken, setUser, removeToken, removeUser, getToken, getUser } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getToken())
  const user = ref(getUser())

  const isLoggedIn = computed(() => !!token.value && !!user.value)

  async function login(credentials) {
    const response = await authApi.login(credentials)
    if (response.code === 200) {
      token.value = response.data.token
      user.value = response.data.user
      setToken(response.data.token)
      setUser(response.data.user)
    }
    return response
  }

  async function register(data) {
    const response = await authApi.register(data)
    if (response.code === 200) {
      token.value = response.data.token
      user.value = response.data.user
      setToken(response.data.token)
      setUser(response.data.user)
    }
    return response
  }

  function logout() {
    token.value = null
    user.value = null
    removeToken()
    removeUser()
  }

  return { token, user, isLoggedIn, login, register, logout }
})
