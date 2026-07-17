import axios from 'axios'

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
    'X-User-Id': 'test-user-001', // TODO: 从登录态获取
  },
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
