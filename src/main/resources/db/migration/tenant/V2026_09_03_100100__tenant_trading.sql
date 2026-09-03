-- ============================================================================
-- TENANT SCHEMA — TRADING
--
-- Stock, purchases and sales: the three tables that turn a catalogue into a
-- shop. They arrive together because they are one story — a purchase puts stock
-- on the shelf, a sale takes it off, and neither means anything without the
-- ledger in between.
--
-- Depends on V2026_09_03_100000 (vendors) and V2026_08_31_100200 (products).
--
-- Nothing carries a tenant column: the schema *is* the tenant, and a query can
-- only ever see the one its connection is pointed at.
-- ============================================================================


-- ============================================================================
-- 1. PRODUCT STOCKS — what is on the shelf, in the product's base unit
--
-- A cache of stock_movements, not an independent number. It exists so the till
-- can answer "is there any left" with one indexed read instead of summing every
-- movement ever recorded, and only StockLedgerService writes it.
--
-- version is the point of the row: two tills selling the last item both read
-- the same quantity, and optimistic locking is what stops the second one
-- overselling. Selling takes a row lock on top of it, so the loser waits a few
-- milliseconds instead of losing a whole basket to a retry.
-- ============================================================================

CREATE TABLE product_stocks
(
    id            BIGSERIAL PRIMARY KEY,
    product_id    BIGINT                   NOT NULL REFERENCES products (id),

    quantity      NUMERIC(19, 6)           NOT NULL DEFAULT 0,

    -- At or below this a product reads as low. Zero means the mart has not set
    -- one, and it is only ever reported out of stock.
    reorder_level NUMERIC(19, 6)           NOT NULL DEFAULT 0,

    version       BIGINT                   NOT NULL DEFAULT 0,

    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP WITH TIME ZONE
);

-- One stock row per product. Not partial on deleted_at: soft-deleting a stock
-- row and opening a second would leave two numbers for one shelf.
CREATE UNIQUE INDEX uq_product_stocks_product ON product_stocks (product_id);

-- Stock is never negative. The service checks it against the locked row for a
-- readable message; this is what holds if anything ever writes around it.
ALTER TABLE product_stocks
    ADD CONSTRAINT chk_product_stocks_quantity CHECK (quantity >= 0);

ALTER TABLE product_stocks
    ADD CONSTRAINT chk_product_stocks_reorder CHECK (reorder_level >= 0);

-- Serves the "what needs attention" listing, which is the stock screen's default.
CREATE INDEX idx_product_stocks_low ON product_stocks (quantity)
    WHERE deleted_at IS NULL;


-- ============================================================================
-- 2. STOCK MOVEMENTS — the ledger every quantity is derived from
--
-- Append-only. Stock that went out is never edited away, it is brought back by
-- a movement the other way, which is what lets "why does this say 12" be
-- answered. quantity is always positive and in base units; movement_type
-- carries the direction.
--
-- reference_id has no foreign key because it points at whichever table
-- reference_type names — a purchase, a sale, or nothing at all for a hand-
-- entered correction. One column cannot reference three tables.
-- ============================================================================

CREATE TABLE stock_movements
(
    id               BIGSERIAL PRIMARY KEY,
    product_id       BIGINT                   NOT NULL REFERENCES products (id),

    movement_type    VARCHAR(30)              NOT NULL,
    quantity         NUMERIC(19, 6)           NOT NULL,

    -- The product's stock immediately after this movement. Stored rather than
    -- recomputed so a ledger page renders without a running sum, and so a drift
    -- between ledger and cache is visible rather than silent.
    balance_after    NUMERIC(19, 6)           NOT NULL,

    -- What the operator actually typed. "You removed 2" reads very differently
    -- from "you removed 100000" when the operator chose sacks.
    entered_quantity NUMERIC(19, 6),
    entered_unit_id  BIGINT REFERENCES units (id),

    reference_id     BIGINT,
    reference_type   VARCHAR(20),
    remark           VARCHAR(255),

    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP WITH TIME ZONE
);

ALTER TABLE stock_movements
    ADD CONSTRAINT chk_stock_movements_type
        CHECK (movement_type IN ('PURCHASE_IN', 'SALE_OUT', 'SALE_RETURN_IN',
                                 'PURCHASE_RETURN_OUT', 'ADJUSTMENT_IN',
                                 'ADJUSTMENT_OUT', 'WRITE_OFF'));

ALTER TABLE stock_movements
    ADD CONSTRAINT chk_stock_movements_reference_type
        CHECK (reference_type IS NULL
               OR reference_type IN ('PURCHASE', 'SALE', 'ADJUSTMENT', 'WRITE_OFF'));

-- A zero movement says nothing; a negative one duplicates what movement_type
-- already carries.
ALTER TABLE stock_movements
    ADD CONSTRAINT chk_stock_movements_quantity CHECK (quantity > 0);

-- The ledger is read newest-first per product, which is what this pair serves.
CREATE INDEX idx_stock_movements_product ON stock_movements (product_id, created_at DESC);

-- "Which movements did this document cause" — used when a purchase or sale is
-- opened, and by anything reconciling one.
CREATE INDEX idx_stock_movements_reference ON stock_movements (reference_type, reference_id)
    WHERE reference_id IS NOT NULL;


-- ============================================================================
-- 3. PURCHASES — goods bought from a vendor, on one of the vendor's bills
--
-- Every money column is stored as agreed on the day. Recalculating a purchase
-- later against today's prices or today's VAT rate would quietly restate
-- history, which is the one thing a purchase record exists to prevent.
-- ============================================================================

CREATE TABLE purchases
(
    id              BIGSERIAL PRIMARY KEY,
    vendor_id       BIGINT                   NOT NULL REFERENCES vendors (id),

    -- The vendor's own bill number, as printed on the paper that came with the
    -- goods.
    bill_number     VARCHAR(64)              NOT NULL,
    purchase_date   DATE                     NOT NULL,

    payment_method  VARCHAR(20)              NOT NULL,
    tax_scheme      VARCHAR(20)              NOT NULL,

    sub_total       NUMERIC(14, 2)           NOT NULL,
    discount_amount NUMERIC(14, 2)           NOT NULL DEFAULT 0,
    taxable_amount  NUMERIC(14, 2)           NOT NULL,
    vat_amount      NUMERIC(14, 2)           NOT NULL DEFAULT 0,
    net_total       NUMERIC(14, 2)           NOT NULL,

    remark          VARCHAR(255),

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

ALTER TABLE purchases
    ADD CONSTRAINT chk_purchases_payment_method
        CHECK (payment_method IN ('CASH', 'CARD', 'BANK', 'WALLET', 'CREDIT'));

ALTER TABLE purchases
    ADD CONSTRAINT chk_purchases_tax_scheme CHECK (tax_scheme IN ('VAT', 'NON_VAT'));

ALTER TABLE purchases
    ADD CONSTRAINT chk_purchases_amounts
        CHECK (sub_total >= 0 AND discount_amount >= 0 AND vat_amount >= 0
               AND discount_amount <= sub_total);

-- Re-keying the same bill is the commonest data-entry mistake there is, and it
-- doubles both the stock and what the vendor is owed. Soft-deleted rows release
-- the number so a mis-keyed bill can be re-entered.
CREATE UNIQUE INDEX uq_purchases_vendor_bill ON purchases (vendor_id, LOWER(bill_number))
    WHERE deleted_at IS NULL;

CREATE INDEX idx_purchases_date ON purchases (purchase_date DESC);
CREATE INDEX idx_purchases_vendor ON purchases (vendor_id);


CREATE TABLE purchase_items
(
    id                       BIGSERIAL PRIMARY KEY,
    purchase_id              BIGINT                   NOT NULL REFERENCES purchases (id),
    product_id               BIGINT                   NOT NULL REFERENCES products (id),
    product_purchase_unit_id BIGINT                   NOT NULL REFERENCES product_purchase_units (id),

    -- How many of the purchase unit, as written on the bill.
    quantity                 NUMERIC(19, 6)           NOT NULL,

    -- Base units per purchase unit, copied as it stood when the line was
    -- recorded. Editing the catalogue later must not restate how much this bill
    -- actually put on the shelf.
    pack_quantity            NUMERIC(19, 6)           NOT NULL,
    quantity_in_base_units   NUMERIC(19, 6)           NOT NULL,

    rate                     NUMERIC(12, 2)           NOT NULL,
    line_total               NUMERIC(14, 2)           NOT NULL,

    created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at               TIMESTAMP WITH TIME ZONE
);

ALTER TABLE purchase_items
    ADD CONSTRAINT chk_purchase_items_amounts
        CHECK (quantity > 0 AND pack_quantity > 0 AND quantity_in_base_units > 0
               AND rate >= 0 AND line_total >= 0);

CREATE INDEX idx_purchase_items_purchase ON purchase_items (purchase_id);
CREATE INDEX idx_purchase_items_product ON purchase_items (product_id);


-- ============================================================================
-- 4. SALES — goods sold, and the invoice raised for them
--
-- There is no separate invoice table. In a mart the sale *is* the invoice: they
-- are created together, never one without the other, and splitting them would
-- buy nothing but a join and the chance of the two disagreeing.
--
-- The customer's details sit on the row rather than pointing at a customer
-- record: most mart sales are to someone who will never be a record, and a VAT
-- bill needs the name and PAN as they were given at the till that day.
-- ============================================================================

CREATE TABLE sales
(
    id              BIGSERIAL PRIMARY KEY,

    invoice_number  VARCHAR(32)              NOT NULL,
    sold_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    channel         VARCHAR(20)              NOT NULL,
    tax_scheme      VARCHAR(20)              NOT NULL,

    customer_name   VARCHAR(150),
    customer_phone  VARCHAR(30),
    customer_pan    VARCHAR(30),

    sub_total       NUMERIC(14, 2)           NOT NULL,
    discount_amount NUMERIC(14, 2)           NOT NULL DEFAULT 0,
    taxable_amount  NUMERIC(14, 2)           NOT NULL,
    vat_amount      NUMERIC(14, 2)           NOT NULL DEFAULT 0,
    net_total       NUMERIC(14, 2)           NOT NULL,

    payment_method  VARCHAR(20)              NOT NULL,
    payment_status  VARCHAR(20)              NOT NULL,
    paid_amount     NUMERIC(14, 2)           NOT NULL DEFAULT 0,

    -- Cash handed back. Stored because the receipt has to print it.
    change_amount   NUMERIC(14, 2)           NOT NULL DEFAULT 0,

    remark          VARCHAR(255),

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

-- Not partial on deleted_at: an invoice number is never reused, voided or not.
-- A gap in the sequence is a voided bill, and that is information an auditor
-- asks about. This index is also what orders two tills racing for the same
-- number — the loser's insert fails and the sale is retried.
CREATE UNIQUE INDEX uq_sales_invoice_number ON sales (invoice_number);

ALTER TABLE sales
    ADD CONSTRAINT chk_sales_channel CHECK (channel IN ('POS', 'BACK_OFFICE'));

ALTER TABLE sales
    ADD CONSTRAINT chk_sales_tax_scheme CHECK (tax_scheme IN ('VAT', 'NON_VAT'));

ALTER TABLE sales
    ADD CONSTRAINT chk_sales_payment_method
        CHECK (payment_method IN ('CASH', 'CARD', 'BANK', 'WALLET', 'CREDIT'));

ALTER TABLE sales
    ADD CONSTRAINT chk_sales_payment_status
        CHECK (payment_status IN ('UNPAID', 'PARTIAL', 'PAID'));

-- Paid never exceeds the bill: anything over is change, and has its own column.
ALTER TABLE sales
    ADD CONSTRAINT chk_sales_amounts
        CHECK (sub_total >= 0 AND discount_amount >= 0 AND vat_amount >= 0
               AND paid_amount >= 0 AND change_amount >= 0
               AND discount_amount <= sub_total
               AND paid_amount <= net_total);

CREATE INDEX idx_sales_sold_at ON sales (sold_at DESC);
CREATE INDEX idx_sales_payment_status ON sales (payment_status)
    WHERE payment_status <> 'PAID' AND deleted_at IS NULL;


CREATE TABLE sale_items
(
    id                      BIGSERIAL PRIMARY KEY,
    sale_id                 BIGINT                   NOT NULL REFERENCES sales (id),
    product_id              BIGINT                   NOT NULL REFERENCES products (id),
    product_selling_unit_id BIGINT                   NOT NULL REFERENCES product_selling_units (id),

    -- Copied, not joined. A bill reprinted after the product was renamed still
    -- has to read the way it was handed to the customer.
    product_name            VARCHAR(255)             NOT NULL,
    unit_symbol             VARCHAR(20)              NOT NULL,

    quantity                NUMERIC(19, 6)           NOT NULL,
    pack_quantity           NUMERIC(19, 6)           NOT NULL,
    quantity_in_base_units  NUMERIC(19, 6)           NOT NULL,

    rate                    NUMERIC(12, 2)           NOT NULL,
    mrp                     NUMERIC(12, 2),
    discount_amount         NUMERIC(12, 2)           NOT NULL DEFAULT 0,
    line_total              NUMERIC(14, 2)           NOT NULL,

    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at              TIMESTAMP WITH TIME ZONE
);

ALTER TABLE sale_items
    ADD CONSTRAINT chk_sale_items_amounts
        CHECK (quantity > 0 AND pack_quantity > 0 AND quantity_in_base_units > 0
               AND rate >= 0 AND discount_amount >= 0 AND line_total >= 0);

CREATE INDEX idx_sale_items_sale ON sale_items (sale_id);
CREATE INDEX idx_sale_items_product ON sale_items (product_id);


-- ============================================================================
-- 5. BACKFILL — open a stock row for every product already in the catalogue
--
-- The application opens one on first touch, so this is not strictly required;
-- it is here so the stock screen shows the whole catalogue at zero on day one
-- rather than filling in as products happen to be sold.
-- ============================================================================

INSERT INTO product_stocks (product_id, quantity, reorder_level)
SELECT p.id, 0, 0
FROM products p
WHERE p.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM product_stocks s WHERE s.product_id = p.id);
