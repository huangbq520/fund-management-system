import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api'

export const useMarketStore = defineStore('market', () => {
  const indices = ref([])

  async function fetchIndices() {
    try {
      const response = await api.get('/market/indices')
      if (response.code === 200 && response.data) {
        indices.value = response.data
      }
    } catch (err) {
      console.error('获取大盘指数失败:', err)
    }
  }

  return { indices, fetchIndices }
})
