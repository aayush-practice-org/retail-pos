-- ============================================================================
-- TENANT SCHEMA — SALES: add print_count and is_bill_printed columns
-- ============================================================================
-- Tracks how many times the IRD invoice has been printed.
-- Print #1 is the original; every print after is stamped "COPY OF ORIGINAL (N)".

ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS print_count     INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_bill_printed BOOLEAN NOT NULL DEFAULT FALSE;
