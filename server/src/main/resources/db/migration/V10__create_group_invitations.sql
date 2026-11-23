CREATE TABLE group_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    inviter_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    invitee_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_group_invitations_not_self CHECK (inviter_id <> invitee_id)
);

CREATE UNIQUE INDEX uq_group_invitations_pending
    ON group_invitations (group_id, invitee_id)
    WHERE status = 'PENDING';

CREATE INDEX idx_group_invitations_invitee_status
    ON group_invitations (invitee_id, status);
