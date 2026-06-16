<template>
  <Teleport to="body">
    <div v-if="visible" class="modal-overlay" @click.self="handleClose">
      <div class="compare-modal">
        <div class="modal-header">
          <h3>基金对比</h3>
          <button class="close-btn" @click="handleClose">✕</button>
        </div>
        <div class="modal-body" v-if="loading">
          <div class="loading-text">加载中...</div>
        </div>
        <div class="modal-body" v-else-if="error">
          <div class="error-text">{{ error }}</div>
        </div>
        <div class="modal-body" v-else-if="compareData && compareData.funds && compareData.funds.length > 0">
          <div class="compare-table-wrapper">
            <table class="compare-table">
              <thead>
                <tr>
                  <th class="metric-col">指标</th>
                  <th v-for="fund in compareData.funds" :key="fund.fundCode" class="fund-col">
                    <div class="fund-header">
                      <div class="fund-name">{{ fund.fundName || fund.fundCode }}</div>
                      <div class="fund-code">{{ fund.fundCode }}</div>
                    </div>
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td class="metric-label">最新净值</td>
                  <td v-for="fund in compareData.funds" :key="fund.fundCode">
                    {{ fund.unitNetValue || '--' }}
                  </td>
                </tr>
                <tr>
                  <td class="metric-label">估算涨幅</td>
                  <td v-for="fund in compareData.funds" :key="fund.fundCode"
                    :class="getProfitClass(fund.estimatedChange)">
                    {{ formatPercent(fund.estimatedChange) }}
                  </td>
                </tr>
                <tr>
                  <td class="metric-label">自选以来</td>
                  <td v-for="fund in compareData.funds" :key="fund.fundCode"
                    :class="getProfitClass(fund.returnSinceAdded)">
                    {{ formatPercent(fund.returnSinceAdded) }}
                  </td>
                </tr>
                <tr>
                  <td class="metric-label">近1周</td>
                  <td v-for="(fund, idx) in compareData.funds" :key="fund.fundCode"
                    :class="{ 'best-value': isBestValue('oneWeekChange', idx) }">
                    <span :class="getProfitClass(fund.oneWeekChange)">
                      {{ formatPercent(fund.oneWeekChange) }}
                    </span>
                  </td>
                </tr>
                <tr>
                  <td class="metric-label">近1月</td>
                  <td v-for="(fund, idx) in compareData.funds" :key="fund.fundCode"
                    :class="{ 'best-value': isBestValue('oneMonthChange', idx) }">
                    <span :class="getProfitClass(fund.oneMonthChange)">
                      {{ formatPercent(fund.oneMonthChange) }}
                    </span>
                  </td>
                </tr>
                <tr>
                  <td class="metric-label">近3月</td>
                  <td v-for="(fund, idx) in compareData.funds" :key="fund.fundCode"
                    :class="{ 'best-value': isBestValue('threeMonthChange', idx) }">
                    <span :class="getProfitClass(fund.threeMonthChange)">
                      {{ formatPercent(fund.threeMonthChange) }}
                    </span>
                  </td>
                </tr>
                <tr>
                  <td class="metric-label">近6月</td>
                  <td v-for="(fund, idx) in compareData.funds" :key="fund.fundCode"
                    :class="{ 'best-value': isBestValue('sixMonthChange', idx) }">
                    <span :class="getProfitClass(fund.sixMonthChange)">
                      {{ formatPercent(fund.sixMonthChange) }}
                    </span>
                  </td>
                </tr>
                <tr>
                  <td class="metric-label">近1年</td>
                  <td v-for="(fund, idx) in compareData.funds" :key="fund.fundCode"
                    :class="{ 'best-value': isBestValue('oneYearChange', idx) }">
                    <span :class="getProfitClass(fund.oneYearChange)">
                      {{ formatPercent(fund.oneYearChange) }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="modal-body" v-else>
          <div class="empty-text">暂无对比数据</div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useWatchlistStore } from '../stores/watchlistStore'
import { formatPercent, getProfitClass } from '../composables/useFormat'

const props = defineProps({
  visible: Boolean,
  fundCodes: Array
})

const emit = defineEmits(['close'])

const store = useWatchlistStore()
const compareData = ref(null)
const loading = ref(false)
const error = ref('')

watch(() => props.visible, async (newVal) => {
  if (newVal && props.fundCodes && props.fundCodes.length >= 2) {
    loading.value = true
    error.value = ''
    compareData.value = null
    try {
      const response = await store.compareFunds(props.fundCodes)
      if (response.code === 200) {
        compareData.value = response.data
      } else {
        error.value = response.message || '对比数据获取失败'
      }
    } catch (err) {
      error.value = '对比数据获取失败'
    } finally {
      loading.value = false
    }
  }
})

function isBestValue(field, idx) {
  if (!compareData.value || !compareData.value.funds) return false
  const values = compareData.value.funds.map(f => f[field]).filter(v => v != null)
  if (values.length < 2) return false
  const fundValue = compareData.value.funds[idx]?.[field]
  if (fundValue == null) return false
  const maxVal = Math.max(...values)
  return fundValue === maxVal
}

function handleClose() {
  emit('close')
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.compare-modal {
  background: #fff;
  border-radius: 12px;
  width: 90%;
  max-width: 1000px;
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  color: #999;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}

.close-btn:hover {
  background: #f0f0f0;
  color: #333;
}

.modal-body {
  padding: 20px 24px;
  overflow-y: auto;
  flex: 1;
}

.loading-text,
.error-text,
.empty-text {
  text-align: center;
  padding: 40px;
  color: #999;
  font-size: 14px;
}

.compare-table-wrapper {
  overflow-x: auto;
}

.compare-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.compare-table th,
.compare-table td {
  padding: 12px 16px;
  text-align: center;
  border-bottom: 1px solid #f0f0f0;
}

.compare-table thead th {
  background: #fafafa;
  font-weight: 600;
  color: #333;
  position: sticky;
  top: 0;
  z-index: 1;
}

.metric-col {
  text-align: left !important;
  width: 100px;
  min-width: 100px;
}

.metric-label {
  text-align: left !important;
  font-weight: 500;
  color: #555;
  background: #fafafa;
}

.fund-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.fund-name {
  font-size: 14px;
  color: #333;
}

.fund-code {
  font-size: 12px;
  color: #999;
}

.profit-positive {
  color: #e53935;
}

.profit-negative {
  color: #009e5f;
}

.profit-zero {
  color: #999;
}

.best-value {
  background: #fff7e6;
}
</style>
