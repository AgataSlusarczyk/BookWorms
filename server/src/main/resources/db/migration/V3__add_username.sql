-- The name friends know you by. Saved in lower case, so "Anna_K" and "anna_k" are the same person.
-- Every account needs one. Accounts made before this were only for testing –
-- clear the local database first (`docker compose down -v` in server/), or this fails on them.
ALTER TABLE users
    ADD COLUMN username VARCHAR(20) NOT NULL UNIQUE,
    ADD CONSTRAINT users_username_lowercase CHECK (username = lower(username));
