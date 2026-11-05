-- The whole MVP schema, as described in the spec (sections A–G).
-- V1 stays as it is: users keeps its columns (with "name") and only gets new ones here.
--
-- Rules for the whole database (section G):
--   * every column with a fixed list of values has a CHECK,
--   * every foreign key has an index (PostgreSQL doesn't create them on its own),
--   * ON DELETE CASCADE for link tables, RESTRICT for data with history,
--   * users are never deleted, only blocked (status = BLOCKED).

-- ═══════════════════════════════════════════════
-- A. USERS
-- ═══════════════════════════════════════════════

ALTER TABLE users
    -- Visible only to people in the same groups.
    ADD COLUMN first_name  VARCHAR(100),
    ADD COLUMN last_name   VARCHAR(100),
    ADD COLUMN global_role VARCHAR(20) NOT NULL DEFAULT 'USER'
        CHECK (global_role IN ('USER', 'SYSTEM_ADMIN')),
    ADD COLUMN status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'BLOCKED')),
    -- The server always saves the email in lower case.
    ADD CONSTRAINT users_email_lowercase CHECK (email = lower(email));

-- ═══════════════════════════════════════════════
-- B. PUBLIC CATALOG (visible also without an account)
-- ═══════════════════════════════════════════════
-- Book data comes from Google Books (main) or Open Library (backup) – the server calls the API.
-- A book is saved the first time someone opens its page, rates it, adds it to a list or adds a copy.
-- Before adding, the server looks for it by google_books_id / open_library_id / isbn and reuses it,
-- so copies of the same book stay together.

CREATE TABLE books (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source          VARCHAR(20)  NOT NULL CHECK (source IN ('GOOGLE_BOOKS', 'OPEN_LIBRARY', 'MANUAL')),
    google_books_id VARCHAR(50)  UNIQUE,
    open_library_id VARCHAR(50)  UNIQUE,
    -- Always ISBN-13 without dashes.
    isbn            VARCHAR(13)  UNIQUE CHECK (isbn ~ '^[0-9]{13}$'),
    title           VARCHAR(500) NOT NULL,
    -- Authors as one text, e.g. "Terry Pratchett, Neil Gaiman".
    authors         VARCHAR(1000),
    publisher       VARCHAR(255),
    published_year  SMALLINT,
    page_count      INTEGER CHECK (page_count > 0),
    description     TEXT,
    cover_url       VARCHAR(1000),
    -- Who added it by hand. NULL = it came from an API.
    created_by      UUID REFERENCES users (id) ON DELETE RESTRICT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- A book from an API has that API's id, a book typed in by hand has its author.
    CONSTRAINT books_source_matches CHECK (
        (source = 'GOOGLE_BOOKS' AND google_books_id IS NOT NULL) OR
        (source = 'OPEN_LIBRARY' AND open_library_id IS NOT NULL) OR
        (source = 'MANUAL' AND created_by IS NOT NULL)
    )
);

CREATE INDEX books_created_by_idx ON books (created_by);

-- A fixed list of genres. The app turns the code into a name.
CREATE TABLE genres (
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO genres (code) VALUES
    ('FANTASY'),
    ('SCI_FI'),
    ('CRIME'),
    ('THRILLER'),
    ('HORROR'),
    ('ROMANCE'),
    ('HISTORICAL_FICTION'),
    ('LITERARY_FICTION'),
    ('CLASSICS'),
    ('POETRY'),
    ('DRAMA'),
    ('BIOGRAPHY'),
    ('HISTORY'),
    ('REPORTAGE'),
    ('SCIENCE'),
    ('PHILOSOPHY'),
    ('PSYCHOLOGY'),
    ('BUSINESS'),
    ('SELF_HELP'),
    ('TRAVEL'),
    ('CHILDREN'),
    ('YOUNG_ADULT'),
    ('COMICS'),
    ('OTHER');

-- Filled in when a book from an API is saved, e.g. "Fiction / Fantasy / Epic" → FANTASY.
CREATE TABLE book_genres (
    book_id  UUID NOT NULL REFERENCES books (id) ON DELETE CASCADE,
    genre_id UUID NOT NULL REFERENCES genres (id) ON DELETE CASCADE,
    PRIMARY KEY (book_id, genre_id)
);

CREATE INDEX book_genres_genre_id_idx ON book_genres (genre_id);

-- Public ratings and reviews.
CREATE TABLE book_reviews (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    book_id    UUID        NOT NULL REFERENCES books (id) ON DELETE RESTRICT,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    -- Rating × 2: 2 = ★1, 3 = ★1.5, … 10 = ★5.
    rating     SMALLINT    NOT NULL CHECK (rating BETWEEN 2 AND 10),
    -- A rating alone, without text, is fine.
    content    TEXT,
    -- An admin can hide a review that breaks the rules.
    is_hidden  BOOLEAN     NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX book_reviews_book_id_idx ON book_reviews (book_id);
CREATE INDEX book_reviews_user_id_idx ON book_reviews (user_id);

-- ═══════════════════════════════════════════════
-- C. THE USER'S OWN DATA (kept on the phone for offline reading)
-- ═══════════════════════════════════════════════
-- The phone asks for "everything that changed after <last sync>", so each table here has updated_at,
-- and deleting means setting deleted_at (the row stays, so the phone learns it should delete it too).

-- My physical copies.
CREATE TABLE book_copies (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    book_id      UUID        NOT NULL REFERENCES books (id) ON DELETE RESTRICT,
    owner_id     UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    -- Set ONLY by the owner. "Borrowed" isn't kept here – the server works it out from borrowings.
    availability VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
        CHECK (availability IN ('AVAILABLE', 'UNAVAILABLE', 'LOST')),
    -- Optimistic locking, for requests that clash.
    version      INTEGER     NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at   TIMESTAMPTZ
);

CREATE INDEX book_copies_book_id_idx ON book_copies (book_id);
CREATE INDEX book_copies_owner_id_idx ON book_copies (owner_id);
CREATE INDEX book_copies_updated_at_idx ON book_copies (updated_at);

-- My reading status. It's about the book from the catalog, not a copy,
-- so it works for borrowed books and books I don't have too.
CREATE TABLE user_book_status (
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    book_id    UUID        NOT NULL REFERENCES books (id) ON DELETE RESTRICT,
    status     VARCHAR(20) NOT NULL CHECK (status IN ('TO_READ', 'READING', 'READ', 'ABANDONED')),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    PRIMARY KEY (user_id, book_id)
);

CREATE INDEX user_book_status_book_id_idx ON user_book_status (book_id);
CREATE INDEX user_book_status_updated_at_idx ON user_book_status (updated_at);

-- My lists. FAVORITES is made at sign-up and can't be deleted or renamed.
CREATE TABLE book_lists (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id    UUID         NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    type        VARCHAR(20)  NOT NULL DEFAULT 'CUSTOM' CHECK (type IN ('CUSTOM', 'FAVORITES')),
    -- GROUPS = visible in the groups from list_group_visibility.
    visibility  VARCHAR(20)  NOT NULL DEFAULT 'PRIVATE' CHECK (visibility IN ('PRIVATE', 'GROUPS')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX book_lists_owner_id_idx ON book_lists (owner_id);
CREATE INDEX book_lists_updated_at_idx ON book_lists (updated_at);
-- Each user has at most one FAVORITES list.
CREATE UNIQUE INDEX book_lists_one_favorites ON book_lists (owner_id) WHERE type = 'FAVORITES';
-- Two lists of one user can't have the same name (deleted lists don't count).
CREATE UNIQUE INDEX book_lists_unique_name ON book_lists (owner_id, lower(name)) WHERE deleted_at IS NULL;

-- Books on my lists. For sync the list's updated_at is enough – every change here updates it.
CREATE TABLE book_list_items (
    list_id  UUID        NOT NULL REFERENCES book_lists (id) ON DELETE CASCADE,
    -- The book from the catalog, not a copy.
    book_id  UUID        NOT NULL REFERENCES books (id) ON DELETE RESTRICT,
    position INTEGER     NOT NULL,
    -- e.g. "recommended by Kasia".
    note     TEXT,
    added_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (list_id, book_id)
);

CREATE INDEX book_list_items_book_id_idx ON book_list_items (book_id);

-- ═══════════════════════════════════════════════
-- D. GROUPS (private – only members see them)
-- ═══════════════════════════════════════════════
-- The group's library shows each book ONCE (e.g. "Wiedźmin – 2 copies"),
-- built from book_copies + copy_group_visibility.

CREATE TABLE groups (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name               VARCHAR(100) NOT NULL,
    description        TEXT,
    -- For joining with a code or a link. Making a new code replaces the old one.
    invite_code        VARCHAR(20)  NOT NULL UNIQUE,
    -- Set by the admin: can ordinary members invite others too?
    members_can_invite BOOLEAN      NOT NULL DEFAULT false,
    created_by         UUID         NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX groups_created_by_idx ON groups (created_by);

CREATE TABLE group_members (
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    group_id   UUID        NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    group_role VARCHAR(20) NOT NULL DEFAULT 'MEMBER' CHECK (group_role IN ('MEMBER', 'ADMIN')),
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, group_id)
);

CREATE INDEX group_members_group_id_idx ON group_members (group_id);

-- In which of my groups my copy can be seen.
CREATE TABLE copy_group_visibility (
    copy_id  UUID NOT NULL REFERENCES book_copies (id) ON DELETE CASCADE,
    group_id UUID NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    PRIMARY KEY (copy_id, group_id)
);

CREATE INDEX copy_group_visibility_group_id_idx ON copy_group_visibility (group_id);

-- In which groups my list can be seen (only for visibility = GROUPS).
CREATE TABLE list_group_visibility (
    list_id  UUID NOT NULL REFERENCES book_lists (id) ON DELETE CASCADE,
    group_id UUID NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    PRIMARY KEY (list_id, group_id)
);

CREATE INDEX list_group_visibility_group_id_idx ON list_group_visibility (group_id);

-- Comments about a book seen only inside the group – separate from the public reviews.
CREATE TABLE group_book_comments (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id   UUID        NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    book_id    UUID        NOT NULL REFERENCES books (id) ON DELETE RESTRICT,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    content    TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Also serves the group_id foreign key: comments are always read per group and book.
CREATE INDEX group_book_comments_group_book_idx ON group_book_comments (group_id, book_id);
CREATE INDEX group_book_comments_book_id_idx ON group_book_comments (book_id);
CREATE INDEX group_book_comments_user_id_idx ON group_book_comments (user_id);

-- ═══════════════════════════════════════════════
-- E. BORROWING (between members of a group)
-- ═══════════════════════════════════════════════
-- Rules (checked by the server):
--   * a request can only be sent for an AVAILABLE copy that isn't borrowed right now,
--   * when the owner accepts one request, the other PENDING ones for that copy become SUPERSEDED
--     in the same transaction, and their authors get a notification,
--   * when the copy is borrowed (or the request was SUPERSEDED), the user sees
--     "Notify me when it's available" instead of "Borrow" (availability_watchers).

-- Requests to borrow – always for ONE specific copy.
CREATE TABLE borrow_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    copy_id         UUID         NOT NULL REFERENCES book_copies (id) ON DELETE RESTRICT,
    requester_id    UUID         NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    -- REJECTED = the owner said no; CANCELED = the requester took it back;
    -- SUPERSEDED = closed on its own, because the owner accepted another request.
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELED', 'SUPERSEDED')),
    -- Protects against a double tap: the same key from the same user makes one request.
    idempotency_key VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (requester_id, idempotency_key)
);

CREATE INDEX borrow_requests_copy_id_idx ON borrow_requests (copy_id);
-- One user can have at most one waiting request for a copy.
CREATE UNIQUE INDEX borrow_requests_one_pending
    ON borrow_requests (copy_id, requester_id) WHERE status = 'PENDING';

-- Loans, made when a request is accepted. Both sides confirm the handover and the return.
CREATE TABLE borrowings (
    id                                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id                        UUID        NOT NULL UNIQUE REFERENCES borrow_requests (id) ON DELETE RESTRICT,
    copy_id                           UUID        NOT NULL REFERENCES book_copies (id) ON DELETE RESTRICT,
    borrower_id                       UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    status                            VARCHAR(20) NOT NULL DEFAULT 'PENDING_HANDOVER'
        CHECK (status IN ('PENDING_HANDOVER', 'BORROWED', 'PENDING_RETURN', 'RETURNED')),
    due_date                          TIMESTAMPTZ NOT NULL,
    handover_confirmed_by_owner_at    TIMESTAMPTZ,
    handover_confirmed_by_borrower_at TIMESTAMPTZ,
    return_confirmed_by_owner_at      TIMESTAMPTZ,
    return_confirmed_by_borrower_at   TIMESTAMPTZ,
    created_at                        TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- For offline sync ("what I borrowed / who I lent to").
    updated_at                        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX borrowings_borrower_id_idx ON borrowings (borrower_id);
CREATE INDEX borrowings_updated_at_idx ON borrowings (updated_at);
-- Also serves the copy_id foreign key. A copy has at most one loan that isn't RETURNED yet.
CREATE INDEX borrowings_copy_id_idx ON borrowings (copy_id);
CREATE UNIQUE INDEX borrowings_one_active ON borrowings (copy_id) WHERE status <> 'RETURNED';

-- Who wants a notification when a copy is available again.
-- When a loan becomes RETURNED (or the owner makes the copy AVAILABLE again), they get one.
CREATE TABLE availability_watchers (
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    copy_id    UUID        NOT NULL REFERENCES book_copies (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, copy_id)
);

CREATE INDEX availability_watchers_copy_id_idx ON availability_watchers (copy_id);

-- ═══════════════════════════════════════════════
-- F. SYSTEM
-- ═══════════════════════════════════════════════

CREATE TABLE notifications (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type         VARCHAR(30) NOT NULL CHECK (type IN (
        'BORROW_REQUESTED',
        'REQUEST_ACCEPTED',
        'REQUEST_REJECTED',
        'REQUEST_SUPERSEDED',
        'REQUEST_CANCELED',
        'HANDOVER_CONFIRMED',
        'RETURN_CONFIRMED',
        'RETURN_DUE',
        'RETURN_OVERDUE',
        'COPY_AVAILABLE'
    )),
    -- What it's about (e.g. the request or copy id). The app builds the text.
    reference_id UUID,
    is_read      BOOLEAN     NOT NULL DEFAULT false,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Also serves the user_id foreign key: the list is always the newest first.
CREATE INDEX notifications_user_created_idx ON notifications (user_id, created_at DESC);

-- Who did what and when, among the admins.
CREATE TABLE admin_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id    UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    -- e.g. BLOCK_USER, HIDE_REVIEW, DELETE_GROUP. No CHECK – the list grows with every new admin tool.
    action_type VARCHAR(50) NOT NULL,
    target_type VARCHAR(20) NOT NULL CHECK (target_type IN ('USER', 'GROUP', 'BOOK', 'REVIEW', 'COMMENT')),
    target_id   UUID        NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX admin_logs_admin_id_idx ON admin_logs (admin_id);
