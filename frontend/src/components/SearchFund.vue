<template>
  <div class="search-fund">
    <div class="search-box" ref="searchBoxRef">
      <input
        v-model="searchKeyword"
        type="text"
        placeholder="输入基金代码或名称搜索"
        @input="handleInput"
        @focus="handleFocus"
        @keydown.down.prevent="navigateDown"
        @keydown.up.prevent="navigateUp"
        @keydown.enter.prevent="selectHighlighted"
        @keydown.escape="closeDropdown"
        class="search-input"
      />
      <button @click="handleSearch" :disabled="loading" class="search-btn">
        {{ loading ? '搜索中...' : '搜索' }}
      </button>

      <!-- Search Results Dropdown -->
      <div v-if="showDropdown && searchResults.length > 0" class="dropdown">
        <div
          v-for="(fund, index) in searchResults"
          :key="fund.fundCode"
          class="dropdown-item"
          :class="{ selected: selectedIndex === index }"
          @click="selectFund(fund)"
          @mouseenter="selectedIndex = index"
        >
          <div class="fund-basic">
            <span class="fund-name" v-html="highlightKeyword(fund.fundName)"></span>
            <span class="fund-code">{{ fund.fundCode }}</span>
          </div>
          <div class="fund-category">
            <span class="category-badge">{{ fund.categoryDesc }}</span>
          </div>
        </div>
      </div>

      <!-- No Results -->
      <div v-if="showDropdown && searchResults.length === 0 && hasSearched" class="dropdown no-results">
        未找到匹配的基金
      </div>
    </div>

    <!-- Error Message -->
    <div v-if="error" class="error-msg">
      {{ error }}
    </div>

    <!-- Selected Fund Preview -->
    <div v-if="selectedFund && !showDropdown" class="selected-preview">
      <div class="preview-info">
        <span class="preview-name">{{ selectedFund.fundName }}</span>
        <span class="preview-code">{{ selectedFund.fundCode }}</span>
      </div>
      <button @click="handleAdd" class="add-btn">添加</button>
      <button @click="clearSelection" class="clear-btn">清除</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { fundApi } from '../api'

const emit = defineEmits(['add-fund'])

const searchKeyword = ref('')
const searchResults = ref([])
const selectedFund = ref(null)
const selectedIndex = ref(-1)
const loading = ref(false)
const error = ref('')
const showDropdown = ref(false)
const hasSearched = ref(false)

let debounceTimer = null
let clickOutsideHandler = null

const handleInput = () => {
  error.value = ''
  selectedFund.value = null
  selectedIndex.value = -1

  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }

  debounceTimer = setTimeout(() => {
    if (searchKeyword.value.trim().length >= 1) {
      performSearch()
    } else {
      searchResults.value = []
      showDropdown.value = false
    }
  }, 300)
}

const handleFocus = () => {
  if (searchResults.value.length > 0) {
    showDropdown.value = true
  }
}

const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    error.value = '请输入基金代码或名称'
    return
  }

  await performSearch()
}

const performSearch = async () => {
  loading.value = true
  error.value = ''
  hasSearched.value = true
  showDropdown.value = true

  try {
    const response = await fundApi.searchFunds(searchKeyword.value)
    if (response.code === 200 && response.data) {
      searchResults.value = response.data
      selectedIndex.value = searchResults.value.length > 0 ? 0 : -1
    } else {
      searchResults.value = []
      error.value = response.message || '未找到匹配的基金'
    }
  } catch (err) {
    error.value = '搜索失败，请稍后重试'
    console.error(err)
  } finally {
    loading.value = false
  }
}

const navigateDown = () => {
  if (selectedIndex.value < searchResults.value.length - 1) {
    selectedIndex.value++
  }
}

const navigateUp = () => {
  if (selectedIndex.value > 0) {
    selectedIndex.value--
  }
}

const selectHighlighted = () => {
  if (selectedIndex.value >= 0 && selectedIndex.value < searchResults.value.length) {
    selectFund(searchResults.value[selectedIndex.value])
  }
}

const selectFund = (fund) => {
  selectedFund.value = fund
  searchKeyword.value = fund.fundName
  showDropdown.value = false
  error.value = ''
}

const closeDropdown = () => {
  showDropdown.value = false
}

const clearSelection = () => {
  selectedFund.value = null
  searchKeyword.value = ''
  searchResults.value = []
  selectedIndex.value = -1
}

const handleAdd = async () => {
  if (!selectedFund.value) return

  try {
    const response = await fundApi.add(
      selectedFund.value.fundCode,
      selectedFund.value.fundName
    )
    if (response.code === 200) {
      alert('添加成功！')
      clearSelection()
      emit('add-fund')
    } else {
      alert(response.message || '添加失败')
    }
  } catch (err) {
    alert('添加失败，请稍后重试')
    console.error(err)
  }
}

const highlightKeyword = (text) => {
  if (!text || !searchKeyword.value) return text
  const regex = new RegExp(`(${searchKeyword.value})`, 'gi')
  return text.replace(regex, '<span class="highlight">$1</span>')
}

const handleClickOutside = (event) => {
  const searchBox = document.querySelector('.search-box')
  if (searchBox && !searchBox.contains(event.target)) {
    showDropdown.value = false
  }
}

onMounted(() => {
  clickOutsideHandler = handleClickOutside
  document.addEventListener('click', clickOutsideHandler)
})

onUnmounted(() => {
  if (clickOutsideHandler) {
    document.removeEventListener('click', clickOutsideHandler)
  }
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
})
</script>

<style scoped>
.search-fund {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}

.search-box {
  position: relative;
}

.search-input {
  width: 100%;
  padding: 14px 100px 14px 18px;
  border: 2px solid #e8e8e8;
  border-radius: 12px;
  font-size: 16px;
  transition: all 0.3s;
  box-sizing: border-box;
  background: #fafbfc;
}

.search-input:focus {
  outline: none;
  border-color: #667eea;
  background: white;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
}

.search-btn {
  position: absolute;
  right: 4px;
  top: 4px;
  bottom: 4px;
  padding: 10px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.search-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.search-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  box-shadow: none;
}

.dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  max-height: 320px;
  overflow-y: auto;
  z-index: 1000;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

.dropdown-item {
  padding: 14px 18px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
  transition: all 0.2s;
}

.dropdown-item:last-child {
  border-bottom: none;
}

.dropdown-item:hover,
.dropdown-item.selected {
  background: linear-gradient(135deg, #f8f9ff 0%, #f0f4ff 100%);
}

.fund-basic {
  display: flex;
  align-items: center;
  gap: 12px;
}

.fund-name {
  font-size: 15px;
  font-weight: 500;
  color: #333;
}

.fund-code {
  font-size: 13px;
  color: #999;
}

.fund-category {
  display: flex;
  align-items: center;
}

.category-badge {
  font-size: 12px;
  padding: 4px 10px;
  background: linear-gradient(135deg, #e8f4ff 0%, #d4ecff 100%);
  color: #667eea;
  border-radius: 20px;
  font-weight: 500;
}

.highlight {
  background: linear-gradient(135deg, #fff3cd 0%, #ffeeba 100%);
  color: #e74c3c;
  padding: 0 2px;
  border-radius: 2px;
}

.no-results {
  padding: 24px;
  text-align: center;
  color: #999;
  font-size: 14px;
}

.selected-preview {
  margin-top: 16px;
  padding: 18px 20px;
  background: linear-gradient(135deg, #f8f9ff 0%, #f0f4ff 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid #e8f0ff;
}

.preview-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
}

.preview-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.preview-code {
  font-size: 14px;
  color: #666;
  background: white;
  padding: 2px 8px;
  border-radius: 4px;
}

.add-btn {
  padding: 10px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.add-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.clear-btn {
  padding: 10px 18px;
  background: white;
  color: #666;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.clear-btn:hover {
  background: #f5f5f5;
  border-color: #d0d0d0;
}

.error-msg {
  margin-top: 12px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #fff5f5 0%, #ffe8e8 100%);
  color: #e74c3c;
  border-radius: 8px;
  font-size: 14px;
  border: 1px solid #ffd4d4;
}
</style>