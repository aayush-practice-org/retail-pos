-- ============================================================================
-- TENANT SCHEMA — INVENTORY REBUILD
--
-- Replaces the catalogue laid down by V2026_08_31_100100. That version modelled
-- units as two tables (system_units, custom_units), which forced product_variants
-- to carry an original_unit_source column naming which of them its id pointed
-- at — a foreign key Postgres could not enforce. It is replaced here by one
-- units table with a system_defined flag, and one real foreign key.
--
-- Nothing carries a tenant column: the schema *is* the tenant, and a query can
-- only ever see the one its connection is pointed at.
--
-- DESTRUCTIVE. The tables it drops are the previous catalogue, and any rows in
-- them go with it. Safe while no mart has stocked its catalogue yet; once one
-- has, this has to become a copy-forward instead.
-- ============================================================================

DROP TABLE IF EXISTS product_variants CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS custom_units CASCADE;
DROP TABLE IF EXISTS system_units CASCADE;


-- ============================================================================
-- 1. UNITS — the mart's whole unit vocabulary, seeded and mart-defined alike
-- ============================================================================

CREATE TABLE units
(
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(100)             NOT NULL,
    symbol            VARCHAR(20)              NOT NULL,
    measurement_type  VARCHAR(20)              NOT NULL,

    -- Against the reference unit of the same measurement type: Kilogram is 1000
    -- where Gram is the reference. Units with no fixed size — a Sack, a Crate —
    -- carry 1 and defer to the pack quantity the product sets.
    conversion_factor NUMERIC(19, 6)           NOT NULL DEFAULT 1,

    -- The one unit each measurement type is expressed in.
    reference_unit    BOOLEAN                  NOT NULL DEFAULT FALSE,

    -- Seeded below and not editable by the mart, versus mart-defined.
    system_defined    BOOLEAN                  NOT NULL DEFAULT FALSE,

    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP WITH TIME ZONE
);

ALTER TABLE units
    ADD CONSTRAINT chk_units_measurement_type
        CHECK (measurement_type IN ('WEIGHT', 'VOLUME', 'COUNT', 'LENGTH'));

ALTER TABLE units
    ADD CONSTRAINT chk_units_conversion_factor CHECK (conversion_factor > 0);

-- Soft-deleted rows release their name and symbol for reuse.
CREATE UNIQUE INDEX uq_units_name ON units (LOWER(name)) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_units_symbol ON units (LOWER(symbol)) WHERE deleted_at IS NULL;

-- Exactly one reference unit per measurement type: two would make "convert to
-- the reference" ambiguous, and the conversion factors meaningless.
CREATE UNIQUE INDEX uq_units_reference ON units (measurement_type)
    WHERE reference_unit AND deleted_at IS NULL;

CREATE INDEX idx_units_measurement_type ON units (measurement_type);

INSERT INTO units (name, symbol, measurement_type, conversion_factor, reference_unit, system_defined)
VALUES ('Gram', 'g', 'WEIGHT', 1, TRUE, TRUE),
       ('Kilogram', 'kg', 'WEIGHT', 1000, FALSE, TRUE),
       ('Milliliter', 'ml', 'VOLUME', 1, TRUE, TRUE),
       ('Liter', 'l', 'VOLUME', 1000, FALSE, TRUE),
       ('Piece', 'pc', 'COUNT', 1, TRUE, TRUE),
       ('Dozen', 'dz', 'COUNT', 12, FALSE, TRUE),
       ('Centimeter', 'cm', 'LENGTH', 1, TRUE, TRUE),
       ('Meter', 'm', 'LENGTH', 100, FALSE, TRUE);


-- ============================================================================
-- 2. CATEGORIES
-- ============================================================================

CREATE TABLE categories
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100)             NOT NULL,
    description VARCHAR(255),
    image       VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX uq_categories_name ON categories (LOWER(name)) WHERE deleted_at IS NULL;


-- ============================================================================
-- 3. CATEGORY UNITS — which units a category permits, per side of the trade
--
-- The policy every product in the category inherits. A unit allowed for both
-- buying and selling is two rows, so "may this be sold in kg" stays one lookup.
-- ============================================================================

CREATE TABLE category_units
(
    id          BIGSERIAL PRIMARY KEY,
    category_id BIGINT                   NOT NULL REFERENCES categories (id),
    unit_id     BIGINT                   NOT NULL REFERENCES units (id),
    usage_type  VARCHAR(20)              NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP WITH TIME ZONE
);

ALTER TABLE category_units
    ADD CONSTRAINT chk_category_units_usage CHECK (usage_type IN ('PURCHASE', 'SELLING'));

CREATE UNIQUE INDEX uq_category_units ON category_units (category_id, unit_id, usage_type)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_category_units_category ON category_units (category_id);
CREATE INDEX idx_category_units_unit ON category_units (unit_id);


-- ============================================================================
-- 4. PRODUCTS
-- ============================================================================

CREATE TABLE products
(
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(255)             NOT NULL,
    product_code VARCHAR(64),
    base_code    VARCHAR(64),
    description  VARCHAR(1000),
    brand        VARCHAR(255),
    image        VARCHAR(255),
    is_active    BOOLEAN                  NOT NULL DEFAULT TRUE,

    category_id  BIGINT                   NOT NULL REFERENCES categories (id),

    -- The unit every stock figure for this product is held in. Never updated:
    -- changing it would silently reinterpret every quantity already recorded.
    base_unit_id BIGINT                   NOT NULL REFERENCES units (id),

    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_base_unit ON products (base_unit_id);
CREATE INDEX idx_products_active ON products (is_active) WHERE deleted_at IS NULL;

-- Names only have to be unique within a category, so two aisles may both stock
-- a "Basmati". Codes are mart-wide, because that is what they are for.
CREATE UNIQUE INDEX uq_products_name_in_category ON products (category_id, LOWER(name))
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_products_code ON products (LOWER(product_code))
    WHERE product_code IS NOT NULL AND deleted_at IS NULL;


-- ============================================================================
-- 5. PRODUCT PURCHASE UNITS — how a product is bought
--
-- The category says a Sack is permissible; this says what a sack of *this*
-- product holds, so a sack of rice and a sack of sugar can differ while sharing
-- one units row.
-- ============================================================================

CREATE TABLE product_purchase_units
(
    id             BIGSERIAL PRIMARY KEY,
    product_id     BIGINT                   NOT NULL REFERENCES products (id),
    unit_id        BIGINT                   NOT NULL REFERENCES units (id),

    -- How many of the product's base unit one of these holds. Every stock
    -- movement is converted through this before it is written, so the ledger
    -- only ever holds base units.
    pack_quantity  NUMERIC(19, 6)           NOT NULL,

    purchase_price NUMERIC(12, 2),
    is_default     BOOLEAN                  NOT NULL DEFAULT FALSE,
    is_active      BOOLEAN                  NOT NULL DEFAULT TRUE,

    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP WITH TIME ZONE
);

ALTER TABLE product_purchase_units
    ADD CONSTRAINT chk_ppu_pack_quantity CHECK (pack_quantity > 0);

ALTER TABLE product_purchase_units
    ADD CONSTRAINT chk_ppu_purchase_price CHECK (purchase_price IS NULL OR purchase_price >= 0);

CREATE UNIQUE INDEX uq_ppu_product_unit ON product_purchase_units (product_id, unit_id)
    WHERE deleted_at IS NULL;

-- At most one default per product. The service clears the others before setting
-- a new one; this is what holds when two requests try it at once.
CREATE UNIQUE INDEX uq_ppu_default ON product_purchase_units (product_id)
    WHERE is_default AND deleted_at IS NULL;

CREATE INDEX idx_ppu_product ON product_purchase_units (product_id);


-- ============================================================================
-- 6. PRODUCT SELLING UNITS — how a product is sold
-- ============================================================================

CREATE TABLE product_selling_units
(
    id            BIGSERIAL PRIMARY KEY,
    product_id    BIGINT                   NOT NULL REFERENCES products (id),
    unit_id       BIGINT                   NOT NULL REFERENCES units (id),

    pack_quantity NUMERIC(19, 6)           NOT NULL,
    selling_price NUMERIC(12, 2)           NOT NULL,
    mrp           NUMERIC(12, 2),
    sku           VARCHAR(64),
    barcode       VARCHAR(64),
    is_default    BOOLEAN                  NOT NULL DEFAULT FALSE,
    is_active     BOOLEAN                  NOT NULL DEFAULT TRUE,

    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP WITH TIME ZONE
);

ALTER TABLE product_selling_units
    ADD CONSTRAINT chk_psu_pack_quantity CHECK (pack_quantity > 0);

ALTER TABLE product_selling_units
    ADD CONSTRAINT chk_psu_selling_price CHECK (selling_price >= 0);

ALTER TABLE product_selling_units
    ADD CONSTRAINT chk_psu_mrp CHECK (mrp IS NULL OR mrp >= 0);

CREATE UNIQUE INDEX uq_psu_product_unit ON product_selling_units (product_id, unit_id)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_psu_default ON product_selling_units (product_id)
    WHERE is_default AND deleted_at IS NULL;

-- A scan must resolve to exactly one row, so these are mart-wide rather than
-- per product.
CREATE UNIQUE INDEX uq_psu_barcode ON product_selling_units (barcode)
    WHERE barcode IS NOT NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX uq_psu_sku ON product_selling_units (LOWER(sku))
    WHERE sku IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX idx_psu_product ON product_selling_units (product_id);


-- ============================================================================
-- 7. PRODUCT PURCHASE VAT — the rate applied to buying a product in one unit
--
-- Effective-dated rather than a column on the purchase unit: a rate is a fact
-- about a period. Overwriting it when the government moves VAT would restate
-- what last quarter's purchases were taxed at.
-- ============================================================================

CREATE TABLE product_purchase_vats
(
    id                       BIGSERIAL PRIMARY KEY,
    product_purchase_unit_id BIGINT                   NOT NULL REFERENCES product_purchase_units (id),

    -- A percentage: 13 means 13%.
    rate                     NUMERIC(5, 2)            NOT NULL,
    effective_from           DATE                     NOT NULL,

    -- Open-ended while this is the rate in force.
    effective_to             DATE,

    created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at               TIMESTAMP WITH TIME ZONE
);

ALTER TABLE product_purchase_vats
    ADD CONSTRAINT chk_ppv_rate CHECK (rate >= 0 AND rate <= 100);

ALTER TABLE product_purchase_vats
    ADD CONSTRAINT chk_ppv_window CHECK (effective_to IS NULL OR effective_to >= effective_from);

-- At most one open rate per purchase unit, so "what is the rate today" has one
-- answer. The service closes the previous rate before opening a new one; this is
-- what holds if two requests try it at once.
CREATE UNIQUE INDEX uq_ppv_open_rate ON product_purchase_vats (product_purchase_unit_id)
    WHERE effective_to IS NULL AND deleted_at IS NULL;

CREATE INDEX idx_ppv_purchase_unit ON product_purchase_vats (product_purchase_unit_id);
