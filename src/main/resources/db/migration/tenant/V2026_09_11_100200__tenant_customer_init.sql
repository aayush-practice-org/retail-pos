-- ============================================================================
-- TENANT SCHEMA — CUSTOMER INIT
-- ============================================================================

CREATE TABLE customers
(
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(100)             NOT NULL,
    phone        VARCHAR(20)              NOT NULL,
    email        VARCHAR(100),
    pan_number   VARCHAR(30),
    address      VARCHAR(255),
    credit_limit NUMERIC(12, 2),
    is_active    BOOLEAN                  NOT NULL DEFAULT TRUE,

    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX uq_customers_phone ON customers (LOWER(phone))
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_customers_pan ON customers (LOWER(pan_number))
    WHERE pan_number IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX idx_customers_name ON customers (LOWER(name));

-- Link sales to customer
ALTER TABLE sales
    ADD COLUMN customer_id BIGINT REFERENCES customers (id);

CREATE INDEX idx_sales_customer ON sales (customer_id);
