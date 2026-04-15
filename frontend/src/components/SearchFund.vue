<template>
  <div class="search-fund">
    <div class="search-box">
      <input
        v-model="fundCode"
        type="text"
        placeholder="请输入基金代码"
        @keyup.enter="handleSearch"
        class="search-input"
      />
      <button @click="handleSearch" :disabled="loading" class="search-btn">
        {{ loading ? '搜索中...' : '搜索' }}
      </button>
    </div>
    
    <!-- Search Result -->
    <div v-if="searchResult" class="search-result">
      <div class="result-info">
        <span class="fund-name">{{ searchResult.fundName }}</span>
        <span class="fund-code">{{ searchResult.fundCode }}</span>
      </div>
      <div class="result-data">
        <div class="data-item">
          <span class="label">估算净值:</span>
          <span class="value">{{ searchResult.gsz || '-' }}</span>
        </div>
        <div class="data-item">
          <span class="label">估算涨跌幅:</span>
          <span 
            class="value" 
            :class="getChangeClass(searchResult.gszzl)"
          >
            {{ formatPercent(searchResult.gszzl) }}
          </span>
        </div>
      </div>
      <button @click="handleAdd" class="add-btn">添加</button>
    </div>
    
    <!-- Error Message -->
    <div v-if="error" class="error-msg">
      {{ error }}
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { fundApi } from '../api'

const emit = defineEmits(['add-fund'])

const fundCode = ref('')
const loading = ref(false)
const searchResult = ref(null)
const error = ref('')

const handleSearch = async () => {
  if (!fundCode.value.trim()) {
    error.value = '请输入基金代码'
    return
  }
  
  loading.value = true
  error.value = ''
  searchResult.value = null
  
  try {
    const response = await fundApi.search(fundCode.value)
    if (response.code === 200 && response.data) {
      searchResult.value = response.data
    } else {
      error.value = response.message || '基金不存在'
    }
  } catch (err) {
    error.value = '搜索失败，请稍后重试'
    console.error(err)
  } finally {
    loading.value = false
  }
}

const handleAdd = async () => {
  if (!searchResult.value) return
  
  try {
    const response = await fundApi.add(
      searchResult.value.fundCode,
      searchResult.value.fundName
    )
    if (response.code === 200) {
      alert('添加成功！')
      fundCode.value = ''
      searchResult.value = null
      emit('add-fund')
    } else {
      alert(response.message || '添加失败')
    }
  } catch (err) {
    alert('添加失败，请稍后重试')
    console.error(err)
  }
}

const formatPercent = (value) => {
  if (value === null || value === undefined) return '-'
  return (value > 0 ? '+' : '') + value.toFixed(2) + '%'
}

const getChangeClass = (value) => {
  if (value === null || value === undefined) return ''
  return value > 0 ? 'positive' : value < 0 ? 'negative' : ''
}
</script>

<style scoped>
.search-fund {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.search-box {
  display: flex;
  gap: 10px;
}

.search-input {
  flex: 1;
  padding: 12px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 16px;
  transition: border-color 0.3s;
}

.search-input:focus {
  outline: none;
  border-color: #667eea;
}

.search-btn {
  padding: 12px 24px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: background 0.3s;
}

.search-btn:hover:not(:disabled) {
  background: #5568d3;
}

.search-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.search-result {
  margin-top: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.result-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.fund-name {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.fund-code {
  font-size: 14px;
  color: #666;
}

.result-data {
  display: flex;
  gap: 20px;
  margin-bottom: 12px;
}

.data-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.label {
  color: #666;
  font-size: 14px;
}

.value {
  font-size: 16px;
  font-weight: 500;
}

.positive {
  color: #e74c3c;
}

.negative {
  color: #27ae60;
}

.add-btn {
  padding: 8px 20px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.3s;
}

.add-btn:hover {
  background: #5568d3;
}

.error-msg {
  margin-top: 12px;
  padding: 10px;
  background: #fee;
  color: #c33;
  border-radius: 6px;
  font-size: 14px;
}
</style>