-- Ratings: each person has one review of a book, and changes it instead of adding another.
-- has_spoilers hides the text until someone taps it. updated_at shows when the rating last changed.
ALTER TABLE book_reviews
    ADD COLUMN has_spoilers BOOLEAN     NOT NULL DEFAULT false,
    ADD COLUMN updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD CONSTRAINT book_reviews_one_per_user UNIQUE (book_id, user_id);
