-- ============================================================================
-- TENANT SCHEMA — CBMS: PAN registered by default, credentials optional
-- ============================================================================
-- Every mart is PAN registered unless told otherwise: VAT billing is not issued
-- yet, so a setup row created without an explicit registration charges no VAT.
-- The row is created on the first sale, before anyone has entered CBMS
-- credentials, so username and password may be empty until they do.

ALTER TABLE cbms_internal
    ALTER COLUMN tax_registration SET DEFAULT 'PAN_REGISTERED';

ALTER TABLE cbms_internal
    ALTER COLUMN cbms_username DROP NOT NULL;

ALTER TABLE cbms_internal
    ALTER COLUMN cbms_password DROP NOT NULL;

-- Whether the bill has been accepted by the IRD's CBMS.
ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS sync_with_ird BOOLEAN NOT NULL DEFAULT FALSE;
