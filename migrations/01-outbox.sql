--liquibase formatted sql

--changeset bazarnoff:001-create-outbox-events
CREATE TABLE outbox_events
(
    id BIGSERIAL PRIMARY KEY ,
    event_id UUID NOT NULL UNIQUE ,
    aggregate_id BIGINT NOT NULL,
    topic VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,
    attempts INT NOT NULL DEFAULT 0,
    last_error TEXT,
    status VARCHAR(15) NOT NULL CHECK (status in ('PENDING', 'FAILED'))
);

CREATE INDEX idx_outbox_unpublished
ON outbox_events (created_at) WHERE published_at IS NULL
--rollback DROP INDEX idx_outbox_unpublished; DROP TABLE outbox_events;


