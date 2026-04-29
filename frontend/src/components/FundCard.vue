<template>
  <div class="fund-card" :class="{ loading: loading }">
    <div class="card-header">
      <div class="fund-title">
        <span class="fund-name">{{ fundData.fundName || fundData.fundCode }}</span>
        <span class="fund-code">{{ fundData.fundCode }}</span>
      </div>
      <button @click="$emit('remove', fundData.fundCode)" class="remove-btn">×</button>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <span>加载中...</span>
    </div>

    <div v-else-if="error" class="error-state">
      {{ error }}
    </div>

    <template v-else>
      <div class="card-body">
        <div class="main-info">
          <div class="value-block">
            <span class="label">估算净值</span>
            <span class="value">{{ fundData.gsz || '-' }}</span>
          </div>
          <div class="value-block">
            <span class="label">估算涨幅</span>
            <span class="value change" :class="getChangeClass(fundData.gszzl)">
              {{ formatPercent(fundData.gszzl) }}
            </span>
          </div>
          <div class="value-block">
            <span class="label">单位净值</span>
            <span class="value">{{ fundData.dwjz || '-' }}</span>
          </div>
          <div class="value-block">
            <span class="label">昨日涨幅</span>
            <span class="value change" :class="getChangeClass(fundData.jrzf)">
              {{ formatPercent(fundData.jrzf) }}
            </span>
          </div>
        </div>

        <div class="time-info">
          <span class="label">估值时间:</span>
          <span class="time">{{ fundData.gztime || '-' }}</span>
        </div>
      </div>

      <div v-if="showActions" class="card-actions">
        <button @click="$emit('view-detail', fundData.fundCode)" class="action-btn detail-btn">
          查看详情
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
const props = defineProps({
  fundData: {
    type: Object,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  error: {
    type: String,
    default: ''
  },
  showActions: {
    type: Boolean,
    default: true
  }
})

defineEmits(['remove', 'view-detail'])

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
.fund-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s, box-shadow 0.2s;
}

.fund-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.15);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.fund-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.fund-name {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.fund-code {
  font-size: 14px;
  color: #999;
  background: #f5f5f5;
  padding: 2px 8px;
  border-radius: 4px;
}

.remove-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: #fee;
  color: #e74c3c;
  border-radius: 50%;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  transition: background 0.2s;
}

.remove-btn:hover {
  background: #fdd;
}

.loading-state,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px;
  color: #999;
}

.spinner {
  width: 30px;
  height: 30px;
  border: 3px solid #f0f0f0;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 10px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.error-state {
  color: #e74c3c;
}

.card-body {
  margin-bottom: 16px;
}

.main-info {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 12px;
}

.value-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.value-block .label {
  font-size: 12px;
  color: #999;
}

.value-block .value {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.value-block .value.change {
  font-size: 16px;
}

.positive {
  color: #e74c3c;
}

.negative {
  color: #27ae60;
}

.time-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #999;
}

.time {
  color: #666;
}

.card-actions {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.action-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.detail-btn {
  background: #667eea;
  color: white;
}

.detail-btn:hover {
  background: #5568d3;
}

@media (max-width: 768px) {
  .main-info {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>