-- The group's chat. Only members read and write it.
-- The messages of someone who left stay, under their name (users are never deleted).
CREATE TABLE group_messages (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id   UUID        NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    sender_id  UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    content    TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Also serves the group_id foreign key: the chat is always read per group, the newest first, page by page.
CREATE INDEX group_messages_group_created_idx ON group_messages (group_id, created_at DESC, id DESC);
CREATE INDEX group_messages_sender_id_idx ON group_messages (sender_id);
