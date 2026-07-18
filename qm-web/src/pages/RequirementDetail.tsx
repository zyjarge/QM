import { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  Button,
  Card,
  Collapse,
  Descriptions,
  Empty,
  Form,
  Input,
  List,
  message,
  Modal,
  Popconfirm,
  Select,
  Space,
  Tabs,
  Tag,
  Timeline,
  Typography,
} from 'antd'
import api, {
  Baseline,
  MessageArchive,
  Requirement,
  RequirementVersion,
  ReviewFlow,
  ReviewVote,
} from '../api'

const statusNames: Record<string, string> = {
  draft: '草稿',
  clarifying: '待澄清',
  pending_review: '待评审',
  reviewing: '评审中',
  pending_sign: '待签认',
  baselined: '已基线',
  developing: '开发中',
  accepting: '验收中',
  delivered: '已交付',
  archived: '已归档',
  on_hold: '已挂起',
  cancelled: '已取消',
}

// 状态流转操作：当前状态 → 可执行操作（action 字段直接是目标状态名）
const transitionActions: Record<string, { label: string; target: string }[]> = {
  draft: [{ label: '提交澄清', target: 'clarifying' }],
  clarifying: [{ label: '提交评审', target: 'pending_review' }],
  baselined: [{ label: '开始开发', target: 'developing' }],
  developing: [{ label: '开始验收', target: 'accepting' }],
  accepting: [{ label: '验收通过', target: 'delivered' }],
  delivered: [{ label: '归档', target: 'archived' }],
}

const reviewTypeNames: Record<string, string> = {
  tech: '技术评审',
  biz: '业务评审',
  final: '终审',
}

const reviewModeNames: Record<string, string> = {
  all: '会签',
  any: '或签',
}

const reviewStatusTags: Record<string, { color: string; name: string }> = {
  in_progress: { color: 'processing', name: '进行中' },
  passed: { color: 'success', name: '已通过' },
  rejected: { color: 'error', name: '已驳回' },
  cancelled: { color: 'default', name: '已取消' },
}

const decisionTags: Record<string, { color: string; name: string }> = {
  pending: { color: 'default', name: '待投票' },
  approve: { color: 'success', name: '同意' },
  reject: { color: 'error', name: '驳回' },
  abstain: { color: 'warning', name: '弃权' },
}

const errMsg = (e: any, fallback: string) => e?.response?.data?.message || fallback

export default function RequirementDetail() {
  const { id } = useParams()
  const currentUserId = localStorage.getItem('qm_user_id') || ''

  const [req, setReq] = useState<Requirement | null>(null)
  const [versions, setVersions] = useState<RequirementVersion[]>([])
  const [reviews, setReviews] = useState<ReviewFlow[]>([])
  const [baseline, setBaseline] = useState<Baseline | null>(null)
  const [archives, setArchives] = useState<MessageArchive[]>([])
  const [votesMap, setVotesMap] = useState<Record<string, ReviewVote[]>>({})
  const [expandedFlows, setExpandedFlows] = useState<string[]>([])
  const [reviewModalOpen, setReviewModalOpen] = useState(false)
  const [reviewSubmitting, setReviewSubmitting] = useState(false)
  const [rejectFlowId, setRejectFlowId] = useState<string | null>(null)
  const [rejectComment, setRejectComment] = useState('')
  const [reviewForm] = Form.useForm()

  const fetchVotes = async (flowId: string) => {
    try {
      const res = await api.get<any, { data: ReviewVote[] }>(`/reviews/${flowId}/votes`)
      setVotesMap(prev => ({ ...prev, [flowId]: res.data || [] }))
    } catch (e: any) {
      message.error(errMsg(e, '投票明细加载失败'))
    }
  }

  // 统一刷新：详情 + 版本 + 评审 + 基线 + 归档
  const fetchAll = useCallback(async () => {
    if (!id) return
    try {
      const [reqRes, verRes, revRes, baseRes, archRes] = await Promise.all([
        api.get<any, { data: Requirement }>(`/requirements/${id}`),
        api.get<any, { data: RequirementVersion[] }>(`/requirements/${id}/versions`),
        api.get<any, { data: ReviewFlow[] }>(`/requirements/${id}/reviews`),
        api.get<any, { data: Baseline | null }>(`/requirements/${id}/baseline`),
        api.get<any, { data: MessageArchive[] }>(`/archives/requirements/${id}`),
      ])
      setReq(reqRes.data)
      setVersions(verRes.data || [])
      setReviews(revRes.data || [])
      setBaseline(baseRes.data || null)
      setArchives(archRes.data || [])
    } catch (e: any) {
      message.error(errMsg(e, '加载失败'))
    }
  }, [id])

  useEffect(() => { fetchAll() }, [fetchAll])

  // 1. 状态流转
  const doTransition = async (target: string, label: string) => {
    try {
      await api.post(`/requirements/${id}/transitions`, { action: target })
      message.success(`${label}成功`)
      fetchAll()
    } catch (e: any) {
      message.error(errMsg(e, `${label}失败`))
    }
  }

  // 2. 发起评审
  const startReview = async (values: { voterIds: string[]; reviewType: string; mode: string }) => {
    setReviewSubmitting(true)
    try {
      await api.post(`/requirements/${id}/reviews`, values)
      message.success('评审已发起')
      setReviewModalOpen(false)
      fetchAll()
    } catch (e: any) {
      message.error(errMsg(e, '发起评审失败'))
    } finally {
      setReviewSubmitting(false)
    }
  }

  // 3. 评审投票
  const castVote = async (flowId: string, decision: 'approve' | 'reject', comment?: string) => {
    try {
      await api.post(`/reviews/${flowId}/votes`, { voterId: currentUserId, decision, comment })
      message.success(decision === 'approve' ? '已同意' : '已驳回')
      fetchAll()
      fetchVotes(flowId)
      return true
    } catch (e: any) {
      message.error(errMsg(e, '投票失败'))
      return false
    }
  }

  // 4. 基线签认：先取当前版本 contentHash 展示，确认后调用签认接口
  const signBaseline = async () => {
    let version: RequirementVersion | undefined
    try {
      const res = await api.get<any, { data: RequirementVersion }>(`/requirements/${id}/current-version`)
      version = res.data
    } catch (e: any) {
      message.error(errMsg(e, '获取当前版本失败'))
      return
    }
    Modal.confirm({
      title: '基线签认',
      content: (
        <div>
          <p>将当前版本 v{version?.versionNo ?? '-'} 签认为基线，签认后内容固化。</p>
          <p>
            内容 Hash：
            <Typography.Text code style={{ wordBreak: 'break-all' }}>
              {version?.contentHash || '-'}
            </Typography.Text>
          </p>
        </div>
      ),
      okText: '确认签认',
      cancelText: '取消',
      onOk: async () => {
        try {
          await api.post(`/requirements/${id}/baseline/sign`)
          message.success('基线签认成功')
          fetchAll()
        } catch (e: any) {
          message.error(errMsg(e, '基线签认失败'))
        }
      },
    })
  }

  if (!req) return <div>加载中...</div>

  const actions = transitionActions[req.status] || []

  return (
    <div>
      <Card
        title={req.title}
        extra={
          <Space>
            {actions.map(a => (
              <Popconfirm
                key={a.target}
                title={`确认${a.label}？`}
                onConfirm={() => doTransition(a.target, a.label)}
              >
                <Button type="primary">{a.label}</Button>
              </Popconfirm>
            ))}
            {req.status === 'pending_review' && (
              <Button type="primary" onClick={() => setReviewModalOpen(true)}>发起评审</Button>
            )}
            {req.status === 'pending_sign' && (
              <Button type="primary" onClick={signBaseline}>基线签认</Button>
            )}
            <Tag color="blue">{req.reqNo}</Tag>
          </Space>
        }
      >
        <Descriptions column={2}>
          <Descriptions.Item label="状态">
            {statusNames[req.status] || req.status}
          </Descriptions.Item>
          <Descriptions.Item label="优先级">{req.priority}</Descriptions.Item>
          <Descriptions.Item label="产品线">{req.productLine || '-'}</Descriptions.Item>
          <Descriptions.Item label="创建人">{req.createdBy || '-'}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="评审记录" style={{ marginTop: 16 }}>
        {reviews.length === 0 ? (
          <Empty description="暂无评审记录" />
        ) : (
          <Collapse
            activeKey={expandedFlows}
            onChange={keys => {
              const flowIds = keys as string[]
              setExpandedFlows(flowIds)
              flowIds.forEach(fid => {
                if (!votesMap[fid]) fetchVotes(fid)
              })
            }}
            items={reviews.map(f => {
              const statusTag = reviewStatusTags[f.status] || { color: 'default', name: f.status }
              const votes = votesMap[f.id] || []
              const canVote =
                f.status === 'in_progress' &&
                votes.some(v => v.voterId === currentUserId && v.decision === 'pending')
              return {
                key: f.id,
                label: (
                  <Space>
                    <span>第 {f.roundNo || 1} 轮</span>
                    <Tag>{reviewTypeNames[f.reviewType] || f.reviewType}</Tag>
                    <Tag>{reviewModeNames[f.mode] || f.mode}</Tag>
                    <Tag color={statusTag.color}>{statusTag.name}</Tag>
                    <span style={{ color: '#999', fontSize: 12 }}>
                      {f.startedAt}
                      {f.finishedAt ? ` ~ ${f.finishedAt}` : ''}
                    </span>
                  </Space>
                ),
                children: (
                  <>
                    <List
                      size="small"
                      dataSource={votes}
                      locale={{ emptyText: <Empty description="暂无投票" /> }}
                      renderItem={(v: ReviewVote) => {
                        const tag = decisionTags[v.decision] || { color: 'default', name: v.decision }
                        return (
                          <List.Item>
                            <Space>
                              <span>{v.voterId}</span>
                              <Tag color={tag.color}>{tag.name}</Tag>
                              {v.comment && <span style={{ color: '#666' }}>{v.comment}</span>}
                              {v.votedAt && (
                                <span style={{ color: '#999', fontSize: 12 }}>{v.votedAt}</span>
                              )}
                            </Space>
                          </List.Item>
                        )
                      }}
                    />
                    {canVote && (
                      <Space style={{ marginTop: 8 }}>
                        <Popconfirm
                          title="确认投同意票？"
                          onConfirm={() => castVote(f.id, 'approve')}
                        >
                          <Button type="primary" size="small">同意</Button>
                        </Popconfirm>
                        <Button
                          danger
                          size="small"
                          onClick={() => {
                            setRejectFlowId(f.id)
                            setRejectComment('')
                          }}
                        >
                          驳回
                        </Button>
                      </Space>
                    )}
                  </>
                ),
              }
            })}
          />
        )}
      </Card>

      <Card title="基线信息" style={{ marginTop: 16 }}>
        {baseline ? (
          <Descriptions column={3}>
            <Descriptions.Item label="内容 Hash">
              <Typography.Text code>{baseline.contentHash?.substring(0, 16)}...</Typography.Text>
            </Descriptions.Item>
            <Descriptions.Item label="签认人">{baseline.signedBy}</Descriptions.Item>
            <Descriptions.Item label="签认时间">{baseline.signedAt}</Descriptions.Item>
          </Descriptions>
        ) : (
          <Empty description="暂无基线" />
        )}
      </Card>

      <Card style={{ marginTop: 16 }}>
        <Tabs
          items={[
            {
              key: 'versions',
              label: '版本历史',
              children: (
                <Timeline
                  items={versions.map(v => ({
                    children: `v${v.versionNo} - ${v.changeSummary || '初始版本'}`,
                  }))}
                />
              ),
            },
            {
              key: 'archives',
              label: `消息归档 (${archives.length})`,
              children: (
                <List
                  dataSource={archives}
                  locale={{ emptyText: <Empty description="暂无归档消息" /> }}
                  renderItem={(m: MessageArchive) => (
                    <List.Item>
                      <List.Item.Meta
                        title={
                          <Space>
                            <Tag>{m.msgType}</Tag>
                            <span>{m.senderId || '-'}</span>
                            <span style={{ color: '#999', fontSize: 12 }}>{m.msgTime}</span>
                          </Space>
                        }
                        description={m.contentText || '-'}
                      />
                    </List.Item>
                  )}
                />
              ),
            },
          ]}
        />
      </Card>

      <Modal
        title="发起评审"
        open={reviewModalOpen}
        confirmLoading={reviewSubmitting}
        okText="发起"
        cancelText="取消"
        onOk={() => reviewForm.submit()}
        onCancel={() => setReviewModalOpen(false)}
        destroyOnClose
      >
        <Form
          form={reviewForm}
          layout="vertical"
          preserve={false}
          initialValues={{ voterIds: [], reviewType: 'final', mode: 'all' }}
          onFinish={startReview}
        >
          <Form.Item
            name="voterIds"
            label="投票人"
            rules={[{ required: true, message: '请至少添加一名投票人' }]}
          >
            <Select
              mode="tags"
              open={false}
              placeholder="输入飞书 open_id，回车添加"
              tokenSeparators={[',', ' ']}
            />
          </Form.Item>
          <Form.Item name="reviewType" label="评审类型">
            <Select
              options={[
                { value: 'tech', label: '技术评审' },
                { value: 'biz', label: '业务评审' },
                { value: 'final', label: '终审' },
              ]}
            />
          </Form.Item>
          <Form.Item name="mode" label="评审模式">
            <Select
              options={[
                { value: 'all', label: '会签（需全部同意）' },
                { value: 'any', label: '或签（一人同意即可）' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="驳回评审"
        open={!!rejectFlowId}
        okText="确认驳回"
        cancelText="取消"
        okButtonProps={{ disabled: !rejectComment.trim() }}
        onOk={async () => {
          if (!rejectFlowId) return
          const ok = await castVote(rejectFlowId, 'reject', rejectComment.trim())
          if (ok) setRejectFlowId(null)
        }}
        onCancel={() => setRejectFlowId(null)}
      >
        <Input.TextArea
          rows={3}
          placeholder="请填写驳回原因（必填）"
          value={rejectComment}
          onChange={e => setRejectComment(e.target.value)}
        />
      </Modal>
    </div>
  )
}
