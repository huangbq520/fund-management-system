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
        @click="viewDetail(fund.fundCode)"
      >
        <div class="fund-info">
          <div class="fund-name">{{ fund.fundName || fund.fundCode }}</div>
          <div class="fund-code">{{ fund.fundCode }}</div>
        </div>

        <div class="fund-data">
          <div class="data-item">
            <span class="label">估算净值</span>
            <span class="value">{{ fundDataMap[fund.fundCode]?.gsz || '-' }}</span>
          </div>
          <div class="data-item">
            <span class="label">涨跌幅</span>
            <span
              class="value"
              :class="getChangeClass(fundDataMap[fund.fundCode]?.gszzl)"
            >
              {{ formatPercent(fundDataMap[fund.fundCode]?.gszzl) }}
            </span>
          </div>
          <div class="data-item">
            <span class="label">估值时间</span>
            <span class="value">{{ fundDataMap[fund.fundCode]?.gztime || '-' }}</span>
          </div>
        </div>

        <div class="fund-actions">
          <button @click.stop="deleteFund(fund.fundCode)" class="delete-btn" title="删除">
            ✕
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
const fundDataMap = ref({})
const loading = ref(false)

const loadFunds = async () => {
  loading.value = true
  try {
    const response = await fundApi.list()
    if (response.code === 200) {
      funds.value = response.data || []
      await loadFundData()
    }
  } catch (err) {
    console.error('Failed to load funds:', err)
  } finally {
    loading.value = false
  }
}

const loadFundData = async () => {
  const dataMap = {}
  const promises = funds.value.map(async (fund) => {
    try {
      const resp = await fundApi.getFundData(fund.fundCode)
      if (resp.code === 200 && resp.data) {
        dataMap[fund.fundCode] = resp.data
      }
    } catch (err) {
      console.error(`Failed to load fund data for ${fund.fundCode}:`, err)
    }
  })
  await Promise.all(promises)
  fundDataMap.value = dataMap
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
  setInterval(loadFunds, 30000)
})

defineExpose({ refreshList })
</script>

<style scoped>
.fund-list {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.list-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.refresh-btn {
  padding: 8px 18px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf0 100%);
  color: #666;
  border: none;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s;
}

.refresh-btn:hover {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.loading, .empty {
  text-align: center;
  padding: 50px;
  color: #999;
  font-size: 15px;
}

.fund-item {
  display: flex;
  align-items: center;
  padding: 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  border-radius: 12px;
  margin-bottom: 8px;
  background: white;
  border: 1px solid transparent;
}

.fund-item:hover {
  background: linear-gradient(135deg, #f8f9ff 0%, #f0f4ff 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.15);
  border-color: rgba(102, 126, 234, 0.2);
}

.fund-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.fund-info {
  flex: 1;
  min-width: 120px;
}

.fund-name {
  font-size: 15px;
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
  gap: 24px;
  flex: 2;
}

.data-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 80px;
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
  align-items: center;
}

.delete-btn {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 50%;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fee;
  color: #e74c3c;
}

.delete-btn:hover {
  background: #fdd;
  transform: scale(1.1);
}
</style>