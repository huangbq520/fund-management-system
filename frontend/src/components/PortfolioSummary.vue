<template>
  <div class="portfolio-summary">
    <div class="summary-card">
      <button
        type="button"
        class="toggle-eye"
        :title="fundStore.hideAmount ? '点击显示金额' : '点击隐藏金额'"
        @click="fundStore.hideAmount = !fundStore.hideAmount"
        aria-label="toggle-amount-visibility"
      >
        <svg v-if="!fundStore.hideAmount" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8S1 12 1 12z"/>
          <circle cx="12" cy="12" r="3"/>
        </svg>
        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M17.94 17.94A10.94 10.94 0 0 1 12 20c-7 0-11-8-11-8a19.77 19.77 0 0 1 4.17-5.17"/>
          <path d="M9.9 4.24A10.94 10.94 0 0 1 12 4c7 0 11 8 11 8a19.5 19.5 0 0 1-2.16 3.19"/>
          <path d="M14.12 14.12a3 3 0 1 1-4.24-4.24"/>
          <line x1="1" y1="1" x2="23" y2="23"/>
        </svg>
      </button>

      <div class="summary-item">
        <span class="label">总资产</span>
        <span class="value">
          <template v-if="fundStore.hideAmount">******</template>
          <template v-else>{{ formatNumber(summary.totalAsset) }}</template>
        </span>
      </div>
      <div class="summary-divider"></div>
      <div class="summary-item">
        <span class="label">当日总收益</span>
        <span class="value" :class="fundStore.hideAmount ? '' : getProfitClass(summary.todayProfit)">
          <template v-if="fundStore.hideAmount">******</template>
          <template v-else>
            {{ formatProfit(summary.todayProfit) }} ({{ formatPercent(summary.todayProfitRate) }})
          </template>
        </span>
      </div>
      <div class="summary-divider"></div>
      <div class="summary-item">
        <span class="label">持仓收益率</span>
        <span class="value" :class="fundStore.hideAmount ? '' : getProfitClass(summary.totalProfitRate)">
          <template v-if="fundStore.hideAmount">******</template>
          <template v-else>{{ formatPercent(summary.totalProfitRate) }}</template>
        </span>
      </div>
    </div>

    <AllocationPieChart @view-detail="handleViewDetail" />
  </div>
</template>

<script setup>
import { useFundStore } from '../stores/fundStore'
import { storeToRefs } from 'pinia'
import { formatNumber, formatProfit, getProfitClass, formatPercent } from '../composables/useFormat'
import { useAutoRefresh, isTradingHours } from '../composables/useAutoRefresh'
import AllocationPieChart from './AllocationPieChart.vue'

const emit = defineEmits(['view-detail'])

const fundStore = useFundStore()
const { summary } = storeToRefs(fundStore)

useAutoRefresh(() => fundStore.silentFetchSummary(), 30000, isTradingHours)

const handleViewDetail = (fundCode) => {
  emit('view-detail', fundCode)
}
</script>

<style scoped>
.portfolio-summary {
  margin-bottom: 20px;
}

.summary-card {
  background: white;
  border-radius: 16px;
  padding: 24px 32px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  display: flex;
  align-items: center;
  justify-content: space-around;
  position: relative;
  overflow: hidden;
}

.summary-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #1677ff 0%, #1677ff 50%, #69b1ff 100%);
}

.toggle-eye {
  position: absolute;
  top: 10px;
  left: 14px;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: none;
  background: rgba(22, 119, 255, 0.08);
  color: #1677ff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 3;
  transition: background 0.2s ease, transform 0.15s ease, color 0.2s ease;
}

.toggle-eye svg {
  width: 18px;
  height: 18px;
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.toggle-eye:hover {
  background: rgba(22, 119, 255, 0.16);
}

.toggle-eye:active {
  transform: scale(0.9);
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  flex: 1;
  position: relative;
  z-index: 1;
}

.summary-item .label {
  font-size: 14px;
  color: #888;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.summary-item .value {
  font-size: 22px;
  font-weight: 700;
  color: #333;
  transition: color 0.3s;
}

.summary-divider {
  width: 1px;
  height: 50px;
  background: linear-gradient(180deg, transparent 0%, #e0e0e0 50%, transparent 100%);
  position: relative;
  z-index: 1;
}
</style>