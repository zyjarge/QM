import { Form, Input, Select, Button, Card, message } from 'antd'
import { useNavigate } from 'react-router-dom'
import api from '../api'

export default function RequirementCreate() {
  const [form] = Form.useForm()
  const navigate = useNavigate()

  const onFinish = async (values: any) => {
    try {
      await api.post('/requirements', values)
      message.success('创建成功')
      navigate('/')
    } catch (e) {
      message.error('创建失败')
    }
  }

  return (
    <Card title="提需求" style={{ maxWidth: 600 }}>
      <Form form={form} layout="vertical" onFinish={onFinish}>
        <Form.Item name="title" label="标题" rules={[{ required: true }]}>
          <Input placeholder="一句话说清楚要什么" />
        </Form.Item>
        <Form.Item name="reqType" label="类型" initialValue="feature">
          <Select
            options={[
              { label: '新功能', value: 'feature' },
              { label: '优化', value: 'improvement' },
              { label: 'Bug修复', value: 'bugfix' },
            ]}
          />
        </Form.Item>
        <Form.Item name="priority" label="优先级" initialValue="P2">
          <Select
            options={[
              { label: 'P0-紧急', value: 'P0' },
              { label: 'P1-高', value: 'P1' },
              { label: 'P2-中', value: 'P2' },
              { label: 'P3-低', value: 'P3' },
            ]}
          />
        </Form.Item>
        <Form.Item name="productLine" label="产品线">
          <Input placeholder="所属产品线" />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit">提交</Button>
        </Form.Item>
      </Form>
    </Card>
  )
}
