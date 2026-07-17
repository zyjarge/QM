import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Card, Descriptions, Tag, Timeline } from 'antd'
import api, { Requirement } from '../api'

export default function RequirementDetail() {
  const { id } = useParams()
  const [req, setReq] = useState<Requirement | null>(null)
  const [versions, setVersions] = useState<any[]>([])

  useEffect(() => {
    if (id) {
      api.get<any, { data: Requirement }>(`/requirements/${id}`).then(res => setReq(res.data))
      api.get<any, { data: any[] }>(`/requirements/${id}/versions`).then(res => setVersions(res.data))
    }
  }, [id])

  if (!req) return <div>加载中...</div>

  return (
    <div>
      <Card title={req.title} extra={<Tag color="blue">{req.reqNo}</Tag>}>
        <Descriptions column={2}>
          <Descriptions.Item label="状态">{req.status}</Descriptions.Item>
          <Descriptions.Item label="优先级">{req.priority}</Descriptions.Item>
          <Descriptions.Item label="产品线">{req.productLine || '-'}</Descriptions.Item>
          <Descriptions.Item label="创建人">{req.createdBy || '-'}</Descriptions.Item>
        </Descriptions>
      </Card>
      <Card title="版本历史" style={{ marginTop: 16 }}>
        <Timeline
          items={versions.map(v => ({
            children: `v${v.versionNo} - ${v.changeSummary || '初始版本'}`,
          }))}
        />
      </Card>
    </div>
  )
}
