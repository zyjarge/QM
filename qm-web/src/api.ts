import axios from 'axios'

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器：自动带上登录态
api.interceptors.request.use((config) => {
  const userId = localStorage.getItem('qm_user_id')
  if (userId) {
    config.headers['X-User-Id'] = userId
  }
  return config
})

api.interceptors.response.use(
  (res) => res.data,
  (err) => {
    console.error('API Error:', err.response?.data || err.message)
    return Promise.reject(err)
  }
)

export default api

// 类型定义
export interface Requirement {
  id: string
  reqNo: string
  title: string
  reqType: string
  productLine?: string
  module?: string
  priority: string
  status: string
  ownerId?: string
  createdBy?: string
  createdAt: string
  updatedAt: string
}

export interface Page<T> {
  records: T[]
  total: number
  current: number
  size: number
}
