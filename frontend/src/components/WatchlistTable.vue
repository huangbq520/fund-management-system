<template>
  <div class="watchlist-table" :class="{ 'is-batch-mode': isBatchDeleteMode, 'has-checkbox': showCheckboxes }">
    <div class="list-header">
      <h2>我的自选</h2>
      <div class="header-buttons">
        <button @click="refreshList" class="refresh-btn">
          <span>刷新</span>
        </button>
        <!-- 删除模式 -->
        <div v-if="isBatchDeleteMode" class="batch-actions">
          <button
            @click="handleBatchDelete"
            class="delete-btn"
            :disabled="selectedFundCodes.size === 0"
          >
            删除 {{ selectedFundCodes.size > 0 ? '(' + selectedFundCodes.size + ')' : '' }}
          </button>
          <button @click="exitSelectionMode" class="cancel-btn">取消</button>
        </div>
        <!-- 对比模式 -->
        <div v-else-if="isCompareMode" class="batch-actions">
          <button
            @click="openCompareModal"
            class="compare-btn"
            :disabled="selectedFundCodes.size < 2"
          >
            对比 ({{ selectedFundCodes.size }})
          </button>
          <button @click="exitSelectionMode" class="cancel-btn">取消</button>
        </div>
        <!-- 默认模式 -->
        <template v-else>
          <button @click="startBatchDelete" class="batch-btn">
            <span>删除</span>
          </button>
          <button @click="startCompareMode" class="compare-btn">
            <span>对比</span>
          </button>
        </template>
      </div>
    </div>

    <WatchlistGroupBar @filter-change="handleGroupFilter" />

    <!-- 加载中 -->
    <div v-if="loading.items" class="loading">
      <svg viewBox="0 0 240 240" height="240" width="240" class="pl">
        <circle stroke-linecap="round" stroke-dashoffset="-330" stroke-dasharray="0 660" stroke-width="20" stroke="#000" fill="none" r="105" cy="120" cx="120" class="pl__ring pl__ring--a"></circle>
        <circle stroke-linecap="round" stroke-dashoffset="-110" stroke-dasharray="0 220" stroke-width="20" stroke="#000" fill="none" r="35" cy="120" cx="120" class="pl__ring pl__ring--b"></circle>
        <circle stroke-linecap="round" stroke-dasharray="0 440" stroke-width="20" stroke="#000" fill="none" r="70" cy="120" cx="85" class="pl__ring pl__ring--c"></circle>
        <circle stroke-linecap="round" stroke-dasharray="0 440" stroke-width="20" stroke="#000" fill="none" r="70" cy="120" cx="155" class="pl__ring pl__ring--d"></circle>
      </svg>
    </div>

    <!-- 空态 -->
    <div v-else-if="sortedItems.length === 0" class="empty">
      <div class="empty-icon">📋</div>
      <div class="empty-text">还没有自选基金</div>
      <div class="empty-hint">使用顶部搜索栏搜索基金，点击"加入自选"即可</div>
    </div>

    <!-- 自选列表表格 -->
    <div v-else class="table-wrapper">
      <table class="holding-table">
        <thead>
          <tr>
            <th v-if="showCheckboxes" class="col-checkbox">
              <input
                type="checkbox"
                :checked="isAllSelected"
                @change="toggleSelectAll"
                class="checkbox"
              />
            </th>
            <th class="col-name">基金名称</th>
            <th class="col-sector">关联板块</th>
            <th class="col-nav sortable" @click="store.setSort('unitNetValue')">
              最新净值
              <span class="sort-arrow">{{ getSortArrow('unitNetValue') }}</span>
            </th>
            <th class="col-latest-chg sortable" @click="store.setSort('latestChange')">
              最新涨幅
              <span class="sort-arrow">{{ getSortArrow('latestChange') }}</span>
            </th>
            <th class="col-est-chg sortable" @click="store.setSort('estimatedChange')">
              估算涨幅
              <span class="sort-arrow">{{ getSortArrow('estimatedChange') }}</span>
            </th>
            <th class="col-since sortable" @click="store.setSort('returnSinceAdded')">
              自选以来
              <span class="sort-arrow">{{ getSortArrow('returnSinceAdded') }}</span>
            </th>
            <th class="col-w1 sortable" @click="store.setSort('oneWeekChange')">
              近1周
              <span class="sort-arrow">{{ getSortArrow('oneWeekChange') }}</span>
            </th>
            <th class="col-m1 sortable" @click="store.setSort('oneMonthChange')">
              近1月
              <span class="sort-arrow">{{ getSortArrow('oneMonthChange') }}</span>
            </th>
            <th class="col-m3 sortable" @click="store.setSort('threeMonthChange')">
              近3月
              <span class="sort-arrow">{{ getSortArrow('threeMonthChange') }}</span>
            </th>
            <th class="col-m6 sortable" @click="store.setSort('sixMonthChange')">
              近6月
              <span class="sort-arrow">{{ getSortArrow('sixMonthChange') }}</span>
            </th>
            <th class="col-y1 sortable" @click="store.setSort('oneYearChange')">
              近1年
              <span class="sort-arrow">{{ getSortArrow('oneYearChange') }}</span>
            </th>
            <th class="col-action">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="item in sortedItems"
            :key="item.fundCode"
            class="table-row"
          >
            <td v-if="showCheckboxes" class="col-checkbox" @click.stop>
              <input
                type="checkbox"
                :checked="isSelected(item.fundCode)"
                @change="toggleSelect(item.fundCode)"
                class="checkbox"
              />
            </td>
            <td class="col-name" @click="viewDetail(item.fundCode)">
              <div class="fund-name">{{ item.fundName || item.fundCode }}</div>
              <div class="fund-code">{{ item.fundCode }}</div>
            </td>
            <td class="col-sector">
              <span class="sector-placeholder">--</span>
            </td>
            <td class="col-nav" @click="viewDetail(item.fundCode)">
              <div class="nav-container">
                <span>{{ item.unitNetValue || '--' }}</span>
                <span class="nav-date" v-if="item.latestNetValueDate">{{ item.latestNetValueDate }}</span>
              </div>
            </td>
            <td class="col-latest-chg" @click="viewDetail(item.fundCode)">
              <span :class="getProfitClass(item.latestChange)">
                {{ formatPercent(item.latestChange) }}
              </span>
            </td>
            <td class="col-est-chg" @click="viewDetail(item.fundCode)">
              <div class="est-container">
                <span :class="getProfitClass(item.estimatedChange)">
                  {{ formatPercent(item.estimatedChange) }}
                </span>
                <span class="est-time" v-if="item.valuationTime">{{ formatEstimateTime(item.valuationTime) }}</span>
              </div>
            </td>
            <td class="col-since" @click="viewDetail(item.fundCode)">
              <span :class="getProfitClass(item.returnSinceAdded)">
                {{ formatPercent(item.returnSinceAdded) }}
              </span>
            </td>
            <td class="col-w1" @click="viewDetail(item.fundCode)">
              <span :class="getProfitClass(item.oneWeekChange)">
                {{ formatPercent(item.oneWeekChange) }}
              </span>
            </td>
            <td class="col-m1" @click="viewDetail(item.fundCode)">
              <span :class="getProfitClass(item.oneMonthChange)">
                {{ formatPercent(item.oneMonthChange) }}
              </span>
            </td>
            <td class="col-m3" @click="viewDetail(item.fundCode)">
              <span :class="getProfitClass(item.threeMonthChange)">
                {{ formatPercent(item.threeMonthChange) }}
              </span>
            </td>
            <td class="col-m6" @click="viewDetail(item.fundCode)">
              <span :class="getProfitClass(item.sixMonthChange)">
                {{ formatPercent(item.sixMonthChange) }}
              </span>
            </td>
            <td class="col-y1" @click="viewDetail(item.fundCode)">
              <span :class="getProfitClass(item.oneYearChange)">
                {{ formatPercent(item.oneYearChange) }}
              </span>
            </td>
            <td class="col-action">
              <button @click.stop="handleRemove(item)" class="edit-btn" title="移除自选">移除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 对比弹窗 -->
    <WatchlistCompareModal
      :visible="showCompare"
      :fundCodes="Array.from(selectedFundCodes)"
      @close="showCompare = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useWatchlistStore } from '../stores/watchlistStore'
import { useAutoRefresh, isTradingHours } from '../composables/useAutoRefresh'
import { formatPercent, getProfitClass } from '../composables/useFormat'
import { useToast } from '../composables/useToast'
import WatchlistGroupBar from './WatchlistGroupBar.vue'
import WatchlistCompareModal from './WatchlistCompareModal.vue'

const store = useWatchlistStore()
const { items, loading, sortField, sortDirection } = storeToRefs(store)
const toast = useToast()

const emit = defineEmits(['view-detail'])

// 当前分组筛选（null = 全部）
const activeGroupFilter = ref(null)

// 批量删除 / 对比选择 模式
const isBatchDeleteMode = ref(false)
const isCompareMode = ref(false)
const selectedFundCodes = ref(new Set())

// 是否显示复选框（选择模式下才显示）
const showCheckboxes = computed(() => isBatchDeleteMode.value || isCompareMode.value)

// 对比弹窗
const showCompare = ref(false)

// 自动刷新
useAutoRefresh(() => store.silentFetchItems(), 30000, isTradingHours)

onMounted(async () => {
  if (items.value.length === 0) {
    await store.fetchItems()
    await store.fetchGroups()
  }
})

// 排序后的列表
const sortedItems = computed(() => {
  let list = [...items.value]

  // 分组筛选
  if (activeGroupFilter.value !== null) {
    list = list.filter(item => item.groupId === activeGroupFilter.value)
  }

  // 排序
  if (sortField.value) {
    list.sort((a, b) => {
      const aVal = a[sortField.value]
      const bVal = b[sortField.value]
      if (aVal === null || aVal === undefined) return 1
      if (bVal === null || bVal === undefined) return -1
      if (typeof aVal === 'number' && typeof bVal === 'number') {
        return sortDirection.value === 'asc' ? aVal - bVal : bVal - aVal
      }
      const aStr = String(aVal)
      const bStr = String(bVal)
      try {
        const aNum = parseFloat(aStr)
        const bNum = parseFloat(bStr)
        if (!isNaN(aNum) && !isNaN(bNum)) {
          return sortDirection.value === 'asc' ? aNum - bNum : bNum - aNum
        }
      } catch (e) { /* fallback to string compare */ }
      return sortDirection.value === 'asc'
        ? aStr.localeCompare(bStr)
        : bStr.localeCompare(aStr)
    })
  }

  return list
})

function getSortArrow(field) {
  if (sortField.value !== field) return ''
  return sortDirection.value === 'asc' ? '▲' : '▼'
}

// 选择
const isAllSelected = computed(() => {
  if (sortedItems.value.length === 0) return false
  return sortedItems.value.every(item => selectedFundCodes.value.has(item.fundCode))
})

function isSelected(fundCode) {
  return selectedFundCodes.value.has(fundCode)
}

function toggleSelect(fundCode) {
  const newSet = new Set(selectedFundCodes.value)
  if (newSet.has(fundCode)) {
    newSet.delete(fundCode)
  } else {
    if (newSet.size >= 5 && !isBatchDeleteMode.value) {
      toast.error('最多选择5只基金进行对比')
      return
    }
    newSet.add(fundCode)
  }
  selectedFundCodes.value = newSet
}

function toggleSelectAll(e) {
  if (e.target.checked) {
    const codes = sortedItems.value.map(item => item.fundCode)
    if (!isBatchDeleteMode.value && codes.length > 5) {
      selectedFundCodes.value = new Set(codes.slice(0, 5))
      toast.error('最多选择5只基金进行对比，仅选中前5只')
      return
    }
    selectedFundCodes.value = new Set(codes)
  } else {
    selectedFundCodes.value = new Set()
  }
}

function handleGroupFilter(groupId) {
  activeGroupFilter.value = groupId
}

// 进入批量删除模式
function startBatchDelete() {
  isBatchDeleteMode.value = true
  isCompareMode.value = false
  selectedFundCodes.value = new Set()
}

// 进入对比选择模式
function startCompareMode() {
  isCompareMode.value = true
  isBatchDeleteMode.value = false
  selectedFundCodes.value = new Set()
}

// 退出选择模式（删除/对比共用）
function exitSelectionMode() {
  isBatchDeleteMode.value = false
  isCompareMode.value = false
  selectedFundCodes.value = new Set()
}

// 在对比模式下，点击"对比(N)"打开对比弹窗
function openCompareModal() {
  if (selectedFundCodes.value.size < 2) return
  showCompare.value = true
}

async function handleBatchDelete() {
  if (selectedFundCodes.value.size === 0) return
  if (!confirm(`确定要删除选中的 ${selectedFundCodes.value.size} 只自选基金吗？`)) return
  const codes = Array.from(selectedFundCodes.value)
  const response = await store.batchRemove(codes)
  if (response.code === 200) {
    toast.success(`已删除 ${codes.length} 只自选基金`)
    selectedFundCodes.value = new Set()
    isBatchDeleteMode.value = false
  } else {
    toast.error(response.message || '批量删除失败')
  }
}

// 移除单个
async function handleRemove(item) {
  if (!confirm(`确定要将 ${item.fundName || item.fundCode} 从自选中移除吗？`)) return
  const response = await store.removeFromWatchlist(item.fundCode)
  if (response.code === 200) {
    toast.success('已移除自选')
  } else {
    toast.error(response.message || '移除失败')
  }
}

function viewDetail(fundCode) {
  emit('view-detail', fundCode)
}

function refreshList() {
  store.fetchItems()
}

function formatEstimateTime(timeStr) {
  if (!timeStr) return ''
  // gztime 格式如 "2026-06-15 14:30"
  const parts = timeStr.split(' ')
  if (parts.length >= 2) return parts[1]
  return timeStr.length > 5 ? timeStr.substring(timeStr.length - 5) : timeStr
}
</script>

<style scoped>
.watchlist-table {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}

.holding-list {
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

.header-buttons {
  display: flex;
  gap: 12px;
  align-items: center;
}

.batch-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* 统一按钮基础样式 —— 参考持仓列表 */
.batch-btn,
.delete-btn,
.cancel-btn,
.compare-btn,
.refresh-btn {
  position: relative;
  z-index: 0;
  padding: 5px 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  border: 1px solid currentColor;
  border-radius: 25px;
  outline: none;
  overflow: hidden;
  background: transparent;
  transition: color 0.3s 0.1s ease-out, border-color 0.3s ease-out;
  text-align: center;
}

.batch-btn span,
.delete-btn span,
.cancel-btn span,
.compare-btn span,
.refresh-btn span {
  position: relative;
  z-index: 1;
  margin: 10px;
  transition: color 0.3s 0.1s ease-out;
}

/* 统一的 ::before 填充动画 */
.batch-btn::before,
.delete-btn::before,
.cancel-btn::before,
.compare-btn::before,
.refresh-btn::before {
  position: absolute;
  top: 0;
  left: -5em;
  right: 0;
  bottom: 0;
  margin: auto;
  content: '';
  border-radius: 50%;
  display: block;
  width: 20em;
  height: 20em;
  text-align: center;
  transition: box-shadow 0.5s ease-out;
  z-index: 0;
}

/* === 颜色方案 === */

/* 删除按钮：红色 #f56c6c */
.batch-btn,
.delete-btn {
  border-color: #f56c6c;
  color: #f56c6c;
}

.delete-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.batch-btn:hover,
.delete-btn:not(:disabled):hover {
  color: #fff;
  border-color: #f56c6c;
}

.batch-btn:hover::before,
.delete-btn:not(:disabled):hover::before {
  box-shadow: inset 0 0 0 10em #f56c6c;
}

/* 取消按钮：灰色 #909399 */
.cancel-btn {
  border-color: #909399;
  color: #909399;
}

.cancel-btn:hover {
  color: #fff;
  border-color: #909399;
}

.cancel-btn:hover::before {
  box-shadow: inset 0 0 0 10em #909399;
}

/* 刷新按钮：蓝色 #2890F1 */
.refresh-btn {
  border-color: #2890F1;
  color: #2890F1;
}

.refresh-btn:hover {
  color: #fff;
  border-color: #2890F1;
}

.refresh-btn:hover::before {
  box-shadow: inset 0 0 0 10em #2890F1;
}

/* 对比按钮：黄色 #E6A23C */
.compare-btn {
  border-color: #E6A23C;
  color: #E6A23C;
}

.compare-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.compare-btn:not(:disabled):hover {
  color: #fff;
  border-color: #E6A23C;
}

.compare-btn:not(:disabled):hover::before {
  box-shadow: inset 0 0 0 10em #E6A23C;
}

.col-checkbox {
  width: 50px;
  text-align: center;
}

.checkbox {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.loading, .empty {
  text-align: center;
  padding: 50px;
  color: #999;
  font-size: 15px;
}

.table-wrapper {
  overflow-x: auto;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
}

.holding-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: auto;
  min-width: 2000px;
}

.holding-table th,
.holding-table td {
  padding: 14px 10px;
  text-align: center;
  border-bottom: 1px solid #f0f0f0;
}

.holding-table th {
  background: #f8fafc;
  font-weight: 700;
  font-size: 13px;
  color: #000000;
  letter-spacing: 0.3px;
}

.holding-table th.col-action {
  position: sticky;
  right: 0;
  z-index: 21;
  background-color: #f8fafc;
}

.holding-table td.col-action {
  position: sticky;
  right: 0;
  z-index: 11;
  background-color: white;
}

/* 当有复选框列时，基金名称列在复选框列的右侧（sticky） */
.has-checkbox .holding-table th.col-name {
  position: sticky;
  left: 50px;
  z-index: 20;
  background-color: #f8fafc;
}

.has-checkbox .holding-table th.col-checkbox {
  position: sticky;
  left: 0;
  z-index: 25;
  background-color: #f8fafc;
}

.col-checkbox {
  width: 50px;
  text-align: center;
}

/* 行内的 col-name 与 col-checkbox 的 sticky（仅在选择模式下） */
.has-checkbox .col-checkbox {
  width: 50px;
  text-align: center;
  position: sticky;
  left: 0;
  z-index: 15;
  background-color: white;
}

.has-checkbox .col-name {
  left: 50px;
}

.table-row {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.table-row:hover {
  background: #f0f5ff;
  box-shadow: 0 4px 16px rgba(22, 119, 255, 0.12);
}

/* 确保固定列的背景色与行背景同步变化 */
.table-row .col-name,
.table-row .col-action,
.table-row .col-checkbox {
  transition: background-color 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.table-row:hover .col-name,
.table-row:hover .col-action,
.table-row:hover .col-checkbox {
  background-color: #f0f5ff;
}

.table-row:last-child td {
  border-bottom: none;
}

.table-row:last-child {
  border-bottom-left-radius: 12px;
  border-bottom-right-radius: 12px;
}

.col-name {
  text-align: left !important;
  width: 160px;
  background-color: white;
}

/* 只有在选择模式下（有复选框列），才让 col-name 成为 sticky 第二列 */
.has-checkbox .col-name {
  position: sticky;
  left: 50px;
  z-index: 10;
}

.col-name .fund-name {
  font-weight: 700;
  color: #000000;
  font-size: 14px;
}

.col-code {
  width: 90px;
  color: #999;
  font-size: 13px;
}

.col-nav {
  width: 100px;
  font-size: 14px;
}

.col-latest-change {
  width: 100px;
  font-size: 14px;
}

.col-est-chg {
  width: 100px;
  font-size: 14px;
}

.col-since {
  width: 100px;
  font-size: 14px;
}

.nav-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.nav-date {
  font-size: 11px;
  color: #999;
  line-height: 1.2;
}

.est-container {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.est-time {
  display: block;
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}

.col-week,
.col-month,
.col-three-month,
.col-six-month,
.col-year {
  width: 85px;
  font-size: 14px;
}

.col-action {
  width: 90px;
  position: sticky;
  right: 0;
  z-index: 10;
  background-color: white;
}

.edit-btn {
  position: relative;
  transition: all 0.3s ease-in-out;
  box-shadow: 0px 10px 20px rgba(0, 0, 0, 0.2);
  padding-block: 0.35rem;
  padding-inline: 0.8rem;
  background-color: rgb(0 107 179);
  border-radius: 9999px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #ffff;
  gap: 6px;
  font-weight: bold;
  border: 2px solid #ffffff4d;
  outline: none;
  overflow: hidden;
  font-size: 13px;
  white-space: nowrap;
  min-width: auto;
  width: auto;
}

.edit-btn:hover {
  transform: scale(1.05);
  border-color: #fff9;
}

.edit-btn:hover::before {
  animation: shine 1.5s ease-out infinite;
}

.edit-btn::before {
  content: "";
  position: absolute;
  width: 100px;
  height: 100%;
  background-image: linear-gradient(
    120deg,
    rgba(255, 255, 255, 0) 30%,
    rgba(255, 255, 255, 0.8),
    rgba(255, 255, 255, 0) 70%
  );
  top: 0;
  left: -100px;
  opacity: 0.6;
}

@keyframes shine {
  0% { left: -100px; }
  60% { left: 100%; }
  to { left: 100%; }
}

.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 50px;
}

.pl {
  width: 6em;
  height: 6em;
}

.pl__ring {
  animation: ringA 2s linear infinite;
}

.pl__ring--a { stroke: #f42f25; }
.pl__ring--b { animation-name: ringB; stroke: #ffdd00; }
.pl__ring--c { animation-name: ringC; stroke: #255ff4; }
.pl__ring--d { animation-name: ringD; stroke: #2cf425; }

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
  from, 8% { stroke-dasharray: 0 440; stroke-width: 20; stroke-dashoffset: 0; }
  16% { stroke-dasharray: 40 400; stroke-width: 30; stroke-dashoffset: -5; }
  36% { stroke-dasharray: 40 400; stroke-width: 30; stroke-dashoffset: -175; }
  44%, 50% { stroke-dasharray: 0 440; stroke-width: 20; stroke-dashoffset: -220; }
  58% { stroke-dasharray: 40 400; stroke-width: 30; stroke-dashoffset: -225; }
  78% { stroke-dasharray: 40 400; stroke-width: 30; stroke-dashoffset: -395; }
  86%, to { stroke-dasharray: 0 440; stroke-width: 20; stroke-dashoffset: -440; }
}
</style>
