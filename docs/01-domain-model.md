# 01 · 领域模型与 ERD

> 文档版本：v1.0 | 日期：2026-07-17

---

## 1. 领域上下文地图

```
┌─────────────────────────────────────────────────────────────────┐
│                         QM 系统边界                              │
│                                                                  │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐    │
│  │ 需求域    │──│ 协作域    │──│ 流程域    │──│ 追溯域    │    │
│  │Requirement│  │Collab    │   │Workflow  │   │Trace     │    │
│  └────┬─────┘   └────┬─────┘   └────┬─────┘   └────┬─────┘    │
│       │              │              │              │            │
│  ┌────┴──────────────┴──────────────┴──────────────┴─────┐     │
│  │                    支撑域                               │     │
│  │  组织域(Org) │ 通知域(Notify) │ 集成域(IM/AI/Git)      │     │
│  └────────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
        │                │                  │
        ▼                ▼                  ▼
   飞书 OpenAPI      企业微信 API      LLM API（可换）
```

## 2. 核心聚合与实体清单

| 聚合根 | 实体/值对象 | 说明 |
|--------|------------|------|
| **Requirement** | RequirementVersion、AcceptanceCriteria、Attachment、Comment | 需求聚合：版本树在聚合内 |
| **RequirementGroup** | GroupBinding（IM 群绑定）、GroupEvent | 一需求对一群的绑定关系 |
| **ReviewFlow** | ReviewRound、ReviewVote | 评审流程（可多轮） |
| **Baseline** | BaselineSnapshot（含内容指纹） | 基线签认快照 |
| **ChangeRequest** | ChangeDiff、ChangeApproval | 变更单 |
| **MessageArchive** | ArchivedMessage（群消息快照） | 群聊归档 |
| **Task / TraceLink** | TaskRef、CommitRef、TestCaseRef、ReleaseRef | 追溯链 |
| **User / OrgUnit** | UserIdentity（多 IM 身份映射）、Role | 组织域 |
| **Template** | TemplateField（动态表单 Schema） | 需求模板 |
| **Notification** | NotifyTask（超时升级策略） | 通知域 |
| **AuditLog** | — | 全系统审计（跨聚合） |

## 3. ERD（核心实体）

```
┌─────────────────┐         ┌─────────────────┐
│      users       │         │   org_units      │
├─────────────────┤         ├─────────────────┤
│ id (PK)          │         │ id (PK)          │
│ name             │         │ name             │
│ email            │         │ parent_id (FK)   │
│ feishu_open_id   │◆        │ im_dept_id       │
│ wecom_user_id    │◆ 多IM   └─────────────────┘
│ role             │   身份映射
│ status           │
│ created_at       │
└────────┬────────┘
         │
         │ stakeholder (M:N, 带角色)
         ▼
┌─────────────────────────────┐
│   requirement_stakeholders   │
├─────────────────────────────┤
│ requirement_id (FK)          │
│ user_id (FK)                 │
│ stakeholder_role             │ ◆ requester/biz_owner/pm/dev_lead/qa/watcher
│ added_by / added_at          │
└─────────────────────────────┘
         │
         ▼
┌──────────────────────────────┐        ┌──────────────────────────┐
│        requirements           │        │        templates          │
├──────────────────────────────┤        ├──────────────────────────┤
│ id (PK, uuid)                 │        │ id (PK)                   │
│ req_no (UK)  REQ-2026-0001    │◆人类读 │ name                      │
│ title                         │        │ req_type                  │◆ feature/optimization/bug/data/api
│ req_type (FK→templates)       │───────▶│ field_schema (jsonb)      │◆ 动态表单定义
│ product_line                  │        │ is_active                 │
│ module                        │        └──────────────────────────┘
│ priority  P0/P1/P2/P3         │
│ status  (状态机, 见02)         │
│ current_version_id (FK)       │──┐
│ owner_id (FK→users)  产品负责 │  │
│ is_confidential  保密标记      │  │ ◆ 保密需求不建群
│ source_channel web/feishu/wecom/ai │
│ created_by / created_at       │  │
│ updated_at                    │  │
│ closed_at                     │  │
└──────────┬───────────────────┘  │
           │ 1:N                  │
           ▼                      │
┌──────────────────────────────┐  │
│    requirement_versions       │  │
├──────────────────────────────┤  │
│ id (PK)                       │◀─┘
│ requirement_id (FK)           │
│ version_no  (1,2,3...)        │
│ content (jsonb, 块文档)        │◆ TipTap JSON
│ content_text (tsvector)       │◆ 全文索引
│ content_hash (sha256)         │◆ 内容指纹
│ fields_data (jsonb)           │◆ 模板字段值
│ edited_by / edited_at         │
│ change_summary                │
└──────────────────────────────┘
           │
           │ 1:N
           ▼
┌──────────────────────────────┐
│   acceptance_criteria         │
├──────────────────────────────┤
│ id (PK)                       │
│ requirement_id (FK)           │
│ version_id (FK)               │
│ criterion_type                │◆ given_when_then / checklist
│ content (text)                │
│ sort_order                    │
└──────────────────────────────┘

┌──────────────────────────────┐        ┌──────────────────────────┐
│        baselines              │        │      review_flows         │
├──────────────────────────────┤        ├──────────────────────────┤
│ id (PK)                       │        │ id (PK)                   │
│ requirement_id (FK)           │        │ requirement_id (FK)       │
│ version_id (FK)  签认的版本    │        │ round_no                  │
│ content_hash (指纹快照)        │        │ review_type               │◆ tech/biz/final
│ snapshot (jsonb 完整内容)      │        │ mode  all/any             │◆ 会签/或签
│ signed_by (FK→users)          │        │ status                    │◆ in_progress/passed/rejected
│ signed_at                     │        │ started_at / finished_at  │
│ signature_meta (jsonb)        │◆ IP/   └──────────┬───────────────┘
│   设备/卡片消息ID              │                   │ 1:N
└──────────────────────────────┘                   ▼
                                    ┌──────────────────────────┐
                                    │      review_votes         │
┌──────────────────────────────┐   ├──────────────────────────┤
│      change_requests          │   │ id (PK)                   │
├──────────────────────────────┤   │ flow_id (FK)              │
│ id (PK)                       │   │ voter_id (FK→users)       │
│ requirement_id (FK)           │   │ decision approve/reject/  │
│ from_version_id / to_version_id│  │        abstain/pending    │
│ diff (jsonb)                  │   │ comment                   │
│ reason                        │   │ voted_at                  │
│ impact_assessment             │   │ delegated_to (FK, 可转交) │
│ level  minor/major            │◆   │ card_msg_id (IM卡片ID)    │
│ status                        │   └──────────────────────────┘
│ approved_by / approved_at     │
└──────────────────────────────┘

┌──────────────────────────────┐
│     requirement_groups        │◆ 群生命周期引擎核心表
├──────────────────────────────┤
│ id (PK)                       │
│ requirement_id (FK, UK)       │◆ 一对一
│ im_provider feishu/wecom      │
│ chat_id (IM群ID)              │
│ group_name                    │
│ status active/dissolving/     │
│        dissolved              │
│ created_at                    │
│ dissolved_at                  │
│ archive_exported (bool)       │
│ archive_path (MinIO key)      │
└──────────┬───────────────────┘
           │ 1:N
           ▼
┌──────────────────────────────┐
│     message_archives          │◆ 书记员引擎写入
├──────────────────────────────┤
│ id (PK, bigint)               │
│ requirement_id (FK)           │
│ im_msg_id (UK, 幂等键)         │
│ im_provider                   │
│ sender_id (FK→users)          │
│ msg_type text/card/file/...   │
│ content (jsonb 原文快照)       │
│ content_text (tsvector)       │◆ 全文检索
│ is_key_info (bool)            │◆ AI打标：业务规则/决策点
│ key_info_merged (bool)        │◆ 是否已并入需求文档
│ msg_time                      │
└──────────────────────────────┘

┌──────────────────────────────┐
│       trace_links             │◆ 追溯矩阵
├──────────────────────────────┤
│ id (PK)                       │
│ requirement_id (FK)           │
│ link_type task/commit/branch/ │
│   test_case/release/mr        │
│ external_id (任务ID/commit SHA)│
│ external_url                  │
│ title                         │
│ source manual/webhook         │
│ created_at                    │
└──────────────────────────────┘

┌──────────────────────────────┐    ┌───────────────────────────┐
│        audit_logs             │    │      notifications         │
├──────────────────────────────┤    ├───────────────────────────┤
│ id (PK, bigint)               │    │ id (PK)                    │
│ actor_id (FK)                 │    │ user_id (FK)               │
│ action                        │◆   │ type                       │◆ review_request/
│ target_type / target_id       │    │ requirement_id (FK)        │  baseline_sign/timeout...
│ detail (jsonb)                │    │ payload (jsonb)            │
│ ip / user_agent               │    │ channel im_card/im_msg/    │
│ created_at (只增不改)          │    │   web                      │
└──────────────────────────────┘    │ status pending/sent/read   │
                                    │ sent_at / read_at          │
                                    └───────────────────────────┘
```

## 4. 关键设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 需求编号 | `REQ-2026-0001`（年+序列，DB sequence 按年分区） | 人类可读，群里口头引用方便 |
| 文档格式 | TipTap JSON（jsonb）+ 冗余纯文本列 | 结构化编辑 + tsvector 全文检索两不误 |
| 版本与基线分离 | versions 表存历史，baselines 存签认快照 | 签认内容永不篡改（即使原版本后续被编辑） |
| 消息幂等 | `im_msg_id` 唯一约束 | IM 回调会重发，靠 DB 约束去重 |
| 软删除 | 需求/文档不物理删，status=archived | 审计合规 |
| 多 IM 身份 | users 表冗余 feishu_open_id / wecom_user_id | P0 单平台也够；P1 加企微不迁移 |
| 保密需求 | `is_confidential=true` → 群引擎跳过 + RBAC 收紧 | 敏感需求不建群（风险对策） |

## 5. 状态字段速查

```
requirements.status:
  draft → clarifying → pending_review → reviewing
        → pending_sign → baselined → developing
        → accepting → delivered → archived
  特殊态: rejected(评审拒绝回炉) / on_hold(挂起) / cancelled

review_flows.status:   in_progress / passed / rejected / cancelled
change_requests.status: pending / approved / rejected / merged
requirement_groups.status: active / dissolving / dissolved
```

## 6. 索引规划（P0 必备）

```sql
-- 需求检索
CREATE INDEX idx_req_status     ON requirements(status, product_line);
CREATE INDEX idx_req_stake      ON requirement_stakeholders(user_id);
-- 全文搜索
CREATE INDEX idx_ver_fts        ON requirement_versions USING GIN(to_tsvector('simple', content_text));
CREATE INDEX idx_msg_fts        ON message_archives USING GIN(to_tsvector('simple', content_text));
-- 语义搜索（P2, pgvector）
-- ALTER TABLE requirement_versions ADD COLUMN embedding vector(1024);
-- CREATE INDEX ON requirement_versions USING hnsw (embedding vector_cosine_ops);
-- 幂等与关联
CREATE UNIQUE INDEX idx_msg_idem ON message_archives(im_provider, im_msg_id);
CREATE INDEX idx_trace_req      ON trace_links(requirement_id, link_type);
-- 审计
CREATE INDEX idx_audit_target   ON audit_logs(target_type, target_id, created_at DESC);
```
