-- QM 开发库初始化: 启用 pgvector (P2 语义检索预留)
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;  -- 模糊检索辅助
