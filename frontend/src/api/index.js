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
  // Search fund by code
  search: (code) => api.get(`/fund/search?code=${code}`),
  
  // Get fund list
  list: () => api.get('/fund/list'),
  
  // Get fund detail
  detail: (code) => api.get(`/fund/detail?code=${code}`),
  
  // Add fund
  add: (fundCode, fundName) => api.post('/fund/add', { fundCode, fundName }),
  
  // Delete fund
  delete: (fundCode) => api.post('/fund/delete', { fundCode })
}

export default api