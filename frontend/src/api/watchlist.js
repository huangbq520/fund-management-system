import api from './index'

export const watchlistApi = {
  // 获取自选列表（含实时行情）
  list: () => api.get('/watchlist/list'),

  // 添加单个自选
  add: (fundCode, fundName, groupId = null, notes = null) =>
    api.post('/watchlist/add', { fundCode, fundName, groupId, notes }),

  // 移除单个自选
  remove: (fundCode) => api.post('/watchlist/remove', { fundCode }),

  // 批量操作
  batchAdd: (funds) => api.post('/watchlist/batch-add', { funds }),
  batchRemove: (fundCodes) => api.post('/watchlist/batch-remove', { fundCodes }),

  // 分配分组（groupId 为 null 则取消分组）
  assignGroup: (id, groupId) => api.put(`/watchlist/${id}/group`, { groupId }),

  // 分组 CRUD
  getGroups: () => api.get('/watchlist/groups'),
  createGroup: (groupName) => api.post('/watchlist/groups', { groupName }),
  updateGroup: (id, groupName) => api.put(`/watchlist/groups/${id}`, { groupName }),
  deleteGroup: (id) => api.delete(`/watchlist/groups/${id}`),

  // 基金对比（2-5 只）
  compare: (fundCodes) => api.post('/watchlist/compare', { fundCodes })
}
