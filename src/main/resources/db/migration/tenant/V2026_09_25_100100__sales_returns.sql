-- ============================================================================
-- TENANT SCHEMA — SALES RETURNS (credit notes)
-- ============================================================================
-- A sales return is the IRD's credit note: goods handed back against a bill,
-- numbered on its own sequence (CN-YYYYMM-NNNNN) and reported to CBMS against
-- the invoice it reverses. Amounts are prorated from the original bill so the
-- credit note carries the same VAT treatment the bill was raised under.

CREATE TABLE sales_returns
(
    id                 BIGSERIAL PRIMARY KEY,
    credit_note_number VARCHAR(32)              NOT NULL,
    sale_id            BIGINT                   NOT NULL REFERENCES sales (id),
    returned_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    reason             VARCHAR(255)             NOT NULL,

    -- Snapshotted from the bill, as the credit note has to print them
    tax_scheme         VARCHAR(20)              NOT NULL,
    customer_name      VARCHAR(150),
    customer_pan       VARCHAR(30),

    nepali_date        VARCHAR(20),
    fiscal_year        VARCHAR(20),

    sub_total          NUMERIC(14, 2)           NOT NULL,
    discount_amount    NUMERIC(14, 2)           NOT NULL DEFAULT 0,
    taxable_amount     NUMERIC(14, 2)           NOT NULL,
    vat_amount         NUMERIC(14, 2)           NOT NULL DEFAULT 0,
    net_total          NUMERIC(14, 2)           NOT NULL,

    -- Cash handed back. The rest of net_total came off what the customer owed.
    refund_amount      NUMERIC(14, 2)           NOT NULL DEFAULT 0,

    sync_with_ird      BOOLEAN                  NOT NULL DEFAULT FALSE,
    remark             VARCHAR(255),

    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at         TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX uq_sales_returns_credit_note_number ON sales_returns (credit_note_number);
CREATE INDEX idx_sales_returns_sale ON sales_returns (sale_id);
CREATE INDEX idx_sales_returns_returned_at ON sales_returns (returned_at DESC);

ALTER TABLE sales_returns
    ADD CONSTRAINT chk_sales_returns_tax_scheme CHECK (tax_scheme IN ('VAT', 'NON_VAT'));

ALTER TABLE sales_returns
    ADD CONSTRAINT chk_sales_returns_amounts
        CHECK (sub_total >= 0 AND discount_amount >= 0 AND taxable_amount >= 0
               AND vat_amount >= 0 AND net_total >= 0 AND refund_amount >= 0
               AND refund_amount <= net_total);


CREATE TABLE sales_return_items
(
    id                      BIGSERIAL PRIMARY KEY,
    sales_return_id         BIGINT                   NOT NULL REFERENCES sales_returns (id),
    sale_item_id            BIGINT                   NOT NULL REFERENCES sale_items (id),
    product_id              BIGINT                   NOT NULL REFERENCES products (id),

    product_name            VARCHAR(255)             NOT NULL,
    unit_symbol             VARCHAR(20)              NOT NULL,

    quantity                NUMERIC(19, 6)           NOT NULL,
    quantity_in_base_units  NUMERIC(19, 6)           NOT NULL,
    rate                    NUMERIC(12, 2)           NOT NULL,
    line_total              NUMERIC(14, 2)           NOT NULL,

    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at              TIMESTAMP WITH TIME ZONE
);

ALTER TABLE sales_return_items
    ADD CONSTRAINT chk_sales_return_items_amounts
        CHECK (quantity > 0 AND quantity_in_base_units > 0 AND rate >= 0 AND line_total >= 0);

CREATE INDEX idx_sales_return_items_return ON sales_return_items (sales_return_id);
CREATE INDEX idx_sales_return_items_sale_item ON sales_return_items (sale_item_id);


-- What has been credited back against a bill. The customer owes
-- net_total - returned_amount; paid_amount never exceeds that.
ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS returned_amount NUMERIC(14, 2) NOT NULL DEFAULT 0;

ALTER TABLE sales
    ADD CONSTRAINT chk_sales_returned_amount
        CHECK (returned_amount >= 0 AND returned_amount <= net_total);


-- Stock that came back on a return points at the credit note.
ALTER TABLE stock_movements
    DROP CONSTRAINT chk_stock_movements_reference_type;

ALTER TABLE stock_movements
    ADD CONSTRAINT chk_stock_movements_reference_type
        CHECK (reference_type IS NULL
               OR reference_type IN ('PURCHASE', 'SALE', 'SALE_RETURN', 'ADJUSTMENT', 'WRITE_OFF'));
