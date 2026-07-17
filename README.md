# QM · 需求管理平台（Requirement Manager）

> **群即现场，平台即档案；审批在指尖，留底全自动。**
>
> 以需求为轴心的会话式协作平台：一需求一群，流程审批全部在 IM 群内完成，平台自动留底、驱动状态机，需求关闭自动散群并归档。

## 文档索引

| # | 文档 | 内容 |
|---|------|------|
| 00 | [产品概述与定位](docs/00-overview.md) | 痛点、定位、差异化、角色、度量 |
| 01 | [领域模型与 ERD](docs/01-domain-model.md) | 聚合设计、表结构、索引规划 |
| 02 | [需求状态机详设](docs/02-state-machine.md) | 状态流转、评审/变更/签认规则、超时升级 |
| 03 | [IM 集成设计](docs/03-im-integration.md) | 飞书/企微能力矩阵、卡片设计、书记员引擎 |
| 04 | [技术架构设计](docs/04-architecture.md) | 选型决策、模块结构、非功能设计、部署 |
| 05 | [API 设计清单](docs/05-api-design.md) | REST API 全集、错误码、幂等约定 |
| 06 | [安全与权限](docs/06-security-permissions.md) | RBAC、审计、合规、上线检查表 |
| 07 | [分期路线与验收标准](docs/07-roadmap.md) | 技术红线验证、P0-P3 路线、验收标准 |
| 08 | [产品应用组件与输出物](docs/08-product-outputs.md) | 应用组件、输出物清单、角色视角 |

## 核心设计速览

```
业务方提需求(群对话/表单)
   → 自动建群,干系人全部就位
   → AI/模板引导澄清补全
   → 群内卡片评审(会签)
   → 业务负责人群内一键基线签认(内容指纹锁定)
   → 开发/验收(状态播报在群)
   → 归档:导出全部群记录 → 解散群
   → 需求库全文/语义可检索,全链路可追溯
```

## 技术栈（P0）

Java 17 · Spring Boot 3 · PostgreSQL 16 · Redis · RabbitMQ · Meilisearch · MinIO · React 18 + TipTap · Docker Compose · 飞书 OpenAPI（P1 接企微）

## 本地开发

```bash
# 1. 启动中间件栈（PG16+pgvector / Redis / RabbitMQ / Meilisearch / MinIO）
docker compose -f docker-compose.dev.yml up -d

# 2. 初始化表结构（pgvector 扩展由容器自动安装，DDL 需手工执行一次）
psql -h localhost -U qm -d qm -f qm-server/initdb/02-ddl.sql

# 3. 启动后端（Java 17，需飞书应用凭证）
export FEISHU_APP_ID=<your-app-id> FEISHU_APP_SECRET=<your-app-secret>
cd qm-server && mvn spring-boot:run        # http://localhost:8080

# 4. 启动前端（/api 代理目标见 vite.config.ts）
cd qm-web && npm install && npm run dev    # http://localhost:3000
```

备份/恢复/验证脚本见 `backups/`（每日 pg_dump 基线演练已通过）。

## 当前状态

- [x] 设计文档 v1.0（9 篇）
- [x] 飞书技术红线验证 V0-V5 通过
- [x] A9 备份恢复演练通过
- [ ] P0 开发（进行中）

### P0 进度速览

| 模块 | 状态 |
|---|---|
| 需求域（CRUD / 版本 / 内容指纹 / 12 态状态机） | ✅ 已实现 |
| 评审 + 基线签认（卡片回调 / 幂等 / 越权校验） | ✅ 已实现 |
| 群引擎（自动建群 / 解散，飞书真实 API 联调通过） | ✅ 已实现 |
| 组织域 + 飞书 SSO 免登 | ✅ 已实现 |
| Meilisearch 搜索 | ✅ 已实现 |
| 书记员归档（MQ 消费 + 消息幂等） | ⚠️ 消费端就绪，飞书事件生产端待接 |
| 通知域 | ⚠️ 落库可用，IM 投递待接 |
| Web 前端（需求池 / 详情 / 创建 / 通知 / 登录） | ⚠️ 骨架可用，评审/签认等业务操作待填 |
| 变更管理 / 审计落地 / 回调验签 | ⬜ 待开发 |

---
*设计基线日期：2026-07-17*
