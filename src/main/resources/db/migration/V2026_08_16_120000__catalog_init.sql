-- ==========================================
-- MART CATALOGUE INIT
-- units → categories → products → product variants
-- ==========================================

-- 1. System Units (defined at the system level, seeded below, never mutated at runtime)
CREATE TABLE system_units
(
    id                UUID PRIMARY KEY,
    name              VARCHAR(255)             NOT NULL UNIQUE,
    symbol            VARCHAR(20)              NOT NULL UNIQUE,
    measurement_type  VARCHAR(50)              NOT NULL,
    conversion_factor NUMERIC(19, 6)           NOT NULL,
    is_base_unit      BOOLEAN                  NOT NULL,
    version           BIGINT                            DEFAULT 0,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP WITH TIME ZONE
);

-- Seed System Units.
-- conversion_factor is relative to the reference unit of the same measurement
-- type: Gram for WEIGHT, Milliliter for VOLUME, Piece for COUNT, Centimeter for LENGTH.
INSERT INTO system_units (id, name, symbol, measurement_type, conversion_factor, is_base_unit, created_at, updated_at,
                          version)
VALUES ('11111111-2222-3333-4444-555555555501', 'Gram', 'g', 'WEIGHT', 1.000000, true, CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP, 0),
       ('11111111-2222-3333-4444-555555555502', 'Kilogram', 'kg', 'WEIGHT', 1000.000000, false, CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP, 0),
       ('11111111-2222-3333-4444-555555555503', 'Milliliter', 'ml', 'VOLUME', 1.000000, true, CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP, 0),
       ('11111111-2222-3333-4444-555555555504', 'Liter', 'l', 'VOLUME', 1000.000000, false, CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP, 0),
       ('11111111-2222-3333-4444-555555555505', 'Piece', 'pc', 'COUNT', 1.000000, true, CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP, 0),
       ('11111111-2222-3333-4444-555555555506', 'Dozen', 'dz', 'COUNT', 12.000000, false, CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP, 0),
       ('11111111-2222-3333-4444-555555555507', 'Centimeter', 'cm', 'LENGTH', 1.000000, true, CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP, 0),
       ('11111111-2222-3333-4444-555555555508', 'Meter', 'm', 'LENGTH', 100.000000, false, CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

-- 2. Custom Units (mart defined — Sack, Crate, Bundle, ...)
CREATE TABLE custom_units
(
    id                UUID PRIMARY KEY,
    name              VARCHAR(255)             NOT NULL,
    symbol            VARCHAR(20)              NOT NULL,
    measurement_type  VARCHAR(50)              NOT NULL,
    conversion_factor NUMERIC(19, 6)           NOT NULL,
    version           BIGINT                            DEFAULT 0,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX uq_custom_units_name ON custom_units (LOWER(name)) WHERE deleted_at IS NULL;

-- 3. Categories (self referencing — a category may hold subcategories to any depth)
CREATE TABLE categories
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100)             NOT NULL,
    description VARCHAR(255),
    image_url   VARCHAR(255),
    parent_id   BIGINT REFERENCES categories (id) ON DELETE SET NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_categories_parent ON categories (parent_id);

-- Names only have to be unique among siblings, so "Snacks" may sit under both
-- Grocery and Bakery. NULL parents are compared separately because Postgres
-- treats NULLs in a composite unique index as distinct.
CREATE UNIQUE INDEX uq_categories_root_name ON categories (LOWER(name))
    WHERE parent_id IS NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX uq_categories_child_name ON categories (parent_id, LOWER(name))
    WHERE parent_id IS NOT NULL AND deleted_at IS NULL;

-- 4. Products
CREATE TABLE products
(
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(255)             NOT NULL,
    description  VARCHAR(1000),
    brand        VARCHAR(255),
    category_id  BIGINT                   NOT NULL REFERENCES categories (id),
    base_unit_id UUID                     NOT NULL REFERENCES system_units (id),
    image_url    VARCHAR(255),
    is_active    BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_base_unit ON products (base_unit_id);

-- 5. Product Variants (owned by exactly one product, never shared)
CREATE TABLE product_variants
(
    id                   BIGSERIAL PRIMARY KEY,
    product_id           BIGINT                   NOT NULL REFERENCES products (id),
    name                 VARCHAR(100)             NOT NULL,
    sku                  VARCHAR(64),
    barcode              VARCHAR(64),
    pack_size            NUMERIC(19, 6)           NOT NULL,
    original_quantity    NUMERIC(19, 6),
    original_unit_id     UUID,
    original_unit_source VARCHAR(20),
    mrp                  NUMERIC(10, 2),
    selling_price        NUMERIC(10, 2)           NOT NULL,
    is_default           BOOLEAN                  NOT NULL DEFAULT FALSE,
    is_active            BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at           TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_product_variants_product ON product_variants (product_id);
CREATE INDEX idx_product_variants_sku ON product_variants (sku);
CREATE INDEX idx_product_variants_barcode ON product_variants (barcode);

-- Soft deleted rows are excluded so a removed variant does not hold its name,
-- SKU or barcode hostage.
CREATE UNIQUE INDEX uq_product_variants_name ON product_variants (product_id, LOWER(name))
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_product_variants_sku ON product_variants (LOWER(sku))
    WHERE sku IS NOT NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX uq_product_variants_barcode ON product_variants (barcode)
    WHERE barcode IS NOT NULL AND deleted_at IS NULL;
