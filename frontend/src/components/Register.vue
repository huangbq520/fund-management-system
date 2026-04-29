<template>
  <div class="register-container">
    <div class="register-box">
      <h2 class="register-title">注册</h2>

      <form @submit.prevent="handleRegister" class="register-form">
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
          <label>昵称（选填）</label>
          <input
            v-model="formData.nickname"
            type="text"
            placeholder="请输入昵称"
          />
        </div>

        <div class="form-item">
          <label>验证码</label>
          <div class="verify-code-row">
            <input
              v-model="formData.verifyCode"
              type="text"
              placeholder="请输入验证码"
              required
            />
            <button
              type="button"
              class="send-code-btn"
              @click="handleSendCode"
              :disabled="countdown > 0"
            >
              {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
            </button>
          </div>
        </div>

        <div class="form-item">
          <label>密码</label>
          <input
            v-model="formData.password"
            type="password"
            placeholder="请输入密码（至少6位）"
            required
          />
        </div>

        <div class="form-item">
          <label>确认密码</label>
          <input
            v-model="formData.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            required
          />
        </div>

        <div v-if="errorMsg" class="error-message">{{ errorMsg }}</div>
        <div v-if="successMsg" class="success-message">{{ successMsg }}</div>

        <button type="submit" class="register-btn" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>

        <div class="form-footer">
          <span>已有账号？</span>
          <a @click="$emit('go-login')" class="link">立即登录</a>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { authApi, setToken, setUser } from '../api/auth'

const emit = defineEmits(['register-success', 'go-login'])

const formData = ref({
  email: '',
  nickname: '',
  verifyCode: '',
  password: '',
  confirmPassword: ''
})

const loading = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const countdown = ref(0)

const handleSendCode = async () => {
  if (!formData.value.email) {
    errorMsg.value = '请输入邮箱'
    return
  }

  errorMsg.value = ''
  successMsg.value = ''

  try {
    await authApi.sendVerifyCode(formData.value.email)
    successMsg.value = '验证码已发送'
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error) {
    errorMsg.value = error.response?.data?.message || '发送验证码失败'
  }
}

const handleRegister = async () => {
  errorMsg.value = ''
  successMsg.value = ''

  if (formData.value.password.length < 6) {
    errorMsg.value = '密码长度不能少于6位'
    return
  }

  if (formData.value.password !== formData.value.confirmPassword) {
    errorMsg.value = '两次密码输入不一致'
    return
  }

  loading.value = true

  try {
    const response = await authApi.register({
      email: formData.value.email,
      nickname: formData.value.nickname,
      verifyCode: formData.value.verifyCode,
      password: formData.value.password,
      confirmPassword: formData.value.confirmPassword
    })

    if (response.code === 200) {
      setToken(response.data.token)
      setUser(response.data.user)
      emit('register-success', response.data.user)
    } else {
      errorMsg.value = response.message || '注册失败'
    }
  } catch (error) {
    errorMsg.value = error.response?.data?.message || '注册失败，请检查输入'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.register-box {
  background: white;
  padding: 40px;
  border-radius: 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
  width: 100%;
  max-width: 440px;
  position: relative;
  overflow: hidden;
}

.register-box::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
}

.register-title {
  text-align: center;
  color: #333;
  margin: 0 0 32px 0;
  font-size: 28px;
  font-weight: 700;
}

.register-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
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

.verify-code-row {
  display: flex;
  gap: 12px;
}

.verify-code-row input {
  flex: 1;
}

.send-code-btn {
  padding: 12px 18px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.25);
}

.send-code-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.35);
}

.send-code-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
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

.success-message {
  color: #27ae60;
  font-size: 14px;
  text-align: center;
  padding: 10px;
  background: #f0fff4;
  border-radius: 8px;
  border: 1px solid #b8e6c1;
}

.register-btn {
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

.register-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.45);
}

.register-btn:disabled {
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
