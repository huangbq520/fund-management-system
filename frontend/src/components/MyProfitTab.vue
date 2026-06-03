<template>
  <div class="my-profit-tab">
    <div v-if="loading" class="loading">
      <svg viewBox="0 0 240 240" height="240" width="240" class="pl">
        <circle stroke-linecap="round" stroke-dashoffset="-330" stroke-dasharray="0 660" stroke-width="20" stroke="#000" fill="none" r="105" cy="120" cx="120" class="pl__ring pl__ring--a"></circle>
        <circle stroke-linecap="round" stroke-dashoffset="-110" stroke-dasharray="0 220" stroke-width="20" stroke="#000" fill="none" r="35" cy="120" cx="120" class="pl__ring pl__ring--b"></circle>
        <circle stroke-linecap="round" stroke-dasharray="0 440" stroke-width="20" stroke="#000" fill="none" r="70" cy="120" cx="85" class="pl__ring pl__ring--c"></circle>
        <circle stroke-linecap="round" stroke-dasharray="0 440" stroke-width="20" stroke="#000" fill="none" r="70" cy="120" cx="155" class="pl__ring pl__ring--d"></circle>
      </svg>
    </div>

    <div v-else-if="error" class="error">
      <div class="error-icon">🔐</div>
      <div class="error-text">{{ error }}</div>
    </div>

    <template v-else-if="data">
      <div class="summary-row">
        <div class="summary-item">
          <div class="summary-label">累计收益</div>
          <div class="summary-value" :class="profitClass(data.summary.totalProfit)">
            {{ formatProfit(data.summary.totalProfit) }}
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-label">累计收益率</div>
          <div class="summary-value" :class="profitClass(data.summary.totalReturnRate)">
            {{ formatPercent(data.summary.totalReturnRate) }}
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-label">日均收益</div>
          <div class="summary-value" :class="profitClass(data.summary.avgDailyProfit)">
            {{ formatProfit(data.summary.avgDailyProfit) }}
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-label">最大单日收益</div>
          <div class="summary-value" :class="profitClass(data.summary.maxDailyProfit)">
            {{ formatProfit(data.summary.maxDailyProfit) }}
          </div>
        </div>
      </div>

      <div class="period-row">
        <button
          v-for="p in periods"
          :key="p.value"
          :class="['period-btn', { active: period === p.value }]"
          @click="switchPeriod(p.value)"
        >{{ p.label }}</button>
      </div>

      <div class="chart-section">
        <div ref="chartRef" class="chart-canvas"></div>
      </div>

      <div class="detail-section">
        <h4>每日收益明细（{{ data.summary.tradingDays }} 个交易日）</h4>
        <div class="detail-table-wrapper">
          <table class="detail-table">
            <thead>
              <tr>
                <th>日期</th>
                <th>净值</th>
                <th>当日收益</th>
                <th>收益率</th>
                <th>市值</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in data.detailList" :key="item.recordDate">
                <td>{{ formatShortDate(item.recordDate) }}</td>
                <td>{{ item.netValue?.toFixed(4) || '-' }}</td>
                <td :class="profitClass(item.dailyProfit)">{{ formatProfit(item.dailyProfit) }}</td>
                <td :class="profitClass(item.dailyReturnRate)">{{ formatPercent(item.dailyReturnRate) }}</td>
                <td>{{ formatMoney(item.holdAmount) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <div v-else class="empty">
      暂无收益数据，每日收盘后将自动统计
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { fundApi } from '../api'

const echartsLib = window.echarts

const props = defineProps({
  fundCode: { type: String, required: true }
})

const period = ref('6month')
const data = ref(null)
const loading = ref(false)
const error = ref('')
const chartRef = ref(null)
let chartInstance = null

const periods = [
  { label: '近1月', value: '1month' },
  { label: '近3月', value: '3month' },
  { label: '近6月', value: '6month' },
  { label: '近1年', value: '1year' },
  { label: '近3年', value: '3year' },
  { label: '全部', value: 'all' }
]

const colors = {
  up: '#e53935',
  down: '#009e5f',
  zero: '#64748b',
  grid: '#f1f5f9',
  crosshair: '#94a3b8'
}

const fetchData = async () => {
  loading.value = true
  error.value = ''
  try {
    console.log('[MyProfitTab] 开始获取收益数据，基金代码:', props.fundCode, '周期:', period.value)
    const res = await fundApi.getFundDailyProfit(props.fundCode, period.value)
    console.log('[MyProfitTab] API 响应结果:', res)
    
    if (res.code === 200) {
      data.value = res.data
      console.log('[MyProfitTab] 设置数据:', data.value)
      // 等待两次 nextTick，确保 DOM 完全更新
      await nextTick()
      await nextTick()
      console.log('[MyProfitTab] nextTick 后 chartRef:', chartRef.value)
      renderChart()
    } else if (res.code === 401) {
      error.value = res.message || '请先登录后再查看收益数据'
    } else {
      error.value = res.message || '加载失败'
    }
  } catch (err) {
    console.error('[MyProfitTab] 获取数据失败:', err)
    error.value = '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const switchPeriod = (val) => {
  if (period.value === val) return
  period.value = val
  fetchData()
}

const profitClass = (val) => {
  if (val === null || val === undefined || Number(val) === 0) return ''
  return Number(val) > 0 ? 'profit-up' : 'profit-down'
}

const formatProfit = (val) => {
  if (val === null || val === undefined) return '+0.00'
  const n = Number(val)
  return (n >= 0 ? '+' : '') + n.toFixed(2)
}

const formatPercent = (val) => {
  if (val === null || val === undefined) return '0.00%'
  const n = Number(val)
  return (n >= 0 ? '+' : '') + n.toFixed(2) + '%'
}

const formatShortDate = (dateStr) => {
  if (!dateStr) return ''
  const parts = String(dateStr).split('-')
  return parts.length >= 3 ? `${parts[1]}-${parts[2]}` : dateStr
}

const formatMoney = (val) => {
  if (val === null || val === undefined) return '¥0.00'
  const n = Number(val)
  return '¥' + n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const renderChart = () => {
  console.log('===== renderChart 开始 =====')
  
  if (!echartsLib) {
    console.log('没有 echartsLib！')
    return
  }
  
  if (!data.value || !data.value.profitCurve || data.value.profitCurve.length === 0) {
    console.log('没有数据！')
    return
  }
  
  // 轮询检查 DOM 是否存在
  let attempts = 0
  const tryInit = () => {
    attempts++
    console.log(`尝试初始化图表，第 ${attempts} 次...`)
    console.log('chartRef.value:', chartRef.value)
    
    if (!chartRef.value) {
      if (attempts < 10) {
        setTimeout(tryInit, 100) // 100ms 后重试
      } else {
        console.log('图表容器 DOM 不存在！')
      }
      return
    }
    
    console.log('DOM 找到了！开始初始化 ECharts')
    if (chartInstance) {
      chartInstance.dispose()
      chartInstance = null
    }
    
    chartInstance = echartsLib.init(chartRef.value)
    
    const curve = data.value.profitCurve
    const dates = curve.map(p => formatShortDate(p.recordDate))
    const cumValues = curve.map(p => Number(p.cumulativeProfit))
    
    console.log('准备好数据了！')
    console.log('dates:', dates)
    console.log('cumValues:', cumValues)
    
    const option = {
      tooltip: { trigger: 'axis' },
      grid: { left: '10%', right: '10%', bottom: '10%', top: '10%' },
      xAxis: {
        type: 'category',
        data: dates
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          type: 'line',
          data: cumValues,
          smooth: true,
          areaStyle: { opacity: 0.3 }
        }
      ]
    }
    
    console.log('设置 option')
    chartInstance.setOption(option)
    console.log('===== renderChart 完成 =====')
  }
  
  tryInit()
}

const handleResize = () => {
  if (chartInstance) chartInstance.resize()
}

watch(() => props.fundCode, (newCode) => {
  if (newCode) {
    period.value = '6month'
    fetchData()
  }
})

// 监听数据变化，确保有数据时渲染图表
watch(() => data.value, async (newData) => {
  if (newData && newData.profitCurve && newData.profitCurve.length > 0) {
    console.log('数据变化，准备渲染图表')
    await nextTick()
    await nextTick()
    renderChart()
  }
}, { deep: true })

onMounted(async () => {
  if (props.fundCode) fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
.my-profit-tab {
  padding: 4px 0;
}

.loading, .error, .empty {
  text-align: center;
  padding: 60px 20px;
  color: #94a3b8;
  font-size: 14px;
}

.loading-spinner {
  width: 36px; height: 36px;
  border: 3px solid #e2e8f0;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
  margin: 0 auto 12px;
}
@keyframes spin { to { transform: rotate(360deg); } }

.error {
  color: #e53935;
  background: #fef2f2;
  border-radius: 8px;
  margin: 12px 0;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.summary-item {
  background: #f8fafc;
  border-radius: 10px;
  padding: 14px 16px;
  text-align: center;
  border: 1px solid #f1f5f9;
}

.summary-label {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 6px;
}

.summary-value {
  font-size: 18px;
  font-weight: 700;
  color: #334155;
}

.profit-up { color: #e53935 !important; }
.profit-down { color: #009e5f !important; }
.positive { color: #e53935; }

.period-row {
  display: flex;
  gap: 6px;
  margin-bottom: 16px;
  background: #f1f5f9;
  padding: 4px;
  border-radius: 10px;
  width: fit-content;
}

.period-btn {
  padding: 6px 14px;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}
.period-btn:hover { color: #334155; }
.period-btn.active {
  background: #fff;
  color: #1e293b;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.chart-section {
  margin-bottom: 20px;
  background: #fafbfc;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #f1f5f9;
}

.chart-canvas {
  width: 100%;
  height: 300px;
  background: #f8fafc;
  border-radius: 8px;
}

.detail-section h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.detail-table-wrapper {
  max-height: 320px;
  overflow-y: auto;
  border-radius: 8px;
  border: 1px solid #f1f5f9;
}

.detail-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.detail-table th {
  background: #f8fafc;
  padding: 10px 12px;
  text-align: center;
  font-weight: 600;
  color: #64748b;
  position: sticky;
  top: 0;
  z-index: 1;
}
.detail-table td {
  padding: 10px 12px;
  text-align: center;
  border-bottom: 1px solid #f1f5f9;
  color: #475569;
}
.detail-table tbody tr:hover { background: #f8fafc; }

.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 50px;
}

.error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.error-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.error-text {
  color: #64748b;
  font-size: 16px;
  line-height: 1.6;
}

.pl {
  width: 6em;
  height: 6em;
}

.pl__ring {
  animation: ringA 2s linear infinite;
}

.pl__ring--a {
  stroke: #f42f25;
}

.pl__ring--b {
  animation-name: ringB;
  stroke: #ffdd00;
}

.pl__ring--c {
  animation-name: ringC;
  stroke: #255ff4;
}

@keyframes ringA {
  from,
  4% {
    stroke-dasharray: 0 660;
    stroke-width: 20;
    stroke-dashoffset: -330;
  }

  12% {
    stroke-dasharray: 60 600;
    stroke-width: 30;
    stroke-dashoffset: -335;
  }

  32% {
    stroke-dasharray: 60 600;
    stroke-width: 30;
    stroke-dashoffset: -595;
  }

  40%,
  54% {
    stroke-dasharray: 0 660;
    stroke-width: 20;
    stroke-dashoffset: -660;
  }

  62% {
    stroke-dasharray: 60 600;
    stroke-width: 30;
    stroke-dashoffset: -665;
  }

  82% {
    stroke-dasharray: 60 600;
    stroke-width: 30;
    stroke-dashoffset: -925;
  }

  90%,
  to {
    stroke-dasharray: 0 660;
    stroke-width: 20;
    stroke-dashoffset: -990;
  }
}

@keyframes ringB {
  from,
  12% {
    stroke-dasharray: 0 220;
    stroke-width: 20;
    stroke-dashoffset: -110;
  }

  20% {
    stroke-dasharray: 20 200;
    stroke-width: 30;
    stroke-dashoffset: -115;
  }

  40% {
    stroke-dasharray: 20 200;
    stroke-width: 30;
    stroke-dashoffset: -195;
  }

  48%,
  62% {
    stroke-dasharray: 0 220;
    stroke-width: 20;
    stroke-dashoffset: -220;
  }

  70% {
    stroke-dasharray: 20 200;
    stroke-width: 30;
    stroke-dashoffset: -225;
  }

  90% {
    stroke-dasharray: 20 200;
    stroke-width: 30;
    stroke-dashoffset: -305;
  }

  98%,
  to {
    stroke-dasharray: 0 220;
    stroke-width: 20;
    stroke-dashoffset: -330;
  }
}

@keyframes ringC {
  from {
    stroke-dasharray: 0 440;
    stroke-width: 20;
    stroke-dashoffset: 0;
  }

  8% {
    stroke-dasharray: 40 400;
    stroke-width: 30;
    stroke-dashoffset: -5;
  }

  28% {
    stroke-dasharray: 40 400;
    stroke-width: 30;
    stroke-dashoffset: -175;
  }

  36%,
  58% {
    stroke-dasharray: 0 440;
    stroke-width: 20;
    stroke-dashoffset: -220;
  }

  66% {
    stroke-dasharray: 40 400;
    stroke-width: 30;
    stroke-dashoffset: -225;
  }

  86% {
    stroke-dasharray: 40 400;
    stroke-width: 30;
    stroke-dashoffset: -395;
  }

  94%,
  to {
    stroke-dasharray: 0 440;
    stroke-width: 20;
    stroke-dashoffset: -440;
  }
}
</style>
