import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Spin, Result, Button } from 'antd'
import api from '../api'

export default function AuthCallback() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading')

  useEffect(() => {
    const code = searchParams.get('code')
    if (!code) {
      setStatus('error')
      return
    }

    api.post<any, { data: { id: string; name: string } }>('/auth/feishu/login', { code })
      .then(res => {
        localStorage.setItem('qm_user_id', res.data.id)
        localStorage.setItem('qm_user_name', res.data.name)
        setStatus('success')
        setTimeout(() => navigate('/'), 1500)
      })
      .catch(() => setStatus('error'))
  }, [searchParams, navigate])

  if (status === 'loading') {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
        <Spin size="large" tip="飞书登录中..." />
      </div>
    )
  }

  if (status === 'error') {
    return (
      <Result
        status="error"
        title="登录失败"
        subTitle="飞书授权码无效或已过期，请重新登录"
        extra={<Button type="primary" onClick={() => navigate('/login')}>重新登录</Button>}
      />
    )
  }

  return (
    <Result
      status="success"
      title="登录成功"
      subTitle="正在跳转到需求池..."
    />
  )
}
