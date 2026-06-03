<template>
  <div v-if="visible" class="quote-overlay" @click.self="emit('close')">
    <div class="quote-panel">
      <div class="quote-header">
        <div class="header-left">
          <h2>{{ indexName }}</h2>
          <span class="header-code">{{ indexCode }}</span>
        </div>
        <button class="close-btn" @click="emit('close')">&times;</button>
      </div>

      <div v-if="loading" class="quote-loading">
        <svg viewBox="0 0 240 240" height="240" width="240" class="pl">
          <circle stroke-linecap="round" stroke-dashoffset="-330" stroke-dasharray="0 660" stroke-width="20" stroke="#000" fill="none" r="105" cy="120" cx="120" class="pl__ring pl__ring--a"></circle>
          <circle stroke-linecap="round" stroke-dashoffset="-110" stroke-dasharray="0 220" stroke-width="20" stroke="#000" fill="none" r="35" cy="120" cx="120" class="pl__ring pl__ring--b"></circle>
          <circle stroke-linecap="round" stroke-dasharray="0 440" stroke-width="20" stroke="#000" fill="none" r="70" cy="120" cx="85" class="pl__ring pl__ring--c"></circle>
          <circle stroke-linecap="round" stroke-dasharray="0 440" stroke-width="20" stroke="#000" fill="none" r="70" cy="120" cx="155" class="pl__ring pl__ring--d"></circle>
        </svg>
        <span>加载中...</span>
      </div>

      <div v-else-if="error" class="quote-error">
        <span class="error-icon">!</span>
        <span>{{ error }}</span>
      </div>

      <template v-else>
        <div class="summary-section">
          <div class="price-row">
            <span class="current-price" :class="trendClass">{{ formatPrice(latestPrice) }}</span>
            <span class="price-change" :class="trendClass">
              {{ formatChange(latestChange) }} &nbsp; {{ formatPercent(latestChangePercent) }}
            </span>
          </div>
          <div class="metrics-grid">
            <div class="metric-item">
              <span class="metric-label">开盘价</span>
              <span class="metric-value">{{ formatPrice(summary.open) }}</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">最高价</span>
              <span class="metric-value up">{{ formatPrice(summary.high) }}</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">最低价</span>
              <span class="metric-value down">{{ formatPrice(summary.low) }}</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">成交量</span>
              <span class="metric-value">{{ formatVolume(summary.volume) }}</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">成交额</span>
              <span class="metric-value">{{ formatAmount(summary.amount) }}</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">振幅</span>
              <span class="metric-value">{{ formatPercent(summary.amplitude) }}</span>
            </div>
          </div>
        </div>

        <div class="period-bar">
          <button
            v-for="p in periodGroups"
            :key="p.value"
            :class="['period-btn', { active: currentKlt === p.value }]"
            @click="switchPeriod(p.value)"
          >
            {{ p.label }}
          </button>
        </div>

        <div class="chart-area">
          <div ref="mainChartRef" class="main-chart"></div>
          <div ref="volChartRef" class="vol-chart"></div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { marketApi } from '../api'

const echartsLib = window.echarts

const props = defineProps({
  indexCode: { type: String, required: true },
  indexName: { type: String, required: true },
  visible: { type: Boolean, default: false }
})

const emit = defineEmits(['close'])

const mainChartRef = ref(null)
const volChartRef = ref(null)
let mainChart = null
let volChart = null

const loading = ref(false)
const error = ref('')
const klineData = ref([])
const currentKlt = ref('101')

const periodGroups = [
  { label: '1分', value: '1' },
  { label: '5分', value: '5' },
  { label: '15分', value: '15' },
  { label: '30分', value: '30' },
  { label: '60分', value: '60' },
  { label: '日K', value: '101' },
  { label: '周K', value: '102' },
  { label: '月K', value: '103' },
  { label: '季K', value: '104' },
  { label: '年K', value: '105' }
]

const summary = computed(() => {
  if (!klineData.value.length) return {}
  const latest = klineData.value[klineData.value.length - 1]
  return {
    open: latest.open,
    high: latest.high,
    low: latest.low,
    volume: latest.volume,
    amount: latest.amount,
    amplitude: latest.amplitude
  }
})

const latestPrice = computed(() => {
  if (!klineData.value.length) return null
  return klineData.value[klineData.value.length - 1].close
})

const latestChange = computed(() => {
  if (!klineData.value.length) return null
  return klineData.value[klineData.value.length - 1].change
})

const latestChangePercent = computed(() => {
  if (!klineData.value.length) return null
  return klineData.value[klineData.value.length - 1].changePercent
})

const trendClass = computed(() => {
  const c = latestChange.value
  if (c == null) return ''
  return Number(c) >= 0 ? 'up' : 'down'
})

const formatPrice = (val) => {
  if (val == null) return '--'
  return Number(val).toFixed(2)
}

const formatChange = (val) => {
  if (val == null) return '--'
  const n = Number(val)
  return n >= 0 ? `+${n.toFixed(2)}` : n.toFixed(2)
}

const formatPercent = (val) => {
  if (val == null) return '--'
  const n = Number(val)
  return n >= 0 ? `+${n.toFixed(2)}%` : `${n.toFixed(2)}%`
}

const formatVolume = (val) => {
  if (val == null) return '--'
  const n = Number(val)
  if (n >= 1e8) return (n / 1e8).toFixed(2) + '亿手'
  if (n >= 1e4) return (n / 1e4).toFixed(2) + '万手'
  return n.toLocaleString() + '手'
}

const formatAmount = (val) => {
  if (val == null) return '--'
  const n = Number(val)
  if (n >= 1e8) return (n / 1e8).toFixed(2) + '亿元'
  if (n >= 1e4) return (n / 1e4).toFixed(2) + '万元'
  return n.toLocaleString() + '元'
}

const getDateRange = (klt) => {
  const now = new Date()
  const end = toDateStr(now)
  let start
  switch (klt) {
    case '1':
    case '5':
    case '15':
    case '30':
    case '60':
      start = new Date(now)
      start.setDate(start.getDate() - 5)
      break
    case '101':
      start = new Date(now)
      start.setMonth(start.getMonth() - 6)
      break
    case '102':
      start = new Date(now)
      start.setFullYear(start.getFullYear() - 2)
      break
    case '103':
      start = new Date(now)
      start.setFullYear(start.getFullYear() - 3)
      break
    case '104':
    case '105':
      start = new Date(now)
      start.setFullYear(start.getFullYear() - 5)
      break
    default:
      start = new Date(now)
      start.setMonth(start.getMonth() - 6)
  }
  return { start: toDateStr(start), end }
}

const toDateStr = (d) => {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}${m}${day}`
}

const fetchKline = async (klt) => {
  const { start, end } = getDateRange(klt)
  loading.value = true
  error.value = ''

  try {
    const res = await marketApi.getKline(props.indexCode, start, end, klt)
    if (res.code === 200 && res.data?.klines) {
      klineData.value = res.data.klines
      await nextTick()
      renderCharts()
    } else {
      error.value = res.message || '加载失败'
    }
  } catch {
    error.value = '网络错误，请重试'
  } finally {
    loading.value = false
  }
}

const switchPeriod = (klt) => {
  if (currentKlt.value === klt) return
  currentKlt.value = klt
  fetchKline(klt)
}

// ---- Chart rendering ----
const colors = {
  up: '#e53935',
  down: '#009e5f',
  upBg: '#fde8e8',
  downBg: '#e8f8e8'
}

const renderCharts = () => {
  if (!mainChartRef.value || !volChartRef.value || !echartsLib) return
  if (!klineData.value.length) return

  disposeCharts()

  mainChart = echartsLib.init(mainChartRef.value)
  volChart = echartsLib.init(volChartRef.value)

  const dates = klineData.value.map(d => d.date)
  const ohlc = klineData.value.map(d => [d.open, d.close, d.low, d.high])
  const volumes = klineData.value.map(d => {
    const up = Number(d.close) >= Number(d.open)
    return {
      value: Number(d.volume),
      itemStyle: { color: up ? colors.up : colors.down }
    }
  })

  // 计算Y轴范围
  const allPrices = klineData.value.flatMap(d => [Number(d.high), Number(d.low)])
  const yMin = Math.min(...allPrices)
  const yMax = Math.max(...allPrices)
  const yPad = (yMax - yMin) * 0.05

  const mainOption = {
    grid: { left: '8%', right: '2%', top: '3%', bottom: '12%' },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: { color: '#94a3b8', fontSize: 10, interval: Math.max(Math.floor(dates.length / 8), 0) },
      splitLine: { show: false }
    },
    yAxis: {
      type: 'value',
      min: yMin - yPad,
      max: yMax + yPad,
      scale: true,
      splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
      axisLabel: { color: '#94a3b8', fontSize: 10, formatter: v => v.toFixed(0) }
    },
    dataZoom: [
      { type: 'inside', xAxisIndex: [0, 0], start: 0, end: 100 },
      { type: 'slider', xAxisIndex: [0, 0], start: 0, end: 100, bottom: '2%', height: 18,
        borderColor: '#e2e8f0', fillerColor: 'rgba(22,119,255,0.1)',
        handleStyle: { color: '#1677ff' }, textStyle: { fontSize: 10, color: '#94a3b8' } }
    ],
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      backgroundColor: 'rgba(15,23,42,0.92)',
      borderColor: 'transparent',
      borderRadius: 8,
      padding: [10, 14],
      textStyle: { color: '#e2e8f0', fontSize: 12 },
      formatter: (params) => {
        if (!params || !params.length) return ''
        const d = klineData.value[params[0].dataIndex]
        if (!d) return ''
        const changeClass = Number(d.close) >= Number(d.open) ? '#e53935' : '#009e5f'
        const sign = Number(d.change) >= 0 ? '+' : ''
        return `<div style="font-weight:600;margin-bottom:6px;">${d.date}</div>
          <div>开盘: <b>${d.open}</b></div>
          <div>收盘: <b>${d.close}</b></div>
          <div style="color:#e53935;">最高: <b>${d.high}</b></div>
          <div style="color:#009e5f;">最低: <b>${d.low}</b></div>
          <div style="margin-top:4px;color:${changeClass};">涨跌幅: <b>${sign}${d.changePercent}%</b></div>
          <div>成交量: <b>${formatVolume(d.volume)}</b></div>
          <div>成交额: <b>${formatAmount(d.amount)}</b></div>`
      }
    },
    series: [{
      type: 'candlestick',
      data: ohlc,
      itemStyle: {
        color: colors.up,
        color0: colors.down,
        borderColor: colors.up,
        borderColor0: colors.down
      },
      barMaxWidth: 20
    }]
  }

  const volMax = Math.max(...volumes.map(v => v.value))

  const volOption = {
    grid: { left: '8%', right: '2%', top: '3%', bottom: '12%' },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: { show: false },
      splitLine: { show: false }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: volMax * 1.8,
      splitNumber: 3,
      splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
      axisLabel: { color: '#94a3b8', fontSize: 9, formatter: v => {
        if (v >= 1e8) return (v / 1e8).toFixed(1) + '亿'
        if (v >= 1e4) return (v / 1e4).toFixed(0) + '万'
        return v
      }}
    },
    dataZoom: [
      { type: 'inside', xAxisIndex: [0, 0], start: 0, end: 100 },
      { type: 'slider', show: false, start: 0, end: 100 }
    ],
    series: [{
      type: 'bar',
      data: volumes,
      barWidth: '60%'
    }]
  }

  mainChart.setOption(mainOption)
  volChart.setOption(volOption)

  // 联动 dataZoom
  mainChart.on('dataZoom', (params) => {
    if (volChart && !volChart.isDisposed()) {
      volChart.dispatchAction({
        type: 'dataZoom',
        dataZoomIndex: 0,
        start: params.start != null ? params.start : mainChart.getOption().dataZoom[0].start,
        end: params.end != null ? params.end : mainChart.getOption().dataZoom[0].end
      })
    }
  })

  volChart.on('dataZoom', (params) => {
    if (mainChart && !mainChart.isDisposed()) {
      mainChart.dispatchAction({
        type: 'dataZoom',
        dataZoomIndex: 0,
        start: params.start != null ? params.start : volChart.getOption().dataZoom[0].start,
        end: params.end != null ? params.end : volChart.getOption().dataZoom[0].end
      })
    }
  })

  // 十字线联动
  mainChart.on('mousemove', (params) => {
    if (volChart && !volChart.isDisposed()) {
      volChart.dispatchAction({ type: 'showTip', seriesIndex: 0, dataIndex: params.dataIndex })
    }
  })
  mainChart.on('mouseout', () => {
    if (volChart && !volChart.isDisposed()) {
      volChart.dispatchAction({ type: 'hideTip' })
    }
  })
}

const disposeCharts = () => {
  if (mainChart && !mainChart.isDisposed()) { mainChart.dispose(); mainChart = null }
  if (volChart && !volChart.isDisposed()) { volChart.dispose(); volChart = null }
}

const handleResize = () => {
  if (mainChart && !mainChart.isDisposed()) mainChart.resize()
  if (volChart && !volChart.isDisposed()) volChart.resize()
}

watch(() => props.visible, (v) => {
  if (v && props.indexCode) {
    fetchKline(currentKlt.value)
  } else {
    disposeCharts()
  }
})

watch(() => props.indexCode, (newCode) => {
  if (props.visible && newCode) {
    currentKlt.value = '101'
    fetchKline('101')
  }
})

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  disposeCharts()
})
</script>

<style scoped>
.quote-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  z-index: 9999;
  padding-top: 60px;
  overflow-y: auto;
}

.quote-panel {
  background: white;
  border-radius: 20px;
  width: 95%;
  max-width: 1100px;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.25);
}

.quote-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 28px;
  border-bottom: 1px solid #f0f0f0;
  position: sticky;
  top: 0;
  background: white;
  border-radius: 20px 20px 0 0;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.header-left h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.header-code {
  font-size: 13px;
  color: #1677ff;
  background: rgba(22, 119, 255, 0.08);
  padding: 2px 10px;
  border-radius: 4px;
  font-family: monospace;
}

.close-btn {
  width: 36px; height: 36px;
  border: none;
  background: #f5f7fa;
  border-radius: 50%;
  font-size: 22px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  transition: all 0.3s;
}

.close-btn:hover {
  background: #e53935;
  color: white;
}

.quote-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 0;
  color: #888;
  gap: 16px;
}

.quote-error {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px;
  margin: 20px;
  color: #e53935;
  font-size: 15px;
  background: linear-gradient(135deg, #fff5f5, #ffe8e8);
  border-radius: 12px;
}

.error-icon {
  width: 28px; height: 28px;
  background: #e53935;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 16px;
}

/* ---- Summary ---- */
.summary-section {
  padding: 20px 28px;
  border-bottom: 1px solid #f0f0f0;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 16px;
  margin-bottom: 20px;
}

.current-price {
  font-size: 36px;
  font-weight: 700;
  color: #333;
  letter-spacing: -1px;
}

.price-change {
  font-size: 16px;
  font-weight: 500;
}

.price-row .up { color: #e53935; }
.price-row .down { color: #009e5f; }

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.metric-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: #f8fafc;
  border-radius: 10px;
  padding: 12px 14px;
}

.metric-label {
  font-size: 11px;
  color: #94a3b8;
}

.metric-value {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.metric-value.up { color: #e53935; }
.metric-value.down { color: #009e5f; }

/* ---- Period Bar ---- */
.period-bar {
  display: flex;
  gap: 4px;
  padding: 12px 28px;
  background: #f8fafc;
  border-bottom: 1px solid #f0f0f0;
  flex-wrap: wrap;
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

.period-btn:hover {
  color: #334155;
  background: rgba(255,255,255,0.6);
}

.period-btn.active {
  background: white;
  color: #1677ff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  font-weight: 600;
}

/* ---- Charts ---- */
.chart-area {
  padding: 20px 24px 24px;
}

.main-chart {
  width: 100%;
  height: 420px;
}

.vol-chart {
  width: 100%;
  height: 140px;
}

/* ---- Loading Spinner ---- */
.pl {
  width: 6em; height: 6em;
}
.pl__ring {
  animation: ringA 2s linear infinite;
}
.pl__ring--a { stroke: #f42f25; }
.pl__ring--b { animation-name: ringB; stroke: #ffdd00; }
.pl__ring--c { animation-name: ringC; stroke: #255ff4; }
.pl__ring--d { animation-name: ringD; stroke: #255ff4; }

@keyframes ringA {
  from, 4% { stroke-dasharray: 0 660; stroke-width: 20; stroke-dashoffset: -330; }
  12% { stroke-dasharray: 60 600; stroke-width: 30; stroke-dashoffset: -335; }
  32% { stroke-dasharray: 60 600; stroke-width: 30; stroke-dashoffset: -595; }
  40%, 54% { stroke-dasharray: 0 660; stroke-width: 20; stroke-dashoffset: -660; }
  62% { stroke-dasharray: 60 600; stroke-width: 30; stroke-dashoffset: -665; }
  82% { stroke-dasharray: 60 600; stroke-width: 30; stroke-dashoffset: -925; }
  90%, to { stroke-dasharray: 0 660; stroke-width: 20; stroke-dashoffset: -990; }
}

@keyframes ringB {
  from, 12% { stroke-dasharray: 0 220; stroke-width: 20; stroke-dashoffset: -110; }
  20% { stroke-dasharray: 20 200; stroke-width: 30; stroke-dashoffset: -115; }
  40% { stroke-dasharray: 20 200; stroke-width: 30; stroke-dashoffset: -195; }
  48%, 62% { stroke-dasharray: 0 220; stroke-width: 20; stroke-dashoffset: -220; }
  70% { stroke-dasharray: 20 200; stroke-width: 30; stroke-dashoffset: -225; }
  90% { stroke-dasharray: 20 200; stroke-width: 30; stroke-dashoffset: -305; }
  98%, to { stroke-dasharray: 0 220; stroke-width: 20; stroke-dashoffset: -330; }
}

@keyframes ringC {
  from { stroke-dasharray: 0 440; stroke-width: 20; stroke-dashoffset: 0; }
  8% { stroke-dasharray: 40 400; stroke-width: 30; stroke-dashoffset: -5; }
  28% { stroke-dasharray: 40 400; stroke-width: 30; stroke-dashoffset: -175; }
  36%, 58% { stroke-dasharray: 0 440; stroke-width: 20; stroke-dashoffset: -220; }
  66% { stroke-dasharray: 40 400; stroke-width: 30; stroke-dashoffset: -225; }
  86% { stroke-dasharray: 40 400; stroke-width: 30; stroke-dashoffset: -395; }
  94%, to { stroke-dasharray: 0 440; stroke-width: 20; stroke-dashoffset: -440; }
}

@keyframes ringD {
  from { stroke-dasharray: 0 440; stroke-width: 20; stroke-dashoffset: 0; }
  8% { stroke-dasharray: 40 400; stroke-width: 30; stroke-dashoffset: -5; }
  28% { stroke-dasharray: 40 400; stroke-width: 30; stroke-dashoffset: -175; }
  36%, 58% { stroke-dasharray: 0 440; stroke-width: 20; stroke-dashoffset: -220; }
  66% { stroke-dasharray: 40 400; stroke-width: 30; stroke-dashoffset: -225; }
  86% { stroke-dasharray: 40 400; stroke-width: 30; stroke-dashoffset: -395; }
  94%, to { stroke-dasharray: 0 440; stroke-width: 20; stroke-dashoffset: -440; }
}

@media (max-width: 768px) {
  .metrics-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .main-chart { height: 280px; }
  .vol-chart { height: 100px; }
  .current-price { font-size: 28px; }
}
</style>
