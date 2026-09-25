-- ============================================================================
-- TENANT SCHEMA — PURCHASE RETURNS (debit notes)
-- ============================================================================
-- Goods sent back to a vendor against one of their bills — damaged, expired,
-- short-dated — line by line, never the whole bill wholesale. Numbered on
-- their own sequence (DN-YYYYMM-NNNNN). Not reported to CBMS: the vendor's
-- bill is theirs to file, not the mart's.

CREATE TABLE purchase_returns
(
    id                BIGSERIAL PRIMARY KEY,
    debit_note_number VARCHAR(32)              NOT NULL,
    purchase_id       BIGINT                   NOT NULL REFERENCES purchases (id),
    vendor_id         BIGINT                   NOT NULL REFERENCES vendors (id),
    return_date       DATE                     NOT NULL,
    reason            VARCHAR(255)             NOT NULL,

    tax_scheme        VARCHAR(20)              NOT NULL,

    sub_total         NUMERIC(14, 2)           NOT NULL,
    discount_amount   NUMERIC(14, 2)           NOT NULL DEFAULT 0,
    taxable_amount    NUMERIC(14, 2)           NOT NULL,
    vat_amount        NUMERIC(14, 2)           NOT NULL DEFAULT 0,
    net_total         NUMERIC(14, 2)           NOT NULL,

    remark            VARCHAR(255),

    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX uq_purchase_returns_debit_note_number ON purchase_returns (debit_note_number);
CREATE INDEX idx_purchase_returns_purchase ON purchase_returns (purchase_id);
CREATE INDEX idx_purchase_returns_vendor ON purchase_returns (vendor_id);
CREATE INDEX idx_purchase_returns_created_at ON purchase_returns (created_at DESC);

ALTER TABLE purchase_returns
    ADD CONSTRAINT chk_purchase_returns_tax_scheme CHECK (tax_scheme IN ('VAT', 'NON_VAT'));

ALTER TABLE purchase_returns
    ADD CONSTRAINT chk_purchase_returns_amounts
        CHECK (sub_total >= 0 AND discount_amount >= 0 AND taxable_amount >= 0
               AND vat_amount >= 0 AND net_total >= 0);


CREATE TABLE purchase_return_items
(
    id                     BIGSERIAL PRIMARY KEY,
    purchase_return_id     BIGINT                   NOT NULL REFERENCES purchase_returns (id),
    purchase_item_id       BIGINT                   NOT NULL REFERENCES purchase_items (id),
    product_id             BIGINT                   NOT NULL REFERENCES products (id),

    quantity               NUMERIC(19, 6)           NOT NULL,
    quantity_in_base_units NUMERIC(19, 6)           NOT NULL,
    rate                   NUMERIC(12, 2)           NOT NULL,
    line_total             NUMERIC(14, 2)           NOT NULL,

    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at             TIMESTAMP WITH TIME ZONE
);

ALTER TABLE purchase_return_items
    ADD CONSTRAINT chk_purchase_return_items_amounts
        CHECK (quantity > 0 AND quantity_in_base_units > 0 AND rate >= 0 AND line_total >= 0);

CREATE INDEX idx_purchase_return_items_return ON purchase_return_items (purchase_return_id);
CREATE INDEX idx_purchase_return_items_purchase_item ON purchase_return_items (purchase_item_id);


-- What has been sent back against a vendor bill.
ALTER TABLE purchases
    ADD COLUMN IF NOT EXISTS returned_amount NUMERIC(14, 2) NOT NULL DEFAULT 0;

ALTER TABLE purchases
    ADD CONSTRAINT chk_purchases_returned_amount
        CHECK (returned_amount >= 0 AND returned_amount <= net_total);


-- Stock that went back to a vendor points at the debit note.
ALTER TABLE stock_movements
    DROP CONSTRAINT chk_stock_movements_reference_type;

ALTER TABLE stock_movements
    ADD CONSTRAINT chk_stock_movements_reference_type
        CHECK (reference_type IS NULL
               OR reference_type IN ('PURCHASE', 'PURCHASE_RETURN', 'SALE', 'SALE_RETURN',
                                     'ADJUSTMENT', 'WRITE_OFF'));
