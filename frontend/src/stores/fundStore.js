import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import { fundApi } from '../api'

export const useFundStore = defineStore('fund', () => {
  const holdings = ref([])
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
      }
    } catch (err) {
      console.error('Failed to load holdings:', err)
    } finally {
      loading.holdings = false
    }
  }

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

  async function silentFetchHoldings() {
    try {
      const response = await fundApi.getHoldingList()
      if (response.code === 200) {
        holdings.value = response.data || []
      }
    } catch (err) {
      console.error('Failed to load holdings:', err)
    }
  }

  async function silentFetchSummary() {
    try {
      const response = await fundApi.getPortfolioSummary()
      if (response.code === 200 && response.data) {
        summary.value = response.data
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
    if (response.code === 200) {
      await fetchHoldings()
      await fetchSummary()
    }
    return response
  }

  return { holdings, summary, searchResults, loading, fetchHoldings, fetchSummary, searchFunds, addFund,
           silentFetchHoldings, silentFetchSummary, updateHoldingInPlace, recalcSummary }
})
