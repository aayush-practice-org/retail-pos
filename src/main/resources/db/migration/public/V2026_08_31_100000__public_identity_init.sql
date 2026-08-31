-- ============================================================================
-- PUBLIC SCHEMA — SHARED IDENTITY
--
-- Every account in the installation lives here, whatever tenant it belongs to,
-- so a login can be resolved before we know which schema to route the request
-- at. The tenant's own data lives in a schema of its own, created from
-- db/migration/tenant when the super admin registers the admin who owns it.
--
--   SUPER_ADMIN  tenant_id / tenant_slug NULL  — owns the installation
--   ADMIN        tenant_id = own id           — owns one tenant schema
--   staff        tenant_id = their admin's id — work inside that schema
-- ============================================================================

CREATE TABLE users
(
    id            UUID PRIMARY KEY                  DEFAULT gen_random_uuid(),

    -- Credentials
    username      VARCHAR(50)              NOT NULL,
    password      VARCHAR(255)             NOT NULL,
    email         VARCHAR(255)             NOT NULL,

    -- Authorisation
    role_name     VARCHAR(40)              NOT NULL,
    status        VARCHAR(20)              NOT NULL DEFAULT 'ACTIVE',
    expires_at    TIMESTAMP WITH TIME ZONE,
    last_login_at TIMESTAMP WITH TIME ZONE,

    -- Tenancy. NULL for the super admin, who belongs to no tenant. The slug is
    -- denormalised from admins.slug so a login can pick the schema without a
    -- join, and every request after it can do so straight from the token.
    tenant_id     UUID,
    tenant_slug   VARCHAR(63),

    -- Personal information
    full_name     VARCHAR(255),
    dob           DATE,
    gender        VARCHAR(20),
    country       VARCHAR(100),

    -- Contact information
    mobile_number VARCHAR(30),

    -- Address information
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city          VARCHAR(255),
    state         VARCHAR(255),
    zip_code      VARCHAR(20),

    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP WITH TIME ZONE
);

-- Fails closed: an unknown role name would otherwise resolve to no access at
-- all at runtime, silently, on the next deploy.
ALTER TABLE users
    ADD CONSTRAINT chk_users_role_name CHECK (role_name IN (
                                                            'SUPER_ADMIN', 'ADMIN', 'STORE_MANAGER', 'CASHIER',
                                                            'SALES_EXECUTIVE',
                                                            'INVENTORY_MANAGER', 'STORE_KEEPER', 'PURCHASE_OFFICER',
                                                            'ACCOUNTANT',
                                                            'HR_MANAGER', 'CUSTOMER_SUPPORT'));

ALTER TABLE users
    ADD CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'));

ALTER TABLE users
    ADD CONSTRAINT chk_users_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER'));

-- The super admin stands outside tenancy; everyone else must sit inside one.
-- Enforced here as well as in the service, because a row that breaks it can
-- never be routed to a schema and would fail on every request it makes.
ALTER TABLE users
    ADD CONSTRAINT chk_users_tenancy CHECK (
        (role_name = 'SUPER_ADMIN' AND tenant_id IS NULL AND tenant_slug IS NULL)
            OR (role_name <> 'SUPER_ADMIN' AND tenant_id IS NOT NULL AND tenant_slug IS NOT NULL));

-- Soft-deleted accounts release their username and email for reuse.
CREATE UNIQUE INDEX uq_users_username ON users (LOWER(username)) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_users_email ON users (LOWER(email)) WHERE deleted_at IS NULL;

-- At most one super admin account, ever.
CREATE UNIQUE INDEX uq_users_super_admin ON users ((TRUE))
    WHERE role_name = 'SUPER_ADMIN' AND deleted_at IS NULL;

CREATE INDEX idx_users_role ON users (role_name);
CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_tenant ON users (tenant_id) WHERE deleted_at IS NULL;


-- ============================================================================
-- ADMINS — one row per tenant, owned by the ADMIN user of the same id
-- ============================================================================

CREATE TABLE admins
(
    id                      UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,

    company_name            VARCHAR(255)             NOT NULL,
    company_address         VARCHAR(255),
    company_phone           VARCHAR(30),
    registration_number     VARCHAR(100),

    -- The Postgres schema holding this tenant's data. Immutable once issued:
    -- renaming it would orphan the schema the data actually lives in.
    slug                    VARCHAR(63)              NOT NULL,

    subscription_expires_at TIMESTAMP WITH TIME ZONE,

    -- Whether the tenant schema behind the slug has been created and migrated.
    -- The row is written first and provisioned immediately afterwards, so a
    -- failure leaves something to retry rather than an admin with nowhere to work.
    provisioning_status     VARCHAR(20)              NOT NULL DEFAULT 'PENDING',
    provisioning_error      VARCHAR(1000),
    provisioned_at          TIMESTAMP WITH TIME ZONE,

    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at              TIMESTAMP WITH TIME ZONE
);

ALTER TABLE admins
    ADD CONSTRAINT chk_admins_provisioning_status
        CHECK (provisioning_status IN ('PENDING', 'READY', 'FAILED'));

-- A schema name is a physical resource: it cannot be recycled by a soft delete,
-- so unlike usernames these stay unique across deleted rows too.
CREATE UNIQUE INDEX uq_admins_slug ON admins (slug);

CREATE UNIQUE INDEX uq_admins_company_name ON admins (LOWER(company_name)) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_admins_registration_number ON admins (LOWER(registration_number))
    WHERE registration_number IS NOT NULL AND deleted_at IS NULL;
