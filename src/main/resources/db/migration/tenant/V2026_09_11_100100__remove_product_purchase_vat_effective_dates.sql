-- ============================================================================
-- TENANT SCHEMA — REMOVE EFFECTIVE DATES FROM PRODUCT PURCHASE VAT
-- ============================================================================

-- 1. Drop check constraint on effective dates
ALTER TABLE product_purchase_vats
    DROP CONSTRAINT IF EXISTS chk_ppv_window;

-- 2. Drop partial unique index that depended on effective_to
DROP INDEX IF EXISTS uq_ppv_open_rate;

-- 3. Drop date columns
ALTER TABLE product_purchase_vats
    DROP COLUMN IF EXISTS effective_from;

ALTER TABLE product_purchase_vats
    DROP COLUMN IF EXISTS effective_to;
