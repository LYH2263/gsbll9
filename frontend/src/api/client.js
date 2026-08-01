import axios from 'axios'
import { useAuthStore } from '../store/auth'

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

apiClient.interceptors.request.use(config => {
  const authStore = useAuthStore()
  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`
  }
  return config
})

apiClient.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      const authStore = useAuthStore()
      authStore.logout()
    }
    return Promise.reject(error)
  }
)

export const authAPI = {
  login: (studentId, password) => apiClient.post('/auth/login', { studentId, password }),
  verify: () => apiClient.get('/auth/verify'),
}

export const contestAPI = {
  getStatus: () => apiClient.get('/contest/status'),
  startContest: () => apiClient.post('/contest/start'),
  getCurrentQuestion: () => apiClient.get('/contest/current-question'),
  getQuestions: () => apiClient.get('/contest/questions'),
  submitAnswer: (questionId, answer) => apiClient.post('/contest/submit-answer', { questionId, answer }),
  nextQuestion: () => apiClient.post('/contest/next-question'),
  prevQuestion: () => apiClient.post('/contest/prev-question'),
  jumpToQuestion: (questionId) => apiClient.post(`/contest/jump-question/${questionId}`),
  getRankings: () => apiClient.get('/contest/rankings'),
  finalize: () => apiClient.post('/contest/finalize'),
  getHints: (questionId) => apiClient.get(`/contest/hints/${questionId}`),
  unlockHint: (hintId) => apiClient.post(`/contest/unlock-hint/${hintId}`),
  getLatestAnnouncement: () => apiClient.get('/contest/announcements/latest'),
}

export const adminAPI = {
  getUsers: () => apiClient.get('/admin/users'),
  createUser: (user) => apiClient.post('/admin/users', user),
  updateUser: (id, user) => apiClient.put(`/admin/users/${id}`, user),
  deleteUser: (id) => apiClient.delete(`/admin/users/${id}`),
  getCategories: () => apiClient.get('/admin/categories'),
  createCategory: (category) => apiClient.post('/admin/categories', category),
  updateCategory: (id, category) => apiClient.put(`/admin/categories/${id}`, category),
  deleteCategory: (id) => apiClient.delete(`/admin/categories/${id}`),
  getQuestions: () => apiClient.get('/admin/questions'),
  createQuestion: (question) => apiClient.post('/admin/questions', question),
  updateQuestion: (id, question) => apiClient.put(`/admin/questions/${id}`, question),
  deleteQuestion: (id) => apiClient.delete(`/admin/questions/${id}`),
  getQuestionsByCategory: (categoryId) => apiClient.get(`/admin/categories/${categoryId}/questions`),
  getContestConfig: () => apiClient.get('/admin/config/contest'),
  updateContestConfig: (config) => apiClient.put('/admin/config/contest', config),
  getStatistics: () => apiClient.get('/admin/statistics'),
  getHints: (questionId) => apiClient.get(`/admin/questions/${questionId}/hints`),
  createHint: (questionId, hint) => apiClient.post(`/admin/questions/${questionId}/hints`, hint),
  updateHint: (id, hint) => apiClient.put(`/admin/hints/${id}`, hint),
  deleteHint: (id) => apiClient.delete(`/admin/hints/${id}`),
  getAnnouncements: () => apiClient.get('/admin/announcements'),
  getAnnouncement: (id) => apiClient.get(`/admin/announcements/${id}`),
  createAnnouncement: (announcement) => apiClient.post('/admin/announcements', announcement),
  updateAnnouncement: (id, announcement) => apiClient.put(`/admin/announcements/${id}`, announcement),
  deleteAnnouncement: (id) => apiClient.delete(`/admin/announcements/${id}`),
  publishAnnouncement: (id) => apiClient.post(`/admin/announcements/${id}/publish`),
  withdrawAnnouncement: (id) => apiClient.post(`/admin/announcements/${id}/withdraw`),
}

export const scoringAPI = {
  getFirstBloods: () => apiClient.get('/scoring/first-bloods'),
  getConfig: () => apiClient.get('/scoring/config'),
  updateConfig: (config) => apiClient.put('/scoring/config', config),
  getOverview: () => apiClient.get('/scoring/overview'),
}

export default apiClient
