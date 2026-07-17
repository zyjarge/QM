# 03 · IM 集成设计（飞书 / 企业微信）

> 文档版本：v1.0 | 日期：2026-07-17

---

## 1. IMProvider 抽象层（多 IM 隔离的关键）

```java
public interface IMProvider {
    // 身份与组织
    String getProviderType();                    // feishu / wecom
    AuthResult authenticate(String authCode);    // OAuth 免登
    OrgSnapshot syncOrg();                       // 通讯录同步(定时+webhook)

    // 群生命周期
    String createGroup(GroupSpec spec);          // 建群 → chat_id
    void addMembers(String chatId, List<String> userIds);
    void removeMember(String chatId, String userId);
    void updateGroupMeta(String chatId, GroupMeta meta);  // 群名/公告
    void dissolveGroup(String chatId);

    // 消息
    String sendCard(String chatId, Card card);          // 群内卡片
    String sendPrivateCard(String userId, Card card);   // 私聊卡片(企微审批降级)
    String sendText(String chatId, String text);
    void updateCard(String cardId, Card card);          // 卡片状态更新(3/3已通过)

    // 消息读取(能力分级)
    ReadCapability getReadCapability();          // FULL(飞书) / ARCHIVE(企微) / NONE
    List<ArchivedMessage> fetchHistory(String chatId, PageToken page); // 解散前导出
}

enum ReadCapability { FULL, ARCHIVE, NONE }
```

## 2. 平台能力矩阵（红线评估）

| 能力 | 飞书 | 企业微信 | QM 降级策略 |
|------|------|---------|------------|
| 自建应用 + 免登 OAuth | ✓ | ✓ | 一致 |
| 通讯录同步（部门/人员/变更事件） | ✓ | ✓ | 一致 |
| API 建群 | ✓ `im/v1/chats` | ✓ `appchat/create` | 一致 |
| 拉人/踢人/改名/公告 | ✓ | ✓ | 一致 |
| 解散群 | ✓ | ✓ | 一致 |
| 群内交互卡片（按钮回调） | ✓ 原生强 | △ 群聊内弱；应用消息强 | 企微：审批卡片走私聊推送+群播报 |
| 机器人读群消息 | ✓ 事件订阅直接给 | ✗ 需"会话内容存档"（付费/合规向） | 企微：书记员只归档卡片事件+机器人对话；群消息靠解散前导出 |
| 群消息导出 | ✓ | ✓（会话存档 API） | 解散前全量导出，两家都做 |
| @机器人对话 | ✓ | ✓（应用消息回调） | 一致 |

**结论：P0 只做飞书，全功能验证；P1 接入企微，启用降级模式。**

## 3. 群内交互设计（核心体验）

### 3.1 群公告（自动维护，状态一目了然）

```
【REQ-2026-0142】库存预警功能
━━━━━━━━━━━━━━━━━━━━
状态：待评审 🔵    优先级：P1
提出人：张三      产品：李四
━━━━━━━━━━━━━━━━━━━━
待办：@王五 @赵六 请完成技术评审
文档：<平台链接>    进度：评审 1/3
```

### 3.2 关键卡片设计

**评审卡片（会签）**
```json
{
  "header": {"title": "【待评审】REQ-0142 V3 库存预警功能", "template": "blue"},
  "elements": [
    {"tag": "div", "text": "请评审技术方案与验收标准（2 条新增规则）"},
    {"tag": "div", "text": "已通过：✅李四  ⏳王五 ⏳赵六"},
    {"tag": "action", "actions": [
      {"type": "button", "text": "通过", "value": {"act": "approve", "flow": "f123"}, "style": "primary"},
      {"type": "button", "text": "有意见", "value": {"act": "reject", "flow": "f123"}, "style": "danger"},
      {"type": "button", "text": "转交他人", "value": {"act": "delegate", "flow": "f123"}}
    ]}
  ]
}
```

**基线签认卡片（业务负责人专属）**
```json
{
  "header": {"title": "【基线签认】REQ-0142 V3", "template": "orange"},
  "elements": [
    {"tag": "div", "text": "评审已全部通过(3/3)。签认后内容锁定，变更需走变更单。"},
    {"tag": "div", "text": "内容指纹: sha256:9f2c…a41b"},
    {"tag": "div", "text": "验收标准: 3 条 | 影响工期评估: 5 人日"},
    {"tag": "action", "actions": [
      {"type": "button", "text": "✍️ 确认基线", "value": {"act": "sign", "hash": "9f2c…"}, "style": "primary"},
      {"type": "button", "text": "退回修改", "value": {"act": "reject_sign"}, "style": "danger"},
      {"type": "button", "text": "查看完整文档", "url": "https://qm.internal/req/0142"}
    ]}
  ]
}
```

**按钮回调处理：**
```
用户点击 → 飞书回调 /webhook/feishu/card
  → 验签 → 幂等校验(card_id+user+action 去重)
  → 权限校验(该用户是否是该 vote 的 voter)
  → 写 review_votes → 检查流程是否完成
  → 更新群内卡片(进度 2/3) → 发状态事件到 MQ
  → 返回 toast "已记录您的评审意见"
```

### 3.3 AI 澄清对话（群内机器人）

```
张三: @QM助手 我要一个库存预警功能
助手: 好的，已创建草稿 REQ-0142。补齐信息才能提交评审：
      ① 预警阈值怎么定？（按单品/品类？固定值/动态计算？）
      ② 预警触发后通知谁？什么方式？
      ③ 什么场景不需要预警？（如在途订单是否计入？）
张三: 按单品，阈值在商品资料里维护，通知采购员，群里通知
助手: ✅ 已记录。还缺：异常分支（商品资料没维护阈值时怎么办？）
张三: 不维护就不预警
助手: 信息已齐，生成需求草稿 V1：[摘要卡片] @李四 请确认提交评审
```

## 4. 书记员引擎（自动留底）

```
飞书事件订阅 ──▶ /webhook/feishu/events ──▶ MQ(削峰) ──▶ 书记员消费
   │ 事件类型:                                          │
   ├─ im.message.receive_v1  ──▶ 归档 message_archives ─┤(im_msg_id 幂等)
   ├─ im.chat.member.user.added_v1 ──▶ 同步群成员记录    │
   └─ im.chat.disbanded_v1 ──▶ 确认解散完成             │
                                                          ▼
   AI 打标(异步): 识别"决策性发言"(含"就这么定"/"阈值定为"/"不做X") 
     ──▶ is_key_info=true ──▶ 群内提示:"检测到疑似决策点,是否录入需求文档? [录入][忽略]"
     ──▶ 点录入 → 内容追加到需求文档"讨论决议"区块,标来源消息
```

## 5. 群解散与归档流程

```
需求 → archived 状态
  → 群引擎发公告:"本群将于 24h 后解散,全部记录已归档:[归档链接]"
  → T+24h 定时任务:
      1. fetchHistory 全量导出(分页) → message_archives 补齐 + 原始 JSON 存 MinIO
      2. 附件下载 → MinIO(微信文件 7 天过期问题从此根治)
      3. dissolveGroup
      4. group.status=dissolved, archive_path 落库
  → 归档页可阅:按时间线的完整群聊记录(只读),支持搜索
```

## 6. 事件与配置清单

**飞书自建应用需要：**
- 权限范围：`im:chat`（建/管群）、`im:message`（收发消息）、`contact:user.base`+`contact:department.base`（通讯录）、`cardkit`（卡片）
- 事件订阅：`im.message.receive_v1`、`im.chat.member.user.added_v1`、`im.chat.disbanded_v1`、`card.action.triggered`
- 加密：Encrypt Key + Verification Token；回调验签必做

**企微自建应用需要（P1）：**
- 应用消息发送、模板卡片（`button_interaction` 回调）
- `appchat/create|update` 群管理
- 通讯录 Secret + 变更回调
- 会话内容存档（可选采购，决定 ReadCapability=ARCHIVE）

## 7. 安全要点

| 项 | 措施 |
|----|------|
| 回调验签 | 飞书 signature 校验 + timestamp 防重放(±5min) |
| 卡片按钮幂等 | `card_action_id = hash(card_id+user+action)` 唯一约束 |
| 越权防护 | 回调时校验操作者是否为该流程的合法参与者 |
| Token 管理 | app_access_token 缓存+提前 5min 刷新；Secret 存环境变量/KMS |
| 保密需求 | 永不建群；Web 端访问二次鉴权 |
