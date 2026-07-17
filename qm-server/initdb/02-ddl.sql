-- QM P0 数据库初始化脚本
-- 在本地开发环境执行: docker exec -i qm-postgres psql -U qm -d qm < initdb/02-ddl.sql

-- ========== 组织域 ==========
CREATE TABLE IF NOT EXISTS org_units (
    id              VARCHAR(64) PRIMARY KEY,
    name            VARCHAR(256) NOT NULL,
    parent_id       VARCHAR(64),
    im_dept_id      VARCHAR(128),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
    id              VARCHAR(64) PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    email           VARCHAR(256),
    feishu_open_id  VARCHAR(128),
    wecom_user_id   VARCHAR(128),
    role            VARCHAR(32) NOT NULL DEFAULT 'REQUESTER',
    status          VARCHAR(16) NOT NULL DEFAULT 'active',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_users_feishu ON users(feishu_open_id);

-- ========== 需求域 ==========
CREATE TABLE IF NOT EXISTS requirements (
    id              VARCHAR(64) PRIMARY KEY,
    req_no          VARCHAR(32) NOT NULL UNIQUE,
    title           VARCHAR(512) NOT NULL,
    req_type        VARCHAR(32) NOT NULL,
    product_line    VARCHAR(128),
    module          VARCHAR(128),
    priority        VARCHAR(8) NOT NULL DEFAULT 'P2',
    status          VARCHAR(32) NOT NULL DEFAULT 'draft',
    current_version_id VARCHAR(64),
    owner_id        VARCHAR(64),
    is_confidential BOOLEAN NOT NULL DEFAULT FALSE,
    source_channel  VARCHAR(32) NOT NULL DEFAULT 'web',
    created_by      VARCHAR(64),
    closed_at       TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_req_status ON requirements(status, product_line);
CREATE INDEX IF NOT EXISTS idx_req_no ON requirements(req_no);

CREATE TABLE IF NOT EXISTS requirement_versions (
    id              VARCHAR(64) PRIMARY KEY,
    requirement_id  VARCHAR(64) NOT NULL REFERENCES requirements(id),
    version_no      INTEGER NOT NULL,
    content         TEXT,               -- 文档内容（PG text 兼容 jsonb 场景，P2 如需再切回 jsonb）
    content_text    TEXT,               -- 纯文本用于全文检索
    content_hash    VARCHAR(64),
    fields_data     TEXT,               -- 模板字段值（json 字符串）
    edited_by       VARCHAR(64),
    change_summary  VARCHAR(512),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ver_req ON requirement_versions(requirement_id, version_no DESC);
CREATE INDEX IF NOT EXISTS idx_ver_hash ON requirement_versions(content_hash);

CREATE TABLE IF NOT EXISTS acceptance_criteria (
    id              VARCHAR(64) PRIMARY KEY,
    requirement_id  VARCHAR(64) NOT NULL REFERENCES requirements(id),
    version_id      VARCHAR(64) NOT NULL,
    criterion_type  VARCHAR(32) NOT NULL DEFAULT 'checklist',
    content         TEXT NOT NULL,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS requirement_stakeholders (
    id              VARCHAR(64) PRIMARY KEY,
    requirement_id  VARCHAR(64) NOT NULL REFERENCES requirements(id),
    user_id         VARCHAR(64) NOT NULL,
    stakeholder_role VARCHAR(32) NOT NULL,
    added_by        VARCHAR(64),
    added_at        TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP,
    UNIQUE (requirement_id, user_id, stakeholder_role)
);
CREATE INDEX IF NOT EXISTS idx_stake_req ON requirement_stakeholders(requirement_id);
CREATE INDEX IF NOT EXISTS idx_stake_user ON requirement_stakeholders(user_id);

-- ========== 流程域 ==========
CREATE TABLE IF NOT EXISTS review_flows (
    id              VARCHAR(64) PRIMARY KEY,
    requirement_id  VARCHAR(64) NOT NULL REFERENCES requirements(id),
    round_no        INTEGER NOT NULL DEFAULT 1,
    review_type     VARCHAR(32) NOT NULL DEFAULT 'final',
    mode            VARCHAR(16) NOT NULL DEFAULT 'all',
    status          VARCHAR(32) NOT NULL DEFAULT 'in_progress',
    started_at      TIMESTAMP,
    finished_at     TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS review_votes (
    id              VARCHAR(64) PRIMARY KEY,
    flow_id         VARCHAR(64) NOT NULL REFERENCES review_flows(id),
    voter_id        VARCHAR(64) NOT NULL,
    decision        VARCHAR(16) NOT NULL DEFAULT 'pending',
    comment         TEXT,
    voted_at        TIMESTAMP,
    delegated_to    VARCHAR(64),
    card_msg_id     VARCHAR(128),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS baselines (
    id              VARCHAR(64) PRIMARY KEY,
    requirement_id  VARCHAR(64) NOT NULL REFERENCES requirements(id),
    version_id      VARCHAR(64) NOT NULL,
    content_hash    VARCHAR(64) NOT NULL,
    snapshot        TEXT,
    signed_by       VARCHAR(64) NOT NULL,
    signed_at       TIMESTAMP NOT NULL,
    signature_meta  TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS change_requests (
    id              VARCHAR(64) PRIMARY KEY,
    requirement_id  VARCHAR(64) NOT NULL REFERENCES requirements(id),
    from_version_id VARCHAR(64),
    to_version_id   VARCHAR(64),
    diff            TEXT,
    reason          TEXT,
    impact_assessment TEXT,
    level           VARCHAR(16) NOT NULL DEFAULT 'minor',
    status          VARCHAR(32) NOT NULL DEFAULT 'pending',
    approved_by     VARCHAR(64),
    approved_at     TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP
);

-- ========== 群引擎 ==========
CREATE TABLE IF NOT EXISTS requirement_groups (
    id              VARCHAR(64) PRIMARY KEY,
    requirement_id  VARCHAR(64) NOT NULL UNIQUE REFERENCES requirements(id),
    im_provider     VARCHAR(16) NOT NULL DEFAULT 'feishu',
    chat_id         VARCHAR(128) NOT NULL,
    group_name      VARCHAR(256),
    status          VARCHAR(32) NOT NULL DEFAULT 'active',
    dissolved_at    TIMESTAMP,
    archive_exported BOOLEAN NOT NULL DEFAULT FALSE,
    archive_path    VARCHAR(512),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP
);

-- ========== 书记员 ==========
CREATE TABLE IF NOT EXISTS message_archives (
    id              BIGSERIAL PRIMARY KEY,
    requirement_id  VARCHAR(64) NOT NULL REFERENCES requirements(id),
    im_msg_id       VARCHAR(128) NOT NULL,
    im_provider     VARCHAR(16) NOT NULL DEFAULT 'feishu',
    sender_id       VARCHAR(64),
    msg_type        VARCHAR(32) NOT NULL DEFAULT 'text',
    content         TEXT,
    content_text    TEXT,
    is_key_info     BOOLEAN NOT NULL DEFAULT FALSE,
    key_info_merged BOOLEAN NOT NULL DEFAULT FALSE,
    msg_time        TIMESTAMP NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_msg_idem ON message_archives(im_provider, im_msg_id);
CREATE INDEX IF NOT EXISTS idx_msg_req ON message_archives(requirement_id, msg_time DESC);

-- ========== 通知域 ==========
CREATE TABLE IF NOT EXISTS notifications (
    id              VARCHAR(64) PRIMARY KEY,
    user_id         VARCHAR(64) NOT NULL,
    type            VARCHAR(64) NOT NULL,
    requirement_id  VARCHAR(64),
    payload         TEXT,
    channel         VARCHAR(32) NOT NULL DEFAULT 'im_msg',
    status          VARCHAR(16) NOT NULL DEFAULT 'pending',
    sent_at         TIMESTAMP,
    read_at         TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP
);

-- ========== 追溯域 ==========
CREATE TABLE IF NOT EXISTS trace_links (
    id              VARCHAR(64) PRIMARY KEY,
    requirement_id  VARCHAR(64) NOT NULL REFERENCES requirements(id),
    link_type       VARCHAR(32) NOT NULL,
    external_id     VARCHAR(256),
    external_url    VARCHAR(1024),
    title           VARCHAR(512),
    source          VARCHAR(16) NOT NULL DEFAULT 'manual',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_trace_req ON trace_links(requirement_id, link_type);

-- ========== 模板域 ==========
CREATE TABLE IF NOT EXISTS templates (
    id              VARCHAR(64) PRIMARY KEY,
    name            VARCHAR(256) NOT NULL,
    req_type        VARCHAR(32) NOT NULL,
    field_schema    TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP
);

-- ========== 审计域 ==========
CREATE TABLE IF NOT EXISTS audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    actor_id        VARCHAR(64),
    action          VARCHAR(128) NOT NULL,
    target_type     VARCHAR(64),
    target_id       VARCHAR(64),
    detail          TEXT,
    ip              VARCHAR(64),
    user_agent      VARCHAR(512),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_audit_target ON audit_logs(target_type, target_id, created_at DESC);

-- ========== 全文搜索索引（P2 预留） ==========
-- CREATE INDEX idx_ver_fts ON requirement_versions USING GIN(to_tsvector('simple', content_text));
-- CREATE INDEX idx_msg_fts ON message_archives USING GIN(to_tsvector('simple', content_text));

-- ========== 语义搜索（P2 预留, pgvector） ==========
-- ALTER TABLE requirement_versions ADD COLUMN embedding vector(1024);
-- CREATE INDEX idx_ver_embedding ON requirement_versions USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE requirements IS '需求主表';
COMMENT ON TABLE requirement_versions IS '需求版本（含内容指纹）';
COMMENT ON TABLE baselines IS '基线签认快照';
COMMENT ON TABLE requirement_groups IS '需求与 IM 群的绑定关系';
COMMENT ON TABLE message_archives IS '群消息归档（书记员引擎写入）';
COMMENT ON TABLE audit_logs IS '审计日志（只增不改）';
