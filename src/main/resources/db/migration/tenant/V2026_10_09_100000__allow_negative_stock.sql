-- ============================================================================
-- TENANT SCHEMA — STOCK MAY GO NEGATIVE
-- ============================================================================
-- A mart sells what is on the shelf whether or not the purchase behind it has
-- been entered yet, so a sale is no longer refused for want of stock: the
-- quantity goes below zero and comes back up when the bill is keyed in.
--
-- The service still refuses every other outflow that would overdraw — a return
-- to a vendor, a write-off, a downward adjustment — so the check that goes here
-- is only the one a sale could trip.

ALTER TABLE product_stocks
    DROP CONSTRAINT IF EXISTS chk_product_stocks_quantity;
