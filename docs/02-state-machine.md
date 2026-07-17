# 02 · 需求状态机详设

> 文档版本：v1.0 | 日期：2026-07-17

---

## 1. 主状态机（Requirement Lifecycle）

```
                        ┌─────────────────────────────────────────┐
                        │            cancelled (终态)              │
                        └────────────▲────────────────────────────┘
                                     │ 任意非终态可作废(需审批)
                                     │
 draft ──提交──▶ clarifying ──澄清完毕──▶ pending_review ──发起评审──▶ reviewing
  ▲                 ▲  ▲                                                 │
  │                 │  │                                                 ├──评审拒绝──▶ (回 clarifying)
  │                 │  └── AI/人工补全                                   │
  │                 └────── 信息不足打回                                  ▼
  │                                                              pending_sign
  │                                                                 │
  │                        ┌──── 基线签认(业务负责人) ◀──────────────┤
  │                        │        │                                └──签认拒绝──▶ reviewing
  ▼                        │        ▼
(重提)               baselined ★═══════╗
                        │              ║  基线后修改 ══▶ change_requests 流程(见§3)
                        ▼              ║        ┌──── 重大变更 ────▶ 回 reviewing
                   developing ◀════════╝        └──── 轻微变更 ────▶ 留在 baselined
                        │                              (直接更新版本+通知)
                        ▼
                   accepting ──验收通过──▶ delivered ──归档──▶ archived (终态)
                        │
                        └──验收打回──▶ developing

任意状态(除终态) ──挂起──▶ on_hold ──恢复──▶ 回原状态
```

## 2. 状态 × 动作 × 权限矩阵

| 当前状态 | 动作 | 目标状态 | 操作者 | IM 群内表现 |
|---------|------|---------|--------|------------|
| draft | submit | clarifying | 提出人/PM | —（尚无群） |
| clarifying | clarify_done | pending_review | PM | 群内卡片"信息已齐，可发起评审" |
| clarifying | need_more_info | clarifying | PM/AI | 群内 @提出人 补料清单 |
| pending_review | start_review | reviewing | PM | 发评审卡片给所有评审人 |
| reviewing | review_pass | pending_sign | 系统（票数达标自动） | 群内播报"评审通过 3/3" |
| reviewing | review_reject | clarifying | 系统 | 群内播报+拒绝理由 |
| pending_sign | baseline_sign | **baselined** | 业务负责人 | 签认卡片一键确认（含内容指纹） |
| pending_sign | sign_reject | reviewing | 业务负责人 | 需填理由 |
| baselined | start_dev | developing | Dev Lead | 群内播报"进入开发" |
| baselined | request_change | （CR 流程） | 任意干系人 | 变更卡片 |
| developing | ready_accept | accepting | Dev Lead | @QA+业务 验收卡片 |
| accepting | accept_pass | delivered | 提出人/业务负责人 | 群内喜报+验收记录 |
| accepting | accept_reject | developing | 提出人 | 打回理由群内留痕 |
| delivered | archive | archived | PM/系统(定时) | 触发群解散流程(见§5) |
| 任意 | hold / resume / cancel | on_hold/原/cancelled | PM（cancel 需 Biz Owner 批） | 群内播报 |

## 3. 变更管理子流程（基线后唯一修改通道）

```
基线后点击"申请变更"
       │
       ▼
┌─────────────┐   填写:变更原因/内容/影响评估(工期/范围)
│ 创建变更单   │──────────────────────────────┐
└─────────────┘                              ▼
                              ┌────────────────────────┐
                              │ AI 辅助定级建议          │
                              │ minor: 文字/描述修正     │
                              │ major: 规则/范围/验收变化 │
                              └───────────┬────────────┘
                                          ▼
              ┌──────────── minor ────────────────┬──── major ───────────┐
              ▼                                   ▼
     PM 快速审批(群内卡片)              重新发起评审流(major 必过 Biz Owner)
              │                                   │
              ▼                                   ▼
     生成新 version + 自动通知              通过后生成新 version
     全体干系人(含 diff 摘要)               新基线重新签认 ★
              │                                   │
              └───────────► 需求回到可执行状态 ◀───┘
```

**规则：**
- major 变更后必须重新基线签认（指纹更新），否则不允许继续开发
- 每次变更累计计入"返工率"报表
- 变更单与版本 diff 永久可查（版本对比视图）

## 4. 评审流规则（ReviewFlow）

| 配置项 | 取值 | 说明 |
|--------|------|------|
| 评审类型 | tech / biz / final | 技术评审→业务评审→终评，可按模板裁剪 |
| 会签模式 | all | 全员通过才算过（默认） |
| 或签模式 | any | 任一通过即过（仅限低风险类型） |
| 超时 | 48h 提醒本人 → 72h 升级上级 | 通知域执行 |
| 转交 | 评审人可转委托，留痕 delegated_to | 防请假卡点 |
| 意见强制 | reject 必须填写 comment | 保证可改进 |

## 5. 群生命周期联动（与状态机的绑定）

| 状态事件 | 群动作 |
|---------|--------|
| → clarifying | **自动建群**：拉入干系人；群公告=需求摘要+模板缺失项 |
| 干系人变更 | 自动拉/踢（同步 requirement_stakeholders） |
| 状态流转 | 群公告更新 + 群内播报卡片 |
| → archived | 群公告"将于 24h 后解散" → 导出全量消息 → **解散群** → 归档标记 |
| is_confidential=true | 全程不建群，纯平台流程 |

## 6. 超时与升级策略

| 场景 | T+48h | T+72h | T+7d |
|------|-------|-------|------|
| 评审未响应 | 群内 @提醒 | 卡片抄送其上级 | 自动标记"阻塞"并通知 PM |
| 基线未签 | 私聊+群提醒 | 升级业务负责人上级 | 需求降级为 on_hold 候选 |
| 验收未响应 | @验收人 | 抄送 PM | 报表记录"验收积压" |

## 7. 状态机实现要点

- 单一 `RequirementStateMachine` 服务处理全部流转，**禁止**绕过直接 UPDATE status
- 每次流转写 `audit_logs` + 发领域事件（`requirement.state_changed`）到 MQ
- 事件消费者：群引擎（播报）、通知域（待办）、搜索索引（异步）、报表（累计）
- 流转前置校验：目标状态合法 + 操作者权限 + 业务规则（如 baselined 前必须有 ≥1 条验收标准）
