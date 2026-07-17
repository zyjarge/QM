import { useEffect, useState } from 'react'
import { List, Button, Tag } from 'antd'
import api from '../api'

export default function NotificationList() {
  const [notifications, setNotifications] = useState<any[]>([])

  const fetchData = () => {
    api.get<any, { data: any[] }>('/notifications').then(res => setNotifications(res.data))
  }

  useEffect(() => { fetchData() }, [])

  return (
    <List
      header={<Button onClick={fetchData}>刷新</Button>}
      dataSource={notifications}
      renderItem={(item: any) => (
        <List.Item>
          <List.Item.Meta
            title={<span><Tag>{item.type}</Tag>{item.payload}</span>}
            description={item.createdAt}
          />
        </List.Item>
      )}
    />
  )
}
