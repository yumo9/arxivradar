-- V1: 初始 Schema。仅建表结构骨架,Phase 1 会补充索引和数据。

CREATE TABLE IF NOT EXISTS paper (
    id              BIGSERIAL       PRIMARY KEY,
    arxiv_id        VARCHAR(64)     NOT NULL UNIQUE,
    title           TEXT            NOT NULL,
    authors         TEXT            NOT NULL,        -- JSON array 字符串,Phase 1 再决定是否转 JSONB
    abstract        TEXT            NOT NULL,
    categories      TEXT            NOT NULL,        -- JSON array 字符串,同上
    published_date  DATE            NOT NULL,
    pdf_url         VARCHAR(512)    NOT NULL,
    ai_score        JSONB           NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_paper_published_date ON paper (published_date DESC);
CREATE INDEX IF NOT EXISTS idx_paper_deleted ON paper (deleted);

COMMENT ON TABLE paper IS 'arXiv 论文主表';
COMMENT ON COLUMN paper.arxiv_id IS 'arXiv 论文唯一标识, 如 2406.18532';
COMMENT ON COLUMN paper.ai_score IS 'AI 评分 {overall, novelty, impact, readability, methodology}';
COMMENT ON COLUMN paper.deleted IS '软删除标志: 0=正常, 1=已删除';
