<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-content">
      <div class="modal-header">
        <h2>基金详情 - {{ fundCode }}</h2>
        <button @click="handleClose" class="close-btn">×</button>
      </div>

      <div class="modal-body">
        <!-- Loading -->
        <div v-if="loading" class="loading">加载中...</div>

        <!-- Error -->
        <div v-else-if="error" class="error">{{ error }}</div>

        <!-- Content -->
        <template v-else-if="detail">
          <!-- Basic Info Card -->
          <div class="info-card">
            <h3>基本信息</h3>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">基金名称</span>
                <span class="value">{{ detail.fundName || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">基金代码</span>
                <span class="value">{{ detail.fundCode || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">单位净值</span>
                <span class="value">{{ detail.dwjz || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">估算净值</span>
                <span class="value">{{ detail.gsz || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">估算涨跌幅</span>
                <span class="value" :class="getChangeClass(detail.gszzl)">
                  {{ formatPercent(detail.gszzl) }}
                </span>
              </div>
              <div class="info-item">
                <span class="label">净值日期</span>
                <span class="value">{{ detail.jzrq || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">估值时间</span>
                <span class="value">{{ detail.gztime || '-' }}</span>
              </div>
            </div>
          </div>

          <!-- Trend Chart -->
          <div class="chart-card">
            <h3>近90天业绩走势</h3>
            <div ref="chartRef" class="chart-container"></div>
          </div>

          <!-- Holdings Table -->
          <div class="holdings-card">
            <h3>持仓股票 (前10)</h3>
            <table v-if="detail.holdings && detail.holdings.length > 0" class="holdings-table">
              <thead>
                <tr>
                  <th>股票代码</th>
                  <th>股票名称</th>
                  <th>占净值比例</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(holding, index) in detail.holdings" :key="index">
                  <td>{{ holding.stockCode }}</td>
                  <td>{{ holding.stockName }}</td>
                  <td>{{ holding.weight }}</td>
                </tr>
              </tbody>
            </table>
            <div v-else class="no-data">暂无持仓数据</div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { fundApi } from '../api'

const props = defineProps({
  fundCode: {
    type: String,
    required: true
  }
})

const emit = defineEmits(['close'])

const loading = ref(false)
const error = ref('')
const detail = ref(null)
const chartRef = ref(null)
let chartInstance = null

const loadDetail = async () => {
  loading.value = true
  error.value = ''

  try {
    const response = await fundApi.getFundData(props.fundCode)
    if (response.code === 200 && response.data) {
      detail.value = response.data
      await nextTick()
      setTimeout(() => {
        initChart()
      }, 100)
    } else {
      error.value = response.message || '加载失败'
    }
  } catch (err) {
    error.value = '加载失败，请稍后重试'
    console.error(err)
  } finally {
    loading.value = false
  }
}

const initChart = () => {
  const echartsLib = window.echarts || echarts

  if (!chartRef.value) {
    console.log('initChart: chartRef is null, retrying...')
    setTimeout(() => initChart(), 200)
    return
  }

  if (!echartsLib) {
    console.log('initChart: echarts not loaded yet, retrying...')
    setTimeout(() => initChart(), 200)
    return
  }

  if (!detail.value || !detail.value.historyTrend) {
    console.log('initChart: detail or historyTrend not ready')
    return
  }

  const trendData = detail.value.historyTrend
  if (trendData.length === 0) {
    console.log('initChart: trendData is empty')
    return
  }

  const dates = trendData.map(item => {
    const timestamp = Number(item.date)
    const date = new Date(timestamp)
    return `${date.getMonth() + 1}-${date.getDate()}`
  })

  const values = trendData.map(item => item.netValue)

  chartInstance = echartsLib.init(chartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: function (params) {
        const param = params[0]
        const date = new Date(trendData[param.dataIndex].date)
        return `${date.toLocaleDateString()}<br/>净值: ${param.value}`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLabel: {
        rotate: 45,
        interval: Math.floor(dates.length / 6)
      }
    },
    yAxis: {
      type: 'value',
      scale: true,
      axisLabel: {
        formatter: '{value}'
      }
    },
    series: [{
      name: '净值',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 4,
      lineStyle: {
        color: '#667eea',
        width: 2
      },
      itemStyle: {
        color: '#667eea'
      },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(102, 126, 234, 0.3)' },
            { offset: 1, color: 'rgba(102, 126, 234, 0.05)' }
          ]
        }
      },
      data: values
    }]
  }

  chartInstance.setOption(option)

  window.addEventListener('resize', () => {
    chartInstance && chartInstance.resize()
  })
}

const handleClose = () => {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
  emit('close')
}

const formatPercent = (value) => {
  if (value === null || value === undefined) return '-'
  return (value > 0 ? '+' : '') + value.toFixed(2) + '%'
}

const getChangeClass = (value) => {
  if (value === null || value === undefined) return ''
  return value > 0 ? 'positive' : value < 0 ? 'negative' : ''
}

onMounted(() => {
  loadDetail()
})
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
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 900px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.modal-header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: #f0f0f0;
  border-radius: 50%;
  font-size: 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
}

.close-btn:hover {
  background: #e0e0e0;
}

.modal-body {
  padding: 20px;
}

.loading, .error {
  text-align: center;
  padding: 40px;
  color: #999;
}

.error {
  color: #e74c3c;
}

.info-card, .chart-card, .holdings-card {
  margin-bottom: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.info-card h3, .chart-card h3, .holdings-card h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #333;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item .label {
  font-size: 12px;
  color: #999;
}

.info-item .value {
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

.chart-container {
  width: 100%;
  height: 300px;
}

.holdings-table {
  width: 100%;
  border-collapse: collapse;
}

.holdings-table th,
.holdings-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}

.holdings-table th {
  background: #f0f0f0;
  font-weight: 600;
  color: #333;
}

.holdings-table td {
  color: #666;
}

.holdings-table tr:hover {
  background: #f8f9fa;
}

.no-data {
  text-align: center;
  padding: 20px;
  color: #999;
}
</style>