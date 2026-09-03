-- ============================================================================
-- TENANT SCHEMA — VENDORS
--
-- The suppliers a mart buys from, the ledger of what each is owed and has been
-- paid, and the trail of which purchases came from whom.
--
-- Nothing carries a tenant column: the schema *is* the tenant, and a query can
-- only ever see the one its connection is pointed at.
-- ============================================================================


-- ============================================================================
-- 1. VENDORS — supplier master data
--
-- No balance column. What a vendor is owed is derived from vendor_balances, so
-- a figure on a screen can always be traced to the entries that produced it.
-- ============================================================================

CREATE TABLE vendors
(
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(100)             NOT NULL,
    address        VARCHAR(255)             NOT NULL,

    -- Optional: a vendor reached only through a rep on the road has no number
    -- of its own. The application does not require one either.
    contact_number VARCHAR(30),

    -- Tax registration. Optional, but unique across the mart where given.
    pan_number     VARCHAR(30),

    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP WITH TIME ZONE
);

-- Soft-deleted rows release their name and PAN for reuse.
CREATE UNIQUE INDEX uq_vendors_name ON vendors (LOWER(name)) WHERE deleted_at IS NULL;

-- Two vendors on one PAN would make a purchase return unattributable.
CREATE UNIQUE INDEX uq_vendors_pan ON vendors (LOWER(pan_number))
    WHERE pan_number IS NOT NULL AND deleted_at IS NULL;


-- ============================================================================
-- 2. VENDOR BALANCES — the ledger
--
-- Append-only. A mistake is corrected by posting the opposite entry, never by
-- editing the row that recorded it, so the account can be reconstructed as it
-- stood on any day rather than only as it stands now. That is also why there is
-- no version column here: a row is written once and never contended for.
--
-- Amounts are always positive; balance_type is what gives them direction.
-- Outstanding = PAYABLE - RECEIVABLE - SETTLEMENT.
-- ============================================================================

CREATE TABLE vendor_balances
(
    id           BIGSERIAL PRIMARY KEY,
    vendor_id    BIGINT                   NOT NULL REFERENCES vendors (id),

    amount       NUMERIC(19, 6)           NOT NULL,
    balance_type VARCHAR(20)              NOT NULL,

    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP WITH TIME ZONE
);

ALTER TABLE vendor_balances
    ADD CONSTRAINT chk_vendor_balances_type
        CHECK (balance_type IN ('PAYABLE', 'RECEIVABLE', 'SETTLEMENT'));

-- A zero entry says nothing and a negative one duplicates what balance_type
-- already carries — either would make the totals ambiguous.
ALTER TABLE vendor_balances
    ADD CONSTRAINT chk_vendor_balances_amount CHECK (amount > 0);

-- Both reads are per vendor: the paged ledger, and the summary that groups by
-- type. One index over the pair serves each.
CREATE INDEX idx_vendor_balances_vendor_type ON vendor_balances (vendor_id, balance_type);


-- ============================================================================
-- 3. VENDOR HISTORIES — which purchases came from which vendor
--
-- purchase_id carries no foreign key: the purchasing module is not built yet,
-- and referencing a table that does not exist would stop this migration from
-- applying. It becomes a real reference when purchasing lands.
-- ============================================================================

CREATE TABLE vendor_histories
(
    id          BIGSERIAL PRIMARY KEY,
    vendor_id   BIGINT                   NOT NULL REFERENCES vendors (id),
    purchase_id BIGINT,

    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_vendor_histories_vendor ON vendor_histories (vendor_id);
CREATE INDEX idx_vendor_histories_purchase ON vendor_histories (purchase_id);
