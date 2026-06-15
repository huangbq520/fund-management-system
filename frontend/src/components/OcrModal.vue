<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="modal-overlay" @click.self="handleClose">
        <div class="modal-content" :class="{ tall: step === 'result' && recognizedItems.length > 6 }">
          <div class="modal-header">
            <div class="header-left">
              <svg v-if="step === 'upload' || step === 'preview' || step === 'loading'" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#1677ff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                <circle cx="8.5" cy="8.5" r="1.5"/>
                <polyline points="21 15 16 10 5 21"/>
              </svg>
              <svg v-else-if="step === 'error'" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#e53935" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="8" x2="12" y2="12"/>
                <line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
              <svg v-else width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#1677ff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="11" cy="11" r="8"/>
                <path d="m21 21-4.3-4.3"/>
              </svg>
              <h3>{{ headerTitle }}</h3>
            </div>
            <button class="close-btn" @click="handleClose">&times;</button>
          </div>

          <div class="modal-body">
            <!-- upload -->
            <div v-if="step === 'upload'" class="dropzone" @dragover.prevent="handleDragOver" @dragleave.prevent="handleDragLeave" @drop.prevent="handleDrop" :class="{ hover: dragHover }" @click="triggerFileInput">
              <input
                ref="fileInput"
                type="file"
                accept="image/*"
                class="file-input"
                @change="handleFileSelect"
              />
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#a0aec0" stroke-width="1.5">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                <polyline points="17 8 12 3 7 8"/>
                <line x1="12" y1="3" x2="12" y2="15"/>
              </svg>
              <p class="dropzone-text">点击上传图片或拖拽到此处</p>
              <p class="dropzone-hint">支持 JPG、PNG 格式；推荐截图包含基金代码</p>
            </div>

            <!-- preview -->
            <div v-if="step === 'preview'" class="preview-area">
              <img :src="previewUrl" class="preview-img" alt="预览" />
              <div class="preview-actions">
                <button class="action-btn primary-btn" @click="handleRecognize">开始识别</button>
                <button class="action-btn secondary-btn" @click="handleReselect">重新选择</button>
              </div>
            </div>

            <!-- loading -->
            <div v-if="step === 'loading'" class="loading-area">
              <svg class="spinner" width="44" height="44" viewBox="0 0 40 40">
                <circle cx="20" cy="20" r="18" fill="none" stroke="#e2e8f0" stroke-width="3"/>
                <circle cx="20" cy="20" r="18" fill="none" stroke="#1677ff" stroke-width="3"
                  stroke-dasharray="85" stroke-dashoffset="60" stroke-linecap="round"/>
              </svg>
              <p>{{ loadingText }}</p>
              <p v-if="loadingProgress" class="loading-progress">{{ loadingProgress }}</p>
            </div>

            <!-- result -->
            <div v-if="step === 'result'" class="result-area">
              <div class="result-summary" :class="{ empty: !hasAnyRecognized }">
                <div class="summary-icon" :class="{ warn: !hasAnyRecognized }">
                  <svg v-if="hasAnyRecognized" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="20 6 9 17 4 12"/>
                  </svg>
                  <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="15" y1="9" x2="9" y2="15"/>
                    <line x1="9" y1="9" x2="15" y2="15"/>
                  </svg>
                </div>
                <div class="summary-text">
                  <div class="summary-title">
                    <template v-if="hasAnyRecognized">识别完成 · 共 {{ totalCount }} 个结果</template>
                    <template v-else>未能识别到基金信息</template>
                  </div>
                  <div class="summary-sub">
                    <template v-if="hasAnyRecognized">
                      请勾选要添加的基金，或点击下方「全选」批量操作
                    </template>
                    <template v-else>
                      请尝试更清晰的图片，或手动输入基金代码
                    </template>
                  </div>
                </div>
              </div>

              <template v-if="hasAnyRecognized">
                <div class="result-toolbar">
                  <div class="toolbar-stats">
                    <span class="stat stat-selected" v-if="selectedCount > 0">
                      已选 <strong>{{ selectedCount }}</strong>
                    </span>
                    <span class="stat stat-fetched" v-if="fetchedCount > 0">
                      已核验 <strong>{{ fetchedCount }}</strong>
                    </span>
                    <span class="stat stat-pending" v-if="pendingCount > 0">
                      正在核验 <strong>{{ pendingCount }}</strong>
                    </span>
                    <span class="stat stat-invalid" v-if="invalidCount > 0">
                      无效 <strong>{{ invalidCount }}</strong>
                    </span>
                  </div>
                  <button
                    class="select-all-btn"
                    @click="toggleSelectAll"
                    type="button"
                    :disabled="isEnriching"
                  >
                    {{ isAllSelected ? '取消全选' : '全选' }}
                  </button>
                </div>

                <div class="result-list" :class="{ 'has-scroll': recognizedItems.length > 5 }">
                  <div
                    v-for="item in recognizedItems"
                    :key="item.key"
                    class="result-row"
                    :class="{ selected: item.selected, invalid: item.state === 'invalid', pending: item.state === 'loading' }"
                  >
                    <div class="row-checkbox" @click.stop="toggleItem(item)">
                      <div class="cb" :class="{ checked: item.selected, disabled: item.state === 'invalid' }">
                        <svg v-if="item.selected" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round">
                          <polyline points="20 6 9 17 4 12"/>
                        </svg>
                      </div>
                    </div>

                    <div class="row-main" @click="toggleItem(item)">
                      <div class="row-head">
                        <span class="row-tag" :class="item.type">
                          {{ item.type === 'code' ? '代码' : '名称' }}
                        </span>
                        <span class="row-code" v-if="item.type === 'code'">{{ item.query }}</span>
                        <span class="row-name" v-else>{{ item.query }}</span>

                        <span v-if="item.state === 'loading'" class="row-state loading">
                          <svg class="mini-spinner" width="12" height="12" viewBox="0 0 40 40">
                            <circle cx="20" cy="20" r="16" fill="none" stroke="#c9d3e0" stroke-width="4"/>
                            <circle cx="20" cy="20" r="16" fill="none" stroke="#1677ff" stroke-width="4" stroke-dasharray="60" stroke-dashoffset="40" stroke-linecap="round"/>
                          </svg>
                          正在查询
                        </span>
                        <span v-else-if="item.state === 'invalid'" class="row-state invalid">未找到对应基金</span>
                        <span v-else-if="item.state === 'dup'" class="row-state dup">已在持仓</span>
                        <span v-else-if="item.state === 'ok'" class="row-state ok">已核验</span>
                      </div>

                      <div v-if="item.fund" class="row-fund">
                        <span class="fund-name">{{ item.fund.fundName }}</span>
                        <span class="fund-code-inline">{{ item.fund.fundCode }}</span>
                        <span v-if="item.fund.categoryDesc" class="fund-category">{{ item.fund.categoryDesc }}</span>
                      </div>
                      <div v-else-if="item.state === 'invalid'" class="row-empty">
                        无法从公开数据源查询到此基金，已自动排除
                      </div>
                      <div v-else class="row-empty muted">
                        点击左侧选择，确认后将自动查询基金信息并添加到持仓
                      </div>
                    </div>
                  </div>
                </div>

                <div class="result-footer">
                  <button class="action-btn secondary-btn" @click="handleReselect" :disabled="isEnriching">重新识别</button>
                  <button
                    class="action-btn primary-btn"
                    @click="handleConfirm"
                    :disabled="selectedCount === 0 || isEnriching || batchAdding"
                  >
                    <template v-if="isEnriching">
                      <svg class="mini-spinner" width="12" height="12" viewBox="0 0 40 40">
                        <circle cx="20" cy="20" r="16" fill="none" stroke="rgba(255,255,255,0.5)" stroke-width="4"/>
                        <circle cx="20" cy="20" r="16" fill="none" stroke="#fff" stroke-width="4" stroke-dasharray="60" stroke-dashoffset="40" stroke-linecap="round"/>
                      </svg>
                      正在核验 {{ pendingCount }} / {{ selectedAtConfirm }}
                    </template>
                    <template v-else-if="batchAdding">
                      <svg class="mini-spinner" width="12" height="12" viewBox="0 0 40 40">
                        <circle cx="20" cy="20" r="16" fill="none" stroke="rgba(255,255,255,0.5)" stroke-width="4"/>
                        <circle cx="20" cy="20" r="16" fill="none" stroke="#fff" stroke-width="4" stroke-dasharray="60" stroke-dashoffset="40" stroke-linecap="round"/>
                      </svg>
                      添加中…
                    </template>
                    <template v-else>
                      确认查询并添加 {{ selectedCount }} 只基金
                    </template>
                  </button>
                </div>

                <!-- batch result -->
                <Transition name="raw">
                  <div v-if="batchResult" class="batch-result" :class="batchResultClass">
                    <div class="batch-title">
                      <svg v-if="batchResult.successCount > 0" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#2e7d32" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="20 6 9 17 4 12"/>
                      </svg>
                      <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#e53935" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="12" cy="12" r="10"/>
                        <line x1="15" y1="9" x2="9" y2="15"/>
                        <line x1="9" y1="9" x2="15" y2="15"/>
                      </svg>
                      {{ batchResult.successCount > 0 ? '添加完成' : '添加失败' }}
                    </div>
                    <div class="batch-line">
                      <span v-if="batchResult.successCount > 0" class="batch-chip ok">
                        新增 {{ batchResult.successCount }}
                      </span>
                      <span v-if="batchResult.skippedCount > 0" class="batch-chip skip">
                        已存在 {{ batchResult.skippedCount }}
                      </span>
                      <span v-if="batchResult.failedCount > 0" class="batch-chip fail">
                        失败 {{ batchResult.failedCount }}
                      </span>
                    </div>
                    <button class="action-btn primary-btn small" @click="handleClose">关闭</button>
                  </div>
                </Transition>
              </template>

              <div v-else class="result-empty-block">
                <button class="action-btn secondary-btn" @click="handleReselect">重新识别</button>
              </div>
            </div>

            <!-- error -->
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
import { ref, computed, onBeforeUnmount } from 'vue'
import { ocrApi, fundApi } from '../api'

const props = defineProps({
  visible: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'batch-added'])

const step = ref('upload') // upload | preview | loading | result | error
const fileInput = ref(null)
const imageFile = ref(null)
const previewUrl = ref('')
const rawText = ref('')
const errorMsg = ref('')
const loadingText = ref('识别中…')
const recognizedItems = ref([]) // [{ key, type, query, selected, state, fund }]
const pendingSearchCount = ref(0)
const batchAdding = ref(false)
const batchResult = ref(null)
const dragHover = ref(false)
const isEnriching = ref(false)
const selectedAtConfirm = ref(0)
let searchTimer = null

const headerTitle = computed(() => {
  if (step.value === 'upload' || step.value === 'preview') return '从图片识别基金'
  if (step.value === 'loading') return '识别中'
  if (step.value === 'error') return '识别失败'
  return '选择要添加的基金'
})

const loadingProgress = computed(() => {
  if (pendingSearchCount.value > 0) {
    return `正在核验基金信息… (剩余 ${pendingSearchCount.value})`
  }
  return ''
})

const totalCount = computed(() => recognizedItems.value.length)
const codeCount = computed(() => recognizedItems.value.filter(i => i.type === 'code').length)
const nameCount = computed(() => recognizedItems.value.filter(i => i.type === 'name').length)
const selectedCount = computed(() => recognizedItems.value.filter(i => i.selected && i.state !== 'invalid').length)
const fetchedCount = computed(() => recognizedItems.value.filter(i => i.state === 'ok' || i.state === 'dup').length)
const pendingCount = computed(() => recognizedItems.value.filter(i => i.state === 'loading').length)
const invalidCount = computed(() => recognizedItems.value.filter(i => i.state === 'invalid').length)
const isAllSelected = computed(() => {
  const valid = recognizedItems.value.filter(i => i.state !== 'invalid')
  return valid.length > 0 && valid.every(i => i.selected)
})
const hasAnyRecognized = computed(() => recognizedItems.value.length > 0)

const batchResultClass = computed(() => {
  if (!batchResult.value) return ''
  if (batchResult.value.successCount > 0 && batchResult.value.failedCount === 0) return 'all-ok'
  if (batchResult.value.successCount === 0 && batchResult.value.failedCount > 0) return 'all-fail'
  return 'mixed'
})

function resetState() {
  step.value = 'upload'
  imageFile.value = null
  previewUrl.value = ''
  rawText.value = ''
  errorMsg.value = ''
  recognizedItems.value = []
  pendingSearchCount.value = 0
  batchAdding.value = false
  batchResult.value = null
  isEnriching.value = false
  selectedAtConfirm.value = 0
  if (searchTimer) {
    clearTimeout(searchTimer)
    searchTimer = null
  }
}

function handleClose() {
  resetState()
  emit('close')
}

function triggerFileInput() {
  fileInput.value && fileInput.value.click()
}

function handleDragOver() {
  dragHover.value = true
}
function handleDragLeave() {
  dragHover.value = false
}

function handleFileSelect(event) {
  const file = event.target.files && event.target.files[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    errorMsg.value = '请选择图片文件'
    step.value = 'error'
    return
  }
  setFile(file)
}

function handleDrop(event) {
  dragHover.value = false
  const file = event.dataTransfer.files && event.dataTransfer.files[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    errorMsg.value = '请选择图片文件'
    step.value = 'error'
    return
  }
  setFile(file)
}

function setFile(file) {
  imageFile.value = file
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewUrl.value = URL.createObjectURL(file)
  step.value = 'preview'
  errorMsg.value = ''
}

function handleReselect() {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
  resetState()
}

async function handleRecognize() {
  if (!imageFile.value) return
  step.value = 'loading'
  loadingText.value = '识别中…'
  errorMsg.value = ''
  try {
    const response = await ocrApi.recognize(imageFile.value)
    if (response.code === 200 && response.data) {
      rawText.value = response.data.rawText || ''
      const codes = response.data.fundCodes || []
      const names = response.data.fundNames || []

      const items = []
      codes.forEach((c, idx) => {
        items.push({
          key: `c-${idx}-${c}`,
          type: 'code',
          query: c,
          selected: false,
          state: 'idle',
          fund: null
        })
      })
      names.forEach((n, idx) => {
        items.push({
          key: `n-${idx}-${n}`,
          type: 'name',
          query: n,
          selected: false,
          state: 'idle',
          fund: null
        })
      })

      recognizedItems.value = items
      step.value = 'result'
      // 注意：不再自动开始查询，留给用户手动选择后再点击「确认查询」
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

function handleRetry() {
  handleRecognize()
}

async function enrichFunds(items) {
  // 先查当前用户持仓，用于标记"已在持仓"
  let currentHoldings = []
  try {
    const resp = await fundApi.getHoldingList()
    if (resp && resp.code === 200 && resp.data) {
      currentHoldings = resp.data
    }
  } catch (e) {
    console.warn('获取持仓失败', e)
  }
  const holdingCodes = new Set(currentHoldings.map(h => h.fundCode).filter(Boolean))

  // 把将处理的 item 标记为 loading
  items.forEach(i => { i.state = 'loading'; i.fund = null })
  pendingSearchCount.value = items.length

  // 并发限制：最多 4 个同时查询
  const concurrency = 4
  let cursor = 0

  async function worker() {
    while (cursor < items.length) {
      const idx = cursor++
      const item = items[idx]
      if (!item) return

      // 如已是持仓代码，直接标 dup
      if (item.type === 'code' && holdingCodes.has(item.query)) {
        item.state = 'dup'
        item.fund = { fundCode: item.query, fundName: '已在持仓中', categoryDesc: '' }
        pendingSearchCount.value = Math.max(0, pendingSearchCount.value - 1)
        continue
      }

      try {
        const resp = await fundApi.searchFunds(item.query.trim())
        const list = resp && resp.code === 200 && Array.isArray(resp.data) ? resp.data : []
        // 精确匹配优先：基金代码完全一致 / 名称完全一致
        let matched = null
        if (item.type === 'code') {
          matched = list.find(f => f.fundCode === item.query.trim())
        } else {
          matched = list.find(f => f.fundName && f.fundName === item.query.trim())
        }
        if (!matched && list.length > 0) matched = list[0]

        if (matched) {
          // 匹配成功后再看是否已持有
          if (holdingCodes.has(matched.fundCode)) {
            item.state = 'dup'
            item.fund = matched
          } else {
            item.state = 'ok'
            item.fund = matched
          }
        } else {
          item.state = 'invalid'
          item.fund = null
          // 无效 -> 取消选中
          item.selected = false
        }
      } catch (err) {
        console.warn('查询基金失败', item.query, err)
        item.state = 'invalid'
        item.fund = null
        item.selected = false
      } finally {
        pendingSearchCount.value = Math.max(0, pendingSearchCount.value - 1)
      }
    }
  }

  const workers = []
  for (let i = 0; i < Math.min(concurrency, items.length); i++) workers.push(worker())
  await Promise.all(workers)
}

async function handleConfirm() {
  const selected = recognizedItems.value.filter(i => i.selected && i.state !== 'invalid')
  if (selected.length === 0) return

  isEnriching.value = true
  selectedAtConfirm.value = selected.length
  batchResult.value = null

  try {
    await enrichFunds(selected)
    // 查询完成后，自动触发批量添加（只对有效且仍被选中的项）
    await handleBatchAdd()
  } finally {
    isEnriching.value = false
    selectedAtConfirm.value = 0
  }
}

function toggleItem(item) {
  if (item.state === 'invalid') return
  item.selected = !item.selected
}

function toggleSelectAll() {
  const willSelect = !isAllSelected.value
  recognizedItems.value.forEach(i => {
    if (i.state !== 'invalid') i.selected = willSelect
  })
}

async function handleBatchAdd() {
  const selected = recognizedItems.value.filter(i => i.selected && i.state !== 'invalid')
  if (selected.length === 0) return

  batchAdding.value = true
  batchResult.value = null

  const payload = selected.map(i => ({
    fundCode: i.type === 'code' ? i.query : i.fund.fundCode,
    fundName: i.fund && i.fund.fundName ? i.fund.fundName : i.query
  }))

  try {
    const resp = await fundApi.addBatch(payload)
    if (resp.code === 200 && resp.data) {
      batchResult.value = {
        successCount: resp.data.successCount || 0,
        skippedCount: resp.data.skippedCount || 0,
        failedCount: resp.data.failedCount || 0,
        success: resp.data.success || [],
        skipped: resp.data.skipped || [],
        failed: resp.data.failed || []
      }

      // 把已成功的行打上 ok 并取消选中
      const addedCodes = new Set((batchResult.value.success || []).map(i => i.fundCode))
      recognizedItems.value.forEach(i => {
        const fc = i.type === 'code' ? i.query : (i.fund && i.fund.fundCode)
        if (addedCodes.has(fc)) {
          i.state = 'added'
          i.selected = false
        } else if (i.state === 'ok' && i.selected) {
          // 可能是 skipped（已存在）— 标记成 dup
          const skipItem = (batchResult.value.skipped || []).find(s => s.fundCode === fc)
          if (skipItem) {
            i.state = 'dup'
            i.selected = false
          }
        }
      })

      emit('batch-added', batchResult.value)
    } else {
      batchResult.value = {
        successCount: 0,
        skippedCount: 0,
        failedCount: payload.length,
        success: [], skipped: [], failed: [{ message: resp.message || '批量添加失败' }]
      }
    }
  } catch (err) {
    console.error('批量添加失败:', err)
    batchResult.value = {
      successCount: 0, skippedCount: 0, failedCount: payload.length,
      success: [], skipped: [], failed: [{ message: '网络请求失败，请稍后重试' }]
    }
  } finally {
    batchAdding.value = false
  }
}

onBeforeUnmount(() => {
  if (searchTimer) clearTimeout(searchTimer)
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 16px;
  transform: translateZ(0); /* 修复 Chrome sticky+backdrop-filter 层叠穿透 */
}

.modal-content {
  background: #fff;
  border-radius: 16px;
  width: 560px;
  max-width: 100%;
  max-height: 88vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
  overflow: hidden;
}

.modal-content.tall {
  width: 600px;
  max-height: 90vh;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eef1f5;
  background: linear-gradient(180deg, #fafcff 0%, #ffffff 100%);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-left h3 {
  margin: 0;
  font-size: 16px;
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
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.18s;
  line-height: 1;
}

.close-btn:hover {
  background: #f1f5f9;
  color: #334155;
}

.modal-body {
  overflow-y: auto;
  padding: 20px;
}

/* dropzone */
.dropzone {
  border: 2px dashed #cbd5e1;
  border-radius: 12px;
  padding: 40px 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.18s;
  background: #f8fafc;
}

.dropzone:hover, .dropzone.hover {
  border-color: #1677ff;
  background: #eef5ff;
}

.file-input {
  position: absolute;
  inset: 0;
  opacity: 0;
  pointer-events: none;
}

.dropzone-text {
  margin: 14px 0 6px;
  font-size: 15px;
  color: #334155;
  font-weight: 500;
}

.dropzone-hint {
  margin: 0;
  font-size: 12.5px;
  color: #94a3b8;
}

/* preview */
.preview-area { text-align: center; }
.preview-img {
  max-width: 100%;
  max-height: 260px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  object-fit: contain;
  background: #f8fafc;
}

.preview-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
  margin-top: 16px;
}

.action-btn {
  padding: 10px 22px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.18s;
  border: none;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.action-btn.small {
  padding: 6px 14px;
  font-size: 12.5px;
  border-radius: 8px;
}

.primary-btn {
  background: linear-gradient(135deg, #1677ff 0%, #2f89ff 100%);
  color: #fff;
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.3);
}
.primary-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(22, 119, 255, 0.35);
}
.primary-btn:disabled {
  background: #b9c7dc;
  box-shadow: none;
  cursor: not-allowed;
}

.secondary-btn {
  background: #f1f5f9;
  color: #475569;
}
.secondary-btn:hover {
  background: #e2e8f0;
}

/* loading */
.loading-area {
  text-align: center;
  padding: 32px 0 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.spinner { animation: spin 1s linear infinite; }
.mini-spinner { animation: spin 0.9s linear infinite; }

@keyframes spin {
  to { transform: rotate(360deg); }
}
.loading-area p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 14px;
}
.loading-progress {
  color: #94a3b8;
  font-size: 12.5px;
}

/* result */
.result-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.result-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: linear-gradient(135deg, #eef5ff 0%, #f6faff 100%);
  border: 1px solid #dbe9ff;
  border-radius: 10px;
}
.result-summary.empty {
  background: linear-gradient(135deg, #fff4f4 0%, #fff9f9 100%);
  border-color: #f5dede;
}

.summary-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: #1677ff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 10px rgba(22, 119, 255, 0.25);
}
.summary-icon.warn {
  background: #e53935;
  box-shadow: 0 4px 10px rgba(229, 57, 53, 0.25);
}

.summary-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.summary-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}
.summary-sub {
  font-size: 12.5px;
  color: #64748b;
}
.summary-sub strong {
  color: #1677ff;
  font-weight: 600;
  margin: 0 2px;
}

.result-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.toolbar-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.stat {
  font-size: 12px;
  color: #64748b;
  padding: 3px 8px;
  background: #f1f5f9;
  border-radius: 10px;
}
.stat strong { color: #334155; margin: 0 1px; }
.stat-selected { background: #e8f0ff; color: #1677ff; }
.stat-selected strong { color: #1677ff; }
.stat-fetched { background: #e8f8ee; color: #2e7d32; }
.stat-fetched strong { color: #2e7d32; }
.stat-pending { background: #fff6e5; color: #b7791f; }
.stat-pending strong { color: #b7791f; }
.stat-invalid { background: #fbe9e7; color: #c62828; }
.stat-invalid strong { color: #c62828; }

.select-all-btn {
  padding: 5px 12px;
  font-size: 12.5px;
  border: 1px solid #dbe3ee;
  background: #fff;
  color: #475569;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}
.select-all-btn:hover {
  border-color: #1677ff;
  color: #1677ff;
  background: #f5f9ff;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 340px;
  overflow-y: auto;
  padding-right: 4px;
}
.result-list.has-scroll::-webkit-scrollbar {
  width: 6px;
}
.result-list::-webkit-scrollbar-thumb {
  background: #d6dde5;
  border-radius: 3px;
}
.result-list::-webkit-scrollbar-thumb:hover {
  background: #b6bec7;
}

.result-row {
  display: flex;
  align-items: stretch;
  gap: 10px;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s;
}
.result-row:hover {
  border-color: #c5d4eb;
  background: #f8fbff;
}
.result-row.selected {
  border-color: #1677ff;
  background: #eef5ff;
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.08);
}
.result-row.invalid {
  opacity: 0.55;
  cursor: not-allowed;
  background: #fafafa;
  border-color: #e8e8e8;
}
.result-row.pending {
  background: #fefcf3;
  border-color: #f3e7c2;
}

.row-checkbox {
  display: flex;
  align-items: center;
  padding-top: 2px;
}
.cb {
  width: 18px;
  height: 18px;
  border-radius: 5px;
  border: 2px solid #c9d3e0;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  flex-shrink: 0;
}
.cb.checked {
  background: #1677ff;
  border-color: #1677ff;
}
.cb.disabled {
  background: #f1f5f9;
  border-color: #d8dce5;
}

.row-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.row-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.row-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 7px;
  border-radius: 6px;
  background: linear-gradient(135deg, #1677ff, #4096ff);
  color: #fff;
  letter-spacing: 0.5px;
  flex-shrink: 0;
}
.row-tag.name {
  background: linear-gradient(135deg, #8b5cf6, #a78bfa);
}

.row-code {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  font-variant-numeric: tabular-nums;
  letter-spacing: 1px;
}
.row-name {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.row-state {
  margin-left: auto;
  font-size: 11.5px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 20px;
  background: #f1f5f9;
  color: #64748b;
}
.row-state.ok {
  background: #e8f8ee;
  color: #2e7d32;
}
.row-state.invalid {
  background: #fbe9e7;
  color: #c62828;
}
.row-state.dup {
  background: #fff4e5;
  color: #b7791f;
}
.row-state.loading {
  background: #eef5ff;
  color: #1677ff;
}

.row-fund {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 12.5px;
  padding-left: 2px;
}
.fund-name { color: #475569; font-weight: 500; }
.fund-code-inline {
  color: #64748b;
  font-variant-numeric: tabular-nums;
  padding: 0 6px;
  background: #f1f5f9;
  border-radius: 5px;
  font-size: 11.5px;
}
.fund-category {
  padding: 1px 8px;
  background: #eef3ff;
  color: #4b6bd1;
  border-radius: 20px;
  font-size: 11px;
}
.row-empty {
  font-size: 12px;
  color: #94a3b8;
  padding-left: 2px;
}
.row-empty.muted {
  color: #b8c2d2;
  font-size: 11.5px;
}

.result-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #eef1f5;
  gap: 10px;
}

.result-footer .secondary-btn { flex-shrink: 0; }
.result-footer .primary-btn { flex: 1; justify-content: center; }

/* batch result */
.batch-result {
  margin-top: 8px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #f8fbff;
  border: 1px solid #dbe9ff;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.batch-result.all-ok {
  background: #e8f8ee;
  border-color: #bfe5c9;
}
.batch-result.all-fail {
  background: #fbe9e7;
  border-color: #f3c7c1;
}
.batch-result.mixed {
  background: #fff8e6;
  border-color: #f1dfa0;
}

.batch-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #1e293b;
  font-size: 13.5px;
}

.batch-line {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  flex: 1;
}
.batch-chip {
  font-size: 11.5px;
  padding: 3px 8px;
  border-radius: 10px;
  background: #e2e8f0;
  color: #475569;
}
.batch-chip.ok { background: #c8ecd4; color: #2e7d32; }
.batch-chip.skip { background: #ffe4b5; color: #a86a00; }
.batch-chip.fail { background: #f5c7c0; color: #c62828; }

.result-empty-block {
  text-align: center;
  padding: 8px 0;
}

.error-area { text-align: center; }
.error-msg {
  color: #c62828;
  font-size: 14px;
  margin: 16px 0;
  padding: 12px;
  background: #fbe9e7;
  border-radius: 8px;
}

/* transition */
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }

.raw-enter-active, .raw-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.raw-enter-from, .raw-leave-to { opacity: 0; transform: translateY(-4px); }
</style>
