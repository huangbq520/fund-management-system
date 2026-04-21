import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Response interceptor
api.interceptors.response.use(
  response => response.data,
  error => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

// Fund APIs
export const fundApi = {
  // Search funds by keyword (name or code)
  searchFunds: (keyword) => api.get(`/fund/search?keyword=${encodeURIComponent(keyword)}`),

  // Get fund real-time valuation data
  search: (code) => api.get(`/fund/search?keyword=${encodeURIComponent(code)}`),

  // Get fund list
  list: () => api.get('/fund/list'),

  // Get fund detail
  detail: (code) => api.get(`/fund/detail?code=${code}`),

  // Get fund real-time data (valuation)
  getFundData: (code) => api.get(`/fund/data?code=${code}`),

  // Add fund
  add: (fundCode, fundName) => api.post('/fund/add', { fundCode, fundName }),

  // Delete fund
  delete: (fundCode) => api.post('/fund/delete', { fundCode })
}

export default api