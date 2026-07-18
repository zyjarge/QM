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

export interface RequirementVersion {
  id: string
  requirementId: string
  versionNo: number
  content?: string
  contentText?: string
  contentHash?: string
  fieldsData?: string
  editedBy?: string
  changeSummary?: string
  createdAt: string
}

export interface ReviewFlow {
  id: string
  requirementId: string
  roundNo?: number
  reviewType: string // tech/biz/final
  mode: string // all(会签)/any(或签)
  status: string // in_progress/passed/rejected/cancelled
  startedAt?: string
  finishedAt?: string
  createdAt: string
}

export interface ReviewVote {
  id: string
  flowId: string
  voterId: string
  decision: string // approve/reject/abstain/pending
  comment?: string
  votedAt?: string
}

export interface Baseline {
  id: string
  requirementId: string
  versionId: string
  contentHash: string
  snapshot?: string
  signedBy: string
  signedAt: string
  signatureMeta?: string
}

export interface MessageArchive {
  id: number
  requirementId: string
  imMsgId?: string
  imProvider?: string
  senderId?: string
  msgType: string // text/card/file/image/audio/video
  contentText?: string
  isKeyInfo?: boolean
  msgTime?: string
  createdAt: string
}
