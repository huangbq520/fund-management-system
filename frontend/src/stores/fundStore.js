import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import { fundApi } from '../api'

export const useFundStore = defineStore('fund', () => {
  const holdings = ref([])
  const hideAmount = ref(false)
  const summary = ref({
    totalAsset: 0,
    todayProfit: 0,
    todayProfitRate: 0,
    totalProfit: 0,
    totalProfitRate: 0,
    fundCount: 0
  })
  const searchResults = ref([])
  const loading = reactive({
    holdings: false,
    summary: false,
    search: false
  })

  async function fetchHoldings() {
    loading.holdings = true
    try {
      const response = await fundApi.getHoldingList()
      if (response.code === 200) {
        holdings.value = response.data || []
        recalcSummary()  // 从最新持仓数据本地计算汇总，不再单独调 /portfolio/summary
      }
    } catch (err) {
      console.error('Failed to load holdings:', err)
    } finally {
      loading.holdings = false
    }
  }

  // fetchSummary 保留供需要服务端精确汇总的场景（如初始加载），但日常操作依赖 recalcSummary
  async function fetchSummary() {
    loading.summary = true
    try {
      const response = await fundApi.getPortfolioSummary()
      if (response.code === 200 && response.data) {
        summary.value = response.data
      }
    } catch (err) {
      console.error('Failed to load portfolio summary:', err)
    } finally {
      loading.summary = false
    }
  }

  async function searchFunds(keyword) {
    loading.search = true
    try {
      const response = await fundApi.searchFunds(keyword)
      if (response.code === 200 && response.data) {
        searchResults.value = response.data
      } else {
        searchResults.value = []
      }
      return response
    } catch (err) {
      console.error('Search failed:', err)
      return { code: 500, message: '搜索失败' }
    } finally {
      loading.search = false
    }
  }

  async function searchFund(keyword) {
    try {
      return await fundApi.searchFunds(keyword)
    } catch (err) {
      console.error('Search failed:', err)
      return { code: 500, message: '搜索失败' }
    }
  }

  /**
   * 原地合并持仓数据：对比新旧数据，只更新变化的属性
   * 避免全量替换数组导致 Vue 重新渲染所有行
   */
  function mergeHoldingsInPlace(newData) {
    const oldMap = new Map(holdings.value.map(h => [h.fundCode, h]))
    const newCodes = new Set(newData.map(h => h.fundCode))

    // 1. 更新已存在的项（原地修改属性，只触发变化行的重渲染）
    for (const item of newData) {
      const old = oldMap.get(item.fundCode)
      if (old) {
        for (const key of Object.keys(item)) {
          if (old[key] !== item[key]) {
            old[key] = item[key]
          }
        }
      }
    }

    // 2. 移除已不存在的项
    for (let i = holdings.value.length - 1; i >= 0; i--) {
      if (!newCodes.has(holdings.value[i].fundCode)) {
        holdings.value.splice(i, 1)
      }
    }

    // 3. 添加新项
    for (const item of newData) {
      if (!oldMap.has(item.fundCode)) {
        holdings.value.push(item)
      }
    }
  }

  async function silentFetchHoldings() {
    try {
      const response = await fundApi.getHoldingList()
      if (response.code === 200) {
        mergeHoldingsInPlace(response.data || [])
        recalcSummary()
      }
    } catch (err) {
      console.error('Failed to load holdings:', err)
    }
  }

  async function silentFetchSummary() {
    try {
      const response = await fundApi.getPortfolioSummary()
      if (response.code === 200 && response.data) {
        // 只更新变化的字段，避免不必要的渲染
        const newData = response.data
        for (const key of Object.keys(newData)) {
          if (summary.value[key] !== newData[key]) {
            summary.value[key] = newData[key]
          }
        }
      }
    } catch (err) {
      console.error('Failed to load portfolio summary:', err)
    }
  }

  function updateHoldingInPlace(fundCode, updated) {
    const idx = holdings.value.findIndex(h => h.fundCode === fundCode)
    if (idx !== -1) {
      holdings.value[idx] = { ...holdings.value[idx], ...updated }
    }
    holdings.value = [...holdings.value]
    recalcSummary()
  }

  function recalcSummary() {
    const items = holdings.value.filter(h => {
      const val = h.currentValue || h.holdAmount
      return val && Number(val) > 0
    })
    if (items.length === 0) {
      summary.value = { totalAsset: 0, todayProfit: 0, todayProfitRate: 0, totalProfit: 0, totalProfitRate: 0, fundCount: holdings.value.length }
      return
    }
    let totalAsset = 0, todayProfit = 0, totalCost = 0, totalValue = 0
    items.forEach(h => {
      const cv = Number(h.currentValue || h.holdAmount || 0)
      const tp = Number(h.todayProfit || 0)
      const share = Number(h.shareForTodayProfit || h.holdShare || 0)
      const cost = Number(h.costPrice || 0)
      totalAsset += cv
      todayProfit += tp
      if (share > 0 && cost > 0) {
        totalCost += share * cost
        totalValue += cv
      }
    })
    const todayRate = totalAsset > 0 ? (todayProfit / (totalAsset - todayProfit)) * 100 : 0
    const totalProfit = totalValue - totalCost
    const totalRate = totalCost > 0 ? (totalProfit / totalCost) * 100 : 0
    summary.value = {
      totalAsset: Math.round(totalAsset * 100) / 100,
      todayProfit: Math.round(todayProfit * 100) / 100,
      todayProfitRate: Math.round(todayRate * 100) / 100,
      totalProfit: Math.round(totalProfit * 100) / 100,
      totalProfitRate: Math.round(totalRate * 100) / 100,
      fundCount: holdings.value.length
    }
  }

  async function addFund(fundCode, fundName) {
    const response = await fundApi.add(fundCode, fundName)
    if (response.code === 200 && response.data) {
      // 后端直接返回完整 FundHoldingVO，本地追加，不再请求全量列表
      holdings.value.push(response.data)
      recalcSummary()
    }
    return response
  }

  async function addFundBatch(funds) {
    const response = await fundApi.addBatch(funds)
    if (response.code === 200) {
      await fetchHoldings()  // 批量操作结果复杂，仍全量刷新（但 Redis 缓存使其很快）
      // fetchHoldings 内部已调用 recalcSummary，无需再调 fetchSummary
    }
    return response
  }

  async function deleteBatch(fundCodes) {
    const response = await fundApi.deleteBatch(fundCodes)
    if (response.code === 200) {
      // 本地移除已删除的基金，不再请求全量列表
      const codesSet = new Set(fundCodes)
      holdings.value = holdings.value.filter(h => !codesSet.has(h.fundCode))
      recalcSummary()
    }
    return response
  }

  return { holdings, summary, searchResults, loading, hideAmount, fetchHoldings, fetchSummary, searchFunds, searchFund, addFund, addFundBatch,
           silentFetchHoldings, silentFetchSummary, updateHoldingInPlace, recalcSummary, deleteBatch }
})
