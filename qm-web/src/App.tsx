import { Layout, Menu } from 'antd'
import { Routes, Route, useNavigate, useLocation } from 'react-router-dom'
import {
  HomeOutlined,
  UnorderedListOutlined,
  PlusOutlined,
  BellOutlined,
} from '@ant-design/icons'
import RequirementList from './pages/RequirementList'
import RequirementDetail from './pages/RequirementDetail'
import RequirementCreate from './pages/RequirementCreate'
import NotificationList from './pages/NotificationList'

const { Header, Sider, Content } = Layout

export default function App() {
  const navigate = useNavigate()
  const location = useLocation()

  const menuItems = [
    { key: '/', icon: <HomeOutlined />, label: '需求池' },
    { key: '/create', icon: <PlusOutlined />, label: '提需求' },
    { key: '/notifications', icon: <BellOutlined />, label: '通知' },
  ]

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
        <Header style={{ background: '#fff', padding: '0 24px' }}>
          <span style={{ fontSize: 16 }}>群即现场 · 平台即档案</span>
        </Header>
        <Content style={{ padding: 24, background: '#f5f5f5' }}>
          <Routes>
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
