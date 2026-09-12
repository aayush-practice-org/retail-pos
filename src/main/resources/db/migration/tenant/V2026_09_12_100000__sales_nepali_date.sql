-- ============================================================================
-- TENANT SCHEMA — SALES: add Nepali date and fiscal year columns
-- ============================================================================
-- The client (till / browser) knows the BS date; the server only knows the
-- Instant. Both are stored so invoices and the IRD sales book can be printed
-- with the Nepali calendar date the transaction was entered on, exactly as the
-- operator typed it, rather than a server-derived approximation.

ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS nepali_date  VARCHAR(20),
    ADD COLUMN IF NOT EXISTS fiscal_year  VARCHAR(20);
