--liquibase formatted sql

--changeset bazarnoff:001-bot-schema
CREATE SCHEMA IF NOT EXISTS bot;

--changeset bazarnoff:002-create-processed-events
CREATE TABLE bot.processed_events (
    event_id UUID PRIMARY KEY ,
    processed_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_processed_events_processed_at
    ON bot.processed_events (processed_at);
--rollback DROP INDEX idx_processed_events_processed_at; DROP TABLE bot.processed_events;
