--liquibase formatted sql

--changeset codex:20260529-0001-create-news-articles
CREATE TABLE IF NOT EXISTS news_articles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    politician_id UUID REFERENCES politicians(id) ON DELETE SET NULL,
    title TEXT NOT NULL,
    source VARCHAR(255) NOT NULL,
    published_date TIMESTAMP,
    url TEXT UNIQUE NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE IF EXISTS news_articles CASCADE;

--changeset codex:20260529-0002-create-news-article-indexes
CREATE INDEX IF NOT EXISTS idx_news_articles_politician_id ON news_articles(politician_id);
CREATE INDEX IF NOT EXISTS idx_news_articles_published_date ON news_articles(published_date);
CREATE INDEX IF NOT EXISTS idx_news_articles_source ON news_articles(source);
--rollback DROP INDEX IF EXISTS idx_news_articles_source;
--rollback DROP INDEX IF EXISTS idx_news_articles_published_date;
--rollback DROP INDEX IF EXISTS idx_news_articles_politician_id;
