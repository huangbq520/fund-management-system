import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { authApi, setToken, setUser, removeToken, removeUser, getToken, getUser } from '../api/auth'
import { useToast } from '../composables/useToast'

export const useAuthStore = defineStore('auth', () => {
  const router = useRouter()
  const toast = useToast()
  
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
      
      toast.success('登录成功！')
      
      // 尝试重定向到登录前的页面
      const redirectPath = sessionStorage.getItem('redirectAfterLogin')
      if (redirectPath) {
        sessionStorage.removeItem('redirectAfterLogin')
        router.push(redirectPath)
      }
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
      
      toast.success('注册成功！')
    }
    return response
  }

  function logout() {
    token.value = null
    user.value = null
    removeToken()
    removeUser()
    toast.info('已退出登录')
    router.push('/auth')
  }

  return { token, user, isLoggedIn, login, register, logout }
})
