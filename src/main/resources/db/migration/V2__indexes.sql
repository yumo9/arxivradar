-- V2: Phase 1 需要的索引和扩展。

-- 用于关键词 LIKE 搜索(pg_trgm 效果比默认 btree 好)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 论文查询高频索引
CREATE INDEX IF NOT EXISTS idx_paper_title_trgm ON paper USING gin (title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_paper_authors_trgm ON paper USING gin (authors gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_paper_abstract_trgm ON paper USING gin ("abstract" gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_paper_categories ON paper (categories);

-- 排序高频索引
CREATE INDEX IF NOT EXISTS idx_paper_score_overall
    ON paper (((ai_score->>'overall')::int) DESC);
CREATE INDEX IF NOT EXISTS idx_paper_score_novelty
    ON paper (((ai_score->>'novelty')::int) DESC);
