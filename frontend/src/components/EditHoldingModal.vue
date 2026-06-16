<template>
  <!-- 主弹窗：信息 + 卡片 + 按钮 -->
  <Teleport to="body">
    <Transition name="modal">
      <div class="modal-overlay" @click.self="handleClose">
        <div class="modal-card">
          <button class="close-btn" @click="handleClose">&times;</button>

          <!-- ====== 1. 顶部标题与净值信息区 ====== -->
          <div class="info-header">
            <h1 class="fund-name">{{ holding.fundName }}</h1>
            <p class="fund-code-text">{{ holding.fundCode }}</p>
            <div class="nav-line">
              <span class="nav-label">最新净值</span>
              <span class="nav-date">({{ navDateDisplay }})</span>
              <span class="nav-sep">:</span>
              <span class="nav-value">{{ fmtNAV }}</span>
              <span class="nav-change" :class="navChangeClass">{{ fmtNavChange }}</span>
            </div>
          </div>

          <!-- ====== 2. 持仓核心数据区 ====== -->
          <div class="holding-section">
            <div class="holding-cards">
              <div class="h-card">
                <span class="h-card-label">持有金额</span>
                <span class="h-card-value">{{ fmtMoney(holding.currentValue) }}</span>
              </div>
              <div class="h-card">
                <span class="h-card-label">持有收益</span>
                <span class="h-card-value" :class="profitClass(holdingProfit)">{{ fmtProfit(holdingProfit) }}</span>
              </div>
              <div class="h-card">
                <span class="h-card-label">持有天数</span>
                <span class="h-card-value">{{ holdDays }} 天</span>
              </div>
            </div>
          </div>

          <!-- ====== 3. 功能操作按钮区 ====== -->
          <div class="action-section">
            <div class="action-buttons">
              <button class="action-btn action-edit" @click="openScene('edit')">
                修改持仓
              </button>
              <button class="action-btn action-buy" @click="openScene('buy')">
                同步加仓
              </button>
              <button class="action-btn action-sell" @click="openScene('sell')">
                同步减仓
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <!-- ====== 子弹窗：表单 ====== -->
    <Transition name="modal">
      <div v-if="scene" class="sub-overlay" @click.self="closeScene">
        <div class="sub-card">
          <div class="sub-header">
            <button class="back-btn" @click="closeScene">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="15 18 9 12 15 6"/></svg>
            </button>
            <h3 class="sub-title">{{ sceneTitle }}</h3>
            <button class="sub-close" @click="closeScene">&times;</button>
          </div>

          <div class="sub-body">
            <!-- ====== 编辑 ====== -->
            <div v-if="scene === 'edit'">
              <div class="mode-pills">
                <button :class="['pill', { active: mode === 'SHARES' }]" @click="switchMode('SHARES')">按份额</button>
                <button :class="['pill', { active: mode === 'AMOUNT' }]" @click="switchMode('AMOUNT')">按金额</button>
              </div>

              <Transition name="fade" mode="out-in">
                <div v-if="mode === 'SHARES'" key="shares">
                  <div class="form-item">
                    <label>持有份额</label>
                    <input v-model="form.holdShare" type="number" step="0.01" placeholder="输入持有份额" />
                  </div>
                  <div class="form-item">
                    <label>成本价</label>
                    <input v-model="form.costPrice" type="number" step="0.0001" placeholder="输入成本单价" />
                  </div>
                  <div class="calc-row">
                    <div class="calc-item">
                      <span class="calc-label">持仓金额</span>
                      <span class="calc-value">{{ fmtMoney(computedAmount) }}</span>
                    </div>
                    <div class="calc-item">
                      <span class="calc-label">当前市值</span>
                      <span class="calc-value" :class="profitClass(computedProfit)">{{ fmtMoney(computedValue) }}</span>
                    </div>
                  </div>
                  <div class="calc-item full">
                    <span class="calc-label">持仓盈亏</span>
                    <span class="calc-value" :class="profitClass(computedProfit)">{{ fmtProfit(computedProfit) }}（{{ fmtPercent(computedProfitRate) }}）</span>
                  </div>
                </div>

                <div v-else key="amount">
                  <div class="form-item">
                    <label>持仓金额</label>
                    <input v-model="form.holdAmount" type="number" step="0.01" placeholder="输入当前持仓金额" />
                  </div>
                  <div class="form-row">
                    <div class="form-item flex-1">
                      <label>盈亏金额</label>
                      <input v-model="form.profit" type="number" step="0.01" placeholder="累计盈亏" @focus="profitField='amount'" />
                    </div>
                    <span class="or-sep">或</span>
                    <div class="form-item flex-1">
                      <label>收益率</label>
                      <input v-model="form.profitRate" type="number" step="0.01" placeholder="累计收益率" @focus="profitField='rate'" />
                    </div>
                  </div>
                  <div class="calc-row">
                    <div class="calc-item">
                      <span class="calc-label">→ 份额</span>
                      <span class="calc-value">{{ computedReverseShare }}</span>
                    </div>
                    <div class="calc-item">
                      <span class="calc-label">→ 成本价</span>
                      <span class="calc-value">{{ computedReverseCost }}</span>
                    </div>
                  </div>
                </div>
              </Transition>

              <div class="form-item">
                <label>买入日期</label>
                <ScrollDatePicker v-model="form.buyDate" />
              </div>
            </div>

            <!-- ====== 加仓 ====== -->
            <div v-if="scene === 'buy'">
              <div class="form-item">
                <label>买入金额</label>
                <input v-model="buyAmount" type="number" step="0.01" placeholder="输入加仓金额" />
              </div>
              <div class="form-item">
                <label>日期</label>
                <ScrollDatePicker v-model="adjustDate" />
              </div>
              <div class="time-toggle">
                <span class="time-label">确认时间</span>
                <button :class="['time-btn', { active: before3pm }]" @click="before3pm = true">3点前</button>
                <button :class="['time-btn', { active: !before3pm }]" @click="before3pm = false">3点后</button>
              </div>
              <div class="result-row">
                <span>≈ {{ computedBuyShare }} 份</span>
                <span>净值 {{ effectiveNavDisplay }}</span>
              </div>
            </div>

            <!-- ====== 减仓 ====== -->
            <div v-if="scene === 'sell'">
              <div class="quick-row">
                <button v-for="opt in sellOptions" :key="opt.label" class="quick-btn"
                  @click="applySellRatio(opt.ratio)">{{ opt.label }}</button>
              </div>
              <div class="form-item">
                <label>卖出份额</label>
                <input v-model="adjustShare" type="number" step="0.01" placeholder="或手动输入卖出份额" />
                <span class="hint">持有 {{ holding.holdShare }} 份 · 市值 {{ fmtMoney(holding.currentValue) }}</span>
              </div>
              <div class="form-item">
                <label>日期</label>
                <ScrollDatePicker v-model="adjustDate" />
              </div>
              <div class="time-toggle">
                <span class="time-label">确认时间</span>
                <button :class="['time-btn', { active: before3pm }]" @click="before3pm = true">3点前</button>
                <button :class="['time-btn', { active: !before3pm }]" @click="before3pm = false">3点后</button>
              </div>
            </div>
          </div>

          <div class="sub-footer">
            <button class="cancel-btn" @click="closeScene">取消</button>
            <button class="submit-btn" @click="handleSubmit" :disabled="submitting">
              {{ submitting ? '保存中...' : submitLabel }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { fundApi } from '../api'
import { useFundStore } from '../stores/fundStore'
import { useToast } from '../composables/useToast'
import ScrollDatePicker from './ScrollDatePicker.vue'

const props = defineProps({ holding: { type: Object, required: true } })
const emit = defineEmits(['close', 'update'])

const scene = ref(null)
const mode = ref('SHARES')
const profitField = ref('amount')
const submitting = ref(false)
const adjustShare = ref('')
const adjustDate = ref('')
const buyAmount = ref('')
const before3pm = ref(true)
const dateNav = ref(null)
const toast = useToast()
const fundStore = useFundStore()

const form = reactive({
  holdShare: '', costPrice: '', buyDate: '',
  holdAmount: '', profit: '', profitRate: ''
})

const sellOptions = [
  { label: '1/4', ratio: 0.25 },
  { label: '1/2', ratio: 0.5 },
  { label: '全部', ratio: 1 }
]

const sceneTitle = computed(() => {
  if (scene.value === 'edit') return '修改持仓'
  if (scene.value === 'buy') return '同步加仓'
  if (scene.value === 'sell') return '同步减仓'
  return ''
})

const NAV = computed(() => parseFloat(props.holding?.currentNetValue || props.holding?.estimatedNetValue || props.holding?.unitNetValue || 0))

const fmtNAV = computed(() => NAV.value ? NAV.value.toFixed(4) : '--')
const fmtMoney = (v) => { const n = Number(v) || 0; return '¥' + n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }
const fmtProfit = (v) => { const n = Number(v) || 0; return (n >= 0 ? '+' : '') + n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }
const fmtPercent = (v) => { const n = Number(v) || 0; return (n >= 0 ? '+' : '') + n.toFixed(2) + '%' }
const profitClass = (v) => Number(v) > 0 ? 'profit-up' : Number(v) < 0 ? 'profit-down' : ''

const navDateDisplay = computed(() => props.holding?.latestNetValueDate || '--')
const navChangePercent = computed(() => {
  const h = props.holding
  return h?.estimatedChange != null ? Number(h.estimatedChange) : null
})
const fmtNavChange = computed(() => {
  if (navChangePercent.value == null) return '--'
  const n = navChangePercent.value
  return (n >= 0 ? '+' : '') + n.toFixed(2) + '%'
})
const navChangeClass = computed(() => {
  if (navChangePercent.value == null) return ''
  return navChangePercent.value >= 0 ? 'profit-up' : 'profit-down'
})

const holdingProfit = computed(() => {
  const h = props.holding
  if (!h) return 0
  if (h.totalProfit != null) return Number(h.totalProfit)
  const cost = (parseFloat(h.holdShare) || 0) * (parseFloat(h.costPrice) || 0)
  return (parseFloat(h.currentValue) || 0) - cost
})

const holdDays = computed(() => {
  const date = props.holding?.buyDate
  if (!date) return '--'
  const buy = new Date(date)
  const now = new Date()
  return Math.floor((now - buy) / (1000 * 60 * 60 * 24))
})

const computedAmount = computed(() => (parseFloat(form.holdShare) || 0) * (parseFloat(form.costPrice) || 0))
const computedValue = computed(() => (parseFloat(form.holdShare) || 0) * NAV.value)
const computedProfit = computed(() => computedValue.value - computedAmount.value)
const computedProfitRate = computed(() => computedAmount.value ? (computedProfit.value / computedAmount.value) * 100 : 0)

const computedReverseShare = computed(() => { const a = parseFloat(form.holdAmount) || 0; return NAV.value ? (a / NAV.value).toFixed(2) : '0.00' })
const computedReverseCost = computed(() => {
  const share = parseFloat(computedReverseShare.value) || 1
  const amt = parseFloat(form.holdAmount) || 0
  const p = parseFloat(form.profit) || 0
  return ((amt - p) / share).toFixed(4)
})

const computedBuyShare = computed(() => {
  const amt = parseFloat(buyAmount.value) || 0
  return effectiveNAV.value ? (amt / effectiveNAV.value).toFixed(2) : '0.00'
})
const computedBuyCost = computed(() => {
  const nav = effectiveNAV.value
  return nav ? nav.toFixed(4) : '0.0000'
})

watch([adjustDate, before3pm], async ([date, before]) => {
  if (!date || scene.value === 'edit') { dateNav.value = null; return }
  try {
    let qDate = date
    if (!before) {
      const d = new Date(date)
      d.setDate(d.getDate() + 1)
      qDate = d.toISOString().slice(0, 10)
    }
    const res = await fundApi.getNavAt(props.holding.fundCode, qDate)
    dateNav.value = (res.code === 200 && res.data?.nav) ? Number(res.data.nav) : null
  } catch { dateNav.value = null }
})

const effectiveNAV = computed(() => dateNav.value || NAV.value)
const effectiveNavDisplay = computed(() => effectiveNAV.value ? effectiveNAV.value.toFixed(4) : '--')

const submitLabel = computed(() => scene.value === 'buy' ? '确认加仓' : scene.value === 'sell' ? '确认减仓' : '保存修改')

const resetForm = () => {
  const h = props.holding
  form.holdShare = h.holdShare ? String(h.holdShare) : ''
  form.costPrice = h.costPrice ? String(h.costPrice) : ''
  form.buyDate = h.buyDate || ''
  form.holdAmount = h.currentValue ? String(h.currentValue) : ''
  form.profit = ''
  form.profitRate = ''
  adjustShare.value = ''
  adjustDate.value = ''
  buyAmount.value = ''
  before3pm.value = true
  dateNav.value = null
}

watch(() => props.holding, resetForm, { immediate: true })

const switchMode = (m) => {
  if (m === mode.value) return
  if (m === 'AMOUNT') {
    form.holdAmount = computedValue.value.toFixed(2)
    form.profit = computedProfit.value.toFixed(2)
    form.profitRate = ''
  } else {
    form.holdShare = computedReverseShare.value
    form.costPrice = computedReverseCost.value
  }
  mode.value = m
}

const openScene = (s) => {
  resetForm()
  scene.value = s
}

const closeScene = () => {
  scene.value = null
}

const applySellRatio = (ratio) => {
  const h = parseFloat(props.holding.holdShare) || 0
  adjustShare.value = String(Math.round(h * ratio * 100) / 100)
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    let response
    const adjArgs = adjustDate.value
      ? { adjustDate: adjustDate.value, before3pm: before3pm.value }
      : {}

    if (scene.value === 'buy') {
      response = await fundApi.adjustHolding(props.holding.fundCode, 'BUY',
        computedBuyShare.value, computedBuyCost.value, adjArgs)
    } else if (scene.value === 'sell') {
      response = await fundApi.adjustHolding(props.holding.fundCode, 'SELL',
        adjustShare.value, null, adjArgs)
    } else if (mode.value === 'SHARES') {
      response = await fundApi.updateHolding(props.holding.fundCode, {
        mode: 'SHARES', holdShare: form.holdShare || '0', costPrice: form.costPrice || '0',
        buyDate: form.buyDate || null
      })
    } else {
      response = await fundApi.updateHolding(props.holding.fundCode, {
        mode: 'AMOUNT', holdAmount: form.holdAmount || '0',
        profit: profitField.value === 'amount' ? form.profit : null,
        profitRate: profitField.value === 'rate' ? form.profitRate : null,
        buyDate: form.buyDate || null
      })
    }

    if (response.code === 200) {
      // 所有操作（编辑/加仓/减仓）后端都返回完整的 FundHoldingVO
      if (response.data) {
        fundStore.updateHoldingInPlace(props.holding.fundCode, response.data)
      } else {
        fundStore.silentFetchHoldings()
        fundStore.recalcSummary()
      }
      toast.success(submitLabel.value + '成功')
      emit('update')
      closeScene()
    } else {
      toast.error(response.message || '操作失败')
    }
  } catch (err) {
    toast.error('操作失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const handleClose = () => emit('close')
</script>

<style scoped>
/* ====== 主弹窗 Overlay ====== */
.modal-overlay {
  position: fixed; inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex; align-items: center; justify-content: center;
  z-index: 1000;
  transform: translateZ(0);
  padding: 16px;
}

.modal-card {
  background: #fff;
  border-radius: 20px;
  width: 460px;
  max-width: 94vw;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.18);
  position: relative;
  padding-bottom: 28px;
}

.close-btn {
  position: absolute;
  top: 16px; right: 16px;
  background: #f1f5f9;
  border: none;
  width: 32px; height: 32px;
  border-radius: 50%;
  font-size: 20px;
  color: #94a3b8;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
  z-index: 1;
}
.close-btn:hover { background: #e2e8f0; color: #334155; }

/* ====== 1. 顶部信息头 ====== */
.info-header {
  padding: 32px 28px 20px;
  text-align: center;
  background: linear-gradient(180deg, #f8fafc 0%, #fff 100%);
  border-bottom: 1px solid #f1f5f9;
}

.fund-name {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.3;
  letter-spacing: -0.3px;
}

.fund-code-text {
  margin: 0 0 16px;
  font-size: 13px;
  color: #94a3b8;
  font-family: 'SF Mono', 'Fira Code', monospace;
  letter-spacing: 0.5px;
}

.nav-line {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
  flex-wrap: wrap;
}

.nav-label { font-size: 12px; color: #94a3b8; }
.nav-date { font-size: 12px; color: #94a3b8; }
.nav-sep { color: #cbd5e1; margin: 0 2px; }
.nav-value { font-size: 22px; font-weight: 700; color: #0f172a; letter-spacing: -0.5px; }
.nav-change { font-size: 14px; font-weight: 600; margin-left: 2px; }

/* ====== 2. 持仓概览 ====== */
.holding-section { padding: 20px 28px 12px; }

.section-label {
  margin: 0 0 12px;
  font-size: 12px;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.holding-cards { display: flex; flex-direction: column; gap: 10px; }

.h-card {
  display: flex; align-items: center; gap: 8px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 12px;
  padding: 14px 18px;
}

.h-card-label {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  flex-shrink: 0;
}

.h-card-label::after {
  content: '：';
}

.h-card-value {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.3px;
}

/* ====== 3. 操作按钮 ====== */
.action-section { padding: 20px 28px 0; }

.action-buttons { display: flex; gap: 8px; }

.action-btn {
  flex: 1;
  display: flex; align-items: center; justify-content: center; gap: 4px;
  padding: 12px 0;
  border: 1.5px solid #e2e8f0;
  background: #fff;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  color: #64748b;
}

.action-btn:hover { transform: translateY(-1px); box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.action-edit { color: #1677ff; }
.action-edit:hover { border-color: #1677ff; background: #eff6ff; }
.action-buy { color: #e53935; }
.action-buy:hover { border-color: #ef4444; background: #fef2f2; }
.action-sell { color: #16a34a; }
.action-sell:hover { border-color: #22c55e; background: #f0fdf4; }

/* ====== 子弹窗 Overlay ====== */
.sub-overlay {
  position: fixed; inset: 0;
  background: rgba(0, 0, 0, 0.55);
  display: flex; align-items: center; justify-content: center;
  z-index: 2000;
  padding: 16px;
}

.sub-card {
  background: #fff;
  border-radius: 20px;
  width: 420px;
  max-width: 94vw;
  max-height: 85vh;
  overflow-y: auto;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.25);
}

.sub-header {
  display: flex; align-items: center; gap: 8px;
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
  position: sticky; top: 0; background: #fff; z-index: 1;
  border-radius: 20px 20px 0 0;
}

.back-btn, .sub-close {
  background: none; border: none;
  width: 34px; height: 34px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; color: #64748b; transition: all 0.2s;
  flex-shrink: 0;
}
.back-btn:hover, .sub-close:hover { background: #f1f5f9; color: #334155; }
.sub-close { font-size: 22px; margin-left: auto; }

.sub-title {
  flex: 1; margin: 0;
  font-size: 16px; font-weight: 600; color: #1e293b;
  text-align: center;
}

.sub-body { padding: 16px 20px; }

.sub-footer {
  display: flex; justify-content: flex-end; gap: 10px;
  padding: 16px 20px 20px;
  border-top: 1px solid #f1f5f9;
}

.cancel-btn {
  padding: 10px 24px; border-radius: 10px;
  background: #f1f5f9; color: #475569;
  border: none; font-size: 14px; font-weight: 500;
  cursor: pointer; transition: all 0.2s;
}
.cancel-btn:hover { background: #e2e8f0; }

.submit-btn {
  padding: 10px 28px; border-radius: 10px;
  background: linear-gradient(135deg, #1677ff, #3b82f6);
  color: #fff; border: none;
  font-size: 14px; font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 10px rgba(22,119,255,0.25);
  transition: all 0.2s;
}
.submit-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 16px rgba(22,119,255,0.35); }
.submit-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* ---- 表单复用 ---- */
.mode-pills { display: flex; background: #f1f5f9; border-radius: 10px; padding: 3px; margin-bottom: 16px; }
.pill { flex: 1; padding: 8px 0; border: none; background: transparent; border-radius: 8px; font-size: 13px; font-weight: 500; color: #64748b; cursor: pointer; transition: all 0.2s; }
.pill.active { background: #fff; color: #1e293b; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }

.quick-row { display: flex; gap: 8px; margin-bottom: 14px; }
.quick-btn { flex: 1; padding: 8px 0; border: 1px solid #e2e8f0; background: #fff; border-radius: 8px; font-size: 13px; color: #475569; cursor: pointer; transition: all 0.15s; }
.quick-btn:hover { border-color: #1677ff; color: #1677ff; background: #f8f9ff; }

.form-item { margin-bottom: 14px; }
.form-item label { display: block; margin-bottom: 6px; font-size: 13px; color: #64748b; font-weight: 500; }
.form-item input { width: 100%; padding: 10px 14px; border: 1.5px solid #e2e8f0; border-radius: 10px; font-size: 14px; box-sizing: border-box; background: #fafbfc; transition: all 0.2s; color: #0f172a; }
.form-item input:focus { outline: none; border-color: #1677ff; background: #fff; box-shadow: 0 0 0 3px rgba(22,119,255,0.08); }
.hint { display: block; margin-top: 5px; font-size: 12px; color: #94a3b8; }

.form-row { display: flex; gap: 10px; align-items: center; }
.form-row .form-item { flex: 1; }
.flex-1 { flex: 1; }
.or-sep { color: #94a3b8; font-size: 12px; padding-top: 22px; flex-shrink: 0; }

.calc-row { display: flex; gap: 12px; margin-bottom: 14px; }
.calc-item { flex: 1; background: #f8fafc; border-radius: 10px; padding: 10px 14px; }
.calc-item.full { flex: none; width: 100%; }
.calc-label { display: block; font-size: 11px; color: #94a3b8; margin-bottom: 4px; }
.calc-value { font-size: 15px; font-weight: 600; color: #334155; }

.result-row { display: flex; gap: 12px; font-size: 13px; color: #475569; margin-bottom: 14px; }
.result-row span { flex: 1; background: #f8fafc; border-radius: 8px; padding: 10px 12px; text-align: center; font-weight: 500; }

.time-toggle { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; }
.time-label { font-size: 13px; color: #64748b; font-weight: 500; margin-right: 4px; }
.time-btn { padding: 6px 16px; border: 1px solid #e2e8f0; background: #fff; border-radius: 8px; font-size: 13px; color: #64748b; cursor: pointer; transition: all 0.15s; }
.time-btn.active { background: #1677ff; color: #fff; border-color: #1677ff; }
.time-btn:hover:not(.active) { border-color: #1677ff; color: #1677ff; }

.profit-up { color: #e53935 !important; }
.profit-down { color: #16a34a !important; }

/* ---- 动画 ---- */
.fade-enter-active, .fade-leave-active { transition: opacity 0.12s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
