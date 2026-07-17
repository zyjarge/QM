import { useEffect, useState } from 'react'
import { Table, Tag, Button, Input, Select, Space } from 'antd'
import { Link } from 'react-router-dom'
import api, { Requirement, Page } from '../api'

const statusColors: Record<string, string> = {
  draft: 'default',
  clarifying: 'processing',
  pending_review: 'warning',
  reviewing: 'processing',
  pending_sign: 'warning',
  baselined: 'success',
  developing: 'processing',
  pending_accept: 'warning',
  delivered: 'success',
  archived: 'default',
}

const statusNames: Record<string, string> = {
  draft: '草稿',
  clarifying: '待澄清',
  pending_review: '待评审',
  reviewing: '评审中',
  pending_sign: '待签认',
  baselined: '已基线',
  developing: '开发中',
  pending_accept: '待验收',
  delivered: '已交付',
  archived: '已归档',
}

export default function RequirementList() {
  const [data, setData] = useState<Page<Requirement>>({ records: [], total: 0, current: 1, size: 10 })
  const [loading, setLoading] = useState(false)
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<string>()

  const fetchData = async (page = 1, size = 10) => {
    setLoading(true)
    try {
      const params: any = { page, size }
      if (keyword) params.keyword = keyword
      if (status) params.status = status
      const res = await api.get<any, { data: Page<Requirement> }>('/requirements', { params })
      setData(res.data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchData() }, [])

  const columns = [
    { title: '编号', dataIndex: 'reqNo', key: 'reqNo', width: 140 },
    {
      title: '标题',
      dataIndex: 'title',
      key: 'title',
      render: (text: string, record: Requirement) => (
        <Link to={`/requirements/${record.id}`}>{text}</Link>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (s: string) => <Tag color={statusColors[s]}>{statusNames[s] || s}</Tag>,
    },
    { title: '优先级', dataIndex: 'priority', key: 'priority', width: 80 },
    { title: '产品线', dataIndex: 'productLine', key: 'productLine', width: 100 },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  ]

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Input.Search
          placeholder="搜索需求"
          style={{ width: 300 }}
          onSearch={(v) => { setKeyword(v); fetchData() }}
        />
        <Select
          placeholder="状态筛选"
          style={{ width: 120 }}
          allowClear
          onChange={(v) => { setStatus(v); fetchData() }}
          options={Object.entries(statusNames).map(([k, v]) => ({ label: v, value: k }))}
        />
        <Button type="primary" onClick={() => fetchData()}>刷新</Button>
      </Space>
      <Table
        columns={columns}
        dataSource={data.records}
        rowKey="id"
        loading={loading}
        pagination={{
          current: data.current,
          pageSize: data.size,
          total: data.total,
          onChange: fetchData,
        }}
      />
    </div>
  )
}
