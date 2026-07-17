import { Button, Card, Space, Typography } from 'antd'
import { useNavigate } from 'react-router-dom'
import api from '../api'

const { Title, Paragraph } = Typography

export default function Login() {
  const navigate = useNavigate()

  const handleFeishuLogin = async () => {
    const redirectUri = encodeURIComponent(`${window.location.origin}/auth/callback`)
    const res = await api.get<any, { data: { url: string } }>(
      `/auth/feishu/login-url?redirectUri=${redirectUri}`
    )
    window.location.href = res.data.url
  }

  const handleDevLogin = () => {
    // 开发模式：直接写入模拟用户
    localStorage.setItem('qm_user_id', 'ou_468e8d1b320ec3cb25bbe2ccb14b28c7')
    localStorage.setItem('qm_user_name', '开发用户')
    navigate('/')
  }

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', background: '#f0f2f5' }}>
      <Card style={{ width: 400, textAlign: 'center' }}>
        <Title level={3}>QM 需求管理平台</Title>
        <Paragraph type="secondary">群即现场 · 平台即档案</Paragraph>
        <Space direction="vertical" style={{ width: '100%' }}>
          <Button type="primary" size="large" block onClick={handleFeishuLogin}>
            飞书扫码登录
          </Button>
          <Button size="large" block onClick={handleDevLogin}>
            开发模式直接登录
          </Button>
        </Space>
      </Card>
    </div>
  )
}
