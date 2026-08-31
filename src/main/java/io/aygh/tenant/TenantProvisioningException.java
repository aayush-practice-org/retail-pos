package io.aygh.tenant;

/**
 * A tenant's schema could not be created or migrated. Carries the cause so the
 * admin row can record why, and so a retry has something to report.
 */
public class TenantProvisioningException extends RuntimeException {

    public TenantProvisioningException(String message, Throwable cause) {
        super(message, cause);
    }
}
