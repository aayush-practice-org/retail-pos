-- Create application user
DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1
                       FROM pg_roles
                       WHERE rolname = 'app_user') THEN
            CREATE ROLE app_user
                LOGIN
                PASSWORD 'app_password';
        END IF;
    END
$$;

-- Database permissions
GRANT CONNECT ON DATABASE mart_app TO app_user;
GRANT CREATE ON DATABASE mart_app TO app_user;

-- Schema permissions
GRANT USAGE, CREATE ON SCHEMA public TO app_user;

-- Existing tables
GRANT SELECT, INSERT, UPDATE
    ON ALL TABLES IN SCHEMA public
    TO app_user;

-- Existing sequences
GRANT USAGE, SELECT, UPDATE
    ON ALL SEQUENCES IN SCHEMA public
    TO app_user;

-- Existing functions
GRANT EXECUTE
    ON ALL FUNCTIONS IN SCHEMA public
    TO app_user;

-- Future tables
ALTER DEFAULT PRIVILEGES
    FOR ROLE sts
    IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE
    ON TABLES
    TO app_user;

-- Future sequences
ALTER DEFAULT PRIVILEGES
    FOR ROLE sts
    IN SCHEMA public
    GRANT USAGE, SELECT, UPDATE
    ON SEQUENCES
    TO app_user;

-- Future functions
ALTER DEFAULT PRIVILEGES
    FOR ROLE sts
    IN SCHEMA public
    GRANT EXECUTE
    ON FUNCTIONS
    TO app_user;