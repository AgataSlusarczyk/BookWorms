-- The profile photo: a path on the server, e.g. "/photos/<file>.jpg", or NULL when there's none.
ALTER TABLE users
    ADD COLUMN photo_url VARCHAR(255);
