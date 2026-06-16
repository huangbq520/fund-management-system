import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import { watchlistApi } from '../api'

export const useWatchlistStore = defineStore('watchlist', () => {
  const items = ref([])
  const groups = ref([])
  const loading = reactive({
    items: false,
    groups: false
  })

  // 多列排序状态
  const sortField = ref(null)
  const sortDirection = ref('asc')

  // 对比选择（存储 fundCode 数组，最多 5 只）
  const compareSelection = ref([])

  // ==================== 数据获取 ====================

  async function fetchItems() {
    loading.items = true
    try {
      const response = await watchlistApi.list()
      if (response.code === 200) {
        items.value = response.data || []
      }
    } catch (err) {
      console.error('获取自选列表失败:', err)
    } finally {
      loading.items = false
    }
  }

  async function silentFetchItems() {
    try {
      const response = await watchlistApi.list()
      if (response.code === 200) {
        items.value = response.data || []
      }
    } catch (err) {
      // 静默刷新，不打印错误
    }
  }

  async function fetchGroups() {
    loading.groups = true
    try {
      const response = await watchlistApi.getGroups()
      if (response.code === 200) {
        groups.value = response.data || []
      }
    } catch (err) {
      console.error('获取分组列表失败:', err)
    } finally {
      loading.groups = false
    }
  }

  // ==================== CRUD ====================

  async function addToWatchlist(fundCode, fundName, groupId = null, notes = null) {
    const response = await watchlistApi.add(fundCode, fundName, groupId, notes)
    if (response.code === 200) {
      await fetchItems()
      await fetchGroups()
    }
    return response
  }

  async function removeFromWatchlist(fundCode) {
    const response = await watchlistApi.remove(fundCode)
    if (response.code === 200) {
      items.value = items.value.filter(item => item.fundCode !== fundCode)
      await fetchGroups()
    }
    return response
  }

  async function batchRemove(fundCodes) {
    const response = await watchlistApi.batchRemove(fundCodes)
    if (response.code === 200) {
      await fetchItems()
      await fetchGroups()
    }
    return response
  }

  // ==================== 分组管理 ====================

  async function createGroup(groupName) {
    const response = await watchlistApi.createGroup(groupName)
    if (response.code === 200) {
      await fetchGroups()
    }
    return response
  }

  async function updateGroup(id, groupName) {
    const response = await watchlistApi.updateGroup(id, groupName)
    if (response.code === 200) {
      await fetchGroups()
    }
    return response
  }

  async function deleteGroup(id) {
    const response = await watchlistApi.deleteGroup(id)
    if (response.code === 200) {
      await fetchGroups()
      await fetchItems()
    }
    return response
  }

  async function assignGroup(watchlistId, groupId) {
    const response = await watchlistApi.assignGroup(watchlistId, groupId)
    if (response.code === 200) {
      await fetchItems()
      await fetchGroups()
    }
    return response
  }

  // ==================== 排序 ====================

  function setSort(field) {
    if (sortField.value === field) {
      // 同一列：升序 -> 降序 -> 取消排序
      if (sortDirection.value === 'asc') {
        sortDirection.value = 'desc'
      } else if (sortDirection.value === 'desc') {
        sortField.value = null
        sortDirection.value = 'asc'
      }
    } else {
      sortField.value = field
      sortDirection.value = 'asc'
    }
  }

  // ==================== 对比选择 ====================

  function toggleCompareSelection(fundCode) {
    const idx = compareSelection.value.indexOf(fundCode)
    if (idx >= 0) {
      compareSelection.value.splice(idx, 1)
    } else {
      if (compareSelection.value.length >= 5) {
        return false // 最多 5 只
      }
      compareSelection.value.push(fundCode)
    }
    return true
  }

  function clearCompareSelection() {
    compareSelection.value = []
  }

  // ==================== 对比 ====================

  async function compareFunds(fundCodes) {
    try {
      const response = await watchlistApi.compare(fundCodes)
      return response
    } catch (err) {
      console.error('基金对比失败:', err)
      return { code: 500, message: '对比失败' }
    }
  }

  return {
    items, groups, loading,
    sortField, sortDirection,
    compareSelection,
    fetchItems, silentFetchItems, fetchGroups,
    addToWatchlist, removeFromWatchlist, batchRemove,
    createGroup, updateGroup, deleteGroup, assignGroup,
    setSort, toggleCompareSelection, clearCompareSelection,
    compareFunds
  }
})
