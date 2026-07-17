# 04 · 技术架构设计

> 文档版本：v1.0 | 日期：2026-07-17

---

## 1. 总体架构

```
┌────────────────────────────────────────────────────────────────┐
│                        客户端层                                  │
│  Web SPA(React) │ 飞书工作台H5 │ 企微工作台H5(P1) │ OpenAPI      │
└───────┬────────────────┬──────────────────┬──────────┬─────────┘
        │ HTTPS          │ HTTPS            │ HTTPS    │ HTTPS
┌───────▼────────────────▼──────────────────▼──────────▼─────────┐
│                   网关层 (Nginx / Spring Cloud Gateway)          │
│              认证过滤器 · 限流 · 请求日志 · CORS                  │
└───────┬────────────────────────────────────────────────────────┘
        │
┌───────▼────────────────────────────────────────────────────────┐
│              QM Server (Spring Boot 3 · 模块化单体)              │
│  ┌─────────────┬──────────────┬───────────────┬──────────────┐ │
│  │ 需求模块     │ 流程模块      │ 群引擎模块     │ 书记员模块    │ │
│  │ requirement │ workflow     │ group-engine  │ archiver     │ │
│  ├─────────────┼──────────────┼───────────────┼──────────────┤ │
│  │ 追溯模块     │ 报表模块      │ 通知模块       │ AI模块       │ │
│  │ trace       │ report       │ notify        │ ai-assistant │ │
│  ├─────────────┴──────────────┴───────────────┴──────────────┤ │
│  │ 支撑模块: auth(RBAC) │ org-sync │ audit │ template        │ │
│  ├───────────────────────────────────────────────────────────┤ │
│  │ 集成层: IMProvider(feishu/wecom) │ LLMClient │ GitWebhook  │ │
│  └───────────────────────────────────────────────────────────┘ │
└───┬──────────┬──────────┬──────────┬──────────┬────────┬───────┘
    │          │          │          │          │        │
    ▼          ▼          ▼          ▼          ▼        ▼
┌───────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌───────┐ ┌────────┐
│ PG 16 │ │ Redis  │ │RabbitMQ│ │MinIO   │ │Meili- │ │pgvector│
│ 主库  │ │缓存/锁 │ │ 事件   │ │附件/   │ │search │ │(P2语义)│
│       │ │幂等键  │ │ 削峰   │ │归档导出│ │全文   │ │        │
└───────┘ └────────┘ └────────┘ └────────┘ └───────┘ └────────┘
        ▲                    ▲
        │                    │
   飞书 OpenAPI          企业微信 API(P1)      LLM API(OpenAI兼容,可换国产)
```

## 2. 技术选型表

| 层 | 选型 | 版本 | 理由 |
|----|------|------|------|
| 语言/框架 | Java 17 + Spring Boot | 3.3.x | IM 事件+审批流+状态机生态最成熟，招聘池大 |
| Web | Spring MVC + Validation | — | 无响应式必要，阻塞 IO 即可 |
| 持久层 | MyBatis-Plus（或 jOOQ） | 3.5.x | 复杂检索 SQL 可控；jsonb 支持好 |
| DB | PostgreSQL | 16 | jsonb/tsvector/pgvector 一库搞定 |
| 缓存 | Redis | 7 | token 缓存、幂等键、分布式锁、限流 |
| MQ | RabbitMQ | 3.13 | IM 事件削峰、领域事件分发；延迟队列做超时升级 |
| 搜索 | Meilisearch | 1.8+ | 轻量全文检索；中文分词可接受，运维成本远低于 ES |
| 对象存储 | MinIO | latest | 附件/归档导出私有化 |
| 前端 | React 18 + TS + Vite + AntD 5 + TipTap | — | 块编辑器 TipTap 可扩展（@提及/状态徽章节点） |
| 部署 | Docker Compose（P0）→ K8s Helm（预留） | — | 私有部署起步简单 |
| 可观测 | Prometheus + Grafana + Loki | — | 指标+日志，企业级标配 |
| CI/CD | GitHub Actions | — | 用户既有工作流 |

**决策记录（ADR 简表）：**

| # | 决策 | 否决项 | 理由 |
|---|------|--------|------|
| 1 | 模块化单体起步 | 微服务 | P0 团队规模小，域边界用 package 隔离，P3 再拆 |
| 2 | RabbitMQ 必装 | 无 MQ | IM 回调风暴必须削峰；延迟队列天然支持超时升级 |
| 3 | PG pgvector | 独立向量库 | P2 语义检索数据量 < 百万级，pgvector 足够 |
| 4 | Meilisearch | Elasticsearch | 运维成本差一个量级，中文场景够用 |
| 5 | Java 而非 .NET | .NET 8（VERP 栈） | 用户明确：不绑其他项目选型；飞书/LLM SDK Java 生态更全 |

## 3. 模块包结构（模块化单体）

```
com.qm
├── requirement/          # 需求聚合
│   ├── domain/           # Requirement, Version, Baseline...
│   ├── app/              # RequirementService, StateMachine
│   ├── infra/            # Mapper, Repository
│   └── api/              # RequirementController
├── workflow/             # 评审/变更/签认流程
├── groupengine/          # 群生命周期(建/管/散)
├── archiver/             # 书记员:消息归档/AI打标
├── notify/               # 通知/待办/超时升级
├── trace/                # 追溯链(Git webhook)
├── report/               # 报表度量
├── ai/                   # LLM 抽象(澄清助手/RAG)
├── integration/
│   ├── im/               # IMProvider 接口
│   │   ├── feishu/       # 飞书实现
│   │   └── wecom/        # 企微实现(P1)
│   └── git/
├── org/                  # 用户/部门/角色
├── auth/                 # SSO 免登/RBAC/审计
└── common/               # 事件总线/幂等/异常/工具
```

**模块间通信规则：** 同进程内走 Spring 事件（`ApplicationEventPublisher`）+ 跨模块服务接口；对外异步一律走 RabbitMQ。禁止模块互查对方表（通过 service 接口），为将来拆服务留路。

## 4. 关键非功能设计

### 4.1 事件驱动（领域事件清单）

| 事件 | 生产者 | 消费者 |
|------|--------|--------|
| requirement.submitted | requirement | groupengine(建群), notify |
| requirement.state_changed | workflow | groupengine(播报), notify, search-index, report |
| review.vote_cast | workflow | groupengine(更新卡片), notify(下一人) |
| baseline.signed | workflow | groupengine, trace, report |
| change.approved | workflow | groupengine(diff 播报), notify(全体干系人) |
| im.message_received | webhook 入口 | archiver(归档), ai(澄清对话识别) |
| group.dissolved | groupengine | archiver(确认归档完成) |

### 4.2 幂等设计（IM 场景生命线）

| 场景 | 幂等键 | 存储 |
|------|--------|------|
| IM 消息回调 | im_provider + im_msg_id | PG 唯一索引 |
| 卡片按钮 | hash(card_id+user+action+flow_round) | Redis SETNX + PG |
| Webhook(Git) | delivery_id | Redis 24h TTL |
| 建群 | requirement_id 唯一约束（一群一需求） | PG |

### 4.3 一致性与事务

- 单聚合内：DB 事务（需求+版本+审计一个事务）
- 跨模块：最终一致，MQ 事件 + 消费幂等 + 失败重试（指数退避，死信队列告警）
- 建群补偿：建群失败 → requirement 状态不前进，retry 3 次后告警 PM，支持人工"重建群"按钮

### 4.4 性能目标

| 指标 | 目标 |
|------|------|
| API P95 | < 300ms（列表页 < 500ms） |
| IM 事件处理 | 回调 ACK < 200ms（先 ACK 后异步处理） |
| 全文搜索 | < 500ms |
| 群消息归档吞吐 | ≥ 200 msg/s 单实例 |
| 规模假设 | 5 万需求 / 200 万消息 / 500 并发用户 |

### 4.5 可观测

- 业务指标：状态流转计数、卡片响应时长、建群成功率、归档积压量
- 告警规则：MQ 积压 > 1000、建群失败率 > 5%、回调验签失败突增
- 审计日志独立表只增不改，关键表开启 PG 行级审计触发器（P2）

## 5. 部署架构（P0）

```
┌─────────────────────────────────────────────┐
│ Docker Host (ECS/内网服务器)                 │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐        │
│  │ nginx   │ │qm-server│ │qm-web   │        │
│  │ (反代)  │ │ (x2 预留)│ │ (静态)  │        │
│  └─────────┘ └─────────┘ └─────────┘        │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐        │
│  │ postgres│ │ redis   │ │rabbitmq │        │
│  └─────────┘ └─────────┘ └─────────┘        │
│  ┌─────────┐ ┌─────────┐                   │
│  │ minio   │ │meilisearch│                  │
│  └─────────┘ └─────────┘                   │
│  备份: pg_dump 每日 → 异地/OSS; MinIO 版本化  │
└─────────────────────────────────────────────┘
外网入口: 仅 nginx:443(HTTPS); IM 回调路径 /webhook/** 独立限流+IP白名单(可选)
```

## 6. 容量与备份

| 项 | 策略 |
|----|------|
| PG 备份 | 每日全量 + WAL 归档，保留 30 天 |
| MinIO | 版本化 + 生命周期；归档导出不可删除（合规） |
| Redis | AOF；丢缓存可重建（token/幂等键短 TTL） |
| 灾备 RPO/RTO | RPO 24h（P0 可接受），RTO 4h；P2 提升 |
