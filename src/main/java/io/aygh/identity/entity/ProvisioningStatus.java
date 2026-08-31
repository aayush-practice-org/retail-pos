package io.aygh.identity.entity;


public enum ProvisioningStatus {

    /**
     * The admin exists; its schema has not been built yet.
     */
    PENDING,

    /**
     * The schema exists and every tenant migration has been applied to it.
     */
    READY,

    /**
     * Provisioning was attempted and failed — see {@code provisioningError}, then retry.
     */
    FAILED;

    public boolean isUsable() {
        return this == READY;
    }
}
