<template>
  <div class="login-container">
    <div class="login-box">
      <h2 class="login-title">登录</h2>

      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-item">
          <label>邮箱</label>
          <input
            v-model="formData.email"
            type="email"
            placeholder="请输入邮箱"
            required
          />
        </div>

        <div class="form-item">
          <label>密码</label>
          <input
            v-model="formData.password"
            type="password"
            placeholder="请输入密码"
            required
          />
        </div>

        <div v-if="errorMsg" class="error-message">{{ errorMsg }}</div>

        <button type="submit" class="login-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>

        <div class="form-footer">
          <span>还没有账号？</span>
          <a @click="$emit('go-register')" class="link">立即注册</a>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { authApi, setToken, setUser } from '../api/auth'

const emit = defineEmits(['login-success', 'go-register'])

const formData = ref({
  email: '',
  password: ''
})

const loading = ref(false)
const errorMsg = ref('')

const handleLogin = async () => {
  errorMsg.value = ''
  loading.value = true

  try {
    const response = await authApi.login(formData.value)
    if (response.code === 200) {
      setToken(response.data.token)
      setUser(response.data.user)
      emit('login-success', response.data.user)
    } else {
      errorMsg.value = response.message || '登录失败'
    }
  } catch (error) {
    errorMsg.value = error.response?.data?.message || '登录失败，请检查邮箱和密码'
  } finally {
    loading.value = false
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
  padding: 20px;
}

.login-box {
  background: white;
  padding: 48px 40px;
  border-radius: 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
  width: 100%;
  max-width: 420px;
  position: relative;
  overflow: hidden;
}

.login-box::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
}

.login-title {
  text-align: center;
  color: #333;
  margin: 0 0 32px 0;
  font-size: 28px;
  font-weight: 700;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  color: #555;
  font-size: 14px;
  font-weight: 500;
}

.form-item input {
  padding: 14px 16px;
  border: 2px solid #e8e8e8;
  border-radius: 10px;
  font-size: 15px;
  transition: all 0.3s;
  background: #fafbfc;
}

.form-item input:focus {
  outline: none;
  border-color: #667eea;
  background: white;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
}

.error-message {
  color: #e74c3c;
  font-size: 14px;
  text-align: center;
  padding: 10px;
  background: #fff5f5;
  border-radius: 8px;
  border: 1px solid #ffd4d4;
}

.login-btn {
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.35);
  margin-top: 8px;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.45);
}

.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.form-footer {
  text-align: center;
  color: #666;
  font-size: 14px;
  margin-top: 8px;
}

.link {
  color: #667eea;
  cursor: pointer;
  margin-left: 5px;
  font-weight: 500;
  transition: color 0.3s;
}

.link:hover {
  color: #764ba2;
  text-decoration: underline;
}
</style>
