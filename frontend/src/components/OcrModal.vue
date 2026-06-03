<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="modal-overlay" @click.self="handleClose">
        <div class="modal-content">
          <div class="modal-header">
            <h3>图片识别基金</h3>
            <button class="close-btn" @click="handleClose">&times;</button>
          </div>

          <div class="modal-body">
            <!-- upload state -->
            <div v-if="step === 'upload'" class="dropzone" @dragover.prevent @drop.prevent="handleDrop">
              <input
                ref="fileInput"
                type="file"
                accept="image/*"
                class="file-input"
                @change="handleFileSelect"
              />
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#a0aec0" stroke-width="1.5">
                <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
                <circle cx="12" cy="13" r="4"/>
              </svg>
              <p class="dropzone-text">点击上传图片或拖拽到此处</p>
              <p class="dropzone-hint">支持 JPG、PNG、BMP 格式</p>
            </div>

            <!-- preview state -->
            <div v-if="step === 'preview'" class="preview-area">
              <img :src="previewUrl" class="preview-img" alt="预览" />
              <div class="preview-actions">
                <button class="action-btn primary-btn" @click="handleRecognize">开始识别</button>
                <button class="action-btn secondary-btn" @click="handleReselect">重新选择</button>
              </div>
            </div>

            <!-- loading state -->
            <div v-if="step === 'loading'" class="loading-area">
              <svg class="spinner" width="40" height="40" viewBox="0 0 40 40">
                <circle cx="20" cy="20" r="18" fill="none" stroke="#e2e8f0" stroke-width="3"/>
                <circle cx="20" cy="20" r="18" fill="none" stroke="#1677ff" stroke-width="3"
                  stroke-dasharray="85" stroke-dashoffset="60" stroke-linecap="round"/>
              </svg>
              <p>识别中...</p>
            </div>

            <!-- result state -->
            <div v-if="step === 'result'" class="result-area">
              <div v-if="fundCodes.length > 0" class="result-section">
                <p class="result-label">识别到 {{ fundCodes.length }} 个基金代码</p>
                <div
                  v-for="code in fundCodes"
                  :key="code"
                  class="result-item"
                  @click="handleSelectCode(code)"
                >
                  <span class="result-code">{{ code }}</span>
                  <span class="result-arrow">&rarr;</span>
                </div>
              </div>
              <div v-else-if="fundNames.length > 0" class="result-section">
                <p class="result-label">未识别到基金代码，以下为候选基金名称</p>
                <div
                  v-for="(name, idx) in fundNames"
                  :key="idx"
                  class="result-item"
                  @click="handleSelectName(name)"
                >
                  <span class="result-name">{{ name }}</span>
                  <span class="result-arrow">&rarr;</span>
                </div>
              </div>
              <div v-else class="result-empty">
                <p>未能识别到基金信息</p>
              </div>
              <div v-if="rawText" class="raw-text">
                <p class="raw-label">原始识别内容</p>
                <p class="raw-content">{{ rawText }}</p>
              </div>
              <div class="preview-actions" style="margin-top: 16px;">
                <button class="action-btn secondary-btn" @click="handleReselect">重新选择</button>
              </div>
            </div>

            <!-- error state -->
            <div v-if="step === 'error'" class="error-area">
              <p class="error-msg">{{ errorMsg }}</p>
              <div class="preview-actions">
                <button class="action-btn primary-btn" @click="handleRetry">重试</button>
                <button class="action-btn secondary-btn" @click="handleReselect">重新选择</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref } from 'vue'
import { ocrApi } from '../api'

defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'select-fund'])

const step = ref('upload')
const fileInput = ref(null)
const imageFile = ref(null)
const previewUrl = ref('')
const fundCodes = ref([])
const fundNames = ref([])
const rawText = ref('')
const errorMsg = ref('')

const resetState = () => {
  step.value = 'upload'
  imageFile.value = null
  previewUrl.value = ''
  fundCodes.value = []
  fundNames.value = []
  rawText.value = ''
  errorMsg.value = ''
}

const handleClose = () => {
  resetState()
  emit('close')
}

const handleFileSelect = (event) => {
  const file = event.target.files[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    errorMsg.value = '请选择图片文件'
    step.value = 'error'
    return
  }
  setFile(file)
}

const handleDrop = (event) => {
  const file = event.dataTransfer.files[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    errorMsg.value = '请选择图片文件'
    step.value = 'error'
    return
  }
  setFile(file)
}

const setFile = (file) => {
  imageFile.value = file
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewUrl.value = URL.createObjectURL(file)
  step.value = 'preview'
  errorMsg.value = ''
}

const handleReselect = () => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
  resetState()
}

const handleRecognize = async () => {
  if (!imageFile.value) return

  step.value = 'loading'
  errorMsg.value = ''

  try {
    const response = await ocrApi.recognize(imageFile.value)
    if (response.code === 200 && response.data) {
      fundCodes.value = response.data.fundCodes || []
      fundNames.value = response.data.fundNames || []
      rawText.value = response.data.rawText || ''
      step.value = 'result'
    } else {
      errorMsg.value = response.message || '识别失败，请重试'
      step.value = 'error'
    }
  } catch (err) {
    const detail = err.response?.data?.message || err.message || '未知错误'
    errorMsg.value = '网络请求失败: ' + detail
    step.value = 'error'
    console.error('OCR 请求失败:', err)
  }
}

const handleRetry = () => {
  handleRecognize()
}

const handleSelectCode = (code) => {
  emit('select-fund', code)
  handleClose()
}

const handleSelectName = (name) => {
  emit('select-fund', name)
  handleClose()
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: #fff;
  border-radius: 16px;
  width: 440px;
  max-width: 92vw;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f1f5f9;
}

.modal-header h3 {
  margin: 0;
  font-size: 17px;
  color: #1e293b;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  font-size: 22px;
  color: #94a3b8;
  cursor: pointer;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #f1f5f9;
  color: #334155;
}

.modal-body {
  padding: 24px;
}

.dropzone {
  border: 2px dashed #d4dce6;
  border-radius: 12px;
  padding: 40px 24px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s;
  position: relative;
}

.dropzone:hover {
  border-color: #1677ff;
  background: #f8f9ff;
}

.file-input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.dropzone-text {
  margin: 16px 0 8px;
  font-size: 15px;
  color: #475569;
  font-weight: 500;
}

.dropzone-hint {
  margin: 0;
  font-size: 13px;
  color: #94a3b8;
}

.preview-area {
  text-align: center;
}

.preview-img {
  max-width: 100%;
  max-height: 240px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  object-fit: contain;
}

.preview-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
  margin-top: 16px;
}

.action-btn {
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.primary-btn {
  background: linear-gradient(135deg, #1677ff, #1677ff);
  color: #fff;
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.3);
}

.primary-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.4);
}

.secondary-btn {
  background: #f1f5f9;
  color: #475569;
}

.secondary-btn:hover {
  background: #e2e8f0;
}

.loading-area {
  text-align: center;
  padding: 32px 0;
}

.spinner {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-area p {
  margin-top: 16px;
  color: #64748b;
  font-size: 14px;
}

.result-area {
  text-align: left;
}

.result-section {
  margin-bottom: 12px;
}

.result-label {
  font-size: 14px;
  color: #475569;
  margin: 0 0 12px;
  font-weight: 500;
}

.result-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.result-item:hover {
  background: #f0f4ff;
  border-color: #1677ff;
}

.result-code {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  letter-spacing: 2px;
}

.result-name {
  font-size: 15px;
  color: #1e293b;
  font-weight: 500;
}

.result-arrow {
  color: #1677ff;
  font-size: 18px;
  font-weight: 600;
}

.result-empty {
  text-align: center;
  padding: 24px 0;
  color: #94a3b8;
  font-size: 14px;
}

.raw-text {
  margin-top: 16px;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.raw-label {
  font-size: 12px;
  color: #94a3b8;
  margin: 0 0 6px;
}

.raw-content {
  font-size: 13px;
  color: #64748b;
  margin: 0;
  word-break: break-all;
}

.error-area {
  text-align: center;
}

.error-msg {
  color: #e53935;
  font-size: 14px;
  margin: 16px 0;
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
</style>
