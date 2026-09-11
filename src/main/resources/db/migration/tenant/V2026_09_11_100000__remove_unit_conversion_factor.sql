-- ============================================================================
-- TENANT SCHEMA — REMOVE CONVERSION FACTOR FROM UNITS
--
-- Conversion factors are now handled per-product via pack quantities in
-- product_purchase_units and product_selling_units rather than statically
-- on the units table itself.
-- ============================================================================

-- 1. Drop check constraint on conversion_factor if it exists
ALTER TABLE units
    DROP CONSTRAINT IF EXISTS chk_units_conversion_factor;

-- 2. Drop conversion_factor column
ALTER TABLE units
    DROP COLUMN IF EXISTS conversion_factor;
