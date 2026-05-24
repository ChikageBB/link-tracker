--liquibase formatted sql

--changeset bazarnoff:001-create-chats
CREATE TABLE chats
(
    id BIGINT PRIMARY KEY
);

--rollback DROP TABLE chats;

--changeset bazarnoff:002-create-links
CREATE TABLE links
(
    id            BIGSERIAL PRIMARY KEY,
    url           TEXT        NOT NULL UNIQUE,
    last_check_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

--changeset bazarnoff:003-create-chat-links
CREATE TABLE chat_links
(
    chat_id BIGINT NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    link_id BIGINT NOT NULL REFERENCES links (id) ON DELETE CASCADE,
    PRIMARY KEY (chat_id, link_id)
);

CREATE INDEX idx_chat_links_chat_id ON chat_links (chat_id);
CREATE INDEX idx_chat_links_link_id ON chat_links (link_id);

--rollback DROP INDEX idx_chat_links_chat_id; DROP INDEX idx_chat_links_link_id; DROP TABLE chat_links;

--changeset bazarnoff:004-create-tag
CREATE TABLE tags
(
    id   BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

--rollback DROP TABLE tags;

--changeset bazarnoff:005-create-chat-link-tags
CREATE TABLE chat_link_tags
(
    chat_id BIGINT NOT NULL ,
    link_id BIGINT NOT NULL ,
    tag_id BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (chat_id, link_id, tag_id),
    FOREIGN KEY (chat_id, link_id) REFERENCES chat_links(chat_id, link_id) ON DELETE CASCADE
);

CREATE INDEX idx_chat_link_tags_tag_id ON chat_link_tags (tag_id);

--rollback DROP INDEX idx_chat_ling_tags_tag_id; DROP TABLE chat_link_tags;
