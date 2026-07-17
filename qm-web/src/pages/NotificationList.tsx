import { useEffect, useState } from 'react'
import { List, Button, Tag } from 'antd'
import api from '../api'

export default function NotificationList() {
  const [notifications, setNotifications] = useState<any[]>([])
  const [total, setTotal] = useState(0)

  const fetchData = () => {
    api.get<any, { data: { records: any[]; total: number } }>(
      '/notifications?page=1&size=20'
    ).then(res => {
      setNotifications(res.data.records || [])
      setTotal(res.data.total || 0)
    })
  }

  useEffect(() => { fetchData() }, [])

  return (
    <List
      header={
        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <span>共 {total} 条通知</span>
          <Button onClick={fetchData}>刷新</Button>
        </div>
      }
      dataSource={notifications}
      renderItem={(item: any) => (
        <List.Item>
          <List.Item.Meta
            title={<span><Tag color="blue">{item.type}</Tag>{item.payload}</span>}
            description={item.createdAt}
          />
        </List.Item>
      )}
    />
  )
}
