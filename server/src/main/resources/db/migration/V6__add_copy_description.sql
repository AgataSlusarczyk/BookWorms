-- The owner's own description of their copy, e.g. their version of a catalog book's blurb.
-- Only the owner sees it. NULL = they use the catalog's description.
ALTER TABLE book_copies
    ADD COLUMN description TEXT;
