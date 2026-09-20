-- ============================================================================
-- TENANT SCHEMA — RESTORE CONVERSION FACTOR ON UNITS
--
-- V2026_09_11_100000 removed this column on the grounds that conversion is a
-- per-product concern. That is true of *packaging* — a Sack holds whatever the
-- mart decided to fill it with — but not of *physics*: a Kilogram is a thousand
-- Grams in every shop on earth. Dropping both meant a human had to retype 1000
-- for every product sold by the kilogram, which is a universal constant entered
-- by hand, and a typo there silently corrupts a stock ledger.
--
-- So the two are separated rather than merged:
--   units.conversion_factor  — physical, against the measurement type's
--                              reference unit. NULL means "no fixed size".
--   *.pack_quantity          — packaging, per product. Still authoritative.
--
-- NULL is the default precisely so a Sack, a Crate or a Carton cannot pretend
-- to a size it does not have: those rows stay NULL and continue to get their
-- size from the product's pack quantity.
-- ============================================================================

ALTER TABLE units
    ADD COLUMN IF NOT EXISTS conversion_factor NUMERIC(19, 6);

ALTER TABLE units
    DROP CONSTRAINT IF EXISTS chk_units_conversion_factor;

ALTER TABLE units
    ADD CONSTRAINT chk_units_conversion_factor
        CHECK (conversion_factor IS NULL OR conversion_factor > 0);

-- Only the seeded units get a factor. Anything the mart authored itself is left
-- NULL until someone states a size for it.
UPDATE units u
SET conversion_factor = v.factor
FROM (VALUES ('g', 1::NUMERIC),
             ('kg', 1000::NUMERIC),
             ('ml', 1::NUMERIC),
             ('l', 1000::NUMERIC),
             ('pc', 1::NUMERIC),
             ('dz', 12::NUMERIC),
             ('cm', 1::NUMERIC),
             ('m', 100::NUMERIC)) AS v(symbol, factor)
WHERE u.system_defined
  AND LOWER(u.symbol) = v.symbol;

-- A reference unit is the thing others are measured against, so it is 1 by
-- definition; this keeps that true for any reference unit the mart added.
UPDATE units
SET conversion_factor = 1
WHERE reference_unit
  AND conversion_factor IS NULL;
