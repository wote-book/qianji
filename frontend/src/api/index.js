import request from './request'

export const authApi = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data),
  logout: () => request.post('/auth/logout'),
  me: () => request.get('/auth/me'),
  resetPassword: (data) => request.post('/auth/password/reset', data)
}

export const userApi = {
  profile: () => request.get('/user/profile'),
  updateProfile: (data) => request.put('/user/profile', data),
  changePassword: (data) => request.put('/user/password', data),
  updateSecurity: (data) => request.put('/user/security', data)
}

export const adminApi = {
  overview: (days) => request.get('/admin/overview', { params: { days } }),
  users: (params) => request.get('/admin/users', { params }),
  userDetail: (id) => request.get(`/admin/users/${id}`),
  setStatus: (id, status) => request.patch(`/admin/users/${id}/status`, { status }),
  resetPassword: (id) => request.post(`/admin/users/${id}/reset-password`),
  exportUsers: (params) => request.get('/admin/users/export', { params, responseType: 'blob' }),
  usageTrend: (days) => request.get('/admin/usage/trend', { params: { days } }),
  usageModules: (days) => request.get('/admin/usage/modules', { params: { days } }),
  retention: () => request.get('/admin/usage/retention'),
  segments: () => request.get('/admin/usage/segments'),
  loginLogs: (params) => request.get('/admin/logs/login', { params }),
  usageEvents: (params) => request.get('/admin/logs/events', { params })
}

export const accountApi = {
  list: () => request.get('/accounts'),
  create: (data) => request.post('/accounts', data),
  update: (id, data) => request.put(`/accounts/${id}`, data),
  remove: (id) => request.delete(`/accounts/${id}`),
  reconcile: () => request.get('/accounts/reconcile'),
  fix: () => request.post('/accounts/reconcile/fix')
}

export const categoryApi = {
  list: (type) => request.get('/categories', { params: type ? { type } : {} }),
  create: (data) => request.post('/categories', data),
  batchCreate: (data) => request.post('/categories/batch', data),
  update: (id, data) => request.put(`/categories/${id}`, data),
  setStatus: (id, status) => request.patch(`/categories/${id}/status`, { status }),
  remove: (id) => request.delete(`/categories/${id}`)
}

export const recordApi = {
  page: (params) => request.get('/records', { params }),
  create: (data) => request.post('/records', data),
  update: (id, data) => request.put(`/records/${id}`, data),
  remove: (id) => request.delete(`/records/${id}`),
  importPreview: (formData) => request.post('/records/import/preview', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  importCommit: (rows) => request.post('/records/import/commit', rows)
}

export const aiRecordApi = {
  parse: (data) => request.post('/ai/records/parse', data),
  commit: (data) => request.post('/ai/records/commit', data)
}

export const budgetApi = {
  list: (month) => request.get('/budgets', { params: { month } }),
  execution: (month) => request.get('/budgets/execution', { params: { month } }),
  upsert: (data) => request.put('/budgets', data),
  remove: (id) => request.delete(`/budgets/${id}`)
}

export const statsApi = {
  summary: (month) => request.get('/stats/summary', { params: { month } }),
  category: (month, type) => request.get('/stats/category', { params: { month, type } }),
  trend: (params) => request.get('/stats/trend', { params }),
  dashboard: (month) => request.get('/stats/dashboard', { params: { month } }),
  dailyQuota: (month) => request.get('/stats/daily-quota', { params: { month } })
}

export const goalApi = {
  list: () => request.get('/goals'),
  create: (data) => request.post('/goals', data),
  deposit: (id, amount) => request.post(`/goals/${id}/deposit`, { amount }),
  update: (id, data) => request.put(`/goals/${id}`, data),
  remove: (id) => request.delete(`/goals/${id}`)
}

export const exportApi = {
  recordsExcel: (start, end) => request.get('/export/records.xlsx', {
    params: { start, end },
    responseType: 'blob'
  }),
  reportPdf: (start, end) => request.get('/export/report.pdf', {
    params: { start, end },
    responseType: 'blob'
  }),
  template: () => request.get('/records/import/template', { responseType: 'blob' })
}

export const backupApi = {
  exportData: () => request.get('/backup/export'),
  importData: (payload) => request.post('/backup/import', payload)
}

export const briefApi = {
  generate: (month) => request.post('/report/financial-brief', { month })
}

export const insightApi = {
  anomalies: (month) => request.get('/insights/anomalies', { params: { month } }),
  subscriptions: () => request.get('/insights/subscriptions')
}

export const onboardingApi = {
  status: () => request.get('/onboarding/status'),
  complete: () => request.post('/onboarding/complete')
}

export const demoApi = {
  transactionFailure: () => request.post('/demo/transaction-failure')
}

export function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
