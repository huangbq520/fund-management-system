<template>
  <div class="fund-list">
    <div class="list-header">
      <h2>我的基金</h2>
      <button @click="refreshList" class="refresh-btn">
        刷新
      </button>
    </div>
    
    <!-- Loading State -->
    <div v-if="loading" class="loading">
      加载中...
    </div>
    
    <!-- Empty State -->
    <div v-else-if="funds.length === 0" class="empty">
      暂无基金，请搜索添加
    </div>
    
    <!-- Fund List -->
    <div v-else class="list-content">
      <div 
        v-for="fund in funds" 
        :key="fund.fundCode" 
        class="fund-item"
      >
        <div class="fund-info">
          <div class="fund-name">{{ fund.fundName || fund.fundCode }}</div>
          <div class="fund-code">{{ fund.fundCode }}</div>
        </div>
        
        <div class="fund-data">
          <div class="data-item">
            <span class="label">估算净值</span>
            <span class="value">{{ fund.gsz || '-' }}</span>
          </div>
          <div class="data-item">
            <span class="label">涨跌幅</span>
            <span 
              class="value" 
              :class="getChangeClass(fund.gszzl)"
            >
              {{ formatPercent(fund.gszzl) }}
            </span>
          </div>
          <div class="data-item">
            <span class="label">估值时间</span>
            <span class="value">{{ fund.gztime || '-' }}</span>
          </div>
        </div>
        
        <div class="fund-actions">
          <button @click="viewDetail(fund.fundCode)" class="detail-btn">
            查看详情
          </button>
          <button @click="deleteFund(fund.fundCode)" class="delete-btn">
            删除
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { fundApi } from '../api'

const emit = defineEmits(['delete-fund', 'view-detail'])

const funds = ref([])
const loading = ref(false)

const loadFunds = async () => {
  loading.value = true
  try {
    const response = await fundApi.list()
    if (response.code === 200) {
      funds.value = response.data || []
    }
  } catch (err) {
    console.error('Failed to load funds:', err)
  } finally {
    loading.value = false
  }
}

const refreshList = () => {
  loadFunds()
}

const viewDetail = (fundCode) => {
  emit('view-detail', fundCode)
}

const deleteFund = async (fundCode) => {
  if (!confirm('确定要删除该基金吗？')) return
  
  try {
    const response = await fundApi.delete(fundCode)
    if (response.code === 200) {
      alert('删除成功')
      loadFunds()
      emit('delete-fund')
    } else {
      alert(response.message || '删除失败')
    }
  } catch (err) {
    alert('删除失败，请稍后重试')
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

// Load data on mount
onMounted(() => {
  loadFunds()
  // Auto refresh every 30 seconds
  setInterval(loadFunds, 30000)
})

// Expose refresh method for parent
defineExpose({ refreshList })
</script>

<style scoped>
.fund-list {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.list-header h2 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.refresh-btn {
  padding: 8px 16px;
  background: #f0f0f0;
  color: #666;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.3s;
}

.refresh-btn:hover {
  background: #e0e0e0;
}

.loading, .empty {
  text-align: center;
  padding: 40px;
  color: #999;
}

.fund-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.3s;
}

.fund-item:hover {
  background: #f8f9fa;
}

.fund-item:last-child {
  border-bottom: none;
}

.fund-info {
  flex: 1;
  min-width: 120px;
}

.fund-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.fund-code {
  font-size: 12px;
  color: #999;
}

.fund-data {
  display: flex;
  gap: 20px;
  flex: 2;
}

.data-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.data-item .label {
  font-size: 12px;
  color: #999;
}

.data-item .value {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.positive {
  color: #e74c3c;
}

.negative {
  color: #27ae60;
}

.fund-actions {
  display: flex;
  gap: 8px;
}

.detail-btn, .delete-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.detail-btn {
  background: #667eea;
  color: white;
}

.detail-btn:hover {
  background: #5568d3;
}

.delete-btn {
  background: #fee;
  color: #e74c3c;
}

.delete-btn:hover {
  background: #fdd;
}
</style>