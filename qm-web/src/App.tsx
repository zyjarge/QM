import { Layout, Menu, Avatar, Dropdown, Space, message } from 'antd'
import { Routes, Route, useNavigate, useLocation } from 'react-router-dom'
import {
  HomeOutlined,
  UnorderedListOutlined,
  PlusOutlined,
  BellOutlined,
  UserOutlined,
  LogoutOutlined,
} from '@ant-design/icons'
import RequirementList from './pages/RequirementList'
import RequirementDetail from './pages/RequirementDetail'
import RequirementCreate from './pages/RequirementCreate'
import NotificationList from './pages/NotificationList'
import Login from './pages/Login'
import AuthCallback from './pages/AuthCallback'

const { Header, Sider, Content } = Layout

export default function App() {
  const navigate = useNavigate()
  const location = useLocation()

  // 登录态检查
  const userId = localStorage.getItem('qm_user_id')
  const userName = localStorage.getItem('qm_user_name')

  const isAuthPage = location.pathname === '/login' || location.pathname === '/auth/callback'

  if (!userId && !isAuthPage) {
    navigate('/login')
    return null
  }

  const menuItems = [
    { key: '/', icon: <HomeOutlined />, label: '需求池' },
    { key: '/create', icon: <PlusOutlined />, label: '提需求' },
    { key: '/notifications', icon: <BellOutlined />, label: '通知' },
  ]

  const userMenu = {
    items: [
      {
        key: 'logout',
        icon: <LogoutOutlined />,
        label: '退出登录',
        onClick: () => {
          localStorage.removeItem('qm_user_id')
          localStorage.removeItem('qm_user_name')
          navigate('/login')
        },
      },
    ],
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider theme="light" width={200}>
        <div style={{ padding: 16, fontWeight: 'bold', fontSize: 18 }}>
          QM 需求管理
        </div>
        <Menu
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', padding: '0 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ fontSize: 16 }}>群即现场 · 平台即档案</span>
          <Dropdown menu={userMenu}>
            <Space style={{ cursor: 'pointer' }}>
              <Avatar icon={<UserOutlined />} />
              <span>{userName || '未登录'}</span>
            </Space>
          </Dropdown>
        </Header>
        <Content style={{ padding: 24, background: '#f5f5f5' }}>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/auth/callback" element={<AuthCallback />} />
            <Route path="/" element={<RequirementList />} />
            <Route path="/create" element={<RequirementCreate />} />
            <Route path="/requirements/:id" element={<RequirementDetail />} />
            <Route path="/notifications" element={<NotificationList />} />
          </Routes>
        </Content>
      </Layout>
    </Layout>
  )
}
