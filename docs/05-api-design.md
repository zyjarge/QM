# 05 · API 设计清单

> 文档版本：v1.0 | 日期：2026-07-17
> 约定：REST + JSON；统一前缀 `/api/v1`；鉴权 Bearer Token（SSO 换取）；错误码规范见 §6

---

## 1. 需求（Requirement）

| Method | Path | 说明 | 权限 |
|--------|------|------|------|
| POST | /requirements | 创建需求（草稿/直接提交澄清） | 登录用户 |
| GET | /requirements | 列表（status/type/产品线/干系人筛选 + 分页） | 登录用户 |
| GET | /requirements/{id} | 详情（含当前版本内容、干系人、流程状态） | 干系人/PM/管理员 |
| PUT | /requirements/{id} | 编辑（生成新版本，基线后禁止直接编辑） | owner/PM |
| GET | /requirements/{id}/versions | 版本列表 | 干系人 |
| GET | /requirements/{id}/versions/diff?from=1&to=3 | 版本 diff | 干系人 |
| POST | /requirements/{id}/transitions | 状态流转 `{action, comment}` | 按状态机矩阵 |
| POST | /requirements/{id}/stakeholders | 增/删干系人（联动群成员） | owner/PM |
| GET | /requirements/{id}/timeline | 全生命周期时间线（事件+消息+审批） | 干系人 |
| GET | /requirements/{id}/archive | 归档包（群记录+版本+审批） | 干系人 |
| GET | /requirements/search?q=&semantic=false | 全文/语义检索（P2） | 登录用户 |

## 2. 评审（Review）

| Method | Path | 说明 |
|--------|------|------|
| POST | /requirements/{id}/reviews | 发起评审 `{review_type, mode, voter_ids}` |
| GET | /requirements/{id}/reviews | 评审轮次与投票明细 |
| POST | /reviews/{flowId}/votes | 投票（Web 端；IM 走卡片回调） `{decision, comment}` |
| POST | /reviews/{flowId}/delegate | 转交 `{to_user_id}` |
| POST | /reviews/{flowId}/cancel | 撤销本轮 |

## 3. 基线与变更（Baseline / Change）

| Method | Path | 说明 |
|--------|------|------|
| POST | /requirements/{id}/baseline/sign | 基线签认 `{content_hash}`（服务端再校验一致性） |
| GET | /requirements/{id}/baseline | 当前基线快照 |
| POST | /requirements/{id}/changes | 创建变更单 `{reason, impact, new_content}` |
| GET | /requirements/{id}/changes | 变更单列表 |
| POST | /changes/{crId}/approve | 审批通过（minor）或触发重审（major） |
| POST | /changes/{crId}/reject | 驳回 `{reason}` |

## 4. 群（Group，管理/排障用）

| Method | Path | 说明 |
|--------|------|------|
| GET | /requirements/{id}/group | 群绑定信息（chat_id、状态、归档路径） |
| POST | /requirements/{id}/group/recreate | 建群失败后人工重建 |
| POST | /requirements/{id}/group/sync-members | 手动同步干系人↔群成员 |
| GET | /requirements/{id}/messages | 群消息归档查询（分页+搜索） |

## 5. 模板与组织

| Method | Path | 说明 |
|--------|------|------|
| GET/POST/PUT | /templates | 模板 CRUD（field_schema jsonb） |
| GET | /org/users?keyword= | 用户搜索（IM 选择器数据源） |
| GET | /org/departments | 部门树 |
| POST | /org/sync | 手动触发通讯录同步（管理员） |

## 6. 追溯（Trace）

| Method | Path | 说明 |
|--------|------|------|
| POST | /requirements/{id}/links | 手动关联 `{link_type, external_id, url}` |
| GET | /requirements/{id}/links | 追溯矩阵视图数据 |
| POST | /webhooks/git/{provider} | Git push/MR webhook（自动关联分支名含 REQ-XXXX） |
| POST | /webhooks/ci | CI 构建结果（群内播报） |

## 7. AI

| Method | Path | 说明 |
|--------|------|------|
| POST | /ai/clarify | 非 IM 渠道的澄清对话（Web 端采集助手） |
| POST | /requirements/{id}/ai/completeness | 要素完整性检查报告 |
| POST | /requirements/{id}/ai/summarize-thread | 群讨论决议提取 |

## 8. 报表与待办

| Method | Path | 说明 |
|--------|------|------|
| GET | /reports/overview | 吞吐/周期/返工率/一次通过率 |
| GET | /reports/rework | 返工率明细（按产品线/时间段） |
| GET | /todos/me | 我的待办（待评审/待签/待验收） |

## 9. IM 回调入口（不走 /api/v1，独立鉴权）

| Method | Path | 说明 |
|--------|------|------|
| POST | /webhook/feishu/events | 消息/群事件订阅（验签） |
| POST | /webhook/feishu/card | 卡片按钮回调（验签+幂等） |
| POST | /webhook/wecom/events | 企微回调（P1） |
| GET  | /webhook/feishu/events | URL 验证（challenge） |

## 10. 错误码规范

```json
{
  "code": "QM-4001",
  "message": "当前状态[reviewing]不允许执行[edit]",
  "trace_id": "abc123"
}
```

| 段位 | 含义 |
|------|------|
| QM-1xxx | 参数/校验错误 |
| QM-2xxx | 权限/越权 |
| QM-3xxx | 资源不存在/冲突 |
| QM-4xxx | 状态机非法流转 |
| QM-5xxx | IM 集成异常（建群失败/回调失败） |
| QM-6xxx | AI 服务异常（降级不阻塞主流程） |

## 11. 通用约定

- 分页：`?page=1&size=20`，返回 `{list, total, page}`
- 时间：ISO 8601 UTC；ID：uuid v7（时间有序）
- 写操作幂等：客户端可传 `Idempotency-Key` header，服务端 24h 去重
- 审计：所有写 API 自动落 audit_logs（AOP）
