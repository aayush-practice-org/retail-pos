-- ==========================================
-- MART IDENTITY INIT
-- back-office users and their roles
-- ==========================================

CREATE TABLE users
(
    id         UUID PRIMARY KEY,
    full_name  VARCHAR(255),
    username   VARCHAR(255)             NOT NULL,
    password   VARCHAR(255)             NOT NULL,
    email      VARCHAR(255)             NOT NULL,
    phone      VARCHAR(30),
    role_name  VARCHAR(30)              NOT NULL,
    is_active  BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Soft deleted accounts release their username and email for reuse
CREATE UNIQUE INDEX uq_users_username ON users (LOWER(username)) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_users_email ON users (LOWER(email)) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_role ON users (role_name);
