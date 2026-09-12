-- ============================================================================
-- TENANT SCHEMA — CBMS INTERNAL
-- ============================================================================
-- Stores CBMS (Central Billing Monitoring System) credentials and tax setup
-- per tenant (VAT/PAN registration, tax inclusive vs exclusive pricing).

CREATE TABLE IF NOT EXISTS cbms_internal
(
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        UUID,
    tenant_slug      VARCHAR(63),
    cbms_username    VARCHAR(255)             NOT NULL,
    cbms_password    VARCHAR(255)             NOT NULL,
    tax_registration VARCHAR(50)              NOT NULL,
    tax_included     BOOLEAN                  NOT NULL DEFAULT FALSE,
    pan              VARCHAR(50),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP WITH TIME ZONE
);
